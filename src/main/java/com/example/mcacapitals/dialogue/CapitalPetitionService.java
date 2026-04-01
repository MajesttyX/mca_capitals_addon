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
import com.example.mcacapitals.network.ModNetwork;
import com.example.mcacapitals.network.OpenBetrothalSelectionPacket;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCARelationshipBridge;
import com.example.mcacapitals.util.MCAReputationBridge;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalPetitionService {

    public static final String PETITION_THRONE = "mcacapitals_petition_throne";
    public static final String PETITION_SEIZE_THRONE = "mcacapitals_seize_throne";
    public static final String PETITION_COMMANDER = "mcacapitals_petition_commander";
    public static final String PETITION_NOBLE_LORD = "mcacapitals_petition_noble_lord";
    public static final String PETITION_NOBLE_DUKE = "mcacapitals_petition_noble_duke";
    public static final String PETITION_BETROTHAL = "mcacapitals_petition_betrothal";
    public static final String PETITION_BETROTHAL_RECOMMEND = "mcacapitals_petition_betrothal_recommend";

    private static final int THRONE_PETITION_MIN_POPULATION = 35;
    private static final int THRONE_PETITION_MIN_HEARTS = 2500;

    private static final int SEIZURE_MIN_REPUTATION = 1500;
    private static final int SEIZURE_COMMANDER_HEARTS = 200;

    private static final int COMMANDER_PETITION_MIN_POPULATION = 30;
    private static final int COMMANDER_PETITION_MIN_HEARTS = 1000;

    private static final int LORD_PETITION_MIN_HEARTS = 400;
    private static final int LORD_PETITION_MIN_MASTER_VILLAGERS = 3;

    private static final int DUKE_PETITION_MIN_HEARTS = 1500;
    private static final int DUKE_PETITION_MIN_POPULATION = 30;

    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;
    private static final ResourceLocation COVER_ME_IN_DIAMONDS_ID = new ResourceLocation("minecraft", "story/shiny_gear");

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

        if (PETITION_SEIZE_THRONE.equals(command)) {
            handleSeizeThrone(player, villagerEntity);
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

        if (PETITION_BETROTHAL.equals(command)) {
            handleBetrothalPetition(player, villagerEntity);
            return true;
        }

        if (PETITION_BETROTHAL_RECOMMEND.equals(command)) {
            handleBetrothalPetition(player, villagerEntity);
            return true;
        }

        return false;
    }

    public static boolean handleBetrothalSelection(ServerPlayer player, UUID capitalId, UUID targetId) {
        if (player == null || capitalId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That capital can no longer hear this petition."));
            return false;
        }

        if (capital.getVillageId() == null) {
            player.sendSystemMessage(Component.literal("That capital is missing its village record."));
            return false;
        }

        if (!isPlayerBetrothalCandidate(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("Only an eligible teen or adult noble of the capital may be chosen for betrothal."));
            return false;
        }

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promise(player, MCAIntegrationBridge.getEntityByUuid(level, targetId));

        if (!result.success()) {
            player.sendSystemMessage(Component.literal(result.message()));
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String targetName = buildBetrothalCandidateName(level, capital, targetId);

        CapitalChronicleService.addEntry(
                level,
                capital,
                "By royal petition, " + targetName + " was promised to " + player.getName().getString()
                        + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        player.sendSystemMessage(Component.literal(
                "Your petition is accepted. " + targetName + " is now betrothed to you."
        ));
        return true;
    }

    public static boolean handleRecommendedBetrothalSelection(ServerPlayer player, UUID capitalId, UUID firstId, UUID secondId) {
        if (player == null || capitalId == null || firstId == null || secondId == null) {
            return false;
        }

        if (firstId.equals(secondId)) {
            player.sendSystemMessage(Component.literal("You must choose two different villagers for a betrothal recommendation."));
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That capital can no longer hear this petition."));
            return false;
        }

        if (capital.getVillageId() == null) {
            player.sendSystemMessage(Component.literal("That capital is missing its village record."));
            return false;
        }

        if (!isRecommendedBetrothalCandidate(level, capital, firstId) || !isRecommendedBetrothalCandidate(level, capital, secondId)) {
            player.sendSystemMessage(Component.literal("Only residents of the capital may be named in a betrothal recommendation."));
            return false;
        }

        Entity firstVillager = MCAIntegrationBridge.getEntityByUuid(level, firstId);
        Entity secondVillager = MCAIntegrationBridge.getEntityByUuid(level, secondId);

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promiseVillagerToVillager(firstVillager, secondVillager);

        if (!result.success()) {
            player.sendSystemMessage(Component.literal(result.message()));
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String firstName = buildBetrothalCandidateName(level, capital, firstId);
        String secondName = buildBetrothalCandidateName(level, capital, secondId);

        CapitalChronicleService.addEntry(
                level,
                capital,
                "By royal recommendation, " + firstName + " was promised to " + secondName
                        + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        player.sendSystemMessage(Component.literal(
                "Your recommendation is accepted. " + firstName + " and " + secondName + " are now betrothed."
        ));
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

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
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

    private static void handleSeizeThrone(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueLineAndClose(player, villagerEntity, "Only a reigning sovereign may be challenged for the throne.");
            return;
        }

        if (capital.getVillageId() == null) {
            sendDialogueLineAndClose(player, villagerEntity, "This capital is missing its village record.");
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to attempt a seizure of the throne.");
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int reputation = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (reputation < SEIZURE_MIN_REPUTATION) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueLineAndClose(player, villagerEntity, "Your bid for the throne fails. The capital turns against you for your insolence.");
            return;
        }

        if (!hasCoverMeInDiamonds(player)) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueLineAndClose(player, villagerEntity, "Your bid for the throne fails. You have not yet proven the might expected of a usurper.");
            return;
        }

        boolean playerIsCommander = PlayerCapitalTitleService.isCommander(level, capital, player.getUUID());
        boolean commanderAligned = hasCommanderAllegiance(level, capital, player.getUUID());

        if (!playerIsCommander && !commanderAligned) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueLineAndClose(player, villagerEntity, "Your bid for the throne fails. Neither the Commander nor the army stands with you.");
            return;
        }

        performCoup(level, capital, player);

        sendDialogueLineAndClose(
                player,
                villagerEntity,
                "Your coup succeeds. The throne of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                        + " is now yours."
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

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueLineAndClose(player, villagerEntity, "You must stand before the sovereign to present this petition.");
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
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
            sendDialogueLineAndClose(player, villagerEntity, "You already hold the office of Commander of the Royal Army.");
            return;
        }

        boolean appointed = CapitalCommanderService.appointPlayerCommander(level, capital, player);
        if (!appointed) {
            sendDialogueLineAndClose(player, villagerEntity, "The office of Commander of the Royal Army could not be reassigned.");
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

    private static void handleBetrothalPetition(ServerPlayer player, Entity villagerEntity) {
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

        List<OpenBetrothalSelectionPacket.Candidate> playerCandidates = collectPlayerBetrothalCandidates(level, capital);
        List<OpenBetrothalSelectionPacket.Candidate> recommendationCandidates = collectRecommendedBetrothalCandidates(level, capital);

        MCACapitals.LOGGER.info(
                "[MCACapitals] Betrothal petition candidates found. playerCandidates={}, recommendationCandidates={}",
                playerCandidates.size(),
                recommendationCandidates.size()
        );

        if (playerCandidates.isEmpty() && recommendationCandidates.size() < 2) {
            sendDialogueLineAndClose(
                    player,
                    villagerEntity,
                    "There is no eligible match in this capital who may presently be named in a betrothal petition."
            );
            return;
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenBetrothalSelectionPacket(
                        capital.getCapitalId(),
                        MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                        playerCandidates,
                        recommendationCandidates
                )
        );

        MCACapitals.LOGGER.info("[MCACapitals] Sent betrothal selection packet to player={}", player.getName().getString());
    }

    private static List<OpenBetrothalSelectionPacket.Candidate> collectPlayerBetrothalCandidates(ServerLevel level, CapitalRecord capital) {
        List<OpenBetrothalSelectionPacket.Candidate> result = new ArrayList<>();
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID residentId : residents) {
            if (!isPlayerBetrothalCandidate(level, capital, residentId)) {
                continue;
            }
            result.add(new OpenBetrothalSelectionPacket.Candidate(
                    residentId,
                    buildBetrothalCandidateName(level, capital, residentId)
            ));
        }

        result.sort(Comparator.comparing(OpenBetrothalSelectionPacket.Candidate::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static List<OpenBetrothalSelectionPacket.Candidate> collectRecommendedBetrothalCandidates(ServerLevel level, CapitalRecord capital) {
        List<OpenBetrothalSelectionPacket.Candidate> result = new ArrayList<>();
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID residentId : residents) {
            if (!isRecommendedBetrothalCandidate(level, capital, residentId)) {
                continue;
            }
            result.add(new OpenBetrothalSelectionPacket.Candidate(
                    residentId,
                    buildBetrothalCandidateName(level, capital, residentId)
            ));
        }

        result.sort(Comparator.comparing(OpenBetrothalSelectionPacket.Candidate::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static boolean isPlayerBetrothalCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(level, entityId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(level, entityId)) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getConsort())
                || entityId.equals(capital.getDowager())
                || entityId.equals(capital.getCommander())) {
            return false;
        }

        String title = CapitalTitleResolver.getDisplayTitle(level, capital, entityId);
        return "Lord".equals(title)
                || "Lady".equals(title)
                || "Duke".equals(title)
                || "Duchess".equals(title)
                || "Prince".equals(title)
                || "Princess".equals(title)
                || "Heir Apparent".equals(title);
    }

    private static boolean isRecommendedBetrothalCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(level, entityId)) {
            return false;
        }

        return !entityId.equals(capital.getSovereign());
    }

    private static String buildBetrothalCandidateName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        String displayTitle = CapitalTitleResolver.getDisplayTitle(level, capital, entityId);

        if (displayTitle == null || displayTitle.isBlank() || "Commoner".equals(displayTitle) || "None".equals(displayTitle)) {
            return baseName;
        }

        if (baseName.startsWith(displayTitle + " ")) {
            return baseName;
        }

        return displayTitle + " " + baseName;
    }

    private static boolean hasCommanderAllegiance(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null) {
            return false;
        }

        UUID commanderId = capital.getCommander();
        if (commanderId == null) {
            return false;
        }

        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(level, commanderId, playerId);
        return hearts >= SEIZURE_COMMANDER_HEARTS;
    }

    private static boolean hasCoverMeInDiamonds(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(COVER_ME_IN_DIAMONDS_ID);
        if (advancement == null) {
            return false;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        return progress.isDone();
    }

    private static void performCoup(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        UUID formerSovereignId = capital.getSovereign();
        String formerSovereignName = resolveLoadedName(level, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        Set<UUID> formerRoyalFamily = collectRoyalFamily(capital);
        formerRoyalFamily.remove(player.getUUID());

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        stripFormerRoyalFamily(capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);

        applyHeartsPenaltyToFamily(level, formerRoyalFamily, player.getUUID(), -200);

        CapitalChronicleService.addEntry(
                level,
                capital,
                playerName + " seized the throne of " + villageName
                        + " from " + formerSovereignName + " by force."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static Set<UUID> collectRoyalFamily(CapitalRecord capital) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (capital == null) {
            return ids;
        }

        if (capital.getSovereign() != null) ids.add(capital.getSovereign());
        if (capital.getConsort() != null) ids.add(capital.getConsort());
        if (capital.getDowager() != null) ids.add(capital.getDowager());
        if (capital.getHeir() != null) ids.add(capital.getHeir());

        ids.addAll(capital.getRoyalChildren());
        ids.addAll(capital.getDisinheritedRoyalChildren());
        ids.addAll(capital.getLegitimizedRoyalChildren());

        return ids;
    }

    private static void stripFormerRoyalFamily(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setDowager(null);
        capital.setDowagerFemale(false);
        capital.setHeir(null);
        capital.setHeirFemale(false);
        capital.setHeirMode(CapitalRecord.HeirMode.NONE);

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();
        capital.getDisinheritedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildFemale().clear();
        capital.getRoyalSuccessionOrder().clear();
    }

    private static void applyCapitalPenalty(ServerLevel level, Set<UUID> residents, UUID playerId, int delta) {
        if (level == null || residents == null || playerId == null) {
            return;
        }

        for (UUID residentId : residents) {
            adjustHearts(level, residentId, playerId, delta);
        }
    }

    private static void applyHeartsPenaltyToFamily(ServerLevel level, Set<UUID> familyIds, UUID playerId, int delta) {
        if (level == null || familyIds == null || playerId == null) {
            return;
        }

        for (UUID familyId : familyIds) {
            adjustHearts(level, familyId, playerId, delta);
        }
    }

    private static void adjustHearts(ServerLevel level, UUID villagerId, UUID playerId, int delta) {
        try {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, villagerId);
            if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
                return;
            }

            Object brain = entity.getClass().getMethod("getVillagerBrain").invoke(entity);
            if (brain == null) {
                return;
            }

            Object memoriesObj = brain.getClass().getMethod("getMemories").invoke(brain);
            if (!(memoriesObj instanceof Map<?, ?> memories)) {
                return;
            }

            Object memory = memories.get(playerId);
            if (memory == null) {
                return;
            }

            Object heartsObj = memory.getClass().getMethod("getHearts").invoke(memory);
            int currentHearts = heartsObj instanceof Integer i ? i : 0;
            int newHearts = currentHearts + delta;

            try {
                Method setter = memory.getClass().getMethod("setHearts", int.class);
                setter.invoke(memory, newHearts);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Field heartsField = memory.getClass().getDeclaredField("hearts");
                heartsField.setAccessible(true);
                heartsField.setInt(memory, newHearts);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to adjust hearts for villager='{}' player='{}' ({})",
                    villagerId,
                    playerId,
                    t.toString()
            );
        }
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