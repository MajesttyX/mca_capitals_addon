package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CapitalAmbientConversationRuntime {

    private static final Map<UUID, LastResponseState> LAST_RESPONSE_STATE = new HashMap<>();
    private static final Set<String> WARNED_MISSING_FRIENDLY_KEYS = ConcurrentHashMap.newKeySet();

    private CapitalAmbientConversationRuntime() {
    }

    static boolean hasCall(
            Entity caller,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        return resolveCallKey(caller, context, conversationId) != null;
    }

    static boolean hasResponse(
            Entity responder,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        return !resolveResponseKeys(responder, context, conversationId).isEmpty();
    }

    static Component createCall(
            ServerLevel level,
            CapitalRecord capital,
            CapitalRecord foreignCapital,
            Entity caller,
            Entity responder,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        if (level == null || capital == null || caller == null || responder == null || context == null || conversationId == null) {
            return null;
        }

        String key = resolveCallKey(caller, context, conversationId);
        if (key == null) {
            return null;
        }

        CapitalDialogueContext dialogueContext =
                CapitalDialogueContext.createAmbient(level, capital, foreignCapital, caller, responder);
        return Component.translatable(key, dialogueContext.arguments());
    }

    static Component createResponse(
            ServerLevel level,
            CapitalRecord capital,
            CapitalRecord foreignCapital,
            Entity responder,
            Entity caller,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        if (level == null || capital == null || responder == null || caller == null || context == null || conversationId == null) {
            return null;
        }

        List<String> keys = resolveResponseKeys(responder, context, conversationId);
        if (keys.isEmpty()) {
            return null;
        }

        int index = pickResponseIndex(
                level,
                responder.getUUID(),
                context.path() + ":" + conversationId,
                keys.size()
        );

        CapitalDialogueContext dialogueContext =
                CapitalDialogueContext.createAmbient(level, capital, foreignCapital, responder, caller);
        return Component.translatable(keys.get(index), dialogueContext.arguments());
    }

    private static String resolveCallKey(
            Entity caller,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        String personality = CapitalDialoguePersonalityResolver.resolve(caller);
        if (personality == null) {
            return null;
        }

        String key = callKey(personality, context, conversationId);
        if (CapitalDialogueTranslationIndex.hasKey(key)) {
            return key;
        }

        String friendly = CapitalDialoguePersonalityResolver.defaultPersonality();
        String fallback = callKey(friendly, context, conversationId);
        if (CapitalDialogueTranslationIndex.hasKey(fallback)) {
            return fallback;
        }

        warnMissingFriendly("call", context, conversationId);
        return null;
    }

    private static List<String> resolveResponseKeys(
            Entity responder,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        String personality = CapitalDialoguePersonalityResolver.resolve(responder);
        if (personality == null) {
            return List.of();
        }

        List<String> keys = CapitalDialogueTranslationIndex.findDotNumberedKeys(
                responseBase(personality, context, conversationId)
        );
        if (!keys.isEmpty()) {
            return keys;
        }

        String friendly = CapitalDialoguePersonalityResolver.defaultPersonality();
        List<String> fallback = CapitalDialogueTranslationIndex.findDotNumberedKeys(
                responseBase(friendly, context, conversationId)
        );
        if (!fallback.isEmpty()) {
            return fallback;
        }

        warnMissingFriendly("response", context, conversationId);
        return List.of();
    }

    private static String callKey(
            String personality,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        return "mcacapitals.dialogue."
                + personality
                + ".call."
                + context.path()
                + "."
                + conversationId;
    }

    private static String responseBase(
            String personality,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        return "mcacapitals.dialogue."
                + personality
                + ".response."
                + context.path()
                + "."
                + conversationId;
    }

    private static int pickResponseIndex(
            ServerLevel level,
            UUID responderId,
            String conversationKey,
            int size
    ) {
        if (size <= 1) {
            return 0;
        }

        int index = level.random.nextInt(size);
        LastResponseState state = LAST_RESPONSE_STATE.computeIfAbsent(
                responderId,
                ignored -> new LastResponseState()
        );

        if (conversationKey.equals(state.lastConversationKey)
                && index == state.lastIndex) {
            index = (index + 1) % size;
        }

        state.lastConversationKey = conversationKey;
        state.lastIndex = index;
        return index;
    }

    private static void warnMissingFriendly(
            String type,
            CapitalAmbientConversationDefinitions.Context context,
            String conversationId
    ) {
        String warningKey = type + ":" + context.path() + ":" + conversationId;
        if (WARNED_MISSING_FRIENDLY_KEYS.add(warningKey)) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Missing Friendly Talk of the Town {} for {} conversation {}",
                    type,
                    context.path(),
                    conversationId
            );
        }
    }

    private static final class LastResponseState {
        private String lastConversationKey = "";
        private int lastIndex = -1;
    }

    static void clearRuntimeState() {
        LAST_RESPONSE_STATE.clear();
        WARNED_MISSING_FRIENDLY_KEYS.clear();
    }

}
