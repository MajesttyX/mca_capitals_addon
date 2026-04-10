package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;

public class SovereignMarriageCaptureHandler {

    @SubscribeEvent
    public void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ResourceLocation id = event.getAdvancement() == null ? null : event.getAdvancement().getId();
        if (id == null) {
            return;
        }

        String path = id.getPath();
        if (path == null || !path.toLowerCase().contains("till_death_do_us_part")) {
            return;
        }

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

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        // no state to clear
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