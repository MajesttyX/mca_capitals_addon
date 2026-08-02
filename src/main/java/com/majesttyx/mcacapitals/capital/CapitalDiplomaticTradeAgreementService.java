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

    static String validateEstablishment(
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
            return "That Trade Agreement is invalid.";
        }

        if (first.getState() != CapitalState.ACTIVE
                || second.getState() != CapitalState.ACTIVE) {
            return "Both capitals must be active before establishing trade.";
        }

        CapitalDiplomaticState diplomaticState =
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (diplomaticState == CapitalDiplomaticState.WAR
                || diplomaticState == CapitalDiplomaticState.TRUCE) {
            return "A Trade Agreement cannot be established during War or an active Truce.";
        }

        int relationship =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (relationship < MINIMUM_RELATIONSHIP) {
            return "Relations must be Cordial or better before establishing a Trade Agreement.";
        }

        if (!CapitalBuildingService.hasStorage(level, first)
                || !CapitalBuildingService.hasStorage(level, second)) {
            return "Both capitals require an operational MCA Storage building before establishing trade.";
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
                return "The current Trade Agreement has expired and must be cleared before another can be proposed.";
            }

            if (!existing.isInRenewalWindow(
                    level.getGameTime()
            )) {
                return "These capitals already have an active Trade Agreement. Renewal becomes available during the final two Minecraft days of its term.";
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

        String entry = renewal
                ? firstName
                + " and "
                + secondName
                + " renewed their Trade Agreement for another thirteen Minecraft days."
                : firstName
                + " and "
                + secondName
                + " established a Trade Agreement lasting thirteen Minecraft days.";

        CapitalChronicleService.addEntry(
                level,
                first,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                second,
                entry
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
                    Component.literal(
                            audience.failureMessage()
                    )
            );
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
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

        if (!isActive(level, source, target)) {
            player.sendSystemMessage(
                    Component.literal(
                            "These capitals do not have an active Trade Agreement."
                    )
            );
            return 0;
        }

        if (!end(
                level,
                source,
                target,
                "at the request of "
                        + CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                source
                        )
                        + "."
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The Trade Agreement could not be ended."
                    )
            );
            return 0;
        }

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                source.getCapitalId(),
                target.getCapitalId(),
                -5,
                "Trade Agreement ended",
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
                            "Trade Agreement Ended",
                            CapitalDiplomaticAgreementText
                                    .capitalName(
                                            level,
                                            source
                                    )
                                    + " ended its Trade Agreement with "
                                    + targetName
                                    + "."
                    );
        }

        player.sendSystemMessage(
                Component.literal(
                        "The Trade Agreement with "
                                + targetName
                                + " has ended."
                )
        );

        return 1;
    }

    static boolean end(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second,
            String reason
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

        String suffix = reason == null
                || reason.isBlank()
                ? "."
                : " " + reason.trim();

        String entry =
                "The Trade Agreement between "
                        + firstName
                        + " and "
                        + secondName
                        + " ended"
                        + suffix;

        CapitalChronicleService.addEntry(
                level,
                first,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                second,
                entry
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

        CapitalChronicleService.addEntry(
                level,
                source,
                "A renewal of the Trade Agreement with "
                        + CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                target
                        )
                        + " was proposed."
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

        CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                level,
                firstPlayer,
                "Trade Agreement Renewal Available",
                "The Trade Agreement between "
                        + firstName
                        + " and "
                        + secondName
                        + " expires in two Minecraft days. Speak to the Ambassador to propose its renewal."
        );

        if (!secondPlayer.equals(firstPlayer)) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            secondPlayer,
                            "Trade Agreement Renewal Available",
                            "The Trade Agreement between "
                                    + firstName
                                    + " and "
                                    + secondName
                                    + " expires in two Minecraft days. Speak to the Ambassador to propose its renewal."
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
                "after its thirteen-day term expired."
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
                            "Trade Agreement Expired",
                            "The Trade Agreement with "
                                    + otherForFirst
                                    + " reached the end of its thirteen-day term without renewal."
                    );
        }

        if (secondPlayer != null
                && !secondPlayer.equals(firstPlayer)) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            secondPlayer,
                            "Trade Agreement Expired",
                            "The Trade Agreement with "
                                    + otherForSecond
                                    + " reached the end of its thirteen-day term without renewal."
                    );
        }
    }
}
