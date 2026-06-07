package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.access.McaCapitalsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class EntityPersistentData {

    private EntityPersistentData() {
    }

    public static CompoundTag get(Entity entity) {
        if (entity instanceof McaCapitalsPersistentDataHolder holder) {
            return holder.mcacapitals$getPersistentData();
        }

        return new CompoundTag();
    }
}