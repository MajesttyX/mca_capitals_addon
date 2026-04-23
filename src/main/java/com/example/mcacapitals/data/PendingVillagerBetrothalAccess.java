package com.example.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public final class PendingVillagerBetrothalAccess {

    private PendingVillagerBetrothalAccess() {
    }

    public static PendingVillagerBetrothalSavedData get(ServerLevel level) {
        return PendingVillagerBetrothalSavedData.get(level);
    }

    public static List<PendingVillagerBetrothalSavedData.PendingPair> getPairs(ServerLevel level) {
        return get(level).getPairs();
    }

    public static boolean hasPendingBetrothal(ServerLevel level, UUID villagerId) {
        return get(level).hasPendingBetrothal(villagerId);
    }

    public static UUID getPartner(ServerLevel level, UUID villagerId) {
        return get(level).getPartner(villagerId);
    }

    public static boolean containsPair(ServerLevel level, UUID firstId, UUID secondId) {
        return get(level).containsPair(firstId, secondId);
    }

    public static void setPendingBetrothal(ServerLevel level, UUID firstId, UUID secondId) {
        get(level).setPair(firstId, secondId);
    }

    public static void removePendingBetrothal(ServerLevel level, UUID firstId, UUID secondId) {
        get(level).removePair(firstId, secondId);
    }

    public static void removeVillager(ServerLevel level, UUID villagerId) {
        get(level).removeVillager(villagerId);
    }
}