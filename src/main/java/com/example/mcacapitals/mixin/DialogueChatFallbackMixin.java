package com.example.mcacapitals.mixin;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.mca.resources.data.dialogue.Actions", remap = false)
public abstract class DialogueChatFallbackMixin {

    private static final List<String> CAPITAL_CHAT_LINES = List.of(
            "There is never a true quiet day in a capital. Even silence here feels watchful.",
            "People speak more carefully in a capital. Words tend to travel farther than footsteps.",
            "A capital draws ambition the way lanterns draw moths.",
            "Once a village becomes a capital, every little dispute starts sounding like state business.",
            "You can always tell a capital by the way everyone seems to know half a secret.",
            "There is pride in a capital, though not always the peaceful sort.",
            "A crown changes more than the ruler. It changes the whole mood of a place.",
            "In a capital, even ordinary days feel like they are being observed.",
            "People stand a little straighter in a capital, as though history might be watching.",
            "Every capital has its gossip, its grudges, and its grand ideas. Usually all at once.",
            "A capital is never short on rumours. Truth is the harder thing to come by.",
            "It is strange how quickly a place begins to think itself important once it becomes a capital.",
            "Capitals have a way of making small matters feel larger than they are.",
            "There is always someone in a capital trying to rise, impress, or interfere.",
            "Life in a capital feels busier, even when no one can quite say why.",
            "The people here carry themselves differently now. A capital changes expectations.",
            "You hear more polished smiles and quieter schemes in a capital than anywhere else.",
            "A founded capital always seems to believe tomorrow will bring something significant.",
            "In a capital, news reaches you quickly, though sense often arrives later.",
            "There is a certain weight to living in a capital. Some wear it proudly, others poorly.",
            "A capital teaches people to pay attention, whether they wish to or not.",
            "Even celebration feels more political in a capital.",
            "No one admits to listening for rumours here, yet everyone somehow hears them.",
            "A capital makes people feel closer to power, even when they are nowhere near it.",
            "There is more ceremony in a capital, but not always more wisdom.",
            "The air in a capital always feels full of plans, promises, and suspicion.",
            "Every capital has a pulse of its own. You feel it most when something is about to change.",
            "People remember slights longer in a capital. Importance has a way of feeding memory.",
            "A village may live simply, but a capital rarely allows itself that luxury.",
            "There is always the sense in a capital that someone, somewhere, is waiting for their moment."
    );

    @Inject(
            method = "lambda$static$0(Ljava/lang/String;Lforge/net/mca/entity/VillagerEntityMCA;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$replaceCapitalChatFallback(
            String nextKey,
            @Coerce Object villagerObj,
            ServerPlayer player,
            CallbackInfo ci
    ) {
        if (player == null || villagerObj == null || nextKey == null) {
            return;
        }

        if (!"chat.success".equals(nextKey)) {
            return;
        }

        if (!(villagerObj instanceof Entity villager)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null) {
            return;
        }

        if (capital.getState() != CapitalState.ACTIVE) {
            return;
        }

        if (CAPITAL_CHAT_LINES.isEmpty()) {
            return;
        }

        if (level.random.nextInt(100) >= 30) {
            return;
        }

        String line = CAPITAL_CHAT_LINES.get(level.random.nextInt(CAPITAL_CHAT_LINES.size()));
        String spokenLine = villager.getName().getString() + ": " + line;

        MCACapitals.LOGGER.info(
                "[MCACapitals] Replaced capital chat.success fallback. villager='{}', player='{}', line='{}'",
                villager.getName().getString(),
                player.getName().getString(),
                spokenLine
        );

        player.sendSystemMessage(Component.literal(spokenLine));
        ci.cancel();
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