package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class CapitalAmbassadorUrgentMatterHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (handle(
                event.getEntity() instanceof ServerPlayer player
                        ? player
                        : null,
                event.getTarget(),
                event.getHand()
        )) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(
            PlayerInteractEvent.EntityInteractSpecific event
    ) {
        if (handle(
                event.getEntity() instanceof ServerPlayer player
                        ? player
                        : null,
                event.getTarget(),
                event.getHand()
        )) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private boolean handle(
            ServerPlayer player,
            Entity target,
            InteractionHand hand
    ) {
        if (player == null
                || player.level().isClientSide
                || hand != InteractionHand.MAIN_HAND
                || player.isShiftKeyDown()) {
            return false;
        }

        return CapitalAmbassadorUrgentMatterService.openIfPresent(
                player,
                target
        );
    }
}