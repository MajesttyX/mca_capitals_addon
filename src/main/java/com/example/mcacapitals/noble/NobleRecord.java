package com.example.mcacapitals.noble;

import java.util.UUID;

public class NobleRecord {
    private final UUID villagerId;
    private NobleTitle directTitle;
    private UUID capitalId;
    private boolean titleGrantedByMarriage;
    private int successionOrder;

    public NobleRecord(UUID villagerId, NobleTitle directTitle) {
        this.villagerId = villagerId;
        this.directTitle = directTitle;
    }

    public UUID getVillagerId() {
        return villagerId;
    }

    public NobleTitle getDirectTitle() {
        return directTitle;
    }

    public void setDirectTitle(NobleTitle directTitle) {
        this.directTitle = directTitle;
    }

    public UUID getCapitalId() {
        return capitalId;
    }

    public void setCapitalId(UUID capitalId) {
        this.capitalId = capitalId;
    }

    public boolean isTitleGrantedByMarriage() {
        return titleGrantedByMarriage;
    }

    public void setTitleGrantedByMarriage(boolean titleGrantedByMarriage) {
        this.titleGrantedByMarriage = titleGrantedByMarriage;
    }

    public int getSuccessionOrder() {
        return successionOrder;
    }

    public void setSuccessionOrder(int successionOrder) {
        this.successionOrder = successionOrder;
    }
}
