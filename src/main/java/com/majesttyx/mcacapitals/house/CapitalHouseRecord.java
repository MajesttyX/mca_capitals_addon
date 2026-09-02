package com.majesttyx.mcacapitals.house;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CapitalHouseRecord {

    private final UUID houseId;
    private final UUID capitalId;

    private String houseName;
    private CapitalHouseTier tier;
    private UUID founderId;
    private UUID headId;
    private UUID parentHouseId;
    private long foundedGameTime;
    private boolean active;

    private final Set<UUID> currentMembers = new LinkedHashSet<>();
    private final Set<UUID> formerMembers = new LinkedHashSet<>();
    private final List<CapitalHouseHistoryEntry> history = new ArrayList<>();

    public CapitalHouseRecord(UUID houseId, UUID capitalId, String houseName) {
        this.houseId = houseId;
        this.capitalId = capitalId;
        this.houseName = normalize(houseName);
        this.tier = CapitalHouseTier.NOBLE;
        this.active = true;
    }

    public UUID getHouseId() {
        return houseId;
    }

    public UUID getCapitalId() {
        return capitalId;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = normalize(houseName);
    }

    public CapitalHouseTier getTier() {
        return tier;
    }

    public void setTier(CapitalHouseTier tier) {
        this.tier = tier == null ? CapitalHouseTier.NOBLE : tier;
    }

    public UUID getFounderId() {
        return founderId;
    }

    public void setFounderId(UUID founderId) {
        this.founderId = founderId;
    }

    public UUID getHeadId() {
        return headId;
    }

    public void setHeadId(UUID headId) {
        this.headId = headId;
    }

    public UUID getParentHouseId() {
        return parentHouseId;
    }

    public void setParentHouseId(UUID parentHouseId) {
        this.parentHouseId = parentHouseId;
    }

    public long getFoundedGameTime() {
        return foundedGameTime;
    }

    public void setFoundedGameTime(long foundedGameTime) {
        this.foundedGameTime = foundedGameTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<UUID> getCurrentMembers() {
        return Collections.unmodifiableSet(currentMembers);
    }

    public Set<UUID> getFormerMembers() {
        return Collections.unmodifiableSet(formerMembers);
    }

    public List<CapitalHouseHistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean isCurrentMember(UUID memberId) {
        return memberId != null && currentMembers.contains(memberId);
    }

    public boolean isFormerMember(UUID memberId) {
        return memberId != null && formerMembers.contains(memberId);
    }

    public boolean addCurrentMember(UUID memberId) {
        if (memberId == null) {
            return false;
        }
        boolean changed = formerMembers.remove(memberId);
        return currentMembers.add(memberId) || changed;
    }

    public boolean removeCurrentMember(UUID memberId) {
        if (memberId == null || !currentMembers.remove(memberId)) {
            return false;
        }
        formerMembers.add(memberId);
        return true;
    }

    public boolean addFormerMember(UUID memberId) {
        if (memberId == null) {
            return false;
        }
        boolean changed = currentMembers.remove(memberId);
        return formerMembers.add(memberId) || changed;
    }

    public void addHistory(CapitalHouseHistoryEntry entry) {
        if (entry != null) {
            history.add(entry);
        }
    }

    public boolean hasHistory(
            CapitalHouseHistoryType type,
            UUID subjectId
    ) {
        if (type == null) {
            return false;
        }
        for (CapitalHouseHistoryEntry entry : history) {
            if (entry != null
                    && entry.type() == type
                    && Objects.equals(entry.subjectId(), subjectId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHistory(
            CapitalHouseHistoryType type,
            UUID subjectId,
            UUID relatedHouseId
    ) {
        if (type == null) {
            return false;
        }
        for (CapitalHouseHistoryEntry entry : history) {
            if (entry != null
                    && entry.type() == type
                    && Objects.equals(entry.subjectId(), subjectId)
                    && Objects.equals(entry.relatedHouseId(), relatedHouseId)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
