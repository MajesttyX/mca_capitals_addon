package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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
                        "Diplomatic Gifts",
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
                    "Diplomatic Gifts",
                    ambassadorEntity
                            .getName()
                            .getString()
                            + ": You will need to hold a filled Diplomatic Package before I can send a gift to another capital."
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
                    "Diplomatic Gifts",
                    ambassadorEntity
                            .getName()
                            .getString()
                            + ": This Diplomatic Package is empty. Place at least one gift inside it before we send it."
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
                    "Diplomatic Gifts",
                    ambassadorEntity
                            .getName()
                            .getString()
                            + ": There are no other established capitals to receive a package."
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

            String relationship =
                    CapitalRelationshipBand
                            .fromScore(score)
                            .getDisplayName();

            boolean enabled =
                    cooldown <= 0L;

            String cooldownText =
                    enabled
                            ? "Status: Ready to Send"
                            : "Available in "
                            + CapitalDiplomaticGiftText
                            .formatDuration(
                                    cooldown
                            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            targetName,
                            "Relationship: "
                                    + relationship
                                    + " ("
                                    + score
                                    + ")",
                            cooldownText,
                            "",
                            "Send Gift to "
                                    + targetName,
                            "/capitalgift send "
                                    + ambassadorId
                                    + " "
                                    + target.getCapitalId(),
                            enabled,
                            enabled
                                    ? ""
                                    : cooldownText
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.GIFT_DESTINATIONS,
                        "Send Gift to Capital",
                        ambassadorEntity
                                .getName()
                                .getString(),
                        "Choose the capital that should receive the held Diplomatic Package.",
                        "",
                        entries,
                        List.of()
                )
        );

        return true;
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        "",
                        message,
                        "",
                        List.of(),
                        List.of()
                )
        );
    }
}