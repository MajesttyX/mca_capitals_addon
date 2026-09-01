package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.MoveState;
import forge.net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignCivilianResponseService {

    private static final int BELL_SEARCH_RADIUS = 72;
    private static final double BELL_RING_DISTANCE_SQR =
            3.5D * 3.5D;
    private static final float BELL_APPROACH_SPEED = 0.65F;

    private static final Map<UUID, UUID> CAMPAIGN_RINGERS =
            new HashMap<>();
    private static final Set<UUID> RUNG_CAMPAIGNS =
            new HashSet<>();
    private static final Map<UUID, Set<UUID>> CAMPAIGN_RESPONDERS =
            new HashMap<>();
    private static final Map<UUID, MoveState> ORIGINAL_MOVE_STATES =
            new HashMap<>();

    private CapitalCampaignCivilianResponseService() {
    }

    static void tickLevel(
            ServerLevel level,
            List<CapitalCampaignRecord> campaigns
    ) {
        Set<UUID> activeCampaignIds =
                new HashSet<>();

        for (CapitalCampaignRecord campaign :
                campaigns) {
            if (campaign != null
                    && campaign.isActiveCampaign()) {
                activeCampaignIds.add(
                        campaign.getCampaignId()
                );
            }
        }

        Set<UUID> knownCampaigns =
                new HashSet<>();

        knownCampaigns.addAll(
                CAMPAIGN_RINGERS.keySet()
        );
        knownCampaigns.addAll(
                RUNG_CAMPAIGNS
        );
        knownCampaigns.addAll(
                CAMPAIGN_RESPONDERS.keySet()
        );

        for (UUID campaignId :
                knownCampaigns) {
            if (!activeCampaignIds.contains(
                    campaignId
            )) {
                clearCampaign(
                        level,
                        campaignId
                );
            }
        }

        for (CapitalCampaignRecord campaign :
                campaigns) {
            tickCampaign(
                    level,
                    campaign
            );
        }
    }

    private static void tickCampaign(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        if (campaign == null
                || campaign.getPhase()
                != CapitalCampaignPhase.ACTIVE
                || campaign.isFieldDefeatResolutionPending()
                || campaign.isCrownRallyPending()) {
            if (campaign != null) {
                clearCampaign(
                        level,
                        campaign.getCampaignId()
                );
            }
            return;
        }

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign
                                .getDefendingCapitalId()
                );

        if (defendingCapital == null) {
            clearCampaign(
                    level,
                    campaign.getCampaignId()
            );
            return;
        }

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (defendingVillage == null
                || !hasPlayerInside(
                level,
                defendingVillage
        )) {
            clearCampaign(
                    level,
                    campaign.getCampaignId()
            );
            return;
        }

        Set<UUID> detained =
                CapitalJusticeDataAccess
                        .getDetainedPrisoners(
                                level,
                                defendingCapital
                                        .getCapitalId()
                        );

        List<VillagerEntityMCA> civilians =
                findCivilians(
                        level,
                        campaign,
                        defendingCapital,
                        defendingVillage,
                        detained
                );

        if (civilians.isEmpty()) {
            return;
        }

        BlockPos bellPos =
                findMeetingPoint(
                        level,
                        defendingVillage
                );

        UUID campaignId =
                campaign.getCampaignId();

        if (!RUNG_CAMPAIGNS.contains(
                campaignId
        )
                && bellPos != null) {
            handleBellRinger(
                    level,
                    campaignId,
                    bellPos,
                    civilians
            );
        }

        UUID ringerId =
                CAMPAIGN_RINGERS.get(
                        campaignId
                );

        Set<UUID> responders =
                CAMPAIGN_RESPONDERS
                        .computeIfAbsent(
                                campaignId,
                                ignored ->
                                        new HashSet<>()
                        );

        for (VillagerEntityMCA civilian :
                civilians) {
            if (civilian.getUUID()
                    .equals(ringerId)
                    && !RUNG_CAMPAIGNS.contains(
                    campaignId
            )) {
                continue;
            }

            applyCivilianAlarm(
                    level,
                    civilian
            );

            responders.add(
                    civilian.getUUID()
            );
        }
    }

    private static List<VillagerEntityMCA> findCivilians(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital,
            Village defendingVillage,
            Set<UUID> detained
    ) {
        List<VillagerEntityMCA> civilians =
                new ArrayList<>();

        for (UUID residentId :
                CapitalResidentScanner.scanResidents(
                        level,
                        defendingCapital.getCapitalId()
                )) {
            if (residentId == null
                    || campaign.containsAttacker(
                    residentId
            )
                    || campaign.containsDefender(
                    residentId
            )
                    || defendingCapital
                    .isRoyalGuard(residentId)
                    || residentId.equals(
                    defendingCapital.getSovereign()
            )
                    || detained.contains(residentId)) {
                continue;
            }

            if (!(MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            residentId
                    )
                    instanceof VillagerEntityMCA civilian)
                    || !civilian.isAlive()
                    || civilian.isRemoved()
                    || civilian.isSleeping()
                    || !defendingVillage
                    .isWithinBorder(civilian)) {
                continue;
            }

            civilians.add(civilian);
        }

        civilians.sort(
                Comparator.comparing(
                        villager ->
                                villager.getUUID()
                                        .toString()
                )
        );

        return civilians;
    }

    private static void handleBellRinger(
            ServerLevel level,
            UUID campaignId,
            BlockPos bellPos,
            List<VillagerEntityMCA> civilians
    ) {
        UUID currentRingerId =
                CAMPAIGN_RINGERS.get(
                        campaignId
                );

        VillagerEntityMCA ringer = null;

        if (currentRingerId != null) {
            for (VillagerEntityMCA civilian :
                    civilians) {
                if (currentRingerId.equals(
                        civilian.getUUID()
                )) {
                    ringer = civilian;
                    break;
                }
            }
        }

        if (ringer == null) {
            ringer = civilians.stream()
                    .filter(civilian ->
                            MCAIntegrationBridge
                                    .isTeenOrAdultVillager(
                                            level,
                                            civilian.getUUID()
                                    )
                    )
                    .min(
                            Comparator.comparingDouble(
                                    civilian ->
                                            civilian.distanceToSqr(
                                                    bellPos.getX()
                                                            + 0.5D,
                                                    bellPos.getY()
                                                            + 0.5D,
                                                    bellPos.getZ()
                                                            + 0.5D
                                            )
                            )
                    )
                    .orElse(null);

            if (ringer == null) {
                return;
            }

            CAMPAIGN_RINGERS.put(
                    campaignId,
                    ringer.getUUID()
            );
        }

        forceMoveFreely(ringer);

        Brain<VillagerEntityMCA> brain =
                ringer.getMCABrain();

        brain.eraseMemory(
                MemoryModuleType
                        .HEARD_BELL_TIME
        );

        brain.setMemory(
                MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(bellPos)
        );

        brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(
                        bellPos,
                        BELL_APPROACH_SPEED,
                        2
                )
        );

        brain.setActiveActivityIfPossible(
                Activity.IDLE
        );

        ringer.getNavigation().moveTo(
                bellPos.getX() + 0.5D,
                bellPos.getY(),
                bellPos.getZ() + 0.5D,
                BELL_APPROACH_SPEED
        );

        if (ringer.distanceToSqr(
                bellPos.getX() + 0.5D,
                bellPos.getY() + 0.5D,
                bellPos.getZ() + 0.5D
        ) > BELL_RING_DISTANCE_SQR) {
            return;
        }

        BlockState bellState =
                level.getBlockState(bellPos);

        if (bellState.getBlock()
                instanceof BellBlock bell
                && bell.attemptToRing(
                ringer,
                level,
                bellPos,
                null
        )) {
            RUNG_CAMPAIGNS.add(
                    campaignId
            );

            CAMPAIGN_RESPONDERS
                    .computeIfAbsent(
                            campaignId,
                            ignored ->
                                    new HashSet<>()
                    )
                    .add(
                            ringer.getUUID()
                    );

            applyCivilianAlarm(
                    level,
                    ringer
            );
        }
    }

    private static void applyCivilianAlarm(
            ServerLevel level,
            VillagerEntityMCA civilian
    ) {
        forceMoveFreely(civilian);

        Brain<VillagerEntityMCA> brain =
                civilian.getMCABrain();

        brain.setMemory(
                MemoryModuleType
                        .HEARD_BELL_TIME,
                level.getGameTime()
        );

        brain.setActiveActivityIfPossible(
                Activity.HIDE
        );
    }

    private static void forceMoveFreely(
            VillagerEntityMCA villager
    ) {
        MoveState current =
                villager.getVillagerBrain()
                        .getMoveState();

        ORIGINAL_MOVE_STATES.putIfAbsent(
                villager.getUUID(),
                current
        );

        if (current != MoveState.MOVE) {
            villager.getVillagerBrain()
                    .setMoveState(
                            MoveState.MOVE,
                            null
                    );
        }
    }

    private static BlockPos findMeetingPoint(
            ServerLevel level,
            Village village
    ) {
        BlockPos center =
                new BlockPos(
                        village.getCenter()
                );

        return level.getPoiManager()
                .findClosest(
                        holder ->
                                holder.is(
                                        PoiTypes.MEETING
                                ),
                        center,
                        BELL_SEARCH_RADIUS,
                        PoiManager.Occupancy.ANY
                )
                .orElse(null);
    }

    private static boolean hasPlayerInside(
            ServerLevel level,
            Village village
    ) {
        for (ServerPlayer player :
                level.players()) {
            if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && village.isWithinBorder(player)) {
                return true;
            }
        }

        return false;
    }

    private static void clearCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        if (campaignId == null) {
            return;
        }

        Set<UUID> responders =
                CAMPAIGN_RESPONDERS.remove(
                        campaignId
                );

        UUID ringerId =
                CAMPAIGN_RINGERS.remove(
                        campaignId
                );

        Set<UUID> affected =
                new HashSet<>();

        if (responders != null) {
            affected.addAll(responders);
        }

        if (ringerId != null) {
            affected.add(ringerId);
        }

        for (UUID villagerId : affected) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            villagerId
                    )
                    instanceof VillagerEntityMCA villager) {
                Brain<VillagerEntityMCA> brain =
                        villager.getMCABrain();

                brain.eraseMemory(
                        MemoryModuleType
                                .HEARD_BELL_TIME
                );

                brain.eraseMemory(
                        MemoryModuleType.WALK_TARGET
                );

                brain.eraseMemory(
                        MemoryModuleType.LOOK_TARGET
                );

                if (brain.isActive(
                        Activity.HIDE
                )) {
                    brain.setActiveActivityIfPossible(
                            Activity.IDLE
                    );
                }

                MoveState original =
                        ORIGINAL_MOVE_STATES.remove(
                                villagerId
                        );

                if (original != null
                        && villager.getVillagerBrain()
                        .getMoveState() != original) {
                    villager.getVillagerBrain()
                            .setMoveState(
                                    original,
                                    null
                            );
                }

                villager.getNavigation().stop();
            } else {
                ORIGINAL_MOVE_STATES.remove(
                        villagerId
                );
            }
        }

        RUNG_CAMPAIGNS.remove(
                campaignId
        );
    }

    public static void clearRuntimeState() {
        CAMPAIGN_RINGERS.clear();
        RUNG_CAMPAIGNS.clear();
        CAMPAIGN_RESPONDERS.clear();
        ORIGINAL_MOVE_STATES.clear();
    }

}