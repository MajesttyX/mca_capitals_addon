package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterHandler;
import com.majesttyx.mcacapitals.capital.CapitalCampaignCombatDamageHandler;
import com.majesttyx.mcacapitals.capital.CapitalCampaignCombatTickHandler;
import com.majesttyx.mcacapitals.capital.CapitalCampaignProcessor;
import com.majesttyx.mcacapitals.capital.CapitalCampaignCasualtyService;
import com.majesttyx.mcacapitals.capital.CapitalCampaignHornRetreatHandler;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementProcessor;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticShipmentProcessor;
import com.majesttyx.mcacapitals.capital.CapitalExileDiscoveryHandler;
import com.majesttyx.mcacapitals.capital.CapitalForeignStorageRaidService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerLegalHandler;
import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.capital.CapitalPrisonerHandler;
import com.majesttyx.mcacapitals.capital.CapitalTradeExchangeProcessor;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionProcessor;
import com.majesttyx.mcacapitals.dialogue.CapitalAmbientDialogueHandler;
import com.majesttyx.mcacapitals.event.CapitalDeathEvents;
import com.majesttyx.mcacapitals.identity.VillagerIdentityTrackingSyncHandler;
import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.majesttyx.mcacapitals.item.DecreeOfTheHouseHandler;
import com.majesttyx.mcacapitals.item.LegitimizationDecreeHandler;
import com.majesttyx.mcacapitals.item.RoyalDisinheritanceHandler;
import com.majesttyx.mcacapitals.item.RoyalPardonHandler;
import com.majesttyx.mcacapitals.item.RoyalScepterHandler;
import com.majesttyx.mcacapitals.item.SuccessionDecreeHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

public final class FabricEventRegistrar {

    private FabricEventRegistrar() {
    }

    public static void register() {
        CapitalLifecycleHandler lifecycleHandler =
                new CapitalLifecycleHandler();

        CapitalPopulationScanner populationScanner =
                new CapitalPopulationScanner();

        CapitalExileDiscoveryHandler exileDiscoveryHandler =
                new CapitalExileDiscoveryHandler();

        CapitalPrisonerHandler prisonerHandler =
                new CapitalPrisonerHandler();

        CapitalPlayerLegalHandler playerLegalHandler =
                new CapitalPlayerLegalHandler();

        CapitalAmbientDialogueHandler ambientDialogueHandler =
                new CapitalAmbientDialogueHandler();

        BetrothalDecreeHandler betrothalDecreeHandler =
                new BetrothalDecreeHandler();

        SovereignMarriageCaptureHandler sovereignMarriageCaptureHandler =
                new SovereignMarriageCaptureHandler();

        ServerLifecycleEvents.SERVER_STARTED.register(
                lifecycleHandler::onServerStarted
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {
                    lifecycleHandler.onServerStopped(
                            server
                    );

                    sovereignMarriageCaptureHandler.clear();
                }
        );

        ServerTickEvents.END_WORLD_TICK.register(
                serverLevel -> {
                    populationScanner.onLevelTick(
                            serverLevel
                    );

                    CapitalCampaignProcessor.onLevelTick(
                            serverLevel
                    );

                    CapitalCampaignCombatTickHandler.onLevelTick(
                            serverLevel
                    );

                    CapitalCampaignCasualtyService.onLevelTick(
                            serverLevel
                    );

                    CapitalWartimeSuccessionProcessor.onLevelTick(
                            serverLevel
                    );

                    CapitalDiplomaticShipmentProcessor.onLevelTick(
                            serverLevel
                    );

                    CapitalDiplomaticAgreementProcessor.onLevelTick(
                            serverLevel
                    );

                    CapitalTradeExchangeProcessor.onLevelTick(
                            serverLevel
                    );

                    exileDiscoveryHandler.onLevelTick(
                            serverLevel
                    );

                    prisonerHandler.onLevelTick(
                            serverLevel
                    );

                    ambientDialogueHandler.onLevelTick(
                            serverLevel
                    );

                    betrothalDecreeHandler.onLevelTick(
                            serverLevel
                    );

                    for (ServerPlayer player :
                            serverLevel.players()) {
                        VillagerIdentityTrackingSyncHandler.onPlayerTick(
                                player
                        );

                        sovereignMarriageCaptureHandler.onPlayerTick(
                                player
                        );

                        playerLegalHandler.onPlayerTick(
                                player
                        );

                        CapitalForeignStorageRaidService.refreshIncident(
                                player
                        );
                    }
                }
        );

        ServerEntityEvents.ENTITY_LOAD.register(
                (entity, serverLevel) ->
                        VillagerIdentityTrackingSyncHandler.onEntityJoinLevel(
                                entity,
                                serverLevel
                        )
        );

        EntityTrackingEvents.START_TRACKING.register(
                VillagerIdentityTrackingSyncHandler::onStartTracking
        );

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                CapitalCampaignCombatDamageHandler::allowDamage
        );

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    CapitalCampaignCasualtyService.onLivingDeath(
                            entity
                    );

                    CapitalDeathEvents.onLivingDeath(
                            entity,
                            damageSource
                    );
                }
        );

        UseItemCallback.EVENT.register(
                (player, level, hand) -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        CapitalCampaignHornRetreatHandler.onUseItem(
                                serverPlayer,
                                hand
                        );
                    }

                    return InteractionResultHolder.pass(
                            player.getItemInHand(
                                    hand
                            )
                    );
                }
        );

        UseEntityCallback.EVENT.register(
                (player, level, hand, entity, hitResult) -> {
                    InteractionResult result =
                            CapitalAmbassadorUrgentMatterHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            DecreeOfTheHouseHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            RoyalScepterHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            SuccessionDecreeHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            DeclarationOfAbdicationHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            LegitimizationDecreeHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            RoyalDisinheritanceHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    result =
                            RoyalPardonHandler.handleEntityInteract(
                                    player,
                                    entity,
                                    hand
                            );

                    if (result != InteractionResult.PASS) {
                        return result;
                    }

                    return InteractionResult.PASS;
                }
        );
    }
}