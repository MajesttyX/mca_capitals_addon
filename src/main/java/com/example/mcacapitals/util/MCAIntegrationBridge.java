package com.example.mcacapitals.util;

import net.mca.entity.VillagerEntityMCA;
import net.mca.server.world.data.FamilyTree;
import net.mca.server.world.data.FamilyTreeNode;
import net.mca.server.world.data.Village;
import net.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MCAIntegrationBridge {

    private MCAIntegrationBridge() {
    }

    public static Entity getEntityByUuid(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return null;
        }

        for (Entity entity : level.getEntities().getAll()) {
            if (entityId.equals(entity.getUUID())) {
                return entity;
            }
        }

        return null;
    }

    public static boolean isMCAVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        return entity instanceof VillagerEntityMCA;
    }

    public static boolean isFemale(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (entity instanceof VillagerEntityMCA villager) {
            return villager.getGenetics().getGender().binary().getDataName().equalsIgnoreCase("female");
        }
        return false;
    }

    public static boolean hasFamilyNode(ServerLevel level, UUID entityId) {
        return getFamilyNode(level, entityId).isPresent();
    }

    public static UUID getSpouse(ServerLevel level, UUID entityId) {
        Optional<FamilyTreeNode> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID partner = nodeOpt.get().partner();
        return isNullUuid(partner) ? null : partner;
    }

    public static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        Optional<FamilyTreeNode> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UUID> result = new LinkedHashSet<>();
        for (UUID childId : nodeOpt.get().children()) {
            if (!isNullUuid(childId)) {
                result.add(childId);
            }
        }
        return result;
    }

    public static boolean isChildOf(ServerLevel level, UUID childId, UUID parentId) {
        Optional<FamilyTreeNode> childOpt = getFamilyNode(level, childId);
        if (childOpt.isEmpty() || parentId == null) {
            return false;
        }

        FamilyTreeNode child = childOpt.get();
        UUID father = child.father();
        UUID mother = child.mother();

        return parentId.equals(father) || parentId.equals(mother);
    }

    public static boolean isMCAGuard(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!(entity instanceof VillagerEntityMCA villager)) {
            return false;
        }

        String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
        return profession.contains("guard") || profession.contains("archer");
    }

    public static boolean isMasterProfessionVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!(entity instanceof VillagerEntityMCA villager)) {
            return false;
        }

        return villager.getVillagerData().getLevel() >= 5;
    }

    public static boolean isAliveAdultOrChildVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        return entity instanceof VillagerEntityMCA villager && villager.isAlive() && !villager.isRemoved();
    }

    public static String describeProfession(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (entity instanceof VillagerEntityMCA villager) {
            return villager.getVillagerData().getProfession().toString()
                    + "@"
                    + villager.getVillagerData().getLevel();
        }
        return "non_mca";
    }

    public static Set<Integer> getVillageIdsAtOrAbovePopulation(ServerLevel level, int requiredPopulation) {
        if (level == null) {
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        VillageManager manager = VillageManager.get(level);

        for (Village village : manager) {
            if (village.isVillage() && village.getPopulation() >= requiredPopulation) {
                result.add(village.getId());
            }
        }

        return result;
    }

    public static Set<Integer> getAllVillageIds(ServerLevel level) {
        if (level == null) {
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        VillageManager manager = VillageManager.get(level);

        for (Village village : manager) {
            result.add(village.getId());
        }

        return result;
    }

    public static Integer getVillageIdForResident(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return null;
        }

        VillageManager manager = VillageManager.get(level);

        for (Village village : manager) {
            if (village.getResidentsUUIDs().anyMatch(entityId::equals)) {
                return village.getId();
            }
        }

        return null;
    }

    public static boolean hasVillage(ServerLevel level, int villageId) {
        if (level == null) {
            return false;
        }

        return VillageManager.get(level).getOrEmpty(villageId).isPresent();
    }

    public static boolean isVillage(ServerLevel level, int villageId) {
        if (level == null) {
            return false;
        }

        return VillageManager.get(level)
                .getOrEmpty(villageId)
                .map(Village::isVillage)
                .orElse(false);
    }

    public static int getVillagePopulation(ServerLevel level, int villageId) {
        if (level == null) {
            return 0;
        }

        return VillageManager.get(level)
                .getOrEmpty(villageId)
                .map(Village::getPopulation)
                .orElse(0);
    }

    public static String getVillageName(ServerLevel level, Integer villageId) {
        if (level == null || villageId == null) {
            return "Unknown Village";
        }

        return VillageManager.get(level)
                .getOrEmpty(villageId)
                .map(Village::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Unknown Village");
    }

    public static BlockPos getVillageCenter(ServerLevel level, Integer villageId) {
        if (level == null || villageId == null) {
            return BlockPos.ZERO;
        }

        return VillageManager.get(level)
                .getOrEmpty(villageId)
                .map(Village::getCenter)
                .map(center -> {
                    Vec3i vec = center;
                    return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
                })
                .orElse(BlockPos.ZERO);
    }

    public static Set<UUID> getVillageResidents(ServerLevel level, int villageId) {
        if (level == null) {
            return Collections.emptySet();
        }

        Optional<Village> villageOpt = VillageManager.get(level).getOrEmpty(villageId);
        if (villageOpt.isEmpty()) {
            return Collections.emptySet();
        }

        return villageOpt.get()
                .getResidentsUUIDs()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String describeEntity(ServerLevel level, UUID entityId) {
        return "isMCA=" + isMCAVillager(level, entityId)
                + ", hasFamilyNode=" + hasFamilyNode(level, entityId)
                + ", isGuard=" + isMCAGuard(level, entityId)
                + ", isMaster=" + isMasterProfessionVillager(level, entityId)
                + ", isFemale=" + isFemale(level, entityId)
                + ", spouse=" + (getSpouse(level, entityId) == null ? "none" : getSpouse(level, entityId))
                + ", childCount=" + getChildren(level, entityId).size()
                + ", profession=" + describeProfession(level, entityId);
    }

    private static Optional<FamilyTreeNode> getFamilyNode(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return Optional.empty();
        }

        try {
            return FamilyTree.get(level).getOrEmpty(entityId);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static boolean isNullUuid(UUID uuid) {
        return uuid == null || new UUID(0L, 0L).equals(uuid);
    }
}