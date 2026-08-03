package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.data.DiplomaticShipmentStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticGiftDispatchService {

    private CapitalDiplomaticGiftDispatchService() {
    }

    static int dispatch(ServerPlayer player, UUID ambassadorId, UUID targetCapitalId) {
        if (player == null || ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        Entity ambassadorEntity = player.serverLevel().getEntity(ambassadorId);
        CapitalDiplomaticGiftValidation.Validation validation =
                CapitalDiplomaticGiftValidation.validateAudience(
                        player,
                        ambassadorEntity,
                        true
                );
        if (!validation.valid()) {
            player.sendSystemMessage(Component.literal(validation.failureMessage()));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord sourceCapital = validation.sourceCapital();
        CapitalRecord targetCapital = CapitalManager.getCapital(targetCapitalId);
        if (targetCapital == null
                || targetCapital.getState() != CapitalState.ACTIVE
                || targetCapital.getCapitalId() == null
                || targetCapital.getCapitalId().equals(sourceCapital.getCapitalId())) {
            player.sendSystemMessage(Component.literal(
                    "That capital is no longer available to receive a diplomatic package."
            ));
            return 0;
        }

        long cooldown = CapitalDiplomacyDataAccess.getGiftCooldownRemaining(
                level,
                sourceCapital.getCapitalId(),
                targetCapital.getCapitalId()
        );
        if (cooldown > 0L) {
            player.sendSystemMessage(Component.literal(
                    "Another package may be sent to "
                            + CapitalDiplomaticGiftText.getCapitalName(level, targetCapital)
                            + " in "
                            + CapitalDiplomaticGiftText.formatDuration(cooldown)
                            + "."
            ));
            return 0;
        }

        CapitalDiplomaticGiftValidation.HeldPackage heldPackage =
                CapitalDiplomaticGiftValidation.findHeldPackage(player);
        if (heldPackage == null) {
            player.sendSystemMessage(Component.literal(
                    "Hold a filled Diplomatic Package in either hand."
            ));
            return 0;
        }

        List<ItemStack> contents = CapitalDiplomaticGiftValidation.readAndValidateContents(
                heldPackage.stack()
        );
        if (contents.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "The Diplomatic Package must contain at least one item."
            ));
            return 0;
        }

        CapitalGiftAppraisalService.GiftAppraisal appraisal =
                CapitalGiftAppraisalService.appraise(player, targetCapital, contents);
        UUID recipientSovereignId =
                CapitalDiplomaticGiftValidation.getCurrentSovereignId(targetCapital);

        DiplomaticShipment shipment = new DiplomaticShipment(
                UUID.randomUUID(),
                sourceCapital.getCapitalId(),
                targetCapital.getCapitalId(),
                player.getUUID(),
                recipientSovereignId,
                null,
                level.getGameTime(),
                CapitalDiplomaticDelayService.schedule(level),
                appraisal.relationshipDelta(),
                appraisal.description(),
                DiplomaticShipmentStatus.DISPATCHED,
                contents
        );
        CapitalDiplomacyDataAccess.addShipment(level, shipment);
        CapitalDiplomacyDataAccess.beginGiftCooldown(
                level,
                sourceCapital.getCapitalId(),
                targetCapital.getCapitalId()
        );

        player.setItemInHand(heldPackage.hand(), ItemStack.EMPTY);

        String sourceName = CapitalDiplomaticGiftText.getCapitalName(level, sourceCapital);
        String targetName = CapitalDiplomaticGiftText.getCapitalName(level, targetCapital);
        CapitalChronicleService.addEntry(
                level,
                sourceCapital,
                "A diplomatic package was dispatched from "
                        + sourceName
                        + " to "
                        + targetName
                        + "."
        );
        player.sendSystemMessage(Component.literal(
                ambassadorEntity.getName().getString()
                        + ": The package has been dispatched to "
                        + targetName
                        + ". A response may arrive within one to five minutes. I judge it to be "
                        + appraisal.description().toLowerCase()
                        + "."
        ));
        return 1;
    }
}
