package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.UUID;

final class CapitalCampaignReturnService {

    private CapitalCampaignReturnService() {
    }

    static boolean processRetreat(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        Village attackingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                attackingCapital
                        );

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (attackingVillage == null
                || defendingVillage == null) {
            return false;
        }

        boolean changed = false;
        long now = level.getGameTime();

        boolean forceReturn =
                now >= campaign.getReturnDeadline();

        int homeIndex = 0;

        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (campaign.hasAttackerReturned(
                    attackerId
            )
                    || isKnownDead(
                    level,
                    attackerId
            )) {
                continue;
            }

            if (!(MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            attackerId
                    )
                    instanceof VillagerEntityMCA attacker)
                    || !attacker.isAlive()) {
                continue;
            }

            restoreVisibleState(attacker);

            CapitalCampaignTargetingService
                    .clearCombatTarget(attacker);

            if (attackingVillage
                    .isWithinBorder(attacker)) {
                campaign.markAttackerReturned(
                        attackerId
                );
                changed = true;
                continue;
            }

            if (forceReturn
                    || !defendingVillage
                    .isWithinBorder(attacker)) {
                teleportHome(
                        level,
                        attackingVillage,
                        attacker,
                        homeIndex++
                );

                campaign.markAttackerReturned(
                        attackerId
                );

                changed = true;
                continue;
            }

            BlockPos edge =
                    nearestOutsideEdge(
                            level,
                            defendingVillage,
                            attacker.blockPosition()
                    );

            attacker.getNavigation().moveTo(
                    edge.getX() + 0.5D,
                    edge.getY(),
                    edge.getZ() + 0.5D,
                    1.1D
            );
        }

        if (changed) {
            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();
        }

        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (!campaign.hasAttackerReturned(
                    attackerId
            )
                    && !isKnownDead(
                    level,
                    attackerId
            )) {
                return false;
            }
        }

        return true;
    }

    private static void teleportHome(
            ServerLevel level,
            Village attackingVillage,
            VillagerEntityMCA attacker,
            int index
    ) {
        BlockPos center =
                new BlockPos(
                        attackingVillage.getCenter()
                );

        int offsetX =
                (index % 3) - 1;

        int offsetZ =
                (index / 3) - 1;

        int x =
                center.getX() + offsetX * 2;

        int z =
                center.getZ() + offsetZ * 2;

        int y = level.getHeight(
                Heightmap.Types
                        .MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        restoreVisibleState(attacker);

        attacker.teleportTo(
                x + 0.5D,
                y,
                z + 0.5D
        );

        attacker.refreshDimensions();
        refreshClientTracking(level, attacker);
    }

    private static void restoreVisibleState(
            VillagerEntityMCA attacker
    ) {
        attacker.getNavigation().stop();
        attacker.stopUsingItem();

        if (attacker.isSleeping()) {
            attacker.stopSleeping();
        }

        attacker.stopRiding();
        attacker.setNoAi(false);
        attacker.setAggressive(false);
        attacker.setInvisible(false);
        attacker.removeEffect(
                MobEffects.INVISIBILITY
        );
        attacker.setPose(Pose.STANDING);
        attacker.setDeltaMovement(Vec3.ZERO);
        attacker.setPersistenceRequired();
    }

    private static void refreshClientTracking(
            ServerLevel level,
            VillagerEntityMCA attacker
    ) {
        level.getChunkSource()
                .removeEntity(attacker);

        level.getChunkSource()
                .addEntity(attacker);
    }

    private static BlockPos nearestOutsideEdge(
            ServerLevel level,
            Village village,
            BlockPos current
    ) {
        BoundingBox box = village.getBox();

        BlockPos center =
                new BlockPos(village.getCenter());

        int minX = box == null
                ? center.getX() - 24
                : box.minX();

        int maxX = box == null
                ? center.getX() + 24
                : box.maxX();

        int minZ = box == null
                ? center.getZ() - 24
                : box.minZ();

        int maxZ = box == null
                ? center.getZ() + 24
                : box.maxZ();

        int westDistance =
                Math.abs(
                        current.getX() - minX
                );

        int eastDistance =
                Math.abs(
                        maxX - current.getX()
                );

        int northDistance =
                Math.abs(
                        current.getZ() - minZ
                );

        int southDistance =
                Math.abs(
                        maxZ - current.getZ()
                );

        int minimum = Math.min(
                Math.min(
                        westDistance,
                        eastDistance
                ),
                Math.min(
                        northDistance,
                        southDistance
                )
        );

        int x = current.getX();
        int z = current.getZ();

        if (minimum == westDistance) {
            x = minX - 2;
            z = clamp(z, minZ, maxZ);
        } else if (minimum == eastDistance) {
            x = maxX + 2;
            z = clamp(z, minZ, maxZ);
        } else if (minimum == northDistance) {
            z = minZ - 2;
            x = clamp(x, minX, maxX);
        } else {
            z = maxZ + 2;
            x = clamp(x, minX, maxX);
        }

        int y = level.getHeight(
                Heightmap.Types
                        .MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        return new BlockPos(x, y, z);
    }

    private static boolean isKnownDead(
            ServerLevel level,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return true;
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        villagerId
                )) {
            return true;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        return entity == null
                || !entity.isAlive()
                || entity.isRemoved();
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
}