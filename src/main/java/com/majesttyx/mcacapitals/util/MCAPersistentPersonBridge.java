package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class MCAPersistentPersonBridge {

    private MCAPersistentPersonBridge() {
    }

    public static boolean isKnownVillager(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        Entity loaded = MCAEntityBridge.findLoadedEntityByUuid(level, entityId);
        if (loaded != null) {
            return MCAEntityBridge.isMCAVillagerEntity(loaded);
        }

        Optional<Object> node = MCAFamilyBridge.getFamilyNode(level, entityId);
        return node.isPresent() && !isPlayerNode(node.get());
    }

    public static boolean isKnownPlayer(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
            if (player != null) {
                return true;
            }
        }

        Optional<Object> node = MCAFamilyBridge.getFamilyNode(level, entityId);
        return node.isPresent() && isPlayerNode(node.get());
    }

    public static boolean hasCurrentMarriage(
            ServerLevel level,
            UUID personId,
            UUID spouseId
    ) {
        if (level == null || personId == null || spouseId == null) {
            return false;
        }

        Optional<Object> node = MCAFamilyBridge.getFamilyNode(level, personId);
        if (node.isEmpty()) {
            return false;
        }

        UUID partner = MCAReflectionHelper.asUuid(
                MCAReflectionHelper.invoke(node.get(), "partner")
        );
        if (MCAReflectionHelper.isNullUuid(partner)
                || !spouseId.equals(partner)) {
            return false;
        }

        Object relationshipState = MCAReflectionHelper.invoke(
                node.get(),
                "getRelationshipState"
        );
        if (relationshipState == null) {
            return false;
        }

        Object married = MCAReflectionHelper.invoke(
                relationshipState,
                "isMarried"
        );
        return married instanceof Boolean value && value;
    }

    private static boolean isPlayerNode(Object node) {
        Object result = MCAReflectionHelper.invoke(node, "isPlayer");
        return result instanceof Boolean value && value;
    }
}
