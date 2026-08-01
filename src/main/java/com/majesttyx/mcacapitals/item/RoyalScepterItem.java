package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RoyalScepterItem extends Item {

    public RoyalScepterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Royal Scepter");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("A tool of royal authority used to appoint heirs and court roles.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right-click within a capital to use.").withStyle(ChatFormatting.GRAY));
    }
}