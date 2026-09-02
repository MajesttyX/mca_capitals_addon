package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalRuntimeCacheService;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueCacheService;
import com.majesttyx.mcacapitals.identity.CapitalIdentityCacheService;
import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import com.majesttyx.mcacapitals.noble.NobleManager;

public final class CapitalRuntimeStateReset {

    private CapitalRuntimeStateReset() {
    }

    public static void clearServerSessionState() {
        CapitalHardcoreDeathTracker.clearAll();
        CapitalRuntimeCacheService.clearRuntimeState();
        CapitalDialogueCacheService.clearRuntimeState();
        SealedPurseHandler.clearRuntimeState();
        AbdicationPromptManager.clearAll();
        NobleManager.clearAll();
    }

    public static void clearResourceCaches() {
        CapitalDialogueCacheService.clearResourceCaches();
        CapitalIdentityCacheService.clearResourceCaches();
    }
}
