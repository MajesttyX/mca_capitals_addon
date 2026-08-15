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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
            sendMessage(player, Component.translatable("mcacapitals.ui.diplomacy.foreign_relations"), audience.failureMessage(), "");
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

        MCAIntegrationBridge.stopInteracting(ambassadorEntity);

        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();

        boolean escortOpen = CapitalRoyalBetrothalService.hasOpenEscortRequests(level, source);
        Component escortReason = !sovereignAuthority
                ? Component.translatable("mcacapitals.ui.royal_escort.authority")
                : escortOpen
                ? Component.translatable("mcacapitals.ui.royal_escort.waiting")
                : Component.translatable("mcacapitals.ui.royal_escort.none");

        entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                Component.translatable("mcacapitals.ui.royal_escort.title"),
                escortReason,
                Component.empty(),
                Component.empty(),
                Component.translatable("mcacapitals.ui.royal_escort.review"),
                escortOpen && sovereignAuthority
                        ? "/capitalroyalescort review " + ambassadorId
                        : "",
                escortOpen && sovereignAuthority,
                escortReason
        ));

        boolean asylumOpen = CapitalAsylumScreenService.hasReviewableRequests(
                player,
                ambassadorId
        );
        Component asylumReason = !sovereignAuthority
                ? Component.translatable("mcacapitals.ui.asylum.only_sovereign_or_hand")
                : !CapitalBuildingService.hasInn(level, source)
                ? Component.translatable("mcacapitals.ui.asylum.requires_inn_seek")
                : asylumOpen
                ? Component.translatable("mcacapitals.ui.asylum.seeking_admission")
                : Component.translatable("mcacapitals.ui.asylum.none");

        entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                Component.translatable("mcacapitals.ui.asylum.title"),
                asylumReason,
                Component.empty(),
                Component.empty(),
                Component.translatable("mcacapitals.ui.asylum.review"),
                asylumOpen
                        ? "/capitalasylum review " + ambassadorId
                        : "",
                asylumOpen,
                asylumReason
        ));

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

            boolean tradeActive = CapitalDiplomaticTradeAgreementService.isActive(
                    level,
                    source,
                    target
            );

            CapitalCampaignRecord campaign = CapitalCampaignService.getCampaignForCapital(
                    level,
                    source.getCapitalId()
            );

            boolean campaignBetweenCapitals = campaign != null
                    && campaign.containsCapital(target.getCapitalId());

            String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);
            Component targetNameComponent = capitalNameComponent(targetName);

            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    targetNameComponent,
                    Component.translatable(
                            "mcacapitals.ui.diplomacy.relationship_score",
                            CapitalRelationshipBand.fromScore(score).getDisplayComponent(),
                            score
                    ),
                    Component.translatable(
                            "mcacapitals.ui.diplomacy.status",
                            CapitalDiplomaticAgreementText.stateDisplay(state)
                    ),
                    statusFlags(tradeActive, campaignBetweenCapitals),
                    Component.translatable(
                            "mcacapitals.ui.diplomacy.manage_target",
                            targetNameComponent
                    ),
                    "/capitaldiplomacy options "
                            + ambassadorId
                            + " "
                            + target.getCapitalId(),
                    true,
                    Component.empty()
            ));
        }

        if (targets.isEmpty()) {
            Component noTargets = Component.translatable("mcacapitals.ui.diplomacy.no_targets");
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    Component.translatable("mcacapitals.ui.diplomacy.no_known_capitals"),
                    noTargets,
                    Component.empty(),
                    Component.empty(),
                    Component.empty(),
                    "",
                    false,
                    noTargets
            ));
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_TARGETS,
                        Component.translatable("mcacapitals.ui.diplomacy.manage_foreign_relations"),
                        ambassadorEntity.getName(),
                        Component.translatable("mcacapitals.ui.diplomacy.choose_capital"),
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
        if (player == null || ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation menuAudience =
                CapitalDiplomaticAgreementValidation.validateMenuAudience(player, ambassadorId);

        if (!menuAudience.valid()) {
            sendMessage(player, Component.translatable("mcacapitals.ui.diplomacy.foreign_relations"), menuAudience.failureMessage(), "");
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = menuAudience.sourceCapital();
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);
        Component targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(source, target);

        if (targetFailure != null) {
            sendMessage(
                    player,
                    Component.translatable("mcacapitals.ui.diplomacy.foreign_relations"),
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

        CapitalRelationRecord relation = CapitalDiplomacyDataAccess.getOrCreateRelationship(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );

        CapitalDiplomaticState state = relation == null
                ? CapitalDiplomaticState.PEACE
                : relation.getDiplomaticState();

        boolean tradeActive = CapitalDiplomaticTradeAgreementService.isActive(
                level,
                source,
                target
        );

        DiplomaticProposal pending = CapitalAgreementDataAccess.findPendingBetween(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );

        String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);
        Component targetNameComponent = capitalNameComponent(targetName);
        Entity ambassador = level.getEntity(ambassadorId);
        List<OpenAmbassadorCommunicationPacket.Action> actions = new ArrayList<>();

        addGiftAction(actions, player, ambassador, source, target, ambassadorId, targetName);

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

        CapitalDiplomaticAgreementValidation.AudienceValidation formalAudience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);

        Component endTradeReason;
        boolean mayEndTrade;

        if (!formalAudience.valid()) {
            mayEndTrade = false;
            endTradeReason = formalAudience.failureMessage();
        } else if (!tradeActive) {
            mayEndTrade = false;
            endTradeReason = Component.translatable(
                    "mcacapitals.ui.diplomacy.no_trade_in_force",
                    targetNameComponent
            );
        } else {
            mayEndTrade = true;
            endTradeReason = Component.translatable(
                    "mcacapitals.ui.diplomacy.end_trade_with",
                    targetNameComponent
            );
        }

        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                Component.translatable("mcacapitals.ui.diplomacy.end_trade"),
                endTradeReason,
                mayEndTrade
                        ? "/capitaldiplomacy endtrade "
                        + ambassadorId
                        + " "
                        + target.getCapitalId()
                        : "",
                mayEndTrade
        ));

        addWarAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                targetNameComponent,
                Component.translatable("mcacapitals.ui.diplomacy.plan_punitive_war"),
                "punitive",
                Component.translatable("mcacapitals.ui.diplomacy.punitive_outcome")
        );
        addWarAction(
                actions,
                player,
                ambassadorId,
                source,
                target,
                targetNameComponent,
                Component.translatable("mcacapitals.ui.diplomacy.plan_deposition_war"),
                "deposition",
                Component.translatable("mcacapitals.ui.diplomacy.deposition_outcome")
        );

        Component subtitle = Component.translatable(
                "mcacapitals.ui.diplomacy.relationship_and_status",
                CapitalRelationshipBand.fromScore(score).getDisplayComponent(),
                score,
                CapitalDiplomaticAgreementText.stateDisplay(state)
        );

        Component message = pending == null
                ? Component.translatable("mcacapitals.ui.diplomacy.all_actions")
                : Component.translatable(
                        "mcacapitals.ui.diplomacy.pending_response",
                        pending.getType().getCapitalizedIndefiniteComponent()
                );

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                        targetNameComponent,
                        subtitle,
                        message,
                        "/capitaldiplomacy targets " + ambassadorId,
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
                CapitalDiplomaticGiftValidation.validateAudience(
                        player,
                        ambassador,
                        true
                );

        boolean enabled = validation.valid();
        Component reason = validation.failureMessage();

        if (enabled) {
            long cooldown = CapitalDiplomacyDataAccess.getGiftCooldownRemaining(
                    player.serverLevel(),
                    source.getCapitalId(),
                    target.getCapitalId()
            );

            if (cooldown > 0L) {
                enabled = false;
                reason = Component.translatable(
                        "mcacapitals.ui.diplomacy.gift_cooldown",
                        capitalNameComponent(targetName),
                        CapitalDiplomaticGiftText.formatDuration(cooldown)
                );
            } else {
                reason = Component.translatable(
                        "mcacapitals.ui.diplomacy.send_filled_package",
                        capitalNameComponent(targetName)
                );
            }
        }

        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                Component.translatable("mcacapitals.ui.diplomacy.send_package"),
                reason == null
                        ? Component.translatable("mcacapitals.ui.diplomacy.package_unavailable")
                        : reason,
                enabled
                        ? "/capitalgift send "
                        + ambassadorId
                        + " "
                        + target.getCapitalId()
                        : "",
                enabled
        ));
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
        CapitalDiplomaticAgreementValidation.AudienceValidation formalAudience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);

        boolean enabled = true;
        Component reason;

        if (!formalAudience.valid()) {
            enabled = false;
            reason = formalAudience.failureMessage();
        } else if (pending != null) {
            enabled = false;
            reason = Component.translatable(
                    "mcacapitals.ui.diplomacy.pending_already",
                    pending.getType().getCapitalizedIndefiniteComponent()
            );
        } else if (CapitalDiplomaticAgreementValidation.getCurrentSovereignId(target) == null) {
            enabled = false;
            reason = Component.translatable("mcacapitals.ui.diplomacy.no_sovereign_to_answer");
        } else {
            reason = CapitalDiplomaticAgreementValidation.validateProposal(
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
                reason = Component.translatable(
                        "mcacapitals.ui.diplomacy.send_proposal",
                        type.getIndefiniteComponent(),
                        capitalNameComponent(
                                CapitalDiplomaticAgreementText.capitalName(
                                        player.serverLevel(),
                                        target
                                )
                        )
                );
            }
        }

        String command = "";
        if (enabled) {
            command = type == DiplomaticProposalType.ROYAL_BETROTHAL
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

        actions.add(new OpenAmbassadorCommunicationPacket.Action(
                Component.translatable(
                        "mcacapitals.ui.diplomacy.propose",
                        type.getDisplayComponent()
                ),
                reason,
                command,
                enabled
        ));
    }

    private static void addWarAction(
            List<OpenAmbassadorCommunicationPacket.Action> actions,
            ServerPlayer player,
            UUID ambassadorId,
            CapitalRecord source,
            CapitalRecord target,
            Component targetName,
            Component label,
            String goal,
            Component outcome
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation formalAudience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);

        boolean enabled = true;
        Component reason;

        if (!formalAudience.valid()) {
            enabled = false;
            reason = formalAudience.failureMessage();
        } else {
            reason = CapitalWarPlanningService.validateRecovery(
                    player.serverLevel(),
                    source
            );

            if (reason == null) {
                CapitalCampaignEligibilityService.Validation eligibility =
                        CapitalCampaignEligibilityService.validateCampaign(
                                player.serverLevel(),
                                source,
                                target,
                                player.getUUID()
                        );

                if (!eligibility.valid()) {
                    reason = eligibility.failureMessage();
                }
            }

            if (reason != null) {
                enabled = false;
            } else {
                reason = Component.translatable(
                        "mcacapitals.ui.diplomacy.war_plan_description",
                        CapitalWarPlanningService.describePlan(
                                player.serverLevel(),
                                source,
                                target
                        ),
                        targetName,
                        outcome
                );
            }
        }

        actions.add(new OpenAmbassadorCommunicationPacket.Action(
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
        ));
    }

    private static Component statusFlags(
            boolean tradeActive,
            boolean campaignBetweenCapitals
    ) {
        MutableComponent flags = Component.empty();
        boolean hasFlag = false;

        if (tradeActive) {
            flags.append(Component.translatable("mcacapitals.ui.diplomacy.trade_active"));
            hasFlag = true;
        }

        if (campaignBetweenCapitals) {
            if (hasFlag) {
                flags.append(Component.literal(" — "));
            }
            flags.append(Component.translatable("mcacapitals.ui.diplomacy.attack_planned"));
        }

        return flags;
    }

    private static Component capitalNameComponent(String name) {
        return name == null || name.isBlank()
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(name);
    }

    private static void sendMessage(
            ServerPlayer player,
            Component title,
            Component message,
            String backCommand
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        Component.empty(),
                        message,
                        backCommand,
                        List.of(),
                        List.of()
                )
        );
    }
}