package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalSovereignDeclarationPromptService;
import fabric.net.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(
        targets = "fabric.net.mca.network.c2s.InteractionDialogueInitMessage",
        remap = false
)
public abstract class InteractionDialogueInitMessageMixin {

    @Shadow(remap = false)
    @Final
    private UUID villagerUUID;

    @Inject(
            method = "receive",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mcacapitals$promptUndeclaredPlayerAfterDialogueInitialization(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(villagerUUID);
        if (!(entity instanceof VillagerEntityMCA villager)
                || !CapitalSovereignDeclarationPromptService.shouldPrompt(player, villager)) {
            return;
        }

        CapitalSovereignDeclarationPromptService.openPrompt(player, villager);
    }
}
