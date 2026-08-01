package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import com.majesttyx.mcacapitals.capital.CapitalRelationshipBand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CapitalRelationRecord {

    private static final int MAX_HISTORY = 12;

    private static final String KEY_FIRST_CAPITAL_ID =
            "FirstCapitalId";

    private static final String KEY_SECOND_CAPITAL_ID =
            "SecondCapitalId";

    private static final String KEY_SCORE = "Score";
    private static final String KEY_STATE = "State";
    private static final String KEY_TRUCE_UNTIL = "TruceUntil";
    private static final String KEY_HISTORY = "History";

    private final CapitalRelationKey key;

    private int score;

    private CapitalDiplomaticState diplomaticState;

    private long truceUntil;

    private final List<CapitalRelationshipEvent> history =
            new ArrayList<>();

    public CapitalRelationRecord(
            CapitalRelationKey key
    ) {
        this(
                key,
                0,
                CapitalDiplomaticState.PEACE,
                0L
        );
    }

    public CapitalRelationRecord(
            CapitalRelationKey key,
            int score,
            CapitalDiplomaticState diplomaticState,
            long truceUntil
    ) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "Capital relationship key cannot be null."
            );
        }

        this.key = key;
        this.score = clampScore(score);

        this.diplomaticState =
                diplomaticState == null
                        ? CapitalDiplomaticState.PEACE
                        : diplomaticState;

        this.truceUntil = Math.max(
                0L,
                truceUntil
        );
    }

    public CapitalRelationKey getKey() {
        return key;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = clampScore(score);
    }

    public int adjustScore(
            int amount,
            String reason,
            long gameDay,
            UUID initiatingCapitalId
    ) {
        return adjustScoreWithin(
                amount,
                -300,
                300,
                reason,
                gameDay,
                initiatingCapitalId
        );
    }

    public int adjustScoreWithin(
            int amount,
            int minimum,
            int maximum,
            String reason,
            long gameDay,
            UUID initiatingCapitalId
    ) {
        int lowerBound = Math.max(-300, minimum);
        int upperBound = Math.min(300, maximum);

        if (lowerBound > upperBound) {
            return 0;
        }

        int previous = score;

        score = Math.max(
                lowerBound,
                Math.min(
                        upperBound,
                        score + amount
                )
        );

        int applied = score - previous;

        if (applied != 0) {
            addHistory(
                    new CapitalRelationshipEvent(
                            applied,
                            reason,
                            gameDay,
                            initiatingCapitalId
                    )
            );
        }

        return applied;
    }

    public CapitalRelationshipBand getBand() {
        return CapitalRelationshipBand.fromScore(score);
    }

    public CapitalDiplomaticState getDiplomaticState() {
        return diplomaticState;
    }

    public void setDiplomaticState(
            CapitalDiplomaticState diplomaticState
    ) {
        this.diplomaticState =
                diplomaticState == null
                        ? CapitalDiplomaticState.PEACE
                        : diplomaticState;
    }

    public long getTruceUntil() {
        return truceUntil;
    }

    public void setTruceUntil(long truceUntil) {
        this.truceUntil = Math.max(
                0L,
                truceUntil
        );
    }

    public List<CapitalRelationshipEvent> getHistory() {
        return Collections.unmodifiableList(history);
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

        tag.putInt(KEY_SCORE, score);

        tag.putString(
                KEY_STATE,
                diplomaticState.getSerializedName()
        );

        tag.putLong(
                KEY_TRUCE_UNTIL,
                truceUntil
        );

        ListTag historyTag = new ListTag();

        for (CapitalRelationshipEvent event : history) {
            historyTag.add(event.save());
        }

        tag.put(KEY_HISTORY, historyTag);

        return tag;
    }

    public static CapitalRelationRecord load(
            CompoundTag tag
    ) {
        if (!tag.hasUUID(KEY_FIRST_CAPITAL_ID)
                || !tag.hasUUID(KEY_SECOND_CAPITAL_ID)) {
            return null;
        }

        CapitalRelationRecord record =
                new CapitalRelationRecord(
                        CapitalRelationKey.of(
                                tag.getUUID(
                                        KEY_FIRST_CAPITAL_ID
                                ),
                                tag.getUUID(
                                        KEY_SECOND_CAPITAL_ID
                                )
                        ),
                        tag.getInt(KEY_SCORE),
                        CapitalDiplomaticState
                                .fromSerializedName(
                                        tag.getString(KEY_STATE)
                                ),
                        tag.getLong(KEY_TRUCE_UNTIL)
                );

        ListTag historyTag = tag.getList(
                KEY_HISTORY,
                Tag.TAG_COMPOUND
        );

        for (Tag rawEvent : historyTag) {
            record.addHistory(
                    CapitalRelationshipEvent.load(
                            (CompoundTag) rawEvent
                    )
            );
        }

        return record;
    }

    private void addHistory(
            CapitalRelationshipEvent event
    ) {
        if (event == null) {
            return;
        }

        history.add(event);

        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    private static int clampScore(int score) {
        return Math.max(
                -300,
                Math.min(300, score)
        );
    }
}