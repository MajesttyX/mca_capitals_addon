package com.majesttyx.mcacapitals.event;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalDeathTransitionService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public final class CapitalDeathEvents {

    private CapitalDeathEvents() {
    }

    public static void onLivingDeath(
            LivingEntity entity,
            DamageSource source
    ) {
        if (entity == null
                || !(entity.level()
                instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!MCAIntegrationBridge
                .isMCAVillagerEntity(
                        entity
                )) {
            return;
        }

        recordExecutionIfNeeded(
                serverLevel,
                entity,
                source
        );

        CapitalDeathTransitionService
                .handleVillagerDeath(
                        serverLevel,
                        entity
                );
    }

    private static void recordExecutionIfNeeded(
            ServerLevel level,
            LivingEntity villager,
            DamageSource source
    ) {
        if (level == null
                || villager == null
                || !MCAExecutionBridge
                .isMarkedForExecution(
                        level,
                        villager.getUUID()
                )) {
            return;
        }

        CapitalRecord capital =
                resolveCapital(
                        level,
                        villager.getUUID()
                );

        if (capital == null
                || capital.getState()
                != CapitalState.ACTIVE) {
            return;
        }

        String villagerName =
                CapitalNameService.resolveDisplayName(
                        level,
                        capital,
                        villager.getUUID()
                );

        String executionerName =
                resolveExecutionerName(
                        source
                );

        if (executionerName == null
                || executionerName.isBlank()) {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    villagerName
                            + " was executed after being condemned by the Crown."
            );

            return;
        }

        CapitalChronicleService.addEntry(
                level,
                capital,
                villagerName
                        + " was executed by "
                        + executionerName
                        + " after being condemned by the Crown."
        );
    }

    private static CapitalRecord resolveCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.containsEntity(
                    villagerId
            )) {
                return capital;
            }
        }

        Integer villageId =
                MCAIntegrationBridge
                        .getVillageIdForResident(
                                level,
                                villagerId
                        );

        return CapitalManager
                .getCapitalByVillageId(
                        villageId
                );
    }

    private static String resolveExecutionerName(
            DamageSource source
    ) {
        if (source == null
                || source.getEntity() == null) {
            return null;
        }

        return source.getEntity()
                .getName()
                .getString();
    }
}