package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MoveState;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CapitalCampaignCombatService {

    private static final double CAMPAIGN_TARGET_RANGE_SQR =
            96.0D * 96.0D;

    private static final float MELEE_SPEED = 0.6F;
    private static final float RANGED_SPEED = 0.6F;
    private static final long TARGET_MEMORY_TICKS = 80L;

    private CapitalCampaignCombatService() {
    }

    public static boolean isActiveCombatant(
            ServerLevel level,
            UUID villagerId
    ) {
        return findActiveCampaign(
                level,
                villagerId
        ) != null;
    }

    public static boolean areOpposingCombatants(
            ServerLevel level,
            UUID firstId,
            UUID secondId
    ) {
        if (level == null
                || firstId == null
                || secondId == null
                || firstId.equals(secondId)) {
            return false;
        }

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase()
                    != CapitalCampaignPhase.ACTIVE) {
                continue;
            }

            boolean firstAttacker =
                    campaign.containsAttacker(firstId);

            boolean secondAttacker =
                    campaign.containsAttacker(secondId);

            if (firstAttacker == secondAttacker) {
                continue;
            }

            UUID defenderId =
                    firstAttacker
                            ? secondId
                            : firstId;

            if (campaign.containsDefender(defenderId)) {
                return true;
            }

            if (!campaign.isCrownRallyPending()
                    && isCrownDefender(
                    campaign,
                    defenderId
            )) {
                return true;
            }
        }

        return false;
    }

    public static void enforceCombatState(
            VillagerEntityMCA villager
    ) {
        if (villager == null
                || !villager.isAlive()
                || villager.isRemoved()
                || !(villager.level()
                instanceof ServerLevel level)) {
            return;
        }

        CapitalCampaignRecord campaign =
                findActiveCampaign(
                        level,
                        villager.getUUID()
                );

        if (campaign == null) {
            return;
        }

        if (villager.getVillagerBrain().getMoveState()
                != MoveState.MOVE) {
            villager.getVillagerBrain().setMoveState(
                    MoveState.MOVE,
                    null
            );
        }

        Brain<VillagerEntityMCA> brain =
                villager.getMCABrain();

        erasePanicState(
                villager,
                brain
        );

        if (campaign.isFieldDefeatResolutionPending()
                || campaign.isCrownRallyPending()) {
            clearCombatIntent(
                    villager,
                    brain
            );

            brain.setActiveActivityIfPossible(
                    Activity.IDLE
            );

            return;
        }

        LivingEntity target =
                resolveCampaignTarget(
                        level,
                        campaign,
                        villager,
                        brain
                );

        if (target == null) {
            clearCombatIntent(
                    villager,
                    brain
            );

            return;
        }

        boolean ranged =
                villager.getMainHandItem()
                        .getItem()
                        instanceof ProjectileWeaponItem;

        float speed =
                ranged
                        ? RANGED_SPEED
                        : MELEE_SPEED;

        villager.setNoAi(false);
        villager.setAggressive(true);
        villager.setTarget(target);

        brain.setMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY,
                target
        );

        brain.setMemoryWithExpiry(
                MemoryModuleType.ATTACK_TARGET,
                target,
                TARGET_MEMORY_TICKS
        );

        brain.setMemory(
                MemoryModuleType.LOOK_TARGET,
                new EntityTracker(
                        target,
                        true
                )
        );

        brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(
                        target,
                        speed,
                        ranged ? 10 : 1
                )
        );

        brain.setActiveActivityIfPossible(
                Activity.IDLE
        );

        if (!ranged
                || !villager.hasLineOfSight(target)) {
            villager.getNavigation().moveTo(
                    target,
                    speed
            );
        }
    }

    private static CapitalCampaignRecord findActiveCampaign(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null
                || villagerId == null) {
            return null;
        }

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase()
                    != CapitalCampaignPhase.ACTIVE) {
                continue;
            }

            if (campaign.containsAttacker(villagerId)
                    || campaign.containsDefender(villagerId)
                    || isCrownDefender(
                    campaign,
                    villagerId
            )) {
                return campaign;
            }
        }

        return null;
    }

    private static boolean isCrownDefender(
            CapitalCampaignRecord campaign,
            UUID villagerId
    ) {
        if (campaign == null
                || villagerId == null
                || !campaign
                .didDefendingSovereignRefusePeace()) {
            return false;
        }

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign
                                .getDefendingCapitalId()
                );

        if (defendingCapital == null) {
            return false;
        }

        return defendingCapital.isRoyalGuard(
                villagerId
        )
                || villagerId.equals(
                defendingCapital.getSovereign()
        );
    }

    private static void erasePanicState(
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> brain
    ) {
        boolean wasPanicking =
                brain.isActive(Activity.PANIC);

        brain.eraseMemory(
                MemoryModuleType.HURT_BY
        );

        brain.eraseMemory(
                MemoryModuleType.HURT_BY_ENTITY
        );

        brain.eraseMemory(
                MemoryModuleType.IS_PANICKING
        );

        brain.eraseMemory(
                MemoryModuleType.NEAREST_HOSTILE
        );

        villager.setLastHurtByMob(null);

        if (wasPanicking) {
            villager.getNavigation().stop();

            brain.eraseMemory(
                    MemoryModuleType.WALK_TARGET
            );

            brain.setActiveActivityIfPossible(
                    Activity.IDLE
            );
        }
    }

    private static void clearCombatIntent(
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> brain
    ) {
        villager.setTarget(null);
        villager.setAggressive(false);
        villager.getNavigation().stop();

        brain.eraseMemory(
                MemoryModuleType.ATTACK_TARGET
        );

        brain.eraseMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY
        );

        brain.eraseMemory(
                MemoryModuleType.WALK_TARGET
        );

        brain.eraseMemory(
                MemoryModuleType.LOOK_TARGET
        );
    }

    private static LivingEntity resolveCampaignTarget(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> brain
    ) {
        LivingEntity current =
                villager.getTarget();

        if (isValidTarget(
                level,
                villager,
                current
        )) {
            return current;
        }

        LivingEntity memoryTarget =
                brain.getMemory(
                        MemoryModuleType.ATTACK_TARGET
                ).orElse(null);

        if (isValidTarget(
                level,
                villager,
                memoryTarget
        )) {
            return memoryTarget;
        }

        LivingEntity guardMemoryTarget =
                brain.getMemory(
                        MemoryModuleTypeMCA
                                .NEAREST_GUARD_ENEMY
                ).orElse(null);

        if (isValidTarget(
                level,
                villager,
                guardMemoryTarget
        )) {
            return guardMemoryTarget;
        }

        return findNearestCampaignOpponent(
                level,
                campaign,
                villager
        );
    }

    private static LivingEntity findNearestCampaignOpponent(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            VillagerEntityMCA villager
    ) {
        List<UUID> candidateIds =
                new ArrayList<>();

        if (campaign.containsAttacker(
                villager.getUUID()
        )) {
            candidateIds.addAll(
                    campaign.getDefenderIds()
            );

            if (campaign
                    .didDefendingSovereignRefusePeace()) {
                CapitalRecord defendingCapital =
                        CapitalManager.getCapital(
                                campaign
                                        .getDefendingCapitalId()
                        );

                if (defendingCapital != null) {
                    candidateIds.addAll(
                            defendingCapital
                                    .getRoyalGuards()
                    );

                    if (defendingCapital
                            .getSovereign() != null) {
                        candidateIds.add(
                                defendingCapital
                                        .getSovereign()
                        );
                    }
                }
            }
        } else {
            candidateIds.addAll(
                    campaign.getAttackerIds()
            );
        }

        LivingEntity nearest = null;
        double nearestDistance =
                Double.MAX_VALUE;

        for (UUID candidateId : candidateIds) {
            Entity entity =
                    MCAIntegrationBridge
                            .findLoadedEntityByUuid(
                                    level,
                                    candidateId
                            );

            if (!(entity
                    instanceof LivingEntity candidate)
                    || !isValidTarget(
                    level,
                    villager,
                    candidate
            )) {
                continue;
            }

            double distance =
                    villager.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static boolean isValidTarget(
            ServerLevel level,
            VillagerEntityMCA villager,
            LivingEntity target
    ) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == level
                && villager.distanceToSqr(target)
                <= CAMPAIGN_TARGET_RANGE_SQR
                && areOpposingCombatants(
                level,
                villager.getUUID(),
                target.getUUID()
        );
    }
}