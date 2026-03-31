package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalNameService;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.player.PlayerCapitalTitleRecord;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
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

        UUID previousVillagerCommander = capital.getCommander();
        UUID previousPlayerCommander = PlayerCapitalTitleService.getCommanderHolder(level, capital);

        if (villagerId.equals(previousVillagerCommander)) {
            source.sendFailure(Component.literal(resolveName(level, villagerId) + " already holds the office of Commander of the Army."));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        if (previousPlayerCommander != null) {
            String formerPlayerName = resolvePlayerCommanderName(level, capital, previousPlayerCommander);
            PlayerCapitalTitleService.revokeCommander(level, capital, previousPlayerCommander);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    formerPlayerName + " was relieved of the office of Commander of the Army of " + villageName + "."
            );
        }

        if (previousVillagerCommander != null && !previousVillagerCommander.equals(villagerId)) {
            String formerVillagerName = resolveName(level, previousVillagerCommander);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    formerVillagerName + " was relieved of the office of Commander of the Army of " + villageName + "."
            );
        }

        capital.setCommander(villagerId);
        capital.setCommanderFemale(MCAIntegrationBridge.isFemale(level, villagerId));

        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was appointed Commander of the Army of " + villageName + ".");

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

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.literal("That villager is not a resident of the capital."));
            return 0;
        }

        capital.addDuke(villagerId, MCAIntegrationBridge.isFemale(level, villagerId));

        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was elevated to the ducal rank in " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        source.sendSuccess(() -> Component.literal(name + " has been named Duke/Duchess."), false);
        return 1;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuid(CommandSourceStack source, String rawVillagerId) {
        try {
            return UUID.fromString(rawVillagerId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid UUID."));
            return null;
        }
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord capital = CapitalManager.getCapitalBySovereign(villagerId);
        if (capital != null) {
            return capital;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        if (villageId == null) {
            return null;
        }

        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static boolean canManageCapital(ServerPlayer player, CapitalRecord capital) {
        if (player == null || capital == null) {
            return false;
        }

        if (player.hasPermissions(2)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        return sovereign != null && sovereign.equals(player.getUUID());
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }

    private static String resolvePlayerCommanderName(ServerLevel level, CapitalRecord capital, UUID playerId) {
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getName().getString();
        }

        PlayerCapitalTitleRecord record = PlayerCapitalTitleService.get(level, playerId, capital.getCapitalId());
        if (record != null && record.getCachedPlayerName() != null && !record.getCachedPlayerName().isBlank()) {
            return record.getCachedPlayerName();
        }

        return playerId.toString();
    }
}