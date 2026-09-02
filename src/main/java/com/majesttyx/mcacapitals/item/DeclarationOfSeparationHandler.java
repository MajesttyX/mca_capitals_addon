package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import com.majesttyx.mcacapitals.house.CapitalHouseRecord;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDeclarationOfSeparationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DeclarationOfSeparationHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handle(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handle(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private boolean handle(
            Player player,
            Entity rawTarget,
            net.minecraft.world.InteractionHand hand
    ) {
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
        if (!held.is(ModItems.HOUSE_LEDGER.get())) {
            return false;
        }

        if (!player.isShiftKeyDown()) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(
                    Component.translatable(
                            "mcacapitals.system.declaration_of_separation.only_mca"
                    )
            );
            return true;
        }

        CapitalRecord capital =
                CapitalTitleResolver.findCapitalForEntity(
                        level,
                        target.getUUID()
                );

        CapitalHouseRecord currentHouse =
                capital == null
                        || capital.getCapitalId() == null
                        ? null
                        : CapitalHouseDataAccess.findHouseForMember(
                                level,
                                capital.getCapitalId(),
                                target.getUUID()
                        );

        if (capital == null
                || !DeclarationOfSeparationService.isEligibleFounder(
                        level,
                        capital,
                        currentHouse,
                        target.getUUID()
                )) {
            player.sendSystemMessage(
                    Component.translatable(
                            "mcacapitals.system.declaration_of_separation.not_eligible"
                    )
            );
            return true;
        }

        VillagerIdentityService.ensureAssigned(level, target);
        VillagerIdentityData identity =
                VillagerIdentityService.getIdentity(target);

        if (identity == null || !identity.hasFoundedHouse()) {
            player.sendSystemMessage(
                    Component.translatable(
                            "mcacapitals.system.declaration_of_separation.no_current_house"
                    )
            );
            return true;
        }

        ModNetwork.sendToPlayer(
                serverPlayer,
                new OpenDeclarationOfSeparationPacket(
                        target.getUUID(),
                        target.getName().getString(),
                        identity.houseName(),
                        identity.houseWords()
                )
        );

        return true;
    }
}
