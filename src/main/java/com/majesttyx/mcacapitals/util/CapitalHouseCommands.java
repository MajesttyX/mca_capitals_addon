package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenRoyalCharterDecisionPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CapitalHouseCommands {

    private CapitalHouseCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalhouse")
                        .then(Commands.literal("set_and_open")
                                .then(Commands.argument("capitalId", StringArgumentType.string())
                                        .then(Commands.argument("mode", StringArgumentType.string())
                                                .then(Commands.argument("houseName", StringArgumentType.greedyString())
                                                        .executes(ctx -> setAndOpen(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "capitalId"),
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                StringArgumentType.getString(ctx, "houseName")
                                                        ))))))
        );

        dispatcher.register(
                Commands.literal("capitaltest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("house")
                                .then(Commands.literal("show")
                                        .executes(ctx -> showSelf(ctx.getSource()))
                                        .then(Commands.argument("playerUuid", StringArgumentType.string())
                                                .executes(ctx -> showUuid(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "playerUuid")
                                                ))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("capitalId", StringArgumentType.string())
                                                .then(Commands.argument("mode", StringArgumentType.string())
                                                        .then(Commands.argument("houseName", StringArgumentType.greedyString())
                                                                .executes(ctx -> debugSetSelf(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "capitalId"),
                                                                        StringArgumentType.getString(ctx, "mode"),
                                                                        StringArgumentType.getString(ctx, "houseName")
                                                                ))))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> clearSelf(ctx.getSource()))
                                        .then(Commands.argument("playerUuid", StringArgumentType.string())
                                                .executes(ctx -> clearUuid(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "playerUuid")
                                                ))))
                        )
        );
    }

    private static int setAndOpen(CommandSourceStack source, String rawCapitalId, String rawMode, String rawHouseName) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can establish a House from a Royal Charter."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        UUID capitalId = parseUuid(source, rawCapitalId);
        if (capitalId == null) {
            return 0;
        }

        if (!hasMatchingHeldCharter(player, capitalId)) {
            source.sendFailure(Component.literal("You must be holding the matching Royal Charter."));
            return 0;
        }

        if (PlayerHouseService.hasHouse(level, player.getUUID())) {
            ModNetwork.sendToPlayer(player, new OpenRoyalCharterDecisionPacket());
            return 1;
        }

        PlayerHouseInheritanceMode mode = PlayerHouseService.parseMode(rawMode);
        if (mode == null) {
            source.sendFailure(Component.literal("Invalid inheritance mode."));
            return 0;
        }

        String houseName = PlayerHouseService.normalizeHouseName(rawHouseName);
        if (!PlayerHouseService.isValidHouseName(houseName)) {
            source.sendFailure(Component.literal("Invalid House name. Use 2-20 letters, spaces, hyphens, or apostrophes."));
            return 0;
        }

        if (!PlayerHouseService.setHouse(level, player, capitalId, houseName, mode)) {
            source.sendFailure(Component.literal("Could not establish player House."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Established House " + houseName + "."), false);
        ModNetwork.sendToPlayer(player, new OpenRoyalCharterDecisionPacket());
        return 1;
    }

    private static int showSelf(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this command without a UUID."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(PlayerHouseService.describe(source.getLevel(), player.getUUID())), false);
        return 1;
    }

    private static int showUuid(CommandSourceStack source, String rawPlayerUuid) {
        UUID playerId = parseUuid(source, rawPlayerUuid);
        if (playerId == null) {
            return 0;
        }

        source.sendSuccess(() -> Component.literal(PlayerHouseService.describe(source.getLevel(), playerId)), false);
        return 1;
    }

    private static int debugSetSelf(CommandSourceStack source, String rawCapitalId, String rawMode, String rawHouseName) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this command without a UUID."));
            return 0;
        }

        UUID capitalId = parseUuid(source, rawCapitalId);
        if (capitalId == null) {
            return 0;
        }

        PlayerHouseInheritanceMode mode = PlayerHouseService.parseMode(rawMode);
        if (mode == null) {
            source.sendFailure(Component.literal("Invalid inheritance mode."));
            return 0;
        }

        String houseName = PlayerHouseService.normalizeHouseName(rawHouseName);
        if (!PlayerHouseService.isValidHouseName(houseName)) {
            source.sendFailure(Component.literal("Invalid House name. Use 2-20 letters, spaces, hyphens, or apostrophes."));
            return 0;
        }

        if (!PlayerHouseService.setHouse(source.getLevel(), player, capitalId, houseName, mode)) {
            source.sendFailure(Component.literal("Could not set player House."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + " to House " + houseName + "."), false);
        source.sendSuccess(() -> Component.literal(PlayerHouseService.describe(source.getLevel(), player.getUUID())), false);
        return 1;
    }

    private static int clearSelf(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this command without a UUID."));
            return 0;
        }

        PlayerHouseService.clear(source.getLevel(), player.getUUID());
        source.sendSuccess(() -> Component.literal("Cleared player House for " + player.getName().getString() + "."), false);
        return 1;
    }

    private static int clearUuid(CommandSourceStack source, String rawPlayerUuid) {
        UUID playerId = parseUuid(source, rawPlayerUuid);
        if (playerId == null) {
            return 0;
        }

        PlayerHouseService.clear(source.getLevel(), playerId);
        source.sendSuccess(() -> Component.literal("Cleared player House for " + playerId + "."), false);
        return 1;
    }

    private static boolean hasMatchingHeldCharter(ServerPlayer player, UUID capitalId) {
        return isMatchingCharter(player.getMainHandItem(), capitalId) || isMatchingCharter(player.getOffhandItem(), capitalId);
    }

    private static boolean isMatchingCharter(ItemStack stack, UUID capitalId) {
        if (stack == null || stack.isEmpty() || !stack.is(ModItems.ROYAL_CHARTER.get()) || !ModItemStackData.hasCustomData(stack)) {
            return false;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        UUID stackCapitalId = parseUuidSilently(tag.getString(ModDataKeys.CAPITAL_ID));
        return capitalId.equals(stackCapitalId);
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuid(CommandSourceStack source, String value) {
        UUID uuid = parseUuidSilently(value);
        if (uuid == null) {
            source.sendFailure(Component.literal("Invalid UUID: " + value));
        }
        return uuid;
    }

    private static UUID parseUuidSilently(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}