package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignDeploymentService {

    private static final double FIELD_DEFENDER_RADIUS_SQR = 96.0D * 96.0D;

    private static final int[][] FORMATION_OFFSETS = {
            {-3, 2}, {0, 3}, {3, 2}, {-4, 0}, {4, 0}, {-3, -2}, {3, -2}, {0, -4},
            {-5, 3}, {5, 3}, {-5, -3}, {5, -3}, {-7, 1}, {7, 1}, {-7, -2}, {7, -2},
            {-2, 7}, {2, 7}, {-2, -7}, {2, -7}
    };

    private CapitalCampaignDeploymentService() {
    }

    static DeploymentResult deploy(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            ServerPlayer anchor,
            List<VillagerEntityMCA> assembledAttackers
    ) {
        Village defendingVillage = CapitalCampaignEligibilityService.getVillage(level, defendingCapital);
        if (defendingVillage == null) {
            return DeploymentResult.invalid("The defending capital's MCA village is unavailable.");
        }

        if (anchor == null
                || anchor.level() != level
                || !anchor.isAlive()
                || anchor.isSpectator()
                || !defendingVillage.isWithinBorder(anchor)) {
            return DeploymentResult.waiting(
                    "Remain inside the defending capital while the force deploys."
            );
        }

        if (assembledAttackers == null || assembledAttackers.isEmpty()) {
            return DeploymentResult.invalid(
                    "No campaign attackers remained available for deployment."
            );
        }

        List<VillagerEntityMCA> availableAttackers = assembledAttackers.stream()
                .filter(attacker -> attacker != null && attacker.isAlive() && !attacker.isRemoved())
                .limit(CapitalCampaignRecord.MAX_ATTACKERS)
                .toList();

        if (availableAttackers.isEmpty()) {
            return DeploymentResult.invalid(
                    "No campaign attackers remained available for deployment."
            );
        }

        List<BlockPos> positions = createFormation(
                level,
                defendingVillage,
                anchor.blockPosition(),
                availableAttackers,
                availableAttackers.size()
        );

        if (positions.size() < availableAttackers.size()) {
            return DeploymentResult.waiting(
                    "Move to a clearer outdoor area inside the defending capital so the full campaign force can form around you."
            );
        }

        List<UUID> deployedAttackers = new ArrayList<>();
        for (int index = 0; index < availableAttackers.size(); index++) {
            VillagerEntityMCA attacker = availableAttackers.get(index);
            BlockPos position = positions.get(index);

            prepareForCampaignTeleport(attacker);
            attacker.teleportTo(
                    position.getX() + 0.5D,
                    position.getY(),
                    position.getZ() + 0.5D
            );
            attacker.refreshDimensions();
            refreshClientTracking(level, attacker);
            deployedAttackers.add(attacker.getUUID());
        }

        if (deployedAttackers.size() != availableAttackers.size()) {
            return DeploymentResult.invalid(
                    "The complete assembled campaign force could not be deployed."
            );
        }

        List<UUID> defenders = findFieldDefenders(
                level,
                defendingCapital,
                anchor.position()
        );

        campaign.replaceAttackerIds(deployedAttackers);
        campaign.setDefenderIds(defenders);
        campaign.beginFormation(
                level.getGameTime(),
                level.getGameTime() + CapitalCampaignAssemblyService.FORMATION_DURATION_TICKS
        );
        CapitalCampaignDataAccess.get(level).setDirty();

        String attackingName = CapitalDiplomaticAgreementText.capitalName(level, attackingCapital);
        String defendingName = CapitalDiplomaticAgreementText.capitalName(level, defendingCapital);
        String entry = deployedAttackers.size()
                + " campaign attackers from "
                + attackingName
                + " formed inside "
                + defendingName
                + " to face "
                + defenders.size()
                + " field defenders. Prepare for battle!";

        CapitalChronicleService.addEntry(level, attackingCapital, entry);
        CapitalChronicleService.addEntry(level, defendingCapital, entry);

        return DeploymentResult.success();
    }

    static List<UUID> findFieldDefenders(
            ServerLevel level,
            CapitalRecord defendingCapital,
            Vec3 battleCenter
    ) {
        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                defendingCapital.getCapitalId()
        );

        return residents.stream()
                .filter(id -> id != null)
                .filter(id -> !id.equals(defendingCapital.getSovereign()))
                .filter(id -> !defendingCapital.isRoyalGuard(id))
                .filter(id -> MCAIntegrationBridge.isMCAGuard(level, id))
                .filter(id -> MCAIntegrationBridge.isTeenOrAdultVillager(level, id))
                .filter(id -> MCAIntegrationBridge.isLoadedAndAlive(level, id))
                .filter(id -> {
                    if (battleCenter == null) {
                        return true;
                    }
                    Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, id);
                    return entity != null && entity.distanceToSqr(battleCenter) <= FIELD_DEFENDER_RADIUS_SQR;
                })
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
    }

    private static List<BlockPos> createFormation(
            ServerLevel level,
            Village defendingVillage,
            BlockPos anchor,
            List<VillagerEntityMCA> attackers,
            int requiredPositions
    ) {
        List<BlockPos> positions = new ArrayList<>();

        for (int[] offset : FORMATION_OFFSETS) {
            VillagerEntityMCA attacker = attackers.get(
                    Math.min(positions.size(), attackers.size() - 1)
            );
            BlockPos safePosition = findSafeOutdoorPosition(
                    level,
                    defendingVillage,
                    attacker,
                    anchor.offset(offset[0], 0, offset[1])
            );

            if (safePosition == null || positions.contains(safePosition)) {
                continue;
            }

            positions.add(safePosition);
            if (positions.size() >= requiredPositions) {
                break;
            }
        }

        return List.copyOf(positions);
    }

    private static BlockPos findSafeOutdoorPosition(
            ServerLevel level,
            Village defendingVillage,
            VillagerEntityMCA attacker,
            BlockPos horizontal
    ) {
        if (!level.hasChunkAt(horizontal)
                || !defendingVillage.isWithinBorder(horizontal, 0)) {
            return null;
        }

        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                horizontal.getX(),
                horizontal.getZ()
        );

        LinkedHashSet<Integer> heights = new LinkedHashSet<>();
        heights.add(horizontal.getY());
        heights.add(surfaceY);

        for (int offset = 1; offset <= 6; offset++) {
            heights.add(horizontal.getY() + offset);
            heights.add(horizontal.getY() - offset);
            heights.add(surfaceY + offset);
            heights.add(surfaceY - offset);
        }

        for (int y : heights) {
            BlockPos candidate = new BlockPos(horizontal.getX(), y, horizontal.getZ());
            if (isSafeOutdoorPosition(level, defendingVillage, attacker, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isSafeOutdoorPosition(
            ServerLevel level,
            Village defendingVillage,
            VillagerEntityMCA attacker,
            BlockPos position
    ) {
        if (!defendingVillage.isWithinBorder(position, 0)
                || !level.canSeeSky(position.above())
                || !level.getFluidState(position).isEmpty()
                || !level.getFluidState(position.above()).isEmpty()) {
            return false;
        }

        BlockPos floor = position.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }

        AABB destinationBox = attacker.getDimensions(Pose.STANDING)
                .makeBoundingBox(Vec3.atBottomCenterOf(position));
        return level.noCollision(attacker, destinationBox);
    }

    private static void prepareForCampaignTeleport(VillagerEntityMCA attacker) {
        CapitalCampaignTargetingService.clearCombatTarget(attacker);
        attacker.getNavigation().stop();
        attacker.stopUsingItem();

        if (attacker.isSleeping()) {
            attacker.stopSleeping();
        }

        attacker.stopRiding();
        attacker.setNoAi(false);
        attacker.setAggressive(false);
        attacker.setInvisible(false);
        attacker.removeEffect(MobEffects.INVISIBILITY);
        attacker.setPose(Pose.STANDING);
        attacker.setDeltaMovement(Vec3.ZERO);
        attacker.setPersistenceRequired();
    }

    private static void refreshClientTracking(
            ServerLevel level,
            VillagerEntityMCA attacker
    ) {
        level.getChunkSource().removeEntity(attacker);
        level.getChunkSource().addEntity(attacker);
    }

    record DeploymentResult(
            boolean deployed,
            boolean invalid,
            String failureMessage
    ) {
        static DeploymentResult success() {
            return new DeploymentResult(true, false, null);
        }

        static DeploymentResult waiting(String message) {
            return new DeploymentResult(false, false, message);
        }

        static DeploymentResult invalid(String message) {
            return new DeploymentResult(false, true, message);
        }
    }
}
