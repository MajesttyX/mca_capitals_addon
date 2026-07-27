package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class DiplomaticProposal {

    private static final String KEY_PROPOSAL_ID = "ProposalId";
    private static final String KEY_SOURCE_CAPITAL_ID = "SourceCapitalId";
    private static final String KEY_TARGET_CAPITAL_ID = "TargetCapitalId";
    private static final String KEY_SOURCE_SOVEREIGN_ID = "SourceSovereignId";
    private static final String KEY_TARGET_SOVEREIGN_ID = "TargetSovereignId";
    private static final String KEY_NOTIFIED_PLAYER_ID = "NotifiedPlayerId";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_CREATED_AT = "CreatedAt";
    private static final String KEY_SOURCE_ROYAL_ID = "SourceRoyalId";
    private static final String KEY_TARGET_ROYAL_ID = "TargetRoyalId";
    private static final String KEY_RELOCATING_ROYAL_ID = "RelocatingRoyalId";
    private static final String KEY_DESTINATION_CAPITAL_ID = "DestinationCapitalId";

    private final UUID proposalId;
    private final UUID sourceCapitalId;
    private final UUID targetCapitalId;
    private final UUID sourceSovereignId;
    private final UUID targetSovereignId;
    private final DiplomaticProposalType type;
    private final long createdAt;
    private final UUID sourceRoyalId;
    private final UUID targetRoyalId;
    private final UUID relocatingRoyalId;
    private final UUID destinationCapitalId;

    private UUID notifiedPlayerId;

    public DiplomaticProposal(
            UUID proposalId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID sourceSovereignId,
            UUID targetSovereignId,
            DiplomaticProposalType type,
            long createdAt
    ) {
        this(
                proposalId,
                sourceCapitalId,
                targetCapitalId,
                sourceSovereignId,
                targetSovereignId,
                null,
                type,
                createdAt,
                null,
                null,
                null,
                null
        );
    }

    public DiplomaticProposal(
            UUID proposalId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID sourceSovereignId,
            UUID targetSovereignId,
            UUID notifiedPlayerId,
            DiplomaticProposalType type,
            long createdAt
    ) {
        this(
                proposalId,
                sourceCapitalId,
                targetCapitalId,
                sourceSovereignId,
                targetSovereignId,
                notifiedPlayerId,
                type,
                createdAt,
                null,
                null,
                null,
                null
        );
    }

    public DiplomaticProposal(
            UUID proposalId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID sourceSovereignId,
            UUID targetSovereignId,
            UUID notifiedPlayerId,
            DiplomaticProposalType type,
            long createdAt,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID relocatingRoyalId,
            UUID destinationCapitalId
    ) {
        if (proposalId == null
                || sourceCapitalId == null
                || targetCapitalId == null
                || type == null) {
            throw new IllegalArgumentException(
                    "Diplomatic proposal identifiers and type cannot be null."
            );
        }

        if (sourceCapitalId.equals(targetCapitalId)) {
            throw new IllegalArgumentException(
                    "A capital cannot send a diplomatic proposal to itself."
            );
        }

        this.proposalId = proposalId;
        this.sourceCapitalId = sourceCapitalId;
        this.targetCapitalId = targetCapitalId;
        this.sourceSovereignId = sourceSovereignId;
        this.targetSovereignId = targetSovereignId;
        this.notifiedPlayerId = notifiedPlayerId;
        this.type = type;
        this.createdAt = Math.max(0L, createdAt);
        this.sourceRoyalId = sourceRoyalId;
        this.targetRoyalId = targetRoyalId;
        this.relocatingRoyalId = relocatingRoyalId;
        this.destinationCapitalId = destinationCapitalId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public UUID getSourceCapitalId() {
        return sourceCapitalId;
    }

    public UUID getTargetCapitalId() {
        return targetCapitalId;
    }

    public UUID getSourceSovereignId() {
        return sourceSovereignId;
    }

    public UUID getTargetSovereignId() {
        return targetSovereignId;
    }

    public UUID getNotifiedPlayerId() {
        return notifiedPlayerId;
    }

    public void setNotifiedPlayerId(UUID notifiedPlayerId) {
        this.notifiedPlayerId = notifiedPlayerId;
    }

    public boolean wasNotifiedTo(UUID playerId) {
        return playerId != null
                && playerId.equals(notifiedPlayerId);
    }

    public DiplomaticProposalType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public UUID getSourceRoyalId() {
        return sourceRoyalId;
    }

    public UUID getTargetRoyalId() {
        return targetRoyalId;
    }

    public UUID getRelocatingRoyalId() {
        return relocatingRoyalId;
    }

    public UUID getDestinationCapitalId() {
        return destinationCapitalId;
    }

    public boolean hasRoyalBetrothalDetails() {
        return sourceRoyalId != null
                && targetRoyalId != null
                && relocatingRoyalId != null
                && destinationCapitalId != null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID(
                KEY_PROPOSAL_ID,
                proposalId
        );

        tag.putUUID(
                KEY_SOURCE_CAPITAL_ID,
                sourceCapitalId
        );

        tag.putUUID(
                KEY_TARGET_CAPITAL_ID,
                targetCapitalId
        );

        tag.putString(
                KEY_TYPE,
                type.getSerializedName()
        );

        tag.putLong(
                KEY_CREATED_AT,
                createdAt
        );

        if (sourceSovereignId != null) {
            tag.putUUID(
                    KEY_SOURCE_SOVEREIGN_ID,
                    sourceSovereignId
            );
        }

        if (targetSovereignId != null) {
            tag.putUUID(
                    KEY_TARGET_SOVEREIGN_ID,
                    targetSovereignId
            );
        }

        if (notifiedPlayerId != null) {
            tag.putUUID(
                    KEY_NOTIFIED_PLAYER_ID,
                    notifiedPlayerId
            );
        }

        if (sourceRoyalId != null) {
            tag.putUUID(KEY_SOURCE_ROYAL_ID, sourceRoyalId);
        }

        if (targetRoyalId != null) {
            tag.putUUID(KEY_TARGET_ROYAL_ID, targetRoyalId);
        }

        if (relocatingRoyalId != null) {
            tag.putUUID(KEY_RELOCATING_ROYAL_ID, relocatingRoyalId);
        }

        if (destinationCapitalId != null) {
            tag.putUUID(KEY_DESTINATION_CAPITAL_ID, destinationCapitalId);
        }

        return tag;
    }

    public static DiplomaticProposal load(CompoundTag tag) {
        if (!tag.hasUUID(KEY_PROPOSAL_ID)
                || !tag.hasUUID(KEY_SOURCE_CAPITAL_ID)
                || !tag.hasUUID(KEY_TARGET_CAPITAL_ID)) {
            return null;
        }

        DiplomaticProposalType type =
                DiplomaticProposalType.fromSerializedName(
                        tag.getString(KEY_TYPE)
                );

        if (type == null) {
            return null;
        }

        UUID sourceSovereignId =
                tag.hasUUID(KEY_SOURCE_SOVEREIGN_ID)
                        ? tag.getUUID(KEY_SOURCE_SOVEREIGN_ID)
                        : null;

        UUID targetSovereignId =
                tag.hasUUID(KEY_TARGET_SOVEREIGN_ID)
                        ? tag.getUUID(KEY_TARGET_SOVEREIGN_ID)
                        : null;

        UUID notifiedPlayerId =
                tag.hasUUID(KEY_NOTIFIED_PLAYER_ID)
                        ? tag.getUUID(KEY_NOTIFIED_PLAYER_ID)
                        : null;

        return new DiplomaticProposal(
                tag.getUUID(KEY_PROPOSAL_ID),
                tag.getUUID(KEY_SOURCE_CAPITAL_ID),
                tag.getUUID(KEY_TARGET_CAPITAL_ID),
                sourceSovereignId,
                targetSovereignId,
                notifiedPlayerId,
                type,
                tag.getLong(KEY_CREATED_AT),
                tag.hasUUID(KEY_SOURCE_ROYAL_ID)
                        ? tag.getUUID(KEY_SOURCE_ROYAL_ID)
                        : null,
                tag.hasUUID(KEY_TARGET_ROYAL_ID)
                        ? tag.getUUID(KEY_TARGET_ROYAL_ID)
                        : null,
                tag.hasUUID(KEY_RELOCATING_ROYAL_ID)
                        ? tag.getUUID(KEY_RELOCATING_ROYAL_ID)
                        : null,
                tag.hasUUID(KEY_DESTINATION_CAPITAL_ID)
                        ? tag.getUUID(KEY_DESTINATION_CAPITAL_ID)
                        : null
        );
    }
}