package com.majesttyx.mcacapitals.capital;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordNobilityOps {

    private CapitalRecordNobilityOps() {
    }

    static Set<UUID> getDukes(CapitalRecord record) {
        return record.nobility.dukes;
    }

    static Map<UUID, Boolean> getDukeFemale(CapitalRecord record) {
        return record.nobility.dukeFemale;
    }

    static boolean isDuke(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.nobility.dukes, id);
    }

    static boolean isDukeFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.nobility.dukeFemale, id);
    }

    static void addDuke(CapitalRecord record, UUID id, boolean female) {
        CapitalRecordMembers.putMember(record.nobility.dukes, record.nobility.dukeFemale, id, female);
    }

    static void removeDuke(CapitalRecord record, UUID id) {
        CapitalRecordMembers.removeMember(record.nobility.dukes, record.nobility.dukeFemale, id);
    }

    static Set<UUID> getLords(CapitalRecord record) {
        return record.nobility.lords;
    }

    static Map<UUID, Boolean> getLordFemale(CapitalRecord record) {
        return record.nobility.lordFemale;
    }

    static boolean isLord(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.nobility.lords, id);
    }

    static boolean isLordFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.nobility.lordFemale, id);
    }

    static void addLord(CapitalRecord record, UUID id, boolean female) {
        CapitalRecordMembers.putMember(record.nobility.lords, record.nobility.lordFemale, id, female);
    }

    static void removeLord(CapitalRecord record, UUID id) {
        CapitalRecordMembers.removeMember(record.nobility.lords, record.nobility.lordFemale, id);
    }

    static Set<UUID> getKnights(CapitalRecord record) {
        return record.nobility.knights;
    }

    static Map<UUID, Boolean> getKnightFemale(CapitalRecord record) {
        return record.nobility.knightFemale;
    }

    static boolean isKnight(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.nobility.knights, id);
    }

    static boolean isKnightFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.nobility.knightFemale, id);
    }

    static void addKnight(CapitalRecord record, UUID id, boolean female) {
        CapitalRecordMembers.putMember(record.nobility.knights, record.nobility.knightFemale, id, female);
    }

    static void removeKnight(CapitalRecord record, UUID id) {
        CapitalRecordMembers.removeMember(record.nobility.knights, record.nobility.knightFemale, id);
    }

    static Map<UUID, UUID> getMarriageDukeSources(CapitalRecord record) {
        return record.nobility.marriageDukeSources;
    }

    static Map<UUID, Boolean> getMarriageDukeFemale(CapitalRecord record) {
        return record.nobility.marriageDukeFemale;
    }

    static boolean isMarriageDuke(CapitalRecord record, UUID id) {
        return id != null && record.nobility.marriageDukeSources.containsKey(id);
    }

    static boolean isMarriageDukeFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.nobility.marriageDukeFemale, id);
    }

    static UUID getMarriageDukeSource(CapitalRecord record, UUID id) {
        return id == null ? null : record.nobility.marriageDukeSources.get(id);
    }

    static void setMarriageDukeSource(CapitalRecord record, UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            record.nobility.marriageDukeSources.put(holder, source);
            record.nobility.marriageDukeFemale.put(holder, female);
        }
    }

    static void removeMarriageDukeSource(CapitalRecord record, UUID holder) {
        if (holder != null) {
            record.nobility.marriageDukeSources.remove(holder);
            record.nobility.marriageDukeFemale.remove(holder);
        }
    }

    static Map<UUID, UUID> getDowagerDukeSources(CapitalRecord record) {
        return record.nobility.dowagerDukeSources;
    }

    static Map<UUID, Boolean> getDowagerDukeFemale(CapitalRecord record) {
        return record.nobility.dowagerDukeFemale;
    }

    static boolean isDowagerDuke(CapitalRecord record, UUID id) {
        return id != null && record.nobility.dowagerDukeSources.containsKey(id);
    }

    static boolean isDowagerDukeFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.nobility.dowagerDukeFemale, id);
    }

    static UUID getDowagerDukeSource(CapitalRecord record, UUID id) {
        return id == null ? null : record.nobility.dowagerDukeSources.get(id);
    }

    static void setDowagerDukeSource(CapitalRecord record, UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            record.nobility.dowagerDukeSources.put(holder, source);
            record.nobility.dowagerDukeFemale.put(holder, female);
        }
    }

    static void removeDowagerDukeSource(CapitalRecord record, UUID holder) {
        if (holder != null) {
            record.nobility.dowagerDukeSources.remove(holder);
            record.nobility.dowagerDukeFemale.remove(holder);
        }
    }
}