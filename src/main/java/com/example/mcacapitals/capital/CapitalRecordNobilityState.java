package com.example.mcacapitals.capital;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordNobilityState {
    final Set<UUID> dukes = new LinkedHashSet<>();
    final Map<UUID, Boolean> dukeFemale = new LinkedHashMap<>();
    final Set<UUID> lords = new LinkedHashSet<>();
    final Map<UUID, Boolean> lordFemale = new LinkedHashMap<>();
    final Set<UUID> knights = new LinkedHashSet<>();
    final Map<UUID, Boolean> knightFemale = new LinkedHashMap<>();
    final Map<UUID, UUID> marriageDukeSources = new LinkedHashMap<>();
    final Map<UUID, Boolean> marriageDukeFemale = new LinkedHashMap<>();
    final Map<UUID, UUID> dowagerDukeSources = new LinkedHashMap<>();
    final Map<UUID, Boolean> dowagerDukeFemale = new LinkedHashMap<>();
}