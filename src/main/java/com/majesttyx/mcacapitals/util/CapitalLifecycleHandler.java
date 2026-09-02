package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalLifecycleHandler {

    public void onServerStarted(MinecraftServer server) {
        FabricServerAccess.setServer(server);
        CapitalRuntimeStateReset.clearServerSessionState();
        CapitalRuntimeStateReset.clearResourceCaches();
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();

        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        CapitalSavedData data = CapitalDataAccess.get(overworld);
        for (CapitalRecord capital : data.getCapitals()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }
            CapitalManager.putCapital(capital);
        }

        boolean repairedPersistentState = migrateLegacyVillageDimensions(server);
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            repairedPersistentState |= repairStoredGenderFlags(overworld, capital);
        }
        if (repairedPersistentState) {
            CapitalDataAccess.markDirty(overworld);
        }

        for (CapitalRecord capital : data.getCapitals()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }

            ServerLevel capitalLevel = CapitalManager.getCapitalLevel(server, capital);
            if (capitalLevel == null
                    && capital.getVillageDimensionId() == null
                    && capital.getVillageId() != null
                    && MCAIntegrationBridge.hasVillage(overworld, capital.getVillageId())) {
                capitalLevel = overworld;
            }

            if (capitalLevel != null) {
                CapitalCourtWatcher.seedCurrentState(capitalLevel, capital);
            }
        }
    }

    private static boolean repairStoredGenderFlags(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return false;
        }

        boolean changed = false;

        UUID sovereignId = capital.getSovereign() != null
                ? capital.getSovereign()
                : capital.getPlayerSovereignId();
        java.util.Optional<Boolean> sovereignFemale = MCAIntegrationBridge.getFemaleIfKnown(level, sovereignId);
        if (sovereignFemale.isPresent() && capital.isSovereignFemale() != sovereignFemale.get()) {
            capital.setSovereignFemale(sovereignFemale.get());
            changed = true;
        }

        UUID consortId = capital.getConsort() != null
                ? capital.getConsort()
                : capital.getPlayerConsortId();
        java.util.Optional<Boolean> consortFemale = MCAIntegrationBridge.getFemaleIfKnown(level, consortId);
        if (consortFemale.isPresent() && capital.isConsortFemale() != consortFemale.get()) {
            capital.setConsortFemale(consortFemale.get());
            changed = true;
        }

        changed |= repairSingleGender(level, capital.getDowager(), capital.isDowagerFemale(), capital::setDowagerFemale);
        changed |= repairSingleGender(level, capital.getHeir(), capital.isHeirFemale(), capital::setHeirFemale);
        changed |= repairSingleGender(level, capital.getCommander(), capital.isCommanderFemale(), capital::setCommanderFemale);
        changed |= repairSingleGender(level, capital.getHand(), capital.isHandFemale(), capital::setHandFemale);
        changed |= repairSingleGender(level, capital.getHerald(), capital.isHeraldFemale(), capital::setHeraldFemale);
        changed |= repairSingleGender(level, capital.getGrandMaester(), capital.isGrandMaesterFemale(), capital::setGrandMaesterFemale);
        changed |= repairSingleGender(level, capital.getMasterOfLaws(), capital.isMasterOfLawsFemale(), capital::setMasterOfLawsFemale);

        changed |= repairGenderMap(level, capital.getRoyalChildren(), capital.getRoyalChildFemale());
        changed |= repairGenderMap(level, capital.getLegitimizedRoyalChildren(), capital.getLegitimizedRoyalChildFemale());
        changed |= repairGenderMap(level, capital.getPrinceConsortSources().keySet(), capital.getPrinceConsortFemale());
        changed |= repairGenderMap(level, capital.getDowagerPrinceSources().keySet(), capital.getDowagerPrinceFemale());
        changed |= repairGenderMap(level, capital.getDukes(), capital.getDukeFemale());
        changed |= repairGenderMap(level, capital.getMarriageDukeSources().keySet(), capital.getMarriageDukeFemale());
        changed |= repairGenderMap(level, capital.getDowagerDukeSources().keySet(), capital.getDowagerDukeFemale());
        changed |= repairGenderMap(level, capital.getLords(), capital.getLordFemale());
        changed |= repairGenderMap(level, capital.getKnights(), capital.getKnightFemale());
        changed |= repairGenderMap(level, capital.getRoyalGuards(), capital.getRoyalGuardFemale());

        return changed;
    }

    private static boolean repairSingleGender(
            ServerLevel level,
            UUID entityId,
            boolean currentValue,
            java.util.function.Consumer<Boolean> setter
    ) {
        java.util.Optional<Boolean> resolved = MCAIntegrationBridge.getFemaleIfKnown(level, entityId);
        if (resolved.isEmpty() || resolved.get() == currentValue) {
            return false;
        }
        setter.accept(resolved.get());
        return true;
    }

    private static boolean repairGenderMap(
            ServerLevel level,
            Iterable<UUID> ids,
            Map<UUID, Boolean> stored
    ) {
        if (ids == null || stored == null) {
            return false;
        }

        boolean changed = false;
        for (UUID entityId : ids) {
            java.util.Optional<Boolean> resolved = MCAIntegrationBridge.getFemaleIfKnown(level, entityId);
            if (resolved.isEmpty()) {
                continue;
            }
            Boolean previous = stored.put(entityId, resolved.get());
            if (previous == null || previous.booleanValue() != resolved.get()) {
                changed = true;
            }
        }
        return changed;
    }

    private static boolean migrateLegacyVillageDimensions(MinecraftServer server) {
        boolean changed = false;
        if (server == null) {
            return false;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null
                    || capital.getVillageId() == null
                    || capital.getVillageDimensionId() != null) {
                continue;
            }

            List<ServerLevel> candidates = new ArrayList<>();
            for (ServerLevel level : server.getAllLevels()) {
                if (MCAIntegrationBridge.hasVillage(level, capital.getVillageId())) {
                    candidates.add(level);
                }
            }

            ServerLevel resolved = resolveLegacyCapitalLevel(server, capital, candidates);
            if (resolved != null) {
                capital.setVillageDimensionId(CapitalManager.getDimensionId(resolved));
                changed = true;
            }
        }

        return changed;
    }

    private static ServerLevel resolveLegacyCapitalLevel(
            MinecraftServer server,
            CapitalRecord capital,
            List<ServerLevel> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Set<UUID> knownMembers = knownCapitalMembers(capital);
        ServerLevel memberMatch = null;
        for (ServerLevel candidate : candidates) {
            Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(candidate, capital.getVillageId());
            boolean matches = knownMembers.stream().anyMatch(residents::contains);
            if (!matches) {
                continue;
            }
            if (memberMatch != null) {
                memberMatch = null;
                break;
            }
            memberMatch = candidate;
        }
        if (memberMatch != null) {
            return memberMatch;
        }

        ServerLevel overworld = server.overworld();
        if (overworld != null && candidates.contains(overworld)) {
            return overworld;
        }

        return null;
    }

    private static Set<UUID> knownCapitalMembers(CapitalRecord capital) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (capital.getSovereign() != null) ids.add(capital.getSovereign());
        if (capital.getConsort() != null) ids.add(capital.getConsort());
        if (capital.getDowager() != null) ids.add(capital.getDowager());
        if (capital.getHeir() != null) ids.add(capital.getHeir());
        if (capital.getCommander() != null) ids.add(capital.getCommander());
        if (capital.getHand() != null) ids.add(capital.getHand());
        if (capital.getHerald() != null) ids.add(capital.getHerald());
        if (capital.getGrandMaester() != null) ids.add(capital.getGrandMaester());
        if (capital.getMasterOfLaws() != null) ids.add(capital.getMasterOfLaws());
        ids.addAll(capital.getRoyalChildren());
        ids.addAll(capital.getDukes());
        ids.addAll(capital.getLords());
        ids.addAll(capital.getKnights());
        ids.addAll(capital.getRoyalGuards());
        return ids;
    }


    public void onServerStopped(MinecraftServer server) {
        CapitalRuntimeStateReset.clearServerSessionState();
        CapitalRuntimeStateReset.clearResourceCaches();
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();
        FabricServerAccess.clearServer(server);
    }
}