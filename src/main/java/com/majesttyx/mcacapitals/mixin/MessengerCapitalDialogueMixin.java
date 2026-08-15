package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.dialogue.CapitalDialogueService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.mca.entity.VillagerEntityMCA;
import fabric.net.mca.entity.ai.Messenger;
import fabric.net.mca.entity.ai.relationship.Personality;
import fabric.net.mca.server.world.data.PlayerSaveData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.Locale;

@Pseudo
@Mixin(targets = "fabric.net.mca.entity.VillagerEntityMCA", remap = false)
public abstract class MessengerCapitalDialogueMixin {

    public MutableComponent getTranslatable(Player target, String phraseId, Object... params) {
        Messenger messenger = (Messenger) (Object) this;
        Entity speaker = messenger.asEntity();

        if (target instanceof ServerPlayer serverPlayer && speaker != null) {
            Component line = CapitalDialogueService.maybeFormatMcaPhraseLine(
                    serverPlayer,
                    speaker,
                    phraseId
            );
            if (line != null) {
                return line.copy();
            }
        }

        String genderString = "";
        String targetName;

        if (target instanceof ServerPlayer serverPlayer) {
            targetName = MCAIntegrationBridge.getPlayerDialogueName(serverPlayer);
            genderString = "#G" + PlayerSaveData.get(serverPlayer).getGender().name().toLowerCase(Locale.ROOT) + ".";
        } else {
            targetName = target.getName().getString();
        }

        Object[] newParams = new Object[params.length + 1];
        System.arraycopy(params, 0, newParams, 1, params.length);
        newParams[0] = targetName;

        String professionString = "";
        if (speaker instanceof VillagerEntityMCA villager && !villager.isBaby()) {
            professionString = "#P"
                    + BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getProfession()).getPath()
                    + ".";
        }

        String personalityString = "";
        if (speaker instanceof VillagerEntityMCA villager) {
            personalityString = "#E"
                    + Personality.encodeDialogueId(villager.getVillagerBrain().getPersonality().getId())
                    + ".";
        }

        return Component.translatable(
                genderString
                        + personalityString
                        + professionString
                        + "#T"
                        + messenger.getDialogueType(target).name()
                        + "."
                        + phraseId,
                newParams
        );
    }
}