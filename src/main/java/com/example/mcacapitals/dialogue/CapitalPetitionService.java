package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCommanderService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.capital.CapitalState;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
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
    public static final String PETITION_COMMANDER = "mcacapitals_petition_commander";
    public static final String PETITION_NOBLE_LORD = "mcacapitals_petition_noble_lord";
    public static final String PETITION_NOBLE_DUKE = "mcacapitals_petition_noble_duke";

    private static final int THRONE_PETITION_MIN_POPULATION = 35;
    private static final int THRONE_PETITION_MIN_HEARTS = 2500;

    private static final int COMMANDER_PETITION_MIN_POPULATION = 30;
    private static final int COMMANDER_PETITION_MIN_HEARTS = 1000;

    private static final int LORD_PETITION_MIN_HEARTS = 400;
    private static final int LORD_PETITION_MIN_MASTER_VILLAGERS = 3;

    private static final int DUKE_PETITION_MIN_HEARTS = 1500;
    private static final int DUKE_PETITION_MIN_POPULATION = 30;

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

        if (PETITION_THRONE.equals(command)) {
            handleThronePetition(player, villagerEntity);
            return true;
        }

        if (PETITION_COMMANDER.equals(command)) {
            handleCommanderPetition(player, villagerEntity);
            return true;
        }

        if (PETITION_NOBLE_LORD.equals(command)) {
            handleLordPetition(player, villagerEntity);
            return true;
        }

        if (PETITION_NOBLE_DUKE.equals(command)) {
            handleDukePetition(player, villagerEntity);
            return true;
        }

        return false;
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

    private static void handleCommanderPetition(ServerPlayer player, Entity villagerEntity) {
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

        if (player.distanceToSqr(villagerEntity) > MAX_AUDIENCE_DISTANCE_SQR) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        MCACapitals.LOGGER.info("[MCACapitals] Commander petition population={}, required={}", population, COMMANDER_PETITION_MIN_POPULATION);
        if (population < COMMANDER_PETITION_MIN_POPULATION) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The office of Commander of the Royal Army will not be granted until the capital reaches a population of "
                            + COMMANDER_PETITION_MIN_POPULATION + "."
            );
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        MCACapitals.LOGGER.info("[MCACapitals] Commander petition hearts={}, required={}", hearts, COMMANDER_PETITION_MIN_HEARTS);
        if (hearts < COMMANDER_PETITION_MIN_HEARTS) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "You do not yet have the standing to be entrusted with command in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
            );
            return;
        }

        if (CapitalCommanderService.hasOtherPlayerCommander(level, capital, player.getUUID())) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The office of Commander of the Royal Army has already been granted and cannot be taken from another sworn player."
            );
            return;
        }

        UUID currentPlayerCommander = CapitalCommanderService.getPlayerCommander(level, capital);
        if (currentPlayerCommander != null && currentPlayerCommander.equals(player.getUUID())) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "You already hold the office of Commander of the Royal Army."
            );
            return;
        }

        boolean appointed = CapitalCommanderService.appointPlayerCommander(level, capital, player);
        if (!appointed) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The office of Commander of the Royal Army could not be reassigned."
            );
            return;
        }

        sendDialogueLineAndClose(
                player,
                villagerEntity,
                "Your petition is accepted. You are now Commander of the Royal Army of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
        );
    }

    private static void handleLordPetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueLineAndClose(player, villagerEntity, "Only a reigning sovereign can hear this petition.");
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            sendDialogueLineAndClose(player, villagerEntity, "You already hold a higher noble dignity than Lord or Lady.");
            return;
        }
        if (currentTitle == NobleTitle.LORD || currentTitle == NobleTitle.LADY) {
            sendDialogueLineAndClose(player, villagerEntity, "You already hold the dignity of Lord or Lady.");
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        MCACapitals.LOGGER.info("[MCACapitals] Lord petition hearts={}, required={}", hearts, LORD_PETITION_MIN_HEARTS);
        if (hearts < LORD_PETITION_MIN_HEARTS) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "You have not yet earned the standing required to be raised to the lesser nobility of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
            );
            return;
        }

        int masters = countMasterProfessionVillagers(level, residents);
        MCACapitals.LOGGER.info("[MCACapitals] Lord petition masterVillagers={}, required={}", masters, LORD_PETITION_MIN_MASTER_VILLAGERS);
        if (masters < LORD_PETITION_MIN_MASTER_VILLAGERS) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The sovereign will not grant this petition until at least "
                            + LORD_PETITION_MIN_MASTER_VILLAGERS
                            + " master villagers strengthen the standing of the capital."
            );
            return;
        }

        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);
        NobleTitle granted = female ? NobleTitle.LADY : NobleTitle.LORD;

        revokeCommanderIfNeeded(level, capital, player);

        PlayerCapitalTitleService.grantTitle(level, capital, player.getUUID(), granted);

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        CapitalChronicleService.addEntry(
                level,
                capital,
                player.getName().getString() + " was raised to the dignity of "
                        + (female ? "Lady" : "Lord") + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        sendDialogueLineAndClose(
                player,
                villagerEntity,
                "Your petition is accepted. You are now "
                        + (female ? "Lady" : "Lord") + " of " + villageName + "."
        );
    }

    private static void handleDukePetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueLineAndClose(player, villagerEntity, "Only a reigning sovereign can hear this petition.");
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            sendDialogueLineAndClose(player, villagerEntity, "You already hold the dignity of Duke or Duchess.");
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        MCACapitals.LOGGER.info("[MCACapitals] Duke petition population={}, required={}", population, DUKE_PETITION_MIN_POPULATION);
        if (population < DUKE_PETITION_MIN_POPULATION) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "The sovereign will not grant ducal rank until the capital reaches a population of "
                            + DUKE_PETITION_MIN_POPULATION + "."
            );
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        MCACapitals.LOGGER.info("[MCACapitals] Duke petition hearts={}, required={}", hearts, DUKE_PETITION_MIN_HEARTS);
        if (hearts < DUKE_PETITION_MIN_HEARTS) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "You have not yet earned the standing required to be raised to ducal rank in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
            );
            return;
        }

        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);
        NobleTitle granted = female ? NobleTitle.DUCHESS : NobleTitle.DUKE;

        revokeCommanderIfNeeded(level, capital, player);

        PlayerCapitalTitleService.grantTitle(level, capital, player.getUUID(), granted);

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        CapitalChronicleService.addEntry(
                level,
                capital,
                player.getName().getString() + " was raised to the dignity of "
                        + (female ? "Duchess" : "Duke") + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        sendDialogueLineAndClose(
                player,
                villagerEntity,
                "Your petition is accepted. You are now "
                        + (female ? "Duchess" : "Duke") + " of " + villageName + "."
        );
    }

    private static void revokeCommanderIfNeeded(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        if (!PlayerCapitalTitleService.isCommander(level, capital, player.getUUID())) {
            return;
        }

        PlayerCapitalTitleService.revokeCommander(level, capital, player.getUUID());

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        CapitalChronicleService.addEntry(
                level,
                capital,
                player.getName().getString() + " relinquished the office of Commander of the Royal Army of "
                        + villageName + " upon being raised to the nobility."
        );
    }

    private static boolean isAudienceValid(ServerPlayer player, Entity villagerEntity) {
        return player != null
                && villagerEntity != null
                && player.distanceToSqr(villagerEntity) <= MAX_AUDIENCE_DISTANCE_SQR;
    }

    private static int countMasterProfessionVillagers(ServerLevel level, Set<UUID> residents) {
        int count = 0;
        for (UUID residentId : residents) {
            if (MCAIntegrationBridge.isMasterProfessionVillager(level, residentId)) {
                count++;
            }
        }
        return count;
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