package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BetrothalDecreeItem extends Item {

    public BetrothalDecreeItem() {
        super(new Item.Properties().stacksTo(2));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.betrothal_decree_item.used_to_arrange_a_future_marriage_between_two_villagers").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.betrothal_decree_item.requires_two_eligible_villagers_to_be_beside_each_other").withStyle(ChatFormatting.GRAY));
    }
}