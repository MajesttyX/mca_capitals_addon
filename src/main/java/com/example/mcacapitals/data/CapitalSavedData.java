package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalSavedData extends SavedData {

    public static final String DATA_NAME = "mca_capitals";

    public CapitalSavedData() {
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag capitalsTag = new ListTag();

        for (CapitalRecord capital : CapitalManager.getAllCapitals().values()) {
            CompoundTag capitalTag = new CompoundTag();

            capitalTag.putUUID("CapitalId", capital.getCapitalId());

            if (capital.getVillageId() != null) {
                capitalTag.putInt("VillageId", capital.getVillageId());
            }

            if (capital.getSovereign() != null) {
                capitalTag.putUUID("Sovereign", capital.getSovereign());
            }
            capitalTag.putBoolean("SovereignFemale", capital.isSovereignFemale());

            if (capital.getConsort() != null) {
                capitalTag.putUUID("Consort", capital.getConsort());
            }
            capitalTag.putBoolean("ConsortFemale", capital.isConsortFemale());

            if (capital.getDowager() != null) {
                capitalTag.putUUID("Dowager", capital.getDowager());
            }
            capitalTag.putBoolean("DowagerFemale", capital.isDowagerFemale());

            if (capital.getHeir() != null) {
                capitalTag.putUUID("Heir", capital.getHeir());
            }

            capitalTag.putBoolean("MonarchyRejected", capital.isMonarchyRejected());
            capitalTag.putString("State", capital.getState().name());
            capitalTag.put("ChronicleEntries", writeStringList(capital.getChronicleEntries()));
            capitalTag.put("RoyalChildren", writeUuidSet(capital.getRoyalChildren()));
            capitalTag.put("RoyalSuccessionOrder", writeUuidList(capital.getRoyalSuccessionOrder()));
            capitalTag.put("LegitimizedRoyalChildren", writeUuidSet(capital.getLegitimizedRoyalChildren()));
            capitalTag.put("DisinheritedRoyalChildren", writeUuidSet(capital.getDisinheritedRoyalChildren()));
            capitalTag.put("Dukes", writeUuidSet(capital.getDukes()));
            capitalTag.put("Lords", writeUuidSet(capital.getLords()));
            capitalTag.put("Knights", writeUuidSet(capital.getKnights()));

            capitalTag.put("RoyalChildFemale", writeUuidBooleanMap(capital.getRoyalChildFemale()));
            capitalTag.put("DukeFemale", writeUuidBooleanMap(capital.getDukeFemale()));
            capitalTag.put("LordFemale", writeUuidBooleanMap(capital.getLordFemale()));
            capitalTag.put("KnightFemale", writeUuidBooleanMap(capital.getKnightFemale()));

            capitalsTag.add(capitalTag);
        }

        tag.put("Capitals", capitalsTag);
        return tag;
    }

    public static CapitalSavedData load(CompoundTag tag) {
        CapitalSavedData data = new CapitalSavedData();
        CapitalManager.getAllCapitals().clear();

        ListTag capitalsTag = tag.getList("Capitals", Tag.TAG_COMPOUND);
        for (int i = 0; i < capitalsTag.size(); i++) {
            CompoundTag capitalTag = capitalsTag.getCompound(i);

            UUID capitalId = capitalTag.getUUID("CapitalId");
            Integer villageId = capitalTag.contains("VillageId", Tag.TAG_INT) ? capitalTag.getInt("VillageId") : null;
            UUID sovereign = capitalTag.hasUUID("Sovereign") ? capitalTag.getUUID("Sovereign") : null;
            boolean sovereignFemale = capitalTag.getBoolean("SovereignFemale");

            CapitalRecord capital = new CapitalRecord(capitalId, villageId, sovereign, sovereignFemale);

            if (capitalTag.hasUUID("Consort")) {
                capital.setConsort(capitalTag.getUUID("Consort"));
            }
            capital.setConsortFemale(capitalTag.getBoolean("ConsortFemale"));

            if (capitalTag.hasUUID("Dowager")) {
                capital.setDowager(capitalTag.getUUID("Dowager"));
            }
            capital.setDowagerFemale(capitalTag.getBoolean("DowagerFemale"));

            if (capitalTag.hasUUID("Heir")) {
                capital.setHeir(capitalTag.getUUID("Heir"));
            }

            capital.setMonarchyRejected(capitalTag.getBoolean("MonarchyRejected"));

            if (capitalTag.contains("State", Tag.TAG_STRING)) {
                try {
                    capital.setState(CapitalState.valueOf(capitalTag.getString("State")));
                } catch (IllegalArgumentException ignored) {
                    capital.setState(CapitalState.PENDING);
                }
            }

            readStringList(capitalTag.getList("ChronicleEntries", Tag.TAG_STRING), capital.getChronicleEntries());
            readUuidSet(capitalTag.getList("RoyalChildren", Tag.TAG_STRING), capital.getRoyalChildren());
            readUuidList(capitalTag.getList("RoyalSuccessionOrder", Tag.TAG_STRING), capital.getRoyalSuccessionOrder());
            readUuidSet(capitalTag.getList("LegitimizedRoyalChildren", Tag.TAG_STRING), capital.getLegitimizedRoyalChildren());
            readUuidSet(capitalTag.getList("DisinheritedRoyalChildren", Tag.TAG_STRING), capital.getDisinheritedRoyalChildren());
            readUuidSet(capitalTag.getList("Dukes", Tag.TAG_STRING), capital.getDukes());
            readUuidSet(capitalTag.getList("Lords", Tag.TAG_STRING), capital.getLords());
            readUuidSet(capitalTag.getList("Knights", Tag.TAG_STRING), capital.getKnights());

            readUuidBooleanMap(capitalTag.getList("RoyalChildFemale", Tag.TAG_COMPOUND), capital.getRoyalChildFemale());
            readUuidBooleanMap(capitalTag.getList("DukeFemale", Tag.TAG_COMPOUND), capital.getDukeFemale());
            readUuidBooleanMap(capitalTag.getList("LordFemale", Tag.TAG_COMPOUND), capital.getLordFemale());
            readUuidBooleanMap(capitalTag.getList("KnightFemale", Tag.TAG_COMPOUND), capital.getKnightFemale());

            capital.getRoyalChildren().removeIf(capital.getDisinheritedRoyalChildren()::contains);
            capital.getRoyalSuccessionOrder().removeIf(id -> !capital.getRoyalChildren().contains(id));
            for (UUID childId : capital.getRoyalChildren()) {
                if (!capital.getRoyalSuccessionOrder().contains(childId)) {
                    capital.getRoyalSuccessionOrder().add(childId);
                }
            }
            capital.getLegitimizedRoyalChildren().retainAll(capital.getRoyalChildren());

            CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        }

        return data;
    }

    private static ListTag writeUuidSet(Set<UUID> uuids) {
        ListTag listTag = new ListTag();
        for (UUID uuid : uuids) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        return listTag;
    }

    private static ListTag writeUuidList(Iterable<UUID> uuids) {
        ListTag listTag = new ListTag();
        for (UUID uuid : uuids) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        return listTag;
    }

    private static ListTag writeStringList(Iterable<String> values) {
        ListTag listTag = new ListTag();
        for (String value : values) {
            listTag.add(StringTag.valueOf(value));
        }
        return listTag;
    }

    private static void readUuidSet(ListTag listTag, Set<UUID> destination) {
        destination.clear();

        for (int i = 0; i < listTag.size(); i++) {
            String raw = listTag.getString(i);
            try {
                destination.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readUuidList(ListTag listTag, java.util.List<UUID> destination) {
        destination.clear();

        for (int i = 0; i < listTag.size(); i++) {
            String raw = listTag.getString(i);
            try {
                destination.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readStringList(ListTag listTag, java.util.List<String> destination) {
        destination.clear();

        for (int i = 0; i < listTag.size(); i++) {
            destination.add(listTag.getString(i));
        }
    }

    private static ListTag writeUuidBooleanMap(Map<UUID, Boolean> map) {
        ListTag listTag = new ListTag();

        for (Map.Entry<UUID, Boolean> entry : map.entrySet()) {
            CompoundTag pair = new CompoundTag();
            pair.putUUID("Id", entry.getKey());
            pair.putBoolean("Value", entry.getValue());
            listTag.add(pair);
        }

        return listTag;
    }

    private static void readUuidBooleanMap(ListTag listTag, Map<UUID, Boolean> destination) {
        destination.clear();

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag pair = listTag.getCompound(i);
            if (pair.hasUUID("Id")) {
                destination.put(pair.getUUID("Id"), pair.getBoolean("Value"));
            }
        }
    }
}