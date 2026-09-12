package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalRuntimeCacheService;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueCacheService;
import com.majesttyx.mcacapitals.identity.CapitalIdentityCacheService;
import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import com.majesttyx.mcacapitals.network.PendingNetworkInteractionService;

public final class CapitalRuntimeStateReset {

    private CapitalRuntimeStateReset() {
    }

    public static void clearServerSessionState() {
        CapitalManager.clearAll();
        CapitalHardcoreDeathTracker.clearAll();
        CapitalRuntimeCacheService.clearRuntimeState();
        CapitalDialogueCacheService.clearRuntimeState();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();
        SealedPurseHandler.clearRuntimeState();
        AbdicationPromptManager.clearAll();
        PendingNetworkInteractionService.clearAll();
        MCAPlayerBridge.clearRuntimeState();
    }

    public static void clearResourceCaches() {
        CapitalDialogueCacheService.clearResourceCaches();
        CapitalIdentityCacheService.clearResourceCaches();
    }
}
