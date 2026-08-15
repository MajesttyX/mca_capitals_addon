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
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_villager_is_not_a_resident_of_this_capital"));
            return false;
        }
        if (!CapitalCrownStandingService.isFriend(level, capital, targetId)
                || !CapitalCrownStandingService.isWillingToDeclareLoyalty(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_villager_has_not_publicly_declared_loyalty_to_this_crown"));
            return false;
        }
        if (MCAIntegrationBridge.getHeartsWithPlayer(level, targetId, player.getUUID()) < 200) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_villager_has_not_yet_trusted_you_enough_to_declare_their_loyalty"));
            return false;
        }
        if (!isTrustedOfficeEligible(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.a_villager_with_an_active_crown_case_cannot_be_formally_recognized_as"));
            return false;
        }

        CapitalJusticeDataAccess.setPublicStatus(level, capital.getCapitalId(), targetId, CapitalPublicCrownStatus.RECOGNIZED_FRIEND);
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.FRIEND_OF_CROWN_RECOGNIZED_PLAYER, name, player.getName().getString());
        player.sendSystemMessage(Component.translatable("mcacapitals.justice.crown_status.friend_recognized_player", name));
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
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_villager_is_not_eligible_to_be_restored_to_the_crown_s_peace"));
            return false;
        }

        CapitalJusticeDataAccess.setPublicStatus(level, capital.getCapitalId(), targetId, CapitalPublicCrownStatus.RESTORED_TO_PEACE);
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.CROWN_PEACE_RESTORED_PLAYER, name, player.getName().getString());
        player.sendSystemMessage(Component.translatable("mcacapitals.justice.crown_status.peace_restored", name));
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

            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    CapitalNameService.resolveDisplayNameComponent(level, capital, prisonerId),
                    Component.translatable("mcacapitals.ui.crown_standings.awaiting_judgment"),
                    Component.translatable(
                            "mcacapitals.ui.crown_standings.confirmed_cases",
                            CapitalJusticeDataAccess.getConfirmedCaseCount(level, capitalId, prisonerId)
                    ),
                    publicStatusLine(level, capital, prisonerId),
                    Component.translatable("mcacapitals.ui.crown_standings.decide_judgment"),
                    "/capitaljustice options " + masterOfLawsId + " " + prisonerId,
                    true,
                    Component.empty()
            ));
        }

        for (UUID residentId : CapitalResidentScanner.scanResidents(level, capitalId)) {
            CapitalPublicCrownStatus publicStatus = CapitalJusticeDataAccess.getPublicStatus(level, capitalId, residentId);

            if (isRestorationEligible(level, capital, residentId)) {
                entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                        CapitalNameService.resolveDisplayNameComponent(level, capital, residentId),
                        Component.translatable("mcacapitals.ui.crown_standings.eligible_restoration"),
                        Component.translatable("mcacapitals.ui.crown_standings.restoration_wait_complete"),
                        Component.translatable("mcacapitals.justice.public_status.discovered_enemy"),
                        Component.translatable("mcacapitals.ui.crown_standings.restore_peace"),
                        "/capitaljustice restore " + masterOfLawsId + " " + residentId,
                        true,
                        Component.empty()
                ));
                continue;
            }

            if (publicStatus == null
                    && CapitalCrownStandingService.isFriend(level, capital, residentId)
                    && CapitalCrownStandingService.isWillingToDeclareLoyalty(level, capital, residentId)
                    && MCAIntegrationBridge.getHeartsWithPlayer(level, residentId, player.getUUID()) >= 200
                    && isTrustedOfficeEligible(level, capital, residentId)) {
                CapitalTitleResolver.ResolvedTitleId titleId =
                        CapitalTitleResolver.getResolvedTitleId(level, capital, residentId);
                Component nameComponent = CapitalNameService.resolveDisplayNameComponent(
                        level,
                        capital,
                        residentId
                );
                Component heading = titleId == CapitalTitleResolver.ResolvedTitleId.COMMONER
                        || titleId == CapitalTitleResolver.ResolvedTitleId.NONE
                        ? nameComponent
                        : CapitalTitleResolver.getDisplayTitleComponent(level, capital, residentId)
                        .copy()
                        .append(Component.literal(" "))
                        .append(nameComponent);
                entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                        heading,
                        Component.translatable("mcacapitals.ui.crown_standings.declared_loyalty"),
                        Component.translatable(
                                "mcacapitals.ui.crown_standings.declared_support",
                                nameComponent
                        ),
                        Component.translatable("mcacapitals.ui.crown_standings.not_recognized"),
                        Component.translatable(
                                "mcacapitals.ui.crown_standings.recognize",
                                nameComponent
                        ),
                        "/capitaljustice recognize " + masterOfLawsId + " " + residentId,
                        true,
                        Component.empty()
                ));
            }
        }

        entries.sort(Comparator
                .comparing(
                        (OpenAmbassadorCommunicationPacket.Entry entry) -> entry.lineOne().getString(),
                        String.CASE_INSENSITIVE_ORDER
                )
                .thenComparing(
                        entry -> entry.heading().getString(),
                        String.CASE_INSENSITIVE_ORDER
                ));

        ModNetwork.sendToPlayer(player, new OpenAmbassadorCommunicationPacket(
                OpenAmbassadorCommunicationPacket.Mode.JUSTICE_CASES,
                Component.translatable("mcacapitals.ui.crown_standings.title"),
                master.getName(),
                entries.isEmpty()
                        ? Component.translatable("mcacapitals.ui.crown_standings.none")
                        : Component.translatable("mcacapitals.ui.crown_standings.review_message"),
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
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.you_no_longer_have_the_authority_or_audience_required_to_decide_this_c"));
            return 0;
        }
        if (!isAwaitingJudgment(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_prisoner_is_no_longer_awaiting_judgment"));
            return 0;
        }

        String base = "/capitaljustice judge " + masterOfLawsId + " " + targetId + " ";
        List<OpenAmbassadorCommunicationPacket.Action> actions = List.of(
                new OpenAmbassadorCommunicationPacket.Action(
                        Component.translatable("mcacapitals.ui.judgment.royal_pardon"),
                        Component.translatable("mcacapitals.ui.judgment.royal_pardon_description"),
                        base + "pardon",
                        true
                ),
                new OpenAmbassadorCommunicationPacket.Action(
                        Component.translatable("mcacapitals.ui.judgment.imprisonment"),
                        Component.translatable("mcacapitals.ui.judgment.imprisonment_description"),
                        base + "imprisonment",
                        true
                ),
                new OpenAmbassadorCommunicationPacket.Action(
                        Component.translatable("mcacapitals.ui.judgment.exile"),
                        Component.translatable("mcacapitals.ui.judgment.exile_description"),
                        base + "exile",
                        true
                ),
                new OpenAmbassadorCommunicationPacket.Action(
                        Component.translatable("mcacapitals.ui.judgment.execution"),
                        Component.translatable("mcacapitals.ui.judgment.execution_description"),
                        base + "execution",
                        true
                )
        );

        ModNetwork.sendToPlayer(player, new OpenAmbassadorCommunicationPacket(
                OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                Component.translatable(
                        "mcacapitals.ui.judgment.title",
                        CapitalNameService.resolveDisplayNameComponent(level, capital, targetId)
                ),
                Component.translatable("mcacapitals.ui.judgment.confirmed_enemy"),
                Component.translatable("mcacapitals.ui.judgment.choose"),
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
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.you_no_longer_have_the_authority_or_audience_required_to_decide_this_c"));
            return 0;
        }
        if (!isAwaitingJudgment(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_prisoner_is_no_longer_awaiting_judgment"));
            return 0;
        }

        String authorityName = player.getName().getString();
        return applyJudgment(
                level,
                capital,
                targetId,
                judgment,
                authorityName,
                Component.literal(authorityName)
        ) ? 1 : 0;
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
            return applyJudgment(
                    level,
                    capital,
                    targetId,
                    judgment,
                    "the Crown's council",
                    Component.translatable("mcacapitals.justice.authority.crown_council")
            );
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
            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.CROWN_PEACE_RESTORED_COUNCIL, name);
            CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.translatable("mcacapitals.justice.crown_status.peace_restored", name));
            return true;
        }

        List<UUID> friendCandidates = CapitalResidentScanner.scanResidents(level, capitalId)
                .stream()
                .filter(targetId -> CapitalJusticeDataAccess.getPublicStatus(level, capitalId, targetId) == null)
                .filter(targetId -> CapitalCrownStandingService.isFriend(level, capital, targetId))
                .filter(targetId -> CapitalCrownStandingService.isWillingToDeclareLoyalty(level, capital, targetId))
                .filter(targetId -> isTrustedOfficeEligible(level, capital, targetId))
                .filter(targetId -> MCAIntegrationBridge.isTeenOrAdultVillager(level, targetId))
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        if (!friendCandidates.isEmpty() && level.random.nextInt(100) < 2) {
            UUID targetId = friendCandidates.getFirst();
            CapitalJusticeDataAccess.setPublicStatus(level, capitalId, targetId, CapitalPublicCrownStatus.RECOGNIZED_FRIEND);
            CapitalJusticeDataAccess.setLastNpcJudgmentDay(level, capitalId, day);
            String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.FRIEND_OF_CROWN_RECOGNIZED_COUNCIL, name);
            CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.translatable("mcacapitals.justice.crown_status.friend_recognized_council", name));
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
        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.ENEMY_SENTENCE_COMPLETED, name);
        CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, Component.translatable("mcacapitals.justice.sentence.completed", name));
        return true;
    }

    public static void recordPardonResolution(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level != null && capital != null && targetId != null) {
            CapitalJusticeDataAccess.setLastResolvedDay(level, capital.getCapitalId(), targetId, currentDay(level));
        }
    }

    private static boolean applyJudgment(
            ServerLevel level,
            CapitalRecord capital,
            UUID targetId,
            CapitalJudgmentType judgment,
            Object authorityChronicle,
            Component authorityDisplay
    ) {
        UUID capitalId = capital.getCapitalId();
        String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
        Component notification;

        switch (judgment) {
            case PARDON -> {
                CapitalJusticeDataAccess.clearJusticeCase(level, capitalId, targetId);
                MCAExecutionBridge.clearExecutionMark(level, targetId);
                CapitalJusticeDataAccess.setLastResolvedDay(level, capitalId, targetId, currentDay(level));
                notification = Component.translatable(
                        "mcacapitals.justice.judgment.pardon",
                        name,
                        authorityDisplay
                );
            }
            case IMPRISONMENT -> {
                CapitalJusticeDataAccess.setJudgment(level, capitalId, targetId, CapitalJudgmentType.IMPRISONMENT);
                CapitalJusticeDataAccess.setSentenceEndDay(level, capitalId, targetId, currentDay(level) + IMPRISONMENT_DAYS);
                notification = Component.translatable(
                        "mcacapitals.justice.judgment.imprisonment",
                        name,
                        authorityDisplay
                );
            }
            case EXILE -> {
                if (!CapitalAsylumService.markExiled(level, capital, targetId)) {
                    return false;
                }
                CapitalJusticeDataAccess.clearJusticeCase(level, capitalId, targetId);
                CapitalJusticeDataAccess.setLastResolvedDay(level, capitalId, targetId, currentDay(level));
                notification = Component.translatable(
                        "mcacapitals.justice.judgment.exile",
                        name,
                        authorityDisplay
                );
            }
            case EXECUTION -> {
                if (!MCAExecutionBridge.markForExecution(level, targetId)) {
                    return false;
                }
                CapitalJusticeDataAccess.setJudgment(level, capitalId, targetId, CapitalJudgmentType.EXECUTION);
                notification = Component.translatable(
                        "mcacapitals.justice.judgment.execution",
                        name,
                        authorityDisplay
                );
            }
            default -> {
                return false;
            }
        }

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.CROWN_JUDGMENT,
                name,
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.judgment." + judgment.name().toLowerCase(java.util.Locale.ROOT)
                ),
                authorityChronicle
        );
        CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, notification);
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
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.that_capital_is_no_longer_active"));
            return false;
        }
        if (master == null || player.distanceToSqr(master) > MAX_AUDIENCE_DISTANCE_SQR) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.you_must_remain_near_the_master_of_laws_to_make_this_decision"));
            return false;
        }
        if (!mayDecide(player.serverLevel(), capital, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_crown_justice_service.only_the_player_sovereign_or_the_player_hand_serving_an_npc_sovereign"));
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

    private static Component publicStatusLine(ServerLevel level, CapitalRecord capital, UUID targetId) {
        CapitalPublicCrownStatus status = getPublicStatus(level, capital, targetId);
        return status == null ? Component.empty() : status.getDisplayComponent();
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