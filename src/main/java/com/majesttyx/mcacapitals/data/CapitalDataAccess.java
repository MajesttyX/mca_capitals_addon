package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CapitalDataAccess {

    private static final ThreadLocal<DirtyBatchState> DIRTY_BATCH = new ThreadLocal<>();

    private CapitalDataAccess() {
    }

    public static CapitalSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(CapitalSavedData::load, CapitalSavedData::new, CapitalSavedData.DATA_NAME);
    }

    public static DirtyBatch batch(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return DirtyBatch.NO_OP;
        }

        MinecraftServer server = level.getServer();
        DirtyBatchState state = DIRTY_BATCH.get();
        if (state == null) {
            state = new DirtyBatchState(server);
            DIRTY_BATCH.set(state);
        } else if (state.server != server) {
            throw new IllegalStateException("Capital persistence batches cannot span multiple servers on one thread");
        }

        state.depth++;
        return new DirtyBatch(server);
    }

    public static void markDirty(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return;
        }

        DirtyBatchState state = DIRTY_BATCH.get();
        if (state != null && state.server == level.getServer() && state.depth > 0) {
            state.dirty = true;
            return;
        }

        flushDirty(level);
    }

    private static void flushDirty(ServerLevel level) {
        CapitalSavedData data = get(level);
        synchronizeCapitalCollection(data);
        data.setDirty();
    }

    private static void synchronizeCapitalCollection(CapitalSavedData data) {
        List<CapitalRecord> persisted = data.getCapitals();
        Collection<CapitalRecord> runtime = CapitalManager.getAllCapitalRecords();

        if (sameCapitalInstancesInOrder(persisted, runtime)) {
            return;
        }

        persisted.clear();
        for (CapitalRecord capital : runtime) {
            if (capital != null) {
                persisted.add(capital);
            }
        }
    }

    private static boolean sameCapitalInstancesInOrder(
            List<CapitalRecord> persisted,
            Collection<CapitalRecord> runtime
    ) {
        if (persisted.size() != runtime.size()) {
            return false;
        }

        Iterator<CapitalRecord> persistedIterator = persisted.iterator();
        Iterator<CapitalRecord> runtimeIterator = runtime.iterator();
        while (persistedIterator.hasNext() && runtimeIterator.hasNext()) {
            if (persistedIterator.next() != runtimeIterator.next()) {
                return false;
            }
        }

        return !persistedIterator.hasNext() && !runtimeIterator.hasNext();
    }

    private static void finishBatch(MinecraftServer server) {
        DirtyBatchState state = DIRTY_BATCH.get();
        if (state == null || state.server != server || state.depth <= 0) {
            return;
        }

        state.depth--;
        if (state.depth > 0) {
            return;
        }

        boolean dirty = state.dirty;
        DIRTY_BATCH.remove();

        if (dirty) {
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                flushDirty(overworld);
            }
        }
    }

    private static final class DirtyBatchState {
        private final MinecraftServer server;
        private int depth;
        private boolean dirty;

        private DirtyBatchState(MinecraftServer server) {
            this.server = server;
        }
    }

    public static final class DirtyBatch implements AutoCloseable {
        private static final DirtyBatch NO_OP = new DirtyBatch(null);

        private final MinecraftServer server;
        private boolean closed;

        private DirtyBatch(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public void close() {
            if (closed || server == null) {
                return;
            }
            closed = true;
            finishBatch(server);
        }
    }
}
