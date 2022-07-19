package io.deephaven.demo.control;

import io.deephaven.base.Lazy;
import io.deephaven.demo.api.*;
import io.deephaven.demo.gcloud.GoogleDeploymentManager;
import io.deephaven.demo.manager.Execute;
import io.deephaven.demo.manager.IClusterController;
import io.deephaven.demo.manager.NameGen;
import io.smallrye.common.constraint.NotNull;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.mutiny.core.Vertx;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static io.deephaven.demo.manager.NameConstants.*;
import static io.deephaven.demo.manager.IClusterController.*;
import static io.deephaven.demo.gcloud.GoogleDeploymentManager.*;

/**
 * DomainController:
 * <p>
 * <p>
 * The brains enforcing our cluster management policies via live machine-to-dns-to-ip mappings.
 * <p>
 * <p>
 * This class reads in data from google, and uses that to glue together a set of IP addresses {@link IpPool},
 * <p>
 * to DNS records {@link DomainPool}, and to keep tabs on all {@link Machine} objects.
 * <p>
 * <p>
 * This class will discard any Machine that is not a version we should be controlling.
 * <p>
 * <p>
 * Only the "leader" will be allowed to turn off / delete old machines.
 * <p>
 * The leader is determined by checking "my IP address" == "IP address of public controller".
 * <p>
 * The "public controller" is the {@link io.deephaven.demo.manager.NameConstants#DOMAIN}, demo.deephaven.app
 * <p>
 */
public class ClusterController implements IClusterController {

    private static final Logger LOG = Logger.getLogger(ClusterController.class);

    private final GoogleDeploymentManager manager;
    private final IpPool ips;
    private final MachinePool machines;
    private final CountDownLatch latch;
    private final OkHttpClient client;
    private Lazy<Boolean> leader;
    private long checkLatency = 1;
    private volatile boolean shutdown;
    private final WeakHashMap<Machine, Boolean> hasLogged = new WeakHashMap<>();

    private final DhDemoReporter reporter;
    private volatile boolean reservationScheduled;

    public ClusterController() {
        this(new GoogleDeploymentManager("/tmp"));
    }

    public ClusterController(@NotNull final GoogleDeploymentManager manager) {
        this(manager, true, true);
    }

    public ClusterController(@NotNull final GoogleDeploymentManager manager, boolean loadData) {
        this(manager, loadData, loadData);
    }

    public ClusterController(@NotNull final GoogleDeploymentManager manager, boolean loadMachines,
            boolean loadIpsAndDns) {
        this.manager = manager;
        this.reporter = new DhDemoReporter(this::isLeader);
        this.client = new OkHttpClient();
        latch = new CountDownLatch(loadMachines ? 4 : 3);
        if (manager == null) {
            throw new NullPointerException("Manager cannot be null");
        }
        this.ips = manager.getIpPool();
        this.machines = new MachinePool();

        resetLeader();

        // Test if we have gcloud login and print useful messages
        Exception sentinel = new Exception();
        try {
            final Execute.ExecutionResult result =
                    Execute.executeQuiet("gcloud", "config", "list", "account", "--format", "value(core.account)");
            if (result.code == 0) {
                LOG.infof("Running cluster controller as %s:\nLoading machines? %s\nLoading ipsAndDns? %s)",
                        result.out.trim(), loadMachines, loadIpsAndDns);
            } else {
                throw sentinel;
            }
        } catch (Exception e) {
            String failMsg = "gcloud user not authenticated, disabling metadata reads";
            if (e == sentinel) {
                LOG.error(failMsg);
            } else {
                LOG.error(failMsg, e);
            }
        }

        // resolve the leader off-thread, since there's no need to block now
        Execute.setTimer("Leader Check", () -> {
            if (leader.get()) {
                LOG.info("We are the leader!");
            } else {
                LOG.info("We are not the leader.");
            }
        });
        if (loadIpsAndDns) {
            // load used first, so we can figure out machine IPs
            Execute.setTimer("Load Used IPs", this::loadIpsUsedInitial);
            Execute.setTimer("Load Unused IPs", this::loadIpsUnusedInitial);
            Execute.setTimer("Load Domains", this::loadDomainsInitial);
        }
        if (loadMachines) {
            // don't load machines or start any other controller threads if we are manually creating machines
            Execute.setTimer("Load Machines", this::loadMachinesInitial);
        }
    }

    private void resetLeader() {
        leader = new Lazy<>(() -> {
            try {
                final Execute.ExecutionResult result = Execute.execute("bash", "-c",
                        "[[ $(dig +short " + DOMAIN + ") == $(dig +short $(hostname)." + DOMAIN + ") ]] " +
                                "&& echo leader || echo follower");
                return "leader".equals(result.out.trim());
            } catch (IOException | InterruptedException e) {
                LOG.error("Unable to tell if we are leader", e);
                return false;
            }
            // return true;
        });
    }

    private void monitorLoop() {
        reporter.recordUsage(
                (int)machines.getAllMachines()
                    .filter(Machine::isInUse).count(),
                (int)machines.getAllMachines()
                    .filter(Machine::isOnline).count()
        );
        final Vertx vx = Vertx.vertx();
        vx.setTimer(TimeUnit.MINUTES.toMillis(checkLatency), t -> {
            Execute.setTimer("Monitor Loop", () -> {
                try {
                    checkState();
                } finally {
                    monitorLoop();
                }
                return true;
            });
        });
    }

    private synchronized void checkState() throws IOException, InterruptedException {
        // make sure to re-check if we are still the leader.
        // This will prevent an old controller from acting like the leader when a new one is promoted
        resetLeader();
        Execute.setTimer("Check Used IPs", this::loadIpsUsed);
        Execute.setTimer("Check Unused IPs", this::loadIpsUnused);
        Execute.setTimer("Check Domains", this::loadDomains);
        loadMachines();
        // check for machines that have exceeded their limit, reboot them, and then put them back in the usable pool
        Set<Machine> runningMachines = new ConcurrentHashSet<>();
        Set<Machine> availableMachines = new ConcurrentHashSet<>();
        Set<Machine> offlineMachines = new ConcurrentHashSet<>();
        Set<Machine> usedMachines = new ConcurrentHashSet<>();
        machines.getAllMachines().forEach(machine -> {
            if (machine.isOnline()) {
                if (machine.getExpiry() > 0 && machine.getExpiry() < System.currentTimeMillis()) {
                    if (isValidVersion(machine)) {
                        LOG.infof("Machine %s has past its expiry by %sms, shutting it down", machine,
                                System.currentTimeMillis() - machine.getExpiry());
                    }
                    // machine is past expiry...
                    // lets turn this box off, unless it's version is old, in which case, delete it
                    turnOff(machine, true);
                    offlineMachines.add(machine);
                } else {
                    runningMachines.add(machine);
                    if (machine.isInUse()) {
                        usedMachines.add(machine);
                    } else {
                        availableMachines.add(machine);
                    }
                }
            } else {
                machine.setInUse(false);
                offlineMachines.add(machine);
            }
        });
        int numRunning = runningMachines.size();
        int poolSize = getPoolSize();
        int numBuffer = getPoolBuffer();
        final int offlineSize = getMaxOfflineSize();
        LOG.info("Done checking state:\n" +
                runningMachines.size() + " running machines\n" +
                offlineMachines.size() + " offline machines\n" +
                usedMachines.size() + " used machines\n" +
                availableMachines.size() + " available machines\n" +
                poolSize + " or more running machines desired\n" +
                numBuffer + " minimum available machines desired\n" +
                offlineSize + " maximum offline machines desired\n");
        if (!leader.get()) {
            LOG.info("We are not the leader, refusing to alter the machine pool");
            return;
        }
        if (numRunning < poolSize) {
            // not enough nodes are running... turn some on!
            for (Machine next : offlineMachines) {
                if (!next.isOnline()) {
                    numRunning++;
                    next.setOnline(true);
                    // purposely don't parallelize: google will bark at us if we scale up too fast
                    try {
                        manager.turnOn(next);
                        availableMachines.add(next);
                    } catch (Exception e) {
                        LOG.error("Error turning on machine " + next, e);
                    }
                    if (numRunning >= poolSize) {
                        break;
                    }
                }
            }
            // if fewer than needed nodes running, turn things on
            // if we don't have enough machines, add however many machines we need to fill pool
            int limit = 3;
            while (numRunning < poolSize) {
                if (numRunning >= getMaxPoolSize()) {
                    LOG.error("There are already " + numRunning + " running instances, " +
                            "and max size is " + getMaxPoolSize());
                    break;
                } else {
                    LOG.warn("Only " + numRunning + " machines, but want " + poolSize + "; adding a new machine");
                    numRunning++;
                    final Machine newMachine = requestMachine(NameGen.newName(), false);
                    availableMachines.add(newMachine);
                    runningMachines.add(newMachine);
                    if (--limit <= 0) {
                        LOG.info("Taking a break from creating new machines to refresh metadata");
                        return;
                    }
                }
            }
        }
        if (availableMachines.size() < numBuffer) {
            int limit = 3;
            while (availableMachines.size() < numBuffer) {
                LOG.warn("Only " + availableMachines.size() + " machines are running and unusued, but want "
                        + getPoolBuffer() + " available machines; adding a new machine");
                final Machine newMachine = requestMachine(NameGen.newName(), false);
                availableMachines.add(newMachine);
                runningMachines.add(newMachine);
                if (--limit <= 0) {
                    LOG.infof("Taking a break from creating machines to refresh metadata");
                    return;
                }
            }
        } else if (numRunning > poolSize) {
            LOG.info(numRunning + " running machines > " + poolSize
                    + " preferred pool size; trying to shut down extra machines");
            // if more than needed nodes are running, turn off any nodes not servicing clients
            int numShut = 0;
            Set<Machine> notInUse = new TreeSet<>(MachinePool.CMP);
            for (Machine next : runningMachines) {
                if (next.getExpiry() < System.currentTimeMillis()) {
                    next.setInUse(false);
                }
                if (next.isInUse()) {
                    final long millis = next.getExpiry() - System.currentTimeMillis();
                    final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis); // rounds millis into minutes
                    LOG.infof("%s (%s) expires in %s minutes", next.getHost(), next.getDomainName(),
                            // +1 b/c we rounded down already
                            1 + minutes);
                    if (millis > getSessionTtl()) {
                        // this should not happen once we get all existing machine labels fixed up
                        LOG.errorf("%s has an expiry (%s minutes) greater than session TTL (%s minutes)!",
                                next.getHost(), minutes, TimeUnit.MILLISECONDS.toMinutes(getSessionTtl()));
                        fixLease(next);
                    }
                } else {
                    notInUse.add(next);
                }
            }
            // leave a buffer of unused machines running, even if we've exceeded pool size
            for (Machine next : notInUse) {
                if (next.getExpiry() > 0 && next.getExpiry() < System.currentTimeMillis()
                        && availableMachines.size() > getPoolBuffer()) {
                    LOG.infof("Turning off unneeded machine %s, expired %s minutes ago", next.toStringShort(),
                            TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - next.getExpiry()));
                    numRunning--;
                    numShut++;
                    availableMachines.remove(next);
                    turnOff(next, false);
                    if (numRunning <= poolSize) {
                        break;
                    }
                }
            }
            for (Machine next : notInUse) {
                if (next.getExpiry() == 0 && availableMachines.size() > getPoolBuffer()) {
                    LOG.infof("Turning off never-used machine %s", next.toStringShort());
                    numRunning--;
                    numShut++;
                    availableMachines.remove(next);
                    turnOff(next, false);
                    if (numRunning <= poolSize) {
                        break;
                    }
                }
            }


            LOG.info("Shut off " + numShut + " machines; warm-machines available: " + availableMachines.size());
        }
        LOG.info("Attempting to retire machines that have been on too long");
        int retired = 0;
        for (Machine available : availableMachines) {
            if (available.isOnline() && !available.isInUse()) {
                long aliveFor = System.currentTimeMillis() - available.getLastOnline();
                if (aliveFor > getDeletionTtl()) {
                    retired++;
                    LOG.infof("Retiring machine %s as it has been on too long (%s minutes) since being used / started",
                            available.toStringShort(), TimeUnit.MILLISECONDS.toMinutes(aliveFor));
                    // this machine has been online for 3 hours after it was last used,
                    // or 3 hour + sessionTTL w/o being used.
                    // turn this machine off...
                    turnOff(available, true);
                    // and request a new machine
                    if (shouldReserveReplacementMachine()) {
                        requestMachine(false, false);
                    }
                }
            }
        }
        LOG.infof("Retired %s machines", retired);

        // kill any offline machines that are older than a given time; for now, ttl + 1 hour
        reduceOfflineMachines();
        if (retired > 0) {
            // if we retired any machines, we may be able to reduce our reserved machine counts
            try {
                checkReservations(numRunning);
            } catch (Exception e) {
                // don't let reservation
                LOG.error("Unable to update reservations", e);
            }
        }
    }

    private void scheduleReservationCheck() {
        if (!reservationScheduled) {
            reservationScheduled = true;
            Execute.setTimer("Checking reservations", ()-> {
                try {
                    int numRunning = machines.getNumberMachines() - machines.getNumberOfflineMachines();
                    checkReservations(numRunning);
                } finally {
                    reservationScheduled = false;
                }
            });
        }
    }

    private void checkReservations(final int numRunning) {
        if (!leader.get()) {
            // only the leader should consider reservations!
            return;
        }
        final int reservationsAllowed = numRunning + getExtraReservationSize();
        final Map<String, int[]> reservations = getReservations();
        int currentReservations = 0, usedReservations = 0;
        for (int[] value : reservations.values()) {
            currentReservations += value[0];
            usedReservations += value[1];
        }
        LOG.infof("We want %s reservations, and are currently using %s out of %s reservations", reservationsAllowed, usedReservations, currentReservations);

        if (currentReservations < numRunning) {
            // we have fewer reservations than we have running machines. Reserve more.
            manager.reserveMachines(reservations.keySet(), getExtraReservationSize()); // just add small increments at a time
            return;
        }
        while (reservationsAllowed < currentReservations) {
            // we want fewer reservations than we have
            boolean didWork = false;
            for (
                    final Iterator<Map.Entry<String, int[]>> itr = reservations.entrySet().iterator();
                    itr.hasNext();) {
                Map.Entry<String, int[]> e = itr.next();
                int[] reservation = e.getValue();
                if (reservation[1] == 0) {
                    // no reservations in use, we can remove this one
                    manager.removeReservations(e.getKey());
                    currentReservations -= e.getValue()[0];
                    didWork = true;
                    itr.remove();
                }
            }
            if (!didWork) {
                // no unused reservations. keep waiting until more machines are shut down
                LOG.infof("Unable to delete enough reservations to reduce usage from %s to %s", currentReservations, reservationsAllowed);
                break;
            }
        }
        if (reservationsAllowed < currentReservations) {
            // TODO: issue a command to resize existing reservations that have unused machine counts
            // see https://cloud.google.com/compute/docs/instances/reserving-zonal-resources#consuming_reserved_instances
        }
    }

    private Map<String, int[]> getReservations() {
        return manager.getReservations();
    }

    private void reduceOfflineMachines() throws IOException, InterruptedException {
        if (machines.getNumberOfflineMachines() <= getMaxOfflineSize()) {
            return;
        }
        LOG.infof("Attempting to reduce offline machines from %s to %s", machines.getNumberOfflineMachines(),
                getMaxOfflineSize());
        String expiredHourAgo = toTime(System.currentTimeMillis() - getSessionTtl() - TimeUnit.HOURS.toMillis(1));
        // TODO: hide all gcloud executions behind DeploymentManager
        final Execute.ExecutionResult allAncients = gcloudQuiet(true, false, "instances", "list",
                "-q",
                "--format", getMachineFormatFlag(),
                "--filter",
                "( labels." + LABEL_LEASE + " < " + expiredHourAgo + " OR NOT labels." + LABEL_LEASE + " )"
                        + " AND " +
                        "labels." + LABEL_PURPOSE + " <= " + PURPOSE_WORKER
                        // ridiculous <=, >= needed to represent = because = actually means contains, not equals...
                        + " AND " +
                        "labels." + LABEL_PURPOSE + " >= " + PURPOSE_WORKER);
        if (allAncients.code == 0) {
            for (String machine : allAncients.out.split("\n")) {
                final Machine mach = updateMachineFromCsv(machine);
                if (machines.getNumberOfflineMachines() <= getMaxOfflineSize()) {
                    break;
                }
                if (mach != null && !mach.isOnline()) {
                    LOG.infof("Deleting unneeded offline machine %s", mach.toStringShort());
                    turnOff(mach, false);
                }
            }
        } else {
            LOG.errorf("Failed(%s) to read in old, offline machines: %s", allAncients.code,
                    allAncients.getSourceCode());
            warnResult(allAncients);
        }
    }

    private void turnOff(final Machine machine, boolean revive) {
        machine.setOnline(false);
        boolean validVersion = isValidVersion(machine);
        if (!validVersion) {
            synchronized (hasLogged) {
                if (null == hasLogged.put(machine, true)) {
                    LOG.infof("Incorrect version %s (!= our version %s) detected for machine %s", machine.getVersion(),
                            VERSION_MANGLE, machine);
                }
            }
            // immediately remove this machine from rotation, so we don't consider it beyond the scope of this method.
            machines.removeMachine(machine);
            // declare the machine as in-use, for now, so any methods with a reference to it won't try to use it.
            machine.setInUse(true);
        }
        // get off the fork-join pool to do IO:
        Execute.setTimer("Decommission " + machine.getHost(), () -> {
            try {
                // TODO: ping the machine and have a procedure that warns the user this is going to happen:
                // also, we should be hooking up a new DNS name to the machine and deleting the one that is in use
                if (validVersion) {

                    machine.setOnline(false);
                    boolean keep = revive && machines.getNumberOfflineMachines() <= getMaxOfflineSize()
                            && machine.getLastOnline() > (System.currentTimeMillis() - getDeletionTtl());
                    if (keep) {
                        if (leader.get()) {
                            machines.clearExpiry(machine);
                            manager.removeLabel(machine, LABEL_LEASE);
                            gcloud(true, "instances", "stop", "-q", machine.getHost());
                            // setup new DNS for next user
                            manager.replaceDNS(this, machine);
                        }
                        machine.setInUse(false);
                        // put the machine back in the pool
                        machines.addMachine(machine);
                    } else {
                        // yank the machine out of the pool, make sure nobody tries to use it:
                        machines.removeMachine(machine);
                        machine.setDestroyed(true);
                        machine.setInUse(true);

                        // if we're the leader, kill the box
                        if (leader.get()) {
                            manager.deleteMachine(machine.getHost());
                            // setup new DNS for next user
                            // (we delete the machine, but still want fresh DNS for the IP we are releasing)
                            manager.replaceDNS(this, machine);
                        }
                    }
                } else { // not a valid version
                    // only the leader gets to delete instances.
                    if (leader.get()) {
                        if (allowedToDelete(machine)) {
                            // only delete versions older than ourselves.
                            // we don't want an old controller to touch new machines,
                            // but we do want new controllers to delete old machines!
                            machine.setDestroyed(true);
                            manager.deleteMachine(machine.getHost());
                            // setup new DNS for next user
                            manager.replaceDNS(this, machine);
                        }
                    }
                    // mark as "in use" b/c it should not be used anymore (it was already removed from the map)
                    machine.setInUse(true);
                }
                if (shouldReserveReplacementMachine()) {
                    final Machine newMach = requestMachine(false, false);
                    LOG.infof("We shut down %s and the pool was empty enough, so we started new machine %s",
                            machine.toStringShort(), newMach.toStringShort());
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error trying to decommission " + machine.getHost());
                e.printStackTrace();
            }
        });
    }

    private boolean allowedToDelete(final Machine machine) {
        return allowedToDelete(machine.getVersion(), machine.toString(), machine.getExpiry());
    }

    private long getDeletionTtl() {
        return getSessionTtl() + TimeUnit.HOURS.toMillis(3);
    }

    private boolean shouldReserveReplacementMachine() {
        return leader.get() && machines.needsMoreMachines(getPoolBuffer(), getPoolSize(), getMaxPoolSize());
    }

    /**
     * The maximum number of machines to allow at one time (prevents creating new machines)
     * 
     * @return 150 during business hours, 75 otherwise.
     */
    private int getMaxPoolSize() {
        String poolSize = System.getenv("MAX_POOL_SIZE");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        poolSize = System.getProperty("dh-maxPoolSize");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        if (isPeakHours()) {
            return 80;
        }
        return 35;
    }

    private int getMaxOfflineSize() {
        String poolSize = System.getenv("MAX_OFFLINE_SIZE");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        poolSize = System.getProperty("dh-maxOfflineSize");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        if (isPeakHours()) {
            return 9;
        }
        return 3;
    }

    private int getExtraReservationSize() {
        String reservationSize = System.getenv("RESERVATION_BUFFER_SIZE");
        if (reservationSize != null) {
            return Integer.parseInt(reservationSize);
        }
        reservationSize = System.getProperty("dh-reservationBufferSize");
        if (reservationSize != null) {
            return Integer.parseInt(reservationSize);
        }
        if (isPeakHours()) {
            return 2;
        }
        return 1;
    }

    /**
     * @return The number of warm, unused machines to keep around. Default is 4 during business hours, 2 otherwise.
     */
    private int getPoolBuffer() {
        String poolSize = System.getenv("POOL_BUFFER");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        poolSize = System.getProperty("dh-poolBuffer");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        if (isPeakHours()) {
            return 4;
        }
        return 2;
    }

    /**
     * @return The desired size of the "always on" pool of machines. During business hours, we keep 20 machines always
     *         available, only 5 afterhours. We will start adding new machines to keep at least {@link #getPoolBuffer()}
     *         (15 biz | 5 afterhours) machines available
     */
    private int getPoolSize() {
        String poolSize = System.getenv("POOL_SIZE");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        poolSize = System.getProperty("dh-poolSize");
        if (poolSize != null) {
            return Integer.parseInt(poolSize);
        }
        if (isPeakHours()) {
            return 6;
        }
        return 2;
    }


    private void loadMachinesInitial() {
        try {
            loadMachines();
            latch.countDown();
            Execute.setTimer("Wait until loaded", () -> {
                waitUntilReady();
                // a little extra delay: give user a chance to request a machine before we possibly clean it up!
                Execute.setTimer("Refresh state", () -> {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    try {
                        checkState();
                    } finally {
                        monitorLoop();
                    }
                    return true;
                });
            });
        } catch (Throwable t) {
            shutdown = true;
            System.err.println("Unable to load machines, shutting down controller.");
            throw t;
        }
    }

    private void loadMachines() {
        if (shutdown) {
            return;
        }
        String rawOut;
        try {
            LOG.info("Reloading machine metadata from google");
            long mark = System.currentTimeMillis();
            final Execute.ExecutionResult result;
            result = gcloudQuiet(true, false, "instances", "list",
                    "--filter",
                    // so annoying... google prints a warning about = operator changing;
                    // for whatever terrible reason, key<=val && key>=val is how to express key=val,
                    // because key=val is deprecated form of contains(), see `gcloud topic filters`
                    "labels." + LABEL_PURPOSE + "<=" + PURPOSE_WORKER +
                            " AND " +
                            "labels." + LABEL_PURPOSE + ">=" + PURPOSE_WORKER,
                    "--format", getMachineFormatFlag(),
                    "--page-size", Integer.toString(getMaxPoolSize() * 2),
                    "-q");
            rawOut = result.out;
            if (rawOut.contains("Quota exceeded")) {
                checkLatency = checkLatency + 1;
                return;
            } else {
                checkLatency = Math.max(1, checkLatency - 1);
            }
            final String[] lines = rawOut.split("\n");
            if (rawOut.contains("not present")) {
                // empty, no parsing...
                LOG.info("0 active workers found");
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Got " + lines.length + " active workers:");
                    LOG.trace(rawOut);
                } else {
                    LOG.info("Got " + lines.length + " total workers (use trace logging to see response)");
                }
                int online = 0;
                for (String line : lines) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    try {
                        final Machine updated = updateMachineFromCsv(line);
                        if (updated != null) {
                            updated.setMark(mark);
                            if (updated.isOnline()) {
                                online++;
                            }
                        }
                    } catch (Throwable t) {
                        reporter.recordError("Failure parsing machine metadata " + line, t);
                    }
                }
                LOG.infof("Found %s running machines", online);
            }
            machines.getAllMachines().forEach(m -> {
                if (m.getMark() < mark) {
                    // this machine has disappeared! ...forget about it!
                    if (isValidVersion(m)) {
                        LOG.infof("Machine %s seems to have disappeared! (perhaps another controller has removed it?)",
                                m.getHost());
                    }
                    machines.removeMachine(m);
                }
            });

        } catch (IOException | InterruptedException e) {
            reporter.recordError("Failed to get active workers", e);
        }
    }

    private Machine updateMachineFromCsv(final String line) {
        if (line.contains("Google API")) {
            return null;
        }
        String[] bits = line.split(",");
        final String name = bits[getIndexName()];

        if (bits.length < getIndexLength()) {
            // note: we purposely put a never-null item last in the response, so split() doesn't trick us about size
            LOG.error("UNEXPECTED MACHINE METADATA FORMAT: " + line);
            return null;
        }
        boolean online = false;
        if (bits[getIndexStatus()].length() > 0) {
            online =
                    "RUNNING".equals(bits[getIndexStatus()]) ||
                            "STAGING".equals(bits[getIndexStatus()]) ||
                            "PROVISIONING".equals(bits[getIndexStatus()]);
        }
        final String ipAddr, ipName;
        if (bits[getIndexIp()].length() > 0) {
            ipAddr = bits[getIndexIp()].trim();
        } else {
            ipAddr = null;
        }
        // skip any "expensive" operations for invalid versions (we may want to shut this machine down)
        String version = bits[getIndexVersion()];
        boolean needsVersionUpdate = false;
        if (!isValidVersion(version)) {
            LOG.infof("Found invalid version; our version %s, machine version: %s, all data: %s", VERSION_MANGLE,
                    bits[getIndexVersion()], line);
            if (version.contains("_") && VERSION_MANGLE.contains("_")) {
                // a user's custom machine based on git branch name, lets allow version changes
                needsVersionUpdate = true;
            } else {
                // a controller-spawned worker... just ignore.
                maybeRemove(name, version, ipAddr, bits[getIndexHostname()], bits[getIndexLease()], line);
                return null;
            }
        }
        boolean needsIpLabel = false;
        if (bits[getIndexIpName()].length() > 0) {
            ipName = bits[getIndexIpName()].trim();
        } else {
            LOG.infof("No IP name returned in metadata for %s, looking address name up....", name);
            final IpMapping ip = findByIp(ips, ipAddr);
            ipName = ip == null ? null : ip.getName();
            needsIpLabel = ip != null;
        }
        final IpMapping ip = ipName == null ? null : ips.updateOrCreate(ipName, ipAddr);
        Machine mach = machines.getOrCreate(name, this, ip, bits[getIndexIp()]);

        if (needsIpLabel) {
            manager.addLabel(mach, LABEL_IP_NAME, ipName);
        }
        if (needsVersionUpdate) {
            manager.addLabel(mach, LABEL_VERSION, VERSION_MANGLE);
        }
        if (bits[getIndexHostname()].length() > 0) {
            // we aren't really setting a string field,
            // rather we're selecting an existing DomainMapping from the object.
            try {
                String domain = mach.domain() == null ? DOMAIN : mach.domain().getDomainRoot();
                mach.setDomainName(bits[getIndexHostname()].trim() + "." + domain);
            } catch (Throwable t) {
                LOG.error("Got unexpected exception loading machine " + line);
                // just return null if it's busted
                return null;
            }
        }

        mach.setOnline(online);
        mach.setVersion(version);
        if (isValidVersion(mach)) {
            machines.addMachine(mach);
        } else if (!mach.isInUse()) {
            // never turn off unexpired machines.
            if (mach.getExpiry() <= System.currentTimeMillis()) {
                if (leader.get()) {
                    LOG.infof("Turning off invalid-version offline machine %s", mach);
                }
                turnOff(mach, false);
            }
            machines.removeMachine(mach);
        }
        String purpose = bits[getIndexPurpose()];
        switch (purpose) {
            case PURPOSE_WORKER:
                mach.setController(false);
                mach.setSnapshotCreate(false);
                break;
            case PURPOSE_CONTROLLER:
                mach.setController(true);
                mach.setSnapshotCreate(false);
                break;
            case PURPOSE_CREATOR_WORKER:
                mach.setSnapshotCreate(false);
                mach.setController(false);
                break;
            case PURPOSE_CREATOR_CONTROLLER:
                mach.setSnapshotCreate(false);
                mach.setController(true);
                break;
        }
        if (bits[getIndexLease()].length() > 0) {
            if (!bits[getIndexLease()].startsWith("2")) {
                // this lease is broken. fix it.
                fixLease(mach);
            }
            machines.expireInTimeString(mach, bits[getIndexLease()]);
        } else {
            // this machine may or may not have a lease label in gcloud metadata, but it might soon!
            // check if we know this machine was recently reserved before we mark it as not-in-use!
            if (mach.getLastOnline() > 0 && System.currentTimeMillis() - mach.getLastOnline() > getSessionTtl()) {
                mach.setInUse(false);
            }
        }
        return mach;
    }

    private void maybeRemove(final String name, final String version, final String ip, final String hostname,
            final String lease, String debugString) {
        if (!leader.get()) {
            // only the leader gets to delete things
            return;
        }
        final Long expiry = lease == null || lease.isEmpty() ? null : parseTime(lease, debugString);
        if (allowedToDelete(version, debugString, expiry)) {
            Execute.setTimer("Delete " + name + "@" + version, () -> {
                try {
                    LOG.infof("Deleting old machine %s; debug info: %s", name, debugString);
                    manager.deleteMachine(name);
                } catch (Exception e) {
                    LOG.warnf(e, "Unable to delete machine %s; debug info: %s", name, debugString);
                }
                if (hostname != null && hostname.length() > 0) {
                    DomainMapping mapping = new DomainMapping(hostname, DOMAIN);
                    LOG.infof("Purging old DNS record %s for machine %s (%s)", mapping, name, debugString);
                    final String cmd = "dig +short " + mapping.getDomainQualified();
                    try {
                        if (!Execute.execute(cmd).out.isEmpty()) {
                            manager.dns().tx(change -> change.removeRecord(mapping, ip));
                        }
                    } catch (Exception e) {
                        LOG.warnf(e,"Error trying to check DNS mapping %s", cmd);
                    }
                }
            });
        }
    }

    private void fixLease(final Machine mach) {
        // TODO: instead of committing a label-add operation and hoping it doesn't randomly fail,
        // we should instead submit a job to apply this label, and let it fail up to N times (~4)
        final String expiry = toTime(System.currentTimeMillis() + getSessionTtl());
        LOG.infof("Fixing broken lease; expiry was %sms on %s", mach.getExpiry(), mach);
        manager.addLabel(mach, LABEL_LEASE, expiry, (r, e) -> {
            if (e == null) {
                LOG.infof("Successfully renewed lease of %s (%s) to %s", mach.getHost(), mach.getDomainName(), expiry);
            } else {
                LOG.errorf(e, "Unable to clear lease for machine %s", mach);
            }
        });
    }

    private boolean isValidVersion(final Machine machine) {
        return isValidVersion(machine.getVersion());
    }

    private boolean isValidVersion(String version) {
        return VERSION_MANGLE.equals(version);
    }

    private String getMachineFormatFlag() {
        return "csv[box,no-heading](" +
        // if you change the order of these arguments, please update the getIndex*() methods below too.
                "name," +
                "labels." + LABEL_DOMAIN + "," +
                "labels." + LABEL_USER + "," +
                "networkInterfaces[0].accessConfigs[0].natIP," +
                "labels." + LABEL_PURPOSE + "," +
                "labels." + LABEL_LEASE + "," +
                "labels." + LABEL_IP_NAME + "," +
                "labels." + LABEL_VERSION + "," +
                "status" + // status is never empty, so it's best to keep this the last item in this response.
                ")";
    }

    private int getIndexName() {
        return 0;
    }

    private int getIndexHostname() {
        return 1;
    }

    private int getIndexUser() {
        return 2;
    }

    private int getIndexIp() {
        return 3;
    }

    private int getIndexPurpose() {
        return 4;
    }

    private int getIndexLease() {
        return 5;
    }

    private int getIndexIpName() {
        return 6;
    }

    private int getIndexVersion() {
        return 7;
    }

    private int getIndexStatus() {
        // The last item in our csv response should be non-empty, so String.split() gives accurate sized arrays
        return 8;
    }

    // STOP!
    // Make sure that new indices are added above, and that getIndexLength(), below, is the total number of indices
    // STOP MORE:
    // The last item in this list should be never-empty, or semantics around length-checking split() csv line may fail.
    private int getIndexLength() {
        return 9;
    }

    private void loadDomainsInitial() {
        try {
            loadDomains();
            latch.countDown();
            Execute.setTimer("Cleanup DNS", () -> {
                waitUntilReady();
                waitUntilIpsCreated();
                // now! map all our known domains to known IP addresses, and delete anything left hanging
                cleanupDNS();
            });
        } catch (Throwable t) {
            shutdown = true;
            System.err.println("Unable to load domains, shutting down controller.");
            throw t;
        }
    }


    private void loadDomains() {
        if (shutdown) {
            return;
        }
        try {
            final DomainPool domains = manager.getDomainPool();
            long mark = domains.markAll();
            Collection<DomainMapping> liveDomains = manager.getAllDNS(false, domains);
            for (DomainMapping liveDomain : liveDomains) {
                liveDomain.setMark(mark);
            }
            domains.sweep(mark);
        } catch (IOException | InterruptedException e) {
            LOG.error("Error loading domains", e);
        }
    }

    private void cleanupDNS() {
        try {
            final DomainPool domainPool = manager.getDomainPool();
            Collection<DomainMapping> domains = manager.getAllDNS(true, domainPool);

        } catch (IOException | InterruptedException e) {
            LOG.error("Error loading domains", e);
        }

    }

    private void loadIpsUnusedInitial() {
        try {
            loadIpsUnused();
            latch.countDown();
        } catch (Throwable t) {
            shutdown = true;
            System.err.println("Unable to load unused IPs, shutting down controller.");
            throw t;
        }
    }

    private void loadIpsUnused() {
        if (shutdown) {
            return;
        }
        String[] ipBits = null;
        try {
            Execute.ExecutionResult result = gcloudQuiet(true, false, "addresses", "list",
                    "--filter", "region:" + REGION + " AND status:reserved",
                    "--format", "csv[box,no-heading](name,ADDRESS)",
                    "-q");
            if (result.out.contains("Quota exceeded")) {
                checkLatency = checkLatency + 1;
                return;
            } else {
                checkLatency = Math.max(1, checkLatency - 1);
            }
            for (String ipRec : result.out.split("\\s+")) {
                if (ipRec.isEmpty()) {
                    continue;
                }
                ipBits = ipRec.split(",");
                String name = ipBits[0];
                String addr = ipBits[1];
                IpMapping ip = ips.updateOrCreate(name, addr);
                ips.addIpUnused(ip);
            }
            LOG.info("Found " + ips.getNumUnused() + " unused IP addresses");
        } catch (Exception e) {
            System.err
                    .println("Unable to load IPs! Last seen item: " + (ipBits == null ? null : Arrays.asList(ipBits)));
            e.printStackTrace();
        }
    }

    private void loadIpsUsedInitial() {
        try {
            loadIpsUsed();
            latch.countDown();
        } catch (Throwable t) {
            shutdown = true;
            reporter.recordError("Unable to load used IPs, shutting down controller.", t);
            throw t;
        }
    }

    private void loadIpsUsed() {
        if (shutdown) {
            return;
        }
        String[] ipBits = null;
        try {
            Execute.ExecutionResult result = gcloudQuiet(true, false, "addresses", "list",
                    // TODO: label our addresses so we can filter more correctly:
                    "--filter", "region:" + REGION + " AND status:in_use",
                    "--format", "csv[box,no-heading](name,ADDRESS)",
                    "-q");
            if (result.out.contains("Quota exceeded")) {
                checkLatency = checkLatency + 1;
                return;
            } else {
                checkLatency = Math.max(1, checkLatency - 1);
            }
            for (String ipRec : result.out.split("\\s+")) {
                ipBits = ipRec.split(",");
                String name = ipBits[0];
                String addr = ipBits[1];
                if (name.startsWith("dh-") || name.startsWith("perf-")) {
                    // ignore these addresses
                    continue;
                }
                IpMapping ip = ips.updateOrCreate(name, addr);
                ips.addIpUsed(ip);
            }
            LOG.info("Found " + ips.getNumUsed() + " used IP addresses");
        } catch (Exception e) {
            reporter.recordError("Unable to load IPs! Last seen item: " + (ipBits == null ? null : Arrays.asList(ipBits)), e);
        }
    }

    public boolean isReady() {
        return latch.getCount() == 0;
    }

    public boolean isLeader() {
        return leader.get();
    }

    public void waitUntilReady() {
        if (latch.getCount() <= 0) {
            return;
        }
        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            String msg = "Interrupted waiting for controller to read metadata; check for error logs!";
            reporter.recordError(msg, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(msg, e);
        }
    }

    public Machine requestMachine() {
        return requestMachine(true);
    }

    public Machine requestMachine(boolean reserve) {
        return requestMachine(reserve, true);
    }

    public Machine requestMachine(boolean reserve, boolean useExisting) {
        waitUntilReady();
        if (useExisting) {
            Optional<Machine> machine = machines.maybeGetMachine(reserve, this::isValidVersion);
            if (machine.isPresent()) {
                final Machine mach = machine.get();
                // once a machine is selected, we'll want to make sure it's turned on and update its lease.
                // however, we don't want to make the user request wait,
                // we'll offthread the startup and send user to interstitial page.
                moveToRunningState(mach, reserve);
                LOG.info("Sending user to pre-existing machine " + mach);
                return mach;
            }
        }
        String newName = NameGen.newName();
        LOG.info("Sending user to new machine " + newName);
        return requestMachine(NameGen.newName(), reserve);
    }

    public Machine requestMachine(String name, boolean reserve) {
        return requestMachine(name, reserve, true);
    }

    public Machine requestMachine(String name, boolean reserve, boolean multiDomain) {
        // reserve an IP while getting a machine going.
        final Machine machine = machines.createMachine(this, name, multiDomain);
        moveToRunningState(machine, reserve);
        return machine;
    }

    /**
     * Handles making sure a worker we believe to be runnable is actually online and correctly routed to DNS.
     *
     * @param machine The machine to turn on
     * @param reserve Whether or not to actually claim the machine, or just turn it on and let it idle.
     */
    void moveToRunningState(final Machine machine, final boolean reserve) {
        LOG.infof("Moving machine %s to a running state. Reserved? %s", machine.toStringShort(), reserve);
        if (reserve) {
            machines.expireInMillis(machine, getSessionTtl());
            machine.setInUse(true);
        } else {
            // if we aren't reserving this machine, still update the expiry, so machines will gradually rotate
            machine.keepAlive();
        }
        Execute.setTimer("Move " + machine.getHost() + " to running state", () -> {
            Execute.ExecutionResult result = null;
            IpMapping machineIp = machine.getIp();
            try {
                String purpose = machine.isController() ? PURPOSE_CONTROLLER
                        : machine.isSnapshotCreate()
                                ? machine.isController() ? PURPOSE_CREATOR_CONTROLLER : PURPOSE_CREATOR_WORKER
                                : PURPOSE_WORKER;
                // gcloud can sometimes throw errors if we try to read metadata right after machine is created
                int tries = 5;
                while (tries-- > 0) {
                    result = gcloud(true, true, "instances",
                            "describe", machine.getHost(),
                            "-q",
                            "--format", getMachineFormatFlag());
                    if (result.code == 0 || machine.isDestroyed()) {
                        break;
                    } else {
                        // TODO: detect fatal errors and just break...
                        Thread.sleep(500);
                    }
                }
                String resultString = result.out.trim();
                if (result.code != 0) {
                    // check if this was a "not found" message
                    if (resultString.contains("not found")) {
                        // yikes! the machine doesn't exist... conditionally create one w/ the specified ip address
                        // (by name, so it is stable!); only done if env var DH_ALLOW_ANY_MACHINE=true
                        if (machines.getNumberMachines() > getMaxPoolSize()) {
                            LOG.error("Refusing to create more than " + getMaxPoolSize() + " machines (currently "
                                    + machines.getNumberMachines() + ")");
                            // serious error... send slack ping!
                        } else if (isAllowUnexpectedMachines()) {
                            manager.createMachine(machine, ips);
                            result.code = 0;
                            scheduleReservationCheck();
                        }
                    }
                }
                if (result.code != 0) {
                    LOG.errorf("Unable to describe machine %s", machine.toStringShort());
                    GoogleDeploymentManager.warnResult(result);
                    return;
                }

                final Machine updated = updateMachineFromCsv(resultString);
                if (machine.isController()) {
                    LOG.warn("Instance " + machine.getHost() + " had label '" + machine.getPurpose() + "' instead of "
                            + purpose + "; forgetting this instance");
                    machines.removeMachine(machine);
                } else {
                    if (updated == null) {
                        LOG.errorf("INVALID GCLOUD RESPONSE, %s got result: %s ", machine.toStringShort(),
                                resultString);
                        // this is a serious state error, we should slack-nag in #demo
                    } else {
                        if (!updated.isOnline()) {
                            manager.turnOn(updated);
                            scheduleReservationCheck();
                        }
                        if (reserve) {
                            assert updated == machine
                                    : "Somehow got " + updated + " from pool using supplied " + machine;
                            final String expiry = toTime(machine.getExpiry());
                            manager.addLabel(machine, LABEL_LEASE, expiry, (r, e) -> {
                                if (e == null) {
                                    LOG.infof("Updated lease for machine %s label %s=%s", machine.toStringShort(),
                                            LABEL_LEASE, expiry);
                                } else {
                                    LOG.errorf(e, "Failed to renew lease for machine %s label %s=%s",
                                            machine.toStringShort(), LABEL_LEASE, expiry);
                                }
                            });
                        }
                    }
                }
                if (!machineIp.equals(machine.getIp())) {
                    LOG.warnf("Replaced IP %s with %s", machineIp, machine.getIp());
                }
            } catch (Exception e) {
                reporter.recordError("Unable to move machine " + machine + " to running state", e);
            }
            // machine (should be) alive, make sure it has DNS!
            // We check DNS with google; nobody is listening to this method,
            // so we don't care if public DNS resolves yet!
            final Execute.ExecutionResult dnsCheck = manager.checkDns(machine.getDomainName(), DNS_GOOGLE);
            if (dnsCheck.code == 0 && dnsCheck.out.trim().equals(machineIp.getIp())) {
                // machine is alive, DNS is all good.
                LOG.infof("DNS resolved for %s", machine.toStringShort());
            } else {
                if (machine.isDestroyed()) {
                    return;
                }
                // machine is not alive, yet... glue on some ad-hoc DNS
                LOG.warn("DNS does not resolve correctly for " + machine.getDomainName() + " @ " + machine.getIp()
                        + "; setting up DNS records");
                try {
                    setupDns(machine);
                    LOG.warnf("DNS setup requested for %s @ %s", machine.toStringShort(), machine.getIp());
                } catch (Exception e) {
                    reporter.recordError("Unable to setup DNS for machine " + machine, e);
                }
            }

        });
    }

    private boolean isAllowUnexpectedMachines() {
        return "true".equals(System.getenv("DH_ALLOW_ANY_MACHINE"));
    }

    private long getSessionTtl() {
        return TimeUnit.MINUTES.toMillis(45);
    }

    private void setupDns(final Machine machine) {
        final IpMapping machineIp = machine.getIp();
        if (machineIp == null || machineIp.getIp() == null || machineIp.getIp().isEmpty()) {
            final IpMapping mapping = ips.reserveIp(manager, machine);
            ensureAccessConfig(machine, mapping);
            machine.setIp(mapping);
            ips.addIpUsed(mapping);
        }
        manager.assignDns(this, Stream.of(machine));
    }

    private void ensureAccessConfig(final Machine machine, final IpMapping mapping) {
        // TODO: make sure this ip mapping has the correct access-config to the machine:
        // https://cloud.google.com/compute/docs/ip-addresses/reserve-static-external-ip-address#IP_assign

    }

    public IpMapping requestIp() {
        if (ips.getNumUnused() > 0) {
            final IpMapping unused = ips.getUnusedIp(manager);
            if (unused != null) {
                return unused;
            }
        }
        waitUntilReady();
        return ips.getUnusedIp(manager);
    }

    public void waitUntilIpsCreated() {
        manager.waitUntilIpsCreated();
    }

    public boolean isMachineReady(final String domainName) {
        String uri = "https://" + domainName + "/health";
        final Request req = new Request.Builder().get()
                .url(uri)
                .addHeader("User-Agent", "DeephavenCtrl")
                .build();
        final Response response;
        try {
            response = client.newCall(req).execute();
        } catch (IOException e) {
            return false;
        }
        boolean success = response.isSuccessful();
        response.close();
        return success;
    }

    public GoogleDeploymentManager getDeploymentManager() {
        return manager;
    }

    /**
     * Waits until a machine reports healthy by completing full initialization.
     * <p>
     * <p>
     * This method can block for a very long time, and is only used when setting up new workers/controllers.
     * <p>
     * Production workers and controls are created from pre-primed images,
     * <p>
     * So they would never call this long, slow polling method.
     * <p>
     *
     * @param machine The machine whose startup logs we should monitor to see if it online
     * @throws IOException
     * @throws InterruptedException
     */
    public void waitUntilHealthy(final Machine machine) throws IOException, InterruptedException {
        final String key = "finished code: ";
        final String failed = "ran out of tries";
        final Execute.ExecutionResult result = Execute.ssh(true, machine.getDomainName(),
                "function watch_logs() {\n" +
                        "  echo Watching log file /var/log/vm-startup.log\n" +
                        "  while ! test -f /var/log/vm-startup.log ; do sleep 1 ; done\n" +
                        "  tail -f /var/log/vm-startup.log\n" +
                        "}\n" +
                        "function wait_til_ready() {\n" +
                        "  # we sleep 1 per try, so 720 tries > 12 minutes after ssh is online\n" +
                        "  local tries=720\n" +
                        "  echo waiting for localhost:10000 to be responsive\n" +
                        "  watch_logs &\n" +
                        "  pid=$!\n" +
                        "  while (( tries > 0 )) && \n" +
                        "    ! grep -q InitialDeephavenSetupComplete /var/log/vm-startup.log 2> /dev/null; do\n" +
                        "    tries=$((tries-1))\n" +
                        "    (( tries%10 )) || echo \"start-monitor tries remaining: $tries\"\n" +
                        "    sleep 1\n" +
                        "  done\n" +
                        // kill the watch_logs function, if we got here, we don't need more logs
                        "  kill $pid\n" +
                        "  if (( tries > 0 )); then\n" +
                        "    if curl -k https://localhost:10000/health &> /dev/null; then\n" +
                        "        echo \"localhost:10000 is responsive; $(hostname) is alive!\"\n" +
                        "    else\n" +
                        // the startup script does not block until the server comes online.
                        // For now, we're going to ignore "server not responding yet",
                        // but we should probably poll a few 10s of seconds for server to come up
                        // so we can consider "server not working" a failure case.
                        "        echo \"localhost:10000 is not-responsive on $(hostname)\"\n" +
                        "    fi\n" +
                        "  else\n" +
                        "    echo Tried 720 times to reach https://localhost:10000/health but " + failed + "\n" +
                        "  fi\n" +
                        "} ; TIMEFORMAT='\n" +
                        "wait_til_ready exited after: %3Rs\n' ; time wait_til_ready ; code=$? ; " +
                        "sleep 1 ; echo '\n'" + key + "${code}'\n' ; echo ; sleep 1 ; kill $PPID ");
        int realCodeLoc = result.out.lastIndexOf(key);
        if (result.out.contains(failed)) {
            throw new IllegalStateException("wait_til_ready failed:\n\n" + result.out);
        }
        String code =
                result.out.substring(realCodeLoc + key.length(), result.out.indexOf('\n', realCodeLoc + key.length()));
        if (!"0".equals(code)) {
            throw new IllegalStateException("wait_til_ready returned code " + code);
        }
    }

    public Machine findMachine(final String name, final boolean newIfMissing, final boolean noStableIp) {
        final Machine machine = machines.findByName(name);
        if (machine == null) {
            if (newIfMissing) {
                final IpMapping ip;
                if (noStableIp) {
                    ip = ips.updateOrCreate(name, null);
                    ip.setState(IpState.Unclaimed);
                } else {
                    ip = null;
                }
                final Machine created = machines.getOrCreate(name, this, ip, null);
                created.setNoStableIP(noStableIp);
                return created;
            }
        }
        return machine;
    }

    public boolean renewLease(final String domainName) {
        final Optional<Machine> machine = machines.findByDomainName(domainName);
        if (!machine.isPresent()) {
            return false;
        }
        machines.expireInMillis(machine.get(), getSessionTtl());
        return true;
    }

    public MachinePool getMachinePool() {
        return machines;
    }

    public DhDemoReporter getReporter() {
        return reporter;
    }
}
