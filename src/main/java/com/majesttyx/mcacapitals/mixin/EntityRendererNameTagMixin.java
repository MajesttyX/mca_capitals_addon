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
            method = "render",
            at = @At("TAIL")
    )
    private void mcacapitals$renderCapitalNameTagFromEntityRender(
            Entity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        CapitalNameTagHandler.renderCustomNameTag(entity, entity.getDisplayName(), poseStack, bufferSource, packedLight, partialTick);
    }

    @Inject(
            method = "renderNameTag",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mcacapitals$cancelVanillaCapitalNameTag(
            Entity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick,
            CallbackInfo ci
    ) {
        if (CapitalNameTagHandler.shouldUseCustomNameTag(entity)) {
            ci.cancel();
        }
    }
}