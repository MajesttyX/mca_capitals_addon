package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbdicationPromptManager {

    private enum Stage {
        FIRST_CONFIRM,
        FINAL_CONFIRM
    }

    private static final Map<UUID, UUID> PENDING_CAPITALS = new HashMap<>();
    private static final Map<UUID, Stage> PENDING_STAGES = new HashMap<>();

    private AbdicationPromptManager() {
    }

    public static void beginPrompt(ServerPlayer player, CapitalRecord capital) {
        UUID playerId = player.getUUID();
        PENDING_CAPITALS.put(playerId, capital.getCapitalId());
        PENDING_STAGES.put(playerId, Stage.FIRST_CONFIRM);

        player.sendSystemMessage(Component.literal("Do you wish to abdicate the throne? "));
        player.sendSystemMessage(
                clickable("Yes", "/capitalabdication yes", ChatFormatting.GREEN)
                        .append(Component.literal(" / "))
                        .append(clickable("No", "/capitalabdication no", ChatFormatting.RED))
        );
    }

    public static int handleResponse(ServerPlayer player, boolean yes) {
        UUID playerId = player.getUUID();
        UUID capitalId = PENDING_CAPITALS.get(playerId);
        Stage stage = PENDING_STAGES.get(playerId);

        if (capitalId == null || stage == null) {
            player.sendSystemMessage(Component.literal("There is no abdication decision awaiting your answer."));
            return 0;
        }

        if (!yes) {
            clear(playerId);
            player.sendSystemMessage(Component.literal("The Declaration of Abdication has been set aside."));
            return 1;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || !playerId.equals(capital.getSovereign())) {
            clear(playerId);
            player.sendSystemMessage(Component.literal("You are no longer the sovereign of that capital."));
            return 0;
        }

        if (stage == Stage.FIRST_CONFIRM) {
            PENDING_STAGES.put(playerId, Stage.FINAL_CONFIRM);
            player.sendSystemMessage(Component.literal("Are you sure? This cannot be undone."));
            player.sendSystemMessage(
                    clickable("Yes", "/capitalabdication yes", ChatFormatting.GREEN)
                            .append(Component.literal(" / "))
                            .append(clickable("No", "/capitalabdication no", ChatFormatting.RED))
            );
            return 1;
        }

        boolean changed = CapitalFoundationService.abdicateSovereign(player.serverLevel(), capital);
        clear(playerId);

        if (!changed) {
            player.sendSystemMessage(Component.literal("There is no valid successor to receive the throne."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("By solemn declaration, you have abdicated the throne."));
        return 1;
    }

    private static void clear(UUID playerId) {
        PENDING_CAPITALS.remove(playerId);
        PENDING_STAGES.remove(playerId);
    }

    private static MutableComponent clickable(String text, String command, ChatFormatting color) {
        return Component.literal(text).setStyle(
                Style.EMPTY
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        );
    }
}