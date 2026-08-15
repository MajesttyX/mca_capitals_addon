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

    static int dispatch(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null
                || ambassadorId == null
                || targetCapitalId == null) {
            return 0;
        }

        Entity ambassadorEntity =
                player.serverLevel()
                        .getEntity(ambassadorId);

        CapitalDiplomaticGiftValidation
                .Validation validation =
                CapitalDiplomaticGiftValidation
                        .validateAudience(
                                player,
                                ambassadorEntity,
                                true
                        );

        if (!validation.valid()) {
            player.sendSystemMessage(
                    validation.failureMessage()
            );

            return 0;
        }

        ServerLevel level = player.serverLevel();

        CapitalRecord sourceCapital =
                validation.sourceCapital();

        CapitalRecord targetCapital =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        if (targetCapital == null
                || targetCapital.getState()
                != CapitalState.ACTIVE
                || targetCapital.getCapitalId() == null
                || targetCapital.getCapitalId()
                .equals(
                        sourceCapital.getCapitalId()
                )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_diplomatic_gift_dispatch_service.that_capital_is_no_longer_available_to_receive_a_diplomatic_package")
            );

            return 0;
        }

        long cooldown =
                CapitalDiplomacyDataAccess
                        .getGiftCooldownRemaining(
                                level,
                                sourceCapital.getCapitalId(),
                                targetCapital.getCapitalId()
                        );

        if (cooldown > 0L) {
            player.sendSystemMessage(
                    Component.translatable(
                            "mcacapitals.diplomacy.gift.cooldown_message",
                            CapitalDiplomaticGiftText.getCapitalNameComponent(
                                    level,
                                    targetCapital
                            ),
                            CapitalDiplomaticGiftText.formatDuration(cooldown)
                    )
            );

            return 0;
        }

        CapitalDiplomaticGiftValidation
                .HeldPackage heldPackage =
                CapitalDiplomaticGiftValidation
                        .findHeldPackage(player);

        if (heldPackage == null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_diplomatic_gift_dispatch_service.hold_a_filled_diplomatic_package_in_either_hand")
            );

            return 0;
        }

        List<ItemStack> contents =
                CapitalDiplomaticGiftValidation
                        .readAndValidateContents(
                                heldPackage.stack()
                        );

        if (contents.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_diplomatic_gift_dispatch_service.the_diplomatic_package_must_contain_at_least_one_item")
            );

            return 0;
        }

        CapitalGiftAppraisalService
                .GiftAppraisal appraisal =
                CapitalGiftAppraisalService.appraise(
                        player,
                        targetCapital,
                        contents
                );

        UUID recipientSovereignId =
                CapitalDiplomaticGiftValidation
                        .getCurrentSovereignId(
                                targetCapital
                        );

        DiplomaticShipment shipment =
                new DiplomaticShipment(
                        UUID.randomUUID(),
                        sourceCapital.getCapitalId(),
                        targetCapital.getCapitalId(),
                        player.getUUID(),
                        recipientSovereignId,
                        null,
                        level.getGameTime(),
                        CapitalDiplomaticDelayService.schedule(level),
                        appraisal.relationshipDelta(),
                        appraisal.appraisalId().serializedName(),
                        DiplomaticShipmentStatus.DISPATCHED,
                        contents
                );

        CapitalDiplomacyDataAccess.addShipment(
                level,
                shipment
        );

        CapitalDiplomacyDataAccess
                .beginGiftCooldown(
                        level,
                        sourceCapital.getCapitalId(),
                        targetCapital.getCapitalId()
                );

        player.setItemInHand(
                heldPackage.hand(),
                ItemStack.EMPTY
        );

        String sourceName =
                CapitalDiplomaticGiftText
                        .getCapitalName(
                                level,
                                sourceCapital
                        );

        String targetName =
                CapitalDiplomaticGiftText
                        .getCapitalName(
                                level,
                                targetCapital
                        );

        CapitalChronicleService.addEvent(level, sourceCapital, CapitalChronicleEventId.DIPLOMATIC_PACKAGE_DISPATCHED, sourceName, targetName);

        player.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.gift.dispatch_message",
                        ambassadorEntity.getName(),
                        CapitalDiplomaticGiftText.getCapitalNameComponent(level, targetCapital),
                        CapitalGiftAppraisalService.appraisalLowerComponent(
                                appraisal.appraisalId()
                        )
                )
        );

        return 1;
    }
}