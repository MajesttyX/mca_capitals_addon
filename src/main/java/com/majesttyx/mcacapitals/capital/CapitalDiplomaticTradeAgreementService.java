package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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

    static boolean isActive(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        CapitalTradeAgreement agreement = getAgreement(level, first, second);
        return agreement != null && !agreement.isExpired(level.getGameTime());
    }

    static boolean isRenewal(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        return getAgreement(level, first, second) != null;
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

    static boolean mayRenew(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        CapitalTradeAgreement agreement = getAgreement(level, first, second);
        if (agreement == null) {
            return false;
        }

        if (agreement.needsTermInitialization()) {
            agreement.initializeLegacyTerm(level.getGameTime());
            CapitalAgreementDataAccess.markTradeAgreementChanged(level);
        }

        if (agreement.isExpired(level.getGameTime())) {
            return false;
        }

        return agreement.isInRenewalWindow(level.getGameTime());
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
                || first.getCapitalId().equals(second.getCapitalId())) {
            return Component.translatable("mcacapitals.diplomacy.trade.validation.invalid");
        }

        if (first.getState() != CapitalState.ACTIVE
                || second.getState() != CapitalState.ACTIVE) {
            return Component.translatable("mcacapitals.diplomacy.trade.validation.both_active");
        }

        CapitalDiplomaticState diplomaticState = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );

        if (diplomaticState == CapitalDiplomaticState.WAR
                || diplomaticState == CapitalDiplomaticState.TRUCE) {
            return Component.translatable("mcacapitals.diplomacy.trade.validation.war_or_truce");
        }

        int relationship = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );

        if (relationship < MINIMUM_RELATIONSHIP) {
            return Component.translatable("mcacapitals.diplomacy.trade.validation.cordial_required");
        }

        if (!CapitalBuildingService.hasStorage(level, first)
                || !CapitalBuildingService.hasStorage(level, second)) {
            return Component.translatable("mcacapitals.diplomacy.trade.validation.storage_required");
        }

        CapitalTradeAgreement existing = getAgreement(level, first, second);
        if (existing != null) {
            if (existing.needsTermInitialization()) {
                existing.initializeLegacyTerm(level.getGameTime());
                CapitalAgreementDataAccess.markTradeAgreementChanged(level);
            }

            if (existing.isExpired(level.getGameTime())) {
                return Component.translatable("mcacapitals.diplomacy.trade.validation.expired_must_clear");
            }

            if (!existing.isInRenewalWindow(level.getGameTime())) {
                return Component.translatable("mcacapitals.diplomacy.trade.validation.renewal_window");
            }
        }

        return null;
    }

    static boolean establish(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        CapitalTradeAgreement existing = getAgreement(level, first, second);
        boolean renewal = existing != null;

        if (validateEstablishment(level, first, second) != null) {
            return false;
        }

        CapitalTradeAgreement agreement;
        if (renewal) {
            if (!CapitalAgreementDataAccess.renewTradeAgreement(
                    level,
                    first.getCapitalId(),
                    second.getCapitalId()
            )) {
                return false;
            }
            agreement = existing;
        } else {
            agreement = CapitalAgreementDataAccess.establishTradeAgreement(
                    level,
                    first.getCapitalId(),
                    second.getCapitalId()
            );
        }

        if (agreement == null) {
            return false;
        }

        String firstName = CapitalDiplomaticAgreementText.capitalName(level, first);
        String secondName = CapitalDiplomaticAgreementText.capitalName(level, second);
        CapitalChronicleEventId eventId = renewal
                ? CapitalChronicleEventId.TRADE_AGREEMENT_RENEWED
                : CapitalChronicleEventId.TRADE_AGREEMENT_CREATED;

        CapitalChronicleService.addEvent(level, first, eventId, firstName, secondName);
        CapitalChronicleService.addEvent(level, second, eventId, firstName, secondName);
        return true;
    }

    static int endByPlayer(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null || ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);

        if (!audience.valid()) {
            player.sendSystemMessage(audience.failureMessage());
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);

        Component targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(source, target);
        if (targetFailure != null) {
            player.sendSystemMessage(targetFailure);
            return 0;
        }

        if (getAgreement(level, source, target) == null) {
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.capital_diplomatic_trade_agreement_service.these_capitals_do_not_have_an_active_trade_agreement"
            ));
            return 0;
        }

        if (!end(level, source, target, TradeAgreementEndReason.REQUESTED)) {
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.capital_diplomatic_trade_agreement_service.the_trade_agreement_could_not_be_ended"
            ));
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

        String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);
        UUID targetPlayer = CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(level, target);
        if (targetPlayer != null) {
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    targetPlayer,
                    Component.translatable("mcacapitals.diplomacy.trade.ended_title"),
                    Component.translatable(
                            "mcacapitals.diplomacy.trade.ended_by_other",
                            CapitalDiplomaticAgreementText.capitalName(level, source),
                            targetName
                    )
            );
        }

        player.sendSystemMessage(Component.translatable(
                "mcacapitals.diplomacy.trade.ended_with",
                targetName
        ));
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

        boolean removed = CapitalAgreementDataAccess.endTradeAgreement(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
        if (!removed) {
            return false;
        }

        String firstName = CapitalDiplomaticAgreementText.capitalName(level, first);
        String secondName = CapitalDiplomaticAgreementText.capitalName(level, second);
        CapitalChronicleEventId eventId = reason == null
                ? CapitalChronicleEventId.TRADE_AGREEMENT_ENDED
                : switch (reason) {
                    case REQUESTED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_REQUESTED;
                    case TERM_EXPIRED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_TERM_EXPIRED;
                    case MILITARY_ATTACK -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_MILITARY_ATTACK;
                    case WAR_DECLARED -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_WAR_DECLARED;
                    case TRADE_IMPOSSIBLE -> CapitalChronicleEventId.TRADE_AGREEMENT_ENDED_TRADE_IMPOSSIBLE;
                };

        CapitalChronicleService.addEvent(level, first, eventId, firstName, secondName);
        CapitalChronicleService.addEvent(level, second, eventId, firstName, secondName);
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
}
