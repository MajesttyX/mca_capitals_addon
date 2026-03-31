package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalCommanderService {

    public static final int REQUIRED_POPULATION = 30;
    private static final int AURA_RADIUS = 16;
    private static final int REGEN_DURATION_TICKS = 20 * 180;
    private static final int RAID_BLESSING_COOLDOWN_TICKS = 20 * 60;

    private CapitalCommanderService() {
    }

    public static boolean tickCommander(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null) {
            return false;
        }

        boolean changed = false;

        UUID previousCommander = capital.getCommander();
        if (!isValidCommander(level, capital, previousCommander, residents)) {
            if (previousCommander != null) {
                capital.setCommander(null);
                capital.setCommanderFemale(false);
                changed = true;
            }
        }

        if (capital.getCommander() == null && isEligibleForNewCommander(level, capital)) {
            UUID newCommander = findBestCommanderCandidate(level, capital, residents);
            if (newCommander != null) {
                capital.setCommander(newCommander);
                capital.setCommanderFemale(MCAIntegrationBridge.isFemale(level, newCommander));

                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
                String commanderName = resolveDisplayName(level, capital, newCommander);

                CapitalChronicleService.addEntry(level, capital,
                        commanderName + " was appointed Commander of the Royal Guard of " + villageName + ".");

                broadcastCommanderAppointment(level, capital, villageName, commanderName);
                changed = true;
            }
        }

        if (previousCommander != null && capital.getCommander() == null) {
            CapitalChronicleService.addEntry(level, capital,
                    "The office of Commander of the Royal Guard stands vacant in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        if (capital.getCommander() != null) {
            tickCommanderAura(level, capital);
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    private static void tickCommanderAura(ServerLevel level, CapitalRecord capital) {
        UUID commanderId = capital.getCommander();
        Entity commander = MCAIntegrationBridge.getEntityByUuid(level, commanderId);
        if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(commander)) {
            return;
        }

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
            MCAIntegrationBridge.addEffect(villager,
                    new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, 0, false, false));
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

    private static boolean isEligibleForNewCommander(ServerLevel level, CapitalRecord capital) {
        if (capital.getVillageId() == null) {
            return false;
        }
        return MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId()) >= REQUIRED_POPULATION;
    }

    private static UUID findBestCommanderCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        BlockPos center = capital.getVillageId() != null
                ? MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId())
                : BlockPos.ZERO;

        List<UUID> candidates = new ArrayList<>();

        for (UUID residentId : residents) {
            if (!MCAIntegrationBridge.isMCAGuard(level, residentId)) {
                continue;
            }
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, residentId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)) {
                continue;
            }
            candidates.add(residentId);
        }

        candidates.sort(Comparator
                .comparingDouble((UUID id) -> {
                    Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
                    return entity == null ? Double.MAX_VALUE : entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
                })
                .thenComparing(UUID::toString));

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean isValidCommander(ServerLevel level, CapitalRecord capital, UUID commanderId, Set<UUID> residents) {
        if (commanderId == null) {
            return false;
        }

        if (residents != null && !residents.contains(commanderId)) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, commanderId);
        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)
                && MCAIntegrationBridge.isMCAGuard(level, commanderId);
    }

    private static void broadcastCommanderAppointment(ServerLevel level, CapitalRecord capital, String villageName, String commanderName) {
        Component message = Component.literal(
                "Due to their unwavering commitment to " + villageName
                        + " and years of service to the crown, "
                        + commanderName
                        + " has been appointed as Commander of the Royal Guard!"
        );

        CapitalPlayerNotificationService.notifyPlayersInCapital(level, capital, message);
    }

    private static String resolveDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        String strippedName = stripKnownTitles(baseName);

        String title = CapitalTitleResolver.getDisplayTitle(level, capital, entityId);
        if (title == null || title.isBlank() || "Commoner".equalsIgnoreCase(title) || "None".equalsIgnoreCase(title)) {
            return strippedName;
        }

        return title + " " + strippedName;
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();
        String[] knownTitles = {
                "Queen Dowager",
                "Prince Consort",
                "Queen Consort",
                "King Consort",
                "Heir Apparent",
                "Commander",
                "Princess",
                "Prince",
                "Duchess",
                "Duke",
                "Lady",
                "Lord",
                "Dame",
                "Sir",
                "Queen",
                "King"
        };

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String title : knownTitles) {
                String prefix = title + " ";
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return result.isBlank() ? "Unnamed" : result;
    }
}