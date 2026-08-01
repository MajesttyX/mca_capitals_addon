package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.AmbassadorCommunicationClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class OpenAmbassadorCommunicationPacket {

    private final Mode mode;
    private final String title;
    private final String subtitle;
    private final String message;
    private final String backCommand;
    private final List<Entry> entries;
    private final List<Action> actions;

    public OpenAmbassadorCommunicationPacket(
            Mode mode,
            String title,
            String subtitle,
            String message,
            String backCommand,
            List<Entry> entries,
            List<Action> actions
    ) {
        this.mode = mode == null ? Mode.MESSAGE : mode;
        this.title = safe(title);
        this.subtitle = safe(subtitle);
        this.message = safe(message);
        this.backCommand = safe(backCommand);
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public Mode mode() {
        return mode;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public String message() {
        return message;
    }

    public String backCommand() {
        return backCommand;
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<Action> actions() {
        return actions;
    }

    public static void encode(OpenAmbassadorCommunicationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mode.ordinal());
        buffer.writeUtf(packet.title);
        buffer.writeUtf(packet.subtitle);
        buffer.writeUtf(packet.message);
        buffer.writeUtf(packet.backCommand);

        buffer.writeVarInt(packet.entries.size());
        for (Entry entry : packet.entries) {
            buffer.writeUtf(entry.heading());
            buffer.writeUtf(entry.lineOne());
            buffer.writeUtf(entry.lineTwo());
            buffer.writeUtf(entry.lineThree());
            buffer.writeUtf(entry.buttonLabel());
            buffer.writeUtf(entry.command());
            buffer.writeBoolean(entry.enabled());
            buffer.writeUtf(entry.disabledReason());
        }

        buffer.writeVarInt(packet.actions.size());
        for (Action action : packet.actions) {
            buffer.writeUtf(action.label());
            buffer.writeUtf(action.description());
            buffer.writeUtf(action.command());
            buffer.writeBoolean(action.enabled());
        }
    }

    public static OpenAmbassadorCommunicationPacket decode(FriendlyByteBuf buffer) {
        int modeIndex = buffer.readVarInt();
        Mode[] values = Mode.values();

        Mode mode = modeIndex >= 0 && modeIndex < values.length
                ? values[modeIndex]
                : Mode.MESSAGE;

        String title = buffer.readUtf();
        String subtitle = buffer.readUtf();
        String message = buffer.readUtf();
        String backCommand = buffer.readUtf();

        int entryCount = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int index = 0; index < entryCount; index++) {
            entries.add(new Entry(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readBoolean(),
                    buffer.readUtf()
            ));
        }

        int actionCount = buffer.readVarInt();
        List<Action> actions = new ArrayList<>(actionCount);

        for (int index = 0; index < actionCount; index++) {
            actions.add(new Action(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readBoolean()
            ));
        }

        return new OpenAmbassadorCommunicationPacket(
                mode,
                title,
                subtitle,
                message,
                backCommand,
                entries,
                actions
        );
    }

    public static void handle(
            OpenAmbassadorCommunicationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> AmbassadorCommunicationClient.open(packet));
        context.setPacketHandled(true);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public enum Mode {
        FOREIGN_AFFAIRS,
        GIFT_DESTINATIONS,
        DIPLOMACY_TARGETS,
        DIPLOMACY_ACTIONS,
        ASYLUM_REQUESTS,
        ROYAL_ESCORT_REQUESTS,
        JUSTICE_CASES,
        MESSAGE
    }

    public record Entry(
            String heading,
            String lineOne,
            String lineTwo,
            String lineThree,
            String buttonLabel,
            String command,
            boolean enabled,
            String disabledReason
    ) {
        public Entry {
            heading = safe(heading);
            lineOne = safe(lineOne);
            lineTwo = safe(lineTwo);
            lineThree = safe(lineThree);
            buttonLabel = safe(buttonLabel);
            command = safe(command);
            disabledReason = safe(disabledReason);
        }
    }

    public record Action(
            String label,
            String description,
            String command,
            boolean enabled
    ) {
        public Action {
            label = safe(label);
            description = safe(description);
            command = safe(command);
        }
    }
}