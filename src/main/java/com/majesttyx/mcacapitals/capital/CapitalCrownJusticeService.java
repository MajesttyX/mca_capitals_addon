package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJudgmentType;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalCrownJusticeService {

    public static final String DIALOGUE_COMMAND = "mcacapitals_review_crown_justice";

    private static final long RESTORATION_WAIT_DAYS = 5L;
    private static final long IMPRISONMENT_DAYS = 2L;
    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;

    private CapitalCrownJusticeService() {
    }

    public static boolean onCorrectAccusation(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return false;
        }

        CapitalJusticeDataAccess.setPublicStatus(
                level,
                capital.getCapitalId(),
                targetId,
                CapitalPublicCrownStatus.DISCOVERED_ENEMY
        );
        CapitalJusticeDataAccess.incrementConfirmedCaseCount(level, capital.getCapitalId(), targetId);
        removeTrustedOffice(level, capital, targetId);
        CapitalDataAccess.markDirty(level);
        return true;
    }

    public static void syncReign(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return;
        }

        UUID currentSovereign = currentSovereignId(capital);
        UUID recordedSovereign = CapitalJusticeDataAccess.getPublicStatusSovereign(level, capital.getCapitalId());

        if (sameUuid(currentSovereign, recordedSovereign)) {
            return;
        }

        CapitalJusticeDataAccess.clearResolvedPublicStatuses(level, capital.getCapitalId());
        CapitalJusticeDataAccess.setPublicStatusSovereign(level, capital.getCapitalId(), currentSovereign);
    }

    public static CapitalPublicCrownStatus getPublicStatus(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return null;
        }
        return CapitalJusticeDataAccess.getPublicStatus(level, capital.getCapitalId(), targetId);
    }

    public static boolean isDiscoveredEnemy(ServerLevel level, CapitalRecord capital, UUID targetId) {
        return getPublicStatus(level, capital, targetId) == CapitalPublicCrownStatus.DISCOVERED_ENEMY;
    }

    public static boolean isRecognizedFriend(ServerLevel level, CapitalRecord capital, UUID targetId) {
        return getPublicStatus(level, capital, targetId) == CapitalPublicCrownStatus.RECOGNIZED_FRIEND;
    }

    public static boolean isTrustedOfficeEligible(ServerLevel level, CapitalRecord capital, UUID targetId) {
        return !isDiscoveredEnemy(level, capital, targetId)
                && !CapitalJusticeDataAccess.hasArrestWarrant(level, capital.getCapitalId(), targetId)
                && !CapitalJusticeDataAccess.isDetainedPrisoner(level, capital.getCapitalId(), targetId)
                && !MCAExecutionBridge.isMarkedForExecution(level, targetId);
    }

    public static int naturalElevationWeight(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (!isTrustedOfficeEligible(level, capital, targetId)) {
            return 0;
        }
        return isRecognizedFriend(level, capital, targetId) ? 3 : 1;
    }

    public static boolean recognizeFriend(ServerPlayer player, UUID masterOfLawsId, UUID targetId) {
        if (player == null || masterOfLawsId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLawsId);
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, masterOfLawsId);

        if (!validateMasterAudience(player, capital, master)) {
            return false;
        }
        if (!CapitalResidentScanner.scanResidents(level, capital.getCapitalId()).contains(targetId)) {
            player.sendSystemMessage(Component.literal("That villager is not a resident of this capital."));
            return false;
        }
        if (!CapitalCrownStandingService.isFriend(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That villager has not privately declared loyalty to this Crown."));
            return false;
        }
        if (MCAIntegrationBridge.getHeartsWithPlayer(level, targetId, player.getUUID()) < 200) {
            player.sendSystemMessage(Component.literal("That villager has not yet trusted you enough to declare their loyalty openly."));
            return false;
        }
        if (!isTrustedOfficeEligible(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("A villager with an active Crown case cannot be formally recognized as a Friend of the Crown."));
            return false;
        }

        CapitalJusticeDataAccess.setPublicStatus(level, capital.getCapitalId(), targetId, CapitalPublicCrownStatus.RECOGNIZED_FRIEND);
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalChronicleService.addEntry(level, capital, name + " was formally recognized as a Friend of the Crown by " + player.getName().getString() + ".");
        player.sendSystemMessage(Component.literal(name + " is now a Recognized Friend of the Crown."));
        return true;
    }

    public static boolean restoreToPeace(ServerPlayer player, UUID masterOfLawsId, UUID targetId) {
        if (player == null || masterOfLawsId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLawsId);
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, masterOfLawsId);

        if (!validateMasterAudience(player, capital, master)) {
            return false;
        }
        if (!isRestorationEligible(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That villager is not eligible to be restored to the Crown's Peace."));
            return false;
        }

        CapitalJusticeDataAccess.setPublicStatus(level, capital.getCapitalId(), targetId, CapitalPublicCrownStatus.RESTORED_TO_PEACE);
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalChronicleService.addEntry(level, capital, name + " was restored to the Crown's Peace by " + player.getName().getString() + ".");
        player.sendSystemMessage(Component.literal(name + " has been restored to the Crown's Peace."));
        return true;
    }

    public static boolean isRestorationEligible(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();
        if (CapitalJusticeDataAccess.getPublicStatus(level, capitalId, targetId) != CapitalPublicCrownStatus.DISCOVERED_ENEMY) {
            return false;
        }
        if (!CapitalResidentScanner.scanResidents(level, capitalId).contains(targetId)) {
            return false;
        }
        if (CapitalJusticeDataAccess.hasArrestWarrant(level, capitalId, targetId)
                || CapitalJusticeDataAccess.isDetainedPrisoner(level, capitalId, targetId)
                || CapitalJusticeDataAccess.hasDiscoveredExile(level, capitalId, targetId)
                || MCAExecutionBridge.isMarkedForExecution(level, targetId)) {
            return false;
        }

        long resolvedDay = CapitalJusticeDataAccess.getLastResolvedDay(level, capitalId, targetId);
        return resolvedDay != Long.MIN_VALUE && currentDay(level) - resolvedDay >= RESTORATION_WAIT_DAYS;
    }

    public static boolean canShowDialogueAnswer(ServerPlayer player, Entity masterOfLaws) {
        if (player == null || masterOfLaws == null) {
            return false;
        }

        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLaws.getUUID());
        return capital != null
                && capital.getState() == CapitalState.ACTIVE
                && player.distanceToSqr(masterOfLaws) <= MAX_AUDIENCE_DISTANCE_SQR
                && mayDecide(player.serverLevel(), capital, player.getUUID());
    }

    public static int openReview(ServerPlayer player, UUID masterOfLawsId) {
        if (player == null || masterOfLawsId == null) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLawsId);
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, masterOfLawsId);

        if (!validateMasterAudience(player, capital, master)) {
            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();
        UUID capitalId = capital.getCapitalId();

        for (UUID prisonerId : CapitalJusticeDataAccess.getDetainedPrisoners(level, capitalId)) {
            if (CapitalJusticeDataAccess.getJudgment(level, capitalId, prisonerId) != null) {
                continue;
            }

            String name = CapitalNameService.resolveDisplayName(level, capital, prisonerId);
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    name,
                    "Awaiting Judgment",
                    "Confirmed cases: " + CapitalJusticeDataAccess.getConfirmedCaseCount(level, capitalId, prisonerId),
                    publicStatusLine(level, capital, prisonerId),
                    "Decide Judgment",
                    "/capitaljustice options " + masterOfLawsId + " " + prisonerId,
                    true,
                    ""
            ));
        }

        for (UUID residentId : CapitalResidentScanner.scanResidents(level, capitalId)) {
            CapitalPublicCrownStatus publicStatus = CapitalJusticeDataAccess.getPublicStatus(level, capitalId, residentId);
            String name = CapitalNameService.resolveDisplayName(level, capital, residentId);

            if (isRestorationEligible(level, capital, residentId)) {
                entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                        name,
                        "Eligible for Restoration",
                        "Their sentence or pardon was resolved at least five Minecraft days ago.",
                        "Discovered Enemy of the Crown",
                        "Restore Crown's Peace",
                        "/capitaljustice restore " + masterOfLawsId + " " + residentId,
                        true,
                        ""
                ));
                continue;
            }

            if (publicStatus == null
                    && CapitalCrownStandingService.isFriend(level, capital, residentId)
                    && MCAIntegrationBridge.getHeartsWithPlayer(level, residentId, player.getUUID()) >= 200
                    && isTrustedOfficeEligible(level, capital, residentId)) {
                entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                        name,
                        "Declared Loyalty",
                        "This villager has openly declared support for the current Crown.",
                        "Not yet formally recognized",
                        "Recognize Friend",
                        "/capitaljustice recognize " + masterOfLawsId + " " + residentId,
                        true,
                        ""
                ));
            }
        }

        entries.sort(Comparator.comparing(OpenAmbassadorCommunicationPacket.Entry::lineOne)
                .thenComparing(OpenAmbassadorCommunicationPacket.Entry::heading, String.CASE_INSENSITIVE_ORDER));

        ModNetwork.sendToPlayer(player, new OpenAmbassadorCommunicationPacket(
                OpenAmbassadorCommunicationPacket.Mode.JUSTICE_CASES,
                "Crown Justice and Standing",
                master.getName().getString(),
                entries.isEmpty()
                        ? "No judgments, recognitions, or restorations currently require the Crown's decision."
                        : "Review prisoners awaiting judgment and villagers eligible for formal Crown standing.",
                "",
                entries,
                List.of()
        ));
        MCAIntegrationBridge.stopInteracting(master);
        return 1;
    }

    public static int openJudgmentOptions(ServerPlayer player, UUID masterOfLawsId, UUID targetId) {
        if (player == null || masterOfLawsId == null || targetId == null) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLawsId);
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, masterOfLawsId);

        if (capital == null || master == null || player.distanceToSqr(master) > MAX_AUDIENCE_DISTANCE_SQR || !mayDecide(level, capital, player.getUUID())) {
            player.sendSystemMessage(Component.literal("You no longer have the authority or audience required to decide this case."));
            return 0;
        }
        if (!isAwaitingJudgment(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That prisoner is no longer awaiting judgment."));
            return 0;
        }

        String base = "/capitaljustice judge " + masterOfLawsId + " " + targetId + " ";
        List<OpenAmbassadorCommunicationPacket.Action> actions = List.of(
                new OpenAmbassadorCommunicationPacket.Action("Royal Pardon", "Release the prisoner from this case without restoring political trust.", base + "pardon", true),
                new OpenAmbassadorCommunicationPacket.Action("Two-Day Imprisonment", "The prisoner serves two Minecraft days before release.", base + "imprisonment", true),
                new OpenAmbassadorCommunicationPacket.Action("Exile", "Remove residency and declare the prisoner an exile of this capital.", base + "exile", true),
                new OpenAmbassadorCommunicationPacket.Action("Mark for Execution", "Condemn the prisoner under MCA's execution system.", base + "execution", true)
        );

        ModNetwork.sendToPlayer(player, new OpenAmbassadorCommunicationPacket(
                OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                "Judgment: " + CapitalNameService.resolveDisplayName(level, capital, targetId),
                "Confirmed Enemy of the Crown",
                "Choose the Crown's judgment. The original discovery remains in the Chronicle.",
                "/capitaljustice review " + masterOfLawsId,
                List.of(),
                actions
        ));
        return 1;
    }

    public static int decide(ServerPlayer player, UUID masterOfLawsId, UUID targetId, CapitalJudgmentType judgment) {
        if (player == null || masterOfLawsId == null || targetId == null || judgment == null) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveMasterOfLawsCapital(masterOfLawsId);
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, masterOfLawsId);

        if (capital == null || master == null || player.distanceToSqr(master) > MAX_AUDIENCE_DISTANCE_SQR || !mayDecide(level, capital, player.getUUID())) {
            player.sendSystemMessage(Component.literal("You no longer have the authority or audience required to decide this case."));
            return 0;
        }
        if (!isAwaitingJudgment(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That prisoner is no longer awaiting judgment."));
            return 0;
        }

        return applyJudgment(level, capital, targetId, judgment, player.getName().getString()) ? 1 : 0;
    }

    public static boolean tickNpcGovernment(ServerLevel level, CapitalRecord capital) {
        if (level == null
                || capital == null
                || capital.getState() != CapitalState.ACTIVE
                || capital.getSovereign() == null
                || getPlayerDecisionMaker(level, capital) != null) {
            return false;
        }

        long day = currentDay(level);
        if (CapitalJusticeDataAccess.getLastNpcJudgmentDay(level, capital.getCapitalId()) == day) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();
        List<UUID> awaiting = CapitalJusticeDataAccess.getDetainedPrisoners(level, capitalId)
                .stream()
                .filter(targetId -> CapitalJusticeDataAccess.getJudgment(level, capitalId, targetId) == null)
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        if (!awaiting.isEmpty()) {
            UUID targetId = awaiting.getFirst();
            CapitalJudgmentType judgment = chooseNpcJudgment(level, capital, targetId);
            CapitalJusticeDataAccess.setLastNpcJudgmentDay(level, capitalId, day);
            return applyJudgment(level, capital, targetId, judgment, "the Crown's council");
        }

        List<UUID> restorationCandidates = CapitalResidentScanner.scanResidents(level, capitalId)
                .stream()
                .filter(targetId -> isRestorationEligible(level, capital, targetId))
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        if (!restorationCandidates.isEmpty() && level.random.nextInt(100) < npcRestorationChance(level, capital, restorationCandidates.getFirst())) {
            UUID targetId = restorationCandidates.getFirst();
            CapitalJusticeDataAccess.setPublicStatus(level, capitalId, targetId, CapitalPublicCrownStatus.RESTORED_TO_PEACE);
            CapitalJusticeDataAccess.setLastNpcJudgmentDay(level, capitalId, day);
            String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
            CapitalChronicleService.addEntry(level, capital, name + " was restored to the Crown's Peace by the Crown's council.");
            CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.literal(name + " has been restored to the Crown's Peace."));
            return true;
        }

        List<UUID> friendCandidates = CapitalResidentScanner.scanResidents(level, capitalId)
                .stream()
                .filter(targetId -> CapitalJusticeDataAccess.getPublicStatus(level, capitalId, targetId) == null)
                .filter(targetId -> CapitalCrownStandingService.isFriend(level, capital, targetId))
                .filter(targetId -> isTrustedOfficeEligible(level, capital, targetId))
                .filter(targetId -> MCAIntegrationBridge.isTeenOrAdultVillager(level, targetId))
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        if (!friendCandidates.isEmpty() && level.random.nextInt(100) < 10) {
            UUID targetId = friendCandidates.getFirst();
            CapitalJusticeDataAccess.setPublicStatus(level, capitalId, targetId, CapitalPublicCrownStatus.RECOGNIZED_FRIEND);
            CapitalJusticeDataAccess.setLastNpcJudgmentDay(level, capitalId, day);
            String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
            CapitalChronicleService.addEntry(level, capital, name + " was formally recognized as a Friend of the Crown by the Crown's council.");
            CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.literal(name + " has been recognized as a Friend of the Crown."));
            return true;
        }

        CapitalJusticeDataAccess.setLastNpcJudgmentDay(level, capitalId, day);
        return false;
    }

    public static boolean isAwaitingJudgment(ServerLevel level, CapitalRecord capital, UUID targetId) {
        return level != null
                && capital != null
                && targetId != null
                && CapitalJusticeDataAccess.hasArrestWarrant(level, capital.getCapitalId(), targetId)
                && CapitalJusticeDataAccess.isDetainedPrisoner(level, capital.getCapitalId(), targetId)
                && CapitalJusticeDataAccess.getJudgment(level, capital.getCapitalId(), targetId) == null;
    }

    public static boolean completeSentence(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null
                || CapitalJusticeDataAccess.getJudgment(level, capital.getCapitalId(), targetId) != CapitalJudgmentType.IMPRISONMENT) {
            return false;
        }

        long endDay = CapitalJusticeDataAccess.getSentenceEndDay(level, capital.getCapitalId(), targetId);
        if (endDay == Long.MIN_VALUE || currentDay(level) < endDay) {
            return false;
        }

        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalJusticeDataAccess.clearJusticeCase(level, capital.getCapitalId(), targetId);
        CapitalJusticeDataAccess.setLastResolvedDay(level, capital.getCapitalId(), targetId, currentDay(level));
        CapitalChronicleService.addEntry(level, capital, name + " completed a two-day sentence and was released, while remaining a Discovered Enemy of the Crown.");
        CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.literal(name + " has completed the Crown's sentence and been released."));
        return true;
    }

    public static void recordPardonResolution(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level != null && capital != null && targetId != null) {
            CapitalJusticeDataAccess.setLastResolvedDay(level, capital.getCapitalId(), targetId, currentDay(level));
        }
    }

    private static boolean applyJudgment(ServerLevel level, CapitalRecord capital, UUID targetId, CapitalJudgmentType judgment, String authorityName) {
        UUID capitalId = capital.getCapitalId();
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        String entry;

        switch (judgment) {
            case PARDON -> {
                CapitalJusticeDataAccess.clearJusticeCase(level, capitalId, targetId);
                MCAExecutionBridge.clearExecutionMark(level, targetId);
                CapitalJusticeDataAccess.setLastResolvedDay(level, capitalId, targetId, currentDay(level));
                entry = name + " was granted a Royal Pardon by " + authorityName + ", but the discovery as an Enemy of the Crown remains recorded.";
            }
            case IMPRISONMENT -> {
                CapitalJusticeDataAccess.setJudgment(level, capitalId, targetId, CapitalJudgmentType.IMPRISONMENT);
                CapitalJusticeDataAccess.setSentenceEndDay(level, capitalId, targetId, currentDay(level) + IMPRISONMENT_DAYS);
                entry = name + " was sentenced by " + authorityName + " to two Minecraft days of imprisonment.";
            }
            case EXILE -> {
                if (!CapitalAsylumService.markExiled(level, capital, targetId)) {
                    return false;
                }
                CapitalJusticeDataAccess.clearJusticeCase(level, capitalId, targetId);
                CapitalJusticeDataAccess.setLastResolvedDay(level, capitalId, targetId, currentDay(level));
                entry = name + " was sentenced to exile by " + authorityName + ".";
            }
            case EXECUTION -> {
                if (!MCAExecutionBridge.markForExecution(level, targetId)) {
                    return false;
                }
                CapitalJusticeDataAccess.setJudgment(level, capitalId, targetId, CapitalJudgmentType.EXECUTION);
                entry = name + " was marked for execution by " + authorityName + ".";
            }
            default -> {
                return false;
            }
        }

        CapitalChronicleService.addEntry(level, capital, entry);
        CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.literal(entry));
        return true;
    }

    private static CapitalJudgmentType chooseNpcJudgment(ServerLevel level, CapitalRecord capital, UUID targetId) {
        int cases = CapitalJusticeDataAccess.getConfirmedCaseCount(level, capital.getCapitalId(), targetId);
        int pardon = cases <= 1 ? 15 : cases == 2 ? 5 : 2;
        int prison = cases <= 1 ? 65 : cases == 2 ? 45 : 25;
        int exile = cases <= 1 ? 18 : cases == 2 ? 35 : 38;
        int execution = cases <= 1 ? 2 : cases == 2 ? 15 : 35;

        if (isAtWar(level, capital)) {
            pardon = Math.max(0, pardon - 5);
            exile += 3;
            execution += 2;
        }

        int total = pardon + prison + exile + execution;
        int roll = level.random.nextInt(Math.max(1, total));
        if (roll < pardon) {
            return CapitalJudgmentType.PARDON;
        }
        roll -= pardon;
        if (roll < prison) {
            return CapitalJudgmentType.IMPRISONMENT;
        }
        roll -= prison;
        if (roll < exile) {
            return CapitalJudgmentType.EXILE;
        }
        return CapitalJudgmentType.EXECUTION;
    }

    private static int npcRestorationChance(ServerLevel level, CapitalRecord capital, UUID targetId) {
        int cases = CapitalJusticeDataAccess.getConfirmedCaseCount(level, capital.getCapitalId(), targetId);
        int chance = cases <= 1 ? 20 : cases == 2 ? 8 : 2;
        if (isAtWar(level, capital)) {
            chance = Math.max(0, chance - 10);
        }
        return chance;
    }

    private static boolean validateMasterAudience(ServerPlayer player, CapitalRecord capital, Entity master) {
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.literal("That capital is no longer active."));
            return false;
        }
        if (master == null || player.distanceToSqr(master) > MAX_AUDIENCE_DISTANCE_SQR) {
            player.sendSystemMessage(Component.literal("You must remain near the Master of Laws to make this decision."));
            return false;
        }
        if (!mayDecide(player.serverLevel(), capital, player.getUUID())) {
            player.sendSystemMessage(Component.literal("Only the player Sovereign, or the player Hand serving an NPC Sovereign, may make this decision."));
            return false;
        }
        return true;
    }

    private static boolean mayDecide(ServerLevel level, CapitalRecord capital, UUID playerId) {
        UUID decisionMaker = getPlayerDecisionMaker(level, capital);
        return playerId != null && playerId.equals(decisionMaker);
    }

    private static UUID getPlayerDecisionMaker(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return null;
        }
        if (capital.getPlayerSovereignId() != null) {
            return capital.getPlayerSovereignId();
        }
        if (capital.getSovereign() == null) {
            return null;
        }

        UUID playerHand = PlayerCapitalTitleService.getHandHolder(level, capital);
        return playerHand != null
                && playerHand.equals(capital.getHand())
                && PlayerCapitalTitleService.isHand(level, capital, playerHand)
                ? playerHand
                : null;
    }

    private static CapitalRecord resolveMasterOfLawsCapital(UUID masterOfLawsId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && masterOfLawsId.equals(capital.getMasterOfLaws())) {
                return capital;
            }
        }
        return null;
    }

    private static void removeTrustedOffice(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (targetId.equals(capital.getHand())) {
            capital.setHand(null);
        }
        if (targetId.equals(capital.getCommander())) {
            capital.setCommander(null);
        }
        if (targetId.equals(capital.getHerald())) {
            capital.setHerald(null);
            capital.setHeraldDisplayName("");
        }
        if (targetId.equals(capital.getGrandMaester())) {
            capital.setGrandMaester(null);
        }
        if (targetId.equals(capital.getMasterOfLaws())) {
            capital.setMasterOfLaws(null);
        }
        if (capital.isRoyalGuard(targetId)) {
            capital.removeRoyalGuard(targetId);
        }
        if (targetId.equals(CapitalAmbassadorService.getAmbassador(level, capital))) {
            CapitalDiplomacyDataAccess.clearAmbassador(level, capital.getCapitalId());
        }
    }

    private static String publicStatusLine(ServerLevel level, CapitalRecord capital, UUID targetId) {
        CapitalPublicCrownStatus status = getPublicStatus(level, capital, targetId);
        return status == null ? "" : status.getDisplayName();
    }

    private static boolean isAtWar(ServerLevel level, CapitalRecord capital) {
        for (CapitalRecord other : CapitalManager.getAllCapitalRecords()) {
            if (other == null
                    || other.getCapitalId() == null
                    || other.getCapitalId().equals(capital.getCapitalId())) {
                continue;
            }
            if (CapitalDiplomacyDataAccess.getDiplomaticState(
                    level,
                    capital.getCapitalId(),
                    other.getCapitalId()
            ) == CapitalDiplomaticState.WAR) {
                return true;
            }
        }
        return false;
    }

    private static UUID currentSovereignId(CapitalRecord capital) {
        return capital.getPlayerSovereignId() != null ? capital.getPlayerSovereignId() : capital.getSovereign();
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }

    private static boolean sameUuid(UUID first, UUID second) {
        return first == null ? second == null : first.equals(second);
    }
}
