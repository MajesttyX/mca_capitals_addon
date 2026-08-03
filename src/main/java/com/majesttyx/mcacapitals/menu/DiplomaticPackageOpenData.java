package com.majesttyx.mcacapitals.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record DiplomaticPackageOpenData(UUID packageId, boolean mainHand) {

    public static final StreamCodec<RegistryFriendlyByteBuf, DiplomaticPackageOpenData> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, data) -> {
                        buffer.writeUUID(data.packageId());
                        buffer.writeBoolean(data.mainHand());
                    },
                    buffer -> new DiplomaticPackageOpenData(
                            buffer.readUUID(),
                            buffer.readBoolean()
                    )
            );
}
