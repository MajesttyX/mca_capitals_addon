package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalAgreementSavedData extends SavedData {
    public static final String DATA_NAME =
            "mcacapitals_diplomatic_agreements";

    private static final String KEY_PROPOSALS =
            "Proposals";

    private static final String KEY_TRADE_AGREEMENTS =
            "TradeAgreements";

    private final Map<UUID, DiplomaticProposal> proposals =
            new LinkedHashMap<>();

    private final Map<CapitalRelationKey, CapitalTradeAgreement>
            tradeAgreements = new LinkedHashMap<>();
    public void addProposal(DiplomaticProposal proposal) {
        if (proposal == null) {
            return;
        }

        proposals.put(
                proposal.getProposalId(),
                proposal
        );

        setDirty();
    }

    public DiplomaticProposal getProposal(UUID proposalId) {
        if (proposalId == null) {
            return null;
        }

        return proposals.get(proposalId);
    }
    public boolean removeProposal(UUID proposalId) {
        if (proposalId == null) {
            return false;
        }

        boolean removed =
                proposals.remove(proposalId) != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }
    public DiplomaticProposal findPendingBetween(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return null;
        }
        for (DiplomaticProposal proposal : proposals.values()) {
            boolean forward =
                    firstCapitalId.equals(
                            proposal.getSourceCapitalId()
                    )
                            && secondCapitalId.equals(
                            proposal.getTargetCapitalId()
                    );
            boolean reverse =
                    secondCapitalId.equals(
                            proposal.getSourceCapitalId()
                    )
                            && firstCapitalId.equals(
                            proposal.getTargetCapitalId()
                    );

            if (forward || reverse) {
                return proposal;
            }
        }

        return null;
    }
    public List<DiplomaticProposal> getProposalsForTarget(
            UUID targetCapitalId
    ) {
        if (targetCapitalId == null) {
            return List.of();
        }

        List<DiplomaticProposal> result =
                new ArrayList<>();

        for (DiplomaticProposal proposal : proposals.values()) {
            if (targetCapitalId.equals(
                    proposal.getTargetCapitalId()
            )) {
                result.add(proposal);
            }
        }
        return List.copyOf(result);
    }

    public Map<UUID, DiplomaticProposal> getProposalsSnapshot() {
        return new LinkedHashMap<>(proposals);
    }

    public boolean removeProposalsBetween(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return false;
        }
        boolean removed =
                proposals.entrySet().removeIf(entry -> {
                    DiplomaticProposal proposal =
                            entry.getValue();
                    boolean forward =
                            firstCapitalId.equals(
                                    proposal.getSourceCapitalId()
                            )
                                    && secondCapitalId.equals(
                                    proposal.getTargetCapitalId()
                            );
                    boolean reverse =
                            secondCapitalId.equals(
                                    proposal.getSourceCapitalId()
                            )
                                    && firstCapitalId.equals(
                                    proposal.getTargetCapitalId()
                            );

                    return forward || reverse;
                });

        if (removed) {
            setDirty();
        }

        return removed;
    }
    public CapitalTradeAgreement getTradeAgreement(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return null;
        }

        return tradeAgreements.get(
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                )
        );
    }
    public boolean hasTradeAgreement(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        return getTradeAgreement(
                firstCapitalId,
                secondCapitalId
        ) != null;
    }
    public CapitalTradeAgreement establishTradeAgreement(
            UUID firstCapitalId,
            UUID secondCapitalId,
            long establishedAt
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return null;
        }

        CapitalRelationKey key = CapitalRelationKey.of(
                firstCapitalId,
                secondCapitalId
        );
        CapitalTradeAgreement existing =
                tradeAgreements.get(key);

        if (existing != null) {
            return existing;
        }

        CapitalTradeAgreement agreement =
                new CapitalTradeAgreement(
                        key,
                        establishedAt,
                        0L
                );

        tradeAgreements.put(key, agreement);
        setDirty();

        return agreement;
    }
    public boolean endTradeAgreement(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return false;
        }

        boolean removed = tradeAgreements.remove(
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                )
        ) != null;
        if (removed) {
            setDirty();
        }

        return removed;
    }

    public boolean markTradeCompleted(
            UUID firstCapitalId,
            UUID secondCapitalId,
            long completedAt
    ) {
        CapitalTradeAgreement agreement =
                getTradeAgreement(
                        firstCapitalId,
                        secondCapitalId
                );

        if (agreement == null) {
            return false;
        }
        agreement.setLastTradeAt(completedAt);
        setDirty();

        return true;
    }

    public List<CapitalTradeAgreement>
    getTradeAgreementsForCapital(UUID capitalId) {
        if (capitalId == null) {
            return List.of();
        }

        List<CapitalTradeAgreement> result =
                new ArrayList<>();
        for (CapitalTradeAgreement agreement :
                tradeAgreements.values()) {
            if (agreement.containsCapital(capitalId)) {
                result.add(agreement);
            }
        }

        return List.copyOf(result);
    }

    public Map<CapitalRelationKey, CapitalTradeAgreement>
    getTradeAgreementsSnapshot() {
        return new LinkedHashMap<>(tradeAgreements);
    }
    public boolean removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        boolean removedProposals =
                proposals.entrySet().removeIf(entry -> {
                    DiplomaticProposal proposal = entry.getValue();
                    return capitalId.equals(
                            proposal.getSourceCapitalId()
                    )
                            || capitalId.equals(
                            proposal.getTargetCapitalId()
                    );
                });

        boolean removedTradeAgreements =
                tradeAgreements.entrySet().removeIf(entry ->
                        entry.getValue().containsCapital(capitalId)
                );
        if (removedProposals || removedTradeAgreements) {
            setDirty();
            return true;
        }

        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag proposalsTag = new ListTag();

        for (DiplomaticProposal proposal : proposals.values()) {
            proposalsTag.add(proposal.save());
        }
        tag.put(
                KEY_PROPOSALS,
                proposalsTag
        );

        ListTag tradeAgreementsTag = new ListTag();

        for (CapitalTradeAgreement agreement :
                tradeAgreements.values()) {
            tradeAgreementsTag.add(agreement.save());
        }

        tag.put(
                KEY_TRADE_AGREEMENTS,
                tradeAgreementsTag
        );

        return tag;
    }
    public static CapitalAgreementSavedData load(CompoundTag tag) {
        CapitalAgreementSavedData data =
                new CapitalAgreementSavedData();

        ListTag proposalsTag = tag.getList(
                KEY_PROPOSALS,
                Tag.TAG_COMPOUND
        );
        for (Tag rawProposal : proposalsTag) {
            DiplomaticProposal proposal =
                    DiplomaticProposal.load(
                            (CompoundTag) rawProposal
                    );

            if (proposal != null) {
                data.proposals.put(
                        proposal.getProposalId(),
                        proposal
                );
            }
        }
        ListTag tradeAgreementsTag = tag.getList(
                KEY_TRADE_AGREEMENTS,
                Tag.TAG_COMPOUND
        );

        for (Tag rawAgreement : tradeAgreementsTag) {
            CapitalTradeAgreement agreement =
                    CapitalTradeAgreement.load(
                            (CompoundTag) rawAgreement
                    );
            if (agreement != null) {
                data.tradeAgreements.put(
                        agreement.getKey(),
                        agreement
                );
            }
        }

        return data;
    }
}
