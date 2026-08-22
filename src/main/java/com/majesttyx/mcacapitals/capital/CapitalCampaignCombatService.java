package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.conczin.mca.entity.VillagerEntityMCA;
import fabric.net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import fabric.net.conczin.mca.entity.ai.MoveState;
import fabric.net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalCampaignCombatService {

    private static final double CAMPAIGN_TARGET_RANGE_SQR =
            192.0D * 192.0D;

    private static final float MELEE_SPEED = 0.6F;
    private static final float RANGED_SPEED = 0.6F;
    private static final float RETURN_SPEED = 0.6F;
    private static final long TARGET_MEMORY_TICKS = 80L;
    private static final long OUTSIDE_TELEPORT_TICKS = 20L * 5L;

    private static final Map<UUID, Long> OUTSIDE_SINCE =
            new HashMap<>();

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
        return findOpposingCampaign(
                level,
                firstId,
                secondId
        ) != null;
    }

    public static boolean canApplyCampaignDamage(
            ServerLevel level,
            LivingEntity attacker,
            LivingEntity victim
    ) {
        if (level == null
                || attacker == null
                || victim == null) {
            return false;
        }

        CapitalCampaignRecord campaign =
                findOpposingCampaign(
                        level,
                        attacker.getUUID(),
                        victim.getUUID()
                );

        if (campaign == null
                || campaign.isFieldDefeatResolutionPending()
                || campaign.isCrownRallyPending()) {
            return false;
        }

        Village defendingVillage =
                getDefendingVillage(
                        level,
                        campaign
                );

        return defendingVillage != null
                && defendingVillage.isWithinBorder(attacker)
                && defendingVillage.isWithinBorder(victim);
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
            OUTSIDE_SINCE.remove(
                    villager.getUUID()
            );
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

        Village defendingVillage =
                getDefendingVillage(
                        level,
                        campaign
                );

        if (defendingVillage == null) {
            clearCombatIntent(
                    villager,
                    brain
            );
            return;
        }

        if (!defendingVillage
                .isWithinBorder(villager)) {
            returnToBattleBounds(
                    level,
                    defendingVillage,
                    villager,
                    brain
            );
            return;
        }

        OUTSIDE_SINCE.remove(
                villager.getUUID()
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
                        defendingVillage,
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

        float speed = ranged
                ? RANGED_SPEED
                : MELEE_SPEED;

        villager.setNoAi(false);
        villager.setAggressive(true);
        villager.setTarget(target);

        brain.setMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY.get(),
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

    private static CapitalCampaignRecord findOpposingCampaign(
            ServerLevel level,
            UUID firstId,
            UUID secondId
    ) {
        if (level == null
                || firstId == null
                || secondId == null
                || firstId.equals(secondId)) {
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

            boolean firstAttacker =
                    campaign.containsAttacker(firstId);

            boolean secondAttacker =
                    campaign.containsAttacker(secondId);

            if (firstAttacker == secondAttacker) {
                continue;
            }

            UUID defenderId = firstAttacker
                    ? secondId
                    : firstId;

            if (campaign.containsDefender(defenderId)) {
                return campaign;
            }

            if (!campaign.isCrownRallyPending()
                    && isCrownDefender(
                    campaign,
                    defenderId
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

    private static Village getDefendingVillage(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                );

        return defendingCapital == null
                ? null
                : CapitalCampaignEligibilityService
                .getVillage(
                        level,
                        defendingCapital
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
                        .NEAREST_GUARD_ENEMY.get()
        );

        brain.eraseMemory(
                MemoryModuleType.WALK_TARGET
        );

        brain.eraseMemory(
                MemoryModuleType.LOOK_TARGET
        );
    }

    private static void returnToBattleBounds(
            ServerLevel level,
            Village defendingVillage,
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> brain
    ) {
        clearCombatIntent(
                villager,
                brain
        );

        brain.setActiveActivityIfPossible(
                Activity.IDLE
        );

        BlockPos destination =
                findSafeInteriorPosition(
                        level,
                        defendingVillage,
                        villager.blockPosition()
                );

        long now = level.getGameTime();
        long outsideSince = OUTSIDE_SINCE
                .computeIfAbsent(
                        villager.getUUID(),
                        ignored -> now
                );

        if (now - outsideSince
                >= OUTSIDE_TELEPORT_TICKS) {
            villager.setDeltaMovement(Vec3.ZERO);
            villager.teleportTo(
                    destination.getX() + 0.5D,
                    destination.getY(),
                    destination.getZ() + 0.5D
            );
            villager.refreshDimensions();
            OUTSIDE_SINCE.remove(
                    villager.getUUID()
            );
            return;
        }

        brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(
                        destination,
                        RETURN_SPEED,
                        1
                )
        );

        villager.getNavigation().moveTo(
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                RETURN_SPEED
        );
    }

    private static BlockPos findSafeInteriorPosition(
            ServerLevel level,
            Village village,
            BlockPos current
    ) {
        BlockPos center =
                new BlockPos(village.getCenter());

        BoundingBox box = village.getBox();

        if (box == null) {
            return surfacePosition(
                    level,
                    center.getX(),
                    center.getZ()
            );
        }

        int minX = box.minX() + 3;
        int maxX = box.maxX() - 3;
        int minZ = box.minZ() + 3;
        int maxZ = box.maxZ() - 3;

        if (minX > maxX) {
            minX = maxX = center.getX();
        }

        if (minZ > maxZ) {
            minZ = maxZ = center.getZ();
        }

        int x = clamp(
                current.getX(),
                minX,
                maxX
        );
        int z = clamp(
                current.getZ(),
                minZ,
                maxZ
        );

        BlockPos candidate = surfacePosition(
                level,
                x,
                z
        );

        if (village.isWithinBorder(candidate, 0)) {
            return candidate;
        }

        return surfacePosition(
                level,
                center.getX(),
                center.getZ()
        );
    }

    private static BlockPos surfacePosition(
            ServerLevel level,
            int x,
            int z
    ) {
        int y = level.getHeight(
                Heightmap.Types
                        .MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        return new BlockPos(x, y, z);
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    private static LivingEntity resolveCampaignTarget(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            Village defendingVillage,
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> brain
    ) {
        LivingEntity current =
                villager.getTarget();

        if (isValidTarget(
                level,
                campaign,
                defendingVillage,
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
                campaign,
                defendingVillage,
                villager,
                memoryTarget
        )) {
            return memoryTarget;
        }

        LivingEntity guardMemoryTarget =
                brain.getMemory(
                        MemoryModuleTypeMCA
                                .NEAREST_GUARD_ENEMY.get()
                ).orElse(null);

        if (isValidTarget(
                level,
                campaign,
                defendingVillage,
                villager,
                guardMemoryTarget
        )) {
            return guardMemoryTarget;
        }

        return findNearestCampaignOpponent(
                level,
                campaign,
                defendingVillage,
                villager
        );
    }

    private static LivingEntity findNearestCampaignOpponent(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            Village defendingVillage,
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
            Entity entity = MCAIntegrationBridge
                    .findLoadedEntityByUuid(
                            level,
                            candidateId
                    );

            if (!(entity
                    instanceof LivingEntity candidate)
                    || !isValidTarget(
                    level,
                    campaign,
                    defendingVillage,
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
            CapitalCampaignRecord campaign,
            Village defendingVillage,
            VillagerEntityMCA villager,
            LivingEntity target
    ) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == level
                && defendingVillage.isWithinBorder(villager)
                && defendingVillage.isWithinBorder(target)
                && villager.distanceToSqr(target)
                <= CAMPAIGN_TARGET_RANGE_SQR
                && findOpposingCampaign(
                level,
                villager.getUUID(),
                target.getUUID()
        ) == campaign;
    }
}
