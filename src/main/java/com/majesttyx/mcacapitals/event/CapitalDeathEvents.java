package com.majesttyx.mcacapitals.event;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalDeathTransitionService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalSuccessionService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.CapitalHardcoreDeathTracker;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "mcacapitals")
public final class CapitalDeathEvents {

    private CapitalDeathEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            handleHardcorePlayerSovereignDeath(serverLevel, player);
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        recordExecutionIfNeeded(serverLevel, entity, event.getSource());
        CapitalDeathTransitionService.handleVillagerDeath(serverLevel, entity.getUUID());
    }


    private static void handleHardcorePlayerSovereignDeath(ServerLevel deathLevel, ServerPlayer player) {
        if (deathLevel == null
                || player == null
                || deathLevel.getServer() == null
                || !deathLevel.getServer().isHardcore()) {
            return;
        }

        UUID playerId = player.getUUID();
        CapitalHardcoreDeathTracker.record(deathLevel, playerId);

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null
                    || !capital.isPlayerSovereign()
                    || capital.getPlayerSovereignId() == null
                    || !playerId.equals(capital.getPlayerSovereignId())) {
                continue;
            }

            ServerLevel capitalLevel = CapitalManager.getCapitalLevel(deathLevel.getServer(), capital);
            if (capitalLevel == null) {
                continue;
            }

            if (CapitalSuccessionService.handleSuccessionIfNeeded(capitalLevel, capital)) {
                CapitalDataAccess.markDirty(capitalLevel);
            }
        }
    }

    private static void recordExecutionIfNeeded(ServerLevel level, Entity villager, DamageSource source) {
        if (level == null || villager == null || !MCAExecutionBridge.isMarkedForExecution(level, villager.getUUID())) {
            return;
        }

        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return;
        }

        String villagerName = CapitalNameService.resolveDisplayName(level, capital, villager.getUUID());
        String executionerName = resolveExecutionerName(source);

        if (executionerName == null || executionerName.isBlank()) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.EXECUTED_CONDEMNED,
                    villagerName
            );
            return;
        }

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.EXECUTED_BY_CROWN,
                villagerName,
                executionerName
        );
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.containsEntity(villagerId)) {
                return capital;
            }
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(level, villageId);
    }

    private static String resolveExecutionerName(DamageSource source) {
        if (source == null || source.getEntity() == null) {
            return null;
        }

        return source.getEntity().getName().getString();
    }
}