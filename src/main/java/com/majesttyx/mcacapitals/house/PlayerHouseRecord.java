package com.majesttyx.mcacapitals.house;

import java.util.UUID;

public class PlayerHouseRecord {

    private final UUID playerId;
    private String houseName;
    private String houseWords;
    private PlayerHouseInheritanceMode inheritanceMode;
    private long houseNameSetAtGameTime;
    private UUID houseNameSetInCapitalId;
    private String houseNameSetInCapitalName;

    public PlayerHouseRecord(UUID playerId) {
        this.playerId = playerId;
        this.houseName = "";
        this.houseWords = "";
        this.inheritanceMode = PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW;
        this.houseNameSetAtGameTime = 0L;
        this.houseNameSetInCapitalId = null;
        this.houseNameSetInCapitalName = "";
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName == null ? "" : houseName;
    }

    public String getHouseWords() {
        return houseWords;
    }

    public void setHouseWords(String houseWords) {
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public PlayerHouseInheritanceMode getInheritanceMode() {
        return inheritanceMode;
    }

    public void setInheritanceMode(PlayerHouseInheritanceMode inheritanceMode) {
        this.inheritanceMode = inheritanceMode == null ? PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW : inheritanceMode;
    }

    public long getHouseNameSetAtGameTime() {
        return houseNameSetAtGameTime;
    }

    public void setHouseNameSetAtGameTime(long houseNameSetAtGameTime) {
        this.houseNameSetAtGameTime = houseNameSetAtGameTime;
    }

    public UUID getHouseNameSetInCapitalId() {
        return houseNameSetInCapitalId;
    }

    public void setHouseNameSetInCapitalId(UUID houseNameSetInCapitalId) {
        this.houseNameSetInCapitalId = houseNameSetInCapitalId;
    }

    public String getHouseNameSetInCapitalName() {
        return houseNameSetInCapitalName;
    }

    public void setHouseNameSetInCapitalName(String houseNameSetInCapitalName) {
        this.houseNameSetInCapitalName = houseNameSetInCapitalName == null ? "" : houseNameSetInCapitalName;
    }

    public boolean hasHouseName() {
        return houseName != null && !houseName.isBlank();
    }

    public boolean hasHouseWords() {
        return houseWords != null && !houseWords.isBlank();
    }
}