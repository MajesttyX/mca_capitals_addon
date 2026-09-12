package com.majesttyx.mcacapitals.client;

public final class CapitalClientRuntimeStateReset {

    private CapitalClientRuntimeStateReset() {
    }

    public static void clearSessionState() {
        VillagerIdentityClientCache.clearAll();
        BlueprintAuthorityClientCache.clear();
    }
}
