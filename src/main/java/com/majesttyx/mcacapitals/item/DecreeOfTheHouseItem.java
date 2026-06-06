package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DecreeOfTheHouseItem extends Item {

    public DecreeOfTheHouseItem() {
        super(new Item.Properties().durability(5));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Used to revise a villager's recorded surname or House name.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Shift-right-click a villager to edit their surname and House Words if applicable.").withStyle(ChatFormatting.GRAY));
    }
}