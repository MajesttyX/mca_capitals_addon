package com.majesttyx.mcacapitals.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class MCAResidentStateSavedData extends SavedData {

    private static final String DATA_NAME = "mcacapitals_mca_resident_state";
    private static final String KEY_RESIDENTS = "Residents";
    private static final String KEY_VILLAGER = "Villager";
    private static final String KEY_PROFESSION = "Profession";
    private static final String KEY_PROFESSION_LEVEL = "ProfessionLevel";
    private static final String KEY_AGE_STATE = "AgeState";
    private static final String KEY_HEARTS = "Hearts";
    private static final String KEY_PENDING_HEART_DELTAS = "PendingHeartDeltas";
    private static final String KEY_PLAYER = "Player";
    private static final String KEY_VALUE = "Value";

    private final Map<UUID, ResidentState> residents = new HashMap<>();

    static MCAResidentStateSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(MCAResidentStateSavedData::load, MCAResidentStateSavedData::new, DATA_NAME);
    }

    static MCAResidentStateSavedData load(CompoundTag tag) {
        MCAResidentStateSavedData data = new MCAResidentStateSavedData();
        ListTag residentList = tag.getList(KEY_RESIDENTS, Tag.TAG_COMPOUND);

        for (int i = 0; i < residentList.size(); i++) {
            CompoundTag residentTag = residentList.getCompound(i);
            if (!residentTag.hasUUID(KEY_VILLAGER)) {
                continue;
            }

            UUID villagerId = residentTag.getUUID(KEY_VILLAGER);
            String profession = residentTag.getString(KEY_PROFESSION);
            int professionLevel = residentTag.getInt(KEY_PROFESSION_LEVEL);
            String ageState = residentTag.contains(KEY_AGE_STATE, Tag.TAG_STRING)
                    ? residentTag.getString(KEY_AGE_STATE)
                    : "UNASSIGNED";

            Map<UUID, Integer> hearts = readUuidIntMap(residentTag.getList(KEY_HEARTS, Tag.TAG_COMPOUND));
            Map<UUID, Integer> pendingHeartDeltas = readUuidIntMap(
                    residentTag.getList(KEY_PENDING_HEART_DELTAS, Tag.TAG_COMPOUND)
            );

            data.residents.put(
                    villagerId,
                    new ResidentState(profession, professionLevel, ageState, hearts, pendingHeartDeltas)
            );
        }

        return data;
    }

    private static Map<UUID, Integer> readUuidIntMap(ListTag list) {
        Map<UUID, Integer> values = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID(KEY_PLAYER)) {
                values.put(entry.getUUID(KEY_PLAYER), entry.getInt(KEY_VALUE));
            }
        }
        return values;
    }

    Optional<Integer> getProfessionLevel(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        return state == null ? Optional.empty() : Optional.of(state.professionLevel());
    }

    Optional<String> getProfession(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        if (state == null || state.profession().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(state.profession());
    }

    Optional<String> getAgeState(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        if (state == null || state.ageState().isBlank() || "UNASSIGNED".equalsIgnoreCase(state.ageState())) {
            return Optional.empty();
        }
        return Optional.of(state.ageState());
    }

    Optional<Integer> getHearts(UUID villagerId, UUID playerId) {
        if (villagerId == null || playerId == null) {
            return Optional.empty();
        }

        ResidentState state = residents.get(villagerId);
        if (state == null || !state.hearts().containsKey(playerId)) {
            return Optional.empty();
        }
        return Optional.of(state.hearts().get(playerId));
    }

    Map<UUID, Integer> getPendingHeartDeltas(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        if (state == null || state.pendingHeartDeltas().isEmpty()) {
            return Collections.emptyMap();
        }
        return new HashMap<>(state.pendingHeartDeltas());
    }

    void update(
            UUID villagerId,
            String profession,
            int professionLevel,
            String ageState,
            Map<UUID, Integer> hearts
    ) {
        if (villagerId == null) {
            return;
        }

        ResidentState previous = residents.get(villagerId);
        Map<UUID, Integer> resolvedHearts = hearts != null
                ? hearts
                : previous == null ? Collections.emptyMap() : previous.hearts();
        Map<UUID, Integer> pending = previous == null
                ? Collections.emptyMap()
                : previous.pendingHeartDeltas();

        ResidentState replacement = new ResidentState(
                profession == null ? "" : profession,
                professionLevel,
                ageState == null ? "UNASSIGNED" : ageState,
                resolvedHearts,
                pending
        );

        residents.put(villagerId, replacement);
        if (!replacement.equals(previous)) {
            setDirty();
        }
    }

    void queueHeartDelta(UUID villagerId, UUID playerId, int delta) {
        if (villagerId == null || playerId == null || delta == 0) {
            return;
        }

        ResidentState previous = residents.get(villagerId);
        String profession = previous == null ? "" : previous.profession();
        int professionLevel = previous == null ? 0 : previous.professionLevel();
        String ageState = previous == null ? "UNASSIGNED" : previous.ageState();

        Map<UUID, Integer> hearts = previous == null
                ? new HashMap<>()
                : new HashMap<>(previous.hearts());
        if (hearts.containsKey(playerId)) {
            hearts.put(playerId, hearts.get(playerId) + delta);
        }

        Map<UUID, Integer> pending = previous == null
                ? new HashMap<>()
                : new HashMap<>(previous.pendingHeartDeltas());
        pending.merge(playerId, delta, Integer::sum);
        if (pending.getOrDefault(playerId, 0) == 0) {
            pending.remove(playerId);
        }

        ResidentState replacement = new ResidentState(
                profession,
                professionLevel,
                ageState,
                hearts,
                pending
        );
        residents.put(villagerId, replacement);
        if (!replacement.equals(previous)) {
            setDirty();
        }
    }

    void acknowledgeHeartDelta(UUID villagerId, UUID playerId) {
        ResidentState previous = residents.get(villagerId);
        if (previous == null || playerId == null || !previous.pendingHeartDeltas().containsKey(playerId)) {
            return;
        }

        Map<UUID, Integer> pending = new HashMap<>(previous.pendingHeartDeltas());
        pending.remove(playerId);
        residents.put(villagerId, new ResidentState(
                previous.profession(),
                previous.professionLevel(),
                previous.ageState(),
                previous.hearts(),
                pending
        ));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag residentList = new ListTag();

        for (Map.Entry<UUID, ResidentState> entry : residents.entrySet()) {
            CompoundTag residentTag = new CompoundTag();
            residentTag.putUUID(KEY_VILLAGER, entry.getKey());

            ResidentState state = entry.getValue();
            residentTag.putString(KEY_PROFESSION, state.profession());
            residentTag.putInt(KEY_PROFESSION_LEVEL, state.professionLevel());
            residentTag.putString(KEY_AGE_STATE, state.ageState());
            residentTag.put(KEY_HEARTS, writeUuidIntMap(state.hearts()));
            residentTag.put(KEY_PENDING_HEART_DELTAS, writeUuidIntMap(state.pendingHeartDeltas()));
            residentList.add(residentTag);
        }

        tag.put(KEY_RESIDENTS, residentList);
        return tag;
    }

    private static ListTag writeUuidIntMap(Map<UUID, Integer> values) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> entry : values.entrySet()) {
            CompoundTag valueTag = new CompoundTag();
            valueTag.putUUID(KEY_PLAYER, entry.getKey());
            valueTag.putInt(KEY_VALUE, entry.getValue());
            list.add(valueTag);
        }
        return list;
    }

    private record ResidentState(
            String profession,
            int professionLevel,
            String ageState,
            Map<UUID, Integer> hearts,
            Map<UUID, Integer> pendingHeartDeltas
    ) {
        private ResidentState {
            profession = profession == null ? "" : profession;
            ageState = ageState == null || ageState.isBlank() ? "UNASSIGNED" : ageState;
            hearts = hearts == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(hearts));
            pendingHeartDeltas = pendingHeartDeltas == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(pendingHeartDeltas));
        }
    }
}
