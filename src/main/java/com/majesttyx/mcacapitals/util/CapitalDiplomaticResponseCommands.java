package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticCorrespondenceService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticResolutionService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticResponseCommands {

    private CapitalDiplomaticResponseCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalsaccept")
                        .executes(
                                context ->
                                        respondWithoutId(
                                                context.getSource(),
                                                true
                                        )
                        )
                        .then(
                                Commands.argument(
                                                "shipmentId",
                                                StringArgumentType.word()
                                        )
                                        .executes(
                                                context ->
                                                        respondWithId(
                                                                context.getSource(),
                                                                StringArgumentType
                                                                        .getString(
                                                                                context,
                                                                                "shipmentId"
                                                                        ),
                                                                true
                                                        )
                                        )
                        )
        );

        dispatcher.register(
                Commands.literal("capitalsreturn")
                        .executes(
                                context ->
                                        respondWithoutId(
                                                context.getSource(),
                                                false
                                        )
                        )
                        .then(
                                Commands.argument(
                                                "shipmentId",
                                                StringArgumentType.word()
                                        )
                                        .executes(
                                                context ->
                                                        respondWithId(
                                                                context.getSource(),
                                                                StringArgumentType
                                                                        .getString(
                                                                                context,
                                                                                "shipmentId"
                                                                        ),
                                                                false
                                                        )
                                        )
                        )
        );
    }

    private static int respondWithoutId(
            CommandSourceStack source,
            boolean accept
    ) {
        ServerPlayer player =
                getPlayer(source);

        if (player == null) {
            return 0;
        }

        List<DiplomaticShipment> pending =
                CapitalDiplomaticResolutionService
                        .getPendingForPlayer(
                                player.serverLevel(),
                                player.getUUID()
                        );

        if (pending.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You have no diplomatic packages awaiting a response."
                    )
            );

            return 0;
        }

        if (pending.size() == 1) {
            DiplomaticShipment shipment =
                    pending.getFirst();

            return accept
                    ? CapitalDiplomaticResolutionService
                    .acceptPlayerShipment(
                            player,
                            shipment.getShipmentId()
                    )
                    : CapitalDiplomaticResolutionService
                    .returnPlayerShipment(
                            player,
                            shipment.getShipmentId()
                    );
        }

        showPendingList(
                player,
                pending,
                accept
        );

        return 1;
    }

    private static int respondWithId(
            CommandSourceStack source,
            String rawShipmentId,
            boolean accept
    ) {
        ServerPlayer player =
                getPlayer(source);

        if (player == null) {
            return 0;
        }

        UUID shipmentId;

        try {
            shipmentId =
                    UUID.fromString(
                            rawShipmentId
                    );
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.literal(
                            "That diplomatic package ID is invalid."
                    )
            );

            return 0;
        }

        return accept
                ? CapitalDiplomaticResolutionService
                .acceptPlayerShipment(
                        player,
                        shipmentId
                )
                : CapitalDiplomaticResolutionService
                .returnPlayerShipment(
                        player,
                        shipmentId
                );
    }

    private static void showPendingList(
            ServerPlayer player,
            List<DiplomaticShipment> pending,
            boolean accept
    ) {
        ServerLevel level =
                player.serverLevel();

        player.sendSystemMessage(
                Component.literal(
                        accept
                                ? "Choose the diplomatic package to accept:"
                                : "Choose the diplomatic package to return:"
                )
        );

        for (DiplomaticShipment shipment : pending) {
            CapitalRecord sourceCapital =
                    CapitalManager.getCapital(
                            shipment.getSourceCapitalId()
                    );

            String sourceName =
                    CapitalDiplomaticCorrespondenceService
                            .getCapitalName(
                                    level,
                                    sourceCapital
                            );

            String command =
                    accept
                            ? "/capitalsaccept "
                            + shipment.getShipmentId()
                            : "/capitalsreturn "
                            + shipment.getShipmentId();

            String label =
                    accept
                            ? "[Accept] "
                            : "[Return] ";

            ChatFormatting color =
                    accept
                            ? ChatFormatting.DARK_GREEN
                            : ChatFormatting.RED;

            MutableComponent line =
                    Component.literal(label)
                            .setStyle(
                                    Style.EMPTY
                                            .withColor(color)
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action
                                                                    .RUN_COMMAND,
                                                            command
                                                    )
                                            )
                                            .withHoverEvent(
                                                    new HoverEvent(
                                                            HoverEvent.Action
                                                                    .SHOW_TEXT,
                                                            Component.literal(
                                                                    CapitalDiplomaticCorrespondenceService
                                                                            .formatContents(
                                                                                    shipment.getContents()
                                                                            )
                                                            )
                                                    )
                                            )
                            )
                            .append(
                                    Component.literal(
                                            sourceName
                                                    + " — "
                                                    + shipment
                                                    .getAppraisal()
                                    ).withStyle(
                                            ChatFormatting.GOLD
                                    )
                            );

            player.sendSystemMessage(line);
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
                            "Only a player may answer diplomatic correspondence."
                    )
            );

            return null;
        }
    }
}