package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class MCARelationshipBridge {

    private MCARelationshipBridge() {
    }

    public static BetrothalResult promise(ServerPlayer player, Entity villagerEntity) {
        return MCARelationshipOps.promise(player, villagerEntity);
    }

    public static BetrothalResult promiseVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.promiseVillagerToVillager(firstVillager, secondVillager);
    }

    public static boolean areVillagersBetrothedToEachOther(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.areVillagersBetrothedToEachOther(firstVillager, secondVillager);
    }

    public static BetrothalResult marryVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.marryVillagerToVillager(firstVillager, secondVillager);
    }

    public static boolean isActuallyMarried(ServerPlayer player, Entity villagerEntity) {
        return MCARelationshipOps.isActuallyMarried(player, villagerEntity);
    }

    public static boolean isActuallyMarriedToPlayer(ServerPlayer player, UUID villagerId) {
        return MCARelationshipOps.isActuallyMarriedToPlayer(player, villagerId);
    }

    public record BetrothalResult(boolean success, String message) {
        public static BetrothalResult ok() {
            return new BetrothalResult(true, "");
        }

        public static BetrothalResult failure(String message) {
            return new BetrothalResult(false, message);
        }
    }
}