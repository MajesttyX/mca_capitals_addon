package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.SealedPurseCaseSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OpenSealedPurseCaseSelectionPacket {
    private static final int MAX_CASES = 512;
    private final UUID capitalId;
    private final String villageName;
    private final List<CaseEntry> cases;

    public OpenSealedPurseCaseSelectionPacket(UUID capitalId, String villageName, List<CaseEntry> cases) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
        this.cases = new ArrayList<>(cases == null ? List.of() : cases);
    }

    public UUID capitalId() { return capitalId; }
    public String villageName() { return villageName; }
    public List<CaseEntry> cases() { return cases; }

    public static void encode(OpenSealedPurseCaseSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
        buffer.writeInt(packet.cases.size());
        for (CaseEntry caseEntry : packet.cases) {
            buffer.writeUUID(caseEntry.id());
            buffer.writeUtf(caseEntry.name());
            buffer.writeComponent(caseEntry.status());
        }
    }

    public static OpenSealedPurseCaseSelectionPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        int size = buffer.readInt();
        if (size < 0 || size > MAX_CASES) {
            throw new IllegalArgumentException("Invalid Sealed Purse case count: " + size);
        }
        List<CaseEntry> cases = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cases.add(new CaseEntry(buffer.readUUID(), buffer.readUtf(), buffer.readComponent()));
        }
        return new OpenSealedPurseCaseSelectionPacket(capitalId, villageName, cases);
    }

    public static void handle(OpenSealedPurseCaseSelectionPacket packet) {
        if (packet != null) {
            SealedPurseCaseSelectionClient.open(packet);
        }
    }

    public record CaseEntry(UUID id, String name, Component status) {
        public CaseEntry {
            name = name == null ? "" : name;
            status = status == null ? Component.empty() : status;
        }
    }
}
