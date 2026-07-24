package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticAgreementCorrespondenceService {

    private CapitalDiplomaticAgreementCorrespondenceService() {
    }

    public static void sendProposalLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            DiplomaticProposal proposal,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital
    ) {
        if (level == null
                || recipientPlayerId == null
                || proposal == null
                || sourceCapital == null
                || targetCapital == null) {
            return;
        }

        String sourceName =
                CapitalDiplomaticCorrespondenceService.getCapitalName(
                        level,
                        sourceCapital
                );

        String targetName =
                CapitalDiplomaticCorrespondenceService.getCapitalName(
                        level,
                        targetCapital
                );

        MutableComponent page =
                Component.literal(
                        "The court of "
                                + sourceName
                                + " proposes a "
                                + proposal.getType().getDisplayName()
                                + " with "
                                + targetName
                                + ".\n\n"
                );

        page.append(
                commandButton(
                        "[Accept Proposal]",
                        "/capitalsacceptproposal "
                                + proposal.getProposalId(),
                        ChatFormatting.DARK_GREEN,
                        "Accept the proposed "
                                + proposal.getType().getDisplayName()
                                + "."
                )
        );

        page.append(Component.literal("\n\n"));

        page.append(
                commandButton(
                        "[Reject Proposal]",
                        "/capitalsrejectproposal "
                                + proposal.getProposalId(),
                        ChatFormatting.RED,
                        "Reject the proposed "
                                + proposal.getType().getDisplayName()
                                + "."
                )
        );

        sendLetter(
                level,
                recipientPlayerId,
                proposal.getType().getDisplayName()
                        + " Proposal from "
                        + sourceName,
                List.of(page)
        );
    }

    public static void sendNotice(
            ServerLevel level,
            UUID recipientPlayerId,
            String title,
            String message
    ) {
        if (level == null
                || recipientPlayerId == null
                || message == null
                || message.isBlank()) {
            return;
        }

        sendLetter(
                level,
                recipientPlayerId,
                title == null || title.isBlank()
                        ? "Diplomatic Correspondence"
                        : title,
                List.of(Component.literal(message))
        );
    }

    private static void sendLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            String title,
            List<Component> pages
    ) {
        PlayerSaveData saveData =
                PlayerSaveData.get(
                        level,
                        recipientPlayerId
                );

        saveData.sendMail(
                new PlayerSaveData.Letter(
                        title,
                        new ArrayList<>(pages)
                )
        );

        ServerPlayer online =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(recipientPlayerId);

        if (online != null) {
            PlayerSaveData.showMailNotification(online);

            online.sendSystemMessage(
                    Component.literal(
                            "A diplomatic letter is waiting in your MCA mailbox."
                    ).withStyle(ChatFormatting.GOLD)
            );
        }
    }

    private static MutableComponent commandButton(
            String label,
            String command,
            ChatFormatting color,
            String hoverText
    ) {
        return Component.literal(label)
                .setStyle(
                        Style.EMPTY
                                .withColor(color)
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
                                                Component.literal(hoverText)
                                        )
                                )
                );
    }
}