package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.access.McaCapitalsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements McaCapitalsPersistentDataHolder {

    @Unique
    private static final String MCACAPITALS_PERSISTENT_DATA_KEY = "McaCapitalsPersistentData";

    @Unique
    private CompoundTag mcacapitals$persistentData;

    @Override
    public CompoundTag mcacapitals$getPersistentData() {
        if (mcacapitals$persistentData == null) {
            mcacapitals$persistentData = new CompoundTag();
        }
        return mcacapitals$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void mcacapitals$writePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag target = cir.getReturnValue();
        if (target == null) {
            target = tag;
        }

        if (target != null && mcacapitals$persistentData != null && !mcacapitals$persistentData.isEmpty()) {
            target.put(MCACAPITALS_PERSISTENT_DATA_KEY, mcacapitals$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void mcacapitals$readPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag != null && tag.contains(MCACAPITALS_PERSISTENT_DATA_KEY)) {
            mcacapitals$persistentData = tag.getCompound(MCACAPITALS_PERSISTENT_DATA_KEY).copy();
        } else {
            mcacapitals$persistentData = new CompoundTag();
        }
    }
}