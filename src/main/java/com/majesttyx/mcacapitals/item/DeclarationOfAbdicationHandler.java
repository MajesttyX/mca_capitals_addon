package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class DeclarationOfAbdicationHandler {


    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity livingTarget)) {
            return;
        }

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.DECLARATION_OF_ABDICATION.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.the_declaration_of_abdication_can_only_be_used_on_an_mca_sovereign"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.that_villager_is_not_part_of_a_capital"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!targetId.equals(capital.getSovereign())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.only_the_current_sovereign_may_abdicate_the_throne"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        boolean changed = CapitalFoundationService.abdicateSovereign(level, capital);
        if (!changed) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.there_is_no_valid_successor_to_receive_the_throne"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        String displayName = CapitalChronicleIdentitySnapshot.name(level, capital, targetId);
        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.declaration_of_abdication_handler.success",
                displayNameComponent(displayName)
        ));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        if (villageId != null) {
            CapitalRecord byVillage = CapitalManager.getCapitalByVillageId(level, villageId);
            if (byVillage != null) {
                return byVillage;
            }
        }

        CapitalRecord bySovereign = CapitalManager.getCapitalBySovereign(targetId);
        if (bySovereign != null) {
            return bySovereign;
        }

        return null;
    }

    private Component displayNameComponent(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }
        return Component.literal(displayName);
    }


}