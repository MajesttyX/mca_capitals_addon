package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalSovereignDeclarationPromptService;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Replaces the normal Talk question with the allegiance prompt after MCA has
 * completed its own dialogue initialization.
 *
 * Fabric MCA 7.7.32 requires the original initialization path to run so the
 * active InteractScreen is ready to receive dialogue answers. Cancelling the
 * method at HEAD left the player with an empty dialogue panel.
 */
@Pseudo
@Mixin(
        targets = "net.conczin.mca.network.c2s.InteractionDialogueInitMessage",
        remap = false
)
public abstract class InteractionDialogueInitMessageMixin {

    @Shadow
    public abstract UUID villagerUUID();

    @Inject(
            method = "handleServer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void mcacapitals$promptUndeclaredPlayerAfterDialogueInitialization(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(villagerUUID());
        if (!(entity instanceof VillagerEntityMCA villager)
                || !CapitalSovereignDeclarationPromptService.shouldPrompt(player, villager)) {
            return;
        }

        CapitalSovereignDeclarationPromptService.openPrompt(player, villager);
    }
}
