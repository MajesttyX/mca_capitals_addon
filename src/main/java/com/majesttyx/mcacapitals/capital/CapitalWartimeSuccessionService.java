package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalInterregnumDataAccess;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalWartimeSuccessionService {

    public static final long MINIMUM_INTERREGNUM_TICKS = 1_200L;

    private CapitalWartimeSuccessionService() {
    }

    public static boolean handleIfNeeded(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return false;
        }

        CapitalInterregnumRecord existing =
                CapitalInterregnumDataAccess.getRecord(
                        level,
                        capital.getCapitalId()
                );

        if (existing != null) {
            existing = normalizeInterregnumRecord(
                    level,
                    capital,
                    existing
            );
            return tickInterregnum(level, capital, existing);
        }

        UUID sovereignId = capital.getSovereign();
        if (sovereignId == null
                || isSurvivalPlayerSovereignDeath(level, capital)
                || isValidLivingSovereign(level, sovereignId)
                || !isMilitaryCrisis(level, capital)) {
            return false;
        }

        beginInterregnum(level, capital, sovereignId);
        return true;
    }

    public static boolean isInInterregnum(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalInterregnumDataAccess.getRecord(
                level,
                capitalId
        ) != null;
    }

    public static CapitalInterregnumRecord getRecord(
            ServerLevel level,
            UUID capitalId
    ) {
        CapitalInterregnumRecord record =
                CapitalInterregnumDataAccess.getRecord(
                        level,
                        capitalId
                );
        if (record == null) {
            return null;
        }
        return normalizeInterregnumRecord(
                level,
                CapitalManager.getCapital(capitalId),
                record
        );
    }

    public static String getStatusLine(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return "";
        }

        CapitalInterregnumRecord record = getRecord(
                level,
                capital.getCapitalId()
        );
        if (record == null) {
            return "";
        }

        long remaining = Math.max(
                0L,
                record.getResolveAfter() - level.getGameTime()
        );

        if (CapitalCampaignDataAccess.getCampaignForCapital(
                level,
                capital.getCapitalId()
        ) != null) {
            return "Interregnum — awaiting the end of the campaign";
        }

        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 19L) / 20L);
            return "Interregnum — succession in "
                    + seconds
                    + (seconds == 1L ? " second" : " seconds");
        }

        return "Interregnum — succession pending";
    }

    public static boolean clear(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalInterregnumDataAccess.remove(level, capitalId);
    }

    public static boolean beginDepositionInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            String reason
    ) {
        return beginDepositionInterregnum(
                level,
                capital,
                reason,
                null
        );
    }

    public static boolean beginDepositionInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            String reason,
            UUID victoriousClaimantId
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || capital.getSovereign() == null) {
            return false;
        }

        CapitalInterregnumRecord existing =
                CapitalInterregnumDataAccess.getRecord(
                        level,
                        capital.getCapitalId()
                );
        if (existing != null) {
            return existing.wasDeposition();
        }

        UUID deposedSovereignId = capital.getSovereign();
        String deposedName = resolveName(level, deposedSovereignId);
        boolean deposedFemale = capital.isSovereignFemale();
        long now = level.getGameTime();

        CapitalInterregnumRecord record =
                new CapitalInterregnumRecord(
                        capital.getCapitalId(),
                        deposedSovereignId,
                        deposedName,
                        now,
                        now + MINIMUM_INTERREGNUM_TICKS,
                        deposedFemale,
                        capital.isPlayerSovereign(),
                        capital.getPlayerSovereignId(),
                        true,
                        victoriousClaimantId
                );

        if (!CapitalInterregnumDataAccess.begin(level, record)) {
            return false;
        }

        capital.setSovereign(null);
        capital.setSovereignFemale(false);

        if (record.wasPlayerSovereign()) {
            CapitalSovereignAppointmentService.clearPlayerSovereignState(
                    capital
            );
            if (record.getFormerPlayerSovereignId() != null) {
                PlayerCapitalTitleService.clear(
                        level,
                        record.getFormerPlayerSovereignId(),
                        capital.getCapitalId()
                );
            }
        }

        stripDeposedSovereign(level, capital, deposedSovereignId);
        ensureDepositionHand(level, capital);

        String entry = deposedName
                + " was removed from the throne"
                + (reason == null || reason.isBlank()
                ? "."
                : " " + reason.trim())
                + " A wartime interregnum began.";

        CapitalChronicleService.addEntry(level, capital, entry);
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.literal(entry)
        );
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static void beginInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            UUID deceasedSovereignId
    ) {
        String deceasedName = resolveName(level, deceasedSovereignId);
        long now = level.getGameTime();

        CapitalInterregnumRecord record =
                new CapitalInterregnumRecord(
                        capital.getCapitalId(),
                        deceasedSovereignId,
                        deceasedName,
                        now,
                        now + MINIMUM_INTERREGNUM_TICKS,
                        capital.isSovereignFemale(),
                        capital.isPlayerSovereign(),
                        capital.getPlayerSovereignId(),
                        false,
                        null
                );

        if (!CapitalInterregnumDataAccess.begin(level, record)) {
            return;
        }

        CapitalMourningService.startMourning(
                level,
                capital,
                deceasedName + " died."
        );

        capital.setSovereign(null);
        capital.setSovereignFemale(false);

        if (record.wasPlayerSovereign()) {
            CapitalSovereignAppointmentService.clearPlayerSovereignState(
                    capital
            );
            if (record.getFormerPlayerSovereignId() != null) {
                PlayerCapitalTitleService.clear(
                        level,
                        record.getFormerPlayerSovereignId(),
                        capital.getCapitalId()
                );
            }
        }

        endCampaignForSovereignDeath(
                level,
                capital,
                deceasedName
        );

        String capitalName = CapitalDiplomaticAgreementText.capitalName(
                level,
                capital
        );
        CapitalChronicleService.addEntry(
                level,
                capital,
                deceasedName
                        + " died while "
                        + capitalName
                        + " was at war or committed to a campaign. A wartime interregnum began."
        );
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.literal(
                        deceasedName
                                + " has died. The capital has entered a wartime interregnum."
                )
        );
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static boolean tickInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord record
    ) {
        if (record == null || capital == null) {
            return false;
        }

        if (record.wasDeposition()) {
            stripDeposedSovereign(
                    level,
                    capital,
                    record.getDeceasedSovereignId()
            );
            ensureDepositionHand(level, capital);
        }

        UUID currentSovereign = capital.getSovereign();
        if (currentSovereign != null
                && !currentSovereign.equals(
                record.getDeceasedSovereignId()
        )
                && isValidLivingSovereign(level, currentSovereign)) {
            CapitalInterregnumDataAccess.remove(
                    level,
                    capital.getCapitalId()
            );
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The wartime interregnum ended when "
                            + resolveName(level, currentSovereign)
                            + " assumed the throne."
            );
            return true;
        }

        if (!record.wasDeposition()
                && currentSovereign != null
                && currentSovereign.equals(
                record.getDeceasedSovereignId()
        )
                && isValidLivingSovereign(level, currentSovereign)) {
            CapitalInterregnumDataAccess.remove(
                    level,
                    capital.getCapitalId()
            );
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The wartime interregnum ended when the sovereign returned alive."
            );
            return true;
        }

        if (level.getGameTime() < record.getResolveAfter()) {
            return true;
        }

        if (CapitalCampaignDataAccess.getCampaignForCapital(
                level,
                capital.getCapitalId()
        ) != null) {
            return true;
        }

        boolean resolved = CapitalInterregnumSuccessionResolver.resolve(
                level,
                capital,
                record
        );
        if (resolved) {
            CapitalInterregnumDataAccess.remove(
                    level,
                    capital.getCapitalId()
            );
        }
        return true;
    }

    public static boolean ensureDepositionHand(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return false;
        }

        CapitalInterregnumRecord record =
                CapitalInterregnumDataAccess.getRecord(
                        level,
                        capital.getCapitalId()
                );
        if (record == null || !record.wasDeposition()) {
            return false;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(
                level,
                capital.getCapitalId()
        );
        UUID currentHand = capital.getHand();
        if (currentHand != null
                && !currentHand.equals(
                record.getDeceasedSovereignId()
        )
                && CapitalHandSelection.isValidHand(
                level,
                capital,
                currentHand,
                residents
        )) {
            return false;
        }

        boolean changed = false;
        if (currentHand != null) {
            capital.setHand(null);
            capital.setHandFemale(false);
            changed = true;
        }

        UUID replacement = findDepositionHand(
                level,
                capital,
                record,
                residents
        );
        if (replacement == null) {
            if (changed) {
                CapitalCourtWatcher.clearFingerprint(
                        capital.getCapitalId()
                );
                CapitalDataAccess.markDirty(level);
            }
            return changed;
        }

        if (replacement.equals(capital.getCommander())) {
            capital.setCommander(null);
            capital.setCommanderFemale(false);
        }

        capital.setHand(replacement);
        capital.setHandFemale(
                MCAIntegrationBridge.isFemale(level, replacement)
        );
        CapitalChronicleService.addEntry(
                level,
                capital,
                resolveName(level, replacement)
                        + " was appointed Hand of the Crown during the wartime interregnum."
        );
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static UUID findDepositionHand(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord record,
            Set<UUID> residents
    ) {
        LinkedHashSet<UUID> ordered = new LinkedHashSet<>();
        if (capital.getCommander() != null) {
            ordered.add(capital.getCommander());
        }
        ordered.addAll(capital.getDukes());
        ordered.addAll(capital.getLords());
        ordered.addAll(capital.getKnights());
        residents.stream()
                .filter(id -> id != null)
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(ordered::add);

        for (UUID candidate : ordered) {
            if (isValidDepositionHandCandidate(
                    level,
                    capital,
                    record,
                    residents,
                    candidate,
                    true
            )) {
                return candidate;
            }
        }
        for (UUID candidate : ordered) {
            if (isValidDepositionHandCandidate(
                    level,
                    capital,
                    record,
                    residents,
                    candidate,
                    false
            )) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isValidDepositionHandCandidate(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord record,
            Set<UUID> residents,
            UUID candidate,
            boolean requireTrustedStanding
    ) {
        if (candidate == null
                || candidate.equals(record.getDeceasedSovereignId())
                || !residents.contains(candidate)
                || !MCAIntegrationBridge.isMCAVillager(level, candidate)
                || !MCAIntegrationBridge.isTeenOrAdultVillager(
                level,
                candidate
        )
                || !MCAIntegrationBridge.isLoadedAndAlive(
                level,
                candidate
        )
                || CapitalAmbassadorService.isAmbassador(
                level,
                capital,
                candidate
        )
                || candidate.equals(capital.getConsort())
                || candidate.equals(capital.getDowager())
                || candidate.equals(capital.getHeir())
                || candidate.equals(capital.getGrandMaester())
                || candidate.equals(capital.getHerald())
                || candidate.equals(capital.getMasterOfLaws())
                || capital.isRoyalGuard(candidate)) {
            return false;
        }

        if (requireTrustedStanding
                && !CapitalCrownJusticeService.isTrustedOfficeEligible(
                level,
                capital,
                candidate
        )) {
            return false;
        }

        return candidate.equals(capital.getCommander())
                || capital.isDuke(candidate)
                || capital.isLord(candidate)
                || capital.isKnight(candidate)
                || !requireTrustedStanding;
    }

    private static CapitalInterregnumRecord normalizeInterregnumRecord(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord record
    ) {
        if (level == null
                || capital == null
                || record == null
                || record.wasDeposition()) {
            return record;
        }

        Entity former = MCAIntegrationBridge.findLoadedEntityByUuid(
                level,
                record.getDeceasedSovereignId()
        );
        if (former == null
                || !former.isAlive()
                || former.isRemoved()
                || capital.getSovereign() != null) {
            return record;
        }

        CapitalInterregnumRecord normalized =
                new CapitalInterregnumRecord(
                        record.getCapitalId(),
                        record.getDeceasedSovereignId(),
                        record.getDeceasedSovereignName(),
                        record.getStartedAt(),
                        record.getResolveAfter(),
                        record.wasDeceasedSovereignFemale(),
                        record.wasPlayerSovereign(),
                        record.getFormerPlayerSovereignId(),
                        true,
                        record.getVictoriousClaimantId()
                );
        CapitalInterregnumDataAccess.remove(level, record.getCapitalId());
        CapitalInterregnumDataAccess.begin(level, normalized);
        return normalized;
    }

    private static void stripDeposedSovereign(
            ServerLevel level,
            CapitalRecord capital,
            UUID deposedSovereignId
    ) {
        if (level == null
                || capital == null
                || deposedSovereignId == null) {
            return;
        }

        capital.addDisinheritedRoyalChild(deposedSovereignId);
        capital.removeRoyalChild(deposedSovereignId);
        capital.removeLegitimizedRoyalChild(deposedSovereignId);
        capital.removeDuke(deposedSovereignId);
        capital.removeLord(deposedSovereignId);
        capital.removeKnight(deposedSovereignId);
        capital.removeRoyalGuard(deposedSovereignId);

        if (deposedSovereignId.equals(capital.getHeir())) {
            capital.setHeir(null);
            capital.setHeirFemale(false);
            capital.setHeirMode(CapitalRecord.HeirMode.NONE);
        }
        if (deposedSovereignId.equals(capital.getConsort())) {
            capital.setConsort(null);
            capital.setConsortFemale(false);
        }
        if (deposedSovereignId.equals(capital.getDowager())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }
        if (deposedSovereignId.equals(capital.getCommander())) {
            capital.setCommander(null);
            capital.setCommanderFemale(false);
        }
        if (deposedSovereignId.equals(capital.getHand())) {
            capital.setHand(null);
            capital.setHandFemale(false);
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(
                level,
                deposedSovereignId
        );
        if (entity != null) {
            String baseName = CapitalNameService.resolveDisplayName(
                    level,
                    capital,
                    deposedSovereignId
            );
            entity.setCustomName(Component.literal(baseName));
            entity.setCustomNameVisible(true);
        }
    }

    private static boolean isSurvivalPlayerSovereignDeath(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return capital.isPlayerSovereign()
                && level.getServer() != null
                && !level.getServer().isHardcore();
    }

    private static boolean isValidLivingSovereign(
            ServerLevel level,
            UUID entityId
    ) {
        if (entityId == null) {
            return false;
        }
        Entity entity = MCAIntegrationBridge.getEntityByUuid(
                level,
                entityId
        );
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }
        return MCAIntegrationBridge.hasPersistentFamilyNode(
                level,
                entityId
        )
                && !MCAIntegrationBridge.isFamilyNodeDeceased(
                level,
                entityId
        );
    }

    private static boolean isMilitaryCrisis(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (CapitalCampaignDataAccess.getCampaignForCapital(
                level,
                capital.getCapitalId()
        ) != null) {
            return true;
        }

        for (Map.Entry<CapitalRelationKey, CapitalRelationRecord> entry :
                CapitalDiplomacyDataAccess.getRelationshipsSnapshot(
                        level
                ).entrySet()) {
            CapitalRelationKey key = entry.getKey();
            CapitalRelationRecord relation = entry.getValue();
            if (key != null
                    && relation != null
                    && relation.getDiplomaticState()
                    == CapitalDiplomaticState.WAR
                    && (capital.getCapitalId().equals(key.first())
                    || capital.getCapitalId().equals(key.second()))) {
                return true;
            }
        }
        return false;
    }

    private static void endCampaignForSovereignDeath(
            ServerLevel level,
            CapitalRecord capital,
            String deceasedName
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess.getCampaignForCapital(
                        level,
                        capital.getCapitalId()
                );
        if (campaign == null) {
            return;
        }

        CapitalRecord attackingCapital = CapitalManager.getCapital(
                campaign.getAttackingCapitalId()
        );
        CapitalRecord defendingCapital = CapitalManager.getCapital(
                campaign.getDefendingCapitalId()
        );

        if (campaign.getPhase() == CapitalCampaignPhase.MUSTERING) {
            CapitalCampaignTargetingService.clearCampaignTargets(
                    level,
                    campaign
            );
            CapitalCampaignService.completeCampaign(
                    level,
                    campaign.getCampaignId()
            );
            addCampaignEntry(
                    level,
                    attackingCapital,
                    defendingCapital,
                    "The planned attack was abandoned after "
                            + deceasedName
                            + " died."
            );
            return;
        }

        if (campaign.getPhase() == CapitalCampaignPhase.ACTIVE) {
            CapitalCampaignEndReason reason =
                    capital.getCapitalId().equals(
                            campaign.getAttackingCapitalId()
                    )
                            ? CapitalCampaignEndReason
                            .ATTACKING_SOVEREIGN_DIED
                            : CapitalCampaignEndReason
                            .DEFENDING_SOVEREIGN_DIED;
            CapitalCampaignService.beginRetreat(
                    level,
                    campaign.getCampaignId(),
                    reason
            );
            CapitalCampaignTargetingService.clearCampaignTargets(
                    level,
                    campaign
            );
            addCampaignEntry(
                    level,
                    attackingCapital,
                    defendingCapital,
                    "The campaign ended after "
                            + deceasedName
                            + " died; the surviving attackers began retreating."
            );
        }
    }

    private static void addCampaignEntry(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            String entry
    ) {
        if (attackingCapital != null) {
            CapitalChronicleService.addEntry(
                    level,
                    attackingCapital,
                    entry
            );
        }
        if (defendingCapital != null
                && defendingCapital != attackingCapital) {
            CapitalChronicleService.addEntry(
                    level,
                    defendingCapital,
                    entry
            );
        }
    }

    private static String resolveName(
            ServerLevel level,
            UUID entityId
    ) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(
                level,
                entityId
        );
        return entity == null
                ? entityId.toString()
                : entity.getName().getString();
    }
}
