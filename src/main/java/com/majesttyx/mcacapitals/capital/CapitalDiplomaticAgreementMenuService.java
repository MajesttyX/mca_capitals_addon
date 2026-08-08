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
        if (player == null || ambassadorEntity == null) {
            return true;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateMenuAudience(
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

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        UUID ambassadorId = ambassadorEntity.getUUID();

        boolean sovereignAuthority =
                CapitalDiplomaticAuthorityService.mayExerciseSovereignAuthority(
                        level,
                        source,
                        player.getUUID()
                );

        List<CapitalRecord> targets =
                CapitalManager.getAllCapitalRecords()
                        .stream()
                        .filter(target -> target != null)
                        .filter(target ->
                                target.getState() == CapitalState.ACTIVE
                        )
                        .filter(target ->
                                target.getCapitalId() != null
                        )
                        .filter(target ->
                                !target.getCapitalId().equals(
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

        boolean escortOpen =
                CapitalRoyalBetrothalService.hasOpenEscortRequests(
                        level,
                        source
                );

        String escortReason =
                !sovereignAuthority
                        ? "Only the sovereign, or the Hand serving a villager sovereign, may direct a royal escort."
                        : escortOpen
                        ? "An accepted royal betrothal is waiting for its escort."
                        : "No accepted royal betrothal currently awaits an escort.";

        entries.add(
                new OpenAmbassadorCommunicationPacket.Entry(
                        "Royal Escort Requests",
                        escortReason,
                        "",
                        "",
                        "Review Royal Escorts",
                        escortOpen && sovereignAuthority
                                ? "/capitalroyalescort review "
                                + ambassadorId
                                : "",
                        escortOpen && sovereignAuthority,
                        escortReason
                )
        );

        boolean asylumOpen =
                CapitalAsylumScreenService.hasReviewableRequests(
                        player,
                        ambassadorId
                );

        String asylumReason =
                !sovereignAuthority
                        ? "Only the sovereign, or the Hand serving a villager sovereign, may grant asylum."
                        : !CapitalBuildingService.hasInn(
                        level,
                        source
                )
                        ? "The capital requires an operational Inn before refugees can seek asylum."
                        : asylumOpen
                        ? "Refugees are currently seeking admission."
                        : "No refugees are currently seeking asylum inside the capital.";

        entries.add(
                new OpenAmbassadorCommunicationPacket.Entry(
                        "Asylum Requests",
                        asylumReason,
                        "",
                        "",
                        "Review Asylum Requests",
                        asylumOpen
                                ? "/capitalasylum review "
                                + ambassadorId
                                : "",
                        asylumOpen,
                        asylumReason
                )
        );

        for (CapitalRecord target : targets) {
            CapitalDiplomaticTruceService.refreshExpiredTruce(
                    level,
                    source,
                    target
            );

            int score =
                    CapitalDiplomacyDataAccess.getRelationshipScore(
                            level,
                            source.getCapitalId(),
                            target.getCapitalId()
                    );

            CapitalDiplomaticState state =
                    CapitalDiplomacyDataAccess.getDiplomaticState(
                            level,
                            source.getCapitalId(),
                            target.getCapitalId()
                    );

            boolean tradeActive =
                    CapitalDiplomaticTradeAgreementService.isActive(
                            level,
                            source,
                            target
                    );

            CapitalCampaignRecord campaign =
                    CapitalCampaignService.getCampaignForCapital(
                            level,
                            source.getCapitalId()
                    );

            boolean campaignBetweenCapitals =
                    campaign != null
                            && campaign.containsCapital(
                            target.getCapitalId()
                    );

            String targetName =
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            target
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
                                    .stateDisplay(state),
                            statusFlags(
                                    tradeActive,
                                    campaignBetweenCapitals
                            ),
                            "Manage " + targetName,
                            "/capitaldiplomacy options "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            true,
                            ""
                    )
            );
        }

        if (targets.isEmpty()) {
            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            "No Known Foreign Capitals",
                            "There are no other established capitals with which to conduct diplomacy.",
                            "",
                            "",
                            "",
                            "",
                            false,
                            "There are no other established capitals with which to conduct diplomacy."
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .DIPLOMACY_TARGETS,
                        "Manage Foreign Relations",
                        ambassadorEntity
                                .getName()
                                .getString(),
                        "Choose a capital to review every available diplomatic action.",
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

        CapitalDiplomaticAgreementValidation.AudienceValidation
                menuAudience =
                CapitalDiplomaticAgreementValidation
                        .validateMenuAudience(
                                player,
                                ambassadorId
                        );

        if (!menuAudience.valid()) {
            sendMessage(
                    player,
                    "Foreign Relations",
                    menuAudience.failureMessage(),
                    ""
            );
            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord source =
                menuAudience.sourceCapital();

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

        CapitalDiplomaticTruceService.refreshExpiredTruce(
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

        Entity ambassador =
                level.getEntity(
                        ambassadorId
                );

        List<OpenAmbassadorCommunicationPacket.Action> actions =
                new ArrayList<>();

        addGiftAction(
                actions,
                player,
                ambassador,
                source,
                target,
                ambassadorId,
                targetName
        );

        addProposalAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                DiplomaticProposalType.NON_AGGRESSION_PACT,
                state,
                score,
                pending
        );

        addProposalAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                DiplomaticProposalType.ALLIANCE,
                state,
                score,
                pending
        );

        addProposalAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                DiplomaticProposalType.TRUCE,
                state,
                score,
                pending
        );

        addProposalAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                DiplomaticProposalType.TRADE_AGREEMENT,
                state,
                score,
                pending
        );

        addProposalAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                DiplomaticProposalType.ROYAL_BETROTHAL,
                state,
                score,
                pending
        );

        CapitalDiplomaticAgreementValidation.AudienceValidation
                formalAudience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        String endTradeReason;
        boolean mayEndTrade;

        if (!formalAudience.valid()) {
            mayEndTrade = false;
            endTradeReason =
                    formalAudience.failureMessage();
        } else if (!tradeActive) {
            mayEndTrade = false;
            endTradeReason =
                    "No Trade Agreement is currently in force with "
                            + targetName
                            + ".";
        } else {
            mayEndTrade = true;
            endTradeReason =
                    "End the Trade Agreement with "
                            + targetName
                            + ".";
        }

        actions.add(
                new OpenAmbassadorCommunicationPacket.Action(
                        "End Trade Agreement",
                        endTradeReason,
                        mayEndTrade
                                ? "/capitaldiplomacy endtrade "
                                + ambassadorId
                                + " "
                                + target.getCapitalId()
                                : "",
                        mayEndTrade
                )
        );

        addWarAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                targetName,
                "Plan Punitive War",
                "punitive",
                "Victory demands limited reparations."
        );

        addWarAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                targetName,
                "Plan War of Deposition",
                "deposition",
                "Victory removes the defending sovereign and begins an interregnum."
        );

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
                pending == null
                        ? "Every diplomatic course is shown below. Unavailable actions explain what must change first."
                        : CapitalDiplomaticAgreementText
                        .capitalizedWithIndefiniteArticle(
                                pending.getType()
                                        .getDisplayName()
                        )
                        + " is awaiting a response between these capitals.";

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .DIPLOMACY_ACTIONS,
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

    private static void addGiftAction(
            List<OpenAmbassadorCommunicationPacket.Action> actions,
            ServerPlayer player,
            Entity ambassador,
            CapitalRecord source,
            CapitalRecord target,
            UUID ambassadorId,
            String targetName
    ) {
        CapitalDiplomaticGiftValidation.Validation validation =
                CapitalDiplomaticGiftValidation
                        .validateAudience(
                                player,
                                ambassador,
                                true
                        );

        boolean enabled =
                validation.valid();

        String reason =
                validation.failureMessage();

        if (enabled) {
            long cooldown =
                    CapitalDiplomacyDataAccess
                            .getGiftCooldownRemaining(
                                    player.serverLevel(),
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            if (cooldown > 0L) {
                enabled = false;
                reason =
                        "Another package may be sent to "
                                + targetName
                                + " in "
                                + CapitalDiplomaticGiftText
                                .formatDuration(
                                        cooldown
                                )
                                + ".";
            } else {
                reason =
                        "Send the filled Diplomatic Package held in either hand to "
                                + targetName
                                + ".";
            }
        }

        actions.add(
                new OpenAmbassadorCommunicationPacket.Action(
                        "Send Diplomatic Package",
                        reason == null
                                ? "A Diplomatic Package cannot be sent at present."
                                : reason,
                        enabled
                                ? "/capitalgift send "
                                + ambassadorId
                                + " "
                                + target.getCapitalId()
                                : "",
                        enabled
                )
        );
    }

    private static void addProposalAction(
            List<OpenAmbassadorCommunicationPacket.Action> actions,
            ServerPlayer player,
            UUID ambassadorId,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type,
            CapitalDiplomaticState state,
            int score,
            DiplomaticProposal pending
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation
                formalAudience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        boolean enabled = true;
        String reason;

        if (!formalAudience.valid()) {
            enabled = false;
            reason =
                    formalAudience.failureMessage();
        } else if (pending != null) {
            enabled = false;
            reason =
                    CapitalDiplomaticAgreementText
                            .capitalizedWithIndefiniteArticle(
                                    pending.getType()
                                            .getDisplayName()
                            )
                            + " already awaits an answer between these capitals.";
        } else if (
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(
                                target
                        ) == null
        ) {
            enabled = false;
            reason =
                    "That capital currently has no sovereign who can answer a proposal.";
        } else {
            reason =
                    CapitalDiplomaticAgreementValidation
                            .validateProposal(
                                    player.serverLevel(),
                                    source,
                                    target,
                                    type,
                                    state,
                                    score
                            );

            if (reason != null) {
                enabled = false;
            } else {
                reason =
                        "Send "
                                + CapitalDiplomaticAgreementText
                                .withIndefiniteArticle(
                                        type.getDisplayName()
                                )
                                + " proposal to "
                                + CapitalDiplomaticAgreementText
                                .capitalName(
                                        player.serverLevel(),
                                        target
                                )
                                + ".";
            }
        }

        String command = "";

        if (enabled) {
            command =
                    type
                            == DiplomaticProposalType
                            .ROYAL_BETROTHAL
                            ? "/capitaldiplomacy betrothal_source "
                            + ambassadorId
                            + " "
                            + target.getCapitalId()
                            : "/capitaldiplomacy propose "
                            + ambassadorId
                            + " "
                            + target.getCapitalId()
                            + " "
                            + type.getSerializedName();
        }

        actions.add(
                new OpenAmbassadorCommunicationPacket.Action(
                        "Propose "
                                + type.getDisplayName(),
                        reason,
                        command,
                        enabled
                )
        );
    }

    private static void addWarAction(
            List<OpenAmbassadorCommunicationPacket.Action> actions,
            ServerPlayer player,
            UUID ambassadorId,
            CapitalRecord source,
            CapitalRecord target,
            String targetName,
            String label,
            String goal,
            String outcome
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation
                formalAudience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        boolean enabled = true;
        String reason;

        if (!formalAudience.valid()) {
            enabled = false;
            reason =
                    formalAudience.failureMessage();
        } else {
            reason =
                    CapitalWarPlanningService
                            .validateRecovery(
                                    player.serverLevel(),
                                    source
                            );

            if (reason == null) {
                CapitalCampaignEligibilityService.Validation
                        eligibility =
                        CapitalCampaignEligibilityService
                                .validateCampaign(
                                        player.serverLevel(),
                                        source,
                                        target,
                                        player.getUUID()
                                );

                if (!eligibility.valid()) {
                    reason =
                            eligibility.failureMessage();
                }
            }

            if (reason != null) {
                enabled = false;
            } else {
                reason =
                        CapitalWarPlanningService
                                .describePlan(
                                        player.serverLevel(),
                                        source,
                                        target
                                )
                                + " Reserve up to seven Guards and Archers against "
                                + targetName
                                + ". "
                                + outcome;
            }
        }

        actions.add(
                new OpenAmbassadorCommunicationPacket.Action(
                        label,
                        reason,
                        enabled
                                ? "/capitalcampaign launch "
                                + ambassadorId
                                + " "
                                + target.getCapitalId()
                                + " "
                                + goal
                                : "",
                        enabled
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
                        OpenAmbassadorCommunicationPacket.Mode
                                .MESSAGE,
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