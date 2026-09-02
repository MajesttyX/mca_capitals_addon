package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MCAFamilyBridge {

    private MCAFamilyBridge() {
    }

    static boolean hasPersistentFamilyNode(ServerLevel level, UUID entityId) {
        return getFamilyNode(level, entityId).isPresent();
    }

    static boolean hasFamilyNode(ServerLevel level, UUID entityId) {
        return hasPersistentFamilyNode(level, entityId);
    }

    static boolean isFamilyNodeDeceased(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return false;
        }

        Object value = MCAReflectionHelper.invoke(nodeOpt.get(), "isDeceased");
        return value instanceof Boolean b && b;
    }

    static Optional<Boolean> getFemaleIfKnown(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return Optional.empty();
        }

        Object gender = MCAReflectionHelper.invoke(nodeOpt.get(), "gender");
        if (gender == null) {
            return Optional.empty();
        }

        Object binary = MCAReflectionHelper.invoke(gender, "binary");
        Object resolvedGender = binary == null ? gender : binary;
        Object dataName = MCAReflectionHelper.invoke(resolvedGender, "getDataName");
        if (!(dataName instanceof String value)) {
            return Optional.empty();
        }

        if (value.equalsIgnoreCase("female")) {
            return Optional.of(true);
        }
        if (value.equalsIgnoreCase("male")) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    public static boolean isPlayerFamilyNode(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return false;
        }

        Object value = MCAReflectionHelper.invoke(nodeOpt.get(), "isPlayer");
        return value instanceof Boolean b && b;
    }

    static UUID getSpouse(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID partner = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(nodeOpt.get(), "partner"));
        return MCAReflectionHelper.isNullUuid(partner) ? null : partner;
    }

    static String getFamilyNodeName(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        Object value = MCAReflectionHelper.invoke(nodeOpt.get(), "getName");
        if (value instanceof String name && !name.isBlank()) {
            return name;
        }
        return null;
    }

    static boolean isPersistentlyMarried(ServerLevel level, UUID firstId, UUID secondId) {
        if (level == null || firstId == null || secondId == null) {
            return false;
        }

        Optional<Object> firstNode = getFamilyNode(level, firstId);
        if (firstNode.isEmpty()) {
            return false;
        }

        UUID firstPartner = MCAReflectionHelper.asUuid(
                MCAReflectionHelper.invoke(firstNode.get(), "partner")
        );
        if (!secondId.equals(firstPartner)) {
            return false;
        }

        if (!isMarriedRelationshipState(firstNode.get())) {
            return false;
        }

        Optional<Object> secondNode = getFamilyNode(level, secondId);
        if (secondNode.isEmpty()) {
            return true;
        }

        UUID secondPartner = MCAReflectionHelper.asUuid(
                MCAReflectionHelper.invoke(secondNode.get(), "partner")
        );
        if (secondPartner != null && !MCAReflectionHelper.isNullUuid(secondPartner) && !firstId.equals(secondPartner)) {
            return false;
        }

        return true;
    }

    private static boolean isMarriedRelationshipState(Object node) {
        Object state = MCAReflectionHelper.invoke(node, "getRelationshipState");
        if (state == null) {
            return false;
        }

        Object married = MCAReflectionHelper.invoke(state, "isMarried");
        if (married instanceof Boolean value) {
            return value;
        }

        String name = state instanceof Enum<?> e ? e.name() : String.valueOf(state);
        return name.startsWith("MARRIED_");
    }

    static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return Collections.emptySet();
        }

        return MCAReflectionHelper.extractUuidSet(MCAReflectionHelper.invoke(nodeOpt.get(), "children"));
    }

    static UUID getFather(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID father = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(nodeOpt.get(), "father"));
        return MCAReflectionHelper.isNullUuid(father) ? null : father;
    }

    static UUID getMother(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID mother = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(nodeOpt.get(), "mother"));
        return MCAReflectionHelper.isNullUuid(mother) ? null : mother;
    }

    static Set<UUID> getParents(ServerLevel level, UUID entityId) {
        Set<UUID> parents = new HashSet<>();
        UUID father = getFather(level, entityId);
        UUID mother = getMother(level, entityId);

        if (father != null) {
            parents.add(father);
        }
        if (mother != null) {
            parents.add(mother);
        }

        return parents;
    }

    static boolean isChildOf(ServerLevel level, UUID childId, UUID parentId) {
        Optional<Object> childOpt = getFamilyNode(level, childId);
        if (childOpt.isEmpty() || parentId == null) {
            return false;
        }

        Object child = childOpt.get();
        UUID father = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(child, "father"));
        UUID mother = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(child, "mother"));

        return parentId.equals(father) || parentId.equals(mother);
    }

    static boolean areSiblings(ServerLevel level, UUID firstId, UUID secondId) {
        if (level == null || firstId == null || secondId == null || firstId.equals(secondId)) {
            return false;
        }

        Set<UUID> firstParents = getParents(level, firstId);
        Set<UUID> secondParents = getParents(level, secondId);

        if (firstParents.isEmpty() || secondParents.isEmpty()) {
            return false;
        }

        for (UUID parent : firstParents) {
            if (secondParents.contains(parent)) {
                return true;
            }
        }

        return false;
    }

    static boolean isGrandparentOf(ServerLevel level, UUID possibleGrandparent, UUID possibleGrandchild) {
        if (level == null || possibleGrandparent == null || possibleGrandchild == null) {
            return false;
        }

        for (UUID parent : getParents(level, possibleGrandchild)) {
            if (isChildOf(level, parent, possibleGrandparent)) {
                return true;
            }
        }

        return false;
    }

    static boolean isAuntOrUncleOf(ServerLevel level, UUID possibleAuntOrUncle, UUID possibleNieceOrNephew) {
        if (level == null || possibleAuntOrUncle == null || possibleNieceOrNephew == null) {
            return false;
        }

        for (UUID parent : getParents(level, possibleNieceOrNephew)) {
            if (areSiblings(level, possibleAuntOrUncle, parent)) {
                return true;
            }
        }

        return false;
    }

    static boolean areCloselyRelatedForMarriage(ServerLevel level, UUID firstId, UUID secondId) {
        if (level == null || firstId == null || secondId == null) {
            return false;
        }

        if (firstId.equals(secondId)) {
            return true;
        }

        if (isChildOf(level, firstId, secondId) || isChildOf(level, secondId, firstId)) {
            return true;
        }

        if (areSiblings(level, firstId, secondId)) {
            return true;
        }

        if (isGrandparentOf(level, firstId, secondId) || isGrandparentOf(level, secondId, firstId)) {
            return true;
        }

        if (isAuntOrUncleOf(level, firstId, secondId) || isAuntOrUncleOf(level, secondId, firstId)) {
            return true;
        }

        return false;
    }

    static Optional<Object> getFamilyNode(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return Optional.empty();
        }

        Class<?> familyTreeClass = MCAReflectionHelper.resolveAnyClass(MCAReflectionHelper.MCA_FAMILY_TREE_CLASSES);
        if (familyTreeClass == null) {
            return Optional.empty();
        }

        Object familyTree = MCAReflectionHelper.invokeStatic(
                familyTreeClass,
                "get",
                new Class<?>[] {ServerLevel.class},
                level
        );
        if (familyTree == null) {
            MCAReflectionHelper.warnOnce(
                    "getFamilyNode:familyTreeNull",
                    "MCA FamilyTree#get returned null"
            );
            return Optional.empty();
        }

        Object optional = MCAReflectionHelper.invoke(
                familyTree,
                "getOrEmpty",
                new Class<?>[] {UUID.class},
                entityId
        );
        if (optional instanceof Optional<?> opt) {
            return opt.map(node -> node);
        }

        if (optional != null) {
            MCAReflectionHelper.warnOnce(
                    "getFamilyNode:unexpectedReturnType",
                    "MCA FamilyTree#getOrEmpty returned unexpected type: {}",
                    optional.getClass().getName()
            );
        }

        return Optional.empty();
    }
}
