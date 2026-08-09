package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class CapitalExecutionAuthorityService {

    private CapitalExecutionAuthorityService() {
    }

    public static boolean mayIssueDirectExecution(
            ServerPlayer player,
            Entity target
    ) {
        if (player == null
                || target == null
                || !target.isAlive()
                || target.isRemoved()
                || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return false;
        }

        ServerLevel level = player.serverLevel();

        if (target.level() != level) {
            return false;
        }
        CapitalRecord capital = resolveTargetCapital(
                level,
                target.getUUID()
        );

        return capital != null
                && capital.getState() == CapitalState.ACTIVE
                && capital.getPlayerSovereignId() != null
                && capital.getPlayerSovereignId().equals(
                player.getUUID()
        );
    }

    public static CapitalRecord resolveTargetCapital(
            ServerLevel level,
            UUID targetId
    ) {
        if (level == null || targetId == null) {
            return null;
        }

        CapitalRecord capital =
                CapitalTitleResolver.findCapitalForEntity(
                        level,
                        targetId
                );

        if (capital != null) {
            return capital;
        }
        Integer villageId =
                MCAIntegrationBridge.getVillageIdForResident(
                        level,
                        targetId
                );

        return CapitalManager.getCapitalByVillageId(
                villageId
        );
    }
}
