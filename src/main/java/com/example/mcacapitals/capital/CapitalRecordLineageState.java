package com.example.mcacapitals.capital;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordLineageState {
    final Set<UUID> royalChildren = new LinkedHashSet<>();
    final Map<UUID, Boolean> royalChildFemale = new LinkedHashMap<>();
    final Set<UUID> royalHousehold = new LinkedHashSet<>();
    final Set<UUID> disinheritedRoyalChildren = new LinkedHashSet<>();
    final Set<UUID> legitimizedRoyalChildren = new LinkedHashSet<>();
    final Map<UUID, Boolean> legitimizedRoyalChildFemale = new LinkedHashMap<>();
    final List<UUID> royalSuccessionOrder = new ArrayList<>();
    final Map<UUID, UUID> princeConsortSources = new LinkedHashMap<>();
    final Map<UUID, Boolean> princeConsortFemale = new LinkedHashMap<>();
    final Map<UUID, UUID> dowagerPrinceSources = new LinkedHashMap<>();
    final Map<UUID, Boolean> dowagerPrinceFemale = new LinkedHashMap<>();
}