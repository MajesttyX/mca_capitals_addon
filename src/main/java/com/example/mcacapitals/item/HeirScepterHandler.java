package com.example.mcacapitals.item;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;

public class HeirScepterHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (!held.is(ModItems.HEIR_SCEPTER.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity livingTarget)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(livingTarget)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!player.level().isClientSide) {
            return;
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                RoyalScepterClient.openScreen(livingTarget.getUUID(), livingTarget.getName().getString())
        );
    }
}