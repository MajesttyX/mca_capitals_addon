package com.majesttyx.mcacapitals.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DeclarationOfAbdicationItem extends Item {

    public DeclarationOfAbdicationItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide && !player.isShiftKeyDown()) {
            AbdicationClient.openScreen();
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}