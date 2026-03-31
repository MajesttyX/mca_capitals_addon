package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class MCAFamilyBridge {

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

    static UUID getSpouse(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID partner = MCAReflectionHelper.asUuid(MCAReflectionHelper.invoke(nodeOpt.get(), "partner"));
        return MCAReflectionHelper.isNullUuid(partner) ? null : partner;
    }

    static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return Collections.emptySet();
        }

        return MCAReflectionHelper.extractUuidSet(MCAReflectionHelper.invoke(nodeOpt.get(), "children"));
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