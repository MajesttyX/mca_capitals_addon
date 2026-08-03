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

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && !customData.copyTag().isEmpty();
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }

        return customData.copyTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty() || tag == null) {
            return;
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty() || updater == null) {
            return;
        }

        CompoundTag tag = getCustomData(stack);
        updater.accept(tag);
        setCustomData(stack, tag);
    }
}