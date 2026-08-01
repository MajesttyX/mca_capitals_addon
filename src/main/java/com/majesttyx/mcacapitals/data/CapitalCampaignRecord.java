package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalCampaignRecord {

    public static final int MAX_ATTACKERS = 7;
    public static final int PREFERRED_ATTACKERS = 5;

    private static final String KEY_CAMPAIGN_ID = "CampaignId";
    private static final String KEY_ATTACKING_CAPITAL_ID = "AttackingCapitalId";
    private static final String KEY_DEFENDING_CAPITAL_ID = "DefendingCapitalId";
    private static final String KEY_INITIATING_PLAYER_ID = "InitiatingPlayerId";
    private static final String KEY_ATTACKERS = "Attackers";
    private static final String KEY_DEFENDERS = "Defenders";
    private static final String KEY_RETURNED_ATTACKERS = "ReturnedAttackers";
    private static final String KEY_COMBATANT_ID = "CombatantId";
    private static final String KEY_PHASE = "Phase";
    private static final String KEY_CREATED_AT = "CreatedAt";
    private static final String KEY_ACTIVATED_AT = "ActivatedAt";
    private static final String KEY_RETREAT_STARTED_AT = "RetreatStartedAt";
    private static final String KEY_RETURN_DEADLINE = "ReturnDeadline";
    private static final String KEY_END_REASON = "EndReason";
    private static final String KEY_DEFENDING_SOVEREIGN_REFUSED_PEACE =
            "DefendingSovereignRefusedPeace";
    private static final String KEY_TARGET_ATTACKER_COUNT =
            "TargetAttackerCount";
    private static final String KEY_ASSEMBLY_STARTED_AT =
            "AssemblyStartedAt";
    private static final String KEY_FORMATION_ENDS_AT =
            "FormationEndsAt";
    private static final String KEY_LAST_ASSEMBLY_REPORTED_COUNT =
            "LastAssemblyReportedCount";
    private static final String KEY_FIELD_DEFEAT_RESOLUTION_AT =
            "FieldDefeatResolutionAt";
    private static final String KEY_CROWN_RALLY_ENDS_AT =
            "CrownRallyEndsAt";
    private static final String KEY_WAR_CAUSE = "WarCause";
    private static final String KEY_WAR_GOAL = "WarGoal";

    private final UUID campaignId;
    private final UUID attackingCapitalId;
    private final UUID defendingCapitalId;
    private final UUID initiatingPlayerId;
    private final List<UUID> attackerIds;
    private final List<UUID> defenderIds;
    private final List<UUID> returnedAttackerIds;
    private final long createdAt;

    private CapitalCampaignPhase phase;
    private long activatedAt;
    private long retreatStartedAt;
    private long returnDeadline;
    private CapitalCampaignEndReason endReason;
    private boolean defendingSovereignRefusedPeace;
    private int targetAttackerCount;
    private long assemblyStartedAt;
    private long formationEndsAt;
    private int lastAssemblyReportedCount;
    private long fieldDefeatResolutionAt;
    private long crownRallyEndsAt;
    private final CapitalWarCause warCause;
    private final CapitalWarGoal warGoal;

    public CapitalCampaignRecord(
            UUID campaignId,
            UUID attackingCapitalId,
            UUID defendingCapitalId,
            List<UUID> attackerIds,
            long createdAt
    ) {
        this(
                campaignId,
                attackingCapitalId,
                defendingCapitalId,
                null,
                attackerIds,
                createdAt
        );
    }

    public CapitalCampaignRecord(
            UUID campaignId,
            UUID attackingCapitalId,
            UUID defendingCapitalId,
            UUID initiatingPlayerId,
            List<UUID> attackerIds,
            long createdAt
    ) {
        this(
                campaignId,
                attackingCapitalId,
                defendingCapitalId,
                initiatingPlayerId,
                attackerIds,
                List.of(),
                List.of(),
                CapitalCampaignPhase.MUSTERING,
                createdAt,
                0L,
                0L,
                0L,
                CapitalCampaignEndReason.NONE,
                false,
                preferredTarget(
                        attackerIds == null
                                ? 0
                                : attackerIds.size()
                ),
                0L,
                0L,
                -1,
                0L,
                0L,
                CapitalWarCause.UNJUST,
                CapitalWarGoal.PUNITIVE
        );
    }

    public CapitalCampaignRecord(
            UUID campaignId,
            UUID attackingCapitalId,
            UUID defendingCapitalId,
            UUID initiatingPlayerId,
            List<UUID> attackerIds,
            long createdAt,
            CapitalWarCause warCause,
            CapitalWarGoal warGoal
    ) {
        this(
                campaignId,
                attackingCapitalId,
                defendingCapitalId,
                initiatingPlayerId,
                attackerIds,
                List.of(),
                List.of(),
                CapitalCampaignPhase.MUSTERING,
                createdAt,
                0L,
                0L,
                0L,
                CapitalCampaignEndReason.NONE,
                false,
                preferredTarget(
                        attackerIds == null ? 0 : attackerIds.size()
                ),
                0L,
                0L,
                -1,
                0L,
                0L,
                warCause,
                warGoal
        );
    }

    public CapitalCampaignRecord(
            UUID campaignId,
            UUID attackingCapitalId,
            UUID defendingCapitalId,
            UUID initiatingPlayerId,
            List<UUID> attackerIds,
            List<UUID> defenderIds,
            List<UUID> returnedAttackerIds,
            CapitalCampaignPhase phase,
            long createdAt,
            long activatedAt,
            long retreatStartedAt,
            long returnDeadline,
            CapitalCampaignEndReason endReason,
            boolean defendingSovereignRefusedPeace
    ) {
        this(
                campaignId,
                attackingCapitalId,
                defendingCapitalId,
                initiatingPlayerId,
                attackerIds,
                defenderIds,
                returnedAttackerIds,
                phase,
                createdAt,
                activatedAt,
                retreatStartedAt,
                returnDeadline,
                endReason,
                defendingSovereignRefusedPeace,
                preferredTarget(
                        attackerIds == null
                                ? 0
                                : attackerIds.size()
                ),
                0L,
                0L,
                -1,
                0L,
                0L,
                CapitalWarCause.UNJUST,
                CapitalWarGoal.PUNITIVE
        );
    }

    public CapitalCampaignRecord(
            UUID campaignId,
            UUID attackingCapitalId,
            UUID defendingCapitalId,
            UUID initiatingPlayerId,
            List<UUID> attackerIds,
            List<UUID> defenderIds,
            List<UUID> returnedAttackerIds,
            CapitalCampaignPhase phase,
            long createdAt,
            long activatedAt,
            long retreatStartedAt,
            long returnDeadline,
            CapitalCampaignEndReason endReason,
            boolean defendingSovereignRefusedPeace,
            int targetAttackerCount,
            long assemblyStartedAt,
            long formationEndsAt,
            int lastAssemblyReportedCount,
            long fieldDefeatResolutionAt,
            long crownRallyEndsAt,
            CapitalWarCause warCause,
            CapitalWarGoal warGoal
    ) {
        if (campaignId == null
                || attackingCapitalId == null
                || defendingCapitalId == null) {
            throw new IllegalArgumentException(
                    "Campaign and capital IDs cannot be null."
            );
        }

        if (attackingCapitalId.equals(defendingCapitalId)) {
            throw new IllegalArgumentException(
                    "A capital cannot campaign against itself."
            );
        }

        List<UUID> normalizedAttackers =
                normalizeAttackers(attackerIds);

        if (normalizedAttackers.isEmpty()) {
            throw new IllegalArgumentException(
                    "A campaign requires at least one attacker."
            );
        }

        this.campaignId = campaignId;
        this.attackingCapitalId = attackingCapitalId;
        this.defendingCapitalId = defendingCapitalId;
        this.initiatingPlayerId = initiatingPlayerId;
        this.attackerIds =
                new ArrayList<>(normalizedAttackers);
        this.defenderIds =
                new ArrayList<>(
                        normalizeCombatants(
                                defenderIds,
                                Integer.MAX_VALUE
                        )
                );
        this.returnedAttackerIds =
                new ArrayList<>(
                        normalizeCombatants(
                                returnedAttackerIds,
                                MAX_ATTACKERS
                        )
                );
        this.phase =
                phase == null
                        ? CapitalCampaignPhase.MUSTERING
                        : phase;
        this.createdAt = Math.max(0L, createdAt);
        this.activatedAt = Math.max(0L, activatedAt);
        this.retreatStartedAt =
                Math.max(0L, retreatStartedAt);
        this.returnDeadline =
                Math.max(0L, returnDeadline);
        this.endReason =
                endReason == null
                        ? CapitalCampaignEndReason.NONE
                        : endReason;
        this.defendingSovereignRefusedPeace =
                defendingSovereignRefusedPeace;
        this.targetAttackerCount =
                normalizeTargetAttackerCount(
                        targetAttackerCount,
                        normalizedAttackers.size()
                );
        this.assemblyStartedAt =
                Math.max(0L, assemblyStartedAt);
        this.formationEndsAt =
                Math.max(0L, formationEndsAt);
        this.lastAssemblyReportedCount =
                Math.max(
                        -1,
                        lastAssemblyReportedCount
                );
        this.fieldDefeatResolutionAt =
                Math.max(
                        0L,
                        fieldDefeatResolutionAt
                );
        this.crownRallyEndsAt =
                Math.max(0L, crownRallyEndsAt);
        this.warCause = warCause == null
                ? CapitalWarCause.UNJUST
                : warCause;
        this.warGoal = warGoal == null
                ? CapitalWarGoal.PUNITIVE
                : warGoal;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getAttackingCapitalId() {
        return attackingCapitalId;
    }

    public UUID getDefendingCapitalId() {
        return defendingCapitalId;
    }

    public UUID getInitiatingPlayerId() {
        return initiatingPlayerId;
    }

    public List<UUID> getAttackerIds() {
        return List.copyOf(attackerIds);
    }

    public List<UUID> getDefenderIds() {
        return List.copyOf(defenderIds);
    }

    public List<UUID> getReturnedAttackerIds() {
        return List.copyOf(returnedAttackerIds);
    }

    public CapitalCampaignPhase getPhase() {
        return phase;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getActivatedAt() {
        return activatedAt;
    }

    public long getRetreatStartedAt() {
        return retreatStartedAt;
    }

    public long getReturnDeadline() {
        return returnDeadline;
    }

    public CapitalCampaignEndReason getEndReason() {
        return endReason;
    }

    public boolean didDefendingSovereignRefusePeace() {
        return defendingSovereignRefusedPeace;
    }

    public int getTargetAttackerCount() {
        return targetAttackerCount;
    }

    public long getAssemblyStartedAt() {
        return assemblyStartedAt;
    }

    public long getFormationEndsAt() {
        return formationEndsAt;
    }

    public int getLastAssemblyReportedCount() {
        return lastAssemblyReportedCount;
    }

    public long getFieldDefeatResolutionAt() {
        return fieldDefeatResolutionAt;
    }

    public long getCrownRallyEndsAt() {
        return crownRallyEndsAt;
    }

    public CapitalWarCause getWarCause() {
        return warCause;
    }

    public CapitalWarGoal getWarGoal() {
        return warGoal;
    }

    public boolean hasAssemblyStarted() {
        return phase
                == CapitalCampaignPhase.MUSTERING
                && assemblyStartedAt > 0L;
    }

    public boolean isFormationPending() {
        return phase
                == CapitalCampaignPhase.MUSTERING
                && formationEndsAt > 0L;
    }

    public boolean isFieldDefeatResolutionPending() {
        return phase
                == CapitalCampaignPhase.ACTIVE
                && fieldDefeatResolutionAt > 0L;
    }

    public boolean isCrownRallyPending() {
        return phase
                == CapitalCampaignPhase.ACTIVE
                && crownRallyEndsAt > 0L;
    }

    public boolean containsCapital(
            UUID capitalId
    ) {
        return capitalId != null
                && (
                capitalId.equals(attackingCapitalId)
                        || capitalId.equals(
                        defendingCapitalId
                )
        );
    }

    public boolean containsAttacker(
            UUID villagerId
    ) {
        return villagerId != null
                && attackerIds.contains(villagerId);
    }

    public boolean containsDefender(
            UUID villagerId
    ) {
        return villagerId != null
                && defenderIds.contains(villagerId);
    }

    public boolean hasAttackerReturned(
            UUID villagerId
    ) {
        return villagerId != null
                && returnedAttackerIds.contains(
                villagerId
        );
    }

    public boolean isActiveCampaign() {
        return phase
                == CapitalCampaignPhase.MUSTERING
                || phase
                == CapitalCampaignPhase.ACTIVE
                || phase
                == CapitalCampaignPhase.RETREATING;
    }

    public void replaceAttackerIds(
            List<UUID> attackerIds
    ) {
        List<UUID> normalized =
                normalizeAttackers(attackerIds);

        if (normalized.isEmpty()) {
            return;
        }

        this.attackerIds.clear();
        this.attackerIds.addAll(normalized);
        this.returnedAttackerIds.retainAll(
                normalized
        );
    }

    public void setDefenderIds(
            List<UUID> defenderIds
    ) {
        this.defenderIds.clear();
        this.defenderIds.addAll(
                normalizeCombatants(
                        defenderIds,
                        Integer.MAX_VALUE
                )
        );
    }

    public void raiseTargetAttackerCount(
            int targetAttackerCount
    ) {
        this.targetAttackerCount =
                Math.max(
                        this.targetAttackerCount,
                        normalizeTargetAttackerCount(
                                targetAttackerCount,
                                attackerIds.size()
                        )
                );
    }

    public void markAssemblyReportedCount(
            int count
    ) {
        lastAssemblyReportedCount =
                Math.max(0, count);
    }

    public void beginAssembly(
            long gameTime
    ) {
        if (phase
                != CapitalCampaignPhase.MUSTERING) {
            return;
        }

        assemblyStartedAt =
                Math.max(1L, gameTime);
        formationEndsAt = 0L;
        lastAssemblyReportedCount = -1;
        defenderIds.clear();
        clearBattlePacing();
    }

    public void resetAssembly() {
        if (phase
                != CapitalCampaignPhase.MUSTERING
                || formationEndsAt > 0L) {
            return;
        }

        assemblyStartedAt = 0L;
        lastAssemblyReportedCount = -1;
        defenderIds.clear();
        clearBattlePacing();
    }

    public void beginFormation(
            long gameTime,
            long formationEndsAt
    ) {
        if (phase
                != CapitalCampaignPhase.MUSTERING) {
            return;
        }

        assemblyStartedAt =
                Math.max(1L, gameTime);
        this.formationEndsAt =
                Math.max(
                        assemblyStartedAt,
                        formationEndsAt
                );
        clearBattlePacing();
    }

    public void beginFieldDefeatResolution(
            long gameTime,
            long resolutionAt
    ) {
        if (phase
                != CapitalCampaignPhase.ACTIVE
                || defendingSovereignRefusedPeace) {
            return;
        }

        fieldDefeatResolutionAt =
                Math.max(
                        Math.max(1L, gameTime),
                        resolutionAt
                );
        crownRallyEndsAt = 0L;
    }

    public void clearFieldDefeatResolution() {
        fieldDefeatResolutionAt = 0L;
    }

    public void beginCrownRally(
            long gameTime,
            long rallyEndsAt
    ) {
        if (phase
                != CapitalCampaignPhase.ACTIVE) {
            return;
        }

        fieldDefeatResolutionAt = 0L;
        crownRallyEndsAt =
                Math.max(
                        Math.max(1L, gameTime),
                        rallyEndsAt
                );
    }

    public void finishCrownRally() {
        crownRallyEndsAt = 0L;
    }

    public void markAttackerReturned(
            UUID attackerId
    ) {
        if (attackerId != null
                && attackerIds.contains(attackerId)
                && !returnedAttackerIds.contains(
                attackerId
        )) {
            returnedAttackerIds.add(attackerId);
        }
    }

    public void markDefendingSovereignRefusedPeace() {
        defendingSovereignRefusedPeace = true;
        fieldDefeatResolutionAt = 0L;
    }

    public void activate(
            long gameTime
    ) {
        phase = CapitalCampaignPhase.ACTIVE;
        activatedAt =
                Math.max(0L, gameTime);
        retreatStartedAt = 0L;
        returnDeadline = 0L;
        endReason =
                CapitalCampaignEndReason.NONE;
        assemblyStartedAt = 0L;
        formationEndsAt = 0L;
        lastAssemblyReportedCount = -1;
        clearBattlePacing();
    }

    public void finishWithoutRetreat(CapitalCampaignEndReason reason) {
        endReason = reason == null
                ? CapitalCampaignEndReason.INVALIDATED
                : reason;
    }

    public void beginRetreat(
            long gameTime,
            long returnDeadline,
            CapitalCampaignEndReason endReason
    ) {
        phase =
                CapitalCampaignPhase.RETREATING;
        retreatStartedAt =
                Math.max(0L, gameTime);
        this.returnDeadline =
                Math.max(
                        retreatStartedAt,
                        returnDeadline
                );
        this.endReason =
                endReason == null
                        ? CapitalCampaignEndReason
                        .INVALIDATED
                        : endReason;
        assemblyStartedAt = 0L;
        formationEndsAt = 0L;
        lastAssemblyReportedCount = -1;
        clearBattlePacing();
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putUUID(
                KEY_CAMPAIGN_ID,
                campaignId
        );
        tag.putUUID(
                KEY_ATTACKING_CAPITAL_ID,
                attackingCapitalId
        );
        tag.putUUID(
                KEY_DEFENDING_CAPITAL_ID,
                defendingCapitalId
        );

        if (initiatingPlayerId != null) {
            tag.putUUID(
                    KEY_INITIATING_PLAYER_ID,
                    initiatingPlayerId
            );
        }

        tag.putString(
                KEY_PHASE,
                phase.getSerializedName()
        );
        tag.putLong(
                KEY_CREATED_AT,
                createdAt
        );
        tag.putLong(
                KEY_ACTIVATED_AT,
                activatedAt
        );
        tag.putLong(
                KEY_RETREAT_STARTED_AT,
                retreatStartedAt
        );
        tag.putLong(
                KEY_RETURN_DEADLINE,
                returnDeadline
        );
        tag.putString(
                KEY_END_REASON,
                endReason.getSerializedName()
        );
        tag.putBoolean(
                KEY_DEFENDING_SOVEREIGN_REFUSED_PEACE,
                defendingSovereignRefusedPeace
        );
        tag.putInt(
                KEY_TARGET_ATTACKER_COUNT,
                targetAttackerCount
        );
        tag.putLong(
                KEY_ASSEMBLY_STARTED_AT,
                assemblyStartedAt
        );
        tag.putLong(
                KEY_FORMATION_ENDS_AT,
                formationEndsAt
        );
        tag.putInt(
                KEY_LAST_ASSEMBLY_REPORTED_COUNT,
                lastAssemblyReportedCount
        );
        tag.putLong(
                KEY_FIELD_DEFEAT_RESOLUTION_AT,
                fieldDefeatResolutionAt
        );
        tag.putLong(
                KEY_CROWN_RALLY_ENDS_AT,
                crownRallyEndsAt
        );
        tag.putString(KEY_WAR_CAUSE, warCause.getSerializedName());
        tag.putString(KEY_WAR_GOAL, warGoal.getSerializedName());
        tag.put(
                KEY_ATTACKERS,
                saveCombatants(attackerIds)
        );
        tag.put(
                KEY_DEFENDERS,
                saveCombatants(defenderIds)
        );
        tag.put(
                KEY_RETURNED_ATTACKERS,
                saveCombatants(
                        returnedAttackerIds
                )
        );

        return tag;
    }

    public static CapitalCampaignRecord load(
            CompoundTag tag
    ) {
        if (tag == null
                || !tag.hasUUID(KEY_CAMPAIGN_ID)
                || !tag.hasUUID(
                KEY_ATTACKING_CAPITAL_ID
        )
                || !tag.hasUUID(
                KEY_DEFENDING_CAPITAL_ID
        )) {
            return null;
        }

        List<UUID> attackers =
                loadCombatants(
                        tag.getList(
                                KEY_ATTACKERS,
                                Tag.TAG_COMPOUND
                        ),
                        MAX_ATTACKERS
                );

        if (attackers.isEmpty()) {
            return null;
        }

        List<UUID> defenders =
                loadCombatants(
                        tag.getList(
                                KEY_DEFENDERS,
                                Tag.TAG_COMPOUND
                        ),
                        Integer.MAX_VALUE
                );

        List<UUID> returnedAttackers =
                loadCombatants(
                        tag.getList(
                                KEY_RETURNED_ATTACKERS,
                                Tag.TAG_COMPOUND
                        ),
                        MAX_ATTACKERS
                );

        UUID initiatingPlayerId =
                tag.hasUUID(
                        KEY_INITIATING_PLAYER_ID
                )
                        ? tag.getUUID(
                        KEY_INITIATING_PLAYER_ID
                )
                        : null;

        int targetAttackerCount =
                tag.contains(
                        KEY_TARGET_ATTACKER_COUNT,
                        Tag.TAG_INT
                )
                        ? tag.getInt(
                        KEY_TARGET_ATTACKER_COUNT
                )
                        : preferredTarget(
                        attackers.size()
                );

        int lastAssemblyReportedCount =
                tag.contains(
                        KEY_LAST_ASSEMBLY_REPORTED_COUNT,
                        Tag.TAG_INT
                )
                        ? tag.getInt(
                        KEY_LAST_ASSEMBLY_REPORTED_COUNT
                )
                        : -1;

        return new CapitalCampaignRecord(
                tag.getUUID(KEY_CAMPAIGN_ID),
                tag.getUUID(
                        KEY_ATTACKING_CAPITAL_ID
                ),
                tag.getUUID(
                        KEY_DEFENDING_CAPITAL_ID
                ),
                initiatingPlayerId,
                attackers,
                defenders,
                returnedAttackers,
                CapitalCampaignPhase
                        .fromSerializedName(
                                tag.getString(
                                        KEY_PHASE
                                )
                        ),
                tag.getLong(KEY_CREATED_AT),
                tag.getLong(KEY_ACTIVATED_AT),
                tag.getLong(
                        KEY_RETREAT_STARTED_AT
                ),
                tag.getLong(
                        KEY_RETURN_DEADLINE
                ),
                CapitalCampaignEndReason
                        .fromSerializedName(
                                tag.getString(
                                        KEY_END_REASON
                                )
                        ),
                tag.getBoolean(
                        KEY_DEFENDING_SOVEREIGN_REFUSED_PEACE
                ),
                targetAttackerCount,
                tag.getLong(
                        KEY_ASSEMBLY_STARTED_AT
                ),
                tag.getLong(
                        KEY_FORMATION_ENDS_AT
                ),
                lastAssemblyReportedCount,
                tag.getLong(
                        KEY_FIELD_DEFEAT_RESOLUTION_AT
                ),
                tag.getLong(
                        KEY_CROWN_RALLY_ENDS_AT
                ),
                CapitalWarCause.fromSerializedName(
                        tag.getString(KEY_WAR_CAUSE)
                ),
                CapitalWarGoal.fromSerializedName(
                        tag.getString(KEY_WAR_GOAL)
                )
        );
    }

    private void clearBattlePacing() {
        fieldDefeatResolutionAt = 0L;
        crownRallyEndsAt = 0L;
    }

    private static ListTag saveCombatants(
            List<UUID> ids
    ) {
        ListTag tag =
                new ListTag();

        for (UUID id : ids) {
            CompoundTag entry =
                    new CompoundTag();

            entry.putUUID(
                    KEY_COMBATANT_ID,
                    id
            );

            tag.add(entry);
        }

        return tag;
    }

    private static List<UUID> loadCombatants(
            ListTag tag,
            int limit
    ) {
        List<UUID> result =
                new ArrayList<>();

        for (Tag raw : tag) {
            if (result.size() >= limit) {
                break;
            }

            CompoundTag entry =
                    (CompoundTag) raw;

            if (entry.hasUUID(
                    KEY_COMBATANT_ID
            )) {
                result.add(
                        entry.getUUID(
                                KEY_COMBATANT_ID
                        )
                );

                continue;
            }

            if (entry.hasUUID(
                    "AttackerId"
            )) {
                result.add(
                        entry.getUUID(
                                "AttackerId"
                        )
                );
            }
        }

        return normalizeCombatants(
                result,
                limit
        );
    }

    private static int preferredTarget(
            int availableCount
    ) {
        return Math.min(
                MAX_ATTACKERS,
                Math.max(
                        PREFERRED_ATTACKERS,
                        availableCount
                )
        );
    }

    private static int normalizeTargetAttackerCount(
            int requestedCount,
            int rosterSize
    ) {
        int resolved =
                requestedCount <= 0
                        ? preferredTarget(
                        rosterSize
                )
                        : requestedCount;

        return Math.max(
                1,
                Math.min(
                        MAX_ATTACKERS,
                        resolved
                )
        );
    }

    private static List<UUID> normalizeAttackers(
            List<UUID> ids
    ) {
        return normalizeCombatants(
                ids,
                MAX_ATTACKERS
        );
    }

    private static List<UUID> normalizeCombatants(
            List<UUID> ids,
            int limit
    ) {
        Set<UUID> unique =
                new LinkedHashSet<>();

        if (ids != null) {
            for (UUID id : ids) {
                if (id == null) {
                    continue;
                }

                unique.add(id);

                if (unique.size() >= limit) {
                    break;
                }
            }
        }

        return new ArrayList<>(unique);
    }
}