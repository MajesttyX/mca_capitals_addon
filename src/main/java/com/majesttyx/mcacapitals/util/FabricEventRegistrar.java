package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.dialogue.CapitalAmbientDialogueHandler;
import com.majesttyx.mcacapitals.event.CapitalDeathEvents;
import com.majesttyx.mcacapitals.identity.VillagerIdentityTrackingSyncHandler;
import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.majesttyx.mcacapitals.item.DecreeOfTheHouseHandler;
import com.majesttyx.mcacapitals.item.LegitimizationDecreeHandler;
import com.majesttyx.mcacapitals.item.RoyalDisinheritanceHandler;
import com.majesttyx.mcacapitals.item.RoyalScepterHandler;
import com.majesttyx.mcacapitals.item.SuccessionDecreeHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class FabricEventRegistrar {

    private FabricEventRegistrar() {
    }

    public static void register() {
        CapitalLifecycleHandler lifecycleHandler = new CapitalLifecycleHandler();
        CapitalPopulationScanner populationScanner = new CapitalPopulationScanner();
        CapitalAmbientDialogueHandler ambientDialogueHandler = new CapitalAmbientDialogueHandler();
        BetrothalDecreeHandler betrothalDecreeHandler = new BetrothalDecreeHandler();
        SovereignMarriageCaptureHandler sovereignMarriageCaptureHandler = new SovereignMarriageCaptureHandler();

        ServerLifecycleEvents.SERVER_STARTED.register(lifecycleHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            lifecycleHandler.onServerStopped(server);
            sovereignMarriageCaptureHandler.clear();
        });

        ServerTickEvents.END_WORLD_TICK.register(serverLevel -> {
            populationScanner.onLevelTick(serverLevel);
            ambientDialogueHandler.onLevelTick(serverLevel);
            betrothalDecreeHandler.onLevelTick(serverLevel);

            for (ServerPlayer player : serverLevel.players()) {
                VillagerIdentityTrackingSyncHandler.onPlayerTick(player);
                sovereignMarriageCaptureHandler.onPlayerTick(player);
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) ->
                VillagerIdentityTrackingSyncHandler.onEntityJoinLevel(entity, serverLevel)
        );

        EntityTrackingEvents.START_TRACKING.register(VillagerIdentityTrackingSyncHandler::onStartTracking);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
                CapitalDeathEvents.onLivingDeath(entity)
        );

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            InteractionResult result = DecreeOfTheHouseHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = RoyalScepterHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = SuccessionDecreeHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = DeclarationOfAbdicationHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = LegitimizationDecreeHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = RoyalDisinheritanceHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            result = RoyalGuardInteractionHandler.handleEntityInteract(player, entity, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }

            return InteractionResult.PASS;
        });
    }
}