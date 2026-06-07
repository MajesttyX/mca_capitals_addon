package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.util.MCANameCultureFilter;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Pseudo
@Mixin(targets = "net.mca.resources.Names", remap = false)
public class NamesRegionFilterMixin {

    @ModifyVariable(
            method = "pickCitizenName",
            at = @At(value = "STORE"),
            ordinal = 0,
            remap = false
    )
    private static List<String> mcacapitals$filterConfiguredRegions(List<String> regions, ServerLevel level, @Coerce Object gender) {
        return MCANameCultureFilter.filterConfiguredRegions(regions);
    }
}