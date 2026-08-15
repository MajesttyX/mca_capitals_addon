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
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.only_a_player_can_use_this"));
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
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.hold_the_succession_decree_bound_to_this_capital_to_transfer_the_crown"));
            return 0;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.the_bound_capital_no_longer_exists"));
            return 0;
        }

        if (!isPlayerSovereignOfCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.only_the_player_sovereign_of_this_capital_can_use_the_succession_decre"));
            return 0;
        }

        if (targetId.equals(player.getUUID()) || targetId.equals(capital.getSovereign())) {
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.you_already_hold_the_crown_of_this_capital"));
            return 0;
        }

        ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayer(targetId);
        if (targetPlayer != null) {
            if (!CapitalSuccessionDecreeService.transferToPlayer(level, capital, targetPlayer)) {
                source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.the_crown_could_not_be_transferred"));
                return 0;
            }

            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.succession_decree_commands.crown_transferred",
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                    targetPlayer.getName()
            ));
            return 1;
        }

        if (!isEligibleVillagerSuccessor(level, capital, targetId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.that_villager_is_not_eligible_to_receive_this_crown"));
            return 0;
        }

        String targetName = resolveName(level, targetId);
        if (!CapitalSuccessionDecreeService.transferToVillager(level, capital, targetId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.the_crown_could_not_be_transferred"));
            return 0;
        }

        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.succession_decree_commands.crown_transferred",
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                targetName
        ));
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
            source.sendFailure(Component.translatable("mcacapitals.system.succession_decree_commands.invalid_uuid"));
            return null;
        }
    }
}