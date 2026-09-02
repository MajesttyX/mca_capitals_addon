package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
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
        CapitalDiplomaticGiftValidation
                .Validation validation =
                CapitalDiplomaticGiftValidation
                        .validateAudience(
                                player,
                                ambassadorEntity,
                                false
                        );

        if (!validation.valid()) {
            if (player != null) {
                sendMessage(
                        player,
                        Component.translatable(
                                "mcacapitals.ui.diplomatic_gifts.title"
                        ),
                        validation.failureMessage()
                );
            }

            return true;
        }

        CapitalDiplomaticGiftValidation
                .HeldPackage heldPackage =
                CapitalDiplomaticGiftValidation
                        .findHeldPackage(
                                player
                        );

        if (heldPackage == null) {
            MCAIntegrationBridge.stopInteracting(
                    ambassadorEntity
            );

            sendMessage(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.title"
                    ),
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.need_filled_package",
                            ambassadorEntity.getName()
                    )
            );

            return true;
        }

        List<ItemStack> contents =
                CapitalDiplomaticGiftValidation
                        .readAndValidateContents(
                                heldPackage.stack()
                        );

        if (contents.isEmpty()) {
            MCAIntegrationBridge.stopInteracting(
                    ambassadorEntity
            );

            sendMessage(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.title"
                    ),
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.package_empty",
                            ambassadorEntity.getName()
                    )
            );

            return true;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord sourceCapital =
                validation.sourceCapital();

        UUID ambassadorId =
                ambassadorEntity.getUUID();

        List<CapitalRecord> targets =
                CapitalManager
                        .getAllCapitalRecords()
                        .stream()
                        .filter(target ->
                                target != null
                        )
                        .filter(target ->
                                target.getState()
                                        == CapitalState.ACTIVE
                        )
                        .filter(target ->
                                target.getCapitalId()
                                        != null
                        )
                        .filter(target ->
                                !target.getCapitalId()
                                        .equals(
                                                sourceCapital
                                                        .getCapitalId()
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
            sendMessage(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.title"
                    ),
                    Component.translatable(
                            "mcacapitals.ui.diplomatic_gifts.no_targets",
                            ambassadorEntity.getName()
                    )
            );

            return true;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (CapitalRecord target :
                targets) {
            int score =
                    CapitalDiplomacyDataAccess
                            .getRelationshipScore(
                                    level,
                                    sourceCapital
                                            .getCapitalId(),
                                    target.getCapitalId()
                            );

            long cooldown =
                    CapitalDiplomacyDataAccess
                            .getGiftCooldownRemaining(
                                    level,
                                    sourceCapital
                                            .getCapitalId(),
                                    target.getCapitalId()
                            );

            String targetName =
                    CapitalDiplomaticGiftText
                            .getCapitalName(
                                    level,
                                    target
                            );

            Component targetNameComponent =
                    targetName.isBlank()
                            ? Component.translatable(
                                    "mcacapitals.diplomacy.unknown_capital"
                            )
                            : Component.literal(
                                    targetName
                            );

            Component relationship =
                    CapitalRelationshipBand
                            .fromScore(score)
                            .getDisplayComponent();

            boolean enabled =
                    cooldown <= 0L;

            Component statusText =
                    enabled
                            ? Component.translatable(
                                    "mcacapitals.ui.diplomatic_gifts.ready"
                            )
                            : Component.empty();

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            targetNameComponent,
                            Component.translatable(
                                    "mcacapitals.ui.diplomacy.relationship_score",
                                    relationship,
                                    score
                            ),
                            statusText,
                            Component.empty(),
                            Component.translatable(
                                    "mcacapitals.ui.diplomatic_gifts.send_to",
                                    targetNameComponent
                            ),
                            "/capitalgift send "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            enabled,
                            Component.empty()
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .GIFT_DESTINATIONS,
                        Component.translatable(
                                "mcacapitals.ui.diplomatic_gifts.send_title"
                        ),
                        ambassadorEntity.getName(),
                        Component.translatable(
                                "mcacapitals.ui.diplomatic_gifts.choose_target"
                        ),
                        "",
                        entries,
                        List.of()
                )
        );

        return true;
    }

    private static void sendMessage(
            ServerPlayer player,
            Component title,
            Component message
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .MESSAGE,
                        title,
                        Component.empty(),
                        message,
                        "",
                        List.of(),
                        List.of()
                )
        );
    }
}
