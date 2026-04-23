package com.example.mcacapitals.mixin;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.mca.resources.data.dialogue.Actions", remap = false)
public abstract class DialogueChatFallbackMixin {

    private static final String MCA_CHAT_TOPIC = "chat.topic";
    private static final String MCA_CHAT_FAIL = "chat.fail";
    private static final String CAPITAL_CHAT_TOPIC = "mcacapitals_chat_capital_topic";
    private static final String CAPITAL_CHAT_FAIL = "mcacapitals_chat_capital_fail";
    private static final int CAPITAL_TOPIC_CHANCE = 30;
    private static final int CAPITAL_FAIL_CHANCE = 35;

    @ModifyVariable(
            method = "lambda$static$0(Ljava/lang/String;Lforge/net/mca/entity/VillagerEntityMCA;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
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

        if (MCA_CHAT_TOPIC.equals(nextKey)) {
            if (level.random.nextInt(100) < CAPITAL_TOPIC_CHANCE) {
                MCACapitals.LOGGER.info(
                        "[MCACapitals] Redirected capital chat topic. villager='{}', player='{}', next='{}'",
                        villager.getName().getString(),
                        player.getName().getString(),
                        CAPITAL_CHAT_TOPIC
                );
                return CAPITAL_CHAT_TOPIC;
            }
            return nextKey;
        }

        if (level.random.nextInt(100) < CAPITAL_FAIL_CHANCE) {
            MCACapitals.LOGGER.info(
                    "[MCACapitals] Redirected capital chat fail. villager='{}', player='{}', next='{}'",
                    villager.getName().getString(),
                    player.getName().getString(),
                    CAPITAL_CHAT_FAIL
            );
            return CAPITAL_CHAT_FAIL;
        }

        return nextKey;
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