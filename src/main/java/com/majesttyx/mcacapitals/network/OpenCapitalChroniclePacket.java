package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.ChronicleBookClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class OpenCapitalChroniclePacket {

    private final ItemStack bookStack;

    public OpenCapitalChroniclePacket(ItemStack bookStack) {
        this.bookStack = bookStack.copy();
    }

    public static void encode(OpenCapitalChroniclePacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.bookStack);
    }

    public static OpenCapitalChroniclePacket decode(FriendlyByteBuf buffer) {
        return new OpenCapitalChroniclePacket(buffer.readItem());
    }

    public static void handle(OpenCapitalChroniclePacket packet) {
        ChronicleBookClient.openBook(packet.bookStack);
    }
}