package com.example.mcacapitals.player;

import com.example.mcacapitals.noble.NobleTitle;

import java.util.UUID;

public class PlayerCapitalTitleRecord {

    private final UUID playerId;
    private final UUID capitalId;

    private NobleTitle grantedTitle;
    private boolean commander;
    private String cachedPlayerName;

    public PlayerCapitalTitleRecord(UUID playerId, UUID capitalId) {
        this.playerId = playerId;
        this.capitalId = capitalId;
        this.grantedTitle = NobleTitle.COMMONER;
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
        this.grantedTitle = grantedTitle == null ? NobleTitle.COMMONER : grantedTitle;
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
        return grantedTitle == null || grantedTitle == NobleTitle.COMMONER;
    }

    public boolean hasAnyCapitalOffice() {
        return !isCommoner() || commander;
    }
}