package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class CapitalTradeAgreement {

    public static final long TERM_DURATION_TICKS =
            13L * 24000L;

    public static final long RENEWAL_WINDOW_TICKS =
            2L * 24000L;

    private static final String KEY_FIRST_CAPITAL_ID =
            "FirstCapitalId";

    private static final String KEY_SECOND_CAPITAL_ID =
            "SecondCapitalId";
    private static final String KEY_ESTABLISHED_AT =
            "EstablishedAt";

    private static final String KEY_LAST_TRADE_AT =
            "LastTradeAt";

    private static final String KEY_TERM_STARTED_AT =
            "TermStartedAt";

    private static final String KEY_TERM_ENDS_AT =
            "TermEndsAt";

    private static final String KEY_RENEWAL_PROPOSAL_CREATED =
            "RenewalProposalCreated";
    private static final String KEY_RENEWAL_NOTICE_SENT =
            "RenewalNoticeSent";

    private final CapitalRelationKey key;
    private final long establishedAt;
    private long lastTradeAt;
    private long termStartedAt;
    private long termEndsAt;
    private boolean renewalProposalCreated;
    private boolean renewalNoticeSent;
    public CapitalTradeAgreement(
            UUID firstCapitalId,
            UUID secondCapitalId,
            long establishedAt,
            long lastTradeAt
    ) {
        this(
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                ),
                establishedAt,
                lastTradeAt
        );
    }
    public CapitalTradeAgreement(
            CapitalRelationKey key,
            long establishedAt,
            long lastTradeAt
    ) {
        this(
                key,
                establishedAt,
                lastTradeAt,
                Math.max(0L, establishedAt),
                safeAdd(
                        Math.max(0L, establishedAt),
                        TERM_DURATION_TICKS
                ),
                false,
                false
        );
    }
    private CapitalTradeAgreement(
            CapitalRelationKey key,
            long establishedAt,
            long lastTradeAt,
            long termStartedAt,
            long termEndsAt,
            boolean renewalProposalCreated,
            boolean renewalNoticeSent
    ) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "Trade agreement key cannot be null."
            );
        }
        this.key = key;
        this.establishedAt = Math.max(0L, establishedAt);
        this.lastTradeAt = Math.max(0L, lastTradeAt);
        this.termStartedAt = Math.max(0L, termStartedAt);
        this.termEndsAt = Math.max(0L, termEndsAt);
        this.renewalProposalCreated = renewalProposalCreated;
        this.renewalNoticeSent = renewalNoticeSent;
    }

    public CapitalRelationKey getKey() {
        return key;
    }

    public UUID getFirstCapitalId() {
        return key.first();
    }
    public UUID getSecondCapitalId() {
        return key.second();
    }

    public long getEstablishedAt() {
        return establishedAt;
    }

    public long getLastTradeAt() {
        return lastTradeAt;
    }

    public void setLastTradeAt(long lastTradeAt) {
        this.lastTradeAt = Math.max(0L, lastTradeAt);
    }

    public long getTermStartedAt() {
        return termStartedAt;
    }

    public long getTermEndsAt() {
        return termEndsAt;
    }
    public boolean needsTermInitialization() {
        return termEndsAt <= termStartedAt;
    }

    public void initializeLegacyTerm(long currentGameTime) {
        if (!needsTermInitialization()) {
            return;
        }

        startNewTerm(currentGameTime);
    }

    public void renewTerm(long currentGameTime) {
        startNewTerm(currentGameTime);
    }
    public boolean isInRenewalWindow(long currentGameTime) {
        if (needsTermInitialization()) {
            return false;
        }

        long now = Math.max(0L, currentGameTime);
        long renewalBeginsAt = Math.max(
                termStartedAt,
                termEndsAt - RENEWAL_WINDOW_TICKS
        );

        return now >= renewalBeginsAt
                && now < termEndsAt;
    }
    public boolean isExpired(long currentGameTime) {
        return !needsTermInitialization()
                && Math.max(0L, currentGameTime) >= termEndsAt;
    }

    public long getTicksUntilExpiry(long currentGameTime) {
        if (needsTermInitialization()) {
            return TERM_DURATION_TICKS;
        }

        return Math.max(
                0L,
                termEndsAt - Math.max(0L, currentGameTime)
        );
    }
    public boolean isRenewalProposalCreated() {
        return renewalProposalCreated;
    }

    public void markRenewalProposalCreated() {
        renewalProposalCreated = true;
    }

    public boolean isRenewalNoticeSent() {
        return renewalNoticeSent;
    }

    public void markRenewalNoticeSent() {
        renewalNoticeSent = true;
    }
    public boolean containsCapital(UUID capitalId) {
        return capitalId != null
                && (capitalId.equals(key.first())
                || capitalId.equals(key.second()));
    }

    public UUID getOtherCapitalId(UUID capitalId) {
        if (capitalId == null) {
            return null;
        }

        if (capitalId.equals(key.first())) {
            return key.second();
        }

        if (capitalId.equals(key.second())) {
            return key.first();
        }
        return null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID(
                KEY_FIRST_CAPITAL_ID,
                key.first()
        );

        tag.putUUID(
                KEY_SECOND_CAPITAL_ID,
                key.second()
        );

        tag.putLong(
                KEY_ESTABLISHED_AT,
                establishedAt
        );

        tag.putLong(
                KEY_LAST_TRADE_AT,
                lastTradeAt
        );
        tag.putLong(
                KEY_TERM_STARTED_AT,
                termStartedAt
        );

        tag.putLong(
                KEY_TERM_ENDS_AT,
                termEndsAt
        );

        tag.putBoolean(
                KEY_RENEWAL_PROPOSAL_CREATED,
                renewalProposalCreated
        );

        tag.putBoolean(
                KEY_RENEWAL_NOTICE_SENT,
                renewalNoticeSent
        );

        return tag;
    }
    public static CapitalTradeAgreement load(
            CompoundTag tag
    ) {
        if (tag == null
                || !tag.hasUUID(KEY_FIRST_CAPITAL_ID)
                || !tag.hasUUID(KEY_SECOND_CAPITAL_ID)) {
            return null;
        }

        UUID firstCapitalId =
                tag.getUUID(KEY_FIRST_CAPITAL_ID);

        UUID secondCapitalId =
                tag.getUUID(KEY_SECOND_CAPITAL_ID);

        if (firstCapitalId.equals(secondCapitalId)) {
            return null;
        }
        boolean hasTermData =
                tag.contains(KEY_TERM_STARTED_AT)
                        && tag.contains(KEY_TERM_ENDS_AT);
        return new CapitalTradeAgreement(
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                ),
                tag.getLong(KEY_ESTABLISHED_AT),
                tag.getLong(KEY_LAST_TRADE_AT),
                hasTermData
                        ? tag.getLong(KEY_TERM_STARTED_AT)
                        : 0L,
                hasTermData
                        ? tag.getLong(KEY_TERM_ENDS_AT)
                        : 0L,
                hasTermData
                        && tag.getBoolean(
                        KEY_RENEWAL_PROPOSAL_CREATED
                ),
                hasTermData
                        && tag.getBoolean(
                        KEY_RENEWAL_NOTICE_SENT
                )
        );
    }
    private void startNewTerm(long currentGameTime) {
        termStartedAt = Math.max(0L, currentGameTime);
        termEndsAt = safeAdd(
                termStartedAt,
                TERM_DURATION_TICKS
        );
        renewalProposalCreated = false;
        renewalNoticeSent = false;
    }

    private static long safeAdd(
            long value,
            long addition
    ) {
        long normalizedValue = Math.max(0L, value);
        long normalizedAddition = Math.max(0L, addition);
        if (Long.MAX_VALUE - normalizedValue
                < normalizedAddition) {
            return Long.MAX_VALUE;
        }

        return normalizedValue + normalizedAddition;
    }
}
