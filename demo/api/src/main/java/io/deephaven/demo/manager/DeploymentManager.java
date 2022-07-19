package io.deephaven.demo.manager;

import io.deephaven.demo.api.ClusterMap;
import io.deephaven.demo.api.IpMapping;
import io.deephaven.demo.api.DomainPool;
import io.deephaven.demo.api.IpPool;
import io.deephaven.demo.api.Machine;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * A DeploymentManager is responsible for creating and interacting with real VMs.
 * <p>
 * We are using this abstraction layer to push ALL google-cloud-specific code into a single box,
 * so that it can be replaced with alternate implementations when (and if) a customer ever uses this code.
 * <p>
 * Note: there is still some direct usage of Execute.gcloud methods that need to be hidden behind this interface.
 */
public interface DeploymentManager {

    DomainPool getDomainPool();
    IpPool getIpPool();
    String getBaseImageName();

    void assignDns(final IClusterController ctrl, Stream<Machine> nodes) throws IOException, InterruptedException, TimeoutException;
    void createMachine(Machine node, final IpPool ips) throws IOException, InterruptedException;
    default void destroyCluster(Machine machine) throws IOException {
        destroyCluster(Collections.singleton(machine), "");
    }
    void destroyCluster(Collection<Machine> allNodes, String diskPrefix) throws IOException;
    boolean turnOn(Machine node) throws IOException, InterruptedException;
    boolean turnOff(Machine worker) throws IOException, InterruptedException;

    void createSnapshot(String snapshotName, ClusterMap map, boolean forceCreate, String prefix) throws IOException, InterruptedException;
    void restoreSnapshot(String snapshotName, ClusterMap map, boolean restart, String prefix);
    Collection<String> findMissingSnapshots(String snapshotName, ClusterMap map);
    void waitForSsh(Machine node);

    Collection<IpMapping> requestNewIps(int i);

    void waitUntilIpsCreated();

    Logger getLog();

    default void addLabel(Machine mach, String name, String value) {
        addLabel(mach, name, value, (r, e) -> {
            if (e == null) {
                getLog().infof("Set machine %s label %s=%s", mach.toStringShort(), name, value);
            } else {
                getLog().warnf(e, "Unable to update %s label %s=%s", mach.toStringShort(), name, value);
            }

        });
    }
    default void removeLabel(Machine mach, String name) {
        removeLabel(mach, name, (r, e) -> {
            if (e == null) {
                getLog().infof("Cleared machine %s label %s", mach.toStringShort(), name);
            } else {
                getLog().warnf(e, "Unable to update %s label %s", mach.toStringShort(), name);
            }

        });
    }
    void addLabel(Machine mach, String name, String value, BiConsumer<Execute.ExecutionResult, Throwable> failMsg);

    void removeLabel(Machine mach, String name, BiConsumer<Execute.ExecutionResult, Throwable> failMsg);

    Execute.ExecutionResult deleteMachine(String hostName) throws IOException, InterruptedException;

    void checkIpState(IpPool ipPool, io.deephaven.demo.api.IpMapping ip);

    IDnsManager getDns();

    boolean hasMachine(String workerName) throws IOException, InterruptedException;

    String guessRealIp(String name);

    IpMapping findIp(String ipAddr);

    Map<String, int[]> getReservations();

    void removeReservations(String name);

    void reserveMachines(final Collection<String> existing, int reservationSize);
}

