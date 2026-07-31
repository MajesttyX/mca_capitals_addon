package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class CapitalInterregnumRecord {

    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_DECEASED_SOVEREIGN_ID = "DeceasedSovereignId";
    private static final String KEY_DECEASED_SOVEREIGN_NAME = "DeceasedSovereignName";
    private static final String KEY_STARTED_AT = "StartedAt";
    private static final String KEY_RESOLVE_AFTER = "ResolveAfter";
    private static final String KEY_DECEASED_SOVEREIGN_FEMALE = "DeceasedSovereignFemale";
    private static final String KEY_FORMER_PLAYER_SOVEREIGN = "FormerPlayerSovereign";
    private static final String KEY_FORMER_PLAYER_SOVEREIGN_ID = "FormerPlayerSovereignId";
    private static final String KEY_DEPOSITION = "Deposition";
    private static final String KEY_VICTORIOUS_CLAIMANT_ID = "VictoriousClaimantId";

    private final UUID capitalId;
    private final UUID deceasedSovereignId;
    private final String deceasedSovereignName;
    private final long startedAt;
    private final long resolveAfter;
    private final boolean deceasedSovereignFemale;
    private final boolean formerPlayerSovereign;
    private final UUID formerPlayerSovereignId;
    private final boolean deposition;
    private final UUID victoriousClaimantId;

    public CapitalInterregnumRecord(
            UUID capitalId,
            UUID deceasedSovereignId,
            String deceasedSovereignName,
            long startedAt,
            long resolveAfter,
            boolean deceasedSovereignFemale,
            boolean formerPlayerSovereign,
            UUID formerPlayerSovereignId
    ) {
        this(
                capitalId,
                deceasedSovereignId,
                deceasedSovereignName,
                startedAt,
                resolveAfter,
                deceasedSovereignFemale,
                formerPlayerSovereign,
                formerPlayerSovereignId,
                false,
                null
        );
    }

    public CapitalInterregnumRecord(
            UUID capitalId,
            UUID deceasedSovereignId,
            String deceasedSovereignName,
            long startedAt,
            long resolveAfter,
            boolean deceasedSovereignFemale,
            boolean formerPlayerSovereign,
            UUID formerPlayerSovereignId,
            boolean deposition,
            UUID victoriousClaimantId
    ) {
        if (capitalId == null || deceasedSovereignId == null) {
            throw new IllegalArgumentException(
                    "Capital and former sovereign IDs cannot be null."
            );
        }

        this.capitalId = capitalId;
        this.deceasedSovereignId = deceasedSovereignId;
        this.deceasedSovereignName =
                deceasedSovereignName == null
                        || deceasedSovereignName.isBlank()
                        ? deceasedSovereignId.toString()
                        : deceasedSovereignName.trim();
        this.startedAt = Math.max(0L, startedAt);
        this.resolveAfter = Math.max(this.startedAt, resolveAfter);
        this.deceasedSovereignFemale = deceasedSovereignFemale;
        this.formerPlayerSovereign = formerPlayerSovereign;
        this.formerPlayerSovereignId = formerPlayerSovereignId;
        this.deposition = deposition;
        this.victoriousClaimantId = victoriousClaimantId;
    }

    public UUID getCapitalId() {
        return capitalId;
    }

    public UUID getDeceasedSovereignId() {
        return deceasedSovereignId;
    }

    public String getDeceasedSovereignName() {
        return deceasedSovereignName;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getResolveAfter() {
        return resolveAfter;
    }

    public boolean wasDeceasedSovereignFemale() {
        return deceasedSovereignFemale;
    }

    public boolean wasPlayerSovereign() {
        return formerPlayerSovereign;
    }

    public UUID getFormerPlayerSovereignId() {
        return formerPlayerSovereignId;
    }

    public boolean wasDeposition() {
        return deposition;
    }

    public UUID getVictoriousClaimantId() {
        return victoriousClaimantId;
    }

    public boolean mayVictoriousPlayerSeize(UUID playerId) {
        return deposition
                && playerId != null
                && playerId.equals(victoriousClaimantId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID(KEY_CAPITAL_ID, capitalId);
        tag.putUUID(
                KEY_DECEASED_SOVEREIGN_ID,
                deceasedSovereignId
        );
        tag.putString(
                KEY_DECEASED_SOVEREIGN_NAME,
                deceasedSovereignName
        );
        tag.putLong(KEY_STARTED_AT, startedAt);
        tag.putLong(KEY_RESOLVE_AFTER, resolveAfter);
        tag.putBoolean(
                KEY_DECEASED_SOVEREIGN_FEMALE,
                deceasedSovereignFemale
        );
        tag.putBoolean(
                KEY_FORMER_PLAYER_SOVEREIGN,
                formerPlayerSovereign
        );
        tag.putBoolean(KEY_DEPOSITION, deposition);

        if (formerPlayerSovereignId != null) {
            tag.putUUID(
                    KEY_FORMER_PLAYER_SOVEREIGN_ID,
                    formerPlayerSovereignId
            );
        }

        if (victoriousClaimantId != null) {
            tag.putUUID(
                    KEY_VICTORIOUS_CLAIMANT_ID,
                    victoriousClaimantId
            );
        }

        return tag;
    }

    public static CapitalInterregnumRecord load(
            CompoundTag tag
    ) {
        if (tag == null
                || !tag.hasUUID(KEY_CAPITAL_ID)
                || !tag.hasUUID(KEY_DECEASED_SOVEREIGN_ID)) {
            return null;
        }

        UUID formerPlayerSovereignId =
                tag.hasUUID(KEY_FORMER_PLAYER_SOVEREIGN_ID)
                        ? tag.getUUID(KEY_FORMER_PLAYER_SOVEREIGN_ID)
                        : null;

        UUID victoriousClaimantId =
                tag.hasUUID(KEY_VICTORIOUS_CLAIMANT_ID)
                        ? tag.getUUID(KEY_VICTORIOUS_CLAIMANT_ID)
                        : null;

        return new CapitalInterregnumRecord(
                tag.getUUID(KEY_CAPITAL_ID),
                tag.getUUID(KEY_DECEASED_SOVEREIGN_ID),
                tag.getString(KEY_DECEASED_SOVEREIGN_NAME),
                tag.getLong(KEY_STARTED_AT),
                tag.getLong(KEY_RESOLVE_AFTER),
                tag.getBoolean(KEY_DECEASED_SOVEREIGN_FEMALE),
                tag.getBoolean(KEY_FORMER_PLAYER_SOVEREIGN),
                formerPlayerSovereignId,
                tag.getBoolean(KEY_DEPOSITION),
                victoriousClaimantId
        );
    }
}