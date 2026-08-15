package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.identity.MarriageIdentityRepairService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SovereignMarriageCaptureHandler {

    private static final int SCAN_INTERVAL_TICKS = 100;

    private final Map<UUID, Integer> lastScannedTick = new HashMap<>();

    public void onPlayerTick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        int tickCount = player.tickCount;
        int lastTick = lastScannedTick.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
        if (tickCount - lastTick < SCAN_INTERVAL_TICKS) {
            return;
        }

        lastScannedTick.put(player.getUUID(), tickCount);
        capturePlayerMarriage(level, player);
    }

    public void clear() {
        lastScannedTick.clear();
    }

    private void capturePlayerMarriage(ServerLevel level, ServerPlayer player) {
        UUID spouseId = MCAIntegrationBridge.getSpouse(level, player.getUUID());
        Entity spouse = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, spouseId);
        if (spouse == null || !MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            return;
        }

        CapitalRecord sovereignCapital = findPlayerSovereignCapital(player.getUUID());
        if (sovereignCapital != null) {
            capturePlayerSovereignMarriage(level, player, spouse, sovereignCapital);
            return;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }

            if (MarriageIdentityRepairService.repairPlayerVillagerMarriage(level, player, spouse, capital)) {
                return;
            }
        }
    }

    private void capturePlayerSovereignMarriage(ServerLevel level, ServerPlayer player, Entity spouse, CapitalRecord capital) {
        UUID previousConsort = capital.getConsort();

        capital.setConsort(spouse.getUUID());
        capital.setConsortFemale(MCAIntegrationBridge.isFemale(level, spouse.getUUID()));
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName("");

        MarriageIdentityRepairService.repairPlayerVillagerMarriage(level, player, spouse, capital);

        String sovereignName = player.getGameProfile().getName();
        String spouseName = CapitalChronicleIdentitySnapshot.name(level, capital, spouse.getUUID());

        if (!spouse.getUUID().equals(previousConsort) && !hasMarriageEntry(capital, sovereignName, spouseName)) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.ROYAL_MARRIAGE,
                    sovereignName,
                    spouseName
            );
        }

        CapitalDataAccess.markDirty(level);
    }

    private static CapitalRecord findPlayerSovereignCapital(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && playerId.equals(capital.getPlayerSovereignId())) {
                return capital;
            }
        }

        return null;
    }

    private static boolean hasMarriageEntry(CapitalRecord capital, String sovereignName, String spouseName) {
        return CapitalChronicleService.hasMarriageEvent(
                capital,
                CapitalChronicleEventId.ROYAL_MARRIAGE,
                sovereignName,
                spouseName,
                null
        );
    }
}
