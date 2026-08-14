package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RoyalDisinheritanceItem extends Item {

    public RoyalDisinheritanceItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_disinheritance_item.removes_a_royal_child_from_the_line_of_succession").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_disinheritance_item.shift_right_click_an_eligible_royal_child").withStyle(ChatFormatting.GRAY));
    }
}