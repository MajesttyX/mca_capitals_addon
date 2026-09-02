package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records confirmed player death events for the lifetime of the current server.
 * MCA's family tree is persistent identity data and does not reliably represent a
 * player's transient death state, so Hardcore succession must use the Forge death
 * event as the authoritative signal.
 */
public final class CapitalHardcoreDeathTracker {

    private static final Set<UUID> DEAD_PLAYERS = ConcurrentHashMap.newKeySet();

    private CapitalHardcoreDeathTracker() {
    }

    public static void record(ServerLevel level, UUID playerId) {
        if (level == null
                || playerId == null
                || level.getServer() == null
                || !level.getServer().isHardcore()) {
            return;
        }
        DEAD_PLAYERS.add(playerId);
    }

    public static boolean isConfirmedDead(ServerLevel level, UUID playerId) {
        return level != null
                && playerId != null
                && level.getServer() != null
                && level.getServer().isHardcore()
                && DEAD_PLAYERS.contains(playerId);
    }

    public static void clearAll() {
        DEAD_PLAYERS.clear();
    }
}
