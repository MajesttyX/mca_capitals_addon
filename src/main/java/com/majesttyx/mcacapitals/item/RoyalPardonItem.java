package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RoyalPardonItem extends Item {

    public RoyalPardonItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("mcacapitals.system.royal_pardon_item.royal_pardon");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_pardon_item.clears_a_warrant_detention_or_execution_mark").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.royal_pardon_item.only_a_sovereign_or_hand_may_issue_it").withStyle(ChatFormatting.GRAY));
    }
}