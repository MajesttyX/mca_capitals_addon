package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SealedPurseItem extends Item {

    public SealedPurseItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Sealed Purse");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(Component.literal(
                "Gift to the Master of Laws to bury one active case."
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(
                "Success depends on their friendship with you."
        ).withStyle(ChatFormatting.GRAY));
    }
}
