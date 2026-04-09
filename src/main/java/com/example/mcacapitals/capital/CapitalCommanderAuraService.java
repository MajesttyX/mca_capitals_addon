package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;

final class CapitalCommanderAuraService {

    private static final int AURA_RADIUS = 16;
    private static final int REGEN_DURATION_TICKS = 20 * 180;
    private static final int RAID_BLESSING_COOLDOWN_TICKS = 20 * 60;

    private CapitalCommanderAuraService() {
    }

    static void tickCommanderAura(ServerLevel level, CapitalRecord capital, Entity commander) {
        long gameTime = level.getGameTime();
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);

        if (isRaidActive(level, capital)) {
            if (gameTime - capital.getLastCommanderRaidBlessingGameTime() >= RAID_BLESSING_COOLDOWN_TICKS) {
                applyCommanderBlessing(level, commander);
                capital.setLastCommanderRaidBlessingGameTime(gameTime);
                CapitalDataAccess.markDirty(level);
            }
            return;
        }

        if (capital.getLastCommanderRandomBlessingDay() >= currentDay) {
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay < 6000L || timeOfDay > 7000L) {
            return;
        }

        int roll = Math.floorMod((capital.getCapitalId().toString() + ":" + currentDay + ":commanderBlessing").hashCode(), 100);
        if (roll >= 8) {
            return;
        }

        applyCommanderBlessing(level, commander);
        capital.setLastCommanderRandomBlessingDay(currentDay);
        CapitalDataAccess.markDirty(level);
    }

    private static void applyCommanderBlessing(ServerLevel level, Entity commander) {
        AABB area = commander.getBoundingBox().inflate(AURA_RADIUS);

        for (Entity villager : MCAIntegrationBridge.getNearbyMCAVillagers(level, area)) {
            MCAIntegrationBridge.addEffect(
                    villager,
                    new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, 0, false, false)
            );
        }

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (!player.isSpectator()) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, 0, false, false));
            }
        }
    }

    private static boolean isRaidActive(ServerLevel level, CapitalRecord capital) {
        if (capital.getVillageId() == null) {
            return false;
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        Raid raid = level.getRaidAt(center);
        return raid != null && raid.isActive();
    }
}