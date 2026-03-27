package com.example.mcacapitals.network;

import com.example.mcacapitals.client.ChronicleBookClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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

    public static void handle(OpenCapitalChroniclePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ChronicleBookClient.openBook(packet.bookStack));
        context.setPacketHandled(true);
    }
}