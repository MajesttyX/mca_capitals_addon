package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class LegitimizationDecreeItem extends Item {

    public LegitimizationDecreeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Restores or grants dynastic legitimacy to a royal child.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Shift-right-click a valid target.").withStyle(ChatFormatting.GRAY));
    }
}