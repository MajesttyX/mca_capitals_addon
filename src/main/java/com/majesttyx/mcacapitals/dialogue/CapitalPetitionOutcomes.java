package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleEntry;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalRoyalGuardService;
import com.majesttyx.mcacapitals.capital.CapitalRoyalHouseholdService;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class CapitalPetitionOutcomes {

    private CapitalPetitionOutcomes() {
    }

    static void performCoup(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        CapitalInterregnumRecord interregnum =
                CapitalWartimeSuccessionService.getRecord(
                        level,
                        capital.getCapitalId()
                );

        UUID formerSovereignId = capital.getSovereign();
        if (formerSovereignId == null && interregnum != null) {
            formerSovereignId = interregnum.getDeceasedSovereignId();
        }

        Object formerSovereignName = formerSovereignId == null
                ? CapitalChronicleService.translatable("mcacapitals.chronicle.identity.vacant_throne")
                : CapitalChronicleIdentitySnapshot.name(level, capital, formerSovereignId);
        CapitalChronicleEntry.Argument formerSovereignTitle = formerSovereignId == null
                ? CapitalChronicleService.translatable("mcacapitals.dynamic.title.none")
                : CapitalChronicleIdentitySnapshot.title(level, capital, formerSovereignId);
        CapitalChronicleEntry.Argument formerSovereignStyle = formerSovereignId == null
                ? CapitalChronicleService.translatable("mcacapitals.dynamic.title.none")
                : CapitalChronicleIdentitySnapshot.style(level, capital, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        Set<UUID> formerRoyalFamily = collectRoyalFamily(capital);
        if (formerSovereignId != null) {
            formerRoyalFamily.add(formerSovereignId);
        }
        formerRoyalFamily.remove(player.getUUID());

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        stripFormerRoyalFamily(capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        applyHeartsPenaltyToFamily(level, formerRoyalFamily, player.getUUID(), -200);

        if (interregnum != null
                && interregnum.mayVictoriousPlayerSeize(player.getUUID())) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.THRONE_SEIZED_VACANT_DEPOSITION,
                    playerName,
                    villageName,
                    CapitalChronicleIdentitySnapshot.title(level, capital, player.getUUID()),
                    CapitalChronicleIdentitySnapshot.style(level, capital, player.getUUID())
            );
        } else {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.THRONE_SEIZED_BY_FORCE,
                    playerName,
                    villageName,
                    formerSovereignName,
                    CapitalChronicleIdentitySnapshot.title(level, capital, player.getUUID()),
                    CapitalChronicleIdentitySnapshot.style(level, capital, player.getUUID()),
                    formerSovereignTitle,
                    formerSovereignStyle
            );
        }

        CapitalWartimeSuccessionService.clear(
                level,
                capital.getCapitalId()
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    static void applyCapitalPenalty(ServerLevel level, Set<UUID> residents, UUID playerId, int delta) {
        if (level == null || residents == null || playerId == null) {
            return;
        }

        for (UUID residentId : residents) {
            adjustHearts(level, residentId, playerId, delta);
        }
    }

    static void peacefulTransferByPetition(ServerLevel level, CapitalRecord capital, ServerPlayer player, UUID formerSovereignId) {
        String formerSovereignName = CapitalChronicleIdentitySnapshot.name(level, capital, formerSovereignId);
        CapitalChronicleEntry.Argument formerSovereignTitle =
                CapitalChronicleIdentitySnapshot.title(level, capital, formerSovereignId);
        CapitalChronicleEntry.Argument formerSovereignStyle =
                CapitalChronicleIdentitySnapshot.style(level, capital, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        stripFormerRoyalFamily(capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.PETITION_PEACEFUL_TRANSFER,
                formerSovereignName,
                playerName,
                villageName,
                formerSovereignTitle,
                formerSovereignStyle,
                CapitalChronicleIdentitySnapshot.title(level, capital, player.getUUID()),
                CapitalChronicleIdentitySnapshot.style(level, capital, player.getUUID())
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static Set<UUID> collectRoyalFamily(CapitalRecord capital) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (capital == null) {
            return ids;
        }

        if (capital.getSovereign() != null) ids.add(capital.getSovereign());
        if (capital.getConsort() != null) ids.add(capital.getConsort());
        if (capital.getDowager() != null) ids.add(capital.getDowager());
        if (capital.getHeir() != null) ids.add(capital.getHeir());

        ids.addAll(capital.getRoyalChildren());
        ids.addAll(capital.getDisinheritedRoyalChildren());
        ids.addAll(capital.getLegitimizedRoyalChildren());

        return ids;
    }

    private static void stripFormerRoyalFamily(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setDowager(null);
        capital.setDowagerFemale(false);
        capital.setHeir(null);
        capital.setHeirFemale(false);
        capital.setHeirMode(CapitalRecord.HeirMode.NONE);

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();
        capital.getDisinheritedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildFemale().clear();
        capital.getRoyalSuccessionOrder().clear();
        capital.clearRoyalHousehold();
    }

    private static void applyHeartsPenaltyToFamily(ServerLevel level, Set<UUID> familyIds, UUID playerId, int delta) {
        if (level == null || familyIds == null || playerId == null) {
            return;
        }

        for (UUID familyId : familyIds) {
            adjustHearts(level, familyId, playerId, delta);
        }
    }

    private static void adjustHearts(ServerLevel level, UUID villagerId, UUID playerId, int delta) {
        if (!MCAIntegrationBridge.adjustHearts(level, villagerId, playerId, delta)) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to adjust hearts for villager='{}' player='{}'",
                    villagerId,
                    playerId
            );
        }
    }

    private static String resolveLoadedName(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }

        var entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}