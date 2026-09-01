package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
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

public class LegitimizationDecreeHandler {


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
        if (!held.is(ModItems.LEGITIMIZATION_DECREE.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.legitimization_may_only_be_granted_to_an_mca_villager"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_villager_has_no_claim_tied_to_any_capital"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (capital.getSovereign() == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_capital_has_no_sovereign_to_grant_legitimacy"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getConsort())
                || targetId.equals(capital.getDowager())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_title_cannot_be_granted_through_legitimization"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!isEligibleDynasticChild(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_villager_is_not_recognized_as_a_child_of_this_dynasty"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        boolean female = MCAIntegrationBridge.isFemale(level, targetId);
        capital.addLegitimizedRoyalChild(targetId, female);

        if (!capital.getRoyalSuccessionOrder().contains(targetId)) {
            capital.getRoyalSuccessionOrder().add(targetId);
        }

        CapitalFoundationService.refreshCourt(level, capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String displayName = CapitalChronicleIdentitySnapshot.name(level, capital, targetId);
        Component title = Component.translatable(
                female
                        ? "mcacapitals.dynamic.title.royal_child.female"
                        : "mcacapitals.dynamic.title.royal_child.male"
        );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.LEGITIMIZED,
                displayName,
                CapitalChronicleIdentitySnapshot.title(level, capital, targetId),
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );

        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.legitimization_decree_handler.success",
                displayNameComponent(displayName),
                title
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

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital.isRoyalChild(targetId)
                    || capital.isLegitimizedRoyalChild(targetId)
                    || MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())
                    || (capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager()))) {
                return capital;
            }
        }

        return null;
    }

    private boolean isEligibleDynasticChild(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (capital == null || capital.getSovereign() == null || targetId == null) {
            return false;
        }

        if (capital.isRoyalChild(targetId) || capital.isLegitimizedRoyalChild(targetId)) {
            return true;
        }

        if (MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())) {
            return true;
        }

        return capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager());
    }

    private Component displayNameComponent(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }
        return Component.literal(displayName);
    }


}