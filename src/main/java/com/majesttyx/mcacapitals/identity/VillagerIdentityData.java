package com.majesttyx.mcacapitals.identity;

import java.util.UUID;

public record VillagerIdentityData(
        UUID originVillageUuid,
        Integer originVillageId,
        String originVillageName,
        UUID originCapitalId,
        String originCapitalName,
        String originSource,
        long originSetAtGameTime,
        String originDimension,
        int originBlockX,
        int originBlockY,
        int originBlockZ,
        String birthSurname,
        String currentSurname,
        String surnameSource,
        long surnameSetAtGameTime,
        boolean houseFounded,
        String houseName,
        String houseWords,
        String houseWordsPersonality,
        UUID houseFounderId,
        String houseFounderName,
        long houseFoundedAtGameTime,
        UUID houseFoundedInCapitalId,
        String houseFoundedInCapitalName
) {
    public boolean hasOrigin() {
        return originVillageId != null && originVillageName != null && !originVillageName.isBlank();
    }

    public boolean hasSurname() {
        return currentSurname != null && !currentSurname.isBlank();
    }

    public boolean hasFoundedHouse() {
        return houseFounded && houseName != null && !houseName.isBlank();
    }

    public boolean hasHouseWords() {
        return houseWords != null && !houseWords.isBlank();
    }
}