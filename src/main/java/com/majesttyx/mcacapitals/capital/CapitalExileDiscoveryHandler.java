package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalExileDiscoveryHandler {

    private static final int TICK_INTERVAL = 20 * 30;
    private static final double CAPITAL_RADIUS_SQR = 96.0D * 96.0D;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % TICK_INTERVAL != 0L) {
            return;
        }

        boolean changed = false;

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (tickCapital(level, capital)) {
                changed = true;
            }
        }

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private boolean tickCapital(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        if (capital.getVillageId() == null || capital.getMasterOfLaws() == null) {
            return false;
        }

        if (!CapitalMasterOfLawsService.hasUnlockedJustice(level, capital)) {
            return false;
        }

        long currentDay = currentDay(level);
        long lastScanDay = CapitalJusticeDataAccess.getLastExileScanDay(level, capital.getCapitalId());
        if (lastScanDay == currentDay) {
            return false;
        }

        CapitalJusticeDataAccess.setLastExileScanDay(level, capital.getCapitalId(), currentDay);

        boolean changed = false;
        for (UUID candidateId : getKnownStandingCandidates(level, capital)) {
            if (discoverIfExiled(level, capital, candidateId)) {
                changed = true;
            }
        }

        return changed;
    }

    private Set<UUID> getKnownStandingCandidates(ServerLevel level, CapitalRecord capital) {
        Set<UUID> candidates = new LinkedHashSet<>();

        for (Map.Entry<UUID, CrownStanding> entry : capital.getCrownStandings().entrySet()) {
            if (entry.getValue() == CrownStanding.ENEMY_OF_CROWN) {
                candidates.add(entry.getKey());
            }
        }

        candidates.addAll(CapitalResidentScanner.scanResidents(level, capital.getCapitalId()));
        return candidates;
    }

    private boolean discoverIfExiled(ServerLevel level, CapitalRecord capital, UUID candidateId) {
        if (candidateId == null) {
            return false;
        }

        if (CapitalJusticeDataAccess.hasDiscoveredExile(level, capital.getCapitalId(), candidateId)) {
            return false;
        }

        if (CapitalCrownStandingService.getStanding(level, capital, candidateId) != CrownStanding.ENEMY_OF_CROWN) {
            return false;
        }

        if (!MCAIntegrationBridge.isLoadedAndAlive(level, candidateId) || !MCAIntegrationBridge.isTeenOrAdultVillager(level, candidateId)) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, candidateId);
        if (entity == null || !isOutsideCapital(level, capital, entity)) {
            return false;
        }

        CapitalJusticeDataAccess.markDiscoveredExile(level, capital.getCapitalId(), candidateId);

        String targetName = CapitalNameService.resolveDisplayName(level, capital, candidateId);
        String capitalName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        if (capitalName == null || capitalName.isBlank()) {
            capitalName = "the capital";
        }

        CapitalChronicleService.addEntry(
                level,
                capital,
                targetName + " was discovered beyond the bounds of " + capitalName + " and named in the records of exile."
        );

        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.literal("Reports reach the Master of Laws: " + targetName + " has been discovered beyond the capital's bounds.")
        );

        return true;
    }

    private boolean isOutsideCapital(ServerLevel level, CapitalRecord capital, Entity entity) {
        if (level == null || capital == null || capital.getVillageId() == null || entity == null) {
            return false;
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        if (center == null) {
            return false;
        }

        double distance = entity.distanceToSqr(
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D
        );

        return distance > CAPITAL_RADIUS_SQR;
    }

    private long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }
}