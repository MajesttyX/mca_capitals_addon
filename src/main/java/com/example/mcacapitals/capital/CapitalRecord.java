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

    private final Set<UUID> royalHousehold = new LinkedHashSet<>();

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

    private final Map<UUID, UUID> princeConsortSources = new LinkedHashMap<>();
    private final Map<UUID, Boolean> princeConsortFemale = new LinkedHashMap<>();

    private final Map<UUID, UUID> marriageDukeSources = new LinkedHashMap<>();
    private final Map<UUID, Boolean> marriageDukeFemale = new LinkedHashMap<>();

    private final Map<UUID, UUID> dowagerPrinceSources = new LinkedHashMap<>();
    private final Map<UUID, Boolean> dowagerPrinceFemale = new LinkedHashMap<>();

    private final Map<UUID, UUID> dowagerDukeSources = new LinkedHashMap<>();
    private final Map<UUID, Boolean> dowagerDukeFemale = new LinkedHashMap<>();

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
    private UUID hand;
    private boolean handFemale;
    private UUID herald;
    private boolean heraldFemale;
    private UUID grandMaester;
    private boolean grandMaesterFemale;
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
        return containsMember(royalChildren, id);
    }

    public boolean isRoyalChildFemale(UUID id) {
        return isFlaggedFemale(royalChildFemale, id);
    }

    public Set<UUID> getRoyalHousehold() {
        return royalHousehold;
    }

    public boolean isRoyalHouseholdMember(UUID id) {
        return containsMember(royalHousehold, id);
    }

    public void addRoyalHouseholdMember(UUID id) {
        if (id != null) {
            royalHousehold.add(id);
        }
    }

    public void removeRoyalHouseholdMember(UUID id) {
        if (id != null) {
            royalHousehold.remove(id);
        }
    }

    public void clearRoyalHousehold() {
        royalHousehold.clear();
    }

    public void addRoyalChild(UUID id) {
        addRoyalChild(id, false);
    }

    public void addRoyalChild(UUID id, boolean female) {
        if (id != null) {
            putMember(royalChildren, royalChildFemale, id, female);
            disinheritedRoyalChildren.remove(id);
        }
    }

    public void removeRoyalChild(UUID id) {
        removeMember(royalChildren, royalChildFemale, id);
    }

    public Set<UUID> getDisinheritedRoyalChildren() {
        return disinheritedRoyalChildren;
    }

    public void addDisinheritedRoyalChild(UUID id) {
        if (id != null) {
            disinheritedRoyalChildren.add(id);
            removeRoyalChild(id);
            removeLegitimizedRoyalChild(id);
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
        return containsMember(disinheritedRoyalChildren, id);
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
        putMember(legitimizedRoyalChildren, legitimizedRoyalChildFemale, id, female);
        disinheritedRoyalChildren.remove(id);
    }

    public void removeLegitimizedRoyalChild(UUID id) {
        removeMember(legitimizedRoyalChildren, legitimizedRoyalChildFemale, id);
    }

    public boolean isLegitimizedRoyalChild(UUID id) {
        return containsMember(legitimizedRoyalChildren, id);
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
        return containsMember(dukes, id);
    }

    public boolean isDukeFemale(UUID id) {
        return isFlaggedFemale(dukeFemale, id);
    }

    public void addDuke(UUID id, boolean female) {
        putMember(dukes, dukeFemale, id, female);
    }

    public void removeDuke(UUID id) {
        removeMember(dukes, dukeFemale, id);
    }

    public Set<UUID> getLords() {
        return lords;
    }

    public Map<UUID, Boolean> getLordFemale() {
        return lordFemale;
    }

    public boolean isLord(UUID id) {
        return containsMember(lords, id);
    }

    public boolean isLordFemale(UUID id) {
        return isFlaggedFemale(lordFemale, id);
    }

    public void addLord(UUID id, boolean female) {
        putMember(lords, lordFemale, id, female);
    }

    public void removeLord(UUID id) {
        removeMember(lords, lordFemale, id);
    }

    public Set<UUID> getKnights() {
        return knights;
    }

    public Map<UUID, Boolean> getKnightFemale() {
        return knightFemale;
    }

    public boolean isKnight(UUID id) {
        return containsMember(knights, id);
    }

    public boolean isKnightFemale(UUID id) {
        return isFlaggedFemale(knightFemale, id);
    }

    public void addKnight(UUID id, boolean female) {
        putMember(knights, knightFemale, id, female);
    }

    public void removeKnight(UUID id) {
        removeMember(knights, knightFemale, id);
    }

    public Map<UUID, UUID> getPrinceConsortSources() {
        return princeConsortSources;
    }

    public Map<UUID, Boolean> getPrinceConsortFemale() {
        return princeConsortFemale;
    }

    public boolean isPrinceConsort(UUID id) {
        return id != null && princeConsortSources.containsKey(id);
    }

    public boolean isPrinceConsortFemale(UUID id) {
        return isFlaggedFemale(princeConsortFemale, id);
    }

    public UUID getPrinceConsortSource(UUID id) {
        return id == null ? null : princeConsortSources.get(id);
    }

    public void setPrinceConsortSource(UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            princeConsortSources.put(holder, source);
            princeConsortFemale.put(holder, female);
        }
    }

    public void removePrinceConsortSource(UUID holder) {
        if (holder != null) {
            princeConsortSources.remove(holder);
            princeConsortFemale.remove(holder);
        }
    }

    public Map<UUID, UUID> getMarriageDukeSources() {
        return marriageDukeSources;
    }

    public Map<UUID, Boolean> getMarriageDukeFemale() {
        return marriageDukeFemale;
    }

    public boolean isMarriageDuke(UUID id) {
        return id != null && marriageDukeSources.containsKey(id);
    }

    public boolean isMarriageDukeFemale(UUID id) {
        return isFlaggedFemale(marriageDukeFemale, id);
    }

    public UUID getMarriageDukeSource(UUID id) {
        return id == null ? null : marriageDukeSources.get(id);
    }

    public void setMarriageDukeSource(UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            marriageDukeSources.put(holder, source);
            marriageDukeFemale.put(holder, female);
        }
    }

    public void removeMarriageDukeSource(UUID holder) {
        if (holder != null) {
            marriageDukeSources.remove(holder);
            marriageDukeFemale.remove(holder);
        }
    }

    public Map<UUID, UUID> getDowagerPrinceSources() {
        return dowagerPrinceSources;
    }

    public Map<UUID, Boolean> getDowagerPrinceFemale() {
        return dowagerPrinceFemale;
    }

    public boolean isDowagerPrince(UUID id) {
        return id != null && dowagerPrinceSources.containsKey(id);
    }

    public boolean isDowagerPrinceFemale(UUID id) {
        return isFlaggedFemale(dowagerPrinceFemale, id);
    }

    public UUID getDowagerPrinceSource(UUID id) {
        return id == null ? null : dowagerPrinceSources.get(id);
    }

    public void setDowagerPrinceSource(UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            dowagerPrinceSources.put(holder, source);
            dowagerPrinceFemale.put(holder, female);
        }
    }

    public void removeDowagerPrinceSource(UUID holder) {
        if (holder != null) {
            dowagerPrinceSources.remove(holder);
            dowagerPrinceFemale.remove(holder);
        }
    }

    public Map<UUID, UUID> getDowagerDukeSources() {
        return dowagerDukeSources;
    }

    public Map<UUID, Boolean> getDowagerDukeFemale() {
        return dowagerDukeFemale;
    }

    public boolean isDowagerDuke(UUID id) {
        return id != null && dowagerDukeSources.containsKey(id);
    }

    public boolean isDowagerDukeFemale(UUID id) {
        return isFlaggedFemale(dowagerDukeFemale, id);
    }

    public UUID getDowagerDukeSource(UUID id) {
        return id == null ? null : dowagerDukeSources.get(id);
    }

    public void setDowagerDukeSource(UUID holder, UUID source, boolean female) {
        if (holder != null && source != null) {
            dowagerDukeSources.put(holder, source);
            dowagerDukeFemale.put(holder, female);
        }
    }

    public void removeDowagerDukeSource(UUID holder) {
        if (holder != null) {
            dowagerDukeSources.remove(holder);
            dowagerDukeFemale.remove(holder);
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

    public UUID getHand() {
        return hand;
    }

    public void setHand(UUID hand) {
        this.hand = hand;
    }

    public boolean isHandFemale() {
        return handFemale;
    }

    public void setHandFemale(boolean handFemale) {
        this.handFemale = handFemale;
    }

    public UUID getHerald() {
        return herald;
    }

    public void setHerald(UUID herald) {
        this.herald = herald;
    }

    public boolean isHeraldFemale() {
        return heraldFemale;
    }

    public void setHeraldFemale(boolean heraldFemale) {
        this.heraldFemale = heraldFemale;
    }

    public UUID getGrandMaester() {
        return grandMaester;
    }

    public void setGrandMaester(UUID grandMaester) {
        this.grandMaester = grandMaester;
    }

    public boolean isGrandMaesterFemale() {
        return grandMaesterFemale;
    }

    public void setGrandMaesterFemale(boolean grandMaesterFemale) {
        this.grandMaesterFemale = grandMaesterFemale;
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
        return containsMember(royalGuards, id);
    }

    public boolean isRoyalGuardFemale(UUID id) {
        return isFlaggedFemale(royalGuardFemale, id);
    }

    public void addRoyalGuard(UUID id, boolean female, UUID liege) {
        if (id != null) {
            putMember(royalGuards, royalGuardFemale, id, female);
            royalGuardLiege = liege;
        }
    }

    public void removeRoyalGuard(UUID id) {
        if (id != null) {
            removeMember(royalGuards, royalGuardFemale, id);
            royalGuardPatrolling.remove(id);
            royalGuardPatrolAnchors.remove(id);
            royalGuardDutyModes.remove(id);
        }
    }

    public Set<UUID> getDisgracedRoyalGuards() {
        return disgracedRoyalGuards;
    }

    public boolean isDisgracedRoyalGuard(UUID id) {
        return containsMember(disgracedRoyalGuards, id);
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
            GuardDutyMode resolvedMode = mode == null ? GuardDutyMode.FOLLOW_SOVEREIGN : mode;
            royalGuardDutyModes.put(id, resolvedMode);
            if (resolvedMode == GuardDutyMode.PATROL_ANCHOR) {
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

        return hasCourtIdentity(entityId)
                || hasRoyalHouseholdState(entityId)
                || hasRoyalChildState(entityId)
                || hasNobleOffice(entityId)
                || hasGuardOffice(entityId)
                || hasCommanderState(entityId)
                || hasHandState(entityId)
                || hasHeraldState(entityId)
                || hasGrandMaesterState(entityId)
                || hasPlayerCourtState(entityId);
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
        this.heirFemale = resolveHeirFemale(heir, royalChildFemale);

        replaceMembers(this.royalChildren, this.royalChildFemale, royalChildren, royalChildFemale);
        replaceMembers(this.dukes, this.dukeFemale, dukes, dukeFemale);
        replaceMembers(this.lords, this.lordFemale, lords, lordFemale);
        replaceMembers(this.knights, this.knightFemale, knights, knightFemale);
    }

    private boolean hasCourtIdentity(UUID entityId) {
        return entityId.equals(sovereign)
                || entityId.equals(consort)
                || entityId.equals(dowager)
                || entityId.equals(heir);
    }

    private boolean hasRoyalHouseholdState(UUID entityId) {
        return royalHousehold.contains(entityId);
    }

    private boolean hasRoyalChildState(UUID entityId) {
        return royalChildren.contains(entityId)
                || disinheritedRoyalChildren.contains(entityId)
                || legitimizedRoyalChildren.contains(entityId)
                || princeConsortSources.containsKey(entityId)
                || dowagerPrinceSources.containsKey(entityId);
    }

    private boolean hasNobleOffice(UUID entityId) {
        return dukes.contains(entityId)
                || marriageDukeSources.containsKey(entityId)
                || dowagerDukeSources.containsKey(entityId)
                || lords.contains(entityId)
                || knights.contains(entityId);
    }

    private boolean hasGuardOffice(UUID entityId) {
        return royalGuards.contains(entityId)
                || disgracedRoyalGuards.contains(entityId);
    }

    private boolean hasCommanderState(UUID entityId) {
        return entityId.equals(commander);
    }

    private boolean hasHandState(UUID entityId) {
        return entityId.equals(hand);
    }

    private boolean hasHeraldState(UUID entityId) {
        return entityId.equals(herald);
    }

    private boolean hasGrandMaesterState(UUID entityId) {
        return entityId.equals(grandMaester);
    }

    private boolean hasPlayerCourtState(UUID entityId) {
        return entityId.equals(playerSovereignId)
                || entityId.equals(playerConsortId);
    }

    private static boolean containsMember(Set<UUID> members, UUID id) {
        return id != null && members.contains(id);
    }

    private static boolean isFlaggedFemale(Map<UUID, Boolean> femaleFlags, UUID id) {
        return id != null && femaleFlags.getOrDefault(id, false);
    }

    private static void putMember(Set<UUID> members, Map<UUID, Boolean> femaleFlags, UUID id, boolean female) {
        if (id != null) {
            members.add(id);
            femaleFlags.put(id, female);
        }
    }

    private static void removeMember(Set<UUID> members, Map<UUID, Boolean> femaleFlags, UUID id) {
        if (id != null) {
            members.remove(id);
            femaleFlags.remove(id);
        }
    }

    private static void replaceMembers(Set<UUID> members, Map<UUID, Boolean> femaleFlags, Set<UUID> replacements, Map<UUID, Boolean> replacementFemaleFlags) {
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

    private static boolean resolveHeirFemale(UUID heir, Map<UUID, Boolean> royalChildFemale) {
        return heir != null && royalChildFemale.getOrDefault(heir, false);
    }

    public enum HeirMode {
        NONE,
        DYNASTIC,
        MANUAL
    }

    public enum GuardDutyMode {
        FOLLOW_SOVEREIGN,
        PATROL_ANCHOR
    }
}