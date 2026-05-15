package com.majesttyx.mcacapitals.capital;

import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalRecord {

    final UUID capitalId;
    Integer villageId;
    CapitalState state;

    final CapitalRecordIdentityState identity = new CapitalRecordIdentityState();
    final CapitalRecordLineageState lineage = new CapitalRecordLineageState();
    final CapitalRecordNobilityState nobility = new CapitalRecordNobilityState();
    final CapitalRecordCourtState court = new CapitalRecordCourtState();
    final CapitalRecordGuardState guard = new CapitalRecordGuardState();

    public CapitalRecord(UUID capitalId, Integer villageId) {
        this(capitalId, villageId, null, false);
    }

    public CapitalRecord(UUID capitalId, Integer villageId, UUID sovereign, boolean sovereignFemale) {
        this.capitalId = capitalId;
        this.villageId = villageId;
        identity.sovereign = sovereign;
        identity.sovereignFemale = sovereignFemale;
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
        return identity.sovereign;
    }

    public void setSovereign(UUID sovereign) {
        identity.sovereign = sovereign;
    }

    public boolean isSovereignFemale() {
        return identity.sovereignFemale;
    }

    public void setSovereignFemale(boolean sovereignFemale) {
        identity.sovereignFemale = sovereignFemale;
    }

    public UUID getConsort() {
        return identity.consort;
    }

    public void setConsort(UUID consort) {
        identity.consort = consort;
    }

    public boolean isConsortFemale() {
        return identity.consortFemale;
    }

    public void setConsortFemale(boolean consortFemale) {
        identity.consortFemale = consortFemale;
    }

    public UUID getDowager() {
        return identity.dowager;
    }

    public void setDowager(UUID dowager) {
        identity.dowager = dowager;
    }

    public boolean isDowagerFemale() {
        return identity.dowagerFemale;
    }

    public void setDowagerFemale(boolean dowagerFemale) {
        identity.dowagerFemale = dowagerFemale;
    }

    public UUID getHeir() {
        return identity.heir;
    }

    public void setHeir(UUID heir) {
        identity.heir = heir;
    }

    public boolean isHeirFemale() {
        return identity.heirFemale;
    }

    public void setHeirFemale(boolean heirFemale) {
        identity.heirFemale = heirFemale;
    }

    public HeirMode getHeirMode() {
        return identity.heirMode;
    }

    public void setHeirMode(HeirMode heirMode) {
        identity.heirMode = heirMode == null ? HeirMode.DYNASTIC : heirMode;
    }

    public Set<UUID> getRoyalChildren() {
        return CapitalRecordLineageOps.getRoyalChildren(this);
    }

    public Map<UUID, Boolean> getRoyalChildFemale() {
        return CapitalRecordLineageOps.getRoyalChildFemale(this);
    }

    public boolean isRoyalChild(UUID id) {
        return CapitalRecordLineageOps.isRoyalChild(this, id);
    }

    public boolean isRoyalChildFemale(UUID id) {
        return CapitalRecordLineageOps.isRoyalChildFemale(this, id);
    }

    public Set<UUID> getRoyalHousehold() {
        return CapitalRecordLineageOps.getRoyalHousehold(this);
    }

    public boolean isRoyalHouseholdMember(UUID id) {
        return CapitalRecordLineageOps.isRoyalHouseholdMember(this, id);
    }

    public void addRoyalHouseholdMember(UUID id) {
        CapitalRecordLineageOps.addRoyalHouseholdMember(this, id);
    }

    public void removeRoyalHouseholdMember(UUID id) {
        CapitalRecordLineageOps.removeRoyalHouseholdMember(this, id);
    }

    public void clearRoyalHousehold() {
        CapitalRecordLineageOps.clearRoyalHousehold(this);
    }

    public void addRoyalChild(UUID id) {
        CapitalRecordLineageOps.addRoyalChild(this, id);
    }

    public void addRoyalChild(UUID id, boolean female) {
        CapitalRecordLineageOps.addRoyalChild(this, id, female);
    }

    public void removeRoyalChild(UUID id) {
        CapitalRecordLineageOps.removeRoyalChild(this, id);
    }

    public Set<UUID> getDisinheritedRoyalChildren() {
        return CapitalRecordLineageOps.getDisinheritedRoyalChildren(this);
    }

    public void addDisinheritedRoyalChild(UUID id) {
        CapitalRecordLineageOps.addDisinheritedRoyalChild(this, id);
    }

    public void disinheritRoyalChild(UUID id) {
        CapitalRecordLineageOps.disinheritRoyalChild(this, id);
    }

    public void removeDisinheritedRoyalChild(UUID id) {
        CapitalRecordLineageOps.removeDisinheritedRoyalChild(this, id);
    }

    public boolean isDisinheritedRoyalChild(UUID id) {
        return CapitalRecordLineageOps.isDisinheritedRoyalChild(this, id);
    }

    public Set<UUID> getLegitimizedRoyalChildren() {
        return CapitalRecordLineageOps.getLegitimizedRoyalChildren(this);
    }

    public Map<UUID, Boolean> getLegitimizedRoyalChildFemale() {
        return CapitalRecordLineageOps.getLegitimizedRoyalChildFemale(this);
    }

    public void addLegitimizedRoyalChild(UUID id) {
        CapitalRecordLineageOps.addLegitimizedRoyalChild(this, id);
    }

    public void addLegitimizedRoyalChild(UUID id, boolean female) {
        CapitalRecordLineageOps.addLegitimizedRoyalChild(this, id, female);
    }

    public void removeLegitimizedRoyalChild(UUID id) {
        CapitalRecordLineageOps.removeLegitimizedRoyalChild(this, id);
    }

    public boolean isLegitimizedRoyalChild(UUID id) {
        return CapitalRecordLineageOps.isLegitimizedRoyalChild(this, id);
    }

    public List<UUID> getRoyalSuccessionOrder() {
        return CapitalRecordLineageOps.getRoyalSuccessionOrder(this);
    }

    public void setRoyalSuccessionOrder(List<UUID> order) {
        CapitalRecordLineageOps.setRoyalSuccessionOrder(this, order);
    }

    public Map<UUID, UUID> getPrinceConsortSources() {
        return CapitalRecordLineageOps.getPrinceConsortSources(this);
    }

    public Map<UUID, Boolean> getPrinceConsortFemale() {
        return CapitalRecordLineageOps.getPrinceConsortFemale(this);
    }

    public boolean isPrinceConsort(UUID id) {
        return CapitalRecordLineageOps.isPrinceConsort(this, id);
    }

    public boolean isPrinceConsortFemale(UUID id) {
        return CapitalRecordLineageOps.isPrinceConsortFemale(this, id);
    }

    public UUID getPrinceConsortSource(UUID id) {
        return CapitalRecordLineageOps.getPrinceConsortSource(this, id);
    }

    public void setPrinceConsortSource(UUID holder, UUID source, boolean female) {
        CapitalRecordLineageOps.setPrinceConsortSource(this, holder, source, female);
    }

    public void removePrinceConsortSource(UUID holder) {
        CapitalRecordLineageOps.removePrinceConsortSource(this, holder);
    }

    public Map<UUID, UUID> getDowagerPrinceSources() {
        return CapitalRecordLineageOps.getDowagerPrinceSources(this);
    }

    public Map<UUID, Boolean> getDowagerPrinceFemale() {
        return CapitalRecordLineageOps.getDowagerPrinceFemale(this);
    }

    public boolean isDowagerPrince(UUID id) {
        return CapitalRecordLineageOps.isDowagerPrince(this, id);
    }

    public boolean isDowagerPrinceFemale(UUID id) {
        return CapitalRecordLineageOps.isDowagerPrinceFemale(this, id);
    }

    public UUID getDowagerPrinceSource(UUID id) {
        return CapitalRecordLineageOps.getDowagerPrinceSource(this, id);
    }

    public void setDowagerPrinceSource(UUID holder, UUID source, boolean female) {
        CapitalRecordLineageOps.setDowagerPrinceSource(this, holder, source, female);
    }

    public void removeDowagerPrinceSource(UUID holder) {
        CapitalRecordLineageOps.removeDowagerPrinceSource(this, holder);
    }

    public Set<UUID> getDukes() {
        return CapitalRecordNobilityOps.getDukes(this);
    }

    public Map<UUID, Boolean> getDukeFemale() {
        return CapitalRecordNobilityOps.getDukeFemale(this);
    }

    public boolean isDuke(UUID id) {
        return CapitalRecordNobilityOps.isDuke(this, id);
    }

    public boolean isDukeFemale(UUID id) {
        return CapitalRecordNobilityOps.isDukeFemale(this, id);
    }

    public void addDuke(UUID id, boolean female) {
        CapitalRecordNobilityOps.addDuke(this, id, female);
    }

    public void removeDuke(UUID id) {
        CapitalRecordNobilityOps.removeDuke(this, id);
    }

    public Set<UUID> getLords() {
        return CapitalRecordNobilityOps.getLords(this);
    }

    public Map<UUID, Boolean> getLordFemale() {
        return CapitalRecordNobilityOps.getLordFemale(this);
    }

    public boolean isLord(UUID id) {
        return CapitalRecordNobilityOps.isLord(this, id);
    }

    public boolean isLordFemale(UUID id) {
        return CapitalRecordNobilityOps.isLordFemale(this, id);
    }

    public void addLord(UUID id, boolean female) {
        CapitalRecordNobilityOps.addLord(this, id, female);
    }

    public void removeLord(UUID id) {
        CapitalRecordNobilityOps.removeLord(this, id);
    }

    public Set<UUID> getKnights() {
        return CapitalRecordNobilityOps.getKnights(this);
    }

    public Map<UUID, Boolean> getKnightFemale() {
        return CapitalRecordNobilityOps.getKnightFemale(this);
    }

    public boolean isKnight(UUID id) {
        return CapitalRecordNobilityOps.isKnight(this, id);
    }

    public boolean isKnightFemale(UUID id) {
        return CapitalRecordNobilityOps.isKnightFemale(this, id);
    }

    public void addKnight(UUID id, boolean female) {
        CapitalRecordNobilityOps.addKnight(this, id, female);
    }

    public void removeKnight(UUID id) {
        CapitalRecordNobilityOps.removeKnight(this, id);
    }

    public Map<UUID, UUID> getMarriageDukeSources() {
        return CapitalRecordNobilityOps.getMarriageDukeSources(this);
    }

    public Map<UUID, Boolean> getMarriageDukeFemale() {
        return CapitalRecordNobilityOps.getMarriageDukeFemale(this);
    }

    public boolean isMarriageDuke(UUID id) {
        return CapitalRecordNobilityOps.isMarriageDuke(this, id);
    }

    public boolean isMarriageDukeFemale(UUID id) {
        return CapitalRecordNobilityOps.isMarriageDukeFemale(this, id);
    }

    public UUID getMarriageDukeSource(UUID id) {
        return CapitalRecordNobilityOps.getMarriageDukeSource(this, id);
    }

    public void setMarriageDukeSource(UUID holder, UUID source, boolean female) {
        CapitalRecordNobilityOps.setMarriageDukeSource(this, holder, source, female);
    }

    public void removeMarriageDukeSource(UUID holder) {
        CapitalRecordNobilityOps.removeMarriageDukeSource(this, holder);
    }

    public Map<UUID, UUID> getDowagerDukeSources() {
        return CapitalRecordNobilityOps.getDowagerDukeSources(this);
    }

    public Map<UUID, Boolean> getDowagerDukeFemale() {
        return CapitalRecordNobilityOps.getDowagerDukeFemale(this);
    }

    public boolean isDowagerDuke(UUID id) {
        return CapitalRecordNobilityOps.isDowagerDuke(this, id);
    }

    public boolean isDowagerDukeFemale(UUID id) {
        return CapitalRecordNobilityOps.isDowagerDukeFemale(this, id);
    }

    public UUID getDowagerDukeSource(UUID id) {
        return CapitalRecordNobilityOps.getDowagerDukeSource(this, id);
    }

    public void setDowagerDukeSource(UUID holder, UUID source, boolean female) {
        CapitalRecordNobilityOps.setDowagerDukeSource(this, holder, source, female);
    }

    public void removeDowagerDukeSource(UUID holder) {
        CapitalRecordNobilityOps.removeDowagerDukeSource(this, holder);
    }

    public boolean isPlayerSovereign() {
        return CapitalRecordCourtOps.isPlayerSovereign(this);
    }

    public void setPlayerSovereign(boolean playerSovereign) {
        CapitalRecordCourtOps.setPlayerSovereign(this, playerSovereign);
    }

    public UUID getPlayerSovereignId() {
        return CapitalRecordCourtOps.getPlayerSovereignId(this);
    }

    public void setPlayerSovereignId(UUID playerSovereignId) {
        CapitalRecordCourtOps.setPlayerSovereignId(this, playerSovereignId);
    }

    public String getPlayerSovereignName() {
        return CapitalRecordCourtOps.getPlayerSovereignName(this);
    }

    public void setPlayerSovereignName(String playerSovereignName) {
        CapitalRecordCourtOps.setPlayerSovereignName(this, playerSovereignName);
    }

    public boolean isPlayerConsort() {
        return CapitalRecordCourtOps.isPlayerConsort(this);
    }

    public void setPlayerConsort(boolean playerConsort) {
        CapitalRecordCourtOps.setPlayerConsort(this, playerConsort);
    }

    public UUID getPlayerConsortId() {
        return CapitalRecordCourtOps.getPlayerConsortId(this);
    }

    public void setPlayerConsortId(UUID playerConsortId) {
        CapitalRecordCourtOps.setPlayerConsortId(this, playerConsortId);
    }

    public String getPlayerConsortName() {
        return CapitalRecordCourtOps.getPlayerConsortName(this);
    }

    public void setPlayerConsortName(String playerConsortName) {
        CapitalRecordCourtOps.setPlayerConsortName(this, playerConsortName);
    }

    public boolean isMonarchyRejected() {
        return CapitalRecordCourtOps.isMonarchyRejected(this);
    }

    public void setMonarchyRejected(boolean monarchyRejected) {
        CapitalRecordCourtOps.setMonarchyRejected(this, monarchyRejected);
    }

    public boolean isMourningActive() {
        return CapitalRecordCourtOps.isMourningActive(this);
    }

    public void setMourningActive(boolean mourningActive) {
        CapitalRecordCourtOps.setMourningActive(this, mourningActive);
    }

    public long getMourningEndDay() {
        return CapitalRecordCourtOps.getMourningEndDay(this);
    }

    public void setMourningEndDay(long mourningEndDay) {
        CapitalRecordCourtOps.setMourningEndDay(this, mourningEndDay);
    }

    public List<String> getChronicleEntries() {
        return CapitalRecordCourtOps.getChronicleEntries(this);
    }

    public void addChronicleEntry(String entry) {
        CapitalRecordCourtOps.addChronicleEntry(this, entry);
    }

    public Map<UUID, String> getMourningOriginalClothes() {
        return CapitalRecordCourtOps.getMourningOriginalClothes(this);
    }

    public UUID getCommander() {
        return CapitalRecordCourtOps.getCommander(this);
    }

    public void setCommander(UUID commander) {
        CapitalRecordCourtOps.setCommander(this, commander);
    }

    public boolean isCommanderFemale() {
        return CapitalRecordCourtOps.isCommanderFemale(this);
    }

    public void setCommanderFemale(boolean commanderFemale) {
        CapitalRecordCourtOps.setCommanderFemale(this, commanderFemale);
    }

    public UUID getHand() {
        return CapitalRecordCourtOps.getHand(this);
    }

    public void setHand(UUID hand) {
        CapitalRecordCourtOps.setHand(this, hand);
    }

    public boolean isHandFemale() {
        return CapitalRecordCourtOps.isHandFemale(this);
    }

    public void setHandFemale(boolean handFemale) {
        CapitalRecordCourtOps.setHandFemale(this, handFemale);
    }

    public UUID getHerald() {
        return CapitalRecordCourtOps.getHerald(this);
    }

    public void setHerald(UUID herald) {
        CapitalRecordCourtOps.setHerald(this, herald);
    }

    public boolean isHeraldFemale() {
        return CapitalRecordCourtOps.isHeraldFemale(this);
    }

    public void setHeraldFemale(boolean heraldFemale) {
        CapitalRecordCourtOps.setHeraldFemale(this, heraldFemale);
    }

    public String getHeraldDisplayName() {
        return CapitalRecordCourtOps.getHeraldDisplayName(this);
    }

    public void setHeraldDisplayName(String heraldDisplayName) {
        CapitalRecordCourtOps.setHeraldDisplayName(this, heraldDisplayName);
    }

    public UUID getGrandMaester() {
        return CapitalRecordCourtOps.getGrandMaester(this);
    }

    public void setGrandMaester(UUID grandMaester) {
        CapitalRecordCourtOps.setGrandMaester(this, grandMaester);
    }

    public boolean isGrandMaesterFemale() {
        return CapitalRecordCourtOps.isGrandMaesterFemale(this);
    }

    public void setGrandMaesterFemale(boolean grandMaesterFemale) {
        CapitalRecordCourtOps.setGrandMaesterFemale(this, grandMaesterFemale);
    }

    public long getLastCommanderRaidBlessingGameTime() {
        return CapitalRecordCourtOps.getLastCommanderRaidBlessingGameTime(this);
    }

    public void setLastCommanderRaidBlessingGameTime(long lastCommanderRaidBlessingGameTime) {
        CapitalRecordCourtOps.setLastCommanderRaidBlessingGameTime(this, lastCommanderRaidBlessingGameTime);
    }

    public long getLastCommanderRandomBlessingDay() {
        return CapitalRecordCourtOps.getLastCommanderRandomBlessingDay(this);
    }

    public void setLastCommanderRandomBlessingDay(long lastCommanderRandomBlessingDay) {
        CapitalRecordCourtOps.setLastCommanderRandomBlessingDay(this, lastCommanderRandomBlessingDay);
    }

    public Set<UUID> getRoyalGuards() {
        return CapitalRecordGuardOps.getRoyalGuards(this);
    }

    public Map<UUID, Boolean> getRoyalGuardFemale() {
        return CapitalRecordGuardOps.getRoyalGuardFemale(this);
    }

    public boolean isRoyalGuard(UUID id) {
        return CapitalRecordGuardOps.isRoyalGuard(this, id);
    }

    public boolean isRoyalGuardFemale(UUID id) {
        return CapitalRecordGuardOps.isRoyalGuardFemale(this, id);
    }

    public void addRoyalGuard(UUID id, boolean female, UUID liege) {
        CapitalRecordGuardOps.addRoyalGuard(this, id, female, liege);
    }

    public void removeRoyalGuard(UUID id) {
        CapitalRecordGuardOps.removeRoyalGuard(this, id);
    }

    public Set<UUID> getDisgracedRoyalGuards() {
        return CapitalRecordGuardOps.getDisgracedRoyalGuards(this);
    }

    public boolean isDisgracedRoyalGuard(UUID id) {
        return CapitalRecordGuardOps.isDisgracedRoyalGuard(this, id);
    }

    public void disgraceRoyalGuard(UUID id) {
        CapitalRecordGuardOps.disgraceRoyalGuard(this, id);
    }

    public UUID getRoyalGuardLiege() {
        return CapitalRecordGuardOps.getRoyalGuardLiege(this);
    }

    public void setRoyalGuardLiege(UUID royalGuardLiege) {
        CapitalRecordGuardOps.setRoyalGuardLiege(this, royalGuardLiege);
    }

    public Set<UUID> getRoyalGuardPatrolling() {
        return CapitalRecordGuardOps.getRoyalGuardPatrolling(this);
    }

    public Map<UUID, GuardDutyMode> getRoyalGuardDutyModes() {
        return CapitalRecordGuardOps.getRoyalGuardDutyModes(this);
    }

    public GuardDutyMode getRoyalGuardDutyMode(UUID id) {
        return CapitalRecordGuardOps.getRoyalGuardDutyMode(this, id);
    }

    public void setRoyalGuardDutyMode(UUID id, GuardDutyMode mode) {
        CapitalRecordGuardOps.setRoyalGuardDutyMode(this, id, mode);
    }

    public Map<UUID, BlockPos> getRoyalGuardPatrolAnchors() {
        return CapitalRecordGuardOps.getRoyalGuardPatrolAnchors(this);
    }

    public BlockPos getRoyalGuardPatrolAnchor(UUID id) {
        return CapitalRecordGuardOps.getRoyalGuardPatrolAnchor(this, id);
    }

    public void setRoyalGuardPatrolAnchor(UUID id, BlockPos anchor) {
        CapitalRecordGuardOps.setRoyalGuardPatrolAnchor(this, id, anchor);
    }

    public long getLastRoyalGuardPromptDay() {
        return CapitalRecordGuardOps.getLastRoyalGuardPromptDay(this);
    }

    public void setLastRoyalGuardPromptDay(long lastRoyalGuardPromptDay) {
        CapitalRecordGuardOps.setLastRoyalGuardPromptDay(this, lastRoyalGuardPromptDay);
    }

    public UUID getPendingPlayerGuardSelectionRequester() {
        return CapitalRecordGuardOps.getPendingPlayerGuardSelectionRequester(this);
    }

    public void setPendingPlayerGuardSelectionRequester(UUID pendingPlayerGuardSelectionRequester) {
        CapitalRecordGuardOps.setPendingPlayerGuardSelectionRequester(this, pendingPlayerGuardSelectionRequester);
    }

    public boolean containsEntity(UUID entityId) {
        return CapitalRecordQueryOps.containsEntity(this, entityId);
    }

    public void replaceDynamicRoles(
            UUID sovereign, boolean sovereignFemale,
            UUID heir,
            Set<UUID> royalChildren, Map<UUID, Boolean> royalChildFemale,
            Set<UUID> dukes, Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords, Map<UUID, Boolean> lordFemale
    ) {
        CapitalRecordQueryOps.replaceDynamicRoles(
                this,
                sovereign, sovereignFemale, heir,
                royalChildren, royalChildFemale,
                dukes, dukeFemale,
                lords, lordFemale,
                nobility.knights, new LinkedHashMap<>(nobility.knightFemale)
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
        CapitalRecordQueryOps.replaceDynamicRoles(
                this,
                sovereign, sovereignFemale, heir,
                royalChildren, royalChildFemale,
                dukes, dukeFemale,
                lords, lordFemale,
                knights, knightFemale
        );
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