package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.identity.SurnameSource;
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

public class CapitalIdentityCommands {

    private static final double LOOK_TARGET_REACH = 16.0D;

    private CapitalIdentityCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaltest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("identity")
                                .then(Commands.literal("show")
                                        .executes(ctx -> showLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> showUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                                .then(Commands.literal("ensure")
                                        .executes(ctx -> ensureLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> ensureUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                                .then(Commands.literal("assign_origin")
                                        .executes(ctx -> assignOriginLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> assignOriginUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                                .then(Commands.literal("assign_surname")
                                        .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                .executes(ctx -> assignSurnameLookTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "surname")
                                                ))))
                                .then(Commands.literal("assign_surname_uuid")
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                        .executes(ctx -> assignSurnameUuidTarget(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "uuid"),
                                                                StringArgumentType.getString(ctx, "surname")
                                                        )))))
                                .then(Commands.literal("assign_birth_surname")
                                        .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                .executes(ctx -> assignBirthSurnameLookTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "surname")
                                                ))))
                                .then(Commands.literal("assign_birth_surname_uuid")
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                        .executes(ctx -> assignBirthSurnameUuidTarget(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "uuid"),
                                                                StringArgumentType.getString(ctx, "surname")
                                                        )))))
                                .then(Commands.literal("assign_current_surname")
                                        .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                .executes(ctx -> assignCurrentSurnameLookTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "surname")
                                                ))))
                                .then(Commands.literal("assign_current_surname_uuid")
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .then(Commands.argument("surname", StringArgumentType.greedyString())
                                                        .executes(ctx -> assignCurrentSurnameUuidTarget(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "uuid"),
                                                                StringArgumentType.getString(ctx, "surname")
                                                        )))))
                                .then(Commands.literal("clear_origin")
                                        .executes(ctx -> clearOriginLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> clearOriginUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                                .then(Commands.literal("clear_surname")
                                        .executes(ctx -> clearSurnameLookTarget(ctx.getSource()))
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> clearSurnameUuidTarget(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "uuid")
                                                ))))
                        )
        );
    }

    private static int showLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        sendIdentity(source, target);
        return 1;
    }

    private static int showUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        sendIdentity(source, target);
        return 1;
    }

    private static int ensureLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        boolean changed = VillagerIdentityService.ensureAssigned(source.getLevel(), target);
        source.sendSuccess(() -> Component.literal("Ensured identity for " + target.getName().getString() + ". changed=" + changed), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int ensureUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        boolean changed = VillagerIdentityService.ensureAssigned(source.getLevel(), target);
        source.sendSuccess(() -> Component.literal("Ensured identity for " + target.getName().getString() + ". changed=" + changed), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignOriginLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        boolean changed = VillagerIdentityService.assignOriginFromCurrentVillage(source.getLevel(), target);
        if (!changed) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.could_not_assign_origin_the_villager_may_not_currently_belong_to_an_mc"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Assigned origin for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignOriginUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        boolean changed = VillagerIdentityService.assignOriginFromCurrentVillage(source.getLevel(), target);
        if (!changed) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.could_not_assign_origin_the_villager_may_not_currently_belong_to_an_mc"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Assigned origin for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignSurnameLookTarget(CommandSourceStack source, String surname) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        return assignSurname(source, target, surname);
    }

    private static int assignSurnameUuidTarget(CommandSourceStack source, String rawUuid, String surname) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        return assignSurname(source, target, surname);
    }

    private static int assignBirthSurnameLookTarget(CommandSourceStack source, String surname) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        return assignBirthSurname(source, target, surname);
    }

    private static int assignBirthSurnameUuidTarget(CommandSourceStack source, String rawUuid, String surname) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        return assignBirthSurname(source, target, surname);
    }

    private static int assignCurrentSurnameLookTarget(CommandSourceStack source, String surname) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        return assignCurrentSurname(source, target, surname);
    }

    private static int assignCurrentSurnameUuidTarget(CommandSourceStack source, String rawUuid, String surname) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        return assignCurrentSurname(source, target, surname);
    }

    private static int clearOriginLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        VillagerIdentityService.clearOrigin(target);
        source.sendSuccess(() -> Component.literal("Cleared origin for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int clearOriginUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        VillagerIdentityService.clearOrigin(target);
        source.sendSuccess(() -> Component.literal("Cleared origin for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int clearSurnameLookTarget(CommandSourceStack source) {
        Entity target = getLookedAtMCAVillager(source);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.look_at_a_loaded_mca_villager_within_16_blocks"));
            return 0;
        }

        VillagerIdentityService.clearSurname(target);
        source.sendSuccess(() -> Component.literal("Cleared surname for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int clearSurnameUuidTarget(CommandSourceStack source, String rawUuid) {
        Entity target = getUuidTarget(source, rawUuid);
        if (target == null) {
            return 0;
        }

        VillagerIdentityService.clearSurname(target);
        source.sendSuccess(() -> Component.literal("Cleared surname for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignSurname(CommandSourceStack source, Entity target, String surname) {
        if (!VillagerIdentityService.isValidDebugSurname(surname)) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.invalid_surname_use_1_40_characters_no_formatting_codes"));
            return 0;
        }

        boolean changed = VillagerIdentityService.assignSurname(source.getLevel(), target, surname, SurnameSource.DEBUG);
        if (!changed) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.could_not_assign_surname"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Assigned birth and current surname for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignBirthSurname(CommandSourceStack source, Entity target, String surname) {
        if (!VillagerIdentityService.isValidDebugSurname(surname)) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.invalid_surname_use_1_40_characters_no_formatting_codes"));
            return 0;
        }

        boolean changed = VillagerIdentityService.assignBirthSurname(source.getLevel(), target, surname, SurnameSource.DEBUG);
        if (!changed) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.could_not_assign_birth_surname"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Assigned birth surname for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static int assignCurrentSurname(CommandSourceStack source, Entity target, String surname) {
        if (!VillagerIdentityService.isValidDebugSurname(surname)) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.invalid_surname_use_1_40_characters_no_formatting_codes"));
            return 0;
        }

        boolean changed = VillagerIdentityService.assignCurrentSurname(source.getLevel(), target, surname, SurnameSource.DEBUG);
        if (!changed) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.could_not_assign_current_surname"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Assigned current surname for " + target.getName().getString() + "."), false);
        sendIdentity(source, target);
        return 1;
    }

    private static void sendIdentity(CommandSourceStack source, Entity target) {
        source.sendSuccess(() -> Component.literal(VillagerIdentityService.describe(target)), false);
    }

    private static Entity getLookedAtMCAVillager(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_identity_commands.only_a_player_can_use_the_look_target_version_of_this_command"));
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