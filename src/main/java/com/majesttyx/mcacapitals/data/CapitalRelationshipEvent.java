package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record CapitalRelationshipEvent(
        int amount,
        String reason,
        long gameDay,
        UUID initiatingCapitalId
) {

    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_REASON = "Reason";
    private static final String KEY_GAME_DAY = "GameDay";
    private static final String KEY_INITIATING_CAPITAL_ID =
            "InitiatingCapitalId";

    public CapitalRelationshipEvent {
        reason = reason == null ? "" : reason;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putInt(KEY_AMOUNT, amount);
        tag.putString(KEY_REASON, reason);
        tag.putLong(KEY_GAME_DAY, gameDay);

        if (initiatingCapitalId != null) {
            tag.putUUID(
                    KEY_INITIATING_CAPITAL_ID,
                    initiatingCapitalId
            );
        }

        return tag;
    }

    public static CapitalRelationshipEvent load(
            CompoundTag tag
    ) {
        UUID initiatingCapitalId =
                tag.hasUUID(KEY_INITIATING_CAPITAL_ID)
                        ? tag.getUUID(KEY_INITIATING_CAPITAL_ID)
                        : null;

        return new CapitalRelationshipEvent(
                tag.getInt(KEY_AMOUNT),
                tag.getString(KEY_REASON),
                tag.getLong(KEY_GAME_DAY),
                initiatingCapitalId
        );
    }
}