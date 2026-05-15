package com.majesttyx.mcacapitals.capital;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordMembers {

    private CapitalRecordMembers() {
    }

    static boolean containsMember(Set<UUID> members, UUID id) {
        return id != null && members.contains(id);
    }

    static boolean isFlaggedFemale(Map<UUID, Boolean> femaleFlags, UUID id) {
        return id != null && femaleFlags.getOrDefault(id, false);
    }

    static void putMember(Set<UUID> members, Map<UUID, Boolean> femaleFlags, UUID id, boolean female) {
        if (id != null) {
            members.add(id);
            femaleFlags.put(id, female);
        }
    }

    static void removeMember(Set<UUID> members, Map<UUID, Boolean> femaleFlags, UUID id) {
        if (id != null) {
            members.remove(id);
            femaleFlags.remove(id);
        }
    }

    static void replaceMembers(Set<UUID> members, Map<UUID, Boolean> femaleFlags, Set<UUID> replacements, Map<UUID, Boolean> replacementFemaleFlags) {
        members.clear();
        femaleFlags.clear();
        if (replacements == null) {
            return;
        }
        for (UUID id : replacements) {
            if (id != null) {
                members.add(id);
                femaleFlags.put(id, replacementFemaleFlags.getOrDefault(id, false));
            }
        }
    }

    static boolean resolveHeirFemale(UUID heir, Map<UUID, Boolean> royalChildFemale) {
        return heir != null && royalChildFemale.getOrDefault(heir, false);
    }
}