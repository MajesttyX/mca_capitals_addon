package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.identity.HouseFoundationService;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class CapitalHouseFoundationCommands {

    private static final double LOOK_TARGET_REACH = 16.0D;

    private CapitalHouseFoundationCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaltest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("house_foundation")
                                .then(Commands.literal("found")
                                        .executes(ctx -> foundLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> foundUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> clearLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> clearUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                        )
        );
    }

    private static int foundLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.literal("Look at a loaded MCA villager within 16 blocks."));
            return 0;
        }

        return foundHouse(source, target);
    }

    private static int foundUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        return foundHouse(source, target);
    }

    private static int clearLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.literal("Look at a loaded MCA villager within 16 blocks."));
            return 0;
        }

        HouseFoundationService.clearHouse(target);
        source.sendSuccess(() -> Component.literal("Cleared House foundation data for " + target.getName().getString() + "."), false);
        source.sendSuccess(() -> Component.literal(VillagerIdentityService.describe(target)), false);
        return 1;
    }

    private static int clearUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        HouseFoundationService.clearHouse(target);
        source.sendSuccess(() -> Component.literal("Cleared House foundation data for " + target.getName().getString() + "."), false);
        source.sendSuccess(() -> Component.literal(VillagerIdentityService.describe(target)), false);
        return 1;
    }

    private static int foundHouse(CommandSourceStack source, Entity target) {
        HouseFoundationService.HouseFoundationResult result = HouseFoundationService.foundHouse(source.getLevel(), target);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        source.sendSuccess(() -> Component.literal(VillagerIdentityService.describe(target)), false);
        return 1;
    }

    private static Entity getLookedAtMCAVillager(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use the look-target version of this command."));
            return null;
        }

        Entity target = EntityLookTargetHelper.getLookedAtEntity(player, LOOK_TARGET_REACH);
        if (target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return null;
        }

        return target;
    }

    private static Entity getUuidTarget(CommandSourceStack source, String rawUuid) {
        UUID uuid = parseUuid(source, rawUuid);
        if (uuid == null) {
            return null;
        }

        ServerLevel level = source.getLevel();
        Entity target = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, uuid);
        if (target == null) {
            source.sendFailure(Component.literal("No loaded MCA villager found for UUID " + uuid + "."));
            return null;
        }

        return target;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuid(CommandSourceStack source, String rawUuid) {
        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid UUID: " + rawUuid));
            return null;
        }
    }
}