package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class CapitalDiplomaticTradeAgreementService {

    static final int MINIMUM_RELATIONSHIP = 10;

    private CapitalDiplomaticTradeAgreementService() {
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
            return "That trade agreement is invalid.";
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

        if (isActive(level, first, second)) {
            return "These capitals already have an active Trade Agreement.";
        }

        return null;
    }

    static boolean establish(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (validateEstablishment(
                level,
                first,
                second
        ) != null) {
            return false;
        }

        CapitalTradeAgreement agreement =
                CapitalAgreementDataAccess.establishTradeAgreement(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

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

        String entry =
                firstName
                        + " and "
                        + secondName
                        + " established a Trade Agreement.";

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

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(
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
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);

        String targetFailure =
                CapitalDiplomaticAgreementValidation.validateTarget(
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
                        + CapitalDiplomaticAgreementText.capitalName(
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

        if (target.getPlayerSovereignId() != null) {
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    target.getPlayerSovereignId(),
                    "Trade Agreement Ended",
                    CapitalDiplomaticAgreementText.capitalName(
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

        String suffix = reason == null || reason.isBlank()
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
}