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

        CapitalNameService.refreshCapitalNames(level, capital, CapitalResidentScanner.scanResidents(level, capital.getCapitalId()));
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
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

    private static void tickBehaviors(ServerLevel level, CapitalRecord capital) {
        Entity sovereignEntity = resolveSovereignEntity(level, capital);
        if (sovereignEntity == null) {
            return;
        }

        double sovereignMoveSqr = sovereignEntity.getDeltaMovement().horizontalDistanceSqr();
        boolean sovereignStanding = sovereignMoveSqr < 0.0009D;
        boolean sovereignSprinting = sovereignEntity.isSprinting() || sovereignMoveSqr > 0.012D;

        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            if (guardId.equals(capital.getDowager()) || guardId.equals(capital.getConsort())) {
                capital.removeRoyalGuard(guardId);
                continue;
            }

            Entity guard = MCAIntegrationBridge.getEntityByUuid(level, guardId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(guard)) {
                continue;
            }

            if (capital.getRoyalGuardPatrolling().contains(guardId)) {
                BlockPos anchor = capital.getRoyalGuardPatrolAnchors().getOrDefault(guardId, guard.blockPosition());
                if (guard.distanceToSqr(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D) > 100.0D || level.random.nextInt(10) == 0) {
                    int dx = level.random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
                    int dz = level.random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
                    MCAIntegrationBridge.moveTo(guard, anchor.getX() + 0.5D + dx, anchor.getY(), anchor.getZ() + 0.5D + dz, 0.9D);
                }
                continue;
            }

            if (isInNativeMcaStayState(guard)) {
                stopMovement(guard);
                continue;
            }

            double distanceToSovereignSqr = sovereignEntity.distanceToSqr(guard);

            if (sovereignStanding) {
                if (distanceToSovereignSqr <= MAX_IDLE_DISTANCE * MAX_IDLE_DISTANCE) {
                    stopMovement(guard);
                } else {
                    MCAIntegrationBridge.moveTo(
                            guard,
                            sovereignEntity.getX(),
                            sovereignEntity.getY(),
                            sovereignEntity.getZ(),
                            STATIONARY_CATCHUP_SPEED
                    );
                }
                continue;
            }

            if (distanceToSovereignSqr <= FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE) {
                stopMovement(guard);
                continue;
            }

            double speed = sovereignSprinting ? SPRINT_FOLLOW_SPEED : WALK_FOLLOW_SPEED;
            MCAIntegrationBridge.moveTo(
                    guard,
                    sovereignEntity.getX(),
                    sovereignEntity.getY(),
                    sovereignEntity.getZ(),
                    speed
            );
        }
    }

    private static boolean isInNativeMcaStayState(Entity entity) {
        if (entity == null) {
            return false;
        }

        try {
            Method getVillagerBrain = entity.getClass().getMethod("getVillagerBrain");
            Object brain = getVillagerBrain.invoke(entity);
            if (brain == null) {
                return false;
            }

            for (Method method : brain.getClass().getMethods()) {
                if (!method.getName().equals("getMoveState") || method.getParameterCount() != 0) {
                    continue;
                }

                Object moveState = method.invoke(brain);
                if (moveState instanceof Enum<?> stateEnum) {
                    return "STAY".equals(stateEnum.name());
                }
                if (moveState != null) {
                    return "STAY".equalsIgnoreCase(String.valueOf(moveState));
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static void stopMovement(Entity entity) {
        if (entity == null) {
            return;
        }

        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            return;
        }

        try {
            Method getNavigation = entity.getClass().getMethod("getNavigation");
            Object navigation = getNavigation.invoke(entity);
            if (navigation != null) {
                Method stop = navigation.getClass().getMethod("stop");
                stop.invoke(navigation);
            }
        } catch (Exception ignored) {
        }
    }

    private static Entity resolveSovereignEntity(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return null;
        }

        Entity sovereignEntity = MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
        if (sovereignEntity != null) {
            return sovereignEntity;
        }

        return level.getServer().getPlayerList().getPlayer(capital.getSovereign());
    }

    public static boolean togglePatrol(ServerLevel level, CapitalRecord capital, UUID guardId) {
        if (!capital.isRoyalGuard(guardId)) return false;
        if (guardId.equals(capital.getDowager()) || guardId.equals(capital.getConsort())) return false;

        if (capital.getRoyalGuardPatrolling().contains(guardId)) {
            capital.getRoyalGuardPatrolling().remove(guardId);
            capital.getRoyalGuardPatrolAnchors().remove(guardId);
        } else {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, guardId);
            BlockPos anchor = entity != null ? entity.blockPosition() : BlockPos.ZERO;
            capital.getRoyalGuardPatrolling().add(guardId);
            capital.getRoyalGuardPatrolAnchors().put(guardId, anchor);
        }
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static boolean isEligibleForNewRoyalGuard(ServerLevel level, CapitalRecord capital) {
        return capital.getVillageId() != null
                && MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId()) >= REQUIRED_POPULATION
                && capital.getRoyalGuards().size() < MAX_ROYAL_GUARDS;
    }

    private static boolean isCandidate(ServerLevel level, CapitalRecord capital, UUID residentId) {
        if (residentId == null) return false;
        if (capital.isRoyalGuard(residentId) || capital.isDisgracedRoyalGuard(residentId)) return false;
        if (!MCAIntegrationBridge.isMCAFootGuard(level, residentId)) return false;
        if (residentId.equals(capital.getSovereign())) return false;
        if (residentId.equals(capital.getConsort())) return false;
        if (residentId.equals(capital.getDowager())) return false;
        if (residentId.equals(capital.getCommander())) return false;
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, residentId);
        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity);
    }

    private static UUID findBestCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        return getValidCandidates(level, capital, residents).stream().findFirst().orElse(null);
    }

    private static boolean isValidRoyalGuard(ServerLevel level, CapitalRecord capital, UUID guardId, Set<UUID> residents) {
        if (guardId == null) return false;
        if (guardId.equals(capital.getSovereign())) return false;
        if (guardId.equals(capital.getConsort())) return false;
        if (guardId.equals(capital.getDowager())) return false;
        if (guardId.equals(capital.getCommander())) return false;
        if (capital.isDisgracedRoyalGuard(guardId)) return false;

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, guardId);
        if (entity == null) {
            return true;
        }

        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)
                && MCAIntegrationBridge.isMCAFootGuard(level, guardId);
    }

    private static void maybePromptPlayerSovereign(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        Entity sovereignEntity = resolveSovereignEntity(level, capital);
        if (!(sovereignEntity instanceof ServerPlayer player)) return;

        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        if (capital.getLastRoyalGuardPromptDay() >= currentDay) return;

        List<UUID> candidates = getValidCandidates(level, capital, residents);
        if (candidates.isEmpty()) return;

        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) return;

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String guardName = capital.isSovereignFemale() ? "Queensguard" : "Kingsguard";

        player.sendSystemMessage(Component.literal(
                "As sovereign of " + villageName + ", you may now appoint loyal defenders to your " + guardName + "."
        ));

        capital.setLastRoyalGuardPromptDay(currentDay);
        CapitalDataAccess.markDirty(level);
    }

    private static void recordDisgrace(ServerLevel level, CapitalRecord capital, UUID guardId) {
        String guardName = buildRoyalGuardDisplayName(level, capital, guardId);
        CapitalChronicleService.addEntry(level, capital,
                guardName + " was disgraced and stripped of royal guard honors after the fall of their sovereign.");
    }

    private static String buildRoyalGuardHistoryName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        int stateIndex = baseName.indexOf(" (");
        if (stateIndex >= 0) {
            baseName = baseName.substring(0, stateIndex).trim();
        }
        baseName = baseName.replace(" of the Kingsguard", "").replace(" of the Queensguard", "");
        return baseName.trim();
    }

    public static String buildRoyalGuardDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        int stateIndex = baseName.indexOf(" (");
        if (stateIndex >= 0) {
            baseName = baseName.substring(0, stateIndex).trim();
        }
        baseName = baseName.replace(" of the Kingsguard", "").replace(" of the Queensguard", "");
        for (String prefix : new String[]{
                "High Queen ",
                "High King ",
                "Dowager Queen ",
                "Dowager King ",
                "Queen Consort ",
                "King Consort ",
                "Heir Apparent ",
                "Crown Princess ",
                "Crown Prince ",
                "Princess Consort ",
                "Prince Consort ",
                "Hand of the Queen ",
                "Hand of the King ",
                "Princess ",
                "Prince ",
                "Duchess ",
                "Duke ",
                "Commander ",
                "Lady ",
                "Lord ",
                "Dame ",
                "Sir ",
                "Queen ",
                "King "
        }) {
            if (baseName.startsWith(prefix)) {
                baseName = baseName.substring(prefix.length()).trim();
                break;
            }
        }
        String honorific = MCAIntegrationBridge.isFemale(level, entityId) ? "Dame " : "Sir ";
        String suffix = capital.isSovereignFemale() ? " of the Queensguard" : " of the Kingsguard";
        return honorific + baseName + suffix;
    }
}