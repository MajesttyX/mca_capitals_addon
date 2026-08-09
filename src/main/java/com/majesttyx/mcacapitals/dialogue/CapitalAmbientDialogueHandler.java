package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CapitalAmbientDialogueHandler {

    private static final long SCAN_INTERVAL_TICKS = 20L * 10L;
    private static final long CAPITAL_COOLDOWN_TICKS = 20L * 75L;
    private static final long VILLAGER_COOLDOWN_TICKS = 20L * 180L;
    private static final int EVENING_START_TIME = 9500;
    private static final int EVENING_END_TIME = 12500;
    private static final int CHAT_CHANCE_PERCENT = 35;
    private static final double BELL_RADIUS = 18.0D;
    private static final double PLAYER_HEAR_RADIUS = 24.0D;
    private static final double PLAYER_NEAR_BELL_RADIUS = 40.0D;

    private final Map<UUID, Long> lastCapitalChatterTick = new HashMap<>();
    private final Map<UUID, Long> lastVillagerChatterTick = new HashMap<>();

    public void onLevelTick(ServerLevel level) {
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        int dayTime = (int) (level.getDayTime() % 24000L);
        if (dayTime < EVENING_START_TIME || dayTime > EVENING_END_TIME) {
            return;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            tickCapital(level, capital, gameTime);
        }
    }

    private void tickCapital(ServerLevel level, CapitalRecord capital, long gameTime) {
        if (capital == null || capital.getState() != CapitalState.ACTIVE || capital.getVillageId() == null) {
            return;
        }

        long lastCapitalTick = lastCapitalChatterTick.getOrDefault(capital.getCapitalId(), Long.MIN_VALUE);
        if (lastCapitalTick != Long.MIN_VALUE && gameTime - lastCapitalTick < CAPITAL_COOLDOWN_TICKS) {
            return;
        }

        if (level.random.nextInt(100) >= CHAT_CHANCE_PERCENT) {
            return;
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        if (center == null || center.equals(BlockPos.ZERO)) {
            return;
        }

        ServerPlayer player = nearestPlayerNearBell(level, center);
        if (player == null) {
            return;
        }

        Entity speaker = pickSpeakerNearBell(level, center, gameTime);
        if (speaker == null) {
            return;
        }

        String line = CapitalDialogueService.formatCapitalIdleEveningChatter(player, speaker, capital);
        if (line == null || line.isBlank()) {
            return;
        }

        sendToNearbyPlayers(level, speaker, line);
        lastCapitalChatterTick.put(capital.getCapitalId(), gameTime);
        lastVillagerChatterTick.put(speaker.getUUID(), gameTime);
    }

    private ServerPlayer nearestPlayerNearBell(ServerLevel level, BlockPos center) {
        return level.players().stream()
                .filter(player -> player.blockPosition().closerThan(center, PLAYER_NEAR_BELL_RADIUS))
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(
                        center.getX() + 0.5D,
                        center.getY() + 0.5D,
                        center.getZ() + 0.5D
                )))
                .orElse(null);
    }

    private Entity pickSpeakerNearBell(ServerLevel level, BlockPos center, long gameTime) {
        AABB area = new AABB(center).inflate(BELL_RADIUS, 8.0D, BELL_RADIUS);
        List<Entity> villagers = MCAIntegrationBridge.getNearbyMCAVillagers(level, area).stream()
                .filter(villager -> canSpeak(level, villager, gameTime))
                .toList();

        if (villagers.isEmpty()) {
            return null;
        }

        return villagers.get(level.random.nextInt(villagers.size()));
    }

    private boolean canSpeak(ServerLevel level, Entity villager, long gameTime) {
        if (villager == null || !villager.isAlive()) {
            return false;
        }

        if (villager instanceof LivingEntity living && (living.isSleeping() || living.getHealth() <= 0.0F)) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, villager.getUUID());
        if ("BABY".equalsIgnoreCase(ageState) || "TODDLER".equalsIgnoreCase(ageState)) {
            return false;
        }

        long lastVillagerTick = lastVillagerChatterTick.getOrDefault(villager.getUUID(), Long.MIN_VALUE);
        return lastVillagerTick == Long.MIN_VALUE || gameTime - lastVillagerTick >= VILLAGER_COOLDOWN_TICKS;
    }

    private void sendToNearbyPlayers(ServerLevel level, Entity speaker, String line) {
        Component message = Component.literal(speaker.getName().getString() + ": " + line);
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(speaker) <= PLAYER_HEAR_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
    }
}