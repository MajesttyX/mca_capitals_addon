package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.network.SyncBlueprintAuthorityPacket;

public final class BlueprintAuthorityClientCache {

    private static SyncBlueprintAuthorityPacket current;

    private BlueprintAuthorityClientCache() {
    }

    public static void put(SyncBlueprintAuthorityPacket packet) {
        current = packet;
    }

    public static SyncBlueprintAuthorityPacket getForVillage(int villageId) {
        if (current == null || current.villageId() != villageId) {
            return null;
        }
        return current;
    }

    public static SyncBlueprintAuthorityPacket getCurrent() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}