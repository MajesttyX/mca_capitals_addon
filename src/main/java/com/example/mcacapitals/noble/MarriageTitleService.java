package com.example.mcacapitals.noble;

import java.util.UUID;

public class MarriageTitleService {

    private MarriageTitleService() {
    }

    public static void applyVillagerSpouseTitle(UUID spouseId, UUID rulerOrNobleId, boolean spouseIsFemale) {
        NobleTitle sourceTitle = NobleManager.getTitle(rulerOrNobleId);
        NobleTitle spouseTitle = ConsortTitleResolver.getSpouseTitle(sourceTitle, spouseIsFemale);

        NobleRecord spouseRecord = NobleManager.getOrCreate(spouseId);
        spouseRecord.setDirectTitle(spouseTitle);
        spouseRecord.setTitleGrantedByMarriage(true);

        NobleRecord sourceRecord = NobleManager.get(rulerOrNobleId);
        if (sourceRecord != null) {
            spouseRecord.setCapitalId(sourceRecord.getCapitalId());
        }
    }

    public static void removeMarriageTitle(UUID spouseId) {
        NobleRecord spouseRecord = NobleManager.get(spouseId);
        if (spouseRecord == null) {
            return;
        }

        if (spouseRecord.isTitleGrantedByMarriage()) {
            spouseRecord.setDirectTitle(NobleTitle.COMMONER);
            spouseRecord.setTitleGrantedByMarriage(false);
        }
    }

    public static NobleTitle resolvePlayerSpouseTitle(boolean playerIsFemale) {
        return playerIsFemale ? NobleTitle.QUEEN : NobleTitle.KING;
    }
}