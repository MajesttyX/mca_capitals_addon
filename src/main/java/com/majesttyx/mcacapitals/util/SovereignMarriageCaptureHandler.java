package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
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
        capturePlayerSovereignMarriage(level, player);
    }

    public void clear() {
        lastScannedTick.clear();
    }

    private void capturePlayerSovereignMarriage(ServerLevel level, ServerPlayer player) {
        CapitalRecord capital = findCapitalForNewPlayerMarriage(level, player);
        if (capital == null) {
            return;
        }

        UUID sovereignId = capital.getSovereign();
        if (sovereignId == null) {
            return;
        }

        Entity sovereign = MCAIntegrationBridge.getEntityByUuid(level, sovereignId);
        if (sovereign == null || !MCAIntegrationBridge.isMCAVillagerEntity(sovereign)) {
            return;
        }

        if (!MCARelationshipBridge.isActuallyMarried(player, sovereign)) {
            return;
        }

        UUID previousConsort = capital.getConsort();

        capital.setConsort(player.getUUID());
        capital.setConsortFemale(MCAPlayerBridge.isPlayerFemale(level, player));
        capital.setPlayerConsort(true);
        capital.setPlayerConsortId(player.getUUID());
        capital.setPlayerConsortName(player.getGameProfile().getName());

        String sovereignName = sovereign.getName().getString();
        String playerName = player.getGameProfile().getName();

        if (!player.getUUID().equals(previousConsort) && !hasMarriageEntry(capital, sovereignName, playerName)) {
            CapitalChronicleService.addEntry(level, capital, sovereignName + " was married to " + playerName + ".");
        }

        CapitalDataAccess.markDirty(level);
    }

    private static CapitalRecord findCapitalForNewPlayerMarriage(ServerLevel level, ServerPlayer player) {
        Map<UUID, CapitalRecord> capitals = CapitalManager.getAllCapitals();
        for (CapitalRecord capital : capitals.values()) {
            if (capital == null || capital.getSovereign() == null) {
                continue;
            }

            Entity sovereign = MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
            if (sovereign == null || !MCAIntegrationBridge.isMCAVillagerEntity(sovereign)) {
                continue;
            }

            if (MCARelationshipBridge.isActuallyMarried(player, sovereign)) {
                return capital;
            }
        }

        return null;
    }

    private static boolean hasMarriageEntry(CapitalRecord capital, String sovereignName, String playerName) {
        String needle = sovereignName + " was married to " + playerName + ".";
        for (String entry : capital.getChronicleEntries()) {
            if (needle.equals(entry) || entry.endsWith(needle)) {
                return true;
            }
        }
        return false;
    }
}