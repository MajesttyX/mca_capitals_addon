package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
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

        player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.do_you_wish_to_abdicate_the_throne"));
        player.sendSystemMessage(
                clickable(
                        Component.translatable("mcacapitals.system.abdication_prompt_manager.yes"),
                        "/capitalabdication yes",
                        ChatFormatting.GREEN
                )
                        .append(Component.literal(" / "))
                        .append(clickable(
                                Component.translatable("mcacapitals.system.abdication_prompt_manager.no"),
                                "/capitalabdication no",
                                ChatFormatting.RED
                        ))
        );
    }

    public static int handleResponse(ServerPlayer player, boolean yes) {
        UUID playerId = player.getUUID();
        UUID capitalId = PENDING_CAPITALS.get(playerId);
        Stage stage = PENDING_STAGES.get(playerId);

        if (capitalId == null || stage == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.there_is_no_abdication_decision_awaiting_your_answer"));
            return 0;
        }

        if (!yes) {
            clear(playerId);
            player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.the_declaration_of_abdication_has_been_set_aside"));
            return 1;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || !playerId.equals(capital.getSovereign())) {
            clear(playerId);
            player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.you_are_no_longer_the_sovereign_of_that_capital"));
            return 0;
        }

        if (stage == Stage.FIRST_CONFIRM) {
            PENDING_STAGES.put(playerId, Stage.FINAL_CONFIRM);
            player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.are_you_sure_this_cannot_be_undone"));
            player.sendSystemMessage(
                    clickable(
                            Component.translatable("mcacapitals.system.abdication_prompt_manager.yes"),
                            "/capitalabdication yes",
                            ChatFormatting.GREEN
                    )
                            .append(Component.literal(" / "))
                            .append(clickable(
                                    Component.translatable("mcacapitals.system.abdication_prompt_manager.no"),
                                    "/capitalabdication no",
                                    ChatFormatting.RED
                            ))
            );
            return 1;
        }

        boolean changed = CapitalFoundationService.abdicateSovereign(player.serverLevel(), capital);
        clear(playerId);

        if (!changed) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.there_is_no_valid_successor_to_receive_the_throne"));
            return 0;
        }

        player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_manager.by_solemn_declaration_you_have_abdicated_the_throne"));
        return 1;
    }

    private static void clear(UUID playerId) {
        PENDING_CAPITALS.remove(playerId);
        PENDING_STAGES.remove(playerId);
    }

    private static MutableComponent clickable(Component text, String command, ChatFormatting color) {
        return text.copy().setStyle(
                Style.EMPTY
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        );
    }
}