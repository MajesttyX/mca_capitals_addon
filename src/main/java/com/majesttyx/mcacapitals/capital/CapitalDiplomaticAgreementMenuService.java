package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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
            player.sendSystemMessage(
                    Component.literal(
                            audience.failureMessage()
                    )
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
                CapitalManager.getAllCapitalRecords()
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

        if (targets.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                            ambassadorEntity
                                    .getName()
                                    .getString()
                                    + ": There are no other established capitals with which to conduct diplomacy."
                    )
            );

            return true;
        }

        player.sendSystemMessage(
                Component.literal(
                        ambassadorEntity
                                .getName()
                                .getString()
                                + ": Choose the capital whose relations you wish to manage."
                )
        );

        for (CapitalRecord target : targets) {
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

            String command =
                    "/capitaldiplomacy options "
                            + ambassadorId
                            + " "
                            + target.getCapitalId();

            String targetName =
                    CapitalDiplomaticAgreementText
                            .capitalName(
                                    level,
                                    target
                            );

            MutableComponent line =
                    Component.literal("[Manage] ")
                            .setStyle(
                                    CapitalDiplomaticAgreementText
                                            .clickableStyle(
                                                    ChatFormatting.GREEN,
                                                    command,
                                                    "Manage relations with "
                                                            + targetName
                                                            + "."
                                            )
                            )
                            .append(
                                    Component.literal(targetName)
                                            .withStyle(
                                                    ChatFormatting.GOLD
                                            )
                            )
                            .append(
                                    Component.literal(
                                            " — "
                                                    + CapitalRelationshipBand
                                                    .fromScore(score)
                                                    .getDisplayName()
                                                    + " ("
                                                    + score
                                                    + ") — "
                                                    + CapitalDiplomaticAgreementText
                                                    .stateDisplay(state)
                                    ).withStyle(
                                            ChatFormatting.GRAY
                                    )
                            );

            if (tradeActive) {
                line.append(
                        Component.literal(
                                " — Trade Agreement"
                        ).withStyle(
                                ChatFormatting.DARK_GREEN
                        )
                );
            }

            if (campaignBetweenCapitals) {
                line.append(
                        Component.literal(
                                " — Attack Planned"
                        ).withStyle(
                                ChatFormatting.DARK_RED
                        )
                );
            }

            player.sendSystemMessage(line);
        }

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
            player.sendSystemMessage(
                    Component.literal(
                            audience.failureMessage()
                    )
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
            player.sendSystemMessage(
                    Component.literal(targetFailure)
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

        MutableComponent heading =
                Component.literal(
                        CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        target
                                )
                                + " — "
                                + CapitalRelationshipBand
                                .fromScore(score)
                                .getDisplayName()
                                + " ("
                                + score
                                + ") — "
                                + CapitalDiplomaticAgreementText
                                .stateDisplay(state)
                ).withStyle(
                        ChatFormatting.GOLD
                );

        if (tradeActive) {
            heading.append(
                    Component.literal(
                            " — Trade Agreement"
                    ).withStyle(
                            ChatFormatting.DARK_GREEN
                    )
            );
        }

        if (campaignBetweenCapitals) {
            heading.append(
                    Component.literal(
                            " — Attack Planned"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );
        }

        player.sendSystemMessage(heading);

        boolean offeredAction = false;

        if (tradeActive) {
            String command =
                    "/capitaldiplomacy endtrade "
                            + ambassadorId
                            + " "
                            + target.getCapitalId();

            player.sendSystemMessage(
                    Component.literal(
                                    "[End Trade Agreement]"
                            )
                            .setStyle(
                                    CapitalDiplomaticAgreementText
                                            .clickableStyle(
                                                    ChatFormatting.RED,
                                                    command,
                                                    "End the Trade Agreement with "
                                                            + CapitalDiplomaticAgreementText
                                                            .capitalName(
                                                                    level,
                                                                    target
                                                            )
                                                            + "."
                                            )
                            )
            );

            offeredAction = true;
        }

        if (sourceCampaign == null
                && targetCampaign == null) {
            String command =
                    "/capitalcampaign launch "
                            + ambassadorId
                            + " "
                            + target.getCapitalId();

            player.sendSystemMessage(
                    Component.literal(
                                    "[Plan Attack]"
                            )
                            .setStyle(
                                    CapitalDiplomaticAgreementText
                                            .clickableStyle(
                                                    ChatFormatting.DARK_RED,
                                                    command,
                                                    "Reserve up to seven Guards and Archers. War begins and the force arrives when you personally enter "
                                                            + CapitalDiplomaticAgreementText
                                                            .capitalName(
                                                                    level,
                                                                    target
                                                            )
                                                            + "."
                                            )
                            )
            );

            offeredAction = true;
        } else if (campaignBetweenCapitals) {
            player.sendSystemMessage(
                    Component.literal(
                            "An attack between these capitals is already planned or active."
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );
        } else {
            player.sendSystemMessage(
                    Component.literal(
                            "One of these capitals is already committed to another active campaign."
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );
        }

        if (pending != null) {
            player.sendSystemMessage(
                    Component.literal(
                            "A "
                                    + pending.getType()
                                    .getDisplayName()
                                    + " is awaiting a response. Planning an attack remains available, but new diplomatic proposals are blocked until it is resolved."
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );
        } else {
            offeredAction |=
                    sendProposalActionIfValid(
                            player,
                            ambassadorId,
                            source,
                            target,
                            DiplomaticProposalType
                                    .NON_AGGRESSION_PACT,
                            state,
                            score
                    );

            offeredAction |=
                    sendProposalActionIfValid(
                            player,
                            ambassadorId,
                            source,
                            target,
                            DiplomaticProposalType.ALLIANCE,
                            state,
                            score
                    );

            offeredAction |=
                    sendProposalActionIfValid(
                            player,
                            ambassadorId,
                            source,
                            target,
                            DiplomaticProposalType.TRUCE,
                            state,
                            score
                    );

            offeredAction |=
                    sendProposalActionIfValid(
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
                    state == CapitalDiplomaticState.TRUCE
                            && relation != null
                            && relation.getTruceUntil()
                            > level.getGameTime();

            if (state
                    != CapitalDiplomaticState.WAR
                    && !activeTruce) {
                String targetName =
                        CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        target
                                );

                String command =
                        "/capitaldiplomacy war "
                                + ambassadorId
                                + " "
                                + target.getCapitalId();

                player.sendSystemMessage(
                        Component.literal(
                                        "[Declare War]"
                                )
                                .setStyle(
                                        CapitalDiplomaticAgreementText
                                                .clickableStyle(
                                                        ChatFormatting.RED,
                                                        command,
                                                        "Declare war on "
                                                                + targetName
                                                                + "."
                                                )
                                )
                );

                offeredAction = true;
            }
        }

        if (!offeredAction) {
            player.sendSystemMessage(
                    Component.literal(
                            "No new formal diplomatic action is currently available."
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }

        sendBackButton(
                player,
                ambassadorId
        );

        return 1;
    }

    private static boolean
    sendProposalActionIfValid(
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
            return false;
        }

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                player.serverLevel(),
                                target
                        );

        String command =
                "/capitaldiplomacy propose "
                        + ambassadorId
                        + " "
                        + target.getCapitalId()
                        + " "
                        + type.getSerializedName();

        player.sendSystemMessage(
                Component.literal(
                                "[Propose "
                                        + type.getDisplayName()
                                        + "]"
                        )
                        .setStyle(
                                CapitalDiplomaticAgreementText
                                        .clickableStyle(
                                                ChatFormatting.GREEN,
                                                command,
                                                "Send a "
                                                        + type.getDisplayName()
                                                        + " proposal to "
                                                        + targetName
                                                        + "."
                                        )
                        )
        );

        return true;
    }

    private static void sendBackButton(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        player.sendSystemMessage(
                Component.literal(
                                "[Back to Capitals]"
                        )
                        .setStyle(
                                CapitalDiplomaticAgreementText
                                        .clickableStyle(
                                                ChatFormatting.GRAY,
                                                "/capitaldiplomacy targets "
                                                        + ambassadorId,
                                                "Return to the list of capitals."
                                        )
                        )
        );
    }
}