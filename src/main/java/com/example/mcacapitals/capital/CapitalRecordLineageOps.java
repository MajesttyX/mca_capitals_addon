package com.example.mcacapitals.capital;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordLineageOps {

    private CapitalRecordLineageOps() {
    }

    static Set<UUID> getRoyalChildren(CapitalRecord record) {
        return record.lineage.royalChildren;
    }

    static Map<UUID, Boolean> getRoyalChildFemale(CapitalRecord record) {
        return record.lineage.royalChildFemale;
    }

    static boolean isRoyalChild(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.lineage.royalChildren, id);
    }

    static boolean isRoyalChildFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.lineage.royalChildFemale, id);
    }

    static Set<UUID> getRoyalHousehold(CapitalRecord record) {
        return record.lineage.royalHousehold;
    }

    static boolean isRoyalHouseholdMember(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.lineage.royalHousehold, id);
    }

    static void addRoyalHouseholdMember(CapitalRecord record, UUID id) {
        if (id != null) {
            record.lineage.royalHousehold.add(id);
        }
    }

    static void removeRoyalHouseholdMember(CapitalRecord record, UUID id) {
        if (id != null) {
            record.lineage.royalHousehold.remove(id);
        }
    }

    static void clearRoyalHousehold(CapitalRecord record) {
        record.lineage.royalHousehold.clear();
    }

    static void addRoyalChild(CapitalRecord record, UUID id) {
        addRoyalChild(record, id, false);
    }

    static void addRoyalChild(CapitalRecord record, UUID id, boolean female) {
        if (id != null) {
            CapitalRecordMembers.putMember(record.lineage.royalChildren, record.lineage.royalChildFemale, id, female);
            record.lineage.disinheritedRoyalChildren.remove(id);
        }
    }

    static void removeRoyalChild(CapitalRecord record, UUID id) {
        CapitalRecordMembers.removeMember(record.lineage.royalChildren, record.lineage.royalChildFemale, id);
    }

    static Set<UUID> getDisinheritedRoyalChildren(CapitalRecord record) {
        return record.lineage.disinheritedRoyalChildren;
    }

    static void addDisinheritedRoyalChild(CapitalRecord record, UUID id) {
        if (id != null) {
            record.lineage.disinheritedRoyalChildren.add(id);
            removeRoyalChild(record, id);
            removeLegitimizedRoyalChild(record, id);
            record.lineage.royalSuccessionOrder.remove(id);
            if (id.equals(record.identity.heir)) {
                record.identity.heir = null;
                record.identity.heirFemale = false;
            }
        }
    }

    static void disinheritRoyalChild(CapitalRecord record, UUID id) {
        addDisinheritedRoyalChild(record, id);
    }

    static void removeDisinheritedRoyalChild(CapitalRecord record, UUID id) {
        if (id != null) {
            record.lineage.disinheritedRoyalChildren.remove(id);
        }
    }

    static boolean isDisinheritedRoyalChild(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.lineage.disinheritedRoyalChildren, id);
    }

    static Set<UUID> getLegitimizedRoyalChildren(CapitalRecord record) {
        return record.lineage.legitimizedRoyalChildren;
    }

    static Map<UUID, Boolean> getLegitimizedRoyalChildFemale(CapitalRecord record) {
        return record.lineage.legitimizedRoyalChildFemale;
    }

    static void addLegitimizedRoyalChild(CapitalRecord record, UUID id) {
        addLegitimizedRoyalChild(record, id, false);
    }

    static void addLegitimizedRoyalChild(CapitalRecord record, UUID id, boolean female) {
        CapitalRecordMembers.putMember(record.lineage.legitimizedRoyalChildren, record.lineage.legitimizedRoyalChildFemale, id, female);
        record.lineage.disinheritedRoyalChildren.remove(id);
    }

    static void removeLegitimizedRoyalChild(CapitalRecord record, UUID id) {
        CapitalRecordMembers.removeMember(record.lineage.legitimizedRoyalChildren, record.lineage.legitimizedRoyalChildFemale, id);
    }

    static boolean isLegitimizedRoyalChild(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.lineage.legitimizedRoyalChildren, id);
    }

    static List<UUID> getRoyalSuccessionOrder(CapitalRecord record) {
        return record.lineage.royalSuccessionOrder;
    }

    static void setRoyalSuccessionOrder(CapitalRecord record, List<UUID> order) {
        record.lineage.royalSuccessionOrder.clear();
        if (order != null) {
            record.lineage.royalSuccessionOrder.addAll(order);
        }
    }

    static Map<UUID, UUID> getPrinceConsortSources(CapitalRecord record) {
        return record.lineage.princeConsortSources;
    }

    static Map<UUID, Boolean> getPrinceConsortFemale(CapitalRecord record) {
        return record.lineage.princeConsortFemale;
    }

    static boolean isPrinceConsort(CapitalRecord record, UUID id) {
        return id != null && record.lineage.princeConsortSources.containsKey(id);
    }

    static boolean isPrinceConsortFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.lineage.princeConsortFemale, id);
    }

    static UUID getPrinceConsortSource(CapitalRecord record, UUID id) {
        return id == null ? null : record.lineage.princeConsortSources.get(id);
    }

    static void setPrinceConsortSource(CapitalRecord record, UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            record.lineage.princeConsortSources.put(holder, source);
            record.lineage.princeConsortFemale.put(holder, female);
        }
    }

    static void removePrinceConsortSource(CapitalRecord record, UUID holder) {
        if (holder != null) {
            record.lineage.princeConsortSources.remove(holder);
            record.lineage.princeConsortFemale.remove(holder);
        }
    }

    static Map<UUID, UUID> getDowagerPrinceSources(CapitalRecord record) {
        return record.lineage.dowagerPrinceSources;
    }

    static Map<UUID, Boolean> getDowagerPrinceFemale(CapitalRecord record) {
        return record.lineage.dowagerPrinceFemale;
    }

    static boolean isDowagerPrince(CapitalRecord record, UUID id) {
        return id != null && record.lineage.dowagerPrinceSources.containsKey(id);
    }

    static boolean isDowagerPrinceFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.lineage.dowagerPrinceFemale, id);
    }

    static UUID getDowagerPrinceSource(CapitalRecord record, UUID id) {
        return id == null ? null : record.lineage.dowagerPrinceSources.get(id);
    }

    static void setDowagerPrinceSource(CapitalRecord record, UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            record.lineage.dowagerPrinceSources.put(holder, source);
            record.lineage.dowagerPrinceFemale.put(holder, female);
        }
    }

    static void removeDowagerPrinceSource(CapitalRecord record, UUID holder) {
        if (holder != null) {
            record.lineage.dowagerPrinceSources.remove(holder);
            record.lineage.dowagerPrinceFemale.remove(holder);
        }
    }
}