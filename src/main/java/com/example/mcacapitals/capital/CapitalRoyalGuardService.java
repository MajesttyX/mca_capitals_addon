package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalRoyalGuardService {

    public static final int REQUIRED_POPULATION = 35;
    public static final int MAX_ROYAL_GUARDS = 3;
    public static final int PATROL_RADIUS = 3;

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

        capital.addRoyalGuard(villagerId, MCAIntegrationBridge.isFemale(level, villagerId), capital.getSovereign());
        String guardName = buildRoyalGuardDisplayName(level, capital, villagerId);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CapitalChronicleService.addEntry(level, capital,
                guardName + " was named to the royal guard of " + villageName + ".");

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(
                    guardName + " has been named to the royal guard of " + villageName + "."
            ));
        }

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
        Entity sovereignEntity = MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());

        int slot = 0;
        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            if (guardId.equals(capital.getDowager()) || guardId.equals(capital.getConsort())) {
                capital.removeRoyalGuard(guardId);
                continue;
            }

            Entity guard = MCAIntegrationBridge.getEntityByUuid(level, guardId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(guard)) continue;

            if (capital.getRoyalGuardPatrolling().contains(guardId)) {
                BlockPos anchor = capital.getRoyalGuardPatrolAnchors().getOrDefault(guardId, guard.blockPosition());
                if (guard.distanceToSqr(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D) > 100.0D || level.random.nextInt(10) == 0) {
                    int dx = level.random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
                    int dz = level.random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
                    MCAIntegrationBridge.moveTo(guard, anchor.getX() + 0.5D + dx, anchor.getY(), anchor.getZ() + 0.5D + dz, 0.9D);
                }
                continue;
            }

            if (sovereignEntity == null) continue;
            if (sovereignEntity.distanceToSqr(guard) > 256.0D) continue;

            double x = sovereignEntity.getX();
            double y = sovereignEntity.getY();
            double z = sovereignEntity.getZ();

            if (sovereignEntity instanceof LivingEntity living && living.isSleeping()) {
                double angle = (Math.PI * 2.0D / Math.max(1, capital.getRoyalGuards().size())) * slot++;
                double targetX = x + Math.cos(angle) * 2.5D;
                double targetZ = z + Math.sin(angle) * 2.5D;
                MCAIntegrationBridge.moveTo(guard, targetX, y, targetZ, 0.9D);
                continue;
            }

            double angle = (Math.PI * 2.0D / Math.max(1, capital.getRoyalGuards().size())) * slot++;
            double targetX = x + Math.cos(angle) * 1.8D;
            double targetZ = z + Math.sin(angle) * 1.8D;

            if (guard.distanceToSqr(targetX, y, targetZ) > 4.0D) {
                MCAIntegrationBridge.moveTo(guard, targetX, y, targetZ, 1.1D);
            }
        }
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
        if (guardId == null || residents == null || !residents.contains(guardId)) return false;
        if (guardId.equals(capital.getSovereign())) return false;
        if (guardId.equals(capital.getConsort())) return false;
        if (guardId.equals(capital.getDowager())) return false;
        if (guardId.equals(capital.getCommander())) return false;

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, guardId);
        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)
                && MCAIntegrationBridge.isMCAFootGuard(level, guardId)
                && !capital.isDisgracedRoyalGuard(guardId);
    }

    private static void maybePromptPlayerSovereign(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        Entity sovereignEntity = MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
        if (!(sovereignEntity instanceof ServerPlayer player)) return;

        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        if (capital.getLastRoyalGuardPromptDay() >= currentDay) return;

        List<UUID> candidates = getValidCandidates(level, capital, residents);
        if (candidates.isEmpty()) return;

        player.sendSystemMessage(Component.literal(
                "Valid royal guard candidates for " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                        + " are listed below. Use /capitalguard appoint <uuid> to choose one."
        ));
        for (UUID candidate : candidates) {
            String name = buildRoyalGuardDisplayName(level, capital, candidate);
            player.sendSystemMessage(Component.literal("- " + name + " [" + candidate + "]"));
        }

        capital.setLastRoyalGuardPromptDay(currentDay);
        CapitalDataAccess.markDirty(level);
    }

    private static void recordDisgrace(ServerLevel level, CapitalRecord capital, UUID guardId) {
        String guardName = buildRoyalGuardDisplayName(level, capital, guardId);
        CapitalChronicleService.addEntry(level, capital,
                guardName + " was disgraced and stripped of royal guard honors after the fall of their sovereign.");
    }

    public static String buildRoyalGuardDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        baseName = baseName.replace(" of the Kingsguard", "").replace(" of the Queensguard", "");
        for (String prefix : new String[]{"Queen Dowager ", "Prince Consort ", "Queen Consort ", "King Consort ", "Heir Apparent ", "Princess ", "Prince ", "Duchess ", "Duke ", "Lady ", "Lord ", "Dame ", "Sir ", "Queen ", "King "}) {
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