package com.example.mcacapitals.capital;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalRecord {

    private final UUID capitalId;
    private Integer villageId;

    private UUID sovereign;
    private boolean sovereignFemale;

    private UUID consort;
    private boolean consortFemale;

    private UUID dowager;
    private boolean dowagerFemale;

    private UUID heir;

    private boolean monarchyRejected;

    private final List<String> chronicleEntries = new ArrayList<>();

    private final Set<UUID> royalChildren = new LinkedHashSet<>();
    private final List<UUID> royalSuccessionOrder = new ArrayList<>();
    private final Set<UUID> legitimizedRoyalChildren = new LinkedHashSet<>();
    private final Set<UUID> disinheritedRoyalChildren = new LinkedHashSet<>();

    private final Set<UUID> dukes = new LinkedHashSet<>();
    private final Set<UUID> lords = new LinkedHashSet<>();
    private final Set<UUID> knights = new LinkedHashSet<>();

    private final Map<UUID, Boolean> royalChildFemale = new HashMap<>();
    private final Map<UUID, Boolean> dukeFemale = new HashMap<>();
    private final Map<UUID, Boolean> lordFemale = new HashMap<>();
    private final Map<UUID, Boolean> knightFemale = new HashMap<>();

    private CapitalState state;

    public CapitalRecord(UUID capitalId, UUID sovereign, boolean sovereignFemale) {
        this(capitalId, null, sovereign, sovereignFemale);
    }

    public CapitalRecord(UUID capitalId, Integer villageId, UUID sovereign, boolean sovereignFemale) {
        this.capitalId = capitalId;
        this.villageId = villageId;
        this.sovereign = sovereign;
        this.sovereignFemale = sovereignFemale;
        this.state = CapitalState.PENDING;
        this.monarchyRejected = false;
    }

    public synchronized UUID getCapitalId() {
        return capitalId;
    }

    public synchronized Integer getVillageId() {
        return villageId;
    }

    public synchronized void setVillageId(Integer villageId) {
        this.villageId = villageId;
    }

    public synchronized UUID getSovereign() {
        return sovereign;
    }

    public synchronized void setSovereign(UUID sovereign) {
        this.sovereign = sovereign;
    }

    public synchronized boolean isSovereignFemale() {
        return sovereignFemale;
    }

    public synchronized void setSovereignFemale(boolean sovereignFemale) {
        this.sovereignFemale = sovereignFemale;
    }

    public synchronized UUID getConsort() {
        return consort;
    }

    public synchronized void setConsort(UUID consort) {
        this.consort = consort;
    }

    public synchronized boolean isConsortFemale() {
        return consortFemale;
    }

    public synchronized void setConsortFemale(boolean consortFemale) {
        this.consortFemale = consortFemale;
    }

    public synchronized UUID getDowager() {
        return dowager;
    }

    public synchronized void setDowager(UUID dowager) {
        this.dowager = dowager;
    }

    public synchronized boolean isDowagerFemale() {
        return dowagerFemale;
    }

    public synchronized void setDowagerFemale(boolean dowagerFemale) {
        this.dowagerFemale = dowagerFemale;
    }

    public synchronized UUID getHeir() {
        return heir;
    }

    public synchronized void setHeir(UUID heir) {
        this.heir = heir;
    }

    public synchronized boolean isMonarchyRejected() {
        return monarchyRejected;
    }

    public synchronized void setMonarchyRejected(boolean monarchyRejected) {
        this.monarchyRejected = monarchyRejected;
    }

    public synchronized List<String> getChronicleEntries() {
        return chronicleEntries;
    }

    public synchronized void addChronicleEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        chronicleEntries.add(entry);
        if (chronicleEntries.size() > 200) {
            chronicleEntries.remove(0);
        }
    }

    public synchronized Set<UUID> getRoyalChildren() {
        return royalChildren;
    }

    public synchronized List<UUID> getRoyalSuccessionOrder() {
        return royalSuccessionOrder;
    }

    public synchronized Set<UUID> getLegitimizedRoyalChildren() {
        return legitimizedRoyalChildren;
    }

    public synchronized Set<UUID> getDisinheritedRoyalChildren() {
        return disinheritedRoyalChildren;
    }

    public synchronized Set<UUID> getDukes() {
        return dukes;
    }

    public synchronized Set<UUID> getLords() {
        return lords;
    }

    public synchronized Set<UUID> getKnights() {
        return knights;
    }

    public synchronized Map<UUID, Boolean> getRoyalChildFemale() {
        return royalChildFemale;
    }

    public synchronized Map<UUID, Boolean> getDukeFemale() {
        return dukeFemale;
    }

    public synchronized Map<UUID, Boolean> getLordFemale() {
        return lordFemale;
    }

    public synchronized Map<UUID, Boolean> getKnightFemale() {
        return knightFemale;
    }

    public synchronized CapitalState getState() {
        return state;
    }

    public synchronized void setState(CapitalState state) {
        this.state = state;
    }

    public synchronized void addRoyalChild(UUID villagerId, boolean female) {
        if (villagerId != null && !disinheritedRoyalChildren.contains(villagerId)) {
            royalChildren.add(villagerId);
            royalChildFemale.put(villagerId, female);
            if (!royalSuccessionOrder.contains(villagerId)) {
                royalSuccessionOrder.add(villagerId);
            }
        }
    }

    public synchronized void addLegitimizedRoyalChild(UUID villagerId, boolean female) {
        if (villagerId != null) {
            disinheritedRoyalChildren.remove(villagerId);
            legitimizedRoyalChildren.add(villagerId);
            addRoyalChild(villagerId, female);
        }
    }

    public synchronized void disinheritRoyalChild(UUID villagerId) {
        if (villagerId == null) {
            return;
        }

        if (villagerId.equals(heir)) {
            heir = null;
        }

        disinheritedRoyalChildren.add(villagerId);
        legitimizedRoyalChildren.remove(villagerId);
        royalChildren.remove(villagerId);
        royalSuccessionOrder.removeIf(villagerId::equals);
        royalChildFemale.remove(villagerId);
    }

    public synchronized void addDuke(UUID villagerId, boolean female) {
        if (villagerId != null) {
            dukes.add(villagerId);
            dukeFemale.put(villagerId, female);
        }
    }

    public synchronized void addLord(UUID villagerId, boolean female) {
        if (villagerId != null) {
            lords.add(villagerId);
            lordFemale.put(villagerId, female);
        }
    }

    public synchronized void addKnight(UUID villagerId, boolean female) {
        if (villagerId != null) {
            knights.add(villagerId);
            knightFemale.put(villagerId, female);
        }
    }

    public synchronized boolean isRoyalChild(UUID villagerId) {
        return villagerId != null && royalChildren.contains(villagerId);
    }

    public synchronized boolean isLegitimizedRoyalChild(UUID villagerId) {
        return villagerId != null && legitimizedRoyalChildren.contains(villagerId);
    }

    public synchronized boolean isDisinheritedRoyalChild(UUID villagerId) {
        return villagerId != null && disinheritedRoyalChildren.contains(villagerId);
    }

    public synchronized boolean isDuke(UUID villagerId) {
        return villagerId != null && dukes.contains(villagerId);
    }

    public synchronized boolean isLord(UUID villagerId) {
        return villagerId != null && lords.contains(villagerId);
    }

    public synchronized boolean isKnight(UUID villagerId) {
        return villagerId != null && knights.contains(villagerId);
    }

    public synchronized boolean isRoyalChildFemale(UUID villagerId) {
        return villagerId != null && royalChildFemale.getOrDefault(villagerId, false);
    }

    public synchronized boolean isDukeFemale(UUID villagerId) {
        return villagerId != null && dukeFemale.getOrDefault(villagerId, false);
    }

    public synchronized boolean isLordFemale(UUID villagerId) {
        return villagerId != null && lordFemale.getOrDefault(villagerId, false);
    }

    public synchronized boolean isKnightFemale(UUID villagerId) {
        return villagerId != null && knightFemale.getOrDefault(villagerId, false);
    }

    public synchronized boolean containsEntity(UUID entityId) {
        if (entityId == null) {
            return false;
        }

        return entityId.equals(sovereign)
                || entityId.equals(consort)
                || entityId.equals(dowager)
                || entityId.equals(heir)
                || royalChildren.contains(entityId)
                || dukes.contains(entityId)
                || lords.contains(entityId)
                || knights.contains(entityId);
    }

    public synchronized void replaceDynamicRoles(
            UUID newConsort,
            boolean newConsortFemale,
            UUID newHeir,
            Set<UUID> newRoyalChildren,
            Map<UUID, Boolean> newRoyalChildFemale,
            Set<UUID> newLords,
            Map<UUID, Boolean> newLordFemale,
            Set<UUID> newKnights,
            Map<UUID, Boolean> newKnightFemale
    ) {
        this.consort = newConsort;
        this.consortFemale = newConsortFemale;
        this.heir = newHeir;

        this.royalChildren.clear();
        this.royalChildren.addAll(newRoyalChildren);

        this.royalChildFemale.clear();
        this.royalChildFemale.putAll(newRoyalChildFemale);

        this.royalSuccessionOrder.removeIf(id -> !this.royalChildren.contains(id));
        for (UUID childId : this.royalChildren) {
            if (!this.royalSuccessionOrder.contains(childId)) {
                this.royalSuccessionOrder.add(childId);
            }
        }

        this.legitimizedRoyalChildren.retainAll(this.royalChildren);

        this.lords.clear();
        this.lords.addAll(newLords);

        this.lordFemale.clear();
        this.lordFemale.putAll(newLordFemale);

        this.knights.clear();
        this.knights.addAll(newKnights);

        this.knightFemale.clear();
        this.knightFemale.putAll(newKnightFemale);
    }
}