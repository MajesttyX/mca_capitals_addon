package com.example.mcacapitals.data;

import com.example.mcacapitals.noble.NobleRecord;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.util.ModDataKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NobleSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_nobles";

    private final Map<UUID, NobleRecord> nobles = new HashMap<>();

    public Map<UUID, NobleRecord> getNobles() {
        return nobles;
    }

    public NobleRecord get(UUID villagerId) {
        return nobles.get(villagerId);
    }

    public NobleRecord getOrCreate(UUID villagerId) {
        setDirty();
        return nobles.computeIfAbsent(villagerId, id -> new NobleRecord(id, NobleTitle.COMMONER));
    }

    public static NobleSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NobleSavedData::load,
                NobleSavedData::new,
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag nobleList = new ListTag();

        for (NobleRecord record : nobles.values()) {
            CompoundTag nobleTag = new CompoundTag();

            nobleTag.putUUID(ModDataKeys.VILLAGER_ID, record.getVillagerId());
            nobleTag.putString(ModDataKeys.DIRECT_TITLE, record.getDirectTitle().name());

            if (record.getCapitalId() != null) {
                nobleTag.putUUID(ModDataKeys.CAPITAL_ID, record.getCapitalId());
            }

            nobleTag.putBoolean(ModDataKeys.TITLE_GRANTED_BY_MARRIAGE, record.isTitleGrantedByMarriage());
            nobleTag.putInt(ModDataKeys.SUCCESSION_ORDER, record.getSuccessionOrder());

            nobleList.add(nobleTag);
        }

        tag.put(ModDataKeys.NOBLES, nobleList);
        return tag;
    }

    public static NobleSavedData load(CompoundTag tag) {
        NobleSavedData data = new NobleSavedData();

        ListTag nobleList = tag.getList(ModDataKeys.NOBLES, Tag.TAG_COMPOUND);
        for (Tag rawTag : nobleList) {
            CompoundTag nobleTag = (CompoundTag) rawTag;

            UUID villagerId = nobleTag.getUUID(ModDataKeys.VILLAGER_ID);
            NobleTitle title = NobleTitle.valueOf(nobleTag.getString(ModDataKeys.DIRECT_TITLE));

            NobleRecord record = new NobleRecord(villagerId, title);

            if (nobleTag.hasUUID(ModDataKeys.CAPITAL_ID)) {
                record.setCapitalId(nobleTag.getUUID(ModDataKeys.CAPITAL_ID));
            }

            record.setTitleGrantedByMarriage(nobleTag.getBoolean(ModDataKeys.TITLE_GRANTED_BY_MARRIAGE));
            record.setSuccessionOrder(nobleTag.getInt(ModDataKeys.SUCCESSION_ORDER));

            data.nobles.put(villagerId, record);
        }

        return data;
    }
}