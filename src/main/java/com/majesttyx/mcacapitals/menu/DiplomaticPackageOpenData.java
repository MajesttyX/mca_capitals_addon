package com.majesttyx.mcacapitals.menu;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DiplomaticPackageOpenData(UUID packageId, boolean mainHand) {
    public static void encode(DiplomaticPackageOpenData data, FriendlyByteBuf buffer) {
        buffer.writeUUID(data.packageId());
        buffer.writeBoolean(data.mainHand());
    }

    public static DiplomaticPackageOpenData decode(FriendlyByteBuf buffer) {
        return new DiplomaticPackageOpenData(buffer.readUUID(), buffer.readBoolean());
    }
}
