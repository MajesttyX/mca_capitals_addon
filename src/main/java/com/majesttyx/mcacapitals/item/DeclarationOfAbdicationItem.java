package com.majesttyx.mcacapitals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class DeclarationOfAbdicationItem extends Item {

    public DeclarationOfAbdicationItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide && !player.isShiftKeyDown()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AbdicationClient.openScreen());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("A formal declaration used to relinquish the crown.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right-click while standing in the capital.").withStyle(ChatFormatting.GRAY));
    }
}