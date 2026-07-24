package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class CapitalTradeAgreement {

    private static final String KEY_FIRST_CAPITAL_ID =
            "FirstCapitalId";

    private static final String KEY_SECOND_CAPITAL_ID =
            "SecondCapitalId";

    private static final String KEY_ESTABLISHED_AT =
            "EstablishedAt";

    private static final String KEY_LAST_TRADE_AT =
            "LastTradeAt";

    private final CapitalRelationKey key;
    private final long establishedAt;
    private long lastTradeAt;

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
        if (key == null) {
            throw new IllegalArgumentException(
                    "Trade agreement key cannot be null."
            );
        }

        this.key = key;
        this.establishedAt = Math.max(0L, establishedAt);
        this.lastTradeAt = Math.max(0L, lastTradeAt);
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

        return new CapitalTradeAgreement(
                firstCapitalId,
                secondCapitalId,
                tag.getLong(KEY_ESTABLISHED_AT),
                tag.getLong(KEY_LAST_TRADE_AT)
        );
    }
}