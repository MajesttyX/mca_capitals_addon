package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalNaturalEnemyDiscoverySavedData;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalExileDiscoveryHandler {

    private static final int TICK_INTERVAL =
            20 * 30;

    private static final int
            NATURAL_DISCOVERY_CHANCE_PERCENT =
            10;

    private static final long
            NATURAL_DISCOVERY_COOLDOWN_DAYS =
            4L;

    private static final double
            CAPITAL_RADIUS_SQR =
            96.0D * 96.0D;

    @SubscribeEvent
    public void onLevelTick(
            TickEvent.LevelTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level
                instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime()
                % TICK_INTERVAL != 0L) {
            return;
        }

        boolean changed = false;

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            if (tickCapital(
                    level,
                    capital
            )) {
                changed = true;
            }
        }

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private boolean tickCapital(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || capital.getState()
                != CapitalState.ACTIVE
                || !CapitalManager.isCapitalInLevel(capital, level)) {
            return false;
        }

        if (capital.getVillageId() == null
                || capital.getMasterOfLaws()
                == null) {
            return false;
        }

        if (!CapitalMasterOfLawsService
                .hasUnlockedJustice(
                        level,
                        capital
                )) {
            return false;
        }

        long currentDay =
                currentDay(level);

        long lastScanDay =
                CapitalJusticeDataAccess
                        .getLastExileScanDay(
                                level,
                                capital.getCapitalId()
                        );

        if (lastScanDay == currentDay) {
            return false;
        }

        CapitalJusticeDataAccess
                .setLastExileScanDay(
                        level,
                        capital.getCapitalId(),
                        currentDay
                );

        CapitalNaturalEnemyDiscoverySavedData
                discoveryData =
                CapitalNaturalEnemyDiscoverySavedData
                        .get(level);

        long lastDiscoveryDay =
                discoveryData
                        .getLastDiscoveryDay(
                                capital.getCapitalId()
                        );

        if (lastDiscoveryDay
                != Long.MIN_VALUE
                && currentDay - lastDiscoveryDay
                < NATURAL_DISCOVERY_COOLDOWN_DAYS) {
            return false;
        }

        List<UUID> candidates =
                getEligibleCandidates(
                        level,
                        capital
                );

        if (candidates.isEmpty()) {
            return false;
        }

        if (level.random.nextInt(100)
                >= NATURAL_DISCOVERY_CHANCE_PERCENT) {
            return false;
        }

        UUID targetId =
                candidates.get(
                        level.random.nextInt(
                                candidates.size()
                        )
                );

        if (!discoverEnemy(
                level,
                capital,
                targetId
        )) {
            return false;
        }

        discoveryData.setLastDiscoveryDay(
                capital.getCapitalId(),
                currentDay
        );

        return true;
    }

    private List<UUID> getEligibleCandidates(
            ServerLevel level,
            CapitalRecord capital
    ) {
        Set<UUID> residents =
                CapitalResidentScanner
                        .scanResidents(
                                level,
                                capital.getCapitalId()
                        );

        List<UUID> candidates =
                new ArrayList<>();

        for (UUID residentId : residents) {
            if (isEligibleForNaturalDiscovery(
                    level,
                    capital,
                    residentId
            )) {
                candidates.add(residentId);
            }
        }

        return candidates;
    }

    private boolean isEligibleForNaturalDiscovery(
            ServerLevel level,
            CapitalRecord capital,
            UUID targetId
    ) {
        if (level == null
                || capital == null
                || targetId == null) {
            return false;
        }

        if (targetId.equals(
                capital.getSovereign()
        )
                || targetId.equals(
                capital.getPlayerSovereignId()
        )
                || targetId.equals(
                capital.getMasterOfLaws()
        )) {
            return false;
        }

        if (CapitalCrownStandingService
                .getStanding(
                        level,
                        capital,
                        targetId
                )
                != CrownStanding.ENEMY_OF_CROWN) {
            return false;
        }

        if (!MCAIntegrationBridge
                .isLoadedAndAlive(
                        level,
                        targetId
                )
                || !MCAIntegrationBridge
                .isTeenOrAdultVillager(
                        level,
                        targetId
                )
                || !MCAIntegrationBridge
                .isMCAVillager(
                        level,
                        targetId
                )) {
            return false;
        }

        if (CapitalJusticeDataAccess
                .hasArrestWarrant(
                        level,
                        capital.getCapitalId(),
                        targetId
                )
                || CapitalJusticeDataAccess
                .isDetainedPrisoner(
                        level,
                        capital.getCapitalId(),
                        targetId
                )
                || MCAExecutionBridge
                .isMarkedForExecution(
                        level,
                        targetId
                )) {
            return false;
        }

        Entity target =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                targetId
                        );

        return target != null
                && isInsideCapital(
                level,
                capital,
                target
        );
    }

    private boolean discoverEnemy(
            ServerLevel level,
            CapitalRecord capital,
            UUID targetId
    ) {
        if (!isEligibleForNaturalDiscovery(
                level,
                capital,
                targetId
        )) {
            return false;
        }

        boolean warrantIssued =
                CapitalJusticeDataAccess
                        .issueArrestWarrant(
                                level,
                                capital.getCapitalId(),
                                targetId
                        );

        if (!warrantIssued) {
            return false;
        }

        String targetName =
                CapitalNameService
                        .resolveDisplayName(
                                level,
                                capital,
                                targetId
                        );

        if (!CapitalCrownJusticeService
                .onCorrectAccusation(
                        level,
                        capital,
                        targetId
                )) {
            return false;
        }

        CapitalResidentScanner.clearCache(level);

        Set<UUID> residents =
                CapitalResidentScanner
                        .scanResidents(
                                level,
                                capital.getCapitalId()
                        );

        CapitalNameService
                .refreshCapitalNames(
                        level,
                        capital,
                        residents
                );

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        Component warrantLine =
                CapitalJusticeText
                        .arrestWarrantIssued(
                                targetName
                        );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.ENEMY_DISCOVERED,
                targetName
        );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.ARREST_WARRANT_ISSUED,
                targetName
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        capital,
                        Component.translatable(
                                "mcacapitals.justice.enemy_discovered.report",
                                targetName
                        )
                );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        capital,
                        warrantLine
                );

        return true;
    }

    private boolean isInsideCapital(
            ServerLevel level,
            CapitalRecord capital,
            Entity target
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null
                || target == null) {
            return false;
        }

        BlockPos center =
                MCAIntegrationBridge
                        .getVillageCenter(
                                level,
                                capital.getVillageId()
                        );

        if (center == null) {
            return false;
        }

        double distance =
                target.distanceToSqr(
                        center.getX() + 0.5D,
                        center.getY() + 0.5D,
                        center.getZ() + 0.5D
                );

        return distance
                <= CAPITAL_RADIUS_SQR;
    }

    private long currentDay(
            ServerLevel level
    ) {
        return Math.max(
                1L,
                level.getDayTime()
                        / 24000L
                        + 1L
        );
    }
}