package com.example.mcacapitals.capital;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CapitalRecordCourtOps {

    private CapitalRecordCourtOps() {
    }

    static boolean isPlayerSovereign(CapitalRecord record) {
        return record.court.playerSovereign;
    }

    static void setPlayerSovereign(CapitalRecord record, boolean playerSovereign) {
        record.court.playerSovereign = playerSovereign;
    }

    static UUID getPlayerSovereignId(CapitalRecord record) {
        return record.court.playerSovereignId;
    }

    static void setPlayerSovereignId(CapitalRecord record, UUID playerSovereignId) {
        record.court.playerSovereignId = playerSovereignId;
    }

    static String getPlayerSovereignName(CapitalRecord record) {
        return record.court.playerSovereignName;
    }

    static void setPlayerSovereignName(CapitalRecord record, String playerSovereignName) {
        record.court.playerSovereignName = playerSovereignName;
    }

    static boolean isPlayerConsort(CapitalRecord record) {
        return record.court.playerConsort;
    }

    static void setPlayerConsort(CapitalRecord record, boolean playerConsort) {
        record.court.playerConsort = playerConsort;
    }

    static UUID getPlayerConsortId(CapitalRecord record) {
        return record.court.playerConsortId;
    }

    static void setPlayerConsortId(CapitalRecord record, UUID playerConsortId) {
        record.court.playerConsortId = playerConsortId;
    }

    static String getPlayerConsortName(CapitalRecord record) {
        return record.court.playerConsortName;
    }

    static void setPlayerConsortName(CapitalRecord record, String playerConsortName) {
        record.court.playerConsortName = playerConsortName;
    }

    static boolean isMonarchyRejected(CapitalRecord record) {
        return record.court.monarchyRejected;
    }

    static void setMonarchyRejected(CapitalRecord record, boolean monarchyRejected) {
        record.court.monarchyRejected = monarchyRejected;
    }

    static boolean isMourningActive(CapitalRecord record) {
        return record.court.mourningActive;
    }

    static void setMourningActive(CapitalRecord record, boolean mourningActive) {
        record.court.mourningActive = mourningActive;
    }

    static long getMourningEndDay(CapitalRecord record) {
        return record.court.mourningEndDay;
    }

    static void setMourningEndDay(CapitalRecord record, long mourningEndDay) {
        record.court.mourningEndDay = mourningEndDay;
    }

    static List<String> getChronicleEntries(CapitalRecord record) {
        return record.court.chronicleEntries;
    }

    static void addChronicleEntry(CapitalRecord record, String entry) {
        if (entry != null && !entry.isBlank()) {
            record.court.chronicleEntries.add(entry);
        }
    }

    static Map<UUID, String> getMourningOriginalClothes(CapitalRecord record) {
        return record.court.mourningOriginalClothes;
    }

    static UUID getCommander(CapitalRecord record) {
        return record.court.commander;
    }

    static void setCommander(CapitalRecord record, UUID commander) {
        record.court.commander = commander;
    }

    static boolean isCommanderFemale(CapitalRecord record) {
        return record.court.commanderFemale;
    }

    static void setCommanderFemale(CapitalRecord record, boolean commanderFemale) {
        record.court.commanderFemale = commanderFemale;
    }

    static UUID getHand(CapitalRecord record) {
        return record.court.hand;
    }

    static void setHand(CapitalRecord record, UUID hand) {
        record.court.hand = hand;
    }

    static boolean isHandFemale(CapitalRecord record) {
        return record.court.handFemale;
    }

    static void setHandFemale(CapitalRecord record, boolean handFemale) {
        record.court.handFemale = handFemale;
    }

    static UUID getHerald(CapitalRecord record) {
        return record.court.herald;
    }

    static void setHerald(CapitalRecord record, UUID herald) {
        record.court.herald = herald;
    }

    static boolean isHeraldFemale(CapitalRecord record) {
        return record.court.heraldFemale;
    }

    static void setHeraldFemale(CapitalRecord record, boolean heraldFemale) {
        record.court.heraldFemale = heraldFemale;
    }

    static UUID getGrandMaester(CapitalRecord record) {
        return record.court.grandMaester;
    }

    static void setGrandMaester(CapitalRecord record, UUID grandMaester) {
        record.court.grandMaester = grandMaester;
    }

    static boolean isGrandMaesterFemale(CapitalRecord record) {
        return record.court.grandMaesterFemale;
    }

    static void setGrandMaesterFemale(CapitalRecord record, boolean grandMaesterFemale) {
        record.court.grandMaesterFemale = grandMaesterFemale;
    }

    static long getLastCommanderRaidBlessingGameTime(CapitalRecord record) {
        return record.court.lastCommanderRaidBlessingGameTime;
    }

    static void setLastCommanderRaidBlessingGameTime(CapitalRecord record, long value) {
        record.court.lastCommanderRaidBlessingGameTime = value;
    }

    static long getLastCommanderRandomBlessingDay(CapitalRecord record) {
        return record.court.lastCommanderRandomBlessingDay;
    }

    static void setLastCommanderRandomBlessingDay(CapitalRecord record, long value) {
        record.court.lastCommanderRandomBlessingDay = value;
    }
}