package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalDiplomacySavedData
        extends SavedData {

    public static final String DATA_NAME =
            "mcacapitals_diplomacy_data";

    public static final long GIFT_COOLDOWN_TICKS =
            48000L;

    private static final String KEY_AMBASSADORS =
            "Ambassadors";

    private static final String KEY_RELATIONSHIPS =
            "Relationships";

    private static final String KEY_GIFT_COOLDOWNS =
            "GiftCooldowns";

    private static final String KEY_SHIPMENTS =
            "Shipments";

    private static final String KEY_CAPITAL_ID =
            "CapitalId";

    private static final String KEY_AMBASSADOR_ID =
            "AmbassadorId";

    private static final String KEY_SOURCE_CAPITAL_ID =
            "SourceCapitalId";

    private static final String KEY_TARGET_CAPITAL_ID =
            "TargetCapitalId";

    private static final String KEY_EXPIRES_AT =
            "ExpiresAt";

    private final Map<UUID, UUID>
            ambassadorsByCapital =
            new LinkedHashMap<>();

    private final Map<
            CapitalRelationKey,
            CapitalRelationRecord
            > relationships =
            new LinkedHashMap<>();

    private final Map<
            CapitalRouteKey,
            Long
            > giftCooldowns =
            new LinkedHashMap<>();

    private final Map<
            UUID,
            DiplomaticShipment
            > shipments =
            new LinkedHashMap<>();

    public UUID getAmbassador(UUID capitalId) {
        if (capitalId == null) {
            return null;
        }

        return ambassadorsByCapital.get(capitalId);
    }

    public void setAmbassador(
            UUID capitalId,
            UUID ambassadorId
    ) {
        if (capitalId == null) {
            return;
        }

        if (ambassadorId == null) {
            clearAmbassador(capitalId);
            return;
        }

        UUID previous = ambassadorsByCapital.put(
                capitalId,
                ambassadorId
        );

        if (!ambassadorId.equals(previous)) {
            setDirty();
        }
    }

    public boolean clearAmbassador(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return false;
        }

        boolean removed =
                ambassadorsByCapital.remove(
                        capitalId
                ) != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }

    public Map<UUID, UUID>
    getAmbassadorsSnapshot() {
        return new LinkedHashMap<>(
                ambassadorsByCapital
        );
    }

    public CapitalRelationRecord
    getOrCreateRelationship(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        CapitalRelationKey key =
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                );

        return relationships.computeIfAbsent(
                key,
                ignored -> {
                    setDirty();
                    return new CapitalRelationRecord(key);
                }
        );
    }

    public CapitalRelationRecord getRelationship(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return null;
        }

        return relationships.get(
                CapitalRelationKey.of(
                        firstCapitalId,
                        secondCapitalId
                )
        );
    }

    public int getRelationshipScore(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        CapitalRelationRecord record =
                getRelationship(
                        firstCapitalId,
                        secondCapitalId
                );

        return record == null
                ? 0
                : record.getScore();
    }

    public CapitalDiplomaticState
    getDiplomaticState(
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        CapitalRelationRecord record =
                getRelationship(
                        firstCapitalId,
                        secondCapitalId
                );

        return record == null
                ? CapitalDiplomaticState.PEACE
                : record.getDiplomaticState();
    }

    public int adjustRelationship(
            UUID firstCapitalId,
            UUID secondCapitalId,
            int amount,
            String reason,
            long gameDay,
            UUID initiatingCapitalId
    ) {
        CapitalRelationRecord record =
                getOrCreateRelationship(
                        firstCapitalId,
                        secondCapitalId
                );

        int applied = record.adjustScore(
                amount,
                reason,
                gameDay,
                initiatingCapitalId
        );

        setDirty();

        return applied;
    }

    public void setDiplomaticState(
            UUID firstCapitalId,
            UUID secondCapitalId,
            CapitalDiplomaticState state,
            long truceUntil
    ) {
        CapitalRelationRecord record =
                getOrCreateRelationship(
                        firstCapitalId,
                        secondCapitalId
                );

        record.setDiplomaticState(state);
        record.setTruceUntil(truceUntil);

        setDirty();
    }

    public Map<
            CapitalRelationKey,
            CapitalRelationRecord
            > getRelationshipsSnapshot() {
        return new LinkedHashMap<>(relationships);
    }

    public long getGiftCooldownExpiresAt(
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)) {
            return 0L;
        }

        return giftCooldowns.getOrDefault(
                new CapitalRouteKey(
                        sourceCapitalId,
                        targetCapitalId
                ),
                0L
        );
    }

    public long getGiftCooldownRemaining(
            UUID sourceCapitalId,
            UUID targetCapitalId,
            long currentGameTime
    ) {
        return Math.max(
                0L,
                getGiftCooldownExpiresAt(
                        sourceCapitalId,
                        targetCapitalId
                ) - currentGameTime
        );
    }

    public void beginGiftCooldown(
            UUID sourceCapitalId,
            UUID targetCapitalId,
            long currentGameTime
    ) {
        giftCooldowns.put(
                new CapitalRouteKey(
                        sourceCapitalId,
                        targetCapitalId
                ),
                Math.max(0L, currentGameTime)
                        + GIFT_COOLDOWN_TICKS
        );

        setDirty();
    }

    public void removeExpiredGiftCooldowns(
            long currentGameTime
    ) {
        boolean removed =
                giftCooldowns.entrySet().removeIf(
                        entry ->
                                entry.getValue()
                                        <= currentGameTime
                );

        if (removed) {
            setDirty();
        }
    }

    public Map<CapitalRouteKey, Long>
    getGiftCooldownsSnapshot() {
        return new LinkedHashMap<>(giftCooldowns);
    }

    public boolean clearGiftCooldown(
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)) {
            return false;
        }

        boolean removed = giftCooldowns.remove(
                new CapitalRouteKey(
                        sourceCapitalId,
                        targetCapitalId
                )
        ) != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }

    public boolean removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        boolean changed =
                ambassadorsByCapital.remove(capitalId)
                        != null;

        changed |= relationships.entrySet()
                .removeIf(entry ->
                        capitalId.equals(
                                entry.getKey().first()
                        )
                                || capitalId.equals(
                                entry.getKey().second()
                        )
                );

        changed |= giftCooldowns.entrySet()
                .removeIf(entry ->
                        capitalId.equals(
                                entry.getKey()
                                        .sourceCapitalId()
                        )
                                || capitalId.equals(
                                entry.getKey()
                                        .targetCapitalId()
                        )
                );

        changed |= shipments.entrySet()
                .removeIf(entry -> {
                    DiplomaticShipment shipment =
                            entry.getValue();

                    return shipment != null
                            && (
                            capitalId.equals(
                                    shipment
                                            .getSourceCapitalId()
                            )
                                    || capitalId.equals(
                                    shipment
                                            .getTargetCapitalId()
                            )
                    );
                });

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public void addShipment(
            DiplomaticShipment shipment
    ) {
        if (shipment == null) {
            return;
        }

        shipments.put(
                shipment.getShipmentId(),
                shipment
        );

        setDirty();
    }

    public DiplomaticShipment getShipment(
            UUID shipmentId
    ) {
        if (shipmentId == null) {
            return null;
        }

        return shipments.get(shipmentId);
    }

    public boolean removeShipment(
            UUID shipmentId
    ) {
        if (shipmentId == null) {
            return false;
        }

        boolean removed =
                shipments.remove(shipmentId) != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }

    public List<DiplomaticShipment>
    getShipmentsForTarget(
            UUID targetCapitalId
    ) {
        if (targetCapitalId == null) {
            return List.of();
        }

        List<DiplomaticShipment> result =
                new ArrayList<>();

        for (DiplomaticShipment shipment :
                shipments.values()) {
            if (targetCapitalId.equals(
                    shipment.getTargetCapitalId()
            )) {
                result.add(shipment);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public List<DiplomaticShipment>
    getPendingPlayerShipments(
            UUID targetCapitalId
    ) {
        if (targetCapitalId == null) {
            return List.of();
        }

        List<DiplomaticShipment> result =
                new ArrayList<>();

        for (DiplomaticShipment shipment :
                shipments.values()) {
            if (targetCapitalId.equals(
                    shipment.getTargetCapitalId()
            )
                    && shipment
                    .isAwaitingPlayerResponse()) {
                result.add(shipment);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public Map<UUID, DiplomaticShipment>
    getShipmentsSnapshot() {
        return new LinkedHashMap<>(shipments);
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag ambassadorsTag = new ListTag();

        for (Map.Entry<UUID, UUID> entry :
                ambassadorsByCapital.entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null) {
                continue;
            }

            CompoundTag ambassadorTag =
                    new CompoundTag();

            ambassadorTag.putUUID(
                    KEY_CAPITAL_ID,
                    entry.getKey()
            );

            ambassadorTag.putUUID(
                    KEY_AMBASSADOR_ID,
                    entry.getValue()
            );

            ambassadorsTag.add(ambassadorTag);
        }

        tag.put(
                KEY_AMBASSADORS,
                ambassadorsTag
        );

        ListTag relationshipsTag =
                new ListTag();

        for (CapitalRelationRecord relationship :
                relationships.values()) {
            relationshipsTag.add(
                    relationship.save()
            );
        }

        tag.put(
                KEY_RELATIONSHIPS,
                relationshipsTag
        );

        ListTag cooldownsTag = new ListTag();

        for (Map.Entry<CapitalRouteKey, Long> entry :
                giftCooldowns.entrySet()) {
            CompoundTag cooldownTag =
                    new CompoundTag();

            cooldownTag.putUUID(
                    KEY_SOURCE_CAPITAL_ID,
                    entry.getKey().sourceCapitalId()
            );

            cooldownTag.putUUID(
                    KEY_TARGET_CAPITAL_ID,
                    entry.getKey().targetCapitalId()
            );

            cooldownTag.putLong(
                    KEY_EXPIRES_AT,
                    entry.getValue()
            );

            cooldownsTag.add(cooldownTag);
        }

        tag.put(
                KEY_GIFT_COOLDOWNS,
                cooldownsTag
        );

        ListTag shipmentsTag =
                new ListTag();

        for (DiplomaticShipment shipment :
                shipments.values()) {
            shipmentsTag.add(
                    shipment.save(registries)
            );
        }

        tag.put(
                KEY_SHIPMENTS,
                shipmentsTag
        );

        return tag;
    }

    public static CapitalDiplomacySavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        CapitalDiplomacySavedData data =
                new CapitalDiplomacySavedData();

        ListTag ambassadorsTag = tag.getList(
                KEY_AMBASSADORS,
                Tag.TAG_COMPOUND
        );

        for (Tag rawEntry : ambassadorsTag) {
            CompoundTag ambassadorTag =
                    (CompoundTag) rawEntry;

            if (!ambassadorTag.hasUUID(KEY_CAPITAL_ID)
                    || !ambassadorTag.hasUUID(
                    KEY_AMBASSADOR_ID
            )) {
                continue;
            }

            data.ambassadorsByCapital.put(
                    ambassadorTag.getUUID(
                            KEY_CAPITAL_ID
                    ),
                    ambassadorTag.getUUID(
                            KEY_AMBASSADOR_ID
                    )
            );
        }

        ListTag relationshipsTag =
                tag.getList(
                        KEY_RELATIONSHIPS,
                        Tag.TAG_COMPOUND
                );

        for (Tag rawEntry : relationshipsTag) {
            CapitalRelationRecord relationship =
                    CapitalRelationRecord.load(
                            (CompoundTag) rawEntry
                    );

            if (relationship != null) {
                data.relationships.put(
                        relationship.getKey(),
                        relationship
                );
            }
        }

        ListTag cooldownsTag = tag.getList(
                KEY_GIFT_COOLDOWNS,
                Tag.TAG_COMPOUND
        );

        for (Tag rawEntry : cooldownsTag) {
            CompoundTag cooldownTag =
                    (CompoundTag) rawEntry;

            if (!cooldownTag.hasUUID(
                    KEY_SOURCE_CAPITAL_ID
            )
                    || !cooldownTag.hasUUID(
                    KEY_TARGET_CAPITAL_ID
            )) {
                continue;
            }

            UUID sourceCapitalId =
                    cooldownTag.getUUID(
                            KEY_SOURCE_CAPITAL_ID
                    );

            UUID targetCapitalId =
                    cooldownTag.getUUID(
                            KEY_TARGET_CAPITAL_ID
                    );

            if (sourceCapitalId.equals(
                    targetCapitalId
            )) {
                continue;
            }

            data.giftCooldowns.put(
                    new CapitalRouteKey(
                            sourceCapitalId,
                            targetCapitalId
                    ),
                    cooldownTag.getLong(
                            KEY_EXPIRES_AT
                    )
            );
        }

        ListTag shipmentsTag = tag.getList(
                KEY_SHIPMENTS,
                Tag.TAG_COMPOUND
        );

        for (Tag rawEntry : shipmentsTag) {
            DiplomaticShipment shipment =
                    DiplomaticShipment.load(
                            (CompoundTag) rawEntry,
                            registries
                    );

            if (shipment != null) {
                data.shipments.put(
                        shipment.getShipmentId(),
                        shipment
                );
            }
        }

        return data;
    }
}