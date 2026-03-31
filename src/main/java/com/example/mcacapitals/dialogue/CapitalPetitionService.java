package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.capital.CapitalState;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCAReputationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public final class CapitalPetitionService {

    public static final String PETITION_THRONE = "mcacapitals_petition_throne";

    private static final int THRONE_PETITION_MIN_POPULATION = 35;
    private static final int THRONE_PETITION_MIN_HEARTS = 2500;
    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;

    private CapitalPetitionService() {
    }

    public static boolean handleCustomCommand(ServerPlayer player, Entity villagerEntity, String command) {
        MCACapitals.LOGGER.info(
                "[MCACapitals] handleCustomCommand called. command='{}', player='{}', villager='{}'",
                command,
                player != null ? player.getName().getString() : "null",
                villagerEntity != null ? villagerEntity.getName().getString() : "null"
        );

        if (player == null || villagerEntity == null || command == null) {
            return false;
        }

        if (!PETITION_THRONE.equals(command)) {
            return false;
        }

        handleThronePetition(player, villagerEntity);
        return true;
    }

    private static void handleThronePetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueLineAndClose(player, villagerEntity, "Only a reigning sovereign can hear this petition.");
            return;
        }

        if (capital.getVillageId() == null) {
            sendDialogueLineAndClose(player, villagerEntity, "This capital is missing its village record.");
            return;
        }

        if (capital.getSovereign() == null) {
            sendDialogueLineAndClose(player, villagerEntity, "This capital has no reigning sovereign.");
            return;
        }

        if (!villagerEntity.getUUID().equals(capital.getSovereign())) {
            sendDialogueLineAndClose(player, villagerEntity, "That villager is no longer the reigning sovereign.");
            return;
        }

        if (capital.isPlayerSovereign()) {
            sendDialogueLineAndClose(player, villagerEntity, "That throne is already held by a player sovereign.");
            return;
        }

        if (player.distanceToSqr(villagerEntity) > MAX_AUDIENCE_DISTANCE_SQR) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        MCACapitals.LOGGER.info("[MCACapitals] Petition population={}, required={}", population, THRONE_PETITION_MIN_POPULATION);
        if (population < THRONE_PETITION_MIN_POPULATION) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The sovereign will not consider surrendering the throne until the capital reaches a population of "
                            + THRONE_PETITION_MIN_POPULATION + "."
            );
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        MCACapitals.LOGGER.info("[MCACapitals] Petition hearts={}, required={}", hearts, THRONE_PETITION_MIN_HEARTS);
        if (hearts < THRONE_PETITION_MIN_HEARTS) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "You do not yet have the standing to claim the throne of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
            );
            return;
        }

        CapitalRecord existingPlayerCapital = CapitalManager.getCapitalBySovereign(player.getUUID());
        if (existingPlayerCapital != null && !capital.getCapitalId().equals(existingPlayerCapital.getCapitalId())) {
            sendDialogueLineAndClose(player, villagerEntity, "You already rule another capital and cannot petition for this throne.");
            return;
        }

        peacefulTransferByPetition(level, capital, player, villagerEntity.getUUID());

        sendDialogueLineAndClose(
                player,
                villagerEntity,
                "Your petition is accepted. The throne of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + " passes peacefully to you."
        );
    }

    private static CapitalRecord resolveSovereignCapital(ServerLevel level, Entity villagerEntity) {
        if (level == null || villagerEntity == null) {
            return null;
        }

        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, villagerEntity.getUUID());
        if (capital == null) {
            Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerEntity.getUUID());
            capital = CapitalManager.getCapitalByVillageId(villageId);
        }

        if (capital == null) {
            return null;
        }

        if (!villagerEntity.getUUID().equals(capital.getSovereign())) {
            return null;
        }

        if (capital.isPlayerSovereign()) {
            return null;
        }

        return capital;
    }

    private static void peacefulTransferByPetition(ServerLevel level, CapitalRecord capital, ServerPlayer player, UUID formerSovereignId) {
        String formerSovereignName = resolveLoadedName(level, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);

        CapitalChronicleService.addEntry(
                level,
                capital,
                formerSovereignName + " accepted the petition of " + playerName
                        + ", and the throne of " + villageName + " passed peacefully into new hands."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static String resolveLoadedName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }

    private static void sendDialogueLineAndClose(ServerPlayer player, Entity villagerEntity, String line) {
        MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", line);
        player.sendSystemMessage(Component.literal(line));
        tryStopInteracting(villagerEntity);
    }

    private static void tryStopInteracting(Entity villagerEntity) {
        try {
            Method getInteractions = villagerEntity.getClass().getMethod("getInteractions");
            Object interactions = getInteractions.invoke(villagerEntity);
            if (interactions != null) {
                Method stopInteracting = interactions.getClass().getMethod("stopInteracting");
                stopInteracting.invoke(interactions);
            }
        } catch (Throwable t) {
            MCACapitals.LOGGER.warn("[MCACapitals] Failed to stop MCA interaction cleanly", t);
        }
    }
}