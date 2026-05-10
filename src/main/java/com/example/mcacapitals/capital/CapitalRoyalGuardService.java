package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalRoyalGuardService {

    public static final int REQUIRED_POPULATION = 25;
    public static final int MAX_ROYAL_GUARDS = 3;
    public static final int PATROL_RADIUS = 3;

    private static final double FOLLOW_START_DISTANCE = 4.5D;
    private static final double MAX_IDLE_DISTANCE = 14.0D;
    private static final double WALK_FOLLOW_SPEED = 1.1D;
    private static final double SPRINT_FOLLOW_SPEED = 1.0D;
    private static final double STATIONARY_CATCHUP_SPEED = 1.0D;

    private CapitalRoyalGuardService() {
    }

    public static boolean tickRoyalGuards(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        boolean changed = false;

        changed |= handleSovereignChange(level, capital);

        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            if (!isValidRoyalGuard(level, capital, guardId, residents)) {
                capital.removeRoyalGuard(guardId);
                changed = true;
            }
        }

        if (capital.getSovereign() == null) {
            if (changed) {
                CapitalNameService.refreshCapitalNames(level, capital, residents);
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            }
            return changed;
        }

        if (capital.getRoyalGuardLiege() == null) {
            capital.setRoyalGuardLiege(capital.getSovereign());
            changed = true;
        }

        if (MCAIntegrationBridge.isMCAVillager(level, capital.getSovereign())) {
            while (capital.getRoyalGuards().size() < MAX_ROYAL_GUARDS && isEligibleForNewRoyalGuard(level, capital)) {
                UUID candidate = findBestCandidate(level, capital, residents);
                if (candidate == null) break;
                appointRoyalGuard(level, capital, candidate);
                changed = true;
            }
        } else if (capital.getRoyalGuards().size() < MAX_ROYAL_GUARDS && isEligibleForNewRoyalGuard(level, capital)) {
            maybePromptPlayerSovereign(level, capital, residents);
        }

        tickBehaviors(level, capital);

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean clearRoyalGuardsForTransfer(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return false;
        }

        if (capital.getRoyalGuards().isEmpty() && capital.getRoyalGuardLiege() == null) {
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean changed = false;

        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            String guardName = buildRoyalGuardHistoryName(level, guardId);
            capital.removeRoyalGuard(guardId);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    guardName + " was released from the royal guard of " + villageName + " after the transfer of power."
            );
            changed = true;
        }

        capital.setRoyalGuardLiege(capital.getSovereign());
        capital.setLastRoyalGuardPromptDay(0L);

        if (changed) {
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static List<UUID> getValidCandidates(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        List<UUID> result = new ArrayList<>();
        for (UUID residentId : residents) {
            if (!isCandidate(level, capital, residentId)) continue;
            result.add(residentId);
        }
        result.sort(Comparator.comparing(UUID::toString));
        return result;
    }

    public static boolean appointRoyalGuard(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (villagerId == null || capital == null || level == null) return false;
        if (!isCandidate(level, capital, villagerId)) return false;
        if (capital.getRoyalGuards().size() >= MAX_ROYAL_GUARDS) return false;
        if (!isEligibleForNewRoyalGuard(level, capital)) return false;

        if (capital.getRoyalGuardLiege() == null) {
            capital.setRoyalGuardLiege(capital.getSovereign());
        }

        capital.addRoyalGuard(villagerId, MCAIntegrationBridge.isFemale(level, villagerId), capital.getRoyalGuardLiege());
        capital.setRoyalGuardDutyMode(villagerId, CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN);

        String guardName = buildRoyalGuardDisplayName(level, capital, villagerId);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CapitalChronicleService.addEntry(level, capital,
                guardName + " was named to the royal guard of " + villageName + ".");

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    public static boolean togglePatrol(ServerLevel level, CapitalRecord capital, UUID guardId) {
        if (level == null || capital == null || guardId == null) {
            return false;
        }
        if (!capital.getRoyalGuards().contains(guardId)) {
            return false;
        }

        CapitalRecord.GuardDutyMode current = capital.getRoyalGuardDutyMode(guardId);
        CapitalRecord.GuardDutyMode next =
                current == CapitalRecord.GuardDutyMode.PATROL_ANCHOR
                        ? CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN
                        : CapitalRecord.GuardDutyMode.PATROL_ANCHOR;

        capital.setRoyalGuardDutyMode(guardId, next);
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static boolean handleSovereignChange(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        UUID liege = capital.getRoyalGuardLiege();

        if (sovereign == null) {
            if (liege != null || !capital.getRoyalGuards().isEmpty()) {
                for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
                    recordDisgrace(level, capital, guardId);
                    capital.disgraceRoyalGuard(guardId);
                }
                capital.setRoyalGuardLiege(null);
                return true;
            }
            return false;
        }

        if (liege == null) {
            capital.setRoyalGuardLiege(sovereign);
            return true;
        }

        if (!liege.equals(sovereign)) {
            for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
                recordDisgrace(level, capital, guardId);
                capital.disgraceRoyalGuard(guardId);
            }
            capital.setRoyalGuardLiege(sovereign);
            return true;
        }

        return false;
    }

    private static void recordDisgrace(ServerLevel level, CapitalRecord capital, UUID guardId) {
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String name = buildRoyalGuardHistoryName(level, guardId);
        CapitalChronicleService.addEntry(
                level,
                capital,
                name + " was disgraced after failing to preserve the reign of " + villageName + "."
        );
    }

    public static String buildRoyalGuardDisplayName(ServerLevel level, CapitalRecord capital, UUID guardId) {
        String title = MCAIntegrationBridge.isFemale(level, guardId) ? "Dame" : "Sir";
        String baseName = resolveBaseName(level, guardId);
        return title + " " + baseName + " of the " + (capital.isSovereignFemale() ? "Queensguard" : "Kingsguard");
    }

    private static String buildRoyalGuardHistoryName(ServerLevel level, UUID guardId) {
        String title = MCAIntegrationBridge.isFemale(level, guardId) ? "Dame" : "Sir";
        String baseName = resolveBaseName(level, guardId);
        return title + " " + baseName;
    }

    private static String resolveBaseName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity == null) {
            return entityId.toString();
        }
        String currentName = entity.getName().getString();
        currentName = currentName.replace(" of the Kingsguard", "").replace(" of the Queensguard", "").trim();

        String[] prefixes = {
                "High Queen ", "High King ",
                "Dowager Queen ", "Dowager King ",
                "Queen Consort ", "King Consort ",
                "Heir Apparent ", "Crown Princess ", "Crown Prince ",
                "Dowager Princess ", "Dowager Prince ",
                "Princess Consort ", "Prince Consort ",
                "Hand of the Queen ", "Hand of the King ",
                "Grand Maester ", "Maester ", "Court Herald ",
                "Princess ", "Prince ",
                "Lord Commander ",
                "Dowager Duchess ", "Dowager Duke ",
                "Duchess ", "Duke ",
                "Lady ", "Lord ",
                "Dame ", "Sir ",
                "Queen ", "King "
        };

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String prefix : prefixes) {
                if (currentName.startsWith(prefix)) {
                    currentName = currentName.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return currentName.isBlank() ? entityId.toString() : currentName;
    }

    private static boolean isValidRoyalGuard(ServerLevel level, CapitalRecord capital, UUID villagerId, Set<UUID> residents) {
        if (villagerId == null || capital == null || level == null) return false;
        if (capital.getSovereign() == null) return false;
        if (!MCAIntegrationBridge.isAliveMCAVillager(level, villagerId)) return false;
        if (!MCAIntegrationBridge.isMCAGuard(level, villagerId)) return false;
        if (villagerId.equals(capital.getSovereign())) return false;
        return capital.getRoyalGuards().contains(villagerId);
    }

    private static boolean isEligibleForNewRoyalGuard(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) return false;
        return MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId()) >= REQUIRED_POPULATION;
    }

    private static UUID findBestCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        List<UUID> valid = getValidCandidates(level, capital, residents);
        if (valid.isEmpty()) return null;
        return valid.get(0);
    }

    private static boolean isCandidate(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (villagerId == null || capital == null || level == null) return false;
        if (capital.getRoyalGuards().contains(villagerId)) return false;
        if (villagerId.equals(capital.getSovereign())) return false;
        if (villagerId.equals(capital.getCommander())) return false;
        if (villagerId.equals(capital.getHand())) return false;
        if (villagerId.equals(capital.getGrandMaester())) return false;
        if (villagerId.equals(capital.getHerald())) return false;
        if (villagerId.equals(capital.getHeir())) return false;
        if (villagerId.equals(capital.getConsort())) return false;
        if (villagerId.equals(capital.getDowager())) return false;
        if (capital.isRoyalChild(villagerId)) return false;
        if (capital.isLegitimizedRoyalChild(villagerId)) return false;
        if (capital.isPrinceConsort(villagerId)) return false;
        if (capital.isDowagerPrince(villagerId)) return false;
        if (capital.isDuke(villagerId) || capital.isMarriageDuke(villagerId) || capital.isDowagerDuke(villagerId)) return false;
        if (capital.isLord(villagerId)) return false;
        if (!MCAIntegrationBridge.isAliveMCAVillager(level, villagerId)) return false;
        return MCAIntegrationBridge.isMCAGuard(level, villagerId);
    }

    private static void maybePromptPlayerSovereign(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        if (capital.getLastRoyalGuardPromptDay() == currentDay) {
            return;
        }

        List<UUID> candidates = getValidCandidates(level, capital, residents);
        if (candidates.isEmpty()) {
            return;
        }

        UUID playerId = capital.getPlayerSovereignId();
        if (playerId == null) {
            return;
        }

        ServerPlayer sovereign = level.getServer().getPlayerList().getPlayer(playerId);
        if (sovereign == null) {
            return;
        }

        capital.setLastRoyalGuardPromptDay(currentDay);
        CapitalDataAccess.markDirty(level);

        sovereign.sendSystemMessage(Component.literal(
                "Your capital can appoint up to " + MAX_ROYAL_GUARDS + " royal guards. "
                        + "Use /capitaltest court or the Royal Scepter to appoint an eligible guard."
        ));
    }

    private static void tickBehaviors(ServerLevel level, CapitalRecord capital) {
        UUID sovereignId = capital.getSovereign();
        if (sovereignId == null) {
            return;
        }

        Entity sovereign = MCAIntegrationBridge.getEntityByUuid(level, sovereignId);
        if (sovereign == null) {
            return;
        }

        for (UUID guardId : capital.getRoyalGuards()) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, guardId);
            if (!(entity instanceof Mob mob)) {
                continue;
            }

            if (mob.isSleeping()) {
                stopNavigation(mob);
                continue;
            }

            if (capital.getRoyalGuardDutyMode(guardId) == CapitalRecord.GuardDutyMode.PATROL_ANCHOR) {
                stopNavigation(mob);
                continue;
            }

            BlockPos sovereignPos = sovereign.blockPosition();
            double distance = mob.distanceToSqr(sovereign);

            if (distance > MAX_IDLE_DISTANCE * MAX_IDLE_DISTANCE) {
                navigateToEntity(mob, sovereign, SPRINT_FOLLOW_SPEED);
                continue;
            }

            if (distance > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE) {
                double speed = sovereign.isSprinting() ? SPRINT_FOLLOW_SPEED : WALK_FOLLOW_SPEED;
                navigateToEntity(mob, sovereign, speed);
                continue;
            }

            BlockPos idle = findIdlePointNear(level, sovereignPos, mob.getUUID());
            if (idle != null && mob.distanceToSqr(idle.getX() + 0.5D, idle.getY(), idle.getZ() + 0.5D) > 3.0D) {
                navigateToBlock(mob, idle, STATIONARY_CATCHUP_SPEED);
            } else {
                stopNavigation(mob);
            }
        }
    }

    private static BlockPos findIdlePointNear(ServerLevel level, BlockPos center, UUID seedId) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -PATROL_RADIUS; dx <= PATROL_RADIUS; dx++) {
            for (int dz = -PATROL_RADIUS; dz <= PATROL_RADIUS; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                    positions.add(pos);
                }
            }
        }

        if (positions.isEmpty()) {
            return center;
        }

        positions.sort(Comparator.comparing(BlockPos::asLong));
        int index = Math.floorMod((seedId.toString() + ":" + center.asLong()).hashCode(), positions.size());
        return positions.get(index);
    }

    private static void navigateToEntity(Mob mob, Entity target, double speed) {
        mob.getNavigation().moveTo(target, speed);
        faceTargetIfPossible(mob, target);
    }

    private static void navigateToBlock(Mob mob, BlockPos pos, double speed) {
        mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, speed);
    }

    private static void stopNavigation(Mob mob) {
        mob.getNavigation().stop();
    }

    private static void faceTargetIfPossible(Mob mob, Entity target) {
        try {
            Method lookAt = mob.getClass().getMethod("lookAt", Entity.class, float.class, float.class);
            lookAt.invoke(mob, target, 30.0F, 30.0F);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}