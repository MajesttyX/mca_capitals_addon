package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalSuccessionDecreeService;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.SuccessionDecreeItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

public class SuccessionDecreeCommands {

    private SuccessionDecreeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("successiondecree")
                        .then(Commands.literal("confirm")
                                .then(Commands.argument("capitalId", StringArgumentType.string())
                                        .then(Commands.argument("targetId", StringArgumentType.string())
                                                .executes(context -> confirm(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "capitalId"),
                                                        StringArgumentType.getString(context, "targetId")
                                                )))))
        );
    }

    private static int confirm(CommandSourceStack source, String rawCapitalId, String rawTargetId) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID capitalId = parseUuid(source, rawCapitalId);
        UUID targetId = parseUuid(source, rawTargetId);
        if (capitalId == null || targetId == null) {
            return 0;
        }

        ItemStack decree = findHeldBoundDecree(player, capitalId);
        if (decree.isEmpty()) {
            source.sendFailure(Component.literal("Hold the Succession Decree bound to this capital to transfer the crown."));
            return 0;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            source.sendFailure(Component.literal("The bound capital no longer exists."));
            return 0;
        }

        if (!isPlayerSovereignOfCapital(player, capital)) {
            source.sendFailure(Component.literal("Only the player sovereign of this capital can use the Succession Decree."));
            return 0;
        }

        if (targetId.equals(player.getUUID()) || targetId.equals(capital.getSovereign())) {
            source.sendFailure(Component.literal("You already hold the crown of this capital."));
            return 0;
        }

        ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayer(targetId);
        if (targetPlayer != null) {
            if (!CapitalSuccessionDecreeService.transferToPlayer(level, capital, targetPlayer)) {
                source.sendFailure(Component.literal("The crown could not be transferred."));
                return 0;
            }

            player.sendSystemMessage(Component.literal("You transferred the crown of "
                    + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                    + " to " + targetPlayer.getName().getString() + "."));
            return 1;
        }

        if (!isEligibleVillagerSuccessor(level, capital, targetId)) {
            source.sendFailure(Component.literal("That villager is not eligible to receive this crown."));
            return 0;
        }

        String targetName = resolveName(level, targetId);
        if (!CapitalSuccessionDecreeService.transferToVillager(level, capital, targetId)) {
            source.sendFailure(Component.literal("The crown could not be transferred."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("You transferred the crown of "
                + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                + " to " + targetName + "."));
        return 1;
    }

    private static boolean isPlayerSovereignOfCapital(ServerPlayer player, CapitalRecord capital) {
        if (player == null || capital == null) {
            return false;
        }

        UUID playerId = player.getUUID();
        return capital.isPlayerSovereign()
                && (playerId.equals(capital.getPlayerSovereignId()) || playerId.equals(capital.getSovereign()));
    }

    private static boolean isEligibleVillagerSuccessor(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            return false;
        }

        if (!MCAIntegrationBridge.hasPersistentFamilyNode(level, targetId)
                || MCAIntegrationBridge.isFamilyNodeDeceased(level, targetId)) {
            return false;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        if (!residents.contains(targetId)) {
            return false;
        }

        return targetId.equals(capital.getHand())
                || capital.isRoyalChild(targetId)
                || capital.isLegitimizedRoyalChild(targetId);
    }

    private static ItemStack findHeldBoundDecree(ServerPlayer player, UUID capitalId) {
        ItemStack mainHand = player.getMainHandItem();
        if (isBoundDecreeForCapital(mainHand, capitalId)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (isBoundDecreeForCapital(offHand, capitalId)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isBoundDecreeForCapital(ItemStack stack, UUID capitalId) {
        return stack != null
                && !stack.isEmpty()
                && stack.is(ModItems.BLANK_SUCCESSION_DECREE.get())
                && capitalId.equals(SuccessionDecreeItem.getBoundCapitalId(stack));
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.getName().getString();
        }

        return entityId.toString();
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuid(CommandSourceStack source, String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid UUID."));
            return null;
        }
    }
}