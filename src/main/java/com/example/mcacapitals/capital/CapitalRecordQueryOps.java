package com.example.mcacapitals.capital;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordQueryOps {

    private CapitalRecordQueryOps() {
    }

    static boolean containsEntity(CapitalRecord record, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        return hasCourtIdentity(record, entityId)
                || hasRoyalHouseholdState(record, entityId)
                || hasRoyalChildState(record, entityId)
                || hasNobleOffice(record, entityId)
                || hasGuardOffice(record, entityId)
                || hasCommanderState(record, entityId)
                || hasHandState(record, entityId)
                || hasHeraldState(record, entityId)
                || hasGrandMaesterState(record, entityId)
                || hasPlayerCourtState(record, entityId);
    }

    static void replaceDynamicRoles(
            CapitalRecord record,
            UUID sovereign, boolean sovereignFemale,
            UUID heir,
            Set<UUID> royalChildren, Map<UUID, Boolean> royalChildFemale,
            Set<UUID> dukes, Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords, Map<UUID, Boolean> lordFemale,
            Set<UUID> knights, Map<UUID, Boolean> knightFemale
    ) {
        record.identity.sovereign = sovereign;
        record.identity.sovereignFemale = sovereignFemale;
        record.identity.heir = heir;
        record.identity.heirFemale = CapitalRecordMembers.resolveHeirFemale(heir, royalChildFemale);

        CapitalRecordMembers.replaceMembers(record.lineage.royalChildren, record.lineage.royalChildFemale, royalChildren, royalChildFemale);
        CapitalRecordMembers.replaceMembers(record.nobility.dukes, record.nobility.dukeFemale, dukes, dukeFemale);
        CapitalRecordMembers.replaceMembers(record.nobility.lords, record.nobility.lordFemale, lords, lordFemale);
        CapitalRecordMembers.replaceMembers(record.nobility.knights, record.nobility.knightFemale, knights, knightFemale);
    }

    private static boolean hasCourtIdentity(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.identity.sovereign)
                || entityId.equals(record.identity.consort)
                || entityId.equals(record.identity.dowager)
                || entityId.equals(record.identity.heir);
    }

    private static boolean hasRoyalHouseholdState(CapitalRecord record, UUID entityId) {
        return record.lineage.royalHousehold.contains(entityId);
    }

    private static boolean hasRoyalChildState(CapitalRecord record, UUID entityId) {
        return record.lineage.royalChildren.contains(entityId)
                || record.lineage.disinheritedRoyalChildren.contains(entityId)
                || record.lineage.legitimizedRoyalChildren.contains(entityId)
                || record.lineage.princeConsortSources.containsKey(entityId)
                || record.lineage.dowagerPrinceSources.containsKey(entityId);
    }

    private static boolean hasNobleOffice(CapitalRecord record, UUID entityId) {
        return record.nobility.dukes.contains(entityId)
                || record.nobility.marriageDukeSources.containsKey(entityId)
                || record.nobility.dowagerDukeSources.containsKey(entityId)
                || record.nobility.lords.contains(entityId)
                || record.nobility.knights.contains(entityId);
    }

    private static boolean hasGuardOffice(CapitalRecord record, UUID entityId) {
        return record.guard.royalGuards.contains(entityId)
                || record.guard.disgracedRoyalGuards.contains(entityId);
    }

    private static boolean hasCommanderState(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.court.commander);
    }

    private static boolean hasHandState(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.court.hand);
    }

    private static boolean hasHeraldState(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.court.herald);
    }

    private static boolean hasGrandMaesterState(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.court.grandMaester);
    }

    private static boolean hasPlayerCourtState(CapitalRecord record, UUID entityId) {
        return entityId.equals(record.court.playerSovereignId)
                || entityId.equals(record.court.playerConsortId);
    }
}