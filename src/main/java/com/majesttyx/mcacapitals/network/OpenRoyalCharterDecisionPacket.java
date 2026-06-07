package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.item.RoyalCharterClient;
import net.minecraft.network.FriendlyByteBuf;

public class OpenRoyalCharterDecisionPacket {

    public OpenRoyalCharterDecisionPacket() {
    }

    public static void encode(OpenRoyalCharterDecisionPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenRoyalCharterDecisionPacket decode(FriendlyByteBuf buffer) {
        return new OpenRoyalCharterDecisionPacket();
    }

    public static void handle(OpenRoyalCharterDecisionPacket packet) {
        RoyalCharterClient.openDecisionScreen();
    }
}