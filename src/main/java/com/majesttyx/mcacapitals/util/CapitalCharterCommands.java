package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerNotificationService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.RoyalCharterItem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.UUID;

public final class CapitalCharterCommands {

    private CapitalCharterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalcharter")
                        .executes(context -> requestCharter(context.getSource()))
        );
    }

    private static int requestCharter(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can request a Royal Charter."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        CapitalRecord capital = findRecoverableCapital(level, player);
        if (capital == null) {
            source.sendFailure(Component.literal(
                    "There is no unclaimed capital here requesting a Royal Charter."
            ));
            return 0;
        }

        if (hasMatchingCharterInPlayerInventory(player, capital.getCapitalId())) {
            source.sendFailure(Component.literal(
                    "You already hold the Royal Charter for "
                            + capitalName(level, capital)
                            + "."
            ));
            return 0;
        }

        ItemStack charter = RoyalCharterItem.createForCapital(level, capital);
        if (charter.isEmpty()) {
            source.sendFailure(Component.literal(
                    "Could not create a Royal Charter for this capital."
            ));
            return 0;
        }

        boolean inserted = player.addItem(charter);
        if (!inserted) {
            player.drop(charter, false);
        }

        if (!capital.isRoyalCharterIssued()) {
            capital.setRoyalCharterIssued(true);
            CapitalDataAccess.markDirty(level);
        }

        source.sendSuccess(
                () -> Component.literal(
                        "A Royal Charter for "
                                + capitalName(level, capital)
                                + " has been granted."
                ),
                false
        );
        return 1;
    }

    private static CapitalRecord findRecoverableCapital(
            ServerLevel level,
            ServerPlayer player
    ) {
        return CapitalManager.getAllCapitalRecords()
                .stream()
                .filter(capital -> isRecoverableForPlayer(level, capital, player))
                .min(Comparator.comparingDouble(
                        capital -> distanceToCapitalSqr(level, capital, player)
                ))
                .orElse(null);
    }

    private static boolean isRecoverableForPlayer(
            ServerLevel level,
            CapitalRecord capital,
            ServerPlayer player
    ) {
        if (level == null || capital == null || player == null) {
            return false;
        }
        if (capital.getVillageId() == null) {
            return false;
        }
        if (capital.getSovereign() != null || capital.isMonarchyRejected()) {
            return false;
        }
        return CapitalPlayerNotificationService.isPlayerWithinCapital(
                level,
                capital,
                player
        );
    }

    private static double distanceToCapitalSqr(
            ServerLevel level,
            CapitalRecord capital,
            ServerPlayer player
    ) {
        BlockPos center = MCAIntegrationBridge.getVillageCenter(
                level,
                capital.getVillageId()
        );
        return player.distanceToSqr(
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D
        );
    }

    private static boolean hasMatchingCharterInPlayerInventory(
            ServerPlayer player,
            UUID capitalId
    ) {
        for (ItemStack stack : player.getInventory().items) {
            if (isMatchingCharter(stack, capitalId)) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (isMatchingCharter(stack, capitalId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isMatchingCharter(
            ItemStack stack,
            UUID capitalId
    ) {
        if (stack == null
                || stack.isEmpty()
                || !stack.is(ModItems.ROYAL_CHARTER.get())
                || !ModItemStackData.hasCustomData(stack)) {
            return false;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return capitalId != null
                && capitalId.toString().equals(
                        tag.getString(ModDataKeys.CAPITAL_ID)
                );
    }

    private static String capitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null || capital.getVillageId() == null) {
            return "this capital";
        }

        String name = MCAIntegrationBridge.getVillageName(
                level,
                capital.getVillageId()
        );
        return name == null || name.isBlank()
                ? "this capital"
                : name;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            return null;
        }
    }
}
