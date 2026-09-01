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
    private static final String KEY_HEARTS = "Hearts";
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

            Map<UUID, Integer> hearts = new HashMap<>();
            ListTag heartList = residentTag.getList(KEY_HEARTS, Tag.TAG_COMPOUND);
            for (int h = 0; h < heartList.size(); h++) {
                CompoundTag heartTag = heartList.getCompound(h);
                if (heartTag.hasUUID(KEY_PLAYER)) {
                    hearts.put(heartTag.getUUID(KEY_PLAYER), heartTag.getInt(KEY_VALUE));
                }
            }

            data.residents.put(
                    villagerId,
                    new ResidentState(profession, professionLevel, hearts)
            );
        }

        return data;
    }

    Optional<Integer> getProfessionLevel(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        return state == null ? Optional.empty() : Optional.of(state.professionLevel());
    }

    Optional<String> getProfession(UUID villagerId) {
        ResidentState state = residents.get(villagerId);
        if (state == null || state.profession() == null || state.profession().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(state.profession());
    }

    Optional<Integer> getHearts(UUID villagerId, UUID playerId) {
        if (villagerId == null || playerId == null) {
            return Optional.empty();
        }

        ResidentState state = residents.get(villagerId);
        if (state == null) {
            return Optional.empty();
        }

        return Optional.of(state.hearts().getOrDefault(playerId, 0));
    }

    void update(
            UUID villagerId,
            String profession,
            int professionLevel,
            Map<UUID, Integer> hearts
    ) {
        if (villagerId == null) {
            return;
        }

        ResidentState previous = residents.get(villagerId);
        Map<UUID, Integer> resolvedHearts = hearts != null
                ? hearts
                : previous == null ? Collections.emptyMap() : previous.hearts();

        ResidentState replacement = new ResidentState(
                profession == null ? "" : profession,
                professionLevel,
                resolvedHearts
        );

        residents.put(villagerId, replacement);
        if (!replacement.equals(previous)) {
            setDirty();
        }
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

            ListTag heartList = new ListTag();
            for (Map.Entry<UUID, Integer> heartEntry : state.hearts().entrySet()) {
                CompoundTag heartTag = new CompoundTag();
                heartTag.putUUID(KEY_PLAYER, heartEntry.getKey());
                heartTag.putInt(KEY_VALUE, heartEntry.getValue());
                heartList.add(heartTag);
            }
            residentTag.put(KEY_HEARTS, heartList);

            residentList.add(residentTag);
        }

        tag.put(KEY_RESIDENTS, residentList);
        return tag;
    }

    private record ResidentState(
            String profession,
            int professionLevel,
            Map<UUID, Integer> hearts
    ) {
        private ResidentState {
            profession = profession == null ? "" : profession;
            hearts = hearts == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(hearts));
        }
    }
}
