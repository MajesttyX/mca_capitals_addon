package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BetrothalDecreeItem extends Item {

    public BetrothalDecreeItem() {
        super(new Item.Properties().stacksTo(2));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Used to arrange a future marriage between two villagers.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Requires two eligible villagers to be beside each other.").withStyle(ChatFormatting.GRAY));
    }
}