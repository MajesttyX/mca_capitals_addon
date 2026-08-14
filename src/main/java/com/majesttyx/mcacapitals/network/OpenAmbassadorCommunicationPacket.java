package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.AmbassadorCommunicationClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class OpenAmbassadorCommunicationPacket implements CustomPacketPayload {

    public static final Type<OpenAmbassadorCommunicationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "open_ambassador_communication"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAmbassadorCommunicationPacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    OpenAmbassadorCommunicationPacket::encode,
                    OpenAmbassadorCommunicationPacket::decode
            );

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

    public Mode mode() {
        return mode;
    }

    public Component title() {
        return title;
    }

    public Component subtitle() {
        return subtitle;
    }

    public Component message() {
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

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(mode.ordinal());
        ComponentSerialization.STREAM_CODEC.encode(buffer, title);
        ComponentSerialization.STREAM_CODEC.encode(buffer, subtitle);
        ComponentSerialization.STREAM_CODEC.encode(buffer, message);
        buffer.writeUtf(backCommand);

        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.heading());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.lineOne());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.lineTwo());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.lineThree());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.buttonLabel());
            buffer.writeUtf(entry.command());
            buffer.writeBoolean(entry.enabled());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.disabledReason());
        }

        buffer.writeVarInt(actions.size());
        for (Action action : actions) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, action.label());
            ComponentSerialization.STREAM_CODEC.encode(buffer, action.description());
            buffer.writeUtf(action.command());
            buffer.writeBoolean(action.enabled());
        }
    }

    private static OpenAmbassadorCommunicationPacket decode(
            RegistryFriendlyByteBuf buffer
    ) {
        int modeIndex = buffer.readVarInt();
        Mode[] values = Mode.values();

        Mode mode = modeIndex >= 0 && modeIndex < values.length
                ? values[modeIndex]
                : Mode.MESSAGE;

        Component title = ComponentSerialization.STREAM_CODEC.decode(buffer);
        Component subtitle = ComponentSerialization.STREAM_CODEC.decode(buffer);
        Component message = ComponentSerialization.STREAM_CODEC.decode(buffer);
        String backCommand = buffer.readUtf();

        int entryCount = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int index = 0; index < entryCount; index++) {
            entries.add(new Entry(
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    buffer.readUtf(),
                    buffer.readBoolean(),
                    ComponentSerialization.STREAM_CODEC.decode(buffer)
            ));
        }

        int actionCount = buffer.readVarInt();
        List<Action> actions = new ArrayList<>(actionCount);

        for (int index = 0; index < actionCount; index++) {
            actions.add(new Action(
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
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
            IPayloadContext context
    ) {
        context.enqueueWork(() -> AmbassadorCommunicationClient.open(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static Component safe(Component value) {
        return value == null ? Component.empty() : value;
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

    public record Action(
            Component label,
            Component description,
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
