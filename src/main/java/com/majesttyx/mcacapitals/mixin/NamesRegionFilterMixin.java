package com.majesttyx.mcacapitals.mixin;

import com.google.gson.JsonElement;
import com.majesttyx.mcacapitals.util.MCANameCultureFilter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = "fabric.net.conczin.mca.resources.Names", remap = false)
public class NamesRegionFilterMixin {

    @Inject(
            method = "apply",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$filterConfiguredRegions(
            Map<ResourceLocation, JsonElement> prepared,
            ResourceManager manager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        MCANameCultureFilter.applyConfiguredCultureFilter();
    }
}