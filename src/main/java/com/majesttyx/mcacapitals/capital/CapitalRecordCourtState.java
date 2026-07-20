package com.majesttyx.mcacapitals.capital;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CapitalRecordCourtState {
    boolean playerSovereign;
    UUID playerSovereignId;
    String playerSovereignName;
    boolean playerConsort;
    UUID playerConsortId;
    String playerConsortName;
    boolean monarchyRejected;
    boolean royalCharterIssued;
    boolean mourningActive;
    long mourningEndDay;
    final List<String> chronicleEntries = new ArrayList<>();
    final Map<UUID, String> mourningOriginalClothes = new LinkedHashMap<>();
    UUID commander;
    boolean commanderFemale;
    UUID hand;
    boolean handFemale;
    UUID herald;
    boolean heraldFemale;
    String heraldDisplayName;
    UUID grandMaester;
    boolean grandMaesterFemale;
    UUID masterOfLaws;
    boolean masterOfLawsFemale;
    UUID lastCrownStandingSovereign;
    long lastNaturalDukedomDay;
    final Map<UUID, CrownStanding> crownStandings = new LinkedHashMap<>();
    long lastCommanderRaidBlessingGameTime;
    long lastCommanderRandomBlessingDay;
}