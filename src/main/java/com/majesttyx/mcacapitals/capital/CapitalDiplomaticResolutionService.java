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
            recordGraveInsult(level, sourceCapital, targetCapital, shipment);
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

            recordGraveInsult(level, sourceCapital, targetCapital, shipment);
            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "mcacapitals.relationship_reason.diplomatic_insult_returned"
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
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.returned_title"
                    ),
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.returned_condemned",
                            capitalName(
                                    level,
                                    targetCapital
                            )
                    )
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

            recordGraveInsult(level, sourceCapital, targetCapital, shipment);
            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "mcacapitals.relationship_reason.diplomatic_gift_accepted"
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
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.accepted_title"
                    ),
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.accepted_by_court",
                            capitalName(
                                    level,
                                    targetCapital
                            )
                    )
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
        Validation validation = validatePlayerResponse(player, shipmentId);
        if (!validation.valid()) {
            player.sendSystemMessage(validation.failureMessage());
            return 0;
        }

        ServerLevel level = player.serverLevel();
        DiplomaticShipment shipment = validation.shipment();
        CapitalRecord sourceCapital = validation.sourceCapital();
        CapitalRecord targetCapital = validation.targetCapital();

        if (!CapitalDiplomaticStorageService.deposit(level, targetCapital, shipment.getContents())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_diplomatic_resolution_service.the_receiving_capital_s_mca_village_storage_is_unavailable"));
            return 0;
        }

        applyRelationship(
                level,
                sourceCapital,
                targetCapital,
                shipment.getRelationshipDelta(),
                shipment.getRelationshipDelta() < 0
                        ? "mcacapitals.relationship_reason.diplomatic_insult_accepted"
                        : "mcacapitals.relationship_reason.diplomatic_gift_accepted"
        );
        recordAccepted(level, sourceCapital, targetCapital);
        shipment.setStatus(DiplomaticShipmentStatus.ACCEPTED_RESPONSE_IN_TRANSIT);
        shipment.setAvailableAt(CapitalDiplomaticDelayService.schedule(level));
        CapitalDiplomacyDataAccess.get(level).setDirty();

        player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_diplomatic_resolution_service.the_package_has_been_accepted_and_sent_to_the_capital_s_mca_storage_sy"));
        return 1;
    }

    public static int returnPlayerShipment(
            ServerPlayer player,
            UUID shipmentId
    ) {
        Validation validation = validatePlayerResponse(player, shipmentId);
        if (!validation.valid()) {
            player.sendSystemMessage(validation.failureMessage());
            return 0;
        }

        ServerLevel level = player.serverLevel();
        DiplomaticShipment shipment = validation.shipment();
        shipment.setStatus(DiplomaticShipmentStatus.RETURNED_IN_TRANSIT);
        shipment.setAvailableAt(CapitalDiplomaticDelayService.schedule(level));
        CapitalDiplomacyDataAccess.get(level).setDirty();

        player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_diplomatic_resolution_service.the_package_has_been_sent_back_it_may_take_one_to_five_minutes_to_reac"));
        return 1;
    }

    public static boolean completeAcceptedResponse(
            ServerLevel level,
            DiplomaticShipment shipment
    ) {
        if (level == null || shipment == null
                || shipment.getStatus() != DiplomaticShipmentStatus.ACCEPTED_RESPONSE_IN_TRANSIT) {
            return false;
        }

        CapitalRecord sourceCapital = CapitalManager.getCapital(shipment.getSourceCapitalId());
        CapitalRecord targetCapital = CapitalManager.getCapital(shipment.getTargetCapitalId());
        if (sourceCapital == null) {
            CapitalDiplomacyDataAccess.removeShipment(level, shipment.getShipmentId());
            return true;
        }

        shipment.setStatus(DiplomaticShipmentStatus.ACCEPTED);
        notifySource(
                level,
                shipment,
                sourceCapital,
                Component.translatable(
                        "mcacapitals.diplomacy.gift.accepted_title"
                ),
                Component.translatable(
                        "mcacapitals.diplomacy.gift.accepted_by_court",
                        capitalName(level, targetCapital)
                )
        );
        CapitalDiplomacyDataAccess.removeShipment(level, shipment.getShipmentId());
        return true;
    }

    public static boolean completeReturnedResponse(
            ServerLevel level,
            DiplomaticShipment shipment
    ) {
        if (level == null || shipment == null
                || shipment.getStatus() != DiplomaticShipmentStatus.RETURNED_IN_TRANSIT) {
            return false;
        }

        CapitalRecord sourceCapital = CapitalManager.getCapital(shipment.getSourceCapitalId());
        CapitalRecord targetCapital = CapitalManager.getCapital(shipment.getTargetCapitalId());
        if (sourceCapital == null) {
            CapitalDiplomacyDataAccess.removeShipment(level, shipment.getShipmentId());
            return true;
        }

        if (!CapitalDiplomaticStorageService.deposit(level, sourceCapital, shipment.getContents())) {
            return false;
        }

        boolean insulting = shipment.getRelationshipDelta() < 0;
        if (insulting && targetCapital != null) {
            applyRelationship(
                    level,
                    sourceCapital,
                    targetCapital,
                    shipment.getRelationshipDelta(),
                    "mcacapitals.relationship_reason.diplomatic_insult_returned"
            );
        }
        if (targetCapital != null) {
            recordReturned(level, sourceCapital, targetCapital, insulting);
        }

        shipment.setStatus(DiplomaticShipmentStatus.RETURNED);
        notifySource(
                level,
                shipment,
                sourceCapital,
                Component.translatable(
                        "mcacapitals.diplomacy.gift.returned_title"
                ),
                Component.translatable(
                        "mcacapitals.diplomacy.gift.returned_from",
                        capitalName(level, targetCapital)
                )
        );
        CapitalDiplomacyDataAccess.removeShipment(level, shipment.getShipmentId());
        return true;
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
                Component.translatable(
                        "mcacapitals.diplomacy.gift.undeliverable_title"
                ),
                Component.translatable(
                        "mcacapitals.diplomacy.gift.undeliverable_message"
                )
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
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.validation.package_invalid"
                    )
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
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.validation.no_longer_awaiting"
                    )
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
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.validation.capital_missing"
                    )
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        targetCapital,
                        player.getUUID()
                )) {
            return Validation.failure(
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.validation.answer_authority"
                    )
            );
        }

        return Validation.success(
                shipment,
                sourceCapital,
                targetCapital
        );
    }

    private static void recordGraveInsult(
            ServerLevel level,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            DiplomaticShipment shipment
    ) {
        if (level == null
                || sourceCapital == null
                || targetCapital == null
                || shipment == null
                || !CapitalGiftAppraisalService.isGraveInsult(shipment.getAppraisal())) {
            return;
        }

        CapitalChronicleService.addEvent(level, targetCapital, CapitalChronicleEventId.DIPLOMATIC_PACKAGE_GRAVE_INSULT, capitalName(level, sourceCapital), capitalName(level, targetCapital));
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
        String sourceName = capitalName(level, sourceCapital);
        String targetName = capitalName(level, targetCapital);

        CapitalChronicleService.addEvent(
                level,
                sourceCapital,
                CapitalChronicleEventId.DIPLOMATIC_PACKAGE_ACCEPTED_SOURCE,
                sourceName,
                targetName
        );

        CapitalChronicleService.addEvent(
                level,
                targetCapital,
                CapitalChronicleEventId.DIPLOMATIC_PACKAGE_ACCEPTED_TARGET,
                sourceName,
                targetName
        );
    }

    private static void recordReturned(
            ServerLevel level,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            boolean insulting
    ) {
        String sourceName = capitalName(level, sourceCapital);
        String targetName = capitalName(level, targetCapital);

        CapitalChronicleService.addEvent(
                level,
                sourceCapital,
                insulting
                        ? CapitalChronicleEventId.DIPLOMATIC_PACKAGE_INSULT_RETURNED_SOURCE
                        : CapitalChronicleEventId.DIPLOMATIC_PACKAGE_RETURNED_SOURCE,
                targetName,
                sourceName
        );

        CapitalChronicleService.addEvent(
                level,
                targetCapital,
                insulting
                        ? CapitalChronicleEventId.DIPLOMATIC_PACKAGE_INSULT_RETURNED_TARGET
                        : CapitalChronicleEventId.DIPLOMATIC_PACKAGE_RETURNED_TARGET,
                sourceName,
                targetName
        );
    }

    private static void notifySource(
            ServerLevel level,
            DiplomaticShipment shipment,
            CapitalRecord sourceCapital,
            Component title,
            Component message
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
            Component failureMessage
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
                Component message
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