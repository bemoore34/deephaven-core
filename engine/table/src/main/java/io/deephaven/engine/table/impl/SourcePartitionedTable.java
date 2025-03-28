//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.engine.table.impl;

import io.deephaven.base.verify.Assert;
import io.deephaven.engine.liveness.*;
import io.deephaven.engine.primitive.iterator.CloseableIterator;
import io.deephaven.engine.rowset.*;
import io.deephaven.engine.table.*;
import io.deephaven.engine.table.impl.locations.*;
import io.deephaven.engine.table.impl.locations.impl.SingleTableLocationProvider;
import io.deephaven.engine.table.impl.locations.impl.TableLocationSubscriptionBuffer;
import io.deephaven.engine.table.impl.locations.impl.TableLocationUpdateSubscriptionBuffer;
import io.deephaven.engine.table.impl.partitioned.PartitionedTableImpl;
import io.deephaven.engine.table.impl.sources.ArrayBackedColumnSource;
import io.deephaven.engine.table.impl.sources.regioned.RegionedTableComponentFactoryImpl;
import io.deephaven.engine.table.iterators.ChunkedObjectColumnIterator;
import io.deephaven.engine.updategraph.NotificationQueue;
import io.deephaven.engine.updategraph.UpdateCommitter;
import io.deephaven.engine.updategraph.UpdateGraph;
import io.deephaven.engine.updategraph.UpdateSourceCombiner;
import io.deephaven.util.datastructures.linked.IntrusiveDoublyLinkedNode;
import io.deephaven.util.datastructures.linked.IntrusiveDoublyLinkedQueue;
import io.deephaven.util.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link PartitionedTable} of single-location {@link SourceTable}s keyed by {@link TableLocationKey}. Refer to
 * {@link TableLocationKey} for an explanation of partitioning.
 */
public class SourcePartitionedTable extends PartitionedTableImpl {

    private static final String KEY_COLUMN_NAME = "TableLocationKey";
    private static final String CONSTITUENT_COLUMN_NAME = "LocationTable";

    /**
     * Construct a {@link SourcePartitionedTable} from the supplied parameters.
     * <p>
     * Note that refreshLocations and refreshSizes are distinct because there are use cases that supply an external
     * RowSet and hence don't require size refreshes. Others might care for size refreshes, but only the
     * initially-available set of locations.
     *
     * @param constituentDefinition The {@link TableDefinition} expected of constituent {@link Table tables}
     * @param applyTablePermissions Function to apply in order to correctly restrict the visible result rows
     * @param tableLocationProvider Source for table locations
     * @param refreshLocations Whether the set of locations should be refreshed
     * @param refreshSizes Whether the locations found should be refreshed
     * @param locationKeyMatcher Function to filter desired location keys
     */
    public SourcePartitionedTable(
            @NotNull final TableDefinition constituentDefinition,
            @NotNull final UnaryOperator<Table> applyTablePermissions,
            @NotNull final TableLocationProvider tableLocationProvider,
            final boolean refreshLocations,
            final boolean refreshSizes,
            @NotNull final Predicate<ImmutableTableLocationKey> locationKeyMatcher) {
        super(new UnderlyingTableMaintainer(
                constituentDefinition,
                applyTablePermissions,
                tableLocationProvider,
                refreshLocations,
                refreshSizes,
                locationKeyMatcher).result(),
                Set.of(KEY_COLUMN_NAME),
                true,
                CONSTITUENT_COLUMN_NAME,
                constituentDefinition,
                refreshLocations,
                false);
    }

    private static final class UnderlyingTableMaintainer extends ReferenceCountedLivenessNode
            implements NotificationQueue.Dependency {

        private final TableDefinition constituentDefinition;
        private final UnaryOperator<Table> applyTablePermissions;
        private final TableLocationProvider tableLocationProvider;
        private final boolean refreshSizes;
        private final Predicate<ImmutableTableLocationKey> locationKeyMatcher;

        private final TrackingWritableRowSet resultRows;
        private final WritableColumnSource<TableLocationKey> resultTableLocationKeys;
        private final WritableColumnSource<Table> resultLocationTables;
        private final QueryTable result;

        private final UpdateSourceCombiner refreshCombiner;
        private final TableLocationSubscriptionBuffer subscriptionBuffer;
        private final IntrusiveDoublyLinkedQueue<PendingLocationState> pendingLocationStates;
        private final IntrusiveDoublyLinkedQueue<PendingLocationState> readyLocationStates;
        @SuppressWarnings("FieldCanBeLocal") // We need to hold onto this reference for reachability purposes.
        private final Runnable processNewLocationsUpdateRoot;

        private final UpdateCommitter<UnderlyingTableMaintainer> removedLocationsCommitter;
        private List<Table> removedConstituents = null;

        private UnderlyingTableMaintainer(
                @NotNull final TableDefinition constituentDefinition,
                @NotNull final UnaryOperator<Table> applyTablePermissions,
                @NotNull final TableLocationProvider tableLocationProvider,
                final boolean refreshLocations,
                final boolean refreshSizes,
                @NotNull final Predicate<ImmutableTableLocationKey> locationKeyMatcher) {
            super(false);

            this.constituentDefinition = constituentDefinition;
            this.applyTablePermissions = applyTablePermissions;
            this.tableLocationProvider = tableLocationProvider;
            this.refreshSizes = refreshSizes;
            this.locationKeyMatcher = locationKeyMatcher;

            resultRows = RowSetFactory.empty().toTracking();
            resultTableLocationKeys = ArrayBackedColumnSource.getMemoryColumnSource(TableLocationKey.class, null);
            resultLocationTables = ArrayBackedColumnSource.getMemoryColumnSource(Table.class, null);

            final Map<String, ColumnSource<?>> resultSources = new LinkedHashMap<>(2);
            resultSources.put(KEY_COLUMN_NAME, resultTableLocationKeys);
            resultSources.put(CONSTITUENT_COLUMN_NAME, resultLocationTables);
            result = new QueryTable(resultRows, resultSources);

            final boolean needToRefreshLocations = refreshLocations && tableLocationProvider.supportsSubscriptions();
            if (needToRefreshLocations || refreshSizes) {
                result.setRefreshing(true);
                refreshCombiner = new UpdateSourceCombiner(result.getUpdateGraph());
                result.addParentReference(this);
                manage(refreshCombiner);
            } else {
                refreshCombiner = null;
            }

            if (needToRefreshLocations) {
                resultTableLocationKeys.startTrackingPrevValues();
                resultLocationTables.startTrackingPrevValues();

                subscriptionBuffer = new TableLocationSubscriptionBuffer(tableLocationProvider);
                manage(subscriptionBuffer);

                pendingLocationStates = new IntrusiveDoublyLinkedQueue<>(
                        IntrusiveDoublyLinkedNode.Adapter.<PendingLocationState>getInstance());
                readyLocationStates = new IntrusiveDoublyLinkedQueue<>(
                        IntrusiveDoublyLinkedNode.Adapter.<PendingLocationState>getInstance());
                processNewLocationsUpdateRoot = new InstrumentedTableUpdateSource(
                        result,
                        SourcePartitionedTable.class.getSimpleName() + '[' + tableLocationProvider + ']'
                                + "-processPendingLocations") {
                    @Override
                    protected void instrumentedRefresh() {
                        processPendingLocations(true);
                    }
                };
                refreshCombiner.addSource(processNewLocationsUpdateRoot);

                this.removedLocationsCommitter = new UpdateCommitter<>(
                        this,
                        result.getUpdateGraph(),
                        ignored -> {
                            Assert.neqNull(removedConstituents, "removedConstituents");
                            result.unmanage(removedConstituents.stream());
                            removedConstituents = null;
                        });
                processPendingLocations(false);
            } else {
                subscriptionBuffer = null;
                pendingLocationStates = null;
                readyLocationStates = null;
                processNewLocationsUpdateRoot = null;
                removedLocationsCommitter = null;
                tableLocationProvider.refresh();

                final Collection<TableLocation> locations = new ArrayList<>();
                try {
                    retainReference();
                    tableLocationProvider.getTableLocationKeys(
                            (final LiveSupplier<ImmutableTableLocationKey> lstlk) -> {
                                final TableLocation tableLocation = tableLocationProvider.getTableLocation(lstlk.get());
                                manage(tableLocation);
                                locations.add(tableLocation);
                            },
                            locationKeyMatcher);
                    try (final RowSet added = sortAndAddLocations(locations.stream())) {
                        resultRows.insert(added);
                    }
                } finally {
                    dropReference();
                }
            }

            if (refreshCombiner != null) {
                refreshCombiner.install();
            }
        }

        private QueryTable result() {
            return result;
        }

        private RowSet sortAndAddLocations(@NotNull final Stream<TableLocation> locations) {
            final long initialLastRowKey = resultRows.lastRowKey();
            final MutableLong lastInsertedRowKey = new MutableLong(initialLastRowKey);
            // Note that makeConstituentTable expects us to subsequently unmanage the TableLocations
            unmanage(locations.sorted(Comparator.comparing(TableLocation::getKey)).peek(tl -> {
                final long constituentRowKey = lastInsertedRowKey.incrementAndGet();
                final Table constituentTable = makeConstituentTable(tl);

                resultTableLocationKeys.ensureCapacity(constituentRowKey + 1);
                resultTableLocationKeys.set(constituentRowKey, tl.getKey());

                resultLocationTables.ensureCapacity(constituentRowKey + 1);
                resultLocationTables.set(constituentRowKey, constituentTable);

                if (result.isRefreshing()) {
                    result.manage(constituentTable);
                }
            }));
            return initialLastRowKey == lastInsertedRowKey.get()
                    ? RowSetFactory.empty()
                    : RowSetFactory.fromRange(initialLastRowKey + 1, lastInsertedRowKey.get());
        }

        private Table makeConstituentTable(@NotNull final TableLocation tableLocation) {
            final PartitionAwareSourceTable constituent = new PartitionAwareSourceTable(
                    constituentDefinition,
                    "SingleLocationSourceTable-" + tableLocation,
                    RegionedTableComponentFactoryImpl.INSTANCE,
                    new SingleTableLocationProvider(tableLocation, refreshSizes
                            ? tableLocationProvider.getLocationUpdateMode()
                            : TableUpdateMode.STATIC),
                    refreshSizes ? refreshCombiner : null);

            // Transfer management to the constituent CSM. NOTE: this is likely to end up double-managed
            // after the CSM adds the location to the table, but that's acceptable.
            constituent.columnSourceManager.manage(tableLocation);
            // Note that the caller is now responsible for unmanaging tableLocation on behalf of this.

            // Be careful to propagate the systemic attribute properly to child tables
            constituent.setAttribute(Table.SYSTEMIC_TABLE_ATTRIBUTE, result.isSystemicObject());
            return applyTablePermissions.apply(constituent);
        }

        private void processPendingLocations(final boolean notifyListeners) {
            final RowSet removed;
            final RowSet added;

            try (final TableLocationSubscriptionBuffer.LocationUpdate locationUpdate =
                    subscriptionBuffer.processPending()) {
                if (locationUpdate == null) {
                    removed = null;
                } else {
                    removed = processRemovals(locationUpdate);
                    processAdditions(locationUpdate);
                }
                added = checkPendingLocations();
            }

            if (removed == null) {
                if (added == null) {
                    return;
                }
                resultRows.insert(added);
            } else if (added == null) {
                resultRows.remove(removed);
            } else {
                resultRows.update(added, removed);
            }
            if (notifyListeners) {
                result.notifyListeners(new TableUpdateImpl(
                        added == null ? RowSetFactory.empty() : added,
                        removed == null ? RowSetFactory.empty() : removed,
                        RowSetFactory.empty(),
                        RowSetShiftData.EMPTY,
                        ModifiedColumnSet.EMPTY));
            } else {
                if (added != null) {
                    added.close();
                }
                if (removed != null) {
                    removed.close();
                }
            }
        }

        private void processAdditions(final TableLocationSubscriptionBuffer.LocationUpdate locationUpdate) {
            /*
             * This block of code is unfortunate, because it largely duplicates the intent and effort of similar code in
             * RegionedColumnSourceManager. I think that the RegionedColumnSourceManager could be changed to
             * intermediate between TableLocationProvider and SourceTable or SourcePartitionedTable, allowing for much
             * cleaner code in all three. The RCSM could then populate STM nodes or ST regions. We could also add a
             * "RegionManager" to RegionedColumnSources, in order to eliminate the unnecessary post-initialization array
             * population in STM ColumnSources.
             */
            // TODO (https://github.com/deephaven/deephaven-core/issues/867): Refactor around a ticking partition table
            if (locationUpdate != null) {
                locationUpdate.getPendingAddedLocationKeys().stream()
                        .map(LiveSupplier::get)
                        .filter(locationKeyMatcher)
                        .map(tableLocationProvider::getTableLocation)
                        .peek(this::manage)
                        .map(PendingLocationState::new)
                        .forEach(pendingLocationStates::offer);
            }
        }

        private RowSet checkPendingLocations() {
            for (final Iterator<PendingLocationState> iter = pendingLocationStates.iterator(); iter.hasNext();) {
                final PendingLocationState pendingLocationState = iter.next();
                if (pendingLocationState.exists()) {
                    iter.remove();
                    readyLocationStates.offer(pendingLocationState);
                }
            }

            if (readyLocationStates.isEmpty()) {
                return null;
            }

            final RowSet added = sortAndAddLocations(readyLocationStates.stream().map(PendingLocationState::release));
            readyLocationStates.clearFast();
            return added;
        }

        private RowSet processRemovals(final TableLocationSubscriptionBuffer.LocationUpdate locationUpdate) {
            final Set<ImmutableTableLocationKey> relevantRemovedLocations =
                    locationUpdate.getPendingRemovedLocationKeys()
                            .stream()
                            .map(LiveSupplier::get)
                            .filter(locationKeyMatcher)
                            .collect(Collectors.toSet());

            if (relevantRemovedLocations.isEmpty()) {
                return RowSetFactory.empty();
            }

            // Iterate through the pending locations and remove any that are in the removed set.
            List<LivenessReferent> toUnmanage = null;
            for (final Iterator<PendingLocationState> iter = pendingLocationStates.iterator(); iter.hasNext();) {
                final PendingLocationState pendingLocationState = iter.next();
                if (relevantRemovedLocations.contains(pendingLocationState.location.getKey())) {
                    iter.remove();
                    // Release the state and plan to unmanage the location
                    if (toUnmanage == null) {
                        toUnmanage = new ArrayList<>();
                    }
                    toUnmanage.add(pendingLocationState.release());
                }
            }
            if (toUnmanage != null) {
                unmanage(toUnmanage.stream());
                // noinspection UnusedAssignment
                toUnmanage = null;
            }

            // At the end of the cycle we need to make sure we unmanage any removed constituents.
            this.removedConstituents = new ArrayList<>(relevantRemovedLocations.size());
            final RowSetBuilderSequential deleteBuilder = RowSetFactory.builderSequential();

            // We don't have a map of location key to row key, so we have to iterate them. If we decide this is too
            // slow, we could add a TObjectIntMap as we process pending added locations and then we can just make an
            // RowSet of rows to remove by looking up in that map.
            // @formatter:off
            try (final CloseableIterator<ImmutableTableLocationKey> keysIterator =
                         ChunkedObjectColumnIterator.make(resultTableLocationKeys, resultRows);
                 final CloseableIterator<Table> constituentsIterator =
                         ChunkedObjectColumnIterator.make(resultLocationTables, resultRows);
                 final RowSet.Iterator rowsIterator = resultRows.iterator()) {
                // @formatter:on
                while (keysIterator.hasNext()) {
                    final TableLocationKey key = keysIterator.next();
                    final Table constituent = constituentsIterator.next();
                    final long rowKey = rowsIterator.nextLong();
                    if (relevantRemovedLocations.contains(key)) {
                        deleteBuilder.appendKey(rowKey);
                        removedConstituents.add(constituent);
                    }
                }
            }

            if (removedConstituents.isEmpty()) {
                removedConstituents = null;
                return RowSetFactory.empty();
            }
            this.removedLocationsCommitter.maybeActivate();

            final WritableRowSet deletedRows = deleteBuilder.build();
            resultTableLocationKeys.setNull(deletedRows);
            resultLocationTables.setNull(deletedRows);
            return deletedRows;
        }

        @Override
        public boolean satisfied(final long step) {
            if (refreshCombiner == null) {
                throw new UnsupportedOperationException("This method should not be called when result is static");
            }
            return refreshCombiner.satisfied(step);
        }

        @Override
        public UpdateGraph getUpdateGraph() {
            if (refreshCombiner == null) {
                throw new UnsupportedOperationException("This method should not be called when result is static");
            }
            return refreshCombiner.getUpdateGraph();
        }
    }

    private static final class PendingLocationState extends IntrusiveDoublyLinkedNode.Impl<PendingLocationState> {

        private final TableLocation location;
        private final TableLocationUpdateSubscriptionBuffer subscriptionBuffer;

        private PendingLocationState(@NotNull final TableLocation location) {
            this.location = location;
            subscriptionBuffer = new TableLocationUpdateSubscriptionBuffer(location);
        }

        /**
         * Test if the pending location is ready for inclusion in the result table. This means it must have non-null,
         * non-zero size. We expect that this means the location will be immediately included in the resulting table's
         * {@link ColumnSourceManager}, which is a
         * {@link io.deephaven.engine.table.impl.sources.regioned.RegionedColumnSourceManager} in all cases.
         *
         * @return Whether this location exists for purposes of inclusion in the result table
         */
        private boolean exists() {
            subscriptionBuffer.processPending();
            final long localSize = location.getSize();
            // noinspection ConditionCoveredByFurtherCondition
            return localSize != TableLocationState.NULL_SIZE && localSize > 0;
        }

        /**
         * Get rid of the underlying subscription in this state, and return the location.
         *
         * @return The location
         */
        private TableLocation release() {
            subscriptionBuffer.reset();
            return location;
        }
    }
}
