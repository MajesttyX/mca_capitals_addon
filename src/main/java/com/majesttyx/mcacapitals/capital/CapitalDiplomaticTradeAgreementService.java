package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

final class CapitalDiplomaticTradeAgreementService {

    static final int MINIMUM_RELATIONSHIP = 30;

    enum TradeAgreementEndReason {
        REQUESTED,
        TERM_EXPIRED,
        MILITARY_ATTACK,
        WAR_DECLARED,
        TRADE_IMPOSSIBLE
    }

    private CapitalDiplomaticTradeAgreementService() {
    }

    static void tick(ServerLevel level) {
        if (level == null) {
            return;
        }

        Map<CapitalRelationKey, CapitalTradeAgreement> agreements =
                CapitalAgreementDataAccess
                        .getTradeAgreementsSnapshot(level);

        for (CapitalTradeAgreement agreement :
                agreements.values()) {
            if (agreement == null) {
                continue;
            }

            CapitalRecord first = CapitalManager.getCapital(
                    agreement.getFirstCapitalId()
            );

            CapitalRecord second = CapitalManager.getCapital(
                    agreement.getSecondCapitalId()
            );

            if (first == null || second == null) {
                CapitalAgreementDataAccess.endTradeAgreement(
                        level,
                        agreement.getFirstCapitalId(),
                        agreement.getSecondCapitalId()
                );
                continue;
            }

            if (agreement.needsTermInitialization()) {
                agreement.initializeLegacyTerm(
                        level.getGameTime()
                );
                CapitalAgreementDataAccess.get(level)
                        .setDirty();
            }

            if (agreement.isExpired(
                    level.getGameTime()
            )) {
                expireAgreement(
                        level,
                        first,
                        second
                );
                continue;
            }

            CapitalTradeExchangeService.processDueTrade(
                    level,
                    agreement
            );

            if (!isActive(level, first, second)) {
                continue;
            }

            processRenewalWindow(
                    level,
                    agreement,
                    first,
                    second
            );
        }
    }

    static boolean isActive(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (level == null
                || first == null
                || second == null
                || first.getCapitalId() == null
                || second.getCapitalId() == null) {
            return false;
        }

        return CapitalAgreementDataAccess.hasTradeAgreement(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
    }

    static boolean isRenewal(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        return getAgreement(
                level,
                first,
                second
        ) != null;
    }

    static CapitalTradeAgreement getAgreement(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (level == null
                || first == null
                || second == null
                || first.getCapitalId() == null
                || second.getCapitalId() == null) {
            return null;
        }

        return CapitalAgreementDataAccess.getTradeAgreement(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
    }

    static Component validateEstablishment(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (level == null
                || first == null
                || second == null
                || first.getCapitalId() == null
                || second.getCapitalId() == null
                || first.getCapitalId().equals(
                second.getCapitalId()
        )) {
            return Component.translatable(
                    "mcacapitals.diplomacy.trade.validation.invalid"
            );
        }

        if (first.getState() != CapitalState.ACTIVE
                || second.getState() != CapitalState.ACTIVE) {
            return Component.translatable(
                    "mcacapitals.diplomacy.trade.validation.both_active"
            );
        }

        CapitalDiplomaticState diplomaticState =
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (diplomaticState == CapitalDiplomaticState.WAR
                || diplomaticState == CapitalDiplomaticState.TRUCE) {
            return Component.translatable(
                    "mcacapitals.diplomacy.trade.validation.war_or_truce"
            );
        }

        int relationship =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (relationship < MINIMUM_RELATIONSHIP) {
            return Component.translatable(
                    "mcacapitals.diplomacy.trade.validation.cordial_required"
            );
        }

        if (!CapitalBuildingService.hasStorage(level, first)
                || !CapitalBuildingService.hasStorage(level, second)) {
            return Component.translatable(
                    "mcacapitals.diplomacy.trade.validation.storage_required"
            );
        }

        CapitalTradeAgreement existing = getAgreement(
                level,
                first,
                second
        );

        if (existing != null) {
            if (existing.needsTermInitialization()) {
                existing.initializeLegacyTerm(
                        level.getGameTime()
                );
                CapitalAgreementDataAccess.get(level)
                        .setDirty();
            }

            if (existing.isExpired(
                    level.getGameTime()
            )) {
                return Component.translatable(
                        "mcacapitals.diplomacy.trade.validation.expired_must_clear"
                );
            }

            if (!existing.isInRenewalWindow(
                    level.getGameTime()
            )) {
                return Component.translatable(
                        "mcacapitals.diplomacy.trade.validation.renewal_window"
                );
            }
        }

        return null;
    }

    static boolean establish(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        CapitalTradeAgreement existing = getAgreement(
                level,
                first,
                second
        );

        boolean renewal = existing != null;

        if (validateEstablishment(
                level,
                first,
                second
        ) != null) {
            return false;
        }

        CapitalTradeAgreement agreement;

        if (renewal) {
            existing.renewTerm(
                    level.getGameTime()
            );
            CapitalAgreementDataAccess.get(level)
                    .setDirty();
            agreement = existing;
        } else {
            agreement =
                    CapitalAgreementDataAccess
                            .establishTradeAgreement(
                                    level,
                                    first.getCapitalId(),
                                    second.getCapitalId()
                            );
        }

        if (agreement == null) {
            return false;
        }

        String firstName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );

        String secondName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        CapitalChronicleEventId eventId = renewal
                ? CapitalChronicleEventId.TRADE_AGREEMENT_RENEWED
                : CapitalChronicleEventId.TRADE_AGREEMENT_CREATED;

        CapitalChronicleService.addEvent(
                level,
                first,
                eventId,
                firstName,
                secondName
        );

        CapitalChronicleService.addEvent(
                level,
                second,
                eventId,
                firstName,
                secondName
        );

        return true;
    }

    static int endByPlayer(
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
                    audience.failureMessage()
            );
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        CapitalRecord target =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        Component targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                source,
                                target
                        );

        if (targetFailure != null) {
            player.sendSystemMessage(
                    targetFailure
            );
            return 0;
        }

        if (!isActive(level, source, target)) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_diplomatic_trade_agreement_service.these_capitals_do_not_have_an_active_trade_agreement")
            );
            return 0;
        }

        if (!end(
                level,
                source,
                target,
                TradeAgreementEndReason.REQUESTED
        )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_diplomatic_trade_agreement_service.the_trade_agreement_could_not_be_ended")
            );
            return 0;
        }

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                source.getCapitalId(),
                target.getCapitalId(),
                -5,
                "mcacapitals.relationship_reason.trade_agreement_ended",
                source.getCapitalId()
        );

        String targetName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        target
                );

        UUID targetDecisionMaker =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetDecisionMaker != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetDecisionMaker,
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.ended_title"
                            ),
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.ended_by_other",
                                    CapitalDiplomaticAgreementText
                                            .capitalName(
                                                    level,
                                                    source
                                            ),
                                    targetName
                            )
                    );
        }

        player.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.trade.ended_with",
                        targetName
                )
        );

        return 1;
    }

    static boolean end(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second,
            TradeAgreementEndReason reason
    ) {
        if (level == null
                || first == null
                || second == null
                || first.getCapitalId() == null
                || second.getCapitalId() == null) {
            return false;
        }

        boolean removed =
                CapitalAgreementDataAccess.endTradeAgreement(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (!removed) {
            return false;
        }

        String firstName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );

        String secondName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        CapitalChronicleEventId eventId = reason == null
                ? CapitalChronicleEventId.TRADE_AGREEMENT_ENDED
                : switch (reason) {
                    case REQUESTED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_REQUESTED;
                    case TERM_EXPIRED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_TERM_EXPIRED;
                    case MILITARY_ATTACK -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_MILITARY_ATTACK;
                    case WAR_DECLARED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_WAR_DECLARED;
                    case TRADE_IMPOSSIBLE -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_TRADE_IMPOSSIBLE;
                };

        CapitalChronicleService.addEvent(
                level,
                first,
                eventId,
                firstName,
                secondName
        );

        CapitalChronicleService.addEvent(
                level,
                second,
                eventId,
                firstName,
                secondName
        );

        return true;
    }

    static boolean markTradeCompleted(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (!isActive(level, first, second)) {
            return false;
        }

        return CapitalAgreementDataAccess.markTradeCompleted(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
    }

    private static void processRenewalWindow(
            ServerLevel level,
            CapitalTradeAgreement agreement,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (!agreement.isInRenewalWindow(
                level.getGameTime()
        )) {
            return;
        }

        DiplomaticProposal pending =
                CapitalAgreementDataAccess.findPendingBetween(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (pending != null) {
            if (pending.getType()
                    == DiplomaticProposalType.TRADE_AGREEMENT
                    && !agreement.isRenewalProposalCreated()) {
                agreement.markRenewalProposalCreated();
                CapitalAgreementDataAccess.get(level)
                        .setDirty();
            }
            return;
        }

        if (agreement.isRenewalProposalCreated()) {
            return;
        }

        UUID firstPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                first
                        );

        UUID secondPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                second
                        );

        if (firstPlayer != null
                && secondPlayer != null) {
            notifyPlayerRenewalWindow(
                    level,
                    agreement,
                    first,
                    second,
                    firstPlayer,
                    secondPlayer
            );
            return;
        }

        CapitalRecord source;
        CapitalRecord target;

        if (firstPlayer == null
                && first.getSovereign() != null) {
            source = first;
            target = second;
        } else if (secondPlayer == null
                && second.getSovereign() != null) {
            source = second;
            target = first;
        } else {
            return;
        }

        if (!CapitalBuildingService.hasAmbassadorBuildings(
                level,
                source
        )
                || CapitalAmbassadorService.getAmbassador(
                level,
                source
        ) == null
                || validateEstablishment(
                level,
                source,
                target
        ) != null) {
            return;
        }

        UUID sourceSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(source);

        UUID targetSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(target);

        if (sourceSovereignId == null
                || targetSovereignId == null) {
            return;
        }

        long createdAt = level.getGameTime();

        DiplomaticProposal proposal =
                new DiplomaticProposal(
                        UUID.randomUUID(),
                        source.getCapitalId(),
                        target.getCapitalId(),
                        sourceSovereignId,
                        targetSovereignId,
                        null,
                        DiplomaticProposalType.TRADE_AGREEMENT,
                        createdAt,
                        CapitalDiplomaticDelayService.schedule(level),
                        null,
                        null,
                        null,
                        null
                );

        CapitalAgreementDataAccess.addProposal(
                level,
                proposal
        );

        agreement.markRenewalProposalCreated();
        CapitalAgreementDataAccess.get(level)
                .setDirty();

        CapitalChronicleService.addEvent(
                level,
                source,
                CapitalChronicleEventId.TRADE_AGREEMENT_RENEWAL_PROPOSED,
                CapitalDiplomaticAgreementText.capitalName(level, target)
        );
    }

    private static void notifyPlayerRenewalWindow(
            ServerLevel level,
            CapitalTradeAgreement agreement,
            CapitalRecord first,
            CapitalRecord second,
            UUID firstPlayer,
            UUID secondPlayer
    ) {
        if (agreement.isRenewalNoticeSent()) {
            return;
        }

        String firstName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );

        String secondName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        Component renewalTitle =
                Component.translatable(
                        "mcacapitals.diplomacy.trade.renewal_title"
                );
        Component renewalMessage =
                Component.translatable(
                        "mcacapitals.diplomacy.trade.renewal_message",
                        firstName,
                        secondName
                );

        CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                level,
                firstPlayer,
                renewalTitle,
                renewalMessage
        );

        if (!secondPlayer.equals(firstPlayer)) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            secondPlayer,
                            renewalTitle,
                            renewalMessage
                    );
        }

        agreement.markRenewalNoticeSent();
        CapitalAgreementDataAccess.get(level)
                .setDirty();
    }

    private static void expireAgreement(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        DiplomaticProposal pending =
                CapitalAgreementDataAccess.findPendingBetween(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (pending != null
                && pending.getType()
                == DiplomaticProposalType.TRADE_AGREEMENT) {
            CapitalAgreementDataAccess.removeProposal(
                    level,
                    pending.getProposalId()
            );
        }

        if (!end(
                level,
                first,
                second,
                TradeAgreementEndReason.TERM_EXPIRED
        )) {
            return;
        }

        String otherForFirst =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        String otherForSecond =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );

        UUID firstPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                first
                        );

        UUID secondPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                second
                        );

        if (firstPlayer != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            firstPlayer,
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.expired_title"
                            ),
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.expired_message",
                                    otherForFirst
                            )
                    );
        }

        if (secondPlayer != null
                && !secondPlayer.equals(firstPlayer)) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            secondPlayer,
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.expired_title"
                            ),
                            Component.translatable(
                                    "mcacapitals.diplomacy.trade.expired_message",
                                    otherForSecond
                            )
                    );
        }
    }
}
