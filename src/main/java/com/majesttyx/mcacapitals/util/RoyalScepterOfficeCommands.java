package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorService;
import com.majesttyx.mcacapitals.capital.CapitalBuildingService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalMasterOfLawsService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
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

public final class RoyalScepterOfficeCommands {

    private RoyalScepterOfficeCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("royalscepteroffice")
                        .then(
                                Commands.literal("ambassador")
                                        .then(
                                                Commands.argument(
                                                                "villagerId",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        appointAmbassador(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "villagerId"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("masteroflaws")
                                        .then(
                                                Commands.argument(
                                                                "villagerId",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        appointMasterOfLaws(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "villagerId"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int appointAmbassador(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        AppointmentContext context =
                validateTarget(
                        source,
                        rawVillagerId
                );

        if (!context.valid()) {
            return 0;
        }

        if (!CapitalBuildingService
                .hasAmbassadorBuildings(
                        context.level(),
                        context.capital()
                )) {
            source.sendFailure(
                    Component.literal(
                            "The capital requires an operational Inn and Storage building before an Ambassador can be appointed."
                    )
            );
            return 0;
        }

        if (!CapitalAmbassadorService
                .appointAmbassador(
                        context.level(),
                        context.capital(),
                        context.villagerId(),
                        context.residents()
                )) {
            source.sendFailure(
                    Component.literal(
                            "That villager is not eligible to serve as Ambassador."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        context.name()
                                + " was appointed Ambassador."
                ),
                false
        );

        return 1;
    }

    private static int appointMasterOfLaws(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        AppointmentContext context =
                validateTarget(
                        source,
                        rawVillagerId
                );

        if (!context.valid()) {
            return 0;
        }

        if (!CapitalBuildingService.hasPrison(
                context.level(),
                context.capital()
        )) {
            source.sendFailure(
                    Component.literal(
                            "The capital requires an operational Prison before a Master of Laws can be appointed."
                    )
            );
            return 0;
        }

        if (!CapitalMasterOfLawsService
                .appointMasterOfLaws(
                        context.level(),
                        context.capital(),
                        context.villagerId(),
                        context.residents()
                )) {
            source.sendFailure(
                    Component.literal(
                            "That villager is not eligible to serve as Master of Laws."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        context.name()
                                + " was appointed Master of Laws."
                ),
                false
        );

        return 1;
    }

    private static AppointmentContext validateTarget(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return AppointmentContext.failure();
        }

        UUID villagerId =
                parseUuid(
                        source,
                        rawVillagerId
                );

        if (villagerId == null) {
            return AppointmentContext.failure();
        }

        ServerLevel level = player.serverLevel();

        if (!MCAIntegrationBridge.isMCAVillager(
                level,
                villagerId
        )) {
            source.sendFailure(
                    Component.literal(
                            "Target is not an MCA villager."
                    )
            );
            return AppointmentContext.failure();
        }

        CapitalRecord capital =
                resolveCapital(
                        level,
                        villagerId
                );

        if (capital == null) {
            source.sendFailure(
                    Component.literal(
                            "That villager is not part of a capital."
                    )
            );
            return AppointmentContext.failure();
        }

        if (!canManageCapital(
                player,
                capital
        )) {
            source.sendFailure(
                    Component.literal(
                            "Only the sovereign, the authorized Hand, or an operator may use the Royal Scepter here."
                    )
            );
            return AppointmentContext.failure();
        }

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        capital.getCapitalId()
                );

        if (!residents.contains(villagerId)) {
            source.sendFailure(
                    Component.literal(
                            "That villager is not a resident of the capital."
                    )
            );
            return AppointmentContext.failure();
        }

        String name =
                MCAIntegrationBridge
                .getEntityByUuid(
                        level,
                        villagerId
                ) == null
                        ? villagerId.toString()
                        : MCAIntegrationBridge
                        .getEntityByUuid(
                                level,
                                villagerId
                        )
                        .getName()
                        .getString();

        return AppointmentContext.success(
                level,
                capital,
                villagerId,
                residents,
                name
        );
    }

    private static boolean canManageCapital(
            ServerPlayer player,
            CapitalRecord capital
    ) {
        if (player == null
                || capital == null) {
            return false;
        }

        return player.hasPermissions(2)
                || player.getUUID().equals(
                        capital.getPlayerSovereignId()
                )
                || player.getUUID().equals(
                        capital.getSovereign()
                )
                || PlayerCapitalTitleService.isHand(
                        player.serverLevel(),
                        capital,
                        player.getUUID()
                );
    }

    private static CapitalRecord resolveCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord capital =
                CapitalManager.getCapitalBySovereign(
                        villagerId
                );

        if (capital != null) {
            return capital;
        }

        Integer villageId =
                MCAIntegrationBridge
                .getVillageIdForResident(
                        level,
                        villagerId
                );

        if (villageId == null) {
            return null;
        }

        return CapitalManager.getCapitalByVillageId(
                level,
                villageId
        );
    }

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawVillagerId
    ) {
        try {
            return UUID.fromString(rawVillagerId);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.literal(
                            "Invalid villager UUID."
                    )
            );
            return null;
        }
    }

    private static ServerPlayer getPlayer(
            CommandSourceStack source
    ) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(
                    Component.literal(
                            "Only a player can use the Royal Scepter."
                    )
            );
            return null;
        }
    }

    private record AppointmentContext(
            boolean valid,
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents,
            String name
    ) {
        private static AppointmentContext success(
                ServerLevel level,
                CapitalRecord capital,
                UUID villagerId,
                Set<UUID> residents,
                String name
        ) {
            return new AppointmentContext(
                    true,
                    level,
                    capital,
                    villagerId,
                    residents,
                    name
            );
        }

        private static AppointmentContext failure() {
            return new AppointmentContext(
                    false,
                    null,
                    null,
                    null,
                    Set.of(),
                    ""
            );
        }
    }
}
