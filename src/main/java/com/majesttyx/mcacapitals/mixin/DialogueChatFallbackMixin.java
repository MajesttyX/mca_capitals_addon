package com.majesttyx.mcacapitals.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueRuntime;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueService;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueSpeaker;
import com.majesttyx.mcacapitals.dialogue.CapitalPoliticalDialogueService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.resources.data.dialogue.Actions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.conczin.mca.resources.data.dialogue.Actions", remap = false)
public abstract class DialogueChatFallbackMixin {

    private static final String MCA_CHAT_TOPIC = "chat.topic";
    private static final String MCA_CHAT_FAIL = "chat.fail";

    private static final int PLAYER_SOVEREIGN_CHAT_CHANCE = 45;
    private static final int RANK_CHAT_CHANCE = 55;
    private static final int GENERAL_ALL_RANKS_CHANCE = 55;
    private static final int GENERAL_SUCCESS_CHANCE = 45;
    private static final int GENERAL_FAIL_CHANCE = 60;

    /**
     * MCA exposes dialogue actions through its public action factory registry. Wrap the "next"
     * factory once after MCA initializes it instead of injecting into javac's synthetic lambda
     * method. The latter is not a stable server integration point and can silently stop matching
     * when MCA is rebuilt even when its source behavior has not changed.
     */
    @Inject(
            method = "<clinit>",
            at = @At("TAIL"),
            remap = false
    )
    private static void mcacapitals$installCapitalNextAction(CallbackInfo ci) {
        Actions.Factory<JsonElement> mcaNextFactory = Actions.TYPES.get("next");
        if (mcaNextFactory == null) {
            MCACapitals.LOGGER.error(
                    "[MCACapitals] MCA dialogue 'next' action factory was not available; Capitals chat routing was not installed."
            );
            return;
        }

        Actions.TYPES.put("next", json -> {
            Actions.Action originalAction = mcaNextFactory.parse(json);
            String configuredNext = readNextId(json);

            if (configuredNext == null || configuredNext.isBlank()) {
                return originalAction;
            }

            return (villager, player) -> {
                if (mcacapitals$handleManagedRuntimeDialogue(
                        configuredNext,
                        villager,
                        player
                )) {
                    return;
                }

                String redirectedNext = mcacapitals$redirectCapitalChatDialogue(
                        configuredNext,
                        villager,
                        player
                );

                if (mcacapitals$handleManagedRuntimeDialogue(
                        redirectedNext,
                        villager,
                        player
                )) {
                    return;
                }

                if (Objects.equals(configuredNext, redirectedNext)) {
                    originalAction.trigger(villager, player);
                    return;
                }

                mcaNextFactory.parse(new JsonPrimitive(redirectedNext)).trigger(villager, player);
            };
        });
    }

    private static String readNextId(JsonElement json) {
        if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) {
            return null;
        }

        try {
            return json.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean mcacapitals$handleManagedRuntimeDialogue(
            String nextKey,
            Object villagerObj,
            ServerPlayer player
    ) {
        if (!CapitalDialogueRuntime.isManagedRuntimeKey(nextKey)
                || player == null
                || !(villagerObj instanceof Entity villager)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            MCAIntegrationBridge.stopInteracting(villager);
            return true;
        }

        Component line = CapitalDialogueRuntime.formatManagedRuntimeComponent(
                nextKey,
                player,
                villager,
                level,
                capital
        );

        if (line != null && !line.getString().isBlank()) {
            CapitalDialogueSpeaker.speakVillager(player, villager, line);
        }

        MCAIntegrationBridge.stopInteracting(villager);
        return true;
    }

    private static String mcacapitals$redirectCapitalChatDialogue(
            String nextKey,
            Object villagerObj,
            ServerPlayer player
    ) {
        if (nextKey == null || player == null || !(villagerObj instanceof Entity villager)) {
            return nextKey;
        }

        if (!MCA_CHAT_TOPIC.equals(nextKey) && !MCA_CHAT_FAIL.equals(nextKey)) {
            return nextKey;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return nextKey;
        }

        if (isBabyOrToddler(level, villager)) {
            return nextKey;
        }

        if (MCA_CHAT_TOPIC.equals(nextKey)) {
            String newsDialogueId = CapitalDialogueService.maybeResolveCapitalNewsDialogueId(player, villager);
            if (newsDialogueId != null && !newsDialogueId.isBlank()) {
                return newsDialogueId;
            }

            String politicalDialogueId = CapitalPoliticalDialogueService.maybeResolvePoliticalDialogueId(player, villager);
            if (politicalDialogueId != null && !politicalDialogueId.isBlank()) {
                return politicalDialogueId;
            }

            String playerSovereignDialogueId = CapitalDialogueService.maybeResolvePlayerSovereignDialogueId(player, villager);
            if (playerSovereignDialogueId != null
                    && !playerSovereignDialogueId.isBlank()
                    && level.random.nextInt(100) < PLAYER_SOVEREIGN_CHAT_CHANCE) {
                return playerSovereignDialogueId;
            }

            String rankDialogueId = CapitalDialogueService.maybeResolveCapitalRankDialogueId(player, villager);
            if (rankDialogueId != null
                    && !rankDialogueId.isBlank()
                    && level.random.nextInt(100) < RANK_CHAT_CHANCE) {
                return rankDialogueId;
            }

            if (level.random.nextInt(100) < GENERAL_ALL_RANKS_CHANCE) {
                return CapitalDialogueRuntime.GENERAL_ALL_RANKS;
            }

            if (level.random.nextInt(100) < GENERAL_SUCCESS_CHANCE) {
                return CapitalDialogueRuntime.GENERAL_SUCCESS;
            }

            return nextKey;
        }

        if (isUntitledCommoner(level, villager.getUUID())
                && level.random.nextInt(100) < GENERAL_FAIL_CHANCE) {
            return CapitalDialogueRuntime.GENERAL_FAIL;
        }

        return nextKey;
    }

    private static boolean isBabyOrToddler(ServerLevel level, Entity villager) {
        if (level == null || villager == null) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, villager.getUUID());
        return "BABY".equalsIgnoreCase(ageState) || "TODDLER".equalsIgnoreCase(ageState);
    }

    private static boolean isUntitledCommoner(ServerLevel level, UUID villagerId) {
        CapitalTitleResolver.ResolvedTitleId titleId =
                CapitalTitleResolver.getResolvedTitleIdForEntity(level, villagerId);
        return titleId == CapitalTitleResolver.ResolvedTitleId.NONE
                || titleId == CapitalTitleResolver.ResolvedTitleId.COMMONER;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(level, villageId);
    }
}
