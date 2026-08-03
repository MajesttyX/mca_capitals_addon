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
        return Component.literal("Royal Pardon");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(Component.literal(
                "Clears a warrant, detention, or execution mark."
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(
                "Only a Sovereign, Hand, or Lord Commander may issue it."
        ).withStyle(ChatFormatting.GRAY));
    }
}
