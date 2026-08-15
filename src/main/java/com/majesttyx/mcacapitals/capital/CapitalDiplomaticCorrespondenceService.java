package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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

        online.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.correspondence.package_arrival",
                        capitalNameComponent(
                                getCapitalName(level, sourceCapital)
                        )
                ).withStyle(ChatFormatting.GOLD)
        );
    }

    public static void sendResolutionLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            Component title,
            Component message
    ) {
        if (level == null
                || recipientPlayerId == null
                || isBlank(message)) {
            return;
        }

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);

        if (online == null) {
            return;
        }

        Component heading = isBlank(title)
                ? Component.translatable("mcacapitals.diplomacy.correspondence.heading")
                : title;

        online.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.correspondence.notice",
                        heading,
                        message
                ).withStyle(ChatFormatting.GOLD)
        );
    }

    public static Component formatContents(List<ItemStack> contents) {
        if (contents == null || contents.isEmpty()) {
            return Component.translatable("mcacapitals.diplomacy.contents.nothing");
        }

        MutableComponent result = Component.empty();
        boolean hasEntry = false;

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (hasEntry) {
                result.append(Component.literal("\n"));
            }

            result.append(
                    Component.translatable(
                            "mcacapitals.diplomacy.contents.entry",
                            stack.getCount(),
                            stack.getHoverName()
                    )
            );
            hasEntry = true;
        }

        return hasEntry
                ? result
                : Component.translatable("mcacapitals.diplomacy.contents.nothing");
    }

    public static Component formatContentsInline(List<ItemStack> contents) {
        if (contents == null || contents.isEmpty()) {
            return Component.translatable("mcacapitals.diplomacy.contents.nothing");
        }

        MutableComponent result = Component.empty();
        boolean hasEntry = false;

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (hasEntry) {
                result.append(Component.literal(", "));
            }

            result.append(
                    Component.translatable(
                            "mcacapitals.diplomacy.contents.entry",
                            stack.getCount(),
                            stack.getHoverName()
                    )
            );
            hasEntry = true;
        }

        return hasEntry
                ? result
                : Component.translatable("mcacapitals.diplomacy.contents.nothing");
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

    private static Component capitalNameComponent(String name) {
        return name == null
                || name.isBlank()
                || "Unknown Capital".equals(name)
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(name);
    }

    private static boolean isBlank(Component component) {
        return component == null || component.getString().isBlank();
    }
}
