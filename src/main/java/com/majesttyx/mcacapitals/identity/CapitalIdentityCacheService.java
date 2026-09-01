package com.majesttyx.mcacapitals.identity;

public final class CapitalIdentityCacheService {

    private CapitalIdentityCacheService() {
    }

    public static void clearResourceCaches() {
        SurnamePool.clearCache();
        HouseWordsPool.clearCache();
    }
}
