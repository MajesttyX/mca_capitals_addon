package com.majesttyx.mcacapitals.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PendingNetworkInteractionService {

    public enum InteractionType {
        DECREE_OF_THE_HOUSE,
        DECLARATION_OF_SEPARATION
    }

    public record PendingInteraction(
            InteractionType type,
            UUID token,
            UUID targetId,
            boolean playerTarget
    ) {
    }

    private static final Map<UUID, Map<UUID, PendingInteraction>> PENDING_BY_PLAYER = new HashMap<>();

    private PendingNetworkInteractionService() {
    }

    public static UUID authorize(
            ServerPlayer player,
            InteractionType type,
            UUID targetId,
            boolean playerTarget
    ) {
        if (player == null || type == null || targetId == null) {
            return null;
        }

        UUID token = UUID.randomUUID();
        PENDING_BY_PLAYER.computeIfAbsent(
                player.getUUID(),
                ignored -> new HashMap<>()
        ).put(token, new PendingInteraction(type, token, targetId, playerTarget));
        return token;
    }

    public static PendingInteraction consume(
            ServerPlayer player,
            InteractionType expectedType,
            UUID token
    ) {
        if (player == null || expectedType == null || token == null) {
            return null;
        }

        UUID playerId = player.getUUID();
        Map<UUID, PendingInteraction> pendingByToken = PENDING_BY_PLAYER.get(playerId);
        if (pendingByToken == null) {
            return null;
        }

        PendingInteraction pending = pendingByToken.get(token);
        if (pending == null || pending.type() != expectedType) {
            return null;
        }

        pendingByToken.remove(token);
        if (pendingByToken.isEmpty()) {
            PENDING_BY_PLAYER.remove(playerId);
        }
        return pending;
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            PENDING_BY_PLAYER.remove(player.getUUID());
        }
    }

    public static void clearAll() {
        PENDING_BY_PLAYER.clear();
    }
}
