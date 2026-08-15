package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.AmbassadorCommunicationClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class OpenAmbassadorCommunicationPacket {

    private static final int MAX_ENTRIES = 512;
    private static final int MAX_ACTIONS = 512;

    private final Mode mode;
    private final Component title;
    private final Component subtitle;
    private final Component message;
    private final String backCommand;
    private final List<Entry> entries;
    private final List<Action> actions;

    public OpenAmbassadorCommunicationPacket(
            Mode mode,
            Component title,
            Component subtitle,
            Component message,
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

    public Mode mode() { return mode; }
    public Component title() { return title; }
    public Component subtitle() { return subtitle; }
    public Component message() { return message; }
    public String backCommand() { return backCommand; }
    public List<Entry> entries() { return entries; }
    public List<Action> actions() { return actions; }

    public static void encode(OpenAmbassadorCommunicationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mode.ordinal());
        buffer.writeComponent(packet.title);
        buffer.writeComponent(packet.subtitle);
        buffer.writeComponent(packet.message);
        buffer.writeUtf(packet.backCommand);
        buffer.writeVarInt(packet.entries.size());
        for (Entry entry : packet.entries) {
            buffer.writeComponent(entry.heading());
            buffer.writeComponent(entry.lineOne());
            buffer.writeComponent(entry.lineTwo());
            buffer.writeComponent(entry.lineThree());
            buffer.writeComponent(entry.buttonLabel());
            buffer.writeUtf(entry.command());
            buffer.writeBoolean(entry.enabled());
            buffer.writeComponent(entry.disabledReason());
        }
        buffer.writeVarInt(packet.actions.size());
        for (Action action : packet.actions) {
            buffer.writeComponent(action.label());
            buffer.writeComponent(action.description());
            buffer.writeUtf(action.command());
            buffer.writeBoolean(action.enabled());
        }
    }

    public static OpenAmbassadorCommunicationPacket decode(FriendlyByteBuf buffer) {
        int modeIndex = buffer.readVarInt();
        Mode[] values = Mode.values();
        Mode mode = modeIndex >= 0 && modeIndex < values.length ? values[modeIndex] : Mode.MESSAGE;
        Component title = buffer.readComponent();
        Component subtitle = buffer.readComponent();
        Component message = buffer.readComponent();
        String backCommand = buffer.readUtf();

        int entryCount = buffer.readVarInt();
        if (entryCount < 0 || entryCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid Ambassador entry count: " + entryCount);
        }
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            entries.add(new Entry(
                    buffer.readComponent(),
                    buffer.readComponent(),
                    buffer.readComponent(),
                    buffer.readComponent(),
                    buffer.readComponent(),
                    buffer.readUtf(),
                    buffer.readBoolean(),
                    buffer.readComponent()
            ));
        }

        int actionCount = buffer.readVarInt();
        if (actionCount < 0 || actionCount > MAX_ACTIONS) {
            throw new IllegalArgumentException("Invalid Ambassador action count: " + actionCount);
        }
        List<Action> actions = new ArrayList<>(actionCount);
        for (int index = 0; index < actionCount; index++) {
            actions.add(new Action(
                    buffer.readComponent(),
                    buffer.readComponent(),
                    buffer.readUtf(),
                    buffer.readBoolean()
            ));
        }
        return new OpenAmbassadorCommunicationPacket(mode, title, subtitle, message, backCommand, entries, actions);
    }

    public static void handle(
            OpenAmbassadorCommunicationPacket packet,
            java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier
    ) {
        net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> AmbassadorCommunicationClient.open(packet)
        ));
        context.setPacketHandled(true);
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static Component safe(Component value) { return value == null ? Component.empty() : value; }

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
            Component heading,
            Component lineOne,
            Component lineTwo,
            Component lineThree,
            Component buttonLabel,
            String command,
            boolean enabled,
            Component disabledReason
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

    public record Action(Component label, Component description, String command, boolean enabled) {
        public Action {
            label = safe(label);
            description = safe(description);
            command = safe(command);
        }
    }
}
