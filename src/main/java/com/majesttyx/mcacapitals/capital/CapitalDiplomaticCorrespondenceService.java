package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticCorrespondenceService {

    private CapitalDiplomaticCorrespondenceService() {
    }

    public static void sendArrivalLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            DiplomaticShipment shipment,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital
    ) {
        if (level == null
                || recipientPlayerId == null
                || shipment == null
                || sourceCapital == null
                || targetCapital == null) {
            return;
        }

        String sourceName =
                getCapitalName(
                        level,
                        sourceCapital
                );

        String targetName =
                getCapitalName(
                        level,
                        targetCapital
                );

        String contents =
                formatContents(
                        shipment.getContents()
                );

        MutableComponent decisionPage =
                Component.literal(
                        "The Ambassador of "
                                + targetName
                                + " judges the package to be "
                                + shipment
                                .getAppraisal()
                                .toLowerCase()
                                + ".\n\n"
                );

        decisionPage.append(
                commandButton(
                        "[Accept Gift]",
                        "/capitalsaccept "
                                + shipment.getShipmentId(),
                        ChatFormatting.DARK_GREEN,
                        "Accept the package into the capital's Storage system."
                )
        );

        decisionPage.append(
                Component.literal("\n\n")
        );

        decisionPage.append(
                commandButton(
                        "[Return Gift]",
                        "/capitalsreturn "
                                + shipment.getShipmentId(),
                        ChatFormatting.RED,
                        "Return the package to the sending capital."
                )
        );

        decisionPage.append(
                Component.literal(
                        "\n\nYou may also use /capitalsaccept or /capitalsreturn."
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        List<Component> pages = List.of(
                Component.literal(
                        "A diplomatic package has arrived from "
                                + sourceName
                                + ".\n\nContents:\n"
                                + contents
                ),
                decisionPage
        );

        sendLetter(
                level,
                recipientPlayerId,
                "Diplomatic Package from "
                        + sourceName,
                pages
        );
    }

    public static void sendResolutionLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            String title,
            String message
    ) {
        if (level == null
                || recipientPlayerId == null
                || message == null
                || message.isBlank()) {
            return;
        }

        sendLetter(
                level,
                recipientPlayerId,
                title == null
                        || title.isBlank()
                        ? "Diplomatic Correspondence"
                        : title,
                List.of(
                        Component.literal(message)
                )
        );
    }

    private static void sendLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            String title,
            List<Component> pages
    ) {
        PlayerSaveData saveData =
                PlayerSaveData.get(
                        level,
                        recipientPlayerId
                );

        saveData.sendMail(
                new PlayerSaveData.Letter(
                        title,
                        new ArrayList<>(pages)
                )
        );

        ServerPlayer online =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                recipientPlayerId
                        );

        if (online != null) {
            PlayerSaveData.showMailNotification(
                    online
            );

            online.sendSystemMessage(
                    Component.literal(
                            "A diplomatic letter is waiting in your MCA mailbox."
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );
        }
    }

    private static MutableComponent commandButton(
            String label,
            String command,
            ChatFormatting color,
            String hoverText
    ) {
        return Component.literal(label)
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
                                                        hoverText
                                                )
                                        )
                                )
                );
    }

    public static String formatContents(
            List<ItemStack> contents
    ) {
        if (contents == null || contents.isEmpty()) {
            return "Nothing";
        }

        List<String> lines =
                new ArrayList<>();

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            lines.add(
                    stack.getCount()
                            + " × "
                            + stack.getHoverName()
                            .getString()
            );
        }

        return lines.isEmpty()
                ? "Nothing"
                : String.join("\n", lines);
    }

    public static String getCapitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null
                || capital.getVillageId() == null) {
            return "Unknown Capital";
        }

        return MCAIntegrationBridge.getVillageName(
                level,
                capital.getVillageId()
        );
    }
}