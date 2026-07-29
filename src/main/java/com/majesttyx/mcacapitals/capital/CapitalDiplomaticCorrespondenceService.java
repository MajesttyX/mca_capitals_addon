package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);

        if (online == null) {
            return;
        }

        String sourceName = getCapitalName(level, sourceCapital);

        online.sendSystemMessage(
                Component.literal(
                        "Your Ambassador reports an urgent diplomatic package from "
                                + sourceName
                                + ". Speak to the Ambassador to inspect and answer it."
                ).withStyle(ChatFormatting.GOLD)
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

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);

        if (online == null) {
            return;
        }

        String heading = title == null || title.isBlank()
                ? "Diplomatic Correspondence"
                : title;

        online.sendSystemMessage(
                Component.literal(heading + ": " + message)
                        .withStyle(ChatFormatting.GOLD)
        );
    }

    public static String formatContents(List<ItemStack> contents) {
        if (contents == null || contents.isEmpty()) {
            return "Nothing";
        }

        List<String> lines = new ArrayList<>();

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            lines.add(stack.getCount() + " × " + stack.getHoverName().getString());
        }

        return lines.isEmpty() ? "Nothing" : String.join("\n", lines);
    }

    public static String getCapitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null || capital.getVillageId() == null) {
            return "Unknown Capital";
        }

        return MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
    }
}