package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.ai.Messenger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CapitalAmbientDialogueHandler {

    private static final long SCAN_INTERVAL_TICKS = 20L * 10L;
    private static final long CAPITAL_COOLDOWN_TICKS = 20L * 15L;
    private static final long VILLAGER_COOLDOWN_TICKS = 20L * 180L;
    private static final int EVENING_START_TIME = 9000;
    private static final int EVENING_END_TIME = 12000;
    private static final int BELL_SEARCH_RADIUS = 72;
    private static final double BELL_RADIUS = 20.0D;
    private static final double PLAYER_HEAR_RADIUS = 24.0D;
    private static final double PLAYER_NEAR_BELL_RADIUS = 36.0D;

    private final Map<UUID, Long> nextCapitalChatterTick = new HashMap<>();
    private final Map<UUID, Long> nextVillagerChatterTick = new HashMap<>();
    private final Map<UUID, UUID> lastCapitalSpeaker = new HashMap<>();

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
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

        UUID capitalId = capital.getCapitalId();
        if (gameTime < nextCapitalChatterTick.getOrDefault(capitalId, 0L)) {
            return;
        }

        BlockPos villageCenter = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        if (villageCenter == null || villageCenter.equals(BlockPos.ZERO)) {
            return;
        }

        BlockPos meetingPoint = findMeetingPoint(level, villageCenter);
        ServerPlayer player = nearestPlayerNearBell(level, meetingPoint);
        if (player == null) {
            return;
        }

        Entity speaker = pickSpeakerNearBell(level, meetingPoint, capitalId, gameTime);
        if (speaker == null) {
            return;
        }

        String line = CapitalDialogueService.formatCapitalIdleEveningChatter(player, speaker, capital);
        if (line == null || line.isBlank()) {
            return;
        }

        if (speaker instanceof Messenger messenger) {
            messenger.playSpeechEffect();
        }

        sendToNearbyPlayers(level, speaker, line);
        nextCapitalChatterTick.put(capitalId, gameTime + CAPITAL_COOLDOWN_TICKS);
        nextVillagerChatterTick.put(speaker.getUUID(), gameTime + VILLAGER_COOLDOWN_TICKS);
        lastCapitalSpeaker.put(capitalId, speaker.getUUID());
    }

    private BlockPos findMeetingPoint(ServerLevel level, BlockPos villageCenter) {
        return level.getPoiManager()
                .findClosest(
                        holder -> holder.is(PoiTypes.MEETING),
                        villageCenter,
                        BELL_SEARCH_RADIUS,
                        PoiManager.Occupancy.ANY
                )
                .orElse(villageCenter);
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

    private Entity pickSpeakerNearBell(ServerLevel level, BlockPos center, UUID capitalId, long gameTime) {
        AABB area = new AABB(center).inflate(BELL_RADIUS, 8.0D, BELL_RADIUS);
        List<Entity> villagers = MCAIntegrationBridge.getNearbyMCAVillagers(level, area).stream()
                .filter(villager -> canParticipate(level, villager))
                .toList();

        if (villagers.isEmpty()) {
            return null;
        }

        UUID previousSpeaker = lastCapitalSpeaker.get(capitalId);

        List<Entity> preferred = villagers.stream()
                .filter(villager -> !villager.getUUID().equals(previousSpeaker))
                .filter(villager -> gameTime >= nextVillagerChatterTick.getOrDefault(villager.getUUID(), 0L))
                .toList();
        if (!preferred.isEmpty()) {
            return randomSpeaker(level, preferred);
        }

        List<Entity> rested = villagers.stream()
                .filter(villager -> gameTime >= nextVillagerChatterTick.getOrDefault(villager.getUUID(), 0L))
                .toList();
        if (!rested.isEmpty()) {
            return randomSpeaker(level, rested);
        }

        List<Entity> alternate = villagers.stream()
                .filter(villager -> !villager.getUUID().equals(previousSpeaker))
                .toList();
        if (!alternate.isEmpty()) {
            return randomSpeaker(level, alternate);
        }

        return randomSpeaker(level, villagers);
    }

    private Entity randomSpeaker(ServerLevel level, List<Entity> villagers) {
        return villagers.get(level.random.nextInt(villagers.size()));
    }

    private boolean canParticipate(ServerLevel level, Entity villager) {
        if (villager == null || !villager.isAlive()) {
            return false;
        }

        if (villager instanceof LivingEntity living && (living.isSleeping() || living.getHealth() <= 0.0F)) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, villager.getUUID());
        return !"BABY".equalsIgnoreCase(ageState) && !"TODDLER".equalsIgnoreCase(ageState);
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