package com.majesttyx.mcacapitals.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

public final class ModItemStackData {

    private ModItemStackData() {
    }

    public static boolean hasCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.copyTag().isEmpty();
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty() || tag == null) {
            return;
        }

        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty() || updater == null) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
    }
}