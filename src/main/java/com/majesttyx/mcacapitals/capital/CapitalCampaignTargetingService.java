package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.mca.entity.VillagerEntityMCA;
import forge.net.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CapitalCampaignTargetingService {

    private static final double DETECTION_RANGE_SQR =
            96.0D * 96.0D;

    private static final double CLOSE_UNSEEN_RANGE_SQR =
            4.0D * 4.0D;

    private static final double RALLY_RANGE_SQR =
            12.0D * 12.0D;

    private static final float ATTACKER_SPEED =
            0.6F;

    private static final float DEFENDER_SPEED =
            0.6F;

    private static final float RALLY_SPEED =
            0.55F;

    private static final long TARGET_MEMORY_TICKS =
            80L;

    private CapitalCampaignTargetingService() {
    }

    static void applyTargets(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital
    ) {
        List<VillagerEntityMCA> attackers =
                loadedCombatants(
                        level,
                        campaign.getAttackerIds()
                );

        ServerPlayer initiatingPlayer =
                findInitiatingPlayer(
                        level,
                        campaign
                );

        if (!campaign
                .didDefendingSovereignRefusePeace()) {
            List<VillagerEntityMCA> fieldDefenders =
                    loadedCombatants(
                            level,
                            campaign.getDefenderIds()
                    );

            assignTargets(
                    attackers,
                    new ArrayList<>(
                            fieldDefenders
                    ),
                    ATTACKER_SPEED,
                    true
            );

            assignTargets(
                    fieldDefenders,
                    new ArrayList<>(
                            attackers
                    ),
                    DEFENDER_SPEED,
                    true
            );

            rallyUntargetedAttackers(
                    attackers,
                    initiatingPlayer
            );

            return;
        }

        List<VillagerEntityMCA> crownDefenders =
                loadedCombatants(
                        level,
                        List.copyOf(
                                defendingCapital
                                        .getRoyalGuards()
                        )
                );

        if (defendingCapital.getSovereign()
                != null
                && MCAIntegrationBridge
                .findLoadedMCAVillagerByUuid(
                        level,
                        defendingCapital
                                .getSovereign()
                )
                instanceof VillagerEntityMCA sovereign
                && sovereign.isAlive()
                && !sovereign.isRemoved()) {
            crownDefenders.add(sovereign);
        }

        assignTargets(
                attackers,
                new ArrayList<>(
                        crownDefenders
                ),
                ATTACKER_SPEED,
                true
        );

        assignTargets(
                crownDefenders,
                new ArrayList<>(
                        attackers
                ),
                DEFENDER_SPEED,
                true
        );

        rallyUntargetedAttackers(
                attackers,
                initiatingPlayer
        );
    }

    static void clearCampaignTargets(
            ServerLevel level,
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
                        campaign
                                .getDefendingCapitalId()
                );

        if (defendingCapital == null) {
            return;
        }

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

        if (defendingCapital.getSovereign()
                != null
                && MCAIntegrationBridge
                .findLoadedMCAVillagerByUuid(
                        level,
                        defendingCapital
                                .getSovereign()
                )
                instanceof VillagerEntityMCA sovereign) {
            clearCombatTarget(sovereign);
        }
    }

    static void clearCombatTarget(
            VillagerEntityMCA villager
    ) {
        if (villager == null) {
            return;
        }

        suppressPanic(villager);

        villager.setTarget(null);
        villager.setAggressive(false);
        villager.getNavigation().stop();

        villager.getBrain().eraseMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY.get()
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

    private static void assignTargets(
            List<VillagerEntityMCA> combatants,
            List<? extends LivingEntity> rawCandidates,
            float speed,
            boolean pursueWithoutSight
    ) {
        List<LivingEntity> candidates =
                uniqueLivingCandidates(
                        rawCandidates
                );

        if (combatants.isEmpty()) {
            return;
        }

        if (candidates.isEmpty()) {
            for (VillagerEntityMCA combatant :
                    combatants) {
                clearCombatTarget(combatant);
            }

            return;
        }

        List<VillagerEntityMCA> orderedCombatants =
                combatants.stream()
                        .filter(combatant ->
                                combatant != null
                                        && combatant
                                        .isAlive()
                                        && !combatant
                                        .isRemoved()
                        )
                        .sorted(
                                Comparator.comparing(
                                        combatant ->
                                                combatant
                                                        .getUUID()
                                                        .toString()
                                )
                        )
                        .toList();

        Map<UUID, Integer> assignmentCounts =
                new HashMap<>();

        for (LivingEntity candidate :
                candidates) {
            assignmentCounts.put(
                    candidate.getUUID(),
                    0
            );
        }

        for (VillagerEntityMCA combatant :
                orderedCombatants) {
            LivingEntity target =
                    selectTarget(
                            combatant,
                            candidates,
                            assignmentCounts,
                            pursueWithoutSight
                    );

            if (target == null) {
                clearCombatTarget(combatant);
                continue;
            }

            assignmentCounts.compute(
                    target.getUUID(),
                    (id, count) ->
                            count == null
                                    ? 1
                                    : count + 1
            );

            setCombatTarget(
                    combatant,
                    target,
                    speed
            );
        }
    }

    private static LivingEntity selectTarget(
            VillagerEntityMCA source,
            List<LivingEntity> candidates,
            Map<UUID, Integer> assignmentCounts,
            boolean pursueWithoutSight
    ) {
        List<LivingEntity> available =
                candidates.stream()
                        .filter(candidate ->
                                isAvailableTarget(
                                        source,
                                        candidate,
                                        pursueWithoutSight
                                )
                        )
                        .toList();

        if (available.isEmpty()) {
            return null;
        }

        boolean ranged =
                isRanged(source);

        int preferredMaximum =
                ranged ? 1 : 2;

        List<LivingEntity> underPreferredMaximum =
                available.stream()
                        .filter(candidate ->
                                assignmentCounts
                                        .getOrDefault(
                                                candidate
                                                        .getUUID(),
                                                0
                                        )
                                        < preferredMaximum
                        )
                        .toList();

        List<LivingEntity> pool =
                underPreferredMaximum.isEmpty()
                        ? available
                        : underPreferredMaximum;

        LivingEntity currentTarget =
                source.getTarget();

        return pool.stream()
                .min(
                        Comparator
                                .<LivingEntity>comparingInt(
                                        candidate ->
                                                assignmentCounts
                                                        .getOrDefault(
                                                                candidate
                                                                        .getUUID(),
                                                                0
                                                        )
                                )
                                .thenComparingInt(
                                        candidate ->
                                                candidate
                                                        == currentTarget
                                                        ? 0
                                                        : 1
                                )
                                .thenComparingInt(
                                        candidate ->
                                                source
                                                        .hasLineOfSight(
                                                                candidate
                                                        )
                                                        ? 0
                                                        : 1
                                )
                                .thenComparingDouble(
                                        source::distanceToSqr
                                )
                                .thenComparing(
                                        candidate ->
                                                candidate
                                                        .getUUID()
                                                        .toString()
                                )
                )
                .orElse(null);
    }

    private static boolean isAvailableTarget(
            VillagerEntityMCA source,
            LivingEntity candidate,
            boolean pursueWithoutSight
    ) {
        if (candidate == null
                || candidate == source
                || !candidate.isAlive()
                || candidate.isRemoved()
                || candidate.level()
                != source.level()) {
            return false;
        }

        double distance =
                source.distanceToSqr(candidate);

        if (distance
                > DETECTION_RANGE_SQR) {
            return false;
        }

        return pursueWithoutSight
                || source.hasLineOfSight(candidate)
                || distance
                <= CLOSE_UNSEEN_RANGE_SQR;
    }

    private static void setCombatTarget(
            VillagerEntityMCA villager,
            LivingEntity target,
            float speed
    ) {
        if (villager == null) {
            return;
        }

        if (target == null
                || !target.isAlive()
                || target.isRemoved()
                || target.level()
                != villager.level()
                || villager.distanceToSqr(target)
                > DETECTION_RANGE_SQR) {
            clearCombatTarget(villager);
            return;
        }

        suppressPanic(villager);

        villager.setNoAi(false);
        villager.setAggressive(true);

        villager.getBrain().setMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY.get(),
                target
        );

        villager.getBrain().setMemoryWithExpiry(
                MemoryModuleType.ATTACK_TARGET,
                target,
                TARGET_MEMORY_TICKS
        );

        villager.getBrain().setMemory(
                MemoryModuleType.LOOK_TARGET,
                new EntityTracker(
                        target,
                        true
                )
        );

        boolean ranged =
                isRanged(villager);

        villager.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(
                        target,
                        speed,
                        ranged ? 10 : 1
                )
        );

        villager.setTarget(target);

        if (!ranged
                || !villager.hasLineOfSight(
                target
        )) {
            villager.getNavigation().moveTo(
                    target,
                    speed
            );
        }
    }

    private static void rallyUntargetedAttackers(
            List<VillagerEntityMCA> attackers,
            ServerPlayer initiatingPlayer
    ) {
        for (VillagerEntityMCA attacker :
                attackers) {
            if (attacker.getTarget() == null
                    || !attacker
                    .getTarget()
                    .isAlive()) {
                rallyToPlayer(
                        attacker,
                        initiatingPlayer
                );
            }
        }
    }

    private static void rallyToPlayer(
            VillagerEntityMCA attacker,
            ServerPlayer initiatingPlayer
    ) {
        if (attacker == null) {
            return;
        }

        suppressPanic(attacker);

        attacker.setTarget(null);
        attacker.setAggressive(false);

        attacker.getBrain().eraseMemory(
                MemoryModuleTypeMCA
                        .NEAREST_GUARD_ENEMY.get()
        );

        attacker.getBrain().eraseMemory(
                MemoryModuleType.ATTACK_TARGET
        );

        if (initiatingPlayer == null
                || !initiatingPlayer.isAlive()
                || initiatingPlayer.level()
                != attacker.level()) {
            attacker.getNavigation().stop();

            attacker.getBrain().eraseMemory(
                    MemoryModuleType.WALK_TARGET
            );

            attacker.getBrain().eraseMemory(
                    MemoryModuleType.LOOK_TARGET
            );

            return;
        }

        attacker.getBrain().setMemory(
                MemoryModuleType.LOOK_TARGET,
                new EntityTracker(
                        initiatingPlayer,
                        true
                )
        );

        if (attacker.distanceToSqr(
                initiatingPlayer
        ) <= RALLY_RANGE_SQR) {
            attacker.getNavigation().stop();

            attacker.getBrain().eraseMemory(
                    MemoryModuleType.WALK_TARGET
            );

            return;
        }

        attacker.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(
                        initiatingPlayer,
                        RALLY_SPEED,
                        6
                )
        );

        attacker.getNavigation().moveTo(
                initiatingPlayer,
                RALLY_SPEED
        );
    }

    private static void suppressPanic(
            VillagerEntityMCA villager
    ) {
        villager.getBrain().eraseMemory(
                MemoryModuleType.HURT_BY
        );

        villager.getBrain().eraseMemory(
                MemoryModuleType.HURT_BY_ENTITY
        );

        if (villager.getBrain().isActive(
                Activity.PANIC
        )) {
            villager.getBrain()
                    .setActiveActivityIfPossible(
                            Activity.IDLE
                    );
        }
    }

    private static boolean isRanged(
            VillagerEntityMCA villager
    ) {
        return villager.getMainHandItem()
                .getItem()
                instanceof ProjectileWeaponItem;
    }

    private static ServerPlayer findInitiatingPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        if (campaign.getInitiatingPlayerId()
                == null) {
            return null;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                campaign
                                        .getInitiatingPlayerId()
                        );

        return player != null
                && player.level() == level
                && player.isAlive()
                && !player.isSpectator()
                ? player
                : null;
    }

    private static List<VillagerEntityMCA>
    loadedCombatants(
            ServerLevel level,
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
                    && villager.isAlive()
                    && !villager.isRemoved()) {
                result.add(villager);
            }
        }

        return result;
    }

    private static List<LivingEntity>
    uniqueLivingCandidates(
            List<? extends LivingEntity> candidates
    ) {
        Map<UUID, LivingEntity> unique =
                new LinkedHashMap<>();

        for (LivingEntity candidate :
                candidates) {
            if (candidate != null
                    && candidate.isAlive()
                    && !candidate.isRemoved()) {
                unique.putIfAbsent(
                        candidate.getUUID(),
                        candidate
                );
            }
        }

        return new ArrayList<>(
                unique.values()
        );
    }
}