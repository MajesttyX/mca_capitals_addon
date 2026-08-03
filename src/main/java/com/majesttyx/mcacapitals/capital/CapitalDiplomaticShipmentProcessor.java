package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacySavedData;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.data.DiplomaticShipmentStatus;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public final class CapitalDiplomaticShipmentProcessor {

    private CapitalDiplomaticShipmentProcessor() {
    }

    public static void onLevelTick(
            ServerLevel level
    ) {
        if (level == null
                || level != level.getServer().overworld()
                || level.getGameTime() % 20L != 0L) {
            return;
        }

        CapitalDiplomacyDataAccess
                .cleanExpiredGiftCooldowns(level);

        CapitalDiplomacySavedData data =
                CapitalDiplomacyDataAccess.get(level);

        Map<UUID, DiplomaticShipment> shipments =
                data.getShipmentsSnapshot();

        for (DiplomaticShipment shipment :
                shipments.values()) {
            if (shipment == null
                    || !shipment.isReady(
                    level.getGameTime()
            )) {
                continue;
            }

            if (shipment.getStatus()
                    == DiplomaticShipmentStatus
                    .ACCEPTED_RESPONSE_IN_TRANSIT) {
                CapitalDiplomaticResolutionService
                        .completeAcceptedResponse(
                                level,
                                shipment
                        );

                continue;
            }

            if (shipment.getStatus()
                    == DiplomaticShipmentStatus
                    .RETURNED_IN_TRANSIT) {
                CapitalDiplomaticResolutionService
                        .completeReturnedResponse(
                                level,
                                shipment
                        );

                continue;
            }

            if (shipment.getStatus()
                    == DiplomaticShipmentStatus.DISPATCHED) {
                CapitalDiplomaticResolutionService
                        .resolveNpcShipment(
                                level,
                                shipment
                        );
            }

            if (!shipment.isAwaitingPlayerResponse()) {
                continue;
            }

            CapitalRecord sourceCapital =
                    CapitalManager.getCapital(
                            shipment.getSourceCapitalId()
                    );

            CapitalRecord targetCapital =
                    CapitalManager.getCapital(
                            shipment.getTargetCapitalId()
                    );

            if (targetCapital == null) {
                CapitalDiplomaticResolutionService
                        .returnUndeliverable(
                                level,
                                shipment,
                                sourceCapital
                        );

                continue;
            }

            UUID playerDecisionMaker =
                    CapitalDiplomaticAuthorityService
                            .getPlayerDecisionMaker(
                                    level,
                                    targetCapital
                            );

            if (playerDecisionMaker == null) {
                if (targetCapital.getSovereign() != null) {
                    shipment.setStatus(
                            DiplomaticShipmentStatus.DISPATCHED
                    );

                    shipment.setNotifiedPlayerId(null);
                    data.setDirty();

                    CapitalDiplomaticResolutionService
                            .resolveNpcShipment(
                                    level,
                                    shipment
                            );
                }

                continue;
            }

            if (shipment.wasNotifiedTo(
                    playerDecisionMaker
            ) || sourceCapital == null) {
                continue;
            }

            CapitalDiplomaticCorrespondenceService
                    .sendArrivalLetter(
                            level,
                            playerDecisionMaker,
                            shipment,
                            sourceCapital,
                            targetCapital
                    );

            shipment.setNotifiedPlayerId(
                    playerDecisionMaker
            );

            data.setDirty();
        }
    }
}
