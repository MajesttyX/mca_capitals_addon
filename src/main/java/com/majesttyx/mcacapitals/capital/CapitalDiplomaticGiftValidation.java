package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.item.DiplomaticPackageItem;
import com.majesttyx.mcacapitals.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticGiftValidation {

    private static final double MAX_AMBASSADOR_DISTANCE_SQR =
            12.0D * 12.0D;

    private CapitalDiplomaticGiftValidation() {
    }

    static Validation validateAudience(
            ServerPlayer player,
            Entity ambassadorEntity,
            boolean requireFilledPackage
    ) {
        if (player == null
                || ambassadorEntity == null
                || !ambassadorEntity.isAlive()) {
            return Validation.failure(
                    "The Ambassador is unavailable."
            );
        }

        if (player.level() != ambassadorEntity.level()
                || player.distanceToSqr(ambassadorEntity)
                > MAX_AMBASSADOR_DISTANCE_SQR) {
            return Validation.failure(
                    "You must remain near the Ambassador."
            );
        }

        ServerLevel level = player.serverLevel();

        CapitalRecord sourceCapital = findAmbassadorCapital(
                level,
                ambassadorEntity.getUUID()
        );

        if (sourceCapital == null
                || sourceCapital.getState() != CapitalState.ACTIVE) {
            return Validation.failure(
                    "This villager is not the Ambassador of an active capital."
            );
        }

        if (!CapitalDiplomaticAuthorityService.maySendGift(
                level,
                sourceCapital,
                player.getUUID()
        )) {
            return Validation.failure(
                    "Only the sovereign or a Lord, Lady, Duke, or Duchess who holds the title in their own right may send diplomatic packages."
            );
        }

        if (!CapitalBuildingService.hasAmbassadorBuildings(
                level,
                sourceCapital
        )) {
            return Validation.failure(
                    "The capital requires an operational Inn and Storage building before packages can be sent."
            );
        }

        if (requireFilledPackage) {
            HeldPackage heldPackage = findHeldPackage(player);

            if (heldPackage == null
                    || readAndValidateContents(
                    heldPackage.stack()
            ).isEmpty()) {
                return Validation.failure(
                        "Hold a filled Diplomatic Package in either hand."
                );
            }
        }

        return Validation.success(sourceCapital);
    }

    static HeldPackage findHeldPackage(
            ServerPlayer player
    ) {
        if (player == null) {
            return null;
        }

        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return new HeldPackage(
                    InteractionHand.MAIN_HAND,
                    mainHand
            );
        }

        ItemStack offHand = player.getOffhandItem();

        if (offHand.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return new HeldPackage(
                    InteractionHand.OFF_HAND,
                    offHand
            );
        }

        return null;
    }

    static List<ItemStack> readAndValidateContents(
            ItemStack packageStack
    ) {
        if (packageStack == null
                || packageStack.isEmpty()
                || !packageStack.is(
                ModItems.DIPLOMATIC_PACKAGE.get()
        )) {
            return List.of();
        }

        List<ItemStack> contents = new ArrayList<>();

        for (ItemStack stored : DiplomaticPackageItem.readContents(packageStack)) {
            if (stored == null || stored.isEmpty()) {
                continue;
            }

            if (contents.size() >= DiplomaticShipment.MAX_SLOTS) {
                return List.of();
            }

            if (!DiplomaticPackageItem.mayStore(stored)) {
                return List.of();
            }

            contents.add(stored.copy());
        }

        return contents;
    }

    static UUID getCurrentSovereignId(
            CapitalRecord capital
    ) {
        if (capital == null) {
            return null;
        }

        return capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();
    }

    private static CapitalRecord findAmbassadorCapital(
            ServerLevel level,
            UUID ambassadorId
    ) {
        if (level == null || ambassadorId == null) {
            return null;
        }

        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && CapitalAmbassadorService.isAmbassador(
                    level,
                    capital,
                    ambassadorId
            )) {
                return capital;
            }
        }

        return null;
    }

    record HeldPackage(
            InteractionHand hand,
            ItemStack stack
    ) {
    }

    record Validation(
            boolean valid,
            CapitalRecord sourceCapital,
            String failureMessage
    ) {

        static Validation success(
                CapitalRecord capital
        ) {
            return new Validation(
                    true,
                    capital,
                    null
            );
        }

        static Validation failure(
                String message
        ) {
            return new Validation(
                    false,
                    null,
                    message
            );
        }
    }
}