package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class CapitalAmbassadorUrgentMatterHandler {

    private CapitalAmbassadorUrgentMatterHandler() {
    }

    public static InteractionResult handleEntityInteract(
            Player player,
            Entity target,
            InteractionHand hand
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || target == null
                || hand != InteractionHand.MAIN_HAND
                || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        return CapitalAmbassadorUrgentMatterService.openIfPresent(
                serverPlayer,
                target
        )
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }
}
