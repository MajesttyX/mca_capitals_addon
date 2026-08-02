package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalSavedData;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.mca.entity.VillagerEntityMCA;
import forge.net.mca.server.world.data.Village;
import forge.net.mca.server.world.data.VillageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalRoyalBetrothalService {

    private static final int MINIMUM_RELATIONSHIP = 20;

    private CapitalRoyalBetrothalService() {
    }

    static String validateProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            CapitalDiplomaticState state,
            int score
    ) {
        if (level == null || source == null || target == null) {
            return "That Royal Betrothal proposal is invalid.";
        }

        if (state == CapitalDiplomaticState.WAR
                || state == CapitalDiplomaticState.TRUCE) {
            return "A Royal Betrothal cannot be proposed during war or a truce.";
        }

        if (score < MINIMUM_RELATIONSHIP) {
            return "Relations must be at least Cordial before proposing a Royal Betrothal.";
        }

        return findMatch(level, source, target) == null
                ? "These capitals do not currently have two eligible unbetrothed royals."
                : null;
    }

    static Match findMatch(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null || source == null || target == null) {
            return null;
        }

        for (UUID sourceRoyalId : eligibleRoyals(level, source)) {
            for (UUID targetRoyalId : eligibleRoyals(level, target)) {
                UUID forcedDestination = forcedDestination(
                        source,
                        target,
                        sourceRoyalId,
                        targetRoyalId
                );

                if (isCrownHeir(source, sourceRoyalId)
                        && isCrownHeir(target, targetRoyalId)) {
                    continue;
                }

                UUID destination = forcedDestination != null
                        ? forcedDestination
                        : target.getCapitalId();

                Match match = createMatch(
                        level,
                        source,
                        target,
                        sourceRoyalId,
                        targetRoyalId,
                        destination
                );

                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    public static int openSourceRoyalSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        SelectionContext context = validateSelectionContext(
                player,
                ambassadorId,
                targetCapitalId
        );

        if (!context.valid()) {
            sendFailure(player, context.failureMessage());
            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (UUID royalId :
                eligibleRoyals(
                        context.level(),
                        context.source()
                )) {
            String name = royalName(
                    context.level(),
                    context.source(),
                    royalId
            );

            String title = royalTitle(
                    context.level(),
                    context.source(),
                    royalId
            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            title + " " + name,
                            isCrownHeir(
                                    context.source(),
                                    royalId
                            )
                                    ? "The Crown heir must remain in "
                                    + capitalName(
                                    context.level(),
                                    context.source()
                            )
                                    + "."
                                    : "Eligible unmarried member of the royal family.",
                            "",
                            "",
                            "Choose " + name,
                            "/capitaldiplomacy betrothal_target "
                                    + ambassadorId
                                    + " "
                                    + targetCapitalId
                                    + " "
                                    + royalId,
                            true,
                            ""
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                "Choose Your Royal",
                entries,
                entries.isEmpty()
                        ? "Your capital has no eligible unmarried royal."
                        : "Choose the royal who will enter the proposed betrothal.",
                "/capitaldiplomacy options "
                        + ambassadorId
                        + " "
                        + targetCapitalId
        );

        return 1;
    }

    public static int openTargetRoyalSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        if (!isEligibleRoyal(
                context.level(),
                context.source(),
                sourceRoyalId
        )) {
            sendFailure(
                    player,
                    "That royal is no longer eligible for betrothal."
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (UUID targetRoyalId :
                eligibleRoyals(
                        context.level(),
                        context.target()
                )) {
            if (isCrownHeir(
                    context.source(),
                    sourceRoyalId
            )
                    && isCrownHeir(
                    context.target(),
                    targetRoyalId
            )) {
                continue;
            }

            if (MCAIntegrationBridge
                    .areCloselyRelatedForMarriage(
                            context.level(),
                            sourceRoyalId,
                            targetRoyalId
                    )) {
                continue;
            }

            String name = royalName(
                    context.level(),
                    context.target(),
                    targetRoyalId
            );

            String title = royalTitle(
                    context.level(),
                    context.target(),
                    targetRoyalId
            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            title + " " + name,
                            isCrownHeir(
                                    context.target(),
                                    targetRoyalId
                            )
                                    ? "The Crown heir must remain in "
                                    + capitalName(
                                    context.level(),
                                    context.target()
                            )
                                    + "."
                                    : "Eligible unmarried member of the foreign royal family.",
                            "",
                            "",
                            "Choose " + name,
                            "/capitaldiplomacy betrothal_settlement "
                                    + ambassadorId
                                    + " "
                                    + targetCapitalId
                                    + " "
                                    + sourceRoyalId
                                    + " "
                                    + targetRoyalId,
                            true,
                            ""
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                "Choose Their Royal",
                entries,
                entries.isEmpty()
                        ? "The other capital has no compatible eligible royal. Two Crown heirs cannot be betrothed to each other."
                        : "Choose the royal to be matched with "
                        + royalName(
                        context.level(),
                        context.source(),
                        sourceRoyalId
                )
                        + ".",
                "/capitaldiplomacy betrothal_source "
                        + ambassadorId
                        + " "
                        + targetCapitalId
        );

        return 1;
    }

    public static int openSettlementSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        if (!isValidPair(
                context.level(),
                context.source(),
                context.target(),
                sourceRoyalId,
                targetRoyalId
        )) {
            sendFailure(
                    player,
                    "That royal pair is no longer eligible for betrothal."
            );

            return 0;
        }

        UUID forcedDestination =
                forcedDestination(
                        context.source(),
                        context.target(),
                        sourceRoyalId,
                        targetRoyalId
                );

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        if (forcedDestination != null) {
            CapitalRecord destination =
                    CapitalManager.getCapital(
                            forcedDestination
                    );

            String destinationName =
                    capitalName(
                            context.level(),
                            destination
                    );

            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            forcedDestination,
                            destinationName,
                            "A Crown heir must remain in the capital they are destined to inherit."
                    )
            );
        } else {
            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            context.source()
                                    .getCapitalId(),
                            capitalName(
                                    context.level(),
                                    context.source()
                            ),
                            royalName(
                                    context.level(),
                                    context.target(),
                                    targetRoyalId
                            )
                                    + " will be escorted here after acceptance."
                    )
            );

            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            context.target()
                                    .getCapitalId(),
                            capitalName(
                                    context.level(),
                                    context.target()
                            ),
                            royalName(
                                    context.level(),
                                    context.source(),
                                    sourceRoyalId
                            )
                                    + " will be escorted there after acceptance."
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                "Choose the Couple's Capital",
                entries,
                "Choose where "
                        + royalName(
                        context.level(),
                        context.source(),
                        sourceRoyalId
                )
                        + " and "
                        + royalName(
                        context.level(),
                        context.target(),
                        targetRoyalId
                )
                        + " will establish their household.",
                "/capitaldiplomacy betrothal_target "
                        + ambassadorId
                        + " "
                        + targetCapitalId
                        + " "
                        + sourceRoyalId
        );

        return 1;
    }

    public static int proposeSelected(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        Match match =
                createMatch(
                        context.level(),
                        context.source(),
                        context.target(),
                        sourceRoyalId,
                        targetRoyalId,
                        destinationCapitalId
                );

        if (match == null) {
            sendFailure(
                    player,
                    "That royal pair or settlement is no longer eligible."
            );

            return 0;
        }

        return CapitalDiplomaticProposalService
                .proposeRoyalBetrothal(
                        player,
                        ambassadorId,
                        targetCapitalId,
                        match
                );
    }

    static boolean establish(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || proposal == null
                || source == null
                || target == null
                || !proposal
                .hasRoyalBetrothalDetails()) {
            return false;
        }

        Match match =
                createMatch(
                        level,
                        source,
                        target,
                        proposal.getSourceRoyalId(),
                        proposal.getTargetRoyalId(),
                        proposal.getDestinationCapitalId()
                );

        if (match == null
                || !match.relocatingRoyalId()
                .equals(
                        proposal.getRelocatingRoyalId()
                )) {
            return false;
        }

        String firstName =
                royalName(
                        level,
                        source,
                        match.sourceRoyalId()
                );

        String secondName =
                royalName(
                        level,
                        target,
                        match.targetRoyalId()
                );

        PendingVillagerBetrothalAccess
                .setRoyalEscort(
                        level,
                        match.sourceRoyalId(),
                        firstName,
                        match.targetRoyalId(),
                        secondName,
                        match.originCapitalId(),
                        match.destinationCapitalId(),
                        match.relocatingRoyalId()
                );

        String sourceName =
                capitalName(
                        level,
                        source
                );

        String targetName =
                capitalName(
                        level,
                        target
                );

        String entry =
                firstName
                        + " and "
                        + secondName
                        + " were betrothed by agreement between "
                        + sourceName
                        + " and "
                        + targetName
                        + ". They will establish their household in "
                        + capitalName(
                        level,
                        CapitalManager.getCapital(
                                match.destinationCapitalId()
                        )
                )
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                source,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                target,
                entry
        );

        notifyEscortRequest(
                level,
                source,
                target,
                proposal
        );

        return true;
    }

    public static boolean hasOpenEscortRequests(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return false;
        }

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (!record.isCompleted()
                    && (capital.getCapitalId()
                    .equals(
                            record.originCapitalId()
                    )
                    || capital.getCapitalId()
                    .equals(
                            record.destinationCapitalId()
                    ))) {
                return true;
            }
        }

        return false;
    }

    public static int openEscortRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null
                || ambassadorId == null) {
            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        Entity ambassador =
                level.getEntity(
                        ambassadorId
                );

        CapitalRecord audience =
                resolveAmbassadorCapital(
                        level,
                        player,
                        ambassador
                );

        if (audience == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "The Ambassador is unavailable."
                    )
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (record.isCompleted()
                    || !audience.getCapitalId()
                    .equals(
                            record.originCapitalId()
                    )
                    && !audience.getCapitalId()
                    .equals(
                            record.destinationCapitalId()
                    )) {
                continue;
            }

            CapitalRecord origin =
                    CapitalManager.getCapital(
                            record.originCapitalId()
                    );

            CapitalRecord destination =
                    CapitalManager.getCapital(
                            record.destinationCapitalId()
                    );

            String relocatingName =
                    record.nameFor(
                            record.relocatingRoyalId()
                    );

            String partnerName =
                    record.partnerNameFor(
                            record.relocatingRoyalId()
                    );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            relocatingName
                                    + " and "
                                    + partnerName,
                            "Escort "
                                    + relocatingName
                                    + " from "
                                    + capitalName(
                                    level,
                                    origin
                            )
                                    + ".",
                            "Destination: "
                                    + capitalName(
                                    level,
                                    destination
                            ),
                            "The escort is complete when both betrothed royals are alive and physically present inside the destination capital. Their later marriage is separate.",
                            "Escort Pending",
                            "",
                            false,
                            "Bring both betrothed royals together inside the chosen destination capital."
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .ROYAL_ESCORT_REQUESTS,
                        "Royal Escort Requests",
                        ambassador.getName()
                                .getString(),
                        entries.isEmpty()
                                ? "There are no pending royal escorts connected to this capital."
                                : "These accepted betrothals remain pending until both betrothed royals are together inside the chosen capital.",
                        "/capitalurgent continue "
                                + ambassadorId,
                        entries,
                        List.of()
                )
        );

        return 1;
    }

    public static void tickEscorts(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (record == null
                    || record.isCompleted()) {
                continue;
            }

            CapitalRecord destination =
                    CapitalManager.getCapital(
                            record.destinationCapitalId()
                    );

            if (destination == null
                    || destination.getState()
                    != CapitalState.ACTIVE
                    || destination.getVillageId()
                    == null) {
                continue;
            }

            if (CapitalDiplomacyDataAccess
                    .getDiplomaticState(
                            level,
                            record.originCapitalId(),
                            record.destinationCapitalId()
                    )
                    == CapitalDiplomaticState.WAR) {
                continue;
            }

            Village destinationVillage =
                    VillageManager.get(level)
                            .getOrEmpty(
                                    destination.getVillageId()
                            )
                            .orElse(null);

            if (destinationVillage == null) {
                continue;
            }

            Entity firstEntity =
                    MCAIntegrationBridge
                            .findLoadedMCAVillagerByUuid(
                                    level,
                                    record.pair().first()
                            );

            Entity secondEntity =
                    MCAIntegrationBridge
                            .findLoadedMCAVillagerByUuid(
                                    level,
                                    record.pair().second()
                            );

            if (!(firstEntity
                    instanceof VillagerEntityMCA firstVillager)
                    || !(secondEntity
                    instanceof VillagerEntityMCA secondVillager)) {
                continue;
            }

            if (!firstVillager.isAlive()
                    || firstVillager.isRemoved()
                    || !secondVillager.isAlive()
                    || secondVillager.isRemoved()) {
                continue;
            }

            if (!destinationVillage
                    .isWithinBorder(
                            firstVillager
                    )
                    || !destinationVillage
                    .isWithinBorder(
                            secondVillager
                    )) {
                continue;
            }

            if (!PendingVillagerBetrothalAccess
                    .completeRoyalEscort(
                            level,
                            record.pair().first(),
                            record.pair().second()
                    )) {
                continue;
            }

            CapitalRecord origin =
                    CapitalManager.getCapital(
                            record.originCapitalId()
                    );

            String firstName =
                    record.nameFor(
                            record.pair().first()
                    );

            String secondName =
                    record.nameFor(
                            record.pair().second()
                    );

            String destinationName =
                    capitalName(
                            level,
                            destination
                    );

            String completionEntry =
                    firstName
                            + " and "
                            + secondName
                            + " arrived together in "
                            + destinationName
                            + ", completing the royal escort required by their betrothal.";

            if (origin != null) {
                CapitalChronicleService.addEntry(
                        level,
                        origin,
                        completionEntry
                );
            }

            CapitalChronicleService.addEntry(
                    level,
                    destination,
                    completionEntry
            );
        }
    }

    public static void completeRoyalMarriage(
            ServerLevel level,
            PendingVillagerBetrothalSavedData.RoyalEscortRecord escort,
            Entity firstVillager,
            Entity secondVillager
    ) {
        if (level == null
                || escort == null
                || firstVillager == null
                || secondVillager == null) {
            return;
        }

        CapitalRecord origin =
                CapitalManager.getCapital(
                        escort.originCapitalId()
                );

        CapitalRecord destination =
                CapitalManager.getCapital(
                        escort.destinationCapitalId()
                );

        if (origin == null
                || destination == null) {
            return;
        }

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        origin.getCapitalId(),
                        destination.getCapitalId(),
                        25,
                        "Royal marriage completed",
                        destination.getCapitalId()
                );

        String entry =
                firstVillager.getName()
                        .getString()
                        + " and "
                        + secondVillager.getName()
                        .getString()
                        + " completed the royal marriage joining "
                        + capitalName(
                        level,
                        origin
                )
                        + " and "
                        + capitalName(
                        level,
                        destination
                )
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                origin,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                destination,
                entry
        );
    }

    static String proposalDescription(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (proposal == null
                || !proposal
                .hasRoyalBetrothalDetails()) {
            return "a Royal Betrothal";
        }

        return "the betrothal of "
                + royalName(
                level,
                source,
                proposal.getSourceRoyalId()
        )
                + " and "
                + royalName(
                level,
                target,
                proposal.getTargetRoyalId()
        )
                + ", who will settle in "
                + capitalName(
                level,
                CapitalManager.getCapital(
                        proposal.getDestinationCapitalId()
                )
        );
    }

    static List<UUID> eligibleRoyals(
            ServerLevel level,
            CapitalRecord capital
    ) {
        Set<UUID> candidates =
                new LinkedHashSet<>();

        if (capital == null) {
            return List.of();
        }

        if (capital.getHeir() != null) {
            candidates.add(
                    capital.getHeir()
            );
        }

        candidates.addAll(
                capital.getRoyalChildren()
        );

        candidates.addAll(
                capital.getLegitimizedRoyalChildren()
        );

        candidates.addAll(
                capital.getRoyalHousehold()
        );

        UUID sovereignId =
                capital.getSovereign();

        if (sovereignId != null) {
            candidates.addAll(
                    MCAIntegrationBridge
                            .getChildren(
                                    level,
                                    sovereignId
                            )
            );
        }

        List<UUID> result =
                new ArrayList<>();

        for (UUID candidate : candidates) {
            if (isEligibleRoyal(
                    level,
                    capital,
                    candidate
            )) {
                result.add(candidate);
            }
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (UUID id) ->
                                        isCrownHeir(
                                                capital,
                                                id
                                        )
                                                ? 0
                                                : 1
                        )
                        .thenComparing(
                                id ->
                                        royalName(
                                                level,
                                                capital,
                                                id
                                        ),
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                UUID::toString
                        )
        );

        return result;
    }

    static boolean isEligibleRoyal(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        if (level == null
                || capital == null
                || royalId == null) {
            return false;
        }

        UUID sovereignId =
                capital.getSovereign();

        boolean recognizedRoyal =
                royalId.equals(
                        capital.getHeir()
                )
                        || capital.isRoyalChild(
                        royalId
                )
                        || capital
                        .isLegitimizedRoyalChild(
                                royalId
                        )
                        || capital
                        .getRoyalHousehold()
                        .contains(
                                royalId
                        )
                        || sovereignId != null
                        && MCAIntegrationBridge
                        .isChildOf(
                                level,
                                royalId,
                                sovereignId
                        );

        return recognizedRoyal
                && !capital
                .isDisinheritedRoyalChild(
                        royalId
                )
                && !PendingVillagerBetrothalAccess
                .hasPendingBetrothal(
                        level,
                        royalId
                )
                && MCAIntegrationBridge
                .getSpouse(
                        level,
                        royalId
                )
                == null
                && !MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        royalId
                );
    }

    private static boolean isValidPair(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        return isEligibleRoyal(
                level,
                source,
                sourceRoyalId
        )
                && isEligibleRoyal(
                level,
                target,
                targetRoyalId
        )
                && !(isCrownHeir(
                source,
                sourceRoyalId
        )
                && isCrownHeir(
                target,
                targetRoyalId
        ))
                && !MCAIntegrationBridge
                .areCloselyRelatedForMarriage(
                        level,
                        sourceRoyalId,
                        targetRoyalId
                );
    }

    private static Match createMatch(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId
    ) {
        if (!isValidPair(
                level,
                source,
                target,
                sourceRoyalId,
                targetRoyalId
        )
                || destinationCapitalId == null) {
            return null;
        }

        UUID forced =
                forcedDestination(
                        source,
                        target,
                        sourceRoyalId,
                        targetRoyalId
                );

        if (forced != null
                && !forced.equals(
                destinationCapitalId
        )) {
            return null;
        }

        if (!destinationCapitalId.equals(
                source.getCapitalId()
        )
                && !destinationCapitalId.equals(
                target.getCapitalId()
        )) {
            return null;
        }

        boolean settlesInSource =
                destinationCapitalId.equals(
                        source.getCapitalId()
                );

        UUID relocatingRoyalId =
                settlesInSource
                        ? targetRoyalId
                        : sourceRoyalId;

        UUID originCapitalId =
                settlesInSource
                        ? target.getCapitalId()
                        : source.getCapitalId();

        return new Match(
                sourceRoyalId,
                targetRoyalId,
                relocatingRoyalId,
                originCapitalId,
                destinationCapitalId
        );
    }

    private static UUID forcedDestination(
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        if (isCrownHeir(
                source,
                sourceRoyalId
        )) {
            return source.getCapitalId();
        }

        if (isCrownHeir(
                target,
                targetRoyalId
        )) {
            return target.getCapitalId();
        }

        return null;
    }

    private static boolean isCrownHeir(
            CapitalRecord capital,
            UUID royalId
    ) {
        return capital != null
                && royalId != null
                && royalId.equals(
                capital.getHeir()
        );
    }

    private static OpenAmbassadorCommunicationPacket.Entry
    settlementEntry(
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId,
            String destinationName,
            String explanation
    ) {
        return new OpenAmbassadorCommunicationPacket.Entry(
                "Settle in "
                        + destinationName,
                explanation,
                "",
                "",
                "Choose "
                        + destinationName,
                "/capitaldiplomacy betrothal_send "
                        + ambassadorId
                        + " "
                        + targetCapitalId
                        + " "
                        + sourceRoyalId
                        + " "
                        + targetRoyalId
                        + " "
                        + destinationCapitalId,
                true,
                ""
        );
    }

    private static SelectionContext
    validateSelectionContext(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null
                || ambassadorId == null
                || targetCapitalId == null) {
            return SelectionContext.failure(
                    "That Royal Betrothal selection is invalid."
            );
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            return SelectionContext.failure(
                    audience.failureMessage()
            );
        }

        CapitalRecord target =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        String targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                audience.sourceCapital(),
                                target
                        );

        if (targetFailure != null) {
            return SelectionContext.failure(
                    targetFailure
            );
        }

        ServerLevel level =
                player.serverLevel();

        CapitalDiplomaticState state =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                audience.sourceCapital()
                                        .getCapitalId(),
                                target.getCapitalId()
                        );

        int score =
                CapitalDiplomacyDataAccess
                        .getRelationshipScore(
                                level,
                                audience.sourceCapital()
                                        .getCapitalId(),
                                target.getCapitalId()
                        );

        String failure =
                validateProposal(
                        level,
                        audience.sourceCapital(),
                        target,
                        state,
                        score
                );

        if (failure != null) {
            return SelectionContext.failure(
                    failure
            );
        }

        Entity ambassador =
                level.getEntity(
                        ambassadorId
                );

        return SelectionContext.success(
                level,
                audience.sourceCapital(),
                target,
                ambassador
        );
    }

    private static void openSelectionScreen(
            ServerPlayer player,
            Entity ambassador,
            String title,
            List<OpenAmbassadorCommunicationPacket.Entry> entries,
            String description,
            String backCommand
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .ROYAL_ESCORT_REQUESTS,
                        title,
                        ambassador == null
                                ? "Ambassador"
                                : ambassador.getName()
                                .getString(),
                        description,
                        backCommand,
                        entries,
                        List.of()
                )
        );
    }

    private static String royalName(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        if (royalId == null) {
            return "Unknown Royal";
        }

        if (capital != null) {
            return CapitalNameService
                    .resolveDisplayName(
                            level,
                            capital,
                            royalId
                    );
        }

        Entity entity =
                MCAIntegrationBridge
                        .getEntityByUuid(
                                level,
                                royalId
                        );

        return entity == null
                ? royalId.toString()
                : entity.getName()
                .getString();
    }

    private static String royalTitle(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        String title =
                CapitalTitleResolver
                        .getDisplayTitle(
                                level,
                                capital,
                                royalId
                        );

        return title == null
                || title.isBlank()
                || "None".equals(title)
                ? "Royal"
                : title;
    }

    private static String capitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalDiplomaticAgreementText
                .capitalName(
                        level,
                        capital
                );
    }

    private static CapitalRecord
    resolveAmbassadorCapital(
            ServerLevel level,
            ServerPlayer player,
            Entity ambassador
    ) {
        if (level == null
                || player == null
                || ambassador == null
                || !ambassador.isAlive()
                || player.level()
                != ambassador.level()
                || player.distanceToSqr(
                ambassador
        )
                > 144.0D) {
            return null;
        }

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState()
                    == CapitalState.ACTIVE
                    && CapitalAmbassadorService
                    .isAmbassador(
                            level,
                            capital,
                            ambassador.getUUID()
                    )) {
                return capital;
            }
        }

        return null;
    }

    private static void notifyEscortRequest(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposal proposal
    ) {
        CapitalRecord origin =
                CapitalManager.getCapital(
                        proposal
                                .getDestinationCapitalId()
                                .equals(
                                        source.getCapitalId()
                                )
                                ? target.getCapitalId()
                                : source.getCapitalId()
                );

        CapitalRecord destination =
                CapitalManager.getCapital(
                        proposal.getDestinationCapitalId()
                );

        String relocatingName =
                royalName(
                        level,
                        origin,
                        proposal.getRelocatingRoyalId()
                );

        String firstName =
                royalName(
                        level,
                        source,
                        proposal.getSourceRoyalId()
                );

        String secondName =
                royalName(
                        level,
                        target,
                        proposal.getTargetRoyalId()
                );

        String destinationName =
                capitalName(
                        level,
                        destination
                );

        String message =
                "The Royal Betrothal has been accepted. Escort "
                        + relocatingName
                        + " from "
                        + capitalName(
                        level,
                        origin
                )
                        + " to "
                        + destinationName
                        + ". The escort will be complete as soon as "
                        + firstName
                        + " and "
                        + secondName
                        + " are both alive and physically present inside "
                        + destinationName
                        + ". Their marriage will occur separately once MCA considers them eligible.";

        UUID sourcePlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                source
                        );

        UUID targetPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (sourcePlayer != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            sourcePlayer,
                            "Royal Escort Request",
                            message
                    );
        }

        if (targetPlayer != null
                && !targetPlayer.equals(
                sourcePlayer
        )) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetPlayer,
                            "Royal Escort Request",
                            message
                    );
        }
    }

    private static void sendFailure(
            ServerPlayer player,
            String message
    ) {
        if (player != null) {
            player.sendSystemMessage(
                    Component.literal(
                            message
                    )
            );
        }
    }

    record Match(
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID relocatingRoyalId,
            UUID originCapitalId,
            UUID destinationCapitalId
    ) {
    }

    private record SelectionContext(
            boolean valid,
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            Entity ambassador,
            String failureMessage
    ) {
        private static SelectionContext success(
                ServerLevel level,
                CapitalRecord source,
                CapitalRecord target,
                Entity ambassador
        ) {
            return new SelectionContext(
                    true,
                    level,
                    source,
                    target,
                    ambassador,
                    null
            );
        }

        private static SelectionContext failure(
                String message
        ) {
            return new SelectionContext(
                    false,
                    null,
                    null,
                    null,
                    null,
                    message
            );
        }
    }
}