package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorService;
import com.majesttyx.mcacapitals.capital.CapitalBuildingService;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCommanderService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalHandService;
import com.majesttyx.mcacapitals.capital.CapitalHeraldService;
import com.majesttyx.mcacapitals.capital.CapitalMaesterService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalMasterOfLawsService;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRankConflictService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalRoyalGuardService;
import com.majesttyx.mcacapitals.capital.CapitalRoyalHouseholdService;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
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
                        .then(Commands.literal("heir")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointHeir(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("hand")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointHand(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("grandmaester")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointGrandMaester(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("commander")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointCommander(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("royalguard")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointRoyalGuard(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("duke")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointDuke(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("masteroflaws")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointMasterOfLaws(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "villagerId")
                                        ))))
                        .then(Commands.literal("ambassador")
                                .then(Commands.argument("villagerId", StringArgumentType.string())
                                        .executes(context -> appointAmbassador(
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
                || player.getUUID().equals(capital.getSovereign())
                || PlayerCapitalTitleService.isHand(
                player.serverLevel(),
                capital,
                player.getUUID()
        );
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }

        if (level.getServer() != null) {
            ServerPlayer onlinePlayer = level.getServer()
                    .getPlayerList()
                    .getPlayer(entityId);

            if (onlinePlayer != null) {
                return onlinePlayer.getName().getString();
            }
        }

        if (MCAIntegrationBridge.getEntityByUuid(level, entityId) != null) {
            return MCAIntegrationBridge.getEntityByUuid(level, entityId)
                    .getName()
                    .getString();
        }

        return entityId.toString();
    }

    private static boolean isPlayerFemaleById(
            ServerLevel level,
            UUID playerId
    ) {
        if (level == null
                || playerId == null
                || level.getServer() == null) {
            return false;
        }

        ServerPlayer onlinePlayer = level.getServer()
                .getPlayerList()
                .getPlayer(playerId);

        if (onlinePlayer == null) {
            return false;
        }

        return MCAIntegrationBridge.isPlayerFemale(level, onlinePlayer);
    }

    private static String resolvePlayerCommanderName(
            ServerLevel level,
            CapitalRecord capital,
            UUID commanderId
    ) {
        String base = resolveName(level, commanderId);

        NobleTitle title = PlayerCapitalTitleService.getGrantedTitle(
                level,
                capital,
                commanderId
        );

        if (title == NobleTitle.DUKE
                || title == NobleTitle.DUCHESS) {
            return (isPlayerFemaleById(level, commanderId)
                    ? "Duchess "
                    : "Duke ") + base;
        }

        if (title == NobleTitle.LORD
                || title == NobleTitle.LADY) {
            return (isPlayerFemaleById(level, commanderId)
                    ? "Lady "
                    : "Lord ") + base;
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

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        try {
            return UUID.fromString(rawVillagerId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.invalid_uuid"));
            return null;
        }
    }

    private static CapitalRecord resolveCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord capital =
                CapitalManager.getCapitalBySovereign(villagerId);

        if (capital != null) {
            return capital;
        }

        Integer villageId =
                MCAIntegrationBridge.getVillageIdForResident(
                        level,
                        villagerId
                );

        if (villageId == null) {
            return null;
        }

        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static boolean rejectAmbassador(
            CommandSourceStack source,
            ServerLevel level,
            UUID villagerId
    ) {
        if (!CapitalAmbassadorService.isAmbassador(level, villagerId)) {
            return false;
        }

        source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.the_ambassador_cannot_hold_another_royal_or_court_appointment"));

        return true;
    }

    private static int appointHeir(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (villagerId.equals(capital.getSovereign())) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.the_sovereign_is_already_on_the_throne"));
            return 0;
        }

        if (capital.isDisinheritedRoyalChild(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.a_disinherited_royal_child_cannot_be_named_heir_apparent"));
            return 0;
        }

        if (!MCAIntegrationBridge.hasPersistentFamilyNode(
                level,
                villagerId
        ) || MCAIntegrationBridge.isFamilyNodeDeceased(
                level,
                villagerId
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_be_named_heir_apparent"));
            return 0;
        }

        if (villagerId.equals(capital.getHeir())
                && capital.getHeirMode()
                == CapitalRecord.HeirMode.MANUAL) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_heir_apparent",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        capital.setHeir(villagerId);
        capital.setHeirFemale(
                MCAIntegrationBridge.isFemale(level, villagerId)
        );
        capital.setHeirMode(CapitalRecord.HeirMode.MANUAL);

        CapitalRoyalHouseholdService.refreshDynasticHousehold(capital);

        CapitalHeraldService.refreshHeraldAfterStatusChange(
                level,
                capital,
                residents
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.HEIR_APPARENT_NAMED,
                name,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                CapitalChronicleIdentitySnapshot.title(level, capital, villagerId),
                CapitalChronicleIdentitySnapshot.style(level, capital, villagerId)
        );

        return 1;
    }

    private static int appointHand(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalHandService.isEligibleHandCandidate(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_serve_as_hand_of_the_crown"));
            return 0;
        }

        if (villagerId.equals(capital.getHand())) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_hand",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(
                level,
                capital.getVillageId()
        );

        UUID previousHand = capital.getHand();

        if (previousHand != null
                && !previousHand.equals(villagerId)) {
            String formerName = CapitalChronicleIdentitySnapshot.name(level, capital, previousHand);

            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.OFFICE_RELIEVED, formerName, CapitalChronicleIdentitySnapshot.handOffice(level, capital), villageName);
        }

        capital.setHand(villagerId);
        capital.setHandFemale(
                MCAIntegrationBridge.isFemale(level, villagerId)
        );

        CapitalHeraldService.refreshHeraldAfterStatusChange(
                level,
                capital,
                residents
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

        String officeName = capital.isSovereignFemale()
                ? "Hand of the Queen"
                : "Hand of the King";

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.HAND_APPOINTED, name, CapitalChronicleIdentitySnapshot.handOffice(level, capital), villageName);

        return 1;
    }

    private static int appointGrandMaester(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalMaesterService.isEligibleGrandMaesterCandidate(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_serve_as_grand_maester"));
            return 0;
        }

        if (villagerId.equals(capital.getGrandMaester())) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_grand_maester",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(
                level,
                capital.getVillageId()
        );

        UUID previousGrandMaester = capital.getGrandMaester();

        if (previousGrandMaester != null
                && !previousGrandMaester.equals(villagerId)) {
            String formerName = CapitalChronicleIdentitySnapshot.name(
                    level,
                    capital,
                    previousGrandMaester
            );

            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.GRAND_MAESTER_RELIEVED, formerName, villageName);
        }

        capital.setGrandMaester(villagerId);
        capital.setGrandMaesterFemale(
                MCAIntegrationBridge.isFemale(level, villagerId)
        );

        CapitalHeraldService.refreshHeraldAfterStatusChange(
                level,
                capital,
                residents
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.GRAND_MAESTER_APPOINTED, name, villageName);

        return 1;
    }

    private static int appointCommander(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAGuard(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_guard_or_archer_can_be_named_commander_of_the_army"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalCommanderService.isEligibleVillagerCommander(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_serve_as_commander_of_the_army"));
            return 0;
        }

        UUID previousVillagerCommander = capital.getCommander();

        UUID previousPlayerCommander =
                PlayerCapitalTitleService.getCommanderHolder(
                        level,
                        capital
                );

        if (villagerId.equals(previousVillagerCommander)) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_commander",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        String villageName = MCAIntegrationBridge.getVillageName(
                level,
                capital.getVillageId()
        );

        if (previousPlayerCommander != null) {
            String formerPlayerName = resolvePlayerCommanderName(
                    level,
                    capital,
                    previousPlayerCommander
            );

            PlayerCapitalTitleService.revokeCommander(
                    level,
                    capital,
                    previousPlayerCommander
            );

            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.COMMANDER_ARMY_RELIEVED, formerPlayerName, villageName);
        }

        if (previousVillagerCommander != null
                && !previousVillagerCommander.equals(villagerId)) {
            String formerVillagerName = CapitalChronicleIdentitySnapshot.name(
                    level,
                    capital,
                    previousVillagerCommander
            );

            CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.COMMANDER_ARMY_RELIEVED, formerVillagerName, villageName);
        }

        capital.setCommander(villagerId);
        capital.setCommanderFemale(
                MCAIntegrationBridge.isFemale(level, villagerId)
        );

        CapitalHeraldService.refreshHeraldAfterStatusChange(
                level,
                capital,
                residents
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.COMMANDER_ARMY_APPOINTED, name, villageName);

        return 1;
    }

    private static int appointRoyalGuard(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalRoyalGuardService.appointRoyalGuard(
                level,
                capital,
                villagerId
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_join_the_royal_guard"));
            return 0;
        }

        CapitalDataAccess.markDirty(level);
        return 1;
    }

    private static int appointDuke(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (rejectAmbassador(source, level, villagerId)) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_or_an_operator_may_use_the_royal_scepter_here"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalRankConflictService.canReceiveDirectNobleRank(
                level,
                capital,
                villagerId
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_already_holds_a_rank_that_cannot_be_replaced_by_a_ducal"));
            return 0;
        }

        capital.addDuke(
                villagerId,
                MCAIntegrationBridge.isFemale(level, villagerId)
        );

        CapitalHeraldService.refreshHeraldAfterStatusChange(
                level,
                capital,
                residents
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.DUCAL_ELEVATED,
                name,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                CapitalChronicleIdentitySnapshot.title(level, capital, villagerId),
                CapitalChronicleIdentitySnapshot.style(level, capital, villagerId)
        );

        return 1;
    }

    private static int appointMasterOfLaws(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_authorized_hand_or_an_operator_may_use_the_royal_sc"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalBuildingService.hasPrison(level, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.a_recognized_prison_is_required_before_appointing_a_master_of_laws"));
            return 0;
        }

        if (villagerId.equals(capital.getMasterOfLaws())) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_master_of_laws",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        if (!CapitalMasterOfLawsService.isEligibleCandidate(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_serve_as_master_of_laws"));
            return 0;
        }

        if (!CapitalMasterOfLawsService.appointMasterOfLaws(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.the_master_of_laws_appointment_could_not_be_completed"));
            return 0;
        }

        String name = resolveName(level, villagerId);

        source.sendSuccess(
                () -> Component.translatable(
                        "mcacapitals.system.royal_scepter_commands.master_of_laws_appointed",
                        name
                ),
                false
        );

        return 1;
    }

    private static int appointAmbassador(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_a_player_can_use_this"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = parseUuid(source, rawVillagerId);

        if (villagerId == null) {
            return 0;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.target_is_not_an_mca_villager"));
            return 0;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_part_of_a_capital"));
            return 0;
        }

        if (!canManageCapital(player, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.only_the_sovereign_authorized_hand_or_an_operator_may_use_the_royal_sc"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );

        if (!residents.contains(villagerId)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_a_resident_of_the_capital"));
            return 0;
        }

        if (!CapitalBuildingService.hasAmbassadorBuildings(level, capital)) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.a_recognized_inn_and_storage_are_required_before_appointing_an_ambassa"));
            return 0;
        }

        if (CapitalAmbassadorService.isAmbassador(
                level,
                capital,
                villagerId
        )) {
            source.sendFailure(Component.translatable(
                    "mcacapitals.system.royal_scepter_commands.already_ambassador",
                    resolveName(level, villagerId)
            ));
            return 0;
        }

        if (!CapitalAmbassadorService.isEligibleCandidate(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.that_villager_is_not_eligible_to_serve_as_ambassador"));
            return 0;
        }

        if (!CapitalAmbassadorService.appointAmbassador(
                level,
                capital,
                villagerId,
                residents
        )) {
            source.sendFailure(Component.translatable("mcacapitals.system.royal_scepter_commands.the_ambassador_appointment_could_not_be_completed"));
            return 0;
        }

        String name = resolveName(level, villagerId);

        source.sendSuccess(
                () -> Component.translatable(
                        "mcacapitals.system.royal_scepter_commands.ambassador_appointed",
                        name
                ),
                false
        );

        return 1;
    }
}