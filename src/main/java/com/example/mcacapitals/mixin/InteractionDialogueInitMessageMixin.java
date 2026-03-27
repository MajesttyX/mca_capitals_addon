package com.example.mcacapitals.mixin;

import com.example.mcacapitals.dialogue.CapitalDialogueService;
import net.mca.entity.VillagerEntityMCA;
import net.mca.network.c2s.InteractionDialogueInitMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InteractionDialogueInitMessage.class)
public class InteractionDialogueInitMessageMixin {

    @Shadow(remap = false)
    @Final
    private UUID villagerUUID;

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$injectCapitalNews(ServerPlayer player, CallbackInfo ci) {
        Entity entity = player.serverLevel().getEntity(this.villagerUUID);
        if (!(entity instanceof VillagerEntityMCA villager)) {
            return;
        }

        if (CapitalDialogueService.tryOpenCapitalNewsDialogue(player, villager)) {
            ci.cancel();
        }
    }
}