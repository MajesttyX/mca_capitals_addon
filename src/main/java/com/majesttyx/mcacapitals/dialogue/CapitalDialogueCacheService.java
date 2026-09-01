package com.majesttyx.mcacapitals.dialogue;

public final class CapitalDialogueCacheService {

    private CapitalDialogueCacheService() {
    }

    public static void clearRuntimeState() {
        CapitalAmbientConversationRuntime.clearRuntimeState();
        CapitalDialogueRuntime.clearRuntimeState();
        CapitalDialogueService.clearRuntimeState();
        CapitalPoliticalDialogueService.clearRuntimeState();
        CapitalDialoguePersonalityResolver.clearWarningState();
    }

    public static void clearResourceCaches() {
        CapitalAmbientConversationDefinitions.clearCache();
    }
}
