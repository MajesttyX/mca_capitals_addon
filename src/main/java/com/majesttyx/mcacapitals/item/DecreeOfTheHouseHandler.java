package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DecreeOfTheHouseHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleInteraction(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleInteraction(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private boolean handleInteraction(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || player.level().isClientSide) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!(rawTarget instanceof LivingEntity target)) {
            return false;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.DECREE_OF_THE_HOUSE.get())) {
            return false;
        }

        if (!player.isShiftKeyDown()) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_handler.a_decree_of_the_house_can_only_be_used_on_an_mca_villager"));
            return true;
        }

        OpenDecreeOfTheHousePacket packet = DecreeOfTheHouseService.createOpenPacket(level, target);
        if (packet == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_handler.the_records_for_that_villager_could_not_be_opened"));
            return true;
        }

        ModNetwork.sendToPlayer(serverPlayer, packet);
        return true;
    }
}