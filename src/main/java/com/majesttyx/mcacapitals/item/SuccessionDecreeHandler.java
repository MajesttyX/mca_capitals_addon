package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class SuccessionDecreeHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (!held.is(ModItems.BLANK_SUCCESSION_DECREE.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity livingTarget)) {
            return;
        }

        boolean validTargetType = event.getTarget() instanceof Player
                || MCAIntegrationBridge.isMCAVillagerEntity(livingTarget);

        if (!validTargetType) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!SuccessionDecreeItem.isBound(held)) {
            if (!player.level().isClientSide) {
                player.sendSystemMessage(Component.literal("Bind the Succession Decree to a capital before using it."));
            }
            return;
        }

        if (!player.level().isClientSide) {
            return;
        }

        UUID capitalId = SuccessionDecreeItem.getBoundCapitalId(held);
        if (capitalId == null) {
            return;
        }

        SuccessionDecreeClient.openScreen(
                capitalId,
                SuccessionDecreeItem.getBoundCapitalName(held),
                livingTarget.getUUID(),
                livingTarget.getName().getString()
        );
    }
}