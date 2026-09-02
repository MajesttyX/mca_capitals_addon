package com.majesttyx.mcacapitals.capital;

public final class CapitalRuntimeCacheService {

    private CapitalRuntimeCacheService() {
    }

    public static void clearRuntimeState() {
        CapitalAmbassadorService.clearRuntimeCache();
        CapitalCampaignCivilianResponseService.clearRuntimeState();
        CapitalCampaignCombatService.clearRuntimeState();
        CapitalChronicleService.clearRuntimeState();
        CapitalForeignStorageRaidService.clearRuntimeState();
        CapitalMourningService.clearRuntimeState();
    }
}
