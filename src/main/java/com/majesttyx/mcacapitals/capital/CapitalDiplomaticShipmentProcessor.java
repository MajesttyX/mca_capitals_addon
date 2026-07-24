package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacySavedData;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.data.DiplomaticShipmentStatus;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;
import java.util.UUID;

public final class CapitalDiplomaticShipmentProcessor {

    @SubscribeEvent
    public void onLevelTick(
            LevelTickEvent.Post event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        if (level != level.getServer().overworld()) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
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
            if (shipment == null) {
                continue;
            }

            if (shipment.getStatus()
                    == DiplomaticShipmentStatus
                    .DISPATCHED) {
                CapitalDiplomaticResolutionService
                        .resolveNpcShipment(
                                level,
                                shipment
                        );

                continue;
            }

            if (!shipment
                    .isAwaitingPlayerResponse()) {
                continue;
            }

            CapitalRecord sourceCapital =
                    CapitalManager.getCapital(
                            shipment
                                    .getSourceCapitalId()
                    );

            CapitalRecord targetCapital =
                    CapitalManager.getCapital(
                            shipment
                                    .getTargetCapitalId()
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

            UUID playerSovereignId =
                    targetCapital
                            .getPlayerSovereignId();

            if (playerSovereignId == null) {
                if (targetCapital.getSovereign()
                        != null) {
                    shipment.setStatus(
                            DiplomaticShipmentStatus
                                    .DISPATCHED
                    );

                    shipment.setNotifiedPlayerId(
                            null
                    );

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
                    playerSovereignId
            )) {
                continue;
            }

            if (sourceCapital == null) {
                continue;
            }

            CapitalDiplomaticCorrespondenceService
                    .sendArrivalLetter(
                            level,
                            playerSovereignId,
                            shipment,
                            sourceCapital,
                            targetCapital
                    );

            shipment.setNotifiedPlayerId(
                    playerSovereignId
            );

            data.setDirty();
        }
    }
}