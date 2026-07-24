package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalAgreementDataAccess {

    private static final SavedData.Factory<CapitalAgreementSavedData>
            FACTORY =
            new SavedData.Factory<>(
                    CapitalAgreementSavedData::new,
                    CapitalAgreementSavedData::load,
                    null
            );

    private CapitalAgreementDataAccess() {
    }

    public static CapitalAgreementSavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        FACTORY,
                        CapitalAgreementSavedData.DATA_NAME
                );
    }

    public static void addProposal(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
        if (level == null || proposal == null) {
            return;
        }

        get(level).addProposal(proposal);
    }

    public static DiplomaticProposal getProposal(
            ServerLevel level,
            UUID proposalId
    ) {
        if (level == null || proposalId == null) {
            return null;
        }

        return get(level).getProposal(proposalId);
    }

    public static boolean removeProposal(
            ServerLevel level,
            UUID proposalId
    ) {
        if (level == null || proposalId == null) {
            return false;
        }

        return get(level).removeProposal(proposalId);
    }

    public static DiplomaticProposal findPendingBetween(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return null;
        }

        return get(level).findPendingBetween(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static List<DiplomaticProposal> getProposalsForTarget(
            ServerLevel level,
            UUID targetCapitalId
    ) {
        if (level == null || targetCapitalId == null) {
            return List.of();
        }

        return get(level).getProposalsForTarget(
                targetCapitalId
        );
    }

    public static Map<UUID, DiplomaticProposal> getProposalsSnapshot(
            ServerLevel level
    ) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getProposalsSnapshot();
    }

    public static boolean removeProposalsBetween(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return false;
        }

        return get(level).removeProposalsBetween(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static CapitalTradeAgreement getTradeAgreement(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return null;
        }

        return get(level).getTradeAgreement(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static boolean hasTradeAgreement(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        return level != null
                && get(level).hasTradeAgreement(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static CapitalTradeAgreement establishTradeAgreement(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return null;
        }

        return get(level).establishTradeAgreement(
                firstCapitalId,
                secondCapitalId,
                level.getGameTime()
        );
    }

    public static boolean endTradeAgreement(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return false;
        }

        return get(level).endTradeAgreement(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static boolean markTradeCompleted(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return false;
        }

        return get(level).markTradeCompleted(
                firstCapitalId,
                secondCapitalId,
                level.getGameTime()
        );
    }

    public static List<CapitalTradeAgreement>
    getTradeAgreementsForCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return List.of();
        }

        return get(level).getTradeAgreementsForCapital(
                capitalId
        );
    }

    public static Map<CapitalRelationKey, CapitalTradeAgreement>
    getTradeAgreementsSnapshot(ServerLevel level) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getTradeAgreementsSnapshot();
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return false;
        }

        return get(level).removeCapital(capitalId);
    }
}