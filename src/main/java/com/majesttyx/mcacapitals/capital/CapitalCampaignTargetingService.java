package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class CapitalCampaignTargetingService {

    private CapitalCampaignTargetingService() {
    }

    static void applyTargets(
            net.minecraft.server.level.ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital
    ) {
        List<VillagerEntityMCA> attackers =
                loadedCombatants(
                        level,
                        campaign.getAttackerIds()
                );

        List<VillagerEntityMCA> defenders =
                loadedCombatants(
                        level,
                        campaign.getDefenderIds()
                );

        List<VillagerEntityMCA> royalDefenders =
                loadedCombatants(
                        level,
                        List.copyOf(
                                defendingCapital
                                        .getRoyalGuards()
                        )
                );

        LivingEntity sovereign = null;

        if (campaign.didDefendingSovereignRefusePeace()
                && defendingCapital.getSovereign()
                != null) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            defendingCapital
                                    .getSovereign()
                    )
                    instanceof LivingEntity living
                    && living.isAlive()) {
                sovereign = living;
            }
        }

        for (VillagerEntityMCA attacker :
                attackers) {
            LivingEntity target =
                    nearest(attacker, defenders);

            if (target == null) {
                target = sovereign;
            }

            setCombatTarget(
                    attacker,
                    target
            );
        }

        for (VillagerEntityMCA defender :
                defenders) {
            LivingEntity target =
                    nearest(defender, attackers);

            setCombatTarget(
                    defender,
                    target
            );
        }

        for (VillagerEntityMCA royalDefender :
                royalDefenders) {
            LivingEntity target =
                    nearest(
                            royalDefender,
                            attackers
                    );

            setCombatTarget(
                    royalDefender,
                    target
            );
        }
    }

    static void clearCampaignTargets(
            net.minecraft.server.level.ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            attackerId
                    )
                    instanceof VillagerEntityMCA villager) {
                clearCombatTarget(villager);
            }
        }

        for (UUID defenderId :
                campaign.getDefenderIds()) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            defenderId
                    )
                    instanceof VillagerEntityMCA villager) {
                clearCombatTarget(villager);
            }
        }

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                );

        if (defendingCapital != null) {
            for (UUID royalGuardId :
                    defendingCapital.getRoyalGuards()) {
                if (MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                royalGuardId
                        )
                        instanceof VillagerEntityMCA villager) {
                    clearCombatTarget(villager);
                }
            }
        }
    }

    static void clearCombatTarget(
            VillagerEntityMCA villager
    ) {
        if (villager == null) {
            return;
        }

        villager.setTarget(null);

        villager.getBrain().eraseMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY
        );

        villager.getBrain().eraseMemory(
                MemoryModuleType.ATTACK_TARGET
        );

        villager.getBrain().eraseMemory(
                MemoryModuleType.WALK_TARGET
        );

        villager.getBrain().eraseMemory(
                MemoryModuleType.LOOK_TARGET
        );
    }

    private static void setCombatTarget(
            VillagerEntityMCA villager,
            LivingEntity target
    ) {
        if (villager == null) {
            return;
        }

        if (target == null
                || !target.isAlive()) {
            clearCombatTarget(villager);
            return;
        }

        villager.getBrain().setMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY,
                target
        );

        villager.getBrain().setMemory(
                MemoryModuleType.ATTACK_TARGET,
                target
        );

        villager.setTarget(target);
    }

    private static List<VillagerEntityMCA>
    loadedCombatants(
            net.minecraft.server.level.ServerLevel level,
            List<UUID> ids
    ) {
        List<VillagerEntityMCA> result =
                new ArrayList<>();

        for (UUID id : ids) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            id
                    )
                    instanceof VillagerEntityMCA villager
                    && villager.isAlive()) {
                result.add(villager);
            }
        }

        return result;
    }

    private static LivingEntity nearest(
            LivingEntity source,
            List<? extends LivingEntity> candidates
    ) {
        return candidates.stream()
                .filter(candidate ->
                        candidate != null
                )
                .filter(LivingEntity::isAlive)
                .min(
                        Comparator.comparingDouble(
                                source::distanceToSqr
                        )
                )
                .orElse(null);
    }
}