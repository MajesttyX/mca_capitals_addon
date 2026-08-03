package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class DecreeOfTheHouseItem extends Item {

    public DecreeOfTheHouseItem() {
        super(new Item.Properties().durability(5));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            DecreeOfTheHouseService.openPlayerHouseEditor(serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context == null || context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            DecreeOfTheHouseService.openPlayerHouseEditor(serverPlayer);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Used to revise the recorded surname or House name of the target.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right-click to revise your own House name and House Words.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Shift-right-click a villager to edit their surname and House Words if applicable.").withStyle(ChatFormatting.GRAY));
    }
}