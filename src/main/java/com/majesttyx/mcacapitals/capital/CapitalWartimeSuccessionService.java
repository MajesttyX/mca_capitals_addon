package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
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

import java.util.Map;
import java.util.UUID;

public final class CapitalWartimeSuccessionService {

    public static final long MINIMUM_INTERREGNUM_TICKS =
            1200L;

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
            return tickInterregnum(
                    level,
                    capital,
                    existing
            );
        }

        UUID sovereignId = capital.getSovereign();

        if (sovereignId == null
                || isSurvivalPlayerSovereignDeath(
                level,
                capital
        )
                || isValidLivingSovereign(
                level,
                sovereignId
        )
                || !isMilitaryCrisis(
                level,
                capital
        )) {
            return false;
        }

        beginInterregnum(
                level,
                capital,
                sovereignId
        );

        return true;
    }

    public static boolean isInInterregnum(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalInterregnumDataAccess
                .getRecord(
                        level,
                        capitalId
                ) != null;
    }

    public static CapitalInterregnumRecord getRecord(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalInterregnumDataAccess.getRecord(
                level,
                capitalId
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

        CapitalInterregnumRecord record =
                getRecord(
                        level,
                        capital.getCapitalId()
                );

        if (record == null) {
            return "";
        }

        long remaining = Math.max(
                0L,
                record.getResolveAfter()
                        - level.getGameTime()
        );

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        capital.getCapitalId()
                ) != null) {
            return "Interregnum — awaiting the end of the campaign";
        }

        if (remaining > 0L) {
            long seconds = Math.max(
                    1L,
                    (remaining + 19L) / 20L
            );

            return "Interregnum — succession in "
                    + seconds
                    + (seconds == 1L
                    ? " second"
                    : " seconds");
        }

        return "Interregnum — succession pending";
    }

    public static boolean clear(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalInterregnumDataAccess.remove(
                level,
                capitalId
        );
    }

    public static boolean beginDepositionInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            String reason
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || capital.getSovereign() == null) {
            return false;
        }

        UUID deposedSovereignId = capital.getSovereign();
        String deposedName = resolveName(level, deposedSovereignId);
        long now = level.getGameTime();
        CapitalInterregnumRecord record = new CapitalInterregnumRecord(
                capital.getCapitalId(),
                deposedSovereignId,
                deposedName,
                now,
                now + MINIMUM_INTERREGNUM_TICKS,
                capital.isSovereignFemale(),
                capital.isPlayerSovereign(),
                capital.getPlayerSovereignId()
        );

        if (!CapitalInterregnumDataAccess.begin(level, record)) {
            return false;
        }

        capital.setSovereign(null);
        capital.setSovereignFemale(false);

        if (record.wasPlayerSovereign()) {
            CapitalSovereignAppointmentService
                    .clearPlayerSovereignState(capital);
            if (record.getFormerPlayerSovereignId() != null) {
                PlayerCapitalTitleService.clear(
                        level,
                        record.getFormerPlayerSovereignId(),
                        capital.getCapitalId()
                );
            }
        }

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
        String deceasedName = resolveName(
                level,
                deceasedSovereignId
        );

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
                        capital.getPlayerSovereignId()
                );

        if (!CapitalInterregnumDataAccess.begin(
                level,
                record
        )) {
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
            CapitalSovereignAppointmentService
                    .clearPlayerSovereignState(capital);

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

        String capitalName =
                CapitalDiplomaticAgreementText.capitalName(
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

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        capital,
                        Component.literal(
                                deceasedName
                                        + " has died. The capital has entered a wartime interregnum."
                        )
                );

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        CapitalDataAccess.markDirty(level);
    }

    private static boolean tickInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord record
    ) {
        UUID currentSovereign = capital.getSovereign();

        if (currentSovereign != null
                && !currentSovereign.equals(
                record.getDeceasedSovereignId()
        )
                && isValidLivingSovereign(
                level,
                currentSovereign
        )) {
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

        if (currentSovereign != null
                && currentSovereign.equals(
                record.getDeceasedSovereignId()
        )
                && isValidLivingSovereign(
                level,
                currentSovereign
        )) {
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

        if (level.getGameTime()
                < record.getResolveAfter()) {
            return true;
        }

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        capital.getCapitalId()
                ) != null) {
            return true;
        }

        boolean resolved =
                CapitalInterregnumSuccessionResolver.resolve(
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

        Entity entity = MCAIntegrationBridge
                .getEntityByUuid(
                        level,
                        entityId
                );

        if (entity != null) {
            return entity.isAlive()
                    && !entity.isRemoved();
        }

        return MCAIntegrationBridge
                .hasPersistentFamilyNode(
                        level,
                        entityId
                )
                && !MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        entityId
                );
    }

    private static boolean isMilitaryCrisis(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        capital.getCapitalId()
                ) != null) {
            return true;
        }

        for (Map.Entry<
                CapitalRelationKey,
                CapitalRelationRecord
                > entry :
                CapitalDiplomacyDataAccess
                        .get(level)
                        .getRelationshipsSnapshot()
                        .entrySet()) {
            CapitalRelationKey key = entry.getKey();
            CapitalRelationRecord relation = entry.getValue();

            if (key == null
                    || relation == null
                    || relation.getDiplomaticState()
                    != CapitalDiplomaticState.WAR) {
                continue;
            }

            if (capital.getCapitalId().equals(key.first())
                    || capital.getCapitalId().equals(key.second())) {
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
                CapitalCampaignDataAccess
                        .getCampaignForCapital(
                                level,
                                capital.getCapitalId()
                        );

        if (campaign == null) {
            return;
        }

        CapitalRecord attackingCapital =
                CapitalManager.getCapital(
                        campaign.getAttackingCapitalId()
                );

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                );

        if (campaign.getPhase()
                == CapitalCampaignPhase.MUSTERING) {
            CapitalCampaignTargetingService
                    .clearCampaignTargets(
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

        if (campaign.getPhase()
                == CapitalCampaignPhase.ACTIVE) {
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

            CapitalCampaignTargetingService
                    .clearCampaignTargets(
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
        Entity entity = MCAIntegrationBridge
                .getEntityByUuid(
                        level,
                        entityId
                );

        return entity == null
                ? entityId.toString()
                : entity.getName().getString();
    }
}