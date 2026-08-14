package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RoyalScepterItem extends Item {

    public RoyalScepterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("mcacapitals.system.royal_scepter_item.royal_scepter");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_scepter_item.a_tool_of_royal_authority_used_to_appoint_heirs_and_court_roles").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_scepter_item.right_click_within_a_capital_to_use").withStyle(ChatFormatting.GRAY));
    }
}