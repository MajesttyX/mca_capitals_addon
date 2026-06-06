package com.majesttyx.mcacapitals.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class ModItemStackData {

    private ModItemStackData() {
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && !stack.getTag().isEmpty();
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag() || stack.getTag() == null) {
            return new CompoundTag();
        }

        return stack.getTag().copy();
    }

    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty() || tag == null) {
            return;
        }

        stack.setTag(tag.copy());
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty() || updater == null) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        updater.accept(tag);
        stack.setTag(tag);
    }
}