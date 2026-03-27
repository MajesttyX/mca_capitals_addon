package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.UUID;

public class RoyalGuardInteractionHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.isEmpty()) return;
        if (!player.isShiftKeyDown()) return;

        Entity target = event.getTarget();
        UUID targetId = target.getUUID();
        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) return;

        CapitalRecord capital = CapitalManager.getAllCapitals().values().stream()
                .filter(c -> c.isRoyalGuard(targetId))
                .findFirst()
                .orElse(null);
        if (capital == null) return;

        boolean allowed = player.hasPermissions(2) || player.getUUID().equals(capital.getSovereign());
        if (!allowed) {
            player.sendSystemMessage(Component.literal("Only the sovereign may command the royal guard."));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        boolean changed = CapitalRoyalGuardService.togglePatrol(level, capital, targetId);
        if (!changed) return;

        boolean patrolling = capital.getRoyalGuardPatrolling().contains(targetId);
        String displayName = CapitalRoyalGuardService.buildRoyalGuardDisplayName(level, capital, targetId);
        if (patrolling) {
            player.sendSystemMessage(Component.literal(displayName + " will now patrol this area."));
        } else {
            player.sendSystemMessage(Component.literal(displayName + " will now return to following the sovereign."));
        }

        CapitalDataAccess.markDirty(level);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
