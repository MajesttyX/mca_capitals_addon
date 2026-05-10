package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalHandService;
import com.example.mcacapitals.capital.CapitalHeraldService;
import com.example.mcacapitals.capital.CapitalMaesterService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalNameService;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public final class RoyalScepterCommands {

    private RoyalScepterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("royalscepter")
                        .then(Commands.literal("hand")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(context -> appointHand(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("grandmaester")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(context -> appointGrandMaester(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("commander")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(context -> appointCommander(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("royalguard")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(context -> appointRoyalGuard(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("duke")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(context -> appointDuke(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
        );
    }

    private static boolean canManageCapital(ServerPlayer player, CapitalRecord capital) {
        if (player == null || capital == null) {
            return false;
        }
        return player.hasPermissions(2)
                || player.getUUID().equals(capital.getPlayerSovereignId())
                || player.getUUID().equals(capital.getSovereign());
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }
        if (level.getServer() != null) {
            ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(entityId);
            if (onlinePlayer != null) {
                return onlinePlayer.getName().getString();
            }
        }
        if (MCAIntegrationBridge.getEntityByUuid(level, entityId) != null) {
            return MCAIntegrationBridge.getEntityByUuid(level, entityId).getName().getString();
        }
        return entityId.toString();
    }

    private static boolean isPlayerFemaleById(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null || level.getServer() == null) {
            return false;
        }
        ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(playerId);
        if (onlinePlayer == null) {
            return false;
        }
        return MCAIntegrationBridge.isPlayerFemale(level, onlinePlayer);
    }

    private static String resolvePlayerCommanderName(ServerLevel level, CapitalRecord capital, UUID commanderId) {
        String base = resolveName(level, commanderId);
        NobleTitle title = PlayerCapitalTitleService.getGrantedTitle(level, capital, commanderId);
        if (title == NobleTitle.DUKE || title == NobleTitle.DUCHESS) {
            return (isPlayerFemaleById(level, commanderId) ? "Duchess " : "Duke ") + base;
        }
        if (title == NobleTitle.LORD || title == NobleTitle.LADY) {
            return (isPlayerFemaleById(level, commanderId) ? "Lady " : "Lord ") + base;
        }
        return base;
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

    private static int appointHand(CommandSourceStack source, String rawVillagerId) {
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

        if (!CapitalHandService.isEligibleHandCandidate(level, capital, villagerId, residents)) {
            source.sendFailure(Component.literal("That villager is not eligible to serve as Hand of the Crown."));
            return 0;
        }

        if (villagerId.equals(capital.getHand())) {
            source.sendFailure(Component.literal(resolveName(level, villagerId) + " already holds the office of Hand of the Crown."));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        UUID previousHand = capital.getHand();

        if (previousHand != null && !previousHand.equals(villagerId)) {
            String formerName = resolveName(level, previousHand);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    formerName + " was relieved of the office of "
                            + (capital.isSovereignFemale() ? "Hand of the Queen" : "Hand of the King")
                            + " of " + villageName + "."
            );
        }

        capital.setHand(villagerId);
        capital.setHandFemale(MCAIntegrationBridge.isFemale(level, villagerId));

        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        String officeName = capital.isSovereignFemale() ? "Hand of the Queen" : "Hand of the King";
        CapitalChronicleService.addEntry(level, capital,
                name + " was appointed " + officeName + " of " + villageName + ".");

        return 1;
    }

    private static int appointGrandMaester(CommandSourceStack source, String rawVillagerId) {
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

        if (!CapitalMaesterService.isEligibleGrandMaesterCandidate(level, capital, villagerId, residents)) {
            source.sendFailure(Component.literal("That villager is not eligible to serve as Grand Maester."));
            return 0;
        }

        if (villagerId.equals(capital.getGrandMaester())) {
            source.sendFailure(Component.literal(resolveName(level, villagerId) + " already holds the office of Grand Maester."));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        UUID previousGrandMaester = capital.getGrandMaester();

        if (previousGrandMaester != null && !previousGrandMaester.equals(villagerId)) {
            String formerName = resolveName(level, previousGrandMaester);
            CapitalChronicleService.addEntry(level, capital,
                    formerName + " was relieved of the office of Grand Maester of " + villageName + ".");
        }

        capital.setGrandMaester(villagerId);
        capital.setGrandMaesterFemale(MCAIntegrationBridge.isFemale(level, villagerId));

        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was appointed Grand Maester of " + villageName + ".");

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

        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was appointed Commander of the Army of " + villageName + ".");

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

        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = resolveName(level, villagerId);
        CapitalChronicleService.addEntry(level, capital,
                name + " was elevated to the ducal rank in " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        return 1;
    }
}