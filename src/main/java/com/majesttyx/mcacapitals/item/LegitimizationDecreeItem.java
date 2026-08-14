package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LegitimizationDecreeItem extends Item {

    public LegitimizationDecreeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.legitimization_decree_item.restores_or_grants_dynastic_legitimacy_to_a_royal_child").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.legitimization_decree_item.shift_right_click_a_valid_target").withStyle(ChatFormatting.GRAY));
    }
}