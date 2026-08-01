package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DiplomaticShipment {

    public static final int MAX_SLOTS = 3;

    private static final String KEY_SHIPMENT_ID = "ShipmentId";
    private static final String KEY_SOURCE_CAPITAL_ID = "SourceCapitalId";
    private static final String KEY_TARGET_CAPITAL_ID = "TargetCapitalId";
    private static final String KEY_SENDER_SOVEREIGN_ID = "SenderSovereignId";
    private static final String KEY_RECIPIENT_SOVEREIGN_ID = "RecipientSovereignId";
    private static final String KEY_NOTIFIED_PLAYER_ID = "NotifiedPlayerId";
    private static final String KEY_CREATED_AT = "CreatedAt";
    private static final String KEY_AVAILABLE_AT = "AvailableAt";
    private static final String KEY_RELATIONSHIP_DELTA = "RelationshipDelta";
    private static final String KEY_APPRAISAL = "Appraisal";
    private static final String KEY_STATUS = "Status";
    private static final String KEY_CONTENTS = "Contents";

    private final UUID shipmentId;
    private final UUID sourceCapitalId;
    private final UUID targetCapitalId;
    private final UUID senderSovereignId;
    private final UUID recipientSovereignId;
    private UUID notifiedPlayerId;
    private final long createdAt;
    private long availableAt;
    private final int relationshipDelta;
    private final String appraisal;
    private DiplomaticShipmentStatus status;
    private final List<ItemStack> contents;

    public DiplomaticShipment(
            UUID shipmentId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID senderSovereignId,
            UUID recipientSovereignId,
            long createdAt,
            int relationshipDelta,
            String appraisal,
            DiplomaticShipmentStatus status,
            List<ItemStack> contents
    ) {
        this(
                shipmentId,
                sourceCapitalId,
                targetCapitalId,
                senderSovereignId,
                recipientSovereignId,
                null,
                createdAt,
                createdAt,
                relationshipDelta,
                appraisal,
                status,
                contents
        );
    }

    public DiplomaticShipment(
            UUID shipmentId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID senderSovereignId,
            UUID recipientSovereignId,
            UUID notifiedPlayerId,
            long createdAt,
            int relationshipDelta,
            String appraisal,
            DiplomaticShipmentStatus status,
            List<ItemStack> contents
    ) {
        this(
                shipmentId,
                sourceCapitalId,
                targetCapitalId,
                senderSovereignId,
                recipientSovereignId,
                notifiedPlayerId,
                createdAt,
                createdAt,
                relationshipDelta,
                appraisal,
                status,
                contents
        );
    }

    public DiplomaticShipment(
            UUID shipmentId,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            UUID senderSovereignId,
            UUID recipientSovereignId,
            UUID notifiedPlayerId,
            long createdAt,
            long availableAt,
            int relationshipDelta,
            String appraisal,
            DiplomaticShipmentStatus status,
            List<ItemStack> contents
    ) {
        if (shipmentId == null
                || sourceCapitalId == null
                || targetCapitalId == null) {
            throw new IllegalArgumentException(
                    "Diplomatic shipment IDs cannot be null."
            );
        }

        if (sourceCapitalId.equals(targetCapitalId)) {
            throw new IllegalArgumentException(
                    "A diplomatic shipment cannot target its source capital."
            );
        }

        this.shipmentId = shipmentId;
        this.sourceCapitalId = sourceCapitalId;
        this.targetCapitalId = targetCapitalId;
        this.senderSovereignId = senderSovereignId;
        this.recipientSovereignId = recipientSovereignId;
        this.notifiedPlayerId = notifiedPlayerId;
        this.createdAt = Math.max(0L, createdAt);
        this.availableAt = Math.max(this.createdAt, availableAt);
        this.relationshipDelta = Math.max(-15, Math.min(10, relationshipDelta));
        this.appraisal = appraisal == null || appraisal.isBlank()
                ? "Trivial or confusing"
                : appraisal;
        this.status = status == null
                ? DiplomaticShipmentStatus.DISPATCHED
                : status;
        this.contents = copyContents(contents);
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public UUID getSourceCapitalId() {
        return sourceCapitalId;
    }

    public UUID getTargetCapitalId() {
        return targetCapitalId;
    }

    public UUID getSenderSovereignId() {
        return senderSovereignId;
    }

    public UUID getRecipientSovereignId() {
        return recipientSovereignId;
    }

    public UUID getNotifiedPlayerId() {
        return notifiedPlayerId;
    }

    public void setNotifiedPlayerId(UUID notifiedPlayerId) {
        this.notifiedPlayerId = notifiedPlayerId;
    }

    public boolean wasNotifiedTo(UUID playerId) {
        return playerId != null && playerId.equals(notifiedPlayerId);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(long availableAt) {
        this.availableAt = Math.max(0L, availableAt);
    }

    public boolean isReady(long gameTime) {
        return gameTime >= availableAt;
    }

    public int getRelationshipDelta() {
        return relationshipDelta;
    }

    public String getAppraisal() {
        return appraisal;
    }

    public DiplomaticShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(DiplomaticShipmentStatus status) {
        this.status = status == null
                ? DiplomaticShipmentStatus.DISPATCHED
                : status;
    }

    public List<ItemStack> getContents() {
        return Collections.unmodifiableList(copyContents(contents));
    }

    public boolean isAwaitingPlayerResponse() {
        return status == DiplomaticShipmentStatus.AWAITING_PLAYER_RESPONSE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_SHIPMENT_ID, shipmentId);
        tag.putUUID(KEY_SOURCE_CAPITAL_ID, sourceCapitalId);
        tag.putUUID(KEY_TARGET_CAPITAL_ID, targetCapitalId);

        if (senderSovereignId != null) {
            tag.putUUID(KEY_SENDER_SOVEREIGN_ID, senderSovereignId);
        }
        if (recipientSovereignId != null) {
            tag.putUUID(KEY_RECIPIENT_SOVEREIGN_ID, recipientSovereignId);
        }
        if (notifiedPlayerId != null) {
            tag.putUUID(KEY_NOTIFIED_PLAYER_ID, notifiedPlayerId);
        }

        tag.putLong(KEY_CREATED_AT, createdAt);
        tag.putLong(KEY_AVAILABLE_AT, availableAt);
        tag.putInt(KEY_RELATIONSHIP_DELTA, relationshipDelta);
        tag.putString(KEY_APPRAISAL, appraisal);
        tag.putString(KEY_STATUS, status.getSerializedName());

        ListTag contentsTag = new ListTag();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stack.save(stackTag);
                contentsTag.add(stackTag);
            }
        }
        tag.put(KEY_CONTENTS, contentsTag);
        return tag;
    }

    public static DiplomaticShipment load(CompoundTag tag) {
        if (!tag.hasUUID(KEY_SHIPMENT_ID)
                || !tag.hasUUID(KEY_SOURCE_CAPITAL_ID)
                || !tag.hasUUID(KEY_TARGET_CAPITAL_ID)) {
            return null;
        }

        List<ItemStack> contents = new ArrayList<>();
        ListTag contentsTag = tag.getList(KEY_CONTENTS, Tag.TAG_COMPOUND);
        for (Tag rawStack : contentsTag) {
            if (contents.size() >= MAX_SLOTS) {
                break;
            }

            ItemStack stack = ItemStack.of(
                    (CompoundTag) rawStack
            );
            if (!stack.isEmpty()) {
                contents.add(stack);
            }
        }

        UUID senderSovereignId = tag.hasUUID(KEY_SENDER_SOVEREIGN_ID)
                ? tag.getUUID(KEY_SENDER_SOVEREIGN_ID)
                : null;
        UUID recipientSovereignId = tag.hasUUID(KEY_RECIPIENT_SOVEREIGN_ID)
                ? tag.getUUID(KEY_RECIPIENT_SOVEREIGN_ID)
                : null;
        UUID notifiedPlayerId = tag.hasUUID(KEY_NOTIFIED_PLAYER_ID)
                ? tag.getUUID(KEY_NOTIFIED_PLAYER_ID)
                : null;
        long createdAt = tag.getLong(KEY_CREATED_AT);
        long availableAt = tag.contains(KEY_AVAILABLE_AT)
                ? tag.getLong(KEY_AVAILABLE_AT)
                : createdAt;

        return new DiplomaticShipment(
                tag.getUUID(KEY_SHIPMENT_ID),
                tag.getUUID(KEY_SOURCE_CAPITAL_ID),
                tag.getUUID(KEY_TARGET_CAPITAL_ID),
                senderSovereignId,
                recipientSovereignId,
                notifiedPlayerId,
                createdAt,
                availableAt,
                tag.getInt(KEY_RELATIONSHIP_DELTA),
                tag.getString(KEY_APPRAISAL),
                DiplomaticShipmentStatus.fromSerializedName(tag.getString(KEY_STATUS)),
                contents
        );
    }

    private static List<ItemStack> copyContents(List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (ItemStack stack : source) {
            if (copy.size() >= MAX_SLOTS) {
                break;
            }
            if (stack != null && !stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }

        return copy;
    }
}