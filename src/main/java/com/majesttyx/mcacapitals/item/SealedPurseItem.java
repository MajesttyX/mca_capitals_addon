package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SealedPurseItem extends Item {

    public SealedPurseItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("mcacapitals.system.sealed_purse_item.sealed_purse");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("mcacapitals.system.sealed_purse_item.gift_to_the_master_of_laws_to_bury_one_active_case").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("mcacapitals.system.sealed_purse_item.success_depends_on_their_friendship_with_you").withStyle(ChatFormatting.GRAY));
    }
}
