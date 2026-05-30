package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.dialogue.CapitalDialogueService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "forge.net.mca.entity.ai.Messenger", remap = false)
public abstract class MessengerCapitalDialogueMixin {

    @Inject(method = "getTranslatable", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$useCapitalManagedPhrase(
            Player player,
            String phraseKey,
            Object[] args,
            CallbackInfoReturnable<MutableComponent> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!((Object) this instanceof Entity speaker)) {
            return;
        }

        String line = CapitalDialogueService.maybeFormatMcaPhraseLine(serverPlayer, speaker, phraseKey);
        if (line == null || line.isBlank()) {
            return;
        }

        cir.setReturnValue(Component.literal(line));
    }
}