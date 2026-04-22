package com.example.mcacapitals.player;

import com.example.mcacapitals.noble.NobleTitle;

import java.util.UUID;

public class PlayerCapitalTitleRecord {

    private final UUID playerId;
    private final UUID capitalId;

    private NobleTitle grantedTitle;
    private NobleTitle marriageTitle;
    private UUID marriageSourceSpouseId;

    private NobleTitle dowagerBaseTitle;
    private UUID dowagerSourceSpouseId;

    private boolean commander;
    private String cachedPlayerName;

    public PlayerCapitalTitleRecord(UUID playerId, UUID capitalId) {
        this.playerId = playerId;
        this.capitalId = capitalId;
        this.grantedTitle = NobleTitle.COMMONER;
        this.marriageTitle = NobleTitle.COMMONER;
        this.dowagerBaseTitle = NobleTitle.COMMONER;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getCapitalId() {
        return capitalId;
    }

    public NobleTitle getGrantedTitle() {
        return grantedTitle;
    }

    public void setGrantedTitle(NobleTitle grantedTitle) {
        this.grantedTitle = normalize(grantedTitle);
    }

    public NobleTitle getMarriageTitle() {
        return marriageTitle;
    }

    public void setMarriageTitle(NobleTitle marriageTitle) {
        this.marriageTitle = normalize(marriageTitle);
    }

    public UUID getMarriageSourceSpouseId() {
        return marriageSourceSpouseId;
    }

    public void setMarriageSourceSpouseId(UUID marriageSourceSpouseId) {
        this.marriageSourceSpouseId = marriageSourceSpouseId;
    }

    public NobleTitle getDowagerBaseTitle() {
        return dowagerBaseTitle;
    }

    public void setDowagerBaseTitle(NobleTitle dowagerBaseTitle) {
        this.dowagerBaseTitle = normalize(dowagerBaseTitle);
    }

    public UUID getDowagerSourceSpouseId() {
        return dowagerSourceSpouseId;
    }

    public void setDowagerSourceSpouseId(UUID dowagerSourceSpouseId) {
        this.dowagerSourceSpouseId = dowagerSourceSpouseId;
    }

    public boolean isCommander() {
        return commander;
    }

    public void setCommander(boolean commander) {
        this.commander = commander;
    }

    public String getCachedPlayerName() {
        return cachedPlayerName;
    }

    public void setCachedPlayerName(String cachedPlayerName) {
        this.cachedPlayerName = cachedPlayerName;
    }

    public boolean isCommoner() {
        return grantedTitle == NobleTitle.COMMONER;
    }

    public boolean hasMarriageTitle() {
        return marriageTitle != NobleTitle.COMMONER;
    }

    public boolean hasDowagerTitle() {
        return dowagerBaseTitle != NobleTitle.COMMONER;
    }

    public void clearMarriageTitle() {
        this.marriageTitle = NobleTitle.COMMONER;
        this.marriageSourceSpouseId = null;
    }

    public void clearDowagerTitle() {
        this.dowagerBaseTitle = NobleTitle.COMMONER;
        this.dowagerSourceSpouseId = null;
    }

    public boolean hasAnyCapitalOffice() {
        return !isCommoner() || hasMarriageTitle() || hasDowagerTitle() || commander;
    }

    private static NobleTitle normalize(NobleTitle title) {
        return title == null ? NobleTitle.COMMONER : title;
    }
}