package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticGiftMenuService {

    private CapitalDiplomaticGiftMenuService() {
    }

    static boolean openDestinationList(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        CapitalDiplomaticGiftValidation.Validation validation =
                CapitalDiplomaticGiftValidation.validateAudience(
                        player,
                        ambassadorEntity,
                        true
                );

        if (!validation.valid()) {
            if (player != null
                    && validation.failureMessage() != null) {
                player.sendSystemMessage(
                        Component.literal(
                                validation.failureMessage()
                        )
                );
            }

            return true;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord sourceCapital = validation.sourceCapital();
        UUID ambassadorId = ambassadorEntity.getUUID();

        List<CapitalRecord> targets =
                CapitalManager.getAllCapitalRecords()
                        .stream()
                        .filter(target -> target != null)
                        .filter(target ->
                                target.getState()
                                        == CapitalState.ACTIVE
                        )
                        .filter(target ->
                                target.getCapitalId() != null
                        )
                        .filter(target ->
                                !target.getCapitalId().equals(
                                        sourceCapital.getCapitalId()
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        target ->
                                                CapitalDiplomaticGiftText
                                                        .getCapitalName(
                                                                level,
                                                                target
                                                        ),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        MCAIntegrationBridge.stopInteracting(
                ambassadorEntity
        );

        if (targets.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                            ambassadorEntity.getName().getString()
                                    + ": There are no other established capitals to receive a package."
                    )
            );

            return true;
        }

        player.sendSystemMessage(
                Component.literal(
                        ambassadorEntity.getName().getString()
                                + ": Choose the capital that should receive this package."
                )
        );

        for (CapitalRecord target : targets) {
            int score =
                    CapitalDiplomacyDataAccess.getRelationshipScore(
                            level,
                            sourceCapital.getCapitalId(),
                            target.getCapitalId()
                    );

            long cooldown =
                    CapitalDiplomacyDataAccess.getGiftCooldownRemaining(
                            level,
                            sourceCapital.getCapitalId(),
                            target.getCapitalId()
                    );

            String targetName =
                    CapitalDiplomaticGiftText.getCapitalName(
                            level,
                            target
                    );

            String relationship =
                    CapitalRelationshipBand.fromScore(score)
                            .getDisplayName();

            if (cooldown > 0L) {
                player.sendSystemMessage(
                        Component.literal("• " + targetName)
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .append(
                                        Component.literal(
                                                " — "
                                                        + relationship
                                                        + " ("
                                                        + score
                                                        + ")"
                                        ).withStyle(ChatFormatting.GRAY)
                                )
                                .append(
                                        Component.literal(
                                                " — available in "
                                                        + CapitalDiplomaticGiftText
                                                        .formatDuration(
                                                                cooldown
                                                        )
                                        ).withStyle(ChatFormatting.RED)
                                )
                );

                continue;
            }

            String command =
                    "/capitalgift send "
                            + ambassadorId
                            + " "
                            + target.getCapitalId();

            MutableComponent line =
                    Component.literal("[Send] ")
                            .setStyle(
                                    Style.EMPTY
                                            .withColor(ChatFormatting.GREEN)
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND,
                                                            command
                                                    )
                                            )
                                            .withHoverEvent(
                                                    new HoverEvent(
                                                            HoverEvent.Action.SHOW_TEXT,
                                                            Component.literal(
                                                                    "Send the held Diplomatic Package to "
                                                                            + targetName
                                                            )
                                                    )
                                            )
                            )
                            .append(
                                    Component.literal(targetName)
                                            .withStyle(ChatFormatting.GOLD)
                            )
                            .append(
                                    Component.literal(
                                            " — "
                                                    + relationship
                                                    + " ("
                                                    + score
                                                    + ")"
                                    ).withStyle(ChatFormatting.GRAY)
                            );

            player.sendSystemMessage(line);
        }

        return true;
    }
}