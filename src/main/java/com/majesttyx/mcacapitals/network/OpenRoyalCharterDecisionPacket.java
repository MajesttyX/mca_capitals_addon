package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.item.RoyalCharterClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRoyalCharterDecisionPacket {

    public OpenRoyalCharterDecisionPacket() {
    }

    public static void encode(OpenRoyalCharterDecisionPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenRoyalCharterDecisionPacket decode(FriendlyByteBuf buffer) {
        return new OpenRoyalCharterDecisionPacket();
    }

    public static void handle(OpenRoyalCharterDecisionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(RoyalCharterClient::openDecisionScreen);
        context.setPacketHandled(true);
    }
}