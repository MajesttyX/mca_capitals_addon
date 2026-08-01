package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.AccusationSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenAccusationSelectionPacket {

    private final UUID capitalId;
    private final String villageName;
    private final List<Candidate> candidates;

    public OpenAccusationSelectionPacket(UUID capitalId, String villageName, List<Candidate> candidates) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
        this.candidates = new ArrayList<>(candidates);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<Candidate> candidates() {
        return candidates;
    }

    public static void encode(OpenAccusationSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
        buffer.writeInt(packet.candidates.size());

        for (Candidate candidate : packet.candidates) {
            buffer.writeUUID(candidate.id());
            buffer.writeUtf(candidate.name());
        }
    }

    public static OpenAccusationSelectionPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        int size = buffer.readInt();

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buffer.readUUID(), buffer.readUtf()));
        }

        return new OpenAccusationSelectionPacket(capitalId, villageName, candidates);
    }

    public static void handle(
            OpenAccusationSelectionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> AccusationSelectionClient.open(packet));
        context.setPacketHandled(true);
    }

    public record Candidate(UUID id, String name) {
    }
}