package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticAgreementMenuService {

    private CapitalDiplomaticAgreementMenuService() {
    }

    static boolean openCapitalList(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null
                || ambassadorEntity == null) {
            return true;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorEntity.getUUID()
                        );

        if (!audience.valid()) {
            sendMessage(
                    player,
                    "Foreign Relations",
                    audience.failureMessage(),
                    ""
            );

            return true;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord source =
                audience.sourceCapital();

        UUID ambassadorId =
                ambassadorEntity.getUUID();

        List<CapitalRecord> targets =
                CapitalManager
                        .getAllCapitalRecords()
                        .stream()
                        .filter(target ->
                                target != null
                        )
                        .filter(target ->
                                target.getState()
                                        == CapitalState.ACTIVE
                        )
                        .filter(target ->
                                target.getCapitalId()
                                        != null
                        )
                        .filter(target ->
                                !target.getCapitalId()
                                        .equals(
                                                source.getCapitalId()
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        target ->
                                                CapitalDiplomaticAgreementText
                                                        .capitalName(
                                                                level,
                                                                target
                                                        ),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        MCAIntegrationBridge.stopInteracting(
                ambassadorEntity
        );

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        if (CapitalAsylumScreenService
                .hasReviewableRequests(
                        player,
                        ambassadorId
                )) {
            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            "Asylum Requests",
                            "Refugees are currently seeking admission.",
                            "",
                            "",
                            "Review Asylum Requests",
                            "/capitalasylum review "
                                    + ambassadorId,
                            true,
                            ""
                    )
            );
        }

        for (CapitalRecord target :
                targets) {
            CapitalDiplomaticTruceService
                    .refreshExpiredTruce(
                            level,
                            source,
                            target
                    );

            int score =
                    CapitalDiplomacyDataAccess
                            .getRelationshipScore(
                                    level,
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            CapitalDiplomaticState state =
                    CapitalDiplomacyDataAccess
                            .getDiplomaticState(
                                    level,
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            boolean tradeActive =
                    CapitalDiplomaticTradeAgreementService
                            .isActive(
                                    level,
                                    source,
                                    target
                            );

            CapitalCampaignRecord campaign =
                    CapitalCampaignService
                            .getCampaignForCapital(
                                    level,
                                    source.getCapitalId()
                            );

            boolean campaignBetweenCapitals =
                    campaign != null
                            && campaign.containsCapital(
                            target.getCapitalId()
                    );

            String targetName =
                    CapitalDiplomaticAgreementText
                            .capitalName(
                                    level,
                                    target
                            );

            String flags =
                    statusFlags(
                            tradeActive,
                            campaignBetweenCapitals
                    );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            targetName,
                            "Relationship: "
                                    + CapitalRelationshipBand
                                    .fromScore(score)
                                    .getDisplayName()
                                    + " ("
                                    + score
                                    + ")",
                            "Status: "
                                    + CapitalDiplomaticAgreementText
                                    .stateDisplay(
                                            state
                                    ),
                            flags,
                            "Manage "
                                    + targetName,
                            "/capitaldiplomacy options "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            true,
                            ""
                    )
            );
        }

        if (entries.isEmpty()) {
            sendMessage(
                    player,
                    "Foreign Relations",
                    ambassadorEntity
                            .getName()
                            .getString()
                            + ": There are no other established capitals with which to conduct diplomacy.",
                    ""
            );

            return true;
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_TARGETS,
                        "Manage Foreign Relations",
                        ambassadorEntity
                                .getName()
                                .getString(),
                        "Choose the capital whose relations you wish to manage.",
                        "",
                        entries,
                        List.of()
                )
        );

        return true;
    }

    static int openActionList(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null
                || ambassadorId == null
                || targetCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            sendMessage(
                    player,
                    "Foreign Relations",
                    audience.failureMessage(),
                    ""
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord source =
                audience.sourceCapital();

        CapitalRecord target =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        String targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                source,
                                target
                        );

        if (targetFailure != null) {
            sendMessage(
                    player,
                    "Foreign Relations",
                    targetFailure,
                    "/capitaldiplomacy targets "
                            + ambassadorId
            );

            return 0;
        }

        CapitalDiplomaticTruceService
                .refreshExpiredTruce(
                        level,
                        source,
                        target
                );

        int score =
                CapitalDiplomacyDataAccess
                        .getRelationshipScore(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        CapitalRelationRecord relation =
                CapitalDiplomacyDataAccess
                        .getOrCreateRelationship(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        CapitalDiplomaticState state =
                relation == null
                        ? CapitalDiplomaticState.PEACE
                        : relation.getDiplomaticState();

        boolean tradeActive =
                CapitalDiplomaticTradeAgreementService
                        .isActive(
                                level,
                                source,
                                target
                        );

        CapitalCampaignRecord sourceCampaign =
                CapitalCampaignService
                        .getCampaignForCapital(
                                level,
                                source.getCapitalId()
                        );

        CapitalCampaignRecord targetCampaign =
                CapitalCampaignService
                        .getCampaignForCapital(
                                level,
                                target.getCapitalId()
                        );

        boolean campaignBetweenCapitals =
                sourceCampaign != null
                        && sourceCampaign.containsCapital(
                        target.getCapitalId()
                );

        DiplomaticProposal pending =
                CapitalAgreementDataAccess
                        .findPendingBetween(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                target
                        );

        List<OpenAmbassadorCommunicationPacket.Action> actions =
                new ArrayList<>();

        List<String> notices =
                new ArrayList<>();

        if (tradeActive) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "End Trade Agreement",
                            "End the Trade Agreement with "
                                    + targetName
                                    + ".",
                            "/capitaldiplomacy endtrade "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            true
                    )
            );
        }

        if (sourceCampaign == null
                && targetCampaign == null) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Plan Attack",
                            "Reserve up to seven Guards and Archers. War begins and the force arrives when you personally enter "
                                    + targetName
                                    + ".",
                            "/capitalcampaign launch "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            true
                    )
            );
        } else if (campaignBetweenCapitals) {
            notices.add(
                    "An attack between these capitals is already planned or active."
            );
        } else {
            notices.add(
                    "One of these capitals is already committed to another active campaign."
            );
        }

        if (pending != null) {
            notices.add(
                    "A "
                            + pending.getType()
                            .getDisplayName()
                            + " is awaiting a response. New diplomatic proposals are blocked until it is resolved."
            );
        } else {
            addProposalActionIfValid(
                    actions,
                    player,
                    ambassadorId,
                    source,
                    target,
                    DiplomaticProposalType
                            .NON_AGGRESSION_PACT,
                    state,
                    score
            );

            addProposalActionIfValid(
                    actions,
                    player,
                    ambassadorId,
                    source,
                    target,
                    DiplomaticProposalType.ALLIANCE,
                    state,
                    score
            );

            addProposalActionIfValid(
                    actions,
                    player,
                    ambassadorId,
                    source,
                    target,
                    DiplomaticProposalType.TRUCE,
                    state,
                    score
            );

            addProposalActionIfValid(
                    actions,
                    player,
                    ambassadorId,
                    source,
                    target,
                    DiplomaticProposalType
                            .TRADE_AGREEMENT,
                    state,
                    score
            );

            boolean activeTruce =
                    state
                            == CapitalDiplomaticState.TRUCE
                            && relation != null
                            && relation.getTruceUntil()
                            > level.getGameTime();

            if (state
                    != CapitalDiplomaticState.WAR
                    && !activeTruce) {
                actions.add(
                        new OpenAmbassadorCommunicationPacket.Action(
                                "Declare War",
                                "Declare war on "
                                        + targetName
                                        + ".",
                                "/capitaldiplomacy war "
                                        + ambassadorId
                                        + " "
                                        + target.getCapitalId(),
                                true
                        )
                );
            }
        }

        if (actions.isEmpty()) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "No Available Actions",
                            "No new formal diplomatic action is currently available.",
                            "",
                            false
                    )
            );
        }

        String subtitle =
                "Relationship: "
                        + CapitalRelationshipBand
                        .fromScore(score)
                        .getDisplayName()
                        + " ("
                        + score
                        + ") — Status: "
                        + CapitalDiplomaticAgreementText
                        .stateDisplay(state);

        String message =
                String.join(
                        " ",
                        notices
                );

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                        targetName,
                        subtitle,
                        message,
                        "/capitaldiplomacy targets "
                                + ambassadorId,
                        List.of(),
                        actions
                )
        );

        return 1;
    }

    private static void addProposalActionIfValid(
            List<OpenAmbassadorCommunicationPacket.Action> actions,
            ServerPlayer player,
            UUID ambassadorId,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type,
            CapitalDiplomaticState state,
            int score
    ) {
        if (CapitalDiplomaticAgreementValidation
                .validateProposal(
                        player.serverLevel(),
                        source,
                        target,
                        type,
                        state,
                        score
                )
                != null) {
            return;
        }

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                player.serverLevel(),
                                target
                        );

        actions.add(
                new OpenAmbassadorCommunicationPacket.Action(
                        "Propose "
                                + type.getDisplayName(),
                        "Send a "
                                + type.getDisplayName()
                                + " proposal to "
                                + targetName
                                + ".",
                        "/capitaldiplomacy propose "
                                + ambassadorId
                                + " "
                                + target.getCapitalId()
                                + " "
                                + type.getSerializedName(),
                        true
                )
        );
    }

    private static String statusFlags(
            boolean tradeActive,
            boolean campaignBetweenCapitals
    ) {
        List<String> flags =
                new ArrayList<>();

        if (tradeActive) {
            flags.add(
                    "Trade Agreement: Active"
            );
        }

        if (campaignBetweenCapitals) {
            flags.add(
                    "Attack: Planned"
            );
        }

        return String.join(
                " — ",
                flags
        );
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message,
            String backCommand
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        "",
                        message,
                        backCommand,
                        List.of(),
                        List.of()
                )
        );
    }
}