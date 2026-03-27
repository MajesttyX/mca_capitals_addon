package com.example.mcacapitals.capital;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalRecord {

    private final UUID capitalId;
    private Integer villageId;
    private CapitalState state;

    private UUID sovereign;
    private boolean sovereignFemale;

    private UUID consort;
    private boolean consortFemale;

    private UUID dowager;
    private boolean dowagerFemale;

    private UUID heir;
    private boolean heirFemale;
    private HeirMode heirMode = HeirMode.DYNASTIC;

    private final Set<UUID> royalChildren = new LinkedHashSet<>();
    private final Map<UUID, Boolean> royalChildFemale = new LinkedHashMap<>();

    private final Set<UUID> disinheritedRoyalChildren = new LinkedHashSet<>();
    private final Set<UUID> legitimizedRoyalChildren = new LinkedHashSet<>();
    private final Map<UUID, Boolean> legitimizedRoyalChildFemale = new LinkedHashMap<>();
    private final List<UUID> royalSuccessionOrder = new ArrayList<>();

    private final Set<UUID> dukes = new LinkedHashSet<>();
    private final Map<UUID, Boolean> dukeFemale = new LinkedHashMap<>();

    private final Set<UUID> lords = new LinkedHashSet<>();
    private final Map<UUID, Boolean> lordFemale = new LinkedHashMap<>();

    private final Set<UUID> knights = new LinkedHashSet<>();
    private final Map<UUID, Boolean> knightFemale = new LinkedHashMap<>();

    private boolean playerSovereign;
    private UUID playerSovereignId;
    private String playerSovereignName;

    private boolean playerConsort;
    private UUID playerConsortId;
    private String playerConsortName;

    private boolean monarchyRejected;

    private boolean mourningActive;
    private long mourningEndDay;
    private final List<String> chronicleEntries = new ArrayList<>();
    private final Map<UUID, String> mourningOriginalClothes = new LinkedHashMap<>();

    private UUID commander;
    private boolean commanderFemale;
    private long lastCommanderRaidBlessingGameTime;
    private long lastCommanderRandomBlessingDay;

    private final Set<UUID> royalGuards = new LinkedHashSet<>();
    private final Map<UUID, Boolean> royalGuardFemale = new LinkedHashMap<>();
    private final Set<UUID> disgracedRoyalGuards = new LinkedHashSet<>();
    private UUID royalGuardLiege;
    private final Set<UUID> royalGuardPatrolling = new LinkedHashSet<>();
    private final Map<UUID, BlockPos> royalGuardPatrolAnchors = new LinkedHashMap<>();
    private final Map<UUID, GuardDutyMode> royalGuardDutyModes = new LinkedHashMap<>();
    private long lastRoyalGuardPromptDay;
    private UUID pendingPlayerGuardSelectionRequester;

    public CapitalRecord(UUID capitalId, Integer villageId) {
        this(capitalId, villageId, null, false);
    }

    public CapitalRecord(UUID capitalId, Integer villageId, UUID sovereign, boolean sovereignFemale) {
        this.capitalId = capitalId;
        this.villageId = villageId;
        this.sovereign = sovereign;
        this.sovereignFemale = sovereignFemale;
        this.state = CapitalState.ACTIVE;
    }

    public UUID getCapitalId() {
        return capitalId;
    }

    public Integer getVillageId() {
        return villageId;
    }

    public void setVillageId(Integer villageId) {
        this.villageId = villageId;
    }

    public CapitalState getState() {
        return state;
    }

    public void setState(CapitalState state) {
        this.state = state;
    }

    public UUID getSovereign() {
        return sovereign;
    }

    public void setSovereign(UUID sovereign) {
        this.sovereign = sovereign;
    }

    public boolean isSovereignFemale() {
        return sovereignFemale;
    }

    public void setSovereignFemale(boolean sovereignFemale) {
        this.sovereignFemale = sovereignFemale;
    }

    public UUID getConsort() {
        return consort;
    }

    public void setConsort(UUID consort) {
        this.consort = consort;
    }

    public boolean isConsortFemale() {
        return consortFemale;
    }

    public void setConsortFemale(boolean consortFemale) {
        this.consortFemale = consortFemale;
    }

    public UUID getDowager() {
        return dowager;
    }

    public void setDowager(UUID dowager) {
        this.dowager = dowager;
    }

    public boolean isDowagerFemale() {
        return dowagerFemale;
    }

    public void setDowagerFemale(boolean dowagerFemale) {
        this.dowagerFemale = dowagerFemale;
    }

    public UUID getHeir() {
        return heir;
    }

    public void setHeir(UUID heir) {
        this.heir = heir;
    }

    public boolean isHeirFemale() {
        return heirFemale;
    }

    public void setHeirFemale(boolean heirFemale) {
        this.heirFemale = heirFemale;
    }

    public HeirMode getHeirMode() {
        return heirMode;
    }

    public void setHeirMode(HeirMode heirMode) {
        this.heirMode = heirMode == null ? HeirMode.DYNASTIC : heirMode;
    }

    public Set<UUID> getRoyalChildren() {
        return royalChildren;
    }

    public Map<UUID, Boolean> getRoyalChildFemale() {
        return royalChildFemale;
    }

    public boolean isRoyalChild(UUID id) {
        return id != null && royalChildren.contains(id);
    }

    public boolean isRoyalChildFemale(UUID id) {
        return royalChildFemale.getOrDefault(id, false);
    }

    public void addRoyalChild(UUID id) {
        addRoyalChild(id, false);
    }

    public void addRoyalChild(UUID id, boolean female) {
        if (id != null) {
            royalChildren.add(id);
            royalChildFemale.put(id, female);
            disinheritedRoyalChildren.remove(id);
        }
    }

    public void removeRoyalChild(UUID id) {
        if (id != null) {
            royalChildren.remove(id);
            royalChildFemale.remove(id);
        }
    }

    public Set<UUID> getDisinheritedRoyalChildren() {
        return disinheritedRoyalChildren;
    }

    public void addDisinheritedRoyalChild(UUID id) {
        if (id != null) {
            disinheritedRoyalChildren.add(id);
            removeRoyalChild(id);
            royalSuccessionOrder.remove(id);
            if (id.equals(heir)) {
                heir = null;
                heirFemale = false;
            }
        }
    }

    public void disinheritRoyalChild(UUID id) {
        addDisinheritedRoyalChild(id);
    }

    public void removeDisinheritedRoyalChild(UUID id) {
        if (id != null) {
            disinheritedRoyalChildren.remove(id);
        }
    }

    public boolean isDisinheritedRoyalChild(UUID id) {
        return id != null && disinheritedRoyalChildren.contains(id);
    }

    public Set<UUID> getLegitimizedRoyalChildren() {
        return legitimizedRoyalChildren;
    }

    public Map<UUID, Boolean> getLegitimizedRoyalChildFemale() {
        return legitimizedRoyalChildFemale;
    }

    public void addLegitimizedRoyalChild(UUID id) {
        addLegitimizedRoyalChild(id, false);
    }

    public void addLegitimizedRoyalChild(UUID id, boolean female) {
        if (id != null) {
            legitimizedRoyalChildren.add(id);
            legitimizedRoyalChildFemale.put(id, female);
        }
    }

    public void removeLegitimizedRoyalChild(UUID id) {
        if (id != null) {
            legitimizedRoyalChildren.remove(id);
            legitimizedRoyalChildFemale.remove(id);
        }
    }

    public boolean isLegitimizedRoyalChild(UUID id) {
        return id != null && legitimizedRoyalChildren.contains(id);
    }

    public List<UUID> getRoyalSuccessionOrder() {
        return royalSuccessionOrder;
    }

    public void setRoyalSuccessionOrder(List<UUID> order) {
        royalSuccessionOrder.clear();
        if (order != null) {
            royalSuccessionOrder.addAll(order);
        }
    }

    public Set<UUID> getDukes() {
        return dukes;
    }

    public Map<UUID, Boolean> getDukeFemale() {
        return dukeFemale;
    }

    public boolean isDuke(UUID id) {
        return id != null && dukes.contains(id);
    }

    public boolean isDukeFemale(UUID id) {
        return dukeFemale.getOrDefault(id, false);
    }

    public void addDuke(UUID id, boolean female) {
        if (id != null) {
            dukes.add(id);
            dukeFemale.put(id, female);
        }
    }

    public void removeDuke(UUID id) {
        if (id != null) {
            dukes.remove(id);
            dukeFemale.remove(id);
        }
    }

    public Set<UUID> getLords() {
        return lords;
    }

    public Map<UUID, Boolean> getLordFemale() {
        return lordFemale;
    }

    public boolean isLord(UUID id) {
        return id != null && lords.contains(id);
    }

    public boolean isLordFemale(UUID id) {
        return lordFemale.getOrDefault(id, false);
    }

    public void addLord(UUID id, boolean female) {
        if (id != null) {
            lords.add(id);
            lordFemale.put(id, female);
        }
    }

    public void removeLord(UUID id) {
        if (id != null) {
            lords.remove(id);
            lordFemale.remove(id);
        }
    }

    public Set<UUID> getKnights() {
        return knights;
    }

    public Map<UUID, Boolean> getKnightFemale() {
        return knightFemale;
    }

    public boolean isKnight(UUID id) {
        return id != null && knights.contains(id);
    }

    public boolean isKnightFemale(UUID id) {
        return knightFemale.getOrDefault(id, false);
    }

    public void addKnight(UUID id, boolean female) {
        if (id != null) {
            knights.add(id);
            knightFemale.put(id, female);
        }
    }

    public void removeKnight(UUID id) {
        if (id != null) {
            knights.remove(id);
            knightFemale.remove(id);
        }
    }

    public boolean isPlayerSovereign() {
        return playerSovereign;
    }

    public void setPlayerSovereign(boolean playerSovereign) {
        this.playerSovereign = playerSovereign;
    }

    public UUID getPlayerSovereignId() {
        return playerSovereignId;
    }

    public void setPlayerSovereignId(UUID playerSovereignId) {
        this.playerSovereignId = playerSovereignId;
    }

    public String getPlayerSovereignName() {
        return playerSovereignName;
    }

    public void setPlayerSovereignName(String playerSovereignName) {
        this.playerSovereignName = playerSovereignName;
    }

    public boolean isPlayerConsort() {
        return playerConsort;
    }

    public void setPlayerConsort(boolean playerConsort) {
        this.playerConsort = playerConsort;
    }

    public UUID getPlayerConsortId() {
        return playerConsortId;
    }

    public void setPlayerConsortId(UUID playerConsortId) {
        this.playerConsortId = playerConsortId;
    }

    public String getPlayerConsortName() {
        return playerConsortName;
    }

    public void setPlayerConsortName(String playerConsortName) {
        this.playerConsortName = playerConsortName;
    }

    public boolean isMonarchyRejected() {
        return monarchyRejected;
    }

    public void setMonarchyRejected(boolean monarchyRejected) {
        this.monarchyRejected = monarchyRejected;
    }

    public boolean isMourningActive() {
        return mourningActive;
    }

    public void setMourningActive(boolean mourningActive) {
        this.mourningActive = mourningActive;
    }

    public long getMourningEndDay() {
        return mourningEndDay;
    }

    public void setMourningEndDay(long mourningEndDay) {
        this.mourningEndDay = mourningEndDay;
    }

    public List<String> getChronicleEntries() {
        return chronicleEntries;
    }

    public void addChronicleEntry(String entry) {
        if (entry != null && !entry.isBlank()) {
            chronicleEntries.add(entry);
        }
    }

    public Map<UUID, String> getMourningOriginalClothes() {
        return mourningOriginalClothes;
    }

    public UUID getCommander() {
        return commander;
    }

    public void setCommander(UUID commander) {
        this.commander = commander;
    }

    public boolean isCommanderFemale() {
        return commanderFemale;
    }

    public void setCommanderFemale(boolean commanderFemale) {
        this.commanderFemale = commanderFemale;
    }

    public long getLastCommanderRaidBlessingGameTime() {
        return lastCommanderRaidBlessingGameTime;
    }

    public void setLastCommanderRaidBlessingGameTime(long lastCommanderRaidBlessingGameTime) {
        this.lastCommanderRaidBlessingGameTime = lastCommanderRaidBlessingGameTime;
    }

    public long getLastCommanderRandomBlessingDay() {
        return lastCommanderRandomBlessingDay;
    }

    public void setLastCommanderRandomBlessingDay(long lastCommanderRandomBlessingDay) {
        this.lastCommanderRandomBlessingDay = lastCommanderRandomBlessingDay;
    }

    public Set<UUID> getRoyalGuards() {
        return royalGuards;
    }

    public Map<UUID, Boolean> getRoyalGuardFemale() {
        return royalGuardFemale;
    }

    public boolean isRoyalGuard(UUID id) {
        return id != null && royalGuards.contains(id);
    }

    public boolean isRoyalGuardFemale(UUID id) {
        return royalGuardFemale.getOrDefault(id, false);
    }

    public void addRoyalGuard(UUID id, boolean female, UUID liege) {
        if (id != null) {
            royalGuards.add(id);
            royalGuardFemale.put(id, female);
            royalGuardLiege = liege;
        }
    }

    public void removeRoyalGuard(UUID id) {
        if (id != null) {
            royalGuards.remove(id);
            royalGuardFemale.remove(id);
            royalGuardPatrolling.remove(id);
            royalGuardPatrolAnchors.remove(id);
            royalGuardDutyModes.remove(id);
        }
    }

    public Set<UUID> getDisgracedRoyalGuards() {
        return disgracedRoyalGuards;
    }

    public boolean isDisgracedRoyalGuard(UUID id) {
        return id != null && disgracedRoyalGuards.contains(id);
    }

    public void disgraceRoyalGuard(UUID id) {
        if (id != null) {
            removeRoyalGuard(id);
            disgracedRoyalGuards.add(id);
        }
    }

    public UUID getRoyalGuardLiege() {
        return royalGuardLiege;
    }

    public void setRoyalGuardLiege(UUID royalGuardLiege) {
        this.royalGuardLiege = royalGuardLiege;
    }

    public Set<UUID> getRoyalGuardPatrolling() {
        return royalGuardPatrolling;
    }

    public Map<UUID, GuardDutyMode> getRoyalGuardDutyModes() {
        return royalGuardDutyModes;
    }

    public GuardDutyMode getRoyalGuardDutyMode(UUID id) {
        return id == null ? GuardDutyMode.FOLLOW_SOVEREIGN : royalGuardDutyModes.getOrDefault(id, GuardDutyMode.FOLLOW_SOVEREIGN);
    }

    public void setRoyalGuardDutyMode(UUID id, GuardDutyMode mode) {
        if (id != null) {
            royalGuardDutyModes.put(id, mode == null ? GuardDutyMode.FOLLOW_SOVEREIGN : mode);
            if (mode == GuardDutyMode.PATROL_ANCHOR) {
                royalGuardPatrolling.add(id);
            } else {
                royalGuardPatrolling.remove(id);
            }
        }
    }

    public Map<UUID, BlockPos> getRoyalGuardPatrolAnchors() {
        return royalGuardPatrolAnchors;
    }

    public BlockPos getRoyalGuardPatrolAnchor(UUID id) {
        return id == null ? null : royalGuardPatrolAnchors.get(id);
    }

    public void setRoyalGuardPatrolAnchor(UUID id, BlockPos anchor) {
        if (id != null) {
            if (anchor == null) {
                royalGuardPatrolAnchors.remove(id);
            } else {
                royalGuardPatrolAnchors.put(id, anchor);
            }
        }
    }

    public long getLastRoyalGuardPromptDay() {
        return lastRoyalGuardPromptDay;
    }

    public void setLastRoyalGuardPromptDay(long lastRoyalGuardPromptDay) {
        this.lastRoyalGuardPromptDay = lastRoyalGuardPromptDay;
    }

    public UUID getPendingPlayerGuardSelectionRequester() {
        return pendingPlayerGuardSelectionRequester;
    }

    public void setPendingPlayerGuardSelectionRequester(UUID pendingPlayerGuardSelectionRequester) {
        this.pendingPlayerGuardSelectionRequester = pendingPlayerGuardSelectionRequester;
    }

    public boolean containsEntity(UUID entityId) {
        if (entityId == null) {
            return false;
        }

        return entityId.equals(sovereign)
                || entityId.equals(consort)
                || entityId.equals(dowager)
                || entityId.equals(heir)
                || royalChildren.contains(entityId)
                || disinheritedRoyalChildren.contains(entityId)
                || legitimizedRoyalChildren.contains(entityId)
                || dukes.contains(entityId)
                || lords.contains(entityId)
                || knights.contains(entityId)
                || royalGuards.contains(entityId)
                || disgracedRoyalGuards.contains(entityId)
                || entityId.equals(commander)
                || entityId.equals(playerSovereignId)
                || entityId.equals(playerConsortId);
    }

    public void replaceDynamicRoles(
            UUID sovereign, boolean sovereignFemale,
            UUID heir,
            Set<UUID> royalChildren, Map<UUID, Boolean> royalChildFemale,
            Set<UUID> dukes, Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords, Map<UUID, Boolean> lordFemale
    ) {
        replaceDynamicRoles(
                sovereign, sovereignFemale, heir,
                royalChildren, royalChildFemale,
                dukes, dukeFemale,
                lords, lordFemale,
                this.knights, new LinkedHashMap<>(this.knightFemale)
        );
    }

    public void replaceDynamicRoles(
            UUID sovereign, boolean sovereignFemale,
            UUID heir,
            Set<UUID> royalChildren, Map<UUID, Boolean> royalChildFemale,
            Set<UUID> dukes, Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords, Map<UUID, Boolean> lordFemale,
            Set<UUID> knights, Map<UUID, Boolean> knightFemale
    ) {
        this.sovereign = sovereign;
        this.sovereignFemale = sovereignFemale;
        this.heir = heir;
        this.heirFemale = heir != null && royalChildFemale.getOrDefault(heir, false);

        this.royalChildren.clear();
        this.royalChildren.addAll(royalChildren);
        this.royalChildFemale.clear();
        this.royalChildFemale.putAll(royalChildFemale);

        this.dukes.clear();
        this.dukes.addAll(dukes);
        this.dukeFemale.clear();
        this.dukeFemale.putAll(dukeFemale);

        this.lords.clear();
        this.lords.addAll(lords);
        this.lordFemale.clear();
        this.lordFemale.putAll(lordFemale);

        this.knights.clear();
        this.knights.addAll(knights);
        this.knightFemale.clear();
        this.knightFemale.putAll(knightFemale);
    }

    public enum HeirMode {
        DYNASTIC,
        MANUAL,
        NONE
    }

    public enum GuardDutyMode {
        FOLLOW_SOVEREIGN,
        PATROL_ANCHOR
    }
}
