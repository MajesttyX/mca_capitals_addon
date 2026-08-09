package com.majesttyx.mcacapitals.mixin;

import fabric.net.mca.resources.Rank;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fabric.net.mca.advancement.criterion.RankCriterion", remap = false)
public abstract class RankCriterionSuppressionMixin {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$suppressMcaRankAdvancements(ServerPlayer player, Rank rank, CallbackInfo ci) {
        ci.cancel();
    }
}
