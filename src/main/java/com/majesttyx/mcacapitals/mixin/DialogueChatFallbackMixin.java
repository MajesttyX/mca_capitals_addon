package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueRuntime;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @ModifyVariable(
            method = "lambda$static$0(Ljava/lang/String;Lnet/conczin/mca/entity/VillagerEntityMCA;Lnet/minecraft/class_3222;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false,
            require = 0
    )
    private static String mcacapitals$redirectCapitalChatDialogue(
            String nextKey,
            String ignoredCurrentQuestion,
            @Coerce Object villagerObj,
            ServerPlayer player
    ) {
        if (nextKey == null || player == null || villagerObj == null) {
            return nextKey;
        }

        if (!(villagerObj instanceof Entity villager)) {
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
                MCACapitals.LOGGER.info(
                        "[MCACapitals] Redirected capital chat topic to chronicle news. villager='{}', player='{}', next='{}'",
                        villager.getName().getString(),
                        player.getName().getString(),
                        newsDialogueId
                );
                return newsDialogueId;
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

        if (level.random.nextInt(100) < GENERAL_FAIL_CHANCE) {
            return CapitalDialogueRuntime.GENERAL_FAIL;
        }

        return nextKey;
    }

    @Inject(
            method = "lambda$static$0(Ljava/lang/String;Lnet/conczin/mca/entity/VillagerEntityMCA;Lnet/minecraft/class_3222;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void mcacapitals$handleManagedRuntimeDialogue(
            String nextKey,
            @Coerce Object villagerObj,
            ServerPlayer player,
            CallbackInfo ci
    ) {
        if (nextKey == null || player == null || villagerObj == null) {
            return;
        }

        if (!(villagerObj instanceof Entity villager)) {
            return;
        }

        if (!CapitalDialogueRuntime.isManagedRuntimeKey(nextKey)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            MCAIntegrationBridge.stopInteracting(villager);
            ci.cancel();
            return;
        }

        String line = CapitalDialogueRuntime.formatManagedRuntimeLine(nextKey, player, villager, level, capital);
        if (line == null || line.isBlank()) {
            MCAIntegrationBridge.stopInteracting(villager);
            ci.cancel();
            return;
        }

        player.sendSystemMessage(Component.literal(villager.getName().getString() + ": " + line));
        MCAIntegrationBridge.stopInteracting(villager);
        ci.cancel();
    }

    private static boolean isBabyOrToddler(ServerLevel level, Entity villager) {
        if (level == null || villager == null) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, villager.getUUID());
        return "BABY".equalsIgnoreCase(ageState) || "TODDLER".equalsIgnoreCase(ageState);
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }
}