package com.majesttyx.mcacapitals.util;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Memories;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class MCASocialBridge {

    private MCASocialBridge() {
    }

    static boolean adjustHearts(
            ServerLevel level,
            UUID villagerId,
            UUID playerId,
            int delta
    ) {
        if (level == null
                || villagerId == null
                || playerId == null
                || delta == 0) {
            return false;
        }

        Entity entity =
                MCAEntityBridge.getEntityByUuid(
                        level,
                        villagerId
                );

        if (!(entity instanceof VillagerEntityMCA villager)) {
            return false;
        }

        if (level.getServer() == null) {
            return false;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(playerId);

        if (player == null) {
            return false;
        }

        try {
            Memories memories =
                    villager.getVillagerBrain()
                            .getMemoriesForPlayer(
                                    player
                            );

            if (memories == null) {
                return false;
            }

            memories.modHearts(delta);

            return true;
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCASocialBridge#adjustHearts:7.7.32",
                    "Failed to adjust MCA 7.7.32 villager hearts for villager={} player={} delta={} ({})",
                    villagerId,
                    playerId,
                    delta,
                    t.toString()
            );

            return false;
        }
    }

    static boolean stopInteracting(
            Entity villagerEntity
    ) {
        if (villagerEntity == null) {
            return false;
        }

        try {
            Object interactions =
                    MCAReflectionHelper.invoke(
                            villagerEntity,
                            "getInteractions"
                    );

            if (interactions == null) {
                return false;
            }

            Object result =
                    MCAReflectionHelper.invoke(
                            interactions,
                            "stopInteracting"
                    );

            return result == null
                    || Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }
}