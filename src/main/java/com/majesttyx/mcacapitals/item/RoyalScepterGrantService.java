package com.majesttyx.mcacapitals.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RoyalScepterGrantService {

    private RoyalScepterGrantService() {
    }

    public static void grantScepter(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (hasRoyalScepter(player)) {
            return;
        }

        ItemStack stack = new ItemStack(ModItems.ROYAL_SCEPTER.get());
        boolean added = player.getInventory().add(stack);

        if (!added && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static boolean hasRoyalScepter(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ROYAL_SCEPTER.get())) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.ROYAL_SCEPTER.get())) {
                return true;
            }
        }

        return false;
    }
}