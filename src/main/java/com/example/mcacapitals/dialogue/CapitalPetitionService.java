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
import net.minecraft.util.RandomSource;
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
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE);
            return false;
        }

        if (!isPlayerBetrothalCandidate(level, capital, targetId)) {
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_INVALID_TARGET);
            return false;
        }

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promise(player, MCAIntegrationBridge.getEntityByUuid(level, targetId));

        if (!result.success()) {
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_FAILED, result.message());
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

        sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_SUCCESS, targetName);
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
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE);
            return false;
        }

        if (!isRecommendedBetrothalCandidate(level, capital, firstId) || !isRecommendedBetrothalCandidate(level, capital, secondId)) {
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_INVALID_TARGET);
            return false;
        }

        Entity firstVillager = MCAIntegrationBridge.getEntityByUuid(level, firstId);
        Entity secondVillager = MCAIntegrationBridge.getEntityByUuid(level, secondId);

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promiseVillagerToVillager(firstVillager, secondVillager);

        if (!result.success()) {
            sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_FAILED, result.message());
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

        sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_SUCCESS, firstName, secondName);
        return true;
    }

    private static void handleThronePetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (capital.getVillageId() == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_MISSING_VILLAGE);
            return;
        }

        if (capital.getSovereign() == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_NO_SOVEREIGN);
            return;
        }

        if (!villagerEntity.getUUID().equals(capital.getSovereign())) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_NOT_REIGNING);
            return;
        }

        if (capital.isPlayerSovereign()) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_PLAYER_HELD);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < THRONE_PETITION_MIN_POPULATION) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.THRONE_POPULATION_TOO_LOW,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                    THRONE_PETITION_MIN_POPULATION
            );
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < THRONE_PETITION_MIN_HEARTS) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.THRONE_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        CapitalRecord existingPlayerCapital = CapitalManager.getCapitalBySovereign(player.getUUID());
        if (existingPlayerCapital != null && !capital.getCapitalId().equals(existingPlayerCapital.getCapitalId())) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_ALREADY_RULES_OTHER);
            return;
        }

        peacefulTransferByPetition(level, capital, player, villagerEntity.getUUID());

        sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.THRONE_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }

    private static void handleSeizeThrone(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_SOVEREIGN);
            return;
        }

        if (capital.getVillageId() == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_MISSING_VILLAGE);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NOT_IN_AUDIENCE);
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int reputation = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (reputation < SEIZURE_MIN_REPUTATION) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_LOW_REPUTATION);
            return;
        }

        if (!hasCoverMeInDiamonds(player)) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_ADVANCEMENT);
            return;
        }

        boolean playerIsCommander = PlayerCapitalTitleService.isCommander(level, capital, player.getUUID());
        boolean commanderAligned = hasCommanderAllegiance(level, capital, player.getUUID());

        if (!playerIsCommander && !commanderAligned) {
            applyCapitalPenalty(level, residents, player.getUUID(), -50);
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_COMMANDER_SUPPORT);
            return;
        }

        performCoup(level, capital, player);

        sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.SEIZE_THRONE_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }

    private static void handleCommanderPetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (capital.getVillageId() == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_MISSING_VILLAGE);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < COMMANDER_PETITION_MIN_POPULATION) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.COMMANDER_POPULATION_TOO_LOW,
                    COMMANDER_PETITION_MIN_POPULATION
            );
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < COMMANDER_PETITION_MIN_HEARTS) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.COMMANDER_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        if (CapitalCommanderService.hasOtherPlayerCommander(level, capital, player.getUUID())) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_ALREADY_GRANTED);
            return;
        }

        UUID currentPlayerCommander = CapitalCommanderService.getPlayerCommander(level, capital);
        if (currentPlayerCommander != null && currentPlayerCommander.equals(player.getUUID())) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_ALREADY_HELD);
            return;
        }

        boolean appointed = CapitalCommanderService.appointPlayerCommander(level, capital, player);
        if (!appointed) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_REASSIGN_FAILED);
            return;
        }

        sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.COMMANDER_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }

    private static void handleLordPetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.LORD_ALREADY_HIGHER);
            return;
        }
        if (currentTitle == NobleTitle.LORD || currentTitle == NobleTitle.LADY) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.LORD_ALREADY_HELD);
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < LORD_PETITION_MIN_HEARTS) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.LORD_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        int masters = countMasterProfessionVillagers(level, residents);
        if (masters < LORD_PETITION_MIN_MASTER_VILLAGERS) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.LORD_NOT_ENOUGH_MASTERS,
                    LORD_PETITION_MIN_MASTER_VILLAGERS
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

        sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.LORD_SUCCESS,
                female ? "Lady" : "Lord",
                villageName
        );
    }

    private static void handleDukePetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.DUKE_ALREADY_HELD);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < DUKE_PETITION_MIN_POPULATION) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.DUKE_POPULATION_TOO_LOW,
                    DUKE_PETITION_MIN_POPULATION
            );
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < DUKE_PETITION_MIN_HEARTS) {
            sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.DUKE_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
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

        sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.DUKE_SUCCESS,
                female ? "Duchess" : "Duke",
                villageName
        );
    }

    private static void handleBetrothalPetition(ServerPlayer player, Entity villagerEntity) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!isAudienceValid(player, villagerEntity)) {
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
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
            sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.BETROTHAL_NO_ELIGIBLE_MATCH);
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

    private static Entity resolveCapitalSpeakerEntity(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return null;
        }
        return MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
    }

    private static void sendCapitalDialogue(ServerPlayer player, ServerLevel level, CapitalRecord capital, CapitalDialogueKey key, Object... args) {
        Entity speaker = resolveCapitalSpeakerEntity(level, capital);
        RandomSource random = level != null ? level.random : RandomSource.create();
        String line = CapitalDialogueLibrary.getRandomLine(key, random, args);

        if (speaker != null) {
            String spokenLine = CapitalDialogueSpeaker.formatVillagerSpeech(speaker, line);
            MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", spokenLine);
            player.sendSystemMessage(Component.literal(spokenLine));
            return;
        }

        MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", line);
        player.sendSystemMessage(Component.literal(line));
    }

    private static void sendDialogueLineAndClose(ServerPlayer player, Entity villagerEntity, String line) {
        String spokenLine = CapitalDialogueSpeaker.formatVillagerSpeech(villagerEntity, line);
        MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", spokenLine);
        player.sendSystemMessage(Component.literal(spokenLine));
        tryStopInteracting(villagerEntity);
    }

    private static void sendDialogueKeyAndClose(ServerPlayer player, Entity villagerEntity, CapitalDialogueKey key, Object... args) {
        String line = CapitalDialogueLibrary.getRandomLine(
                key,
                villagerEntity != null && villagerEntity.level() != null ? villagerEntity.level().random : null,
                args
        );
        sendDialogueLineAndClose(player, villagerEntity, line);
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