package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.data.DiplomaticShipmentStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticResolutionService {

    private CapitalDiplomaticResolutionService() {
    }

    public static boolean resolveNpcShipment(
            ServerLevel level,
            DiplomaticShipment shipment
    ) {
        if (level == null
                || shipment == null
                || shipment.getStatus()
                != DiplomaticShipmentStatus.DISPATCHED) {
            return false;
        }

        CapitalRecord sourceCapital =
                CapitalManager.getCapital(
                        shipment.getSourceCapitalId()
                );

        CapitalRecord targetCapital =
                CapitalManager.getCapital(
                        shipment.getTargetCapitalId()
                );

        if (sourceCapital == null
                || targetCapital == null) {
            return returnUndeliverable(
                    level,
                    shipment,
                    sourceCapital
            );
        }

        if (CapitalDiplomaticAuthorityService
                .getPlayerDecisionMaker(
                        level,
                        targetCapital
                ) != null) {
            shipment.setStatus(
                    DiplomaticShipmentStatus
                            .AWAITING_PLAYER_RESPONSE
            );

            CapitalDiplomacyDataAccess
                    .get(level)
                    .setDirty();

            return true;
        }

        if (targetCapital.getSovereign() == null) {
            return false;
        }

        if (shipment.getRelationshipDelta() < 0) {
            if (!CapitalDiplomaticStorageService
                    .deposit(
                            level,
                            sourceCapital,
                            shipment.getContents()
                    )) {
                return false;
            }

            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "Diplomatic insult returned"
            );

            recordReturned(
                    level,
                    sourceCapital,
                    targetCapital,
                    true
            );

            shipment.setStatus(
                    DiplomaticShipmentStatus.RETURNED
            );

            notifySource(
                    level,
                    shipment,
                    sourceCapital,
                    "Gift Returned",
                    "The court of "
                            + capitalName(
                            level,
                            targetCapital
                    )
                            + " returned the package and condemned it as an insult."
            );
        } else {
            if (!CapitalDiplomaticStorageService
                    .deposit(
                            level,
                            targetCapital,
                            shipment.getContents()
                    )) {
                return false;
            }

            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "Diplomatic gift accepted"
            );

            recordAccepted(
                    level,
                    sourceCapital,
                    targetCapital
            );

            shipment.setStatus(
                    DiplomaticShipmentStatus.ACCEPTED
            );

            notifySource(
                    level,
                    shipment,
                    sourceCapital,
                    "Gift Accepted",
                    "The court of "
                            + capitalName(
                            level,
                            targetCapital
                    )
                            + " accepted the diplomatic package."
            );
        }

        CapitalDiplomacyDataAccess.removeShipment(
                level,
                shipment.getShipmentId()
        );

        return true;
    }

    public static int acceptPlayerShipment(
            ServerPlayer player,
            UUID shipmentId
    ) {
        Validation validation =
                validatePlayerResponse(
                        player,
                        shipmentId
                );

        if (!validation.valid()) {
            player.sendSystemMessage(
                    Component.literal(
                            validation.failureMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        DiplomaticShipment shipment =
                validation.shipment();

        CapitalRecord sourceCapital =
                validation.sourceCapital();

        CapitalRecord targetCapital =
                validation.targetCapital();

        if (!CapitalDiplomaticStorageService
                .deposit(
                        level,
                        targetCapital,
                        shipment.getContents()
                )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The receiving capital's MCA village storage is unavailable."
                    )
            );

            return 0;
        }

        applyRelationship(
                level,
                sourceCapital,
                targetCapital,
                shipment.getRelationshipDelta(),
                shipment.getRelationshipDelta() < 0
                        ? "Diplomatic insult accepted"
                        : "Diplomatic gift accepted"
        );

        recordAccepted(
                level,
                sourceCapital,
                targetCapital
        );

        shipment.setStatus(
                DiplomaticShipmentStatus.ACCEPTED
        );

        notifySource(
                level,
                shipment,
                sourceCapital,
                "Gift Accepted",
                player.getName().getString()
                        + " accepted the diplomatic package on behalf of "
                        + capitalName(
                        level,
                        targetCapital
                )
                        + "."
        );

        CapitalDiplomacyDataAccess.removeShipment(
                level,
                shipment.getShipmentId()
        );

        player.sendSystemMessage(
                Component.literal(
                        "The package has been accepted and sent to the capital's MCA Storage system."
                )
        );

        return 1;
    }

    public static int returnPlayerShipment(
            ServerPlayer player,
            UUID shipmentId
    ) {
        Validation validation =
                validatePlayerResponse(
                        player,
                        shipmentId
                );

        if (!validation.valid()) {
            player.sendSystemMessage(
                    Component.literal(
                            validation.failureMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        DiplomaticShipment shipment =
                validation.shipment();

        CapitalRecord sourceCapital =
                validation.sourceCapital();

        CapitalRecord targetCapital =
                validation.targetCapital();

        if (!CapitalDiplomaticStorageService
                .deposit(
                        level,
                        sourceCapital,
                        shipment.getContents()
                )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The sending capital's MCA village storage is unavailable, so the package cannot yet be returned."
                    )
            );

            return 0;
        }

        boolean insulting =
                shipment.getRelationshipDelta() < 0;

        if (insulting) {
            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "Diplomatic insult returned"
            );
        }

        recordReturned(
                level,
                sourceCapital,
                targetCapital,
                insulting
        );

        shipment.setStatus(
                DiplomaticShipmentStatus.RETURNED
        );

        notifySource(
                level,
                shipment,
                sourceCapital,
                "Gift Returned",
                player.getName().getString()
                        + " returned the diplomatic package from "
                        + capitalName(
                        level,
                        targetCapital
                )
                        + "."
        );

        CapitalDiplomacyDataAccess.removeShipment(
                level,
                shipment.getShipmentId()
        );

        player.sendSystemMessage(
                Component.literal(
                        "The package has been returned to the sending capital's MCA Storage system."
                )
        );

        return 1;
    }

    public static List<DiplomaticShipment>
    getPendingForPlayer(
            ServerLevel level,
            UUID playerId
    ) {
        if (level == null || playerId == null) {
            return List.of();
        }

        List<DiplomaticShipment> result =
                new ArrayList<>();

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            if (capital == null
                    || capital.getState()
                    != CapitalState.ACTIVE
                    || !CapitalDiplomaticAuthorityService
                    .mayExerciseSovereignAuthority(
                            level,
                            capital,
                            playerId
                    )) {
                continue;
            }

            result.addAll(
                    CapitalDiplomacyDataAccess
                            .getPendingPlayerShipments(
                                    level,
                                    capital.getCapitalId()
                            )
            );
        }

        result.sort(
                Comparator.comparingLong(
                        DiplomaticShipment::getCreatedAt
                )
        );

        return result;
    }

    public static boolean returnUndeliverable(
            ServerLevel level,
            DiplomaticShipment shipment,
            CapitalRecord sourceCapital
    ) {
        if (level == null
                || shipment == null
                || sourceCapital == null) {
            return false;
        }

        if (!CapitalDiplomaticStorageService
                .deposit(
                        level,
                        sourceCapital,
                        shipment.getContents()
                )) {
            return false;
        }

        notifySource(
                level,
                shipment,
                sourceCapital,
                "Package Undeliverable",
                "The destination capital no longer exists. The package was returned to your capital's MCA Storage system."
        );

        CapitalDiplomacyDataAccess.removeShipment(
                level,
                shipment.getShipmentId()
        );

        return true;
    }

    private static Validation validatePlayerResponse(
            ServerPlayer player,
            UUID shipmentId
    ) {
        if (player == null || shipmentId == null) {
            return Validation.failure(
                    "That diplomatic package is invalid."
            );
        }

        ServerLevel level =
                player.serverLevel();

        DiplomaticShipment shipment =
                CapitalDiplomacyDataAccess
                        .getShipment(
                                level,
                                shipmentId
                        );

        if (shipment == null
                || !shipment
                .isAwaitingPlayerResponse()) {
            return Validation.failure(
                    "That diplomatic package is no longer awaiting a response."
            );
        }

        CapitalRecord sourceCapital =
                CapitalManager.getCapital(
                        shipment.getSourceCapitalId()
                );

        CapitalRecord targetCapital =
                CapitalManager.getCapital(
                        shipment.getTargetCapitalId()
                );

        if (sourceCapital == null
                || targetCapital == null
                || targetCapital.getState()
                != CapitalState.ACTIVE) {
            return Validation.failure(
                    "One of the capitals connected to this package no longer exists."
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        targetCapital,
                        player.getUUID()
                )) {
            return Validation.failure(
                    "Only the player sovereign, or the player Hand serving a villager sovereign, may answer this package."
            );
        }

        return Validation.success(
                shipment,
                sourceCapital,
                targetCapital
        );
    }

    private static void applyRelationship(
            ServerLevel level,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            int amount,
            String reason
    ) {
        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        sourceCapital.getCapitalId(),
                        targetCapital.getCapitalId(),
                        amount,
                        reason,
                        sourceCapital.getCapitalId()
                );
    }

    private static void recordAccepted(
            ServerLevel level,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital
    ) {
        String sourceName =
                capitalName(
                        level,
                        sourceCapital
                );

        String targetName =
                capitalName(
                        level,
                        targetCapital
                );

        long day = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        CapitalChronicleService.addEntry(
                level,
                sourceCapital,
                "On day "
                        + day
                        + ", the court of "
                        + targetName
                        + " accepted a diplomatic package from "
                        + sourceName
                        + "."
        );

        CapitalChronicleService.addEntry(
                level,
                targetCapital,
                "On day "
                        + day
                        + ", a diplomatic package from "
                        + sourceName
                        + " was accepted by the court of "
                        + targetName
                        + "."
        );
    }

    private static void recordReturned(
            ServerLevel level,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            boolean insulting
    ) {
        String sourceName =
                capitalName(
                        level,
                        sourceCapital
                );

        String targetName =
                capitalName(
                        level,
                        targetCapital
                );

        long day = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        String sourceEntry =
                insulting
                        ? "On day "
                        + day
                        + ", the court of "
                        + targetName
                        + " condemned and returned a diplomatic package from "
                        + sourceName
                        + "."
                        : "On day "
                        + day
                        + ", the court of "
                        + targetName
                        + " returned a diplomatic package from "
                        + sourceName
                        + ".";

        String targetEntry =
                insulting
                        ? "On day "
                        + day
                        + ", a diplomatic package from "
                        + sourceName
                        + " was condemned as an insult and returned by "
                        + targetName
                        + "."
                        : "On day "
                        + day
                        + ", a diplomatic package from "
                        + sourceName
                        + " was returned by "
                        + targetName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                sourceCapital,
                sourceEntry
        );

        CapitalChronicleService.addEntry(
                level,
                targetCapital,
                targetEntry
        );
    }

    private static void notifySource(
            ServerLevel level,
            DiplomaticShipment shipment,
            CapitalRecord sourceCapital,
            String title,
            String message
    ) {
        UUID recipient =
                sourceCapital.getPlayerSovereignId();

        if (recipient == null) {
            recipient =
                    shipment.getSenderSovereignId();
        }

        if (recipient == null) {
            return;
        }

        CapitalDiplomaticCorrespondenceService
                .sendResolutionLetter(
                        level,
                        recipient,
                        title,
                        message
                );
    }

    private static String capitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalDiplomaticCorrespondenceService
                .getCapitalName(
                        level,
                        capital
                );
    }

    private record Validation(
            boolean valid,
            DiplomaticShipment shipment,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            String failureMessage
    ) {

        private static Validation success(
                DiplomaticShipment shipment,
                CapitalRecord sourceCapital,
                CapitalRecord targetCapital
        ) {
            return new Validation(
                    true,
                    shipment,
                    sourceCapital,
                    targetCapital,
                    null
            );
        }

        private static Validation failure(
                String message
        ) {
            return new Validation(
                    false,
                    null,
                    null,
                    null,
                    message
            );
        }
    }
}