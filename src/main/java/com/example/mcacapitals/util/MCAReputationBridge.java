package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class MCAReputationBridge {

    private MCAReputationBridge() {
    }

    public static int getHeartsWithVillager(ServerLevel level, UUID villagerId, UUID playerId) {
        return MCAIntegrationBridge.getHeartsWithPlayer(level, villagerId, playerId);
    }

    public static int getCapitalHeartsScore(ServerLevel level, Set<UUID> residentIds, UUID playerId) {
        int total = 0;

        for (UUID residentId : residentIds) {
            total += getHeartsWithVillager(level, residentId, playerId);
        }

        return total;
    }

    public static UUID findBestFounder(ServerLevel level, Set<UUID> residentIds, int minimumHearts) {
        return level.players().stream()
                .map(ServerPlayer::getUUID)
                .filter(playerId -> getCapitalHeartsScore(level, residentIds, playerId) >= minimumHearts)
                .max(Comparator.comparingInt(playerId -> getCapitalHeartsScore(level, residentIds, playerId)))
                .orElse(null);
    }

    public static boolean canClaimThrone(ServerLevel level, Set<UUID> residentIds, UUID playerId, int requiredHearts) {
        return getCapitalHeartsScore(level, residentIds, playerId) >= requiredHearts;
    }
}