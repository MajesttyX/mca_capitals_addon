package com.example.mcacapitals.network;

import com.example.mcacapitals.client.BetrothalSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenBetrothalSelectionPacket {

    private final UUID capitalId;
    private final String villageName;
    private final List<Candidate> playerCandidates;
    private final List<Candidate> recommendationCandidates;

    public OpenBetrothalSelectionPacket(
            UUID capitalId,
            String villageName,
            List<Candidate> playerCandidates,
            List<Candidate> recommendationCandidates
    ) {
        this.capitalId = capitalId;
        this.villageName = villageName;
        this.playerCandidates = new ArrayList<>(playerCandidates);
        this.recommendationCandidates = new ArrayList<>(recommendationCandidates);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<Candidate> playerCandidates() {
        return playerCandidates;
    }

    public List<Candidate> recommendationCandidates() {
        return recommendationCandidates;
    }

    public static void encode(OpenBetrothalSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
        writeCandidates(buffer, packet.playerCandidates);
        writeCandidates(buffer, packet.recommendationCandidates);
    }

    public static OpenBetrothalSelectionPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        List<Candidate> playerCandidates = readCandidates(buffer);
        List<Candidate> recommendationCandidates = readCandidates(buffer);

        return new OpenBetrothalSelectionPacket(
                capitalId,
                villageName,
                playerCandidates,
                recommendationCandidates
        );
    }

    public static void handle(OpenBetrothalSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BetrothalSelectionClient.open(packet));
        context.setPacketHandled(true);
    }

    private static void writeCandidates(FriendlyByteBuf buffer, List<Candidate> candidates) {
        buffer.writeInt(candidates.size());
        for (Candidate candidate : candidates) {
            buffer.writeUUID(candidate.id);
            buffer.writeUtf(candidate.name);
        }
    }

    private static List<Candidate> readCandidates(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buffer.readUUID(), buffer.readUtf()));
        }
        return candidates;
    }

    public static class Candidate {
        private final UUID id;
        private final String name;

        public Candidate(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }
    }
}