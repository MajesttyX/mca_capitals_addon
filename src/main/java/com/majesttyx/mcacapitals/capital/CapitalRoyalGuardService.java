package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalRoyalGuardService {

    public static final int REQUIRED_POPULATION = 25;
    public static final int MAX_ROYAL_GUARDS = 2;

    private CapitalRoyalGuardService() {
    }

    public static boolean tickRoyalGuards(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        boolean changed = false;

        changed |= handleSovereignChange(level, capital);

        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            if (!isValidRoyalGuard(level, capital, guardId, residents)) {
                capital.removeRoyalGuard(guardId);
                changed = true;
            }
        }

        if (capital.getSovereign() == null) {
            if (changed) {
                CapitalNameService.refreshCapitalNames(level, capital, residents);
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            }
            return changed;
        }

        if (capital.getRoyalGuardLiege() == null) {
            capital.setRoyalGuardLiege(capital.getSovereign());
            changed = true;
        }

        if (MCAIntegrationBridge.isMCAVillager(level, capital.getSovereign())) {
            while (capital.getRoyalGuards().size() < MAX_ROYAL_GUARDS
                    && isEligibleForNewRoyalGuard(level, capital)) {
                UUID candidate = findBestCandidate(level, capital, residents);
                if (candidate == null) {
                    break;
                }

                appointRoyalGuard(level, capital, candidate);
                changed = true;
            }
        } else if (capital.getRoyalGuards().size() < MAX_ROYAL_GUARDS
                && isEligibleForNewRoyalGuard(level, capital)) {
            maybePromptPlayerSovereign(level, capital, residents);
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean clearRoyalGuardsForTransfer(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return false;
        }

        if (capital.getRoyalGuards().isEmpty() && capital.getRoyalGuardLiege() == null) {
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean changed = false;

        for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
            String guardName = buildRoyalGuardHistoryName(level, guardId);
            capital.removeRoyalGuard(guardId);

            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    guardName
                            + " was released from the royal guard of "
                            + villageName
                            + " after the transfer of power."
            );

            changed = true;
        }

        capital.setRoyalGuardLiege(capital.getSovereign());
        capital.setLastRoyalGuardPromptDay(0L);

        if (changed) {
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static List<UUID> getValidCandidates(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> result = new ArrayList<>();

        for (UUID residentId : residents) {
            if (!isCandidate(level, capital, residentId)) {
                continue;
            }

            result.add(residentId);
        }

        result.sort(Comparator
                .comparing((UUID id) -> !CapitalCrownJusticeService.isRecognizedFriend(level, capital, id))
                .thenComparing(UUID::toString));
        return result;
    }

    public static boolean appointRoyalGuard(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (villagerId == null || capital == null || level == null) {
            return false;
        }

        if (!isCandidate(level, capital, villagerId)) {
            return false;
        }

        if (capital.getRoyalGuards().size() >= MAX_ROYAL_GUARDS) {
            return false;
        }

        if (!isEligibleForNewRoyalGuard(level, capital)) {
            return false;
        }

        if (capital.getRoyalGuardLiege() == null) {
            capital.setRoyalGuardLiege(capital.getSovereign());
        }

        capital.addRoyalGuard(
                villagerId,
                MCAIntegrationBridge.isFemale(level, villagerId),
                capital.getRoyalGuardLiege()
        );

        String guardName = buildRoyalGuardDisplayName(level, capital, villagerId);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CapitalChronicleService.addEntry(
                level,
                capital,
                guardName + " was named to the royal guard of " + villageName + "."
        );

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        return true;
    }

    private static boolean handleSovereignChange(
            ServerLevel level,
            CapitalRecord capital
    ) {
        UUID sovereign = capital.getSovereign();
        UUID liege = capital.getRoyalGuardLiege();

        if (sovereign == null) {
            if (liege != null || !capital.getRoyalGuards().isEmpty()) {
                for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
                    recordDisgrace(level, capital, guardId);
                    capital.disgraceRoyalGuard(guardId);
                }

                capital.setRoyalGuardLiege(null);
                return true;
            }

            return false;
        }

        if (liege == null) {
            capital.setRoyalGuardLiege(sovereign);
            return true;
        }

        if (!liege.equals(sovereign)) {
            for (UUID guardId : new ArrayList<>(capital.getRoyalGuards())) {
                recordDisgrace(level, capital, guardId);
                capital.disgraceRoyalGuard(guardId);
            }

            capital.setRoyalGuardLiege(sovereign);
            return true;
        }

        return false;
    }

    private static void recordDisgrace(
            ServerLevel level,
            CapitalRecord capital,
            UUID guardId
    ) {
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String name = buildRoyalGuardHistoryName(level, guardId);

        CapitalChronicleService.addEntry(
                level,
                capital,
                name
                        + " was disgraced after failing to preserve the reign of "
                        + villageName
                        + "."
        );
    }

    public static String buildRoyalGuardDisplayName(
            ServerLevel level,
            CapitalRecord capital,
            UUID guardId
    ) {
        String title = MCAIntegrationBridge.isFemale(level, guardId)
                ? "Dame"
                : "Sir";

        String baseName = resolveBaseName(level, guardId);

        return title
                + " "
                + baseName
                + " of the "
                + (capital.isSovereignFemale() ? "Queensguard" : "Kingsguard");
    }

    private static String buildRoyalGuardHistoryName(
            ServerLevel level,
            UUID guardId
    ) {
        String title = MCAIntegrationBridge.isFemale(level, guardId)
                ? "Dame"
                : "Sir";

        String baseName = resolveBaseName(level, guardId);
        return title + " " + baseName;
    }

    private static String resolveBaseName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);

        if (entity == null) {
            return entityId.toString();
        }

        String currentName = entity.getName().getString();

        currentName = currentName
                .replace(" of the Kingsguard", "")
                .replace(" of the Queensguard", "")
                .trim();

        String[] prefixes = {
                "High Queen ",
                "High King ",
                "Dowager Queen ",
                "Dowager King ",
                "Queen Consort ",
                "King Consort ",
                "Heir Apparent ",
                "Crown Princess ",
                "Crown Prince ",
                "Dowager Princess ",
                "Dowager Prince ",
                "Princess Consort ",
                "Prince Consort ",
                "Hand of the Queen ",
                "Hand of the King ",
                "Grand Maester ",
                "Master of Laws ",
                "Maester ",
                "Court Herald ",
                "Ambassador ",
                "Princess ",
                "Prince ",
                "Lord Commander ",
                "Dowager Duchess ",
                "Dowager Duke ",
                "Duchess ",
                "Duke ",
                "Lady ",
                "Lord ",
                "Dame ",
                "Sir ",
                "Queen ",
                "King "
        };

        boolean changed = true;

        while (changed) {
            changed = false;

            for (String prefix : prefixes) {
                if (currentName.startsWith(prefix)) {
                    currentName = currentName.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return currentName.isBlank()
                ? entityId.toString()
                : currentName;
    }

    private static boolean isValidRoyalGuard(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents
    ) {
        if (villagerId == null || capital == null || level == null) {
            return false;
        }

        if (capital.getSovereign() == null) {
            return false;
        }

        if (!CapitalRoleValidation.isExistingRoleStillResolvable(
                level,
                villagerId,
                residents
        )) {
            return false;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return false;
        }

        if (CapitalAmbassadorService.isAmbassador(level, villagerId)) {
            return false;
        }

        if (!CapitalRoleValidation.isCurrentlyLoaded(level, villagerId)) {
            return capital.getRoyalGuards().contains(villagerId);
        }

        if (!MCAIntegrationBridge.isMCAGuard(level, villagerId)) {
            return false;
        }

        return capital.getRoyalGuards().contains(villagerId);
    }

    private static boolean isEligibleForNewRoyalGuard(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return false;
        }

        return MCAIntegrationBridge.getVillagePopulation(
                level,
                capital.getVillageId()
        ) >= REQUIRED_POPULATION;
    }

    private static UUID findBestCandidate(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> valid = getValidCandidates(level, capital, residents);

        if (valid.isEmpty()) {
            return null;
        }

        return valid.get(0);
    }

    private static boolean isCandidate(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (villagerId == null || capital == null || level == null) {
            return false;
        }

        if (capital.getRoyalGuards().contains(villagerId)) {
            return false;
        }

        if (!CapitalCrownJusticeService.isTrustedOfficeEligible(level, capital, villagerId)) {
            return false;
        }

        if (CapitalAmbassadorService.isAmbassador(level, villagerId)) {
            return false;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return false;
        }

        if (villagerId.equals(capital.getCommander())) {
            return false;
        }

        if (villagerId.equals(capital.getHand())) {
            return false;
        }

        if (villagerId.equals(capital.getGrandMaester())) {
            return false;
        }

        if (villagerId.equals(capital.getHerald())) {
            return false;
        }

        if (villagerId.equals(capital.getHeir())) {
            return false;
        }

        if (villagerId.equals(capital.getConsort())) {
            return false;
        }

        if (villagerId.equals(capital.getDowager())) {
            return false;
        }

        if (capital.isRoyalChild(villagerId)) {
            return false;
        }

        if (capital.isLegitimizedRoyalChild(villagerId)) {
            return false;
        }

        if (capital.isPrinceConsort(villagerId)) {
            return false;
        }

        if (capital.isDowagerPrince(villagerId)) {
            return false;
        }

        if (capital.isDuke(villagerId)
                || capital.isMarriageDuke(villagerId)
                || capital.isDowagerDuke(villagerId)) {
            return false;
        }

        if (capital.isLord(villagerId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(level, villagerId)) {
            return false;
        }

        return MCAIntegrationBridge.isMCAGuard(level, villagerId);
    }

    private static void maybePromptPlayerSovereign(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        long currentDay = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        if (capital.getLastRoyalGuardPromptDay() == currentDay) {
            return;
        }

        List<UUID> candidates = getValidCandidates(level, capital, residents);

        if (candidates.isEmpty()) {
            return;
        }

        UUID playerId = capital.getPlayerSovereignId();

        if (playerId == null) {
            return;
        }

        ServerPlayer sovereign =
                level.getServer().getPlayerList().getPlayer(playerId);

        if (sovereign == null) {
            return;
        }

        capital.setLastRoyalGuardPromptDay(currentDay);
        CapitalDataAccess.markDirty(level);

        sovereign.sendSystemMessage(Component.literal(
                "Your capital can appoint up to "
                        + MAX_ROYAL_GUARDS
                        + " royal guards. "
        ));
    }
}