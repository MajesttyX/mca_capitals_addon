package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCommanderService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalNameService;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public class RoyalScepterCommands {

    private RoyalScepterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("royalscepter")
                        .then(Commands.literal("heir")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> appointHeir(ctx.getSource(), StringArgumentType.getString(ctx, "villagerId")))))
                        .then(Commands.literal("commander")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> appointCommander(ctx.getSource(), StringArgumentType.getString(ctx, "villagerId")))))
                        .then(Commands.literal("guard")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> appointRoyalGuard(ctx.getSource(), StringArgumentType.getString(ctx, "villagerId")))))
                        .then(Commands.literal("duke")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> appointDuke(ctx.getSource(), StringArgumentType.getString(ctx, "villagerId")))))
        );
    }

    private static int appointHeir(CommandSourceStack source, String rawVillagerId) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);
        if (villagerId == null) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.literal("Target is not an MCA villager."));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null) {
            source.sendFailure(Component.literal("That villager is not part of a capital."));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.literal("Only the sovereign or an operator may use the Royal Scepter here."));
            return 0;
        }

        if (capital.getSovereign() == null) {
            source.sendFailure(Component.literal("That capital does not have a sovereign yet."));
            return 0;
        }

        if (villagerId.equals(capital.getSovereign())) {
            source.sendFailure(Component.literal("The sovereign cannot be their own heir."));
            return 0;
        }

        capital.setHeir(villagerId);
        capital.setHeirMode(CapitalRecord.HeirMode.MANUAL);
        capital.setHeirFemale(MCAIntegrationBridge.isFemale(level, villagerId));

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was named heir to " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        source.sendSuccess(() -> Component.literal(name + " has been named Heir Apparent."), false);
        return 1;
    }

    private static int appointCommander(CommandSourceStack source, String rawVillagerId) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);
        if (villagerId == null) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAGuard(level, villagerId)) {
            source.sendFailure(Component.literal("Only a guard or archer can be named Commander of the Army."));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null) {
            source.sendFailure(Component.literal("That villager is not part of a capital."));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.literal("Only the sovereign or an operator may use the Royal Scepter here."));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.literal("That villager is not a resident of the capital."));
            return 0;
        }

        capital.setCommander(villagerId);
        capital.setCommanderFemale(MCAIntegrationBridge.isFemale(level, villagerId));

        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was appointed Commander of the Army of " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        source.sendSuccess(() -> Component.literal(name + " has been named Commander of the Army."), false);
        return 1;
    }

    private static int appointRoyalGuard(CommandSourceStack source, String rawVillagerId) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);
        if (villagerId == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null) {
            source.sendFailure(Component.literal("That villager is not part of a capital."));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.literal("Only the sovereign or an operator may use the Royal Scepter here."));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.literal("That villager is not a resident of the capital."));
            return 0;
        }

        if (!CapitalRoyalGuardService.appointRoyalGuard(level, capital, villagerId)) {
            source.sendFailure(Component.literal("That villager is not eligible to join the royal guard."));
            return 0;
        }

        CapitalDataAccess.markDirty(level);

        String guardName = CapitalRoyalGuardService.buildRoyalGuardDisplayName(level, capital, villagerId);
        source.sendSuccess(() -> Component.literal(guardName + " has joined the royal guard."), false);
        return 1;
    }

    private static int appointDuke(CommandSourceStack source, String rawVillagerId) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use this."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);
        if (villagerId == null) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.literal("Target is not an MCA villager."));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null) {
            source.sendFailure(Component.literal("That villager is not part of a capital."));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.literal("Only the sovereign or an operator may use the Royal Scepter here."));
            return 0;
        }

        CapitalFoundationService.assignDuke(level, capital, villagerId);
        source.sendSuccess(() -> Component.literal(resolveName(level, villagerId) + " has been raised to ducal rank."), false);
        return 1;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static UUID parseUuid(CommandSourceStack source, String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid villager UUID."));
            return null;
        }
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byRole = com.example.mcacapitals.capital.CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byRole != null) {
            return byRole;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        if (villageId != null) {
            return CapitalManager.getCapitalByVillageId(villageId);
        }

        return null;
    }

    private static boolean canManageCapital(ServerPlayer player, CapitalRecord capital) {
        return player.hasPermissions(2) || player.getUUID().equals(capital.getSovereign());
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}