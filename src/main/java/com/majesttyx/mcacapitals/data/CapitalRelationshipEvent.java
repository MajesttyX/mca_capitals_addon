package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public record CapitalRelationshipEvent(
        int amount,
        String reason,
        long gameDay,
        UUID initiatingCapitalId
) {

    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_REASON = "Reason";
    private static final String KEY_GAME_DAY = "GameDay";
    private static final String KEY_INITIATING_CAPITAL_ID =
            "InitiatingCapitalId";

    private static final String LOCALIZED_REASON_PREFIX =
            "@mcacapitals:relationship_reason:v1:";
    private static final int LOCALIZED_REASON_MAGIC =
            0x4D435252;

    public CapitalRelationshipEvent {
        reason = reason == null ? "" : reason;
    }

    public static String localizedReason(
            String translationKey,
            Object... literalArguments
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
                output.writeInt(LOCALIZED_REASON_MAGIC);
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

            return LOCALIZED_REASON_PREFIX
                    + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(byteStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public Component reasonComponent() {
        LocalizedReason localized =
                decodeLocalizedReason(reason);
        if (localized != null) {
            Object[] arguments =
                    new Object[localized.arguments().size()];
            for (int i = 0; i < localized.arguments().size(); i++) {
                arguments[i] = Component.literal(
                        localized.arguments().get(i)
                );
            }
            return Component.translatable(
                    localized.translationKey(),
                    arguments
            );
        }

        if (reason.startsWith(
                "mcacapitals.relationship_reason."
        )) {
            return Component.translatable(reason);
        }

        return Component.literal(reason);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putInt(KEY_AMOUNT, amount);
        tag.putString(KEY_REASON, reason);
        tag.putLong(KEY_GAME_DAY, gameDay);

        if (initiatingCapitalId != null) {
            tag.putUUID(
                    KEY_INITIATING_CAPITAL_ID,
                    initiatingCapitalId
            );
        }

        return tag;
    }

    public static CapitalRelationshipEvent load(
            CompoundTag tag
    ) {
        UUID initiatingCapitalId =
                tag.hasUUID(KEY_INITIATING_CAPITAL_ID)
                        ? tag.getUUID(KEY_INITIATING_CAPITAL_ID)
                        : null;

        return new CapitalRelationshipEvent(
                tag.getInt(KEY_AMOUNT),
                tag.getString(KEY_REASON),
                tag.getLong(KEY_GAME_DAY),
                initiatingCapitalId
        );
    }

    private static LocalizedReason decodeLocalizedReason(
            String encodedReason
    ) {
        if (encodedReason == null
                || !encodedReason.startsWith(
                LOCALIZED_REASON_PREFIX
        )) {
            return null;
        }

        String encoded = encodedReason.substring(
                LOCALIZED_REASON_PREFIX.length()
        );
        if (encoded.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getUrlDecoder().decode(
                    encoded.getBytes(StandardCharsets.UTF_8)
            );
            try (DataInputStream input =
                         new DataInputStream(
                                 new ByteArrayInputStream(bytes)
                         )) {
                if (input.readInt() != LOCALIZED_REASON_MAGIC) {
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

                return new LocalizedReason(
                        translationKey,
                        List.copyOf(arguments)
                );
            }
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private record LocalizedReason(
            String translationKey,
            List<String> arguments
    ) {
    }
}
