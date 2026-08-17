package com.majesttyx.mcacapitals.capital;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public record CapitalChronicleEntry(
        long day,
        CapitalChronicleEventType type,
        String translationKey,
        String heraldKey,
        String dedupeKey,
        List<Argument> arguments
) {

    public static final String PREFIX = "@mcacapitals:chronicle:v1:";
    private static final int MAGIC = 0x4D434331;

    public enum ArgumentKind {
        LITERAL,
        TRANSLATABLE,
        ITEM_LIST,
        TRANSLATABLE_SNAPSHOT
    }

    public record Argument(ArgumentKind kind, String value) {
        public Argument {
            kind = kind == null ? ArgumentKind.LITERAL : kind;
            value = value == null ? "" : value;
        }

        public static Argument literal(Object value) {
            return new Argument(
                    ArgumentKind.LITERAL,
                    value == null ? "" : String.valueOf(value)
            );
        }

        public static Argument translatable(String translationKey) {
            return new Argument(
                    ArgumentKind.TRANSLATABLE,
                    translationKey == null ? "" : translationKey
            );
        }

        public static Argument translatableSnapshot(
                String translationKey,
                Object... literalArguments
        ) {
            return new Argument(
                    ArgumentKind.TRANSLATABLE_SNAPSHOT,
                    encodeTranslatableSnapshot(
                            translationKey,
                            literalArguments
                    )
            );
        }

        public static Argument itemList(String encodedItems) {
            return new Argument(
                    ArgumentKind.ITEM_LIST,
                    encodedItems == null ? "" : encodedItems
            );
        }

        Component component() {
            return switch (kind) {
                case TRANSLATABLE -> Component.translatable(value);
                case TRANSLATABLE_SNAPSHOT -> renderTranslatableSnapshot(value);
                case ITEM_LIST -> renderItemList(value);
                case LITERAL -> Component.literal(value);
            };
        }

        String dedupeKindName() {
            return kind == ArgumentKind.TRANSLATABLE_SNAPSHOT
                    ? ArgumentKind.LITERAL.name()
                    : kind.name();
        }

        String dedupeValue() {
            return kind == ArgumentKind.TRANSLATABLE_SNAPSHOT
                    ? component().getString()
                    : value;
        }

        private static String encodeTranslatableSnapshot(
                String translationKey,
                Object[] literalArguments
        ) {
            String key = translationKey == null
                    ? ""
                    : translationKey;
            Object[] arguments = literalArguments == null
                    ? new Object[0]
                    : literalArguments;

            try {
                ByteArrayOutputStream byteStream =
                        new ByteArrayOutputStream();
                try (DataOutputStream output =
                             new DataOutputStream(byteStream)) {
                    output.writeInt(0x4D435453);
                    output.writeUTF(key);
                    output.writeInt(arguments.length);
                    for (Object argument : arguments) {
                        output.writeUTF(
                                argument == null
                                        ? ""
                                        : String.valueOf(argument)
                        );
                    }
                }
                return Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(byteStream.toByteArray());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private static Component renderTranslatableSnapshot(
                String encodedSnapshot
        ) {
            TranslatableSnapshot snapshot =
                    decodeTranslatableSnapshot(encodedSnapshot);
            if (snapshot == null) {
                return Component.literal("");
            }

            Object[] arguments =
                    new Object[snapshot.arguments().size()];
            for (int i = 0; i < snapshot.arguments().size(); i++) {
                arguments[i] = Component.literal(
                        snapshot.arguments().get(i)
                );
            }
            return Component.translatable(
                    snapshot.translationKey(),
                    arguments
            );
        }

        private static TranslatableSnapshot decodeTranslatableSnapshot(
                String encodedSnapshot
        ) {
            if (encodedSnapshot == null
                    || encodedSnapshot.isBlank()) {
                return null;
            }

            try {
                byte[] bytes = Base64.getUrlDecoder().decode(
                        encodedSnapshot.getBytes(StandardCharsets.UTF_8)
                );
                try (DataInputStream input =
                             new DataInputStream(
                                     new ByteArrayInputStream(bytes)
                             )) {
                    if (input.readInt() != 0x4D435453) {
                        return null;
                    }

                    String translationKey = input.readUTF();
                    int count = input.readInt();
                    if (translationKey.isBlank()
                            || count < 0
                            || count > 32) {
                        return null;
                    }

                    List<String> arguments =
                            new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        arguments.add(input.readUTF());
                    }

                    return new TranslatableSnapshot(
                            translationKey,
                            List.copyOf(arguments)
                    );
                }
            } catch (IOException | IllegalArgumentException exception) {
                return null;
            }
        }

        private record TranslatableSnapshot(
                String translationKey,
                List<String> arguments
        ) {
        }

        private static Component renderItemList(String encodedItems) {
            if (encodedItems == null || encodedItems.isBlank()) {
                return Component.translatable("mcacapitals.chronicle.goods.none");
            }

            MutableComponent result = Component.empty();
            boolean first = true;
            for (String token : encodedItems.split(";")) {
                int separator = token.lastIndexOf('=');
                if (separator <= 0 || separator >= token.length() - 1) {
                    continue;
                }

                ResourceLocation itemId = ResourceLocation.tryParse(token.substring(0, separator));
                int count;
                try {
                    count = Integer.parseInt(token.substring(separator + 1));
                } catch (NumberFormatException exception) {
                    continue;
                }

                Item item = itemId == null ? null : BuiltInRegistries.ITEM.get(itemId);
                if (item == null || count <= 0) {
                    continue;
                }

                if (!first) {
                    result.append(Component.translatable("mcacapitals.chronicle.goods.separator"));
                }
                result.append(Component.translatable(
                        "mcacapitals.chronicle.goods.entry",
                        count,
                        Component.translatable(item.getDescriptionId())
                ));
                first = false;
            }

            return first
                    ? Component.translatable("mcacapitals.chronicle.goods.none")
                    : result;
        }
    }

    public CapitalChronicleEntry {
        type = type == null ? CapitalChronicleEventType.NONE : type;
        translationKey = translationKey == null ? "" : translationKey;
        heraldKey = heraldKey == null ? "" : heraldKey;
        dedupeKey = dedupeKey == null ? "" : dedupeKey;
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    public Component render() {
        return Component.translatable(
                translationKey,
                componentArguments()
        );
    }

    public Component renderWithDay() {
        return Component.translatable(
                "mcacapitals.chronicle.entry.day",
                Long.toString(day),
                render()
        );
    }

    public Component renderHeraldVariant(int variant) {
        if (heraldKey.isBlank()) {
            return null;
        }

        int normalizedVariant = Math.max(1, Math.min(3, variant));
        return Component.translatable(
                heraldKey + ".0" + normalizedVariant,
                componentArguments()
        );
    }

    public String encode() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(byteStream)) {
                output.writeInt(MAGIC);
                output.writeLong(day);
                output.writeUTF(type.name());
                output.writeUTF(translationKey);
                output.writeUTF(heraldKey);
                output.writeUTF(dedupeKey);
                output.writeInt(arguments.size());
                for (Argument argument : arguments) {
                    output.writeByte(argument.kind().ordinal());
                    output.writeUTF(argument.value());
                }
            }
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(byteStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static CapitalChronicleEntry decode(String raw) {
        if (raw == null || !raw.startsWith(PREFIX)) {
            return null;
        }

        String encoded = raw.substring(PREFIX.length());
        if (encoded.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8));
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) {
                    return null;
                }

                long day = input.readLong();
                CapitalChronicleEventType type = CapitalChronicleEventType.valueOf(input.readUTF());
                String translationKey = input.readUTF();
                String heraldKey = input.readUTF();
                String dedupeKey = input.readUTF();
                int count = input.readInt();
                if (count < 0 || count > 256) {
                    return null;
                }

                List<Argument> arguments = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int ordinal = input.readUnsignedByte();
                    if (ordinal < 0 || ordinal >= ArgumentKind.values().length) {
                        return null;
                    }
                    arguments.add(new Argument(
                            ArgumentKind.values()[ordinal],
                            input.readUTF()
                    ));
                }

                return new CapitalChronicleEntry(
                        day,
                        type,
                        translationKey,
                        heraldKey,
                        dedupeKey,
                        arguments
                );
            }
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private Object[] componentArguments() {
        Object[] values = new Object[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            values[i] = arguments.get(i).component();
        }
        return values;
    }
}
