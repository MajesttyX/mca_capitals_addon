package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PendingVillagerBetrothalSavedData extends SavedData {

    public static final String DATA_NAME =
            "mcacapitals_pending_villager_betrothals";

    private static final String KEY_PAIRS = "Pairs";
    private static final String KEY_FIRST = "First";
    private static final String KEY_SECOND = "Second";
    private static final String KEY_FIRST_NAME = "FirstName";
    private static final String KEY_SECOND_NAME = "SecondName";
    private static final String KEY_ORIGIN_CAPITAL = "OriginCapital";
    private static final String KEY_DESTINATION_CAPITAL = "DestinationCapital";
    private static final String KEY_RELOCATING_ROYAL = "RelocatingRoyal";
    private static final String KEY_ACCEPTED_AT = "AcceptedAt";
    private static final String KEY_COMPLETED_AT = "CompletedAt";

    private final Set<PendingPair> pairs =
            new LinkedHashSet<>();

    private final Map<PendingPair, RoyalEscortRecord>
            royalEscorts =
            new LinkedHashMap<>();

    public static PendingVillagerBetrothalSavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PendingVillagerBetrothalSavedData::load,
                        PendingVillagerBetrothalSavedData::new,
                        DATA_NAME
                );
    }

    public List<PendingPair> getPairs() {
        List<PendingPair> snapshot =
                new ArrayList<>(pairs);

        snapshot.sort(
                Comparator
                        .comparing(
                                (PendingPair pair) ->
                                        pair.first().toString()
                        )
                        .thenComparing(
                                pair ->
                                        pair.second().toString()
                        )
        );

        return snapshot;
    }

    public List<RoyalEscortRecord>
    getRoyalEscorts() {
        List<RoyalEscortRecord> snapshot =
                new ArrayList<>(royalEscorts.values());

        snapshot.sort(
                Comparator
                        .comparingLong(
                                RoyalEscortRecord::acceptedAt
                        )
                        .thenComparing(
                                record ->
                                        record.relocatingRoyalId()
                                                .toString()
                        )
        );

        return snapshot;
    }

    public boolean hasPendingBetrothal(
            UUID villagerId
    ) {
        if (villagerId == null) {
            return false;
        }

        for (PendingPair pair : pairs) {
            if (villagerId.equals(pair.first())
                    || villagerId.equals(pair.second())) {
                return true;
            }
        }

        return false;
    }

    public UUID getPartner(UUID villagerId) {
        if (villagerId == null) {
            return null;
        }

        for (PendingPair pair : pairs) {
            if (villagerId.equals(pair.first())) {
                return pair.second();
            }

            if (villagerId.equals(pair.second())) {
                return pair.first();
            }
        }

        return null;
    }

    public String getPartnerName(UUID villagerId) {
        if (villagerId == null) {
            return "";
        }

        for (RoyalEscortRecord record : royalEscorts.values()) {
            if (villagerId.equals(record.pair().first())) {
                return record.secondName();
            }

            if (villagerId.equals(record.pair().second())) {
                return record.firstName();
            }
        }

        return "";
    }

    public boolean containsPair(
            UUID firstId,
            UUID secondId
    ) {
        if (firstId == null || secondId == null) {
            return false;
        }

        return pairs.contains(
                PendingPair.of(
                        firstId,
                        secondId
                )
        );
    }

    public RoyalEscortRecord getRoyalEscort(
            UUID firstId,
            UUID secondId
    ) {
        if (firstId == null || secondId == null) {
            return null;
        }

        return royalEscorts.get(
                PendingPair.of(
                        firstId,
                        secondId
                )
        );
    }

    public void setPair(
            UUID firstId,
            UUID secondId
    ) {
        if (firstId == null
                || secondId == null
                || firstId.equals(secondId)) {
            return;
        }

        PendingPair canonical =
                PendingPair.of(
                        firstId,
                        secondId
                );

        pairs.removeIf(pair ->
                pair.first().equals(firstId)
                        || pair.second().equals(firstId)
                        || pair.first().equals(secondId)
                        || pair.second().equals(secondId)
        );

        royalEscorts.entrySet().removeIf(entry -> {
            PendingPair pair = entry.getKey();

            return pair.first().equals(firstId)
                    || pair.second().equals(firstId)
                    || pair.first().equals(secondId)
                    || pair.second().equals(secondId);
        });

        pairs.add(canonical);
        setDirty();
    }

    public void setRoyalEscort(
            UUID firstId,
            String firstName,
            UUID secondId,
            String secondName,
            UUID originCapitalId,
            UUID destinationCapitalId,
            UUID relocatingRoyalId,
            long acceptedAt
    ) {
        if (firstId == null
                || secondId == null
                || originCapitalId == null
                || destinationCapitalId == null
                || relocatingRoyalId == null
                || firstId.equals(secondId)
                || originCapitalId.equals(
                destinationCapitalId
        )
                || !relocatingRoyalId.equals(firstId)
                && !relocatingRoyalId.equals(secondId)) {
            return;
        }

        setPair(firstId, secondId);

        PendingPair pair = PendingPair.of(
                firstId,
                secondId
        );

        String canonicalFirstName = pair.first().equals(firstId)
                ? safeName(firstName)
                : safeName(secondName);
        String canonicalSecondName = pair.second().equals(secondId)
                ? safeName(secondName)
                : safeName(firstName);

        royalEscorts.put(
                pair,
                new RoyalEscortRecord(
                        pair,
                        canonicalFirstName,
                        canonicalSecondName,
                        originCapitalId,
                        destinationCapitalId,
                        relocatingRoyalId,
                        Math.max(0L, acceptedAt),
                        0L
                )
        );

        setDirty();
    }

    public boolean completeRoyalEscort(
            UUID firstId,
            UUID secondId,
            long completedAt
    ) {
        if (firstId == null || secondId == null) {
            return false;
        }

        PendingPair pair = PendingPair.of(
                firstId,
                secondId
        );

        RoyalEscortRecord current =
                royalEscorts.get(pair);

        if (current == null || current.isCompleted()) {
            return false;
        }

        royalEscorts.put(
                pair,
                new RoyalEscortRecord(
                        pair,
                        current.firstName(),
                        current.secondName(),
                        current.originCapitalId(),
                        current.destinationCapitalId(),
                        current.relocatingRoyalId(),
                        current.acceptedAt(),
                        Math.max(1L, completedAt)
                )
        );

        setDirty();
        return true;
    }

    public void removePair(
            UUID firstId,
            UUID secondId
    ) {
        if (firstId == null || secondId == null) {
            return;
        }

        PendingPair pair = PendingPair.of(
                firstId,
                secondId
        );

        boolean changed = pairs.remove(pair);
        changed |= royalEscorts.remove(pair) != null;

        if (changed) {
            setDirty();
        }
    }

    public boolean removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        Set<PendingPair> removedPairs =
                new LinkedHashSet<>();

        boolean changed = royalEscorts.entrySet().removeIf(entry -> {
            RoyalEscortRecord record = entry.getValue();
            boolean remove = capitalId.equals(
                    record.originCapitalId()
            ) || capitalId.equals(
                    record.destinationCapitalId()
            );

            if (remove) {
                removedPairs.add(entry.getKey());
            }

            return remove;
        });

        if (!removedPairs.isEmpty()) {
            changed |= pairs.removeAll(removedPairs);
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public void removeVillager(UUID villagerId) {
        if (villagerId == null) {
            return;
        }

        boolean changed = pairs.removeIf(pair ->
                villagerId.equals(pair.first())
                        || villagerId.equals(pair.second())
        );

        changed |= royalEscorts.entrySet().removeIf(entry -> {
            PendingPair pair = entry.getKey();

            return villagerId.equals(pair.first())
                    || villagerId.equals(pair.second());
        });

        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag
    ) {
        ListTag list = new ListTag();

        for (PendingPair pair : getPairs()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_FIRST, pair.first());
            entry.putUUID(KEY_SECOND, pair.second());

            RoyalEscortRecord escort =
                    royalEscorts.get(pair);

            if (escort != null) {
                if (!escort.firstName().isBlank()) {
                    entry.putString(KEY_FIRST_NAME, escort.firstName());
                }
                if (!escort.secondName().isBlank()) {
                    entry.putString(KEY_SECOND_NAME, escort.secondName());
                }

                entry.putUUID(
                        KEY_ORIGIN_CAPITAL,
                        escort.originCapitalId()
                );

                entry.putUUID(
                        KEY_DESTINATION_CAPITAL,
                        escort.destinationCapitalId()
                );

                entry.putUUID(
                        KEY_RELOCATING_ROYAL,
                        escort.relocatingRoyalId()
                );

                entry.putLong(
                        KEY_ACCEPTED_AT,
                        escort.acceptedAt()
                );

                entry.putLong(
                        KEY_COMPLETED_AT,
                        escort.completedAt()
                );
            }

            list.add(entry);
        }

        tag.put(KEY_PAIRS, list);
        return tag;
    }

    public static PendingVillagerBetrothalSavedData load(
            CompoundTag tag
    ) {
        PendingVillagerBetrothalSavedData data =
                new PendingVillagerBetrothalSavedData();

        ListTag list = tag.getList(
                KEY_PAIRS,
                Tag.TAG_COMPOUND
        );

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;

            if (!entry.hasUUID(KEY_FIRST)
                    || !entry.hasUUID(KEY_SECOND)) {
                continue;
            }

            UUID firstId = entry.getUUID(KEY_FIRST);
            UUID secondId = entry.getUUID(KEY_SECOND);

            if (firstId.equals(secondId)) {
                continue;
            }

            PendingPair pair = PendingPair.of(
                    firstId,
                    secondId
            );

            data.pairs.add(pair);

            if (entry.hasUUID(KEY_ORIGIN_CAPITAL)
                    && entry.hasUUID(
                    KEY_DESTINATION_CAPITAL
            )
                    && entry.hasUUID(
                    KEY_RELOCATING_ROYAL
            )) {
                UUID originCapitalId =
                        entry.getUUID(
                                KEY_ORIGIN_CAPITAL
                        );

                UUID destinationCapitalId =
                        entry.getUUID(
                                KEY_DESTINATION_CAPITAL
                        );

                UUID relocatingRoyalId =
                        entry.getUUID(
                                KEY_RELOCATING_ROYAL
                        );

                if (!originCapitalId.equals(
                        destinationCapitalId
                )
                        && (relocatingRoyalId.equals(
                        pair.first()
                )
                        || relocatingRoyalId.equals(
                        pair.second()
                ))) {
                    data.royalEscorts.put(
                            pair,
                            new RoyalEscortRecord(
                                    pair,
                                    safeName(entry.getString(KEY_FIRST_NAME)),
                                    safeName(entry.getString(KEY_SECOND_NAME)),
                                    originCapitalId,
                                    destinationCapitalId,
                                    relocatingRoyalId,
                                    Math.max(
                                            0L,
                                            entry.getLong(
                                                    KEY_ACCEPTED_AT
                                            )
                                    ),
                                    Math.max(
                                            0L,
                                            entry.getLong(
                                                    KEY_COMPLETED_AT
                                            )
                                    )
                            )
                    );
                }
            }
        }

        return data;
    }

    public record PendingPair(
            UUID first,
            UUID second
    ) {
        public static PendingPair of(
                UUID first,
                UUID second
        ) {
            if (first.toString()
                    .compareTo(second.toString()) <= 0) {
                return new PendingPair(first, second);
            }

            return new PendingPair(second, first);
        }
    }

    private static String safeName(String value) {
        return value == null ? "" : value.trim();
    }

    public record RoyalEscortRecord(
            PendingPair pair,
            String firstName,
            String secondName,
            UUID originCapitalId,
            UUID destinationCapitalId,
            UUID relocatingRoyalId,
            long acceptedAt,
            long completedAt
    ) {
        public RoyalEscortRecord {
            firstName = safeName(firstName);
            secondName = safeName(secondName);
        }

        public String nameFor(UUID royalId) {
            if (royalId == null) {
                return "Unknown Royal";
            }
            if (royalId.equals(pair.first())) {
                return firstName.isBlank() ? "Unknown Royal" : firstName;
            }
            if (royalId.equals(pair.second())) {
                return secondName.isBlank() ? "Unknown Royal" : secondName;
            }
            return "Unknown Royal";
        }

        public String partnerNameFor(UUID royalId) {
            if (royalId == null) {
                return "Unknown Royal";
            }
            if (royalId.equals(pair.first())) {
                return secondName.isBlank() ? "Unknown Royal" : secondName;
            }
            if (royalId.equals(pair.second())) {
                return firstName.isBlank() ? "Unknown Royal" : firstName;
            }
            return "Unknown Royal";
        }

        public boolean isCompleted() {
            return completedAt > 0L;
        }
    }
}