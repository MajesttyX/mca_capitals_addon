package com.majesttyx.mcacapitals.data;

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

    public static List<PendingVillagerBetrothalSavedData.RoyalEscortRecord> getRoyalEscorts(ServerLevel level) {
        return get(level).getRoyalEscorts();
    }

    public static boolean hasPendingBetrothal(ServerLevel level, UUID villagerId) {
        return get(level).hasPendingBetrothal(villagerId);
    }

    public static UUID getPartner(ServerLevel level, UUID villagerId) {
        return get(level).getPartner(villagerId);
    }

    public static String getPartnerName(ServerLevel level, UUID villagerId) {
        return get(level).getPartnerName(villagerId);
    }

    public static PendingVillagerBetrothalSavedData.RoyalEscortRecord getRoyalEscort(
            ServerLevel level,
            UUID firstId,
            UUID secondId
    ) {
        return get(level).getRoyalEscort(firstId, secondId);
    }

    public static boolean containsPair(ServerLevel level, UUID firstId, UUID secondId) {
        return get(level).containsPair(firstId, secondId);
    }

    public static void setPendingBetrothal(ServerLevel level, UUID firstId, UUID secondId) {
        get(level).setPair(firstId, secondId);
    }

    public static void setRoyalEscort(
            ServerLevel level,
            UUID firstId,
            String firstName,
            UUID secondId,
            String secondName,
            UUID originCapitalId,
            UUID destinationCapitalId,
            UUID relocatingRoyalId
    ) {
        get(level).setRoyalEscort(
                firstId,
                firstName,
                secondId,
                secondName,
                originCapitalId,
                destinationCapitalId,
                relocatingRoyalId,
                level.getGameTime()
        );
    }

    public static boolean completeRoyalEscort(ServerLevel level, UUID firstId, UUID secondId) {
        return get(level).completeRoyalEscort(firstId, secondId, level.getGameTime());
    }

    public static void removePendingBetrothal(ServerLevel level, UUID firstId, UUID secondId) {
        get(level).removePair(firstId, secondId);
    }

    public static boolean removeCapital(ServerLevel level, UUID capitalId) {
        return get(level).removeCapital(capitalId);
    }

    public static void removeVillager(ServerLevel level, UUID villagerId) {
        get(level).removeVillager(villagerId);
    }
}
