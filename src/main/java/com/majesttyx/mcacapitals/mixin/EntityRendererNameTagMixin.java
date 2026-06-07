package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.client.CapitalNameTagHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin {

    @Inject(
            method = "renderNameTag",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mcacapitals$renderCustomCapitalNameTag(
            Entity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        if (CapitalNameTagHandler.renderCustomNameTag(entity, displayName, poseStack, bufferSource, packedLight)) {
            ci.cancel();
        }
    }
}