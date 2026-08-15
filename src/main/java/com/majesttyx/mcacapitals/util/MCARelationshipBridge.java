package com.majesttyx.mcacapitals.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

    public static BetrothalResult promiseVillagerToVillagerByDecree(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.promiseVillagerToVillagerByDecree(firstVillager, secondVillager);
    }

    public static BetrothalResult validatePendingVillagerBetrothal(ServerLevel level, Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.validatePendingVillagerBetrothal(level, firstVillager, secondVillager);
    }

    public static boolean areVillagersBetrothedToEachOther(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.areVillagersBetrothedToEachOther(firstVillager, secondVillager);
    }

    public static BetrothalResult marryVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.marryVillagerToVillager(firstVillager, secondVillager);
    }

    public static BetrothalResult marryVillagerToVillagerDirect(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipOps.marryVillagerToVillagerDirect(firstVillager, secondVillager);
    }

    public static boolean isActuallyMarried(ServerPlayer player, Entity villagerEntity) {
        return MCARelationshipOps.isActuallyMarried(player, villagerEntity);
    }

    public static boolean isActuallyMarriedToPlayer(ServerPlayer player, UUID villagerId) {
        return MCARelationshipOps.isActuallyMarriedToPlayer(player, villagerId);
    }

    public record BetrothalResult(boolean success, Component message) {
        public static BetrothalResult ok() {
            return new BetrothalResult(true, Component.empty());
        }

        public static BetrothalResult failure(Component message) {
            return new BetrothalResult(false, message == null ? Component.empty() : message);
        }
    }
}