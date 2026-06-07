package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalRoyalGuardService;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class RoyalGuardInteractionHandler {

    public static InteractionResult handleEntityInteract(Player player, Entity target, InteractionHand hand) {
        if (player == null || target == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        UUID targetId = target.getUUID();
        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            return InteractionResult.PASS;
        }

        CapitalRecord capital = CapitalManager.getCapitalForResident(targetId);
        if (capital == null || !capital.isRoyalGuard(targetId)) {
            return InteractionResult.PASS;
        }

        boolean allowed = player.hasPermissions(2) || player.getUUID().equals(capital.getSovereign());
        if (!allowed) {
            player.sendSystemMessage(Component.literal("Only the sovereign may command the royal guard."));
            return InteractionResult.FAIL;
        }

        boolean changed = CapitalRoyalGuardService.togglePatrol(level, capital, targetId);
        if (!changed) {
            return InteractionResult.PASS;
        }

        boolean patrolling = capital.getRoyalGuardPatrolling().contains(targetId);
        String displayName = CapitalRoyalGuardService.buildRoyalGuardDisplayName(level, capital, targetId);
        if (patrolling) {
            player.sendSystemMessage(Component.literal(displayName + " will now patrol this area."));
        } else {
            player.sendSystemMessage(Component.literal(displayName + " will now return to following the sovereign."));
        }

        CapitalDataAccess.markDirty(level);
        return InteractionResult.SUCCESS;
    }
}