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
                false
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

        this.createdAt =
                Math.max(0L, createdAt);

        this.activatedAt =
                Math.max(0L, activatedAt);

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

    public boolean containsCapital(UUID capitalId) {
        return capitalId != null
                && (
                capitalId.equals(attackingCapitalId)
                        || capitalId.equals(defendingCapitalId)
        );
    }

    public boolean containsAttacker(UUID villagerId) {
        return villagerId != null
                && attackerIds.contains(villagerId);
    }

    public boolean containsDefender(UUID villagerId) {
        return villagerId != null
                && defenderIds.contains(villagerId);
    }

    public boolean hasAttackerReturned(UUID villagerId) {
        return villagerId != null
                && returnedAttackerIds.contains(villagerId);
    }

    public boolean isActiveCampaign() {
        return phase == CapitalCampaignPhase.MUSTERING
                || phase == CapitalCampaignPhase.ACTIVE
                || phase == CapitalCampaignPhase.RETREATING;
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
        this.returnedAttackerIds.retainAll(normalized);
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

    public void markAttackerReturned(
            UUID attackerId
    ) {
        if (attackerId != null
                && attackerIds.contains(attackerId)
                && !returnedAttackerIds.contains(attackerId)) {
            returnedAttackerIds.add(attackerId);
        }
    }

    public void markDefendingSovereignRefusedPeace() {
        defendingSovereignRefusedPeace = true;
    }

    public void activate(long gameTime) {
        phase = CapitalCampaignPhase.ACTIVE;
        activatedAt = Math.max(0L, gameTime);
        retreatStartedAt = 0L;
        returnDeadline = 0L;
        endReason = CapitalCampaignEndReason.NONE;
    }

    public void beginRetreat(
            long gameTime,
            long returnDeadline,
            CapitalCampaignEndReason endReason
    ) {
        phase = CapitalCampaignPhase.RETREATING;

        retreatStartedAt =
                Math.max(0L, gameTime);

        this.returnDeadline =
                Math.max(
                        retreatStartedAt,
                        returnDeadline
                );

        this.endReason =
                endReason == null
                        ? CapitalCampaignEndReason.INVALIDATED
                        : endReason;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

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
                saveCombatants(returnedAttackerIds)
        );

        return tag;
    }

    public static CapitalCampaignRecord load(
            CompoundTag tag
    ) {
        if (tag == null
                || !tag.hasUUID(KEY_CAMPAIGN_ID)
                || !tag.hasUUID(KEY_ATTACKING_CAPITAL_ID)
                || !tag.hasUUID(KEY_DEFENDING_CAPITAL_ID)) {
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
                tag.hasUUID(KEY_INITIATING_PLAYER_ID)
                        ? tag.getUUID(KEY_INITIATING_PLAYER_ID)
                        : null;

        return new CapitalCampaignRecord(
                tag.getUUID(KEY_CAMPAIGN_ID),
                tag.getUUID(KEY_ATTACKING_CAPITAL_ID),
                tag.getUUID(KEY_DEFENDING_CAPITAL_ID),
                initiatingPlayerId,
                attackers,
                defenders,
                returnedAttackers,
                CapitalCampaignPhase.fromSerializedName(
                        tag.getString(KEY_PHASE)
                ),
                tag.getLong(KEY_CREATED_AT),
                tag.getLong(KEY_ACTIVATED_AT),
                tag.getLong(KEY_RETREAT_STARTED_AT),
                tag.getLong(KEY_RETURN_DEADLINE),
                CapitalCampaignEndReason.fromSerializedName(
                        tag.getString(KEY_END_REASON)
                ),
                tag.getBoolean(
                        KEY_DEFENDING_SOVEREIGN_REFUSED_PEACE
                )
        );
    }

    private static ListTag saveCombatants(
            List<UUID> ids
    ) {
        ListTag tag = new ListTag();

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

            if (entry.hasUUID(KEY_COMBATANT_ID)) {
                result.add(
                        entry.getUUID(KEY_COMBATANT_ID)
                );

                continue;
            }

            if (entry.hasUUID("AttackerId")) {
                result.add(
                        entry.getUUID("AttackerId")
                );
            }
        }

        return normalizeCombatants(
                result,
                limit
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