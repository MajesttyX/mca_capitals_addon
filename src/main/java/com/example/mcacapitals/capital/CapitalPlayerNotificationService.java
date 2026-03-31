package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class CapitalPlayerNotificationService {

    private static final double CAPITAL_NOTIFICATION_RADIUS_SQR = 96.0D * 96.0D;

    private CapitalPlayerNotificationService() {
    }

    public static void notifyPlayersInCapital(ServerLevel level, CapitalRecord capital, Component message) {
        if (level == null || capital == null || capital.getVillageId() == null || message == null) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            if (isPlayerWithinCapital(level, capital, player)) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static boolean isPlayerWithinCapital(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        if (level == null || capital == null || capital.getVillageId() == null || player == null) {
            return false;
        }

        Optional<Integer> lastSeenVillageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (lastSeenVillageId.isPresent()) {
            return lastSeenVillageId.get().equals(capital.getVillageId());
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        return player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D)
                <= CAPITAL_NOTIFICATION_RADIUS_SQR;
    }
}