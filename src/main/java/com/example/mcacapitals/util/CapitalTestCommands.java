package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalSuccessionService;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public class CapitalTestCommands {

    private static final double LOOK_TARGET_REACH = 16.0D;

    private CapitalTestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaltest")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("show")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    if (CapitalManager.isEmpty()) {
                                        source.sendSuccess(() -> Component.literal("No capitals found."), false);
                                        return 1;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
                                        String heirMode = CapitalDebugFormatters.describeHeirMode(level, capital);

                                        source.sendSuccess(() -> Component.literal(
                                                "Capital " + capital.getCapitalId()
                                                        + " villageId=" + capital.getVillageId()
                                                        + " state=" + capital.getState()
                                                        + " sovereign=" + capital.getSovereign()
                                                        + " consort=" + capital.getConsort()
                                                        + " dowager=" + capital.getDowager()
                                                        + " heir=" + capital.getHeir()
                                                        + " heirMode=" + heirMode
                                                        + " royalChildren=" + capital.getRoyalChildren().size()
                                                        + " disinherited=" + capital.getDisinheritedRoyalChildren().size()
                                                        + " legitimized=" + capital.getLegitimizedRoyalChildren().size()
                                                        + " dukes=" + capital.getDukes().size()
                                                        + " lords=" + capital.getLords().size()
                                                        + " knights=" + capital.getKnights().size()
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Royal Succession Order: " + CapitalDebugFormatters.formatUuidCollection(capital.getRoyalSuccessionOrder())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Disinherited Royals: " + CapitalDebugFormatters.formatUuidCollection(capital.getDisinheritedRoyalChildren())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Legitimized Royals: " + CapitalDebugFormatters.formatUuidCollection(capital.getLegitimizedRoyalChildren())
                                        ), false);
                                    }

                                    return 1;
                                }))

                        .then(Commands.literal("bestfounder")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals found."));
                                        return 0;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
                                        final UUID capitalId = capital.getCapitalId();
                                        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capitalId);

                                        UUID bestFounder = MCAReputationBridge.findBestFounder(level, residents, 1000);

                                        if (bestFounder == null) {
                                            source.sendSuccess(() -> Component.literal(
                                                    "Capital " + capitalId + ": No founder qualifies yet."
                                            ), false);
                                        } else {
                                            int score = MCAReputationBridge.getCapitalHeartsScore(level, residents, bestFounder);
                                            source.sendSuccess(() -> Component.literal(
                                                    "Capital " + capitalId + ": Best founder=" + bestFounder + " hearts=" + score
                                            ), false);
                                        }
                                    }

                                    return 1;
                                }))

                        .then(Commands.literal("claimself")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    Entity entity = source.getEntity();

                                    if (!(entity instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("Only a player can run this."));
                                        return 0;
                                    }

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getAllCapitalRecords().iterator().next();

                                    Set<UUID> residents =
                                            CapitalResidentScanner.scanResidents(player.serverLevel(), capital.getCapitalId());

                                    int score = MCAReputationBridge.getCapitalHeartsScore(
                                            player.serverLevel(),
                                            residents,
                                            player.getUUID()
                                    );

                                    if (score < 2500) {
                                        source.sendFailure(Component.literal("Not enough hearts: " + score));
                                        return 0;
                                    }

                                    CapitalFoundationService.appointPlayerSovereign(
                                            player.serverLevel(),
                                            capital,
                                            player.getUUID(),
                                            false
                                    );

                                    final UUID capitalId = capital.getCapitalId();
                                    source.sendSuccess(() -> Component.literal(
                                            "You have claimed the throne of capital " + capitalId + "."
                                    ), false);

                                    return 1;
                                }))

                        .then(Commands.literal("appointlook")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    Entity entity = source.getEntity();

                                    if (!(entity instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("Only players can run this."));
                                        return 0;
                                    }

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    ServerLevel level = player.serverLevel();
                                    Entity target = EntityLookTargetHelper.getLookedAtEntity(player, LOOK_TARGET_REACH);

                                    if (target == null) {
                                        source.sendFailure(Component.literal("Not looking at an entity."));
                                        return 0;
                                    }

                                    UUID villagerId = target.getUUID();

                                    if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
                                        source.sendFailure(Component.literal("Target is not an MCA villager."));
                                        return 0;
                                    }

                                    Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
                                    if (villageId == null) {
                                        source.sendFailure(Component.literal("Target villager is not bound to an MCA village."));
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId);
                                    if (capital == null) {
                                        source.sendFailure(Component.literal("No capital exists yet for MCA village " + villageId));
                                        return 0;
                                    }

                                    boolean female = MCAIntegrationBridge.isFemale(level, villagerId);

                                    CapitalFoundationService.appointVillagerSovereign(
                                            level,
                                            capital,
                                            villagerId,
                                            female
                                    );

                                    final UUID capitalId = capital.getCapitalId();
                                    final int finalVillageId = villageId;
                                    final String title = CapitalTitleResolver.getDisplayTitle(level, capital, villagerId);

                                    source.sendSuccess(() -> Component.literal(
                                            "Installed " + villagerId
                                                    + " as sovereign of capital " + capitalId
                                                    + " villageId=" + finalVillageId
                                                    + " title=" + title
                                                    + " state=" + capital.getState()
                                                    + " dukes=" + capital.getDukes().size()
                                                    + " lords=" + capital.getLords().size()
                                                    + " knights=" + capital.getKnights().size()
                                    ), false);

                                    return 1;
                                }))

                        .then(Commands.literal("dukelook")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    Entity entity = source.getEntity();

                                    if (!(entity instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("Only players can run this."));
                                        return 0;
                                    }

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    ServerLevel level = player.serverLevel();
                                    Entity target = EntityLookTargetHelper.getLookedAtEntity(player, LOOK_TARGET_REACH);

                                    if (target == null) {
                                        source.sendFailure(Component.literal("Not looking at an entity."));
                                        return 0;
                                    }

                                    UUID villagerId = target.getUUID();

                                    if (!MCAIntegrationBridge.isMCAVillager(level, villagerId)) {
                                        source.sendFailure(Component.literal("Target is not an MCA villager."));
                                        return 0;
                                    }

                                    Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
                                    if (villageId == null) {
                                        source.sendFailure(Component.literal("Target villager is not bound to an MCA village."));
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId);
                                    if (capital == null) {
                                        source.sendFailure(Component.literal("No capital exists yet for MCA village " + villageId));
                                        return 0;
                                    }

                                    CapitalFoundationService.assignDuke(level, capital, villagerId);

                                    final UUID capitalId = capital.getCapitalId();
                                    final int finalVillageId = villageId;
                                    source.sendSuccess(() -> Component.literal(
                                            "Installed " + villagerId + " as duke/duchess of capital " + capitalId
                                                    + " villageId=" + finalVillageId
                                    ), false);

                                    return 1;
                                }))

                        .then(Commands.literal("court")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
                                        source.sendSuccess(() -> Component.literal(
                                                "----- COURT " + capital.getCapitalId() + " villageId=" + capital.getVillageId() + " -----"
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "State: " + capital.getState()
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Heir Mode: " + CapitalDebugFormatters.describeHeirMode(level, capital)
                                        ), false);

                                        UUID sovereign = capital.getSovereign();
                                        source.sendSuccess(() -> Component.literal(
                                                "Sovereign: " + sovereign + " | "
                                                        + (sovereign == null ? "none" : CapitalTitleResolver.getDisplayTitle(level, capital, sovereign))
                                                        + " | "
                                                        + CapitalDebugFormatters.describe(level, sovereign)
                                        ), false);

                                        UUID consort = capital.getConsort();
                                        source.sendSuccess(() -> Component.literal(
                                                "Consort: " + consort + " | "
                                                        + (consort == null ? "none" : CapitalTitleResolver.getDisplayTitle(level, capital, consort))
                                                        + " | "
                                                        + CapitalDebugFormatters.describe(level, consort)
                                        ), false);

                                        UUID dowager = capital.getDowager();
                                        source.sendSuccess(() -> Component.literal(
                                                "Dowager: " + dowager + " | "
                                                        + (dowager == null ? "none" : CapitalTitleResolver.getDisplayTitle(level, capital, dowager))
                                                        + " | "
                                                        + CapitalDebugFormatters.describe(level, dowager)
                                        ), false);

                                        UUID heir = capital.getHeir();
                                        source.sendSuccess(() -> Component.literal(
                                                "Heir: " + heir + " | "
                                                        + (heir == null ? "none" : CapitalTitleResolver.getDisplayTitle(level, capital, heir))
                                                        + " | "
                                                        + CapitalDebugFormatters.describe(level, heir)
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Royal Succession Order: " + CapitalDebugFormatters.formatUuidCollection(capital.getRoyalSuccessionOrder())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Disinherited Royals: " + CapitalDebugFormatters.formatUuidCollection(capital.getDisinheritedRoyalChildren())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Legitimized Royals: " + CapitalDebugFormatters.formatUuidCollection(capital.getLegitimizedRoyalChildren())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal("Royal Children:"), false);
                                        for (UUID id : capital.getRoyalChildren()) {
                                            source.sendSuccess(() -> Component.literal(
                                                    " - " + id + " | "
                                                            + CapitalTitleResolver.getDisplayTitle(level, capital, id)
                                                            + " | " + CapitalDebugFormatters.describe(level, id)
                                            ), false);
                                        }

                                        source.sendSuccess(() -> Component.literal("Dukes / Duchesses:"), false);
                                        for (UUID id : capital.getDukes()) {
                                            source.sendSuccess(() -> Component.literal(
                                                    " - " + id + " | "
                                                            + CapitalTitleResolver.getDisplayTitle(level, capital, id)
                                                            + " | " + CapitalDebugFormatters.describe(level, id)
                                            ), false);
                                        }

                                        source.sendSuccess(() -> Component.literal("Lords / Ladies:"), false);
                                        for (UUID id : capital.getLords()) {
                                            source.sendSuccess(() -> Component.literal(
                                                    " - " + id + " | "
                                                            + CapitalTitleResolver.getDisplayTitle(level, capital, id)
                                                            + " | " + CapitalDebugFormatters.describe(level, id)
                                            ), false);
                                        }

                                        source.sendSuccess(() -> Component.literal("Knights / Dames:"), false);
                                        for (UUID id : capital.getKnights()) {
                                            source.sendSuccess(() -> Component.literal(
                                                    " - " + id + " | "
                                                            + CapitalTitleResolver.getDisplayTitle(level, capital, id)
                                                            + " | " + CapitalDebugFormatters.describe(level, id)
                                            ), false);
                                        }
                                    }

                                    return 1;
                                }))

                        .then(Commands.literal("rebuildcourt")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
                                        CapitalFoundationService.refreshCourt(level, capital);
                                    }

                                    source.sendSuccess(() -> Component.literal(
                                            "Rebuilt all courts."
                                    ), false);

                                    return 1;
                                }))

                        .then(Commands.literal("forcesuccession")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    if (CapitalManager.isEmpty()) {
                                        source.sendFailure(Component.literal("No capitals exist."));
                                        return 0;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
                                        boolean changed = CapitalSuccessionService.handleSuccessionIfNeeded(level, capital);
                                        final UUID capitalId = capital.getCapitalId();
                                        final boolean changedFinal = changed;
                                        source.sendSuccess(() -> Component.literal(
                                                "Forced succession check on capital " + capitalId
                                                        + " villageId=" + capital.getVillageId()
                                                        + " changed=" + changedFinal
                                                        + " sovereign=" + capital.getSovereign()
                                                        + " heir=" + capital.getHeir()
                                        ), false);
                                    }

                                    return 1;
                                }))
        );
    }
}