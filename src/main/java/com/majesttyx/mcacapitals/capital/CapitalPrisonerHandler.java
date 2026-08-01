package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJudgmentType;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalPrisonerHandler {

    private static final int TICK_INTERVAL = 20 * 5;
    private static final long RESPONSE_WINDOW_TICKS = 20L * 120L;
    private static final double CAPITAL_RADIUS_SQR = 96.0D * 96.0D;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
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

        CapitalCrownJusticeService.syncReign(level, capital);

        if (capital.getVillageId() == null
                || capital.getMasterOfLaws() == null
                || !CapitalMasterOfLawsService.hasUnlockedJustice(level, capital)) {
            return false;
        }

        List<AABB> prisonBounds = CapitalBuildingService.getPrisonBounds(level, capital);
        if (prisonBounds.isEmpty()) {
            return false;
        }

        boolean changed = CapitalCrownJusticeService.tickNpcGovernment(level, capital);
        Set<UUID> warrants = CapitalJusticeDataAccess.getArrestWarrants(level, capital.getCapitalId());

        for (UUID targetId : warrants) {
            if (tickWarrantTarget(level, capital, targetId, prisonBounds)) {
                changed = true;
            }
        }

        return changed;
    }

    private boolean tickWarrantTarget(ServerLevel level, CapitalRecord capital, UUID targetId, List<AABB> prisonBounds) {
        if (targetId == null
                || !MCAIntegrationBridge.isLoadedAndAlive(level, targetId)
                || !MCAIntegrationBridge.isTeenOrAdultVillager(level, targetId)) {
            return false;
        }

        Entity target = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, targetId);
        if (target == null) {
            return false;
        }

        if (CapitalJusticeDataAccess.getJudgment(level, capital.getCapitalId(), targetId)
                == CapitalJudgmentType.EXECUTION) {
            return false;
        }

        if (isOutsideCapital(level, capital, target)) {
            return resolveEscortedOutOfCapital(level, capital, targetId);
        }

        if (isInsideAnyPrisonBound(target, prisonBounds)) {
            return tickPrisonStay(level, capital, targetId);
        }

        if (CapitalJusticeDataAccess.isDetainedPrisoner(level, capital.getCapitalId(), targetId)) {
            return false;
        }

        return tickMissedResponseWindow(level, capital, targetId);
    }

    private boolean tickPrisonStay(ServerLevel level, CapitalRecord capital, UUID targetId) {
        long currentDay = currentDay(level);
        boolean newlyDetained = CapitalJusticeDataAccess.markDetainedPrisoner(
                level,
                capital.getCapitalId(),
                targetId,
                currentDay
        );

        if (newlyDetained) {
            String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    targetName + " was delivered to prison and now awaits the Crown's judgment."
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    capital,
                    Component.literal(targetName + " has been delivered to prison and is awaiting judgment.")
            );
            return true;
        }

        CapitalJudgmentType judgment = CapitalJusticeDataAccess.getJudgment(
                level,
                capital.getCapitalId(),
                targetId
        );

        if (judgment == CapitalJudgmentType.IMPRISONMENT) {
            return CapitalCrownJusticeService.completeSentence(level, capital, targetId);
        }

        return false;
    }

    private boolean tickMissedResponseWindow(ServerLevel level, CapitalRecord capital, UUID targetId) {
        long issuedGameTime = CapitalJusticeDataAccess.getArrestWarrantIssuedGameTime(
                level,
                capital.getCapitalId(),
                targetId
        );

        if (issuedGameTime == Long.MIN_VALUE) {
            CapitalJusticeDataAccess.issueArrestWarrant(level, capital.getCapitalId(), targetId);
            return true;
        }
        if (level.getGameTime() - issuedGameTime < RESPONSE_WINDOW_TICKS) {
            return false;
        }

        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        boolean marked = MCAExecutionBridge.markForExecution(level, targetId);
        CapitalJusticeDataAccess.clearJusticeCase(
                level,
                capital.getCapitalId(),
                targetId
        );

        if (marked) {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    targetName + " failed to answer an Arrest Warrant and was marked for execution."
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    capital,
                    Component.literal(targetName + " failed to answer an Arrest Warrant and has been marked for execution.")
            );
        } else {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    targetName + " failed to answer an Arrest Warrant, but the execution mark could not be applied."
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    capital,
                    Component.literal(targetName + " failed to answer an Arrest Warrant, but the execution mark could not be applied.")
            );
        }

        return true;
    }

    private boolean resolveEscortedOutOfCapital(ServerLevel level, CapitalRecord capital, UUID targetId) {
        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        boolean exiled = CapitalAsylumService.markExiled(level, capital, targetId);

        CapitalJusticeDataAccess.clearJusticeCase(level, capital.getCapitalId(), targetId);
        CapitalJusticeDataAccess.setLastResolvedDay(level, capital.getCapitalId(), targetId, currentDay(level));

        if (exiled) {
            String capitalName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
            if (capitalName == null || capitalName.isBlank()) {
                capitalName = "the capital";
            }

            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    targetName + " was led beyond the capital's bounds, stripped of residence, and escaped into exile from " + capitalName + "."
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    capital,
                    Component.literal(targetName + " has escaped beyond the capital's bounds and is now an exile.")
            );
        } else {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    targetName + " was led beyond the capital's bounds and escaped the Crown's warrant, but exile status could not be recorded."
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    capital,
                    Component.literal(targetName + " escaped beyond the capital's bounds, but exile status could not be recorded.")
            );
        }

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

        return entity.distanceToSqr(
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D
        ) > CAPITAL_RADIUS_SQR;
    }

    private boolean isInsideAnyPrisonBound(Entity entity, List<AABB> prisonBounds) {
        if (entity == null || prisonBounds == null || prisonBounds.isEmpty()) {
            return false;
        }

        for (AABB bounds : prisonBounds) {
            if (bounds != null && bounds.inflate(1.5D).contains(entity.position())) {
                return true;
            }
        }
        return false;
    }

    private long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }
}