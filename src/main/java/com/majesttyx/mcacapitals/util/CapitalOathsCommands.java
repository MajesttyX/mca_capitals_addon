package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalBuildingService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalMasterOfLawsService;
import com.majesttyx.mcacapitals.capital.CapitalNaturalDukedomService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CrownStanding;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CapitalOathsCommands {

    private static final double LOOK_TARGET_REACH = 16.0D;
    private static final long EXPIRED_WARRANT_AGE_TICKS = 20L * 130L;
    private static final long AGED_DETENTION_DAYS = 2L;

    private CapitalOathsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaloaths")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("standing")
                                .then(Commands.literal("show")
                                        .executes(ctx -> standingShow(ctx.getSource())))
                                .then(Commands.literal("list")
                                        .executes(ctx -> standingList(ctx.getSource())))
                                .then(Commands.literal("friend")
                                        .executes(ctx -> standingSet(ctx.getSource(), CrownStanding.FRIEND_OF_CROWN)))
                                .then(Commands.literal("enemy")
                                        .executes(ctx -> standingSet(ctx.getSource(), CrownStanding.ENEMY_OF_CROWN)))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> standingClear(ctx.getSource()))))
                        .then(Commands.literal("law")
                                .then(Commands.literal("master")
                                        .then(Commands.literal("show")
                                                .executes(ctx -> masterShow(ctx.getSource())))
                                        .then(Commands.literal("appoint")
                                                .executes(ctx -> masterAppoint(ctx.getSource())))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> masterClear(ctx.getSource())))
                                        .then(Commands.literal("tick")
                                                .executes(ctx -> masterTick(ctx.getSource())))))
                        .then(Commands.literal("warrant")
                                .then(Commands.literal("issue")
                                        .executes(ctx -> warrantIssue(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> warrantClear(ctx.getSource())))
                                .then(Commands.literal("expire")
                                        .executes(ctx -> warrantExpire(ctx.getSource())))
                                .then(Commands.literal("list")
                                        .executes(ctx -> warrantList(ctx.getSource()))))
                        .then(Commands.literal("prison")
                                .then(Commands.literal("detain")
                                        .executes(ctx -> prisonDetain(ctx.getSource(), false)))
                                .then(Commands.literal("release")
                                        .executes(ctx -> prisonRelease(ctx.getSource())))
                                .then(Commands.literal("age")
                                        .executes(ctx -> prisonDetain(ctx.getSource(), true)))
                                .then(Commands.literal("list")
                                        .executes(ctx -> prisonList(ctx.getSource()))))
                        .then(Commands.literal("execution")
                                .then(Commands.literal("mark")
                                        .executes(ctx -> executionMark(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> executionClear(ctx.getSource())))
                                .then(Commands.literal("check")
                                        .executes(ctx -> executionCheck(ctx.getSource()))))
                        .then(Commands.literal("exile")
                                .then(Commands.literal("discover")
                                        .executes(ctx -> exileDiscover(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> exileClear(ctx.getSource())))
                                .then(Commands.literal("list")
                                        .executes(ctx -> exileList(ctx.getSource()))))
                        .then(Commands.literal("accusation")
                                .then(Commands.literal("cooldown")
                                        .then(Commands.literal("show")
                                                .executes(ctx -> accusationCooldownShow(ctx.getSource())))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> accusationCooldownClear(ctx.getSource())))))
                        .then(Commands.literal("dukedom")
                                .then(Commands.literal("info")
                                        .executes(ctx -> dukedomInfo(ctx.getSource())))
                                .then(Commands.literal("tick")
                                        .executes(ctx -> dukedomTick(ctx.getSource())))
                                .then(Commands.literal("resetday")
                                        .executes(ctx -> dukedomResetDay(ctx.getSource())))
                                .then(Commands.literal("elevate")
                                        .executes(ctx -> dukedomElevate(ctx.getSource()))))
        );
    }

    private static int standingShow(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }

        CrownStanding standing = capital.getCrownStanding(context.targetId());
        final String text = "Standing for " + display(context.target()) + " = " + (standing == null ? "unset" : standing.name());
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int standingList(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }

        Map<UUID, CrownStanding> standings = capital.getCrownStandings();
        if (standings.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.no_crown_standings_are_currently_stored_for_this_capital"), false);
            return 1;
        }

        final UUID capitalId = capital.getCapitalId();
        source.sendSuccess(() -> Component.literal("Crown standings for capital " + capitalId + ":"), false);
        for (Map.Entry<UUID, CrownStanding> entry : standings.entrySet()) {
            UUID id = entry.getKey();
            CrownStanding standing = entry.getValue();
            final String line = name(context.level(), capital, id) + " = " + standing.name() + " (" + id + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int standingSet(CommandSourceStack source, CrownStanding standing) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }

        capital.setCrownStanding(context.targetId(), standing);
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());

        final String line = "Set " + display(context.target()) + " to " + standing.name() + ".";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int standingClear(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }

        capital.getCrownStandings().remove(context.targetId());
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());

        final String line = "Cleared Crown standing for " + display(context.target()) + ".";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int masterShow(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }

        UUID master = capital.getMasterOfLaws();
        final String line = master == null
                ? "This capital has no Master of Laws."
                : "Master of Laws: " + name(context.level(), capital, master) + " (" + master + ")";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int masterAppoint(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }

        UUID previous = capital.getMasterOfLaws();
        capital.setMasterOfLaws(context.targetId());
        capital.setMasterOfLawsFemale(MCAIntegrationBridge.isFemale(context.level(), context.targetId()));
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());
        sync(context.level(), previous);
        sync(context.level(), context.targetId());

        final String line = "Appointed " + display(context.target()) + " as Master of Laws.";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int masterClear(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }

        UUID previous = capital.getMasterOfLaws();
        capital.setMasterOfLaws(null);
        capital.setMasterOfLawsFemale(false);
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());
        sync(context.level(), previous);

        source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.cleared_master_of_laws"), false);
        return 1;
    }

    private static int masterTick(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }

        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(context.level(), capital.getCapitalId());
        boolean changed = CapitalMasterOfLawsService.tickMasterOfLaws(context.level(), capital, residents);
        final String line = "Master of Laws tick completed. changed=" + changed + " current=" + capital.getMasterOfLaws();
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int warrantIssue(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        CapitalJusticeDataAccess.issueArrestWarrant(context.level(), capital.getCapitalId(), context.targetId());
        final String line = "Issued Arrest Warrant for " + display(context.target()) + ".";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int warrantClear(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        boolean changed = CapitalJusticeDataAccess.clearArrestWarrant(context.level(), capital.getCapitalId(), context.targetId());
        final String line = "Cleared Arrest Warrant for " + display(context.target()) + ". changed=" + changed;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int warrantExpire(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        CapitalJusticeDataAccess.get(context.level()).clearArrestWarrant(capital.getCapitalId(), context.targetId());
        CapitalJusticeDataAccess.get(context.level()).issueArrestWarrant(capital.getCapitalId(), context.targetId(), Math.max(0L, context.level().getGameTime() - EXPIRED_WARRANT_AGE_TICKS));
        final String line = "Expired Arrest Warrant timer for " + display(context.target()) + ". Wait for the next justice tick.";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int warrantList(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        Set<UUID> warrants = CapitalJusticeDataAccess.getArrestWarrants(context.level(), capital.getCapitalId());
        if (warrants.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.no_active_arrest_warrants"), false);
            return 1;
        }
        for (UUID id : warrants) {
            long issued = CapitalJusticeDataAccess.getArrestWarrantIssuedGameTime(context.level(), capital.getCapitalId(), id);
            final String line = name(context.level(), capital, id) + " warrant issuedGameTime=" + issued + " (" + id + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int prisonDetain(CommandSourceStack source, boolean aged) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        if (!CapitalJusticeDataAccess.hasArrestWarrant(context.level(), capital.getCapitalId(), context.targetId())) {
            CapitalJusticeDataAccess.issueArrestWarrant(context.level(), capital.getCapitalId(), context.targetId());
        }
        if (aged) {
            CapitalJusticeDataAccess.get(context.level()).clearDetainedPrisoner(capital.getCapitalId(), context.targetId());
        }
        long day = aged ? Math.max(1L, currentDay(context.level()) - AGED_DETENTION_DAYS) : currentDay(context.level());
        CapitalJusticeDataAccess.markDetainedPrisoner(context.level(), capital.getCapitalId(), context.targetId(), day);
        final String line = (aged ? "Aged detention timer for " : "Marked detained prisoner: ") + display(context.target()) + ". Wait for the next justice tick.";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int prisonRelease(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        boolean changed = CapitalJusticeDataAccess.clearDetainedPrisoner(context.level(), capital.getCapitalId(), context.targetId());
        final String line = "Cleared detention for " + display(context.target()) + ". changed=" + changed;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int prisonList(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        Set<UUID> detained = CapitalJusticeDataAccess.getDetainedPrisoners(context.level(), capital.getCapitalId());
        if (detained.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.no_detained_prisoners"), false);
            return 1;
        }
        for (UUID id : detained) {
            long day = CapitalJusticeDataAccess.getDetentionStartDay(context.level(), capital.getCapitalId(), id);
            final String line = name(context.level(), capital, id) + " detainedStartDay=" + day + " (" + id + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int executionMark(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        boolean changed = MCAExecutionBridge.markForExecution(context.level(), context.targetId());
        final String line = "Marked for execution: " + display(context.target()) + ". changed=" + changed;
        source.sendSuccess(() -> Component.literal(line), false);
        return changed ? 1 : 0;
    }

    private static int executionClear(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        boolean changed = MCAExecutionBridge.clearExecutionMark(context.level(), context.targetId());
        final String line = "Cleared execution mark: " + display(context.target()) + ". changed=" + changed;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int executionCheck(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        boolean marked = MCAExecutionBridge.isMarkedForExecution(context.level(), context.targetId());
        final String line = display(context.target()) + " markedForExecution=" + marked;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int exileDiscover(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        CapitalJusticeDataAccess.markDiscoveredExile(context.level(), capital.getCapitalId(), context.targetId());
        final String line = "Marked discovered exile: " + display(context.target()) + ".";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int exileClear(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        boolean changed = CapitalJusticeDataAccess.clearDiscoveredExile(context.level(), capital.getCapitalId(), context.targetId());
        final String line = "Cleared discovered exile for " + display(context.target()) + ". changed=" + changed;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int exileList(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        Set<UUID> exiles = CapitalJusticeDataAccess.getDiscoveredExiles(context.level(), capital.getCapitalId());
        if (exiles.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.no_discovered_exiles"), false);
            return 1;
        }
        for (UUID id : exiles) {
            final String line = name(context.level(), capital, id) + " discovered exile (" + id + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int accusationCooldownShow(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        long lastDay = CapitalJusticeDataAccess.getLastAccusationDay(context.level(), capital.getCapitalId(), context.player().getUUID());
        final String line = "Last accusation day=" + lastDay + " currentDay=" + currentDay(context.level());
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int accusationCooldownClear(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        CapitalJusticeDataAccess.setLastAccusationDay(context.level(), capital.getCapitalId(), context.player().getUUID(), Long.MIN_VALUE);
        source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.cleared_accusation_cooldown_for_this_player_in_this_capital"), false);
        return 1;
    }

    private static int dukedomInfo(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        int bigHouses = CapitalBuildingService.countBigHouses(context.level(), capital);
        int allowed = bigHouses / 2;
        int current = capital.getDukes().size();
        long day = capital.getLastNaturalDukedomDay();
        final String line = "Dukedom info: bigHouses=" + bigHouses + " allowedNaturalDukes=" + allowed + " currentDukes=" + current + " lastNaturalDukedomDay=" + day;
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int dukedomTick(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        Set<UUID> residents = CapitalResidentScanner.scanResidents(context.level(), capital.getCapitalId());
        boolean changed = CapitalNaturalDukedomService.tick(context.level(), capital, residents);
        final String line = "Natural dukedom tick completed. changed=" + changed + " dukes=" + capital.getDukes().size();
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int dukedomResetDay(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForPlayer(context.level(), context.player());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_current_capital_found"));
            return 0;
        }
        capital.setLastNaturalDukedomDay(Long.MIN_VALUE);
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());
        source.sendSuccess(() -> Component.translatable("mcacapitals.system.capital_oaths_commands.reset_natural_dukedom_day_for_this_capital"), false);
        return 1;
    }

    private static int dukedomElevate(CommandSourceStack source) {
        CommandContext context = requireLookedAtVillager(source);
        if (context == null) {
            return 0;
        }
        CapitalRecord capital = resolveCapitalForTargetOrPlayer(context.level(), context.player(), context.target());
        if (capital == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.no_capital_found_for_that_villager"));
            return 0;
        }
        boolean female = MCAIntegrationBridge.isFemale(context.level(), context.targetId());
        capital.addDuke(context.targetId(), female);
        com.majesttyx.mcacapitals.data.CapitalDataAccess.markDirty(context.level());
        sync(context.level(), context.targetId());
        final String line = "Elevated " + display(context.target()) + " to " + (female ? "Duchess" : "Duke") + ".";
        source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static CommandContext requirePlayer(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.only_players_can_run_this_command"));
            return null;
        }
        return new CommandContext(source.getLevel(), player, null, null);
    }

    private static CommandContext requireLookedAtVillager(CommandSourceStack source) {
        CommandContext context = requirePlayer(source);
        if (context == null) {
            return null;
        }
        Entity target = EntityLookTargetHelper.getLookedAtEntity(context.player(), LOOK_TARGET_REACH);
        if (target == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.look_at_an_mca_villager_first"));
            return null;
        }
        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_oaths_commands.target_is_not_an_mca_villager"));
            return null;
        }
        return new CommandContext(context.level(), context.player(), target, target.getUUID());
    }

    private static CapitalRecord resolveCapitalForTargetOrPlayer(ServerLevel level, ServerPlayer player, Entity target) {
        if (level != null && target != null) {
            Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, target.getUUID());
            CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId);
            if (capital != null) {
                return capital;
            }
        }
        return resolveCapitalForPlayer(level, player);
    }

    private static CapitalRecord resolveCapitalForPlayer(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return null;
        }

        Optional<Integer> villageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (villageId.isPresent()) {
            CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId.get());
            if (capital != null) {
                return capital;
            }
        }

        CapitalRecord nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null || capital.getVillageId() == null) {
                continue;
            }
            BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
            if (center == null) {
                continue;
            }
            double distance = player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
            if (distance < nearestDistance) {
                nearest = capital;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void sync(ServerLevel level, UUID id) {
        if (level == null || id == null) {
            return;
        }
        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, id);
        if (entity != null) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
        }
    }

    private static String display(Entity entity) {
        return entity == null ? "Unknown" : entity.getName().getString();
    }

    private static String name(ServerLevel level, CapitalRecord capital, UUID id) {
        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, id);
        if (entity != null) {
            return entity.getName().getString();
        }
        return String.valueOf(id);
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }

    private record CommandContext(ServerLevel level, ServerPlayer player, Entity target, UUID targetId) {
    }
}