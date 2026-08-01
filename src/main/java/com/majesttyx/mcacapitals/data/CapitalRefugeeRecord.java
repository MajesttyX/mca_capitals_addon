package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class CapitalRefugeeRecord {

    private static final String KEY_REFUGEE_ID =
            "RefugeeId";

    private static final String KEY_ORIGIN_CAPITAL_ID =
            "OriginCapitalId";

    private static final String KEY_ORIGIN_VILLAGE_ID =
            "OriginVillageId";

    private static final String KEY_ORIGIN_CAPITAL_NAME =
            "OriginCapitalName";

    private static final String KEY_ASYLUM_CAPITAL_ID =
            "AsylumCapitalId";

    private static final String KEY_EXILED_AT =
            "ExiledAt";

    private static final String KEY_ASYLUM_GRANTED_AT =
            "AsylumGrantedAt";

    private static final String KEY_STATUS =
            "Status";

    private final UUID refugeeId;
    private final UUID originCapitalId;
    private final int originVillageId;
    private final String originCapitalName;
    private final long exiledAt;

    private UUID asylumCapitalId;
    private long asylumGrantedAt;
    private CapitalRefugeeStatus status;

    public CapitalRefugeeRecord(
            UUID refugeeId,
            UUID originCapitalId,
            int originVillageId,
            String originCapitalName,
            long exiledAt
    ) {
        this(
                refugeeId,
                originCapitalId,
                originVillageId,
                originCapitalName,
                null,
                exiledAt,
                0L,
                CapitalRefugeeStatus.EXILED
        );
    }

    public CapitalRefugeeRecord(
            UUID refugeeId,
            UUID originCapitalId,
            int originVillageId,
            String originCapitalName,
            UUID asylumCapitalId,
            long exiledAt,
            long asylumGrantedAt,
            CapitalRefugeeStatus status
    ) {
        if (refugeeId == null || originCapitalId == null) {
            throw new IllegalArgumentException(
                    "Refugee and origin capital IDs cannot be null."
            );
        }

        this.refugeeId = refugeeId;
        this.originCapitalId = originCapitalId;
        this.originVillageId = originVillageId;

        this.originCapitalName =
                originCapitalName == null
                        || originCapitalName.isBlank()
                        ? "Unknown Capital"
                        : originCapitalName.trim();

        this.asylumCapitalId = asylumCapitalId;
        this.exiledAt = Math.max(0L, exiledAt);
        this.asylumGrantedAt = Math.max(
                0L,
                asylumGrantedAt
        );

        this.status =
                status == null
                        ? CapitalRefugeeStatus.EXILED
                        : status;
    }

    public UUID getRefugeeId() {
        return refugeeId;
    }

    public UUID getOriginCapitalId() {
        return originCapitalId;
    }

    public int getOriginVillageId() {
        return originVillageId;
    }

    public String getOriginCapitalName() {
        return originCapitalName;
    }

    public UUID getAsylumCapitalId() {
        return asylumCapitalId;
    }

    public long getExiledAt() {
        return exiledAt;
    }

    public long getAsylumGrantedAt() {
        return asylumGrantedAt;
    }

    public CapitalRefugeeStatus getStatus() {
        return status;
    }

    public boolean isAwaitingAsylum() {
        return status == CapitalRefugeeStatus.EXILED
                && asylumCapitalId == null;
    }

    public boolean hasAsylumIn(UUID capitalId) {
        return status
                == CapitalRefugeeStatus.ASYLUM_GRANTED
                && capitalId != null
                && capitalId.equals(asylumCapitalId);
    }

    public void grantAsylum(
            UUID capitalId,
            long grantedAt
    ) {
        if (capitalId == null) {
            return;
        }

        asylumCapitalId = capitalId;
        asylumGrantedAt = Math.max(
                0L,
                grantedAt
        );

        status =
                CapitalRefugeeStatus.ASYLUM_GRANTED;
    }

    public void clearAsylum() {
        asylumCapitalId = null;
        asylumGrantedAt = 0L;
        status = CapitalRefugeeStatus.EXILED;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID(
                KEY_REFUGEE_ID,
                refugeeId
        );

        tag.putUUID(
                KEY_ORIGIN_CAPITAL_ID,
                originCapitalId
        );

        tag.putInt(
                KEY_ORIGIN_VILLAGE_ID,
                originVillageId
        );

        tag.putString(
                KEY_ORIGIN_CAPITAL_NAME,
                originCapitalName
        );

        tag.putLong(
                KEY_EXILED_AT,
                exiledAt
        );

        tag.putLong(
                KEY_ASYLUM_GRANTED_AT,
                asylumGrantedAt
        );

        tag.putString(
                KEY_STATUS,
                status.getSerializedName()
        );

        if (asylumCapitalId != null) {
            tag.putUUID(
                    KEY_ASYLUM_CAPITAL_ID,
                    asylumCapitalId
            );
        }

        return tag;
    }

    public static CapitalRefugeeRecord load(
            CompoundTag tag
    ) {
        if (tag == null
                || !tag.hasUUID(KEY_REFUGEE_ID)
                || !tag.hasUUID(
                KEY_ORIGIN_CAPITAL_ID
        )) {
            return null;
        }

        UUID asylumCapitalId =
                tag.hasUUID(KEY_ASYLUM_CAPITAL_ID)
                        ? tag.getUUID(
                        KEY_ASYLUM_CAPITAL_ID
                )
                        : null;

        return new CapitalRefugeeRecord(
                tag.getUUID(KEY_REFUGEE_ID),
                tag.getUUID(
                        KEY_ORIGIN_CAPITAL_ID
                ),
                tag.getInt(
                        KEY_ORIGIN_VILLAGE_ID
                ),
                tag.getString(
                        KEY_ORIGIN_CAPITAL_NAME
                ),
                asylumCapitalId,
                tag.getLong(KEY_EXILED_AT),
                tag.getLong(
                        KEY_ASYLUM_GRANTED_AT
                ),
                CapitalRefugeeStatus
                        .fromSerializedName(
                                tag.getString(KEY_STATUS)
                        )
        );
    }
}