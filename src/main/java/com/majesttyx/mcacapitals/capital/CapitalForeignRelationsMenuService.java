package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
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

public final class CapitalForeignRelationsMenuService {

    private CapitalForeignRelationsMenuService() {
    }

    public static boolean openTargets(ServerPlayer player, Entity ambassador) {
        if (ambassador == null) {
            sendMessage(player, "Manage Foreign Relations", "The Ambassador is unavailable.", "");
            return true;
        }
        openTargets(player, ambassador.getUUID());
        return true;
    }

    public static int openTargets(ServerPlayer player, UUID ambassadorId) {
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);
        if (!audience.valid()) {
            sendMessage(player, "Manage Foreign Relations", audience.failureMessage(), "");
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        Entity ambassador = level.getEntity(ambassadorId);
        if (ambassador != null) {
            MCAIntegrationBridge.stopInteracting(ambassador);
        }

        List<CapitalRecord> targets = CapitalManager.getAllCapitalRecords()
                .stream()
                .filter(target -> target != null)
                .filter(target -> target.getState() == CapitalState.ACTIVE)
                .filter(target -> target.getCapitalId() != null)
                .filter(target -> !target.getCapitalId().equals(source.getCapitalId()))
                .sorted(Comparator.comparing(
                        target -> CapitalDiplomaticAgreementText.capitalName(level, target),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();
        if (CapitalAsylumScreenService.hasReviewableRequests(player, ambassadorId)) {
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    "Asylum Requests",
                    "Refugees within the capital are awaiting a formal decision.",
                    "",
                    "",
                    "Review Asylum Requests",
                    "/capitalasylum review " + ambassadorId,
                    true,
                    ""
            ));
        }
        if (CapitalRoyalBetrothalService.hasOpenEscortRequests(level, source)) {
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    "Royal Escort Requests",
                    "An accepted Royal Betrothal is waiting for its escort to be completed.",
                    "",
                    "",
                    "Review Royal Escorts",
                    "/capitalroyalescort review " + ambassadorId,
                    true,
                    ""
            ));
        }

        for (CapitalRecord target : targets) {
            CapitalDiplomaticTruceService.refreshExpiredTruce(level, source, target);
            int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId()
            );
            CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId()
            );
            DiplomaticProposal pending = CapitalAgreementDataAccess.findPendingBetween(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId()
            );
            boolean trade = CapitalDiplomaticTradeAgreementService.isActive(level, source, target);

            String status = "Status: " + stateName(state);
            if (trade) {
                status += " | Trade Agreement: Active";
            }
            if (CapitalCampaignService.getCampaignForCapital(
                    level,
                    target.getCapitalId()
            ) != null) {
                status += " | Campaign: Planned or Active";
            }
            String pendingLine = pending == null
                    ? "No proposal is currently pending."
                    : "Pending: " + pending.getType().getDisplayName();

            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    CapitalDiplomaticAgreementText.capitalName(level, target),
                    "Relationship: "
                            + CapitalRelationshipBand.fromScore(score).getDisplayName()
                            + " (" + score + ")",
                    status,
                    pendingLine,
                    "Manage Relations",
                    "/capitaldiplomacy options " + ambassadorId + " " + target.getCapitalId(),
                    true,
                    ""
            ));
        }

        String message = targets.isEmpty()
                ? "There are no other active capitals available for formal diplomacy."
                : "Choose a capital to review every diplomatic action currently available to this court.";
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_TARGETS,
                        "Manage Foreign Relations",
                        ambassador == null ? "" : ambassador.getName().getString(),
                        message,
                        "",
                        entries,
                        List.of()
                )
        );
        return 1;
    }

    public static int openOptions(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);
        if (!audience.valid()) {
            sendMessage(player, "Manage Foreign Relations", audience.failureMessage(), "");
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);
        String targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(source, target);
        if (targetFailure != null) {
            sendMessage(
                    player,
                    "Manage Foreign Relations",
                    targetFailure,
                    "/capitaldiplomacy targets " + ambassadorId
            );
            return 0;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(level, source, target);
        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        DiplomaticProposal pending = CapitalAgreementDataAccess.findPendingBetween(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );

        List<OpenAmbassadorCommunicationPacket.Action> actions = new ArrayList<>();
        actions.add(proposalAction(
                level, source, target, ambassadorId, pending,
                DiplomaticProposalType.NON_AGGRESSION_PACT,
                "Propose Non-Aggression Pact",
                "A formal pledge that neither capital will initiate hostilities."
        ));
        actions.add(proposalAction(
                level, source, target, ambassadorId, pending,
                DiplomaticProposalType.ALLIANCE,
                "Propose Alliance",
                "Bind the two crowns as allies and strengthen their diplomatic standing."
        ));
        actions.add(proposalAction(
                level, source, target, ambassadorId, pending,
                DiplomaticProposalType.TRUCE,
                "Propose Truce",
                "Suspend an existing war for two Minecraft days."
        ));
        actions.add(proposalAction(
                level, source, target, ambassadorId, pending,
                DiplomaticProposalType.TRADE_AGREEMENT,
                "Propose Trade Agreement",
                "Begin a thirteen-day commercial compact with automatic caravans."
        ));
        actions.add(proposalAction(
                level, source, target, ambassadorId, pending,
                DiplomaticProposalType.ROYAL_BETROTHAL,
                "Propose Royal Betrothal",
                "Arrange a dynastic match between eligible members of both royal families."
        ));

        ActionAvailability gift = giftAvailability(player, ambassadorId, target);
        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                "Send Diplomatic Package",
                gift.enabled()
                        ? "Dispatch the filled Diplomatic Package currently held in either hand."
                        : gift.reason(),
                gift.enabled()
                        ? "/capitalgift send " + ambassadorId + " " + targetCapitalId
                        : "",
                gift.enabled()
        ));

        boolean tradeActive = CapitalDiplomaticTradeAgreementService.isActive(level, source, target);
        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                "End Trade Agreement",
                tradeActive
                        ? "End the current compact immediately. Relations will decrease by 5."
                        : "These capitals do not have an active Trade Agreement.",
                tradeActive
                        ? "/capitaldiplomacy end_trade " + ambassadorId + " " + targetCapitalId
                        : "",
                tradeActive
        ));

        ActionAvailability war = warAvailability(
                level,
                player,
                source,
                target,
                state
        );
        String causeDescription = CapitalWarPlanningService.describePlan(
                level,
                source,
                target
        );
        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                "Plan Punitive War",
                war.enabled()
                        ? causeDescription
                        + " Reserve up to seven Guards and Archers. Victory demands limited reparations."
                        : war.reason(),
                war.enabled()
                        ? "/capitalcampaign launch " + ambassadorId + " "
                        + targetCapitalId + " punitive"
                        : "",
                war.enabled()
        ));
        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                "Plan War of Deposition",
                war.enabled()
                        ? causeDescription
                        + " Reserve up to seven Guards and Archers. Victory removes the defending sovereign and begins an interregnum."
                        : war.reason(),
                war.enabled()
                        ? "/capitalcampaign launch " + ambassadorId + " "
                        + targetCapitalId + " deposition"
                        : "",
                war.enabled()
        ));

        String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);
        String message = "Relationship: "
                + CapitalRelationshipBand.fromScore(score).getDisplayName()
                + " (" + score + ") | Status: " + stateName(state);
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                        targetName,
                        "Manage Foreign Relations",
                        message,
                        "/capitaldiplomacy targets " + ambassadorId,
                        List.of(),
                        actions
                )
        );
        return 1;
    }

    private static OpenAmbassadorCommunicationPacket.Action proposalAction(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID ambassadorId,
            DiplomaticProposal pending,
            DiplomaticProposalType type,
            String label,
            String enabledDescription
    ) {
        String failure;
        if (pending != null) {
            failure = "Only one proposal may be pending between two capitals at a time. Current proposal: "
                    + pending.getType().getDisplayName() + ".";
        } else if (CapitalDiplomaticAgreementValidation.getCurrentSovereignId(target) == null) {
            failure = "The receiving capital has no sovereign able to answer this proposal.";
        } else {
            int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId()
            );
            CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId()
            );
            failure = CapitalDiplomaticAgreementValidation.validateProposal(
                    level,
                    source,
                    target,
                    type,
                    state,
                    score
            );
        }

        boolean enabled = failure == null;
        String command = enabled
                ? "/capitaldiplomacy propose " + ambassadorId + " "
                + target.getCapitalId() + " " + type.getSerializedName()
                : "";
        return new OpenAmbassadorCommunicationPacket.Action(
                label,
                enabled ? enabledDescription : failure,
                command,
                enabled
        );
    }

    private static ActionAvailability warAvailability(
            ServerLevel level,
            ServerPlayer player,
            CapitalRecord source,
            CapitalRecord target,
            CapitalDiplomaticState state
    ) {
        if (state == CapitalDiplomaticState.TRUCE) {
            return ActionAvailability.unavailable(
                    "An active Truce forbids either capital from planning an attack."
            );
        }

        String recoveryFailure = CapitalWarPlanningService.validateRecovery(
                level,
                source
        );
        if (recoveryFailure != null) {
            return ActionAvailability.unavailable(recoveryFailure);
        }

        CapitalCampaignEligibilityService.Validation validation =
                CapitalCampaignEligibilityService.validateCampaign(
                        level,
                        source,
                        target,
                        player.getUUID()
                );
        if (!validation.valid()) {
            return ActionAvailability.unavailable(validation.failureMessage());
        }

        return ActionAvailability.available();
    }

    private static ActionAvailability giftAvailability(
            ServerPlayer player,
            UUID ambassadorId,
            CapitalRecord target
    ) {
        if (player == null || target == null) {
            return ActionAvailability.unavailable("That diplomatic destination is unavailable.");
        }
        Entity ambassador = player.serverLevel().getEntity(ambassadorId);
        CapitalDiplomaticGiftValidation.Validation validation =
                CapitalDiplomaticGiftValidation.validateAudience(player, ambassador, true);
        if (!validation.valid()) {
            return ActionAvailability.unavailable(validation.failureMessage());
        }
        if (CapitalDiplomaticGiftValidation.getCurrentSovereignId(target) == null) {
            return ActionAvailability.unavailable(
                    "The receiving capital has no sovereign able to receive a diplomatic package."
            );
        }
        long remaining = CapitalDiplomacyDataAccess.getGiftCooldownRemaining(
                player.serverLevel(),
                validation.sourceCapital().getCapitalId(),
                target.getCapitalId()
        );
        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 19L) / 20L);
            return ActionAvailability.unavailable(
                    "Another package may be sent on this route in approximately " + seconds + " seconds."
            );
        }
        return ActionAvailability.available();
    }

    private static String stateName(CapitalDiplomaticState state) {
        if (state == null) {
            return "Unknown";
        }
        return switch (state) {
            case PEACE -> "Peace";
            case NON_AGGRESSION_PACT -> "Non-Aggression Pact";
            case ALLIANCE -> "Alliance";
            case TRUCE -> "Truce";
            case WAR -> "War";
        };
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message,
            String backCommand
    ) {
        if (player == null) {
            return;
        }
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        "",
                        message == null ? "" : message,
                        backCommand == null ? "" : backCommand,
                        List.of(),
                        List.of()
                )
        );
    }

    private record ActionAvailability(boolean enabled, String reason) {
        private static ActionAvailability available() {
            return new ActionAvailability(true, "");
        }

        private static ActionAvailability unavailable(String reason) {
            return new ActionAvailability(false, reason == null ? "This action is unavailable." : reason);
        }
    }
}
