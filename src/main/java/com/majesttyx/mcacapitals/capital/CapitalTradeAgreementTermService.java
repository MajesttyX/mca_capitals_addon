package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class CapitalTradeAgreementTermService {

    private CapitalTradeAgreementTermService() {
    }

    static boolean processTerm(
            ServerLevel level,
            CapitalTradeAgreement agreement
    ) {
        if (level == null || agreement == null) {
            return false;
        }

        CapitalRecord first = CapitalManager.getCapital(
                agreement.getFirstCapitalId()
        );
        CapitalRecord second = CapitalManager.getCapital(
                agreement.getSecondCapitalId()
        );
        if (first == null || second == null) {
            CapitalAgreementDataAccess.endTradeAgreement(
                    level,
                    agreement.getFirstCapitalId(),
                    agreement.getSecondCapitalId()
            );
            return false;
        }

        long now = level.getGameTime();
        if (agreement.needsTermInitialization()) {
            agreement.initializeLegacyTerm(now);
            CapitalAgreementDataAccess.markTradeAgreementChanged(level);
        }

        if (agreement.isExpired(now)) {
            expire(level, agreement, first, second);
            return false;
        }

        if (!agreement.isInRenewalWindow(now)) {
            return true;
        }

        if (!agreement.isRenewalNoticeSent()) {
            sendRenewalNotices(level, first, second);
            agreement.markRenewalNoticeSent();
            CapitalAgreementDataAccess.markTradeAgreementChanged(level);
        }

        if (!agreement.isRenewalProposalCreated()) {
            DiplomaticProposal pending =
                    CapitalAgreementDataAccess.findPendingBetween(
                            level,
                            first.getCapitalId(),
                            second.getCapitalId()
                    );
            if (pending != null
                    && pending.getType()
                    == DiplomaticProposalType.TRADE_AGREEMENT) {
                agreement.markRenewalProposalCreated();
                CapitalAgreementDataAccess.markTradeAgreementChanged(level);
            } else if (pending == null
                    && createNpcRenewalProposal(
                    level,
                    first,
                    second
            )) {
                agreement.markRenewalProposalCreated();
                CapitalAgreementDataAccess.markTradeAgreementChanged(level);
            }
        }

        return true;
    }

    private static void expire(
            ServerLevel level,
            CapitalTradeAgreement agreement,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (!CapitalDiplomaticTradeAgreementService.end(
                level,
                first,
                second,
                CapitalDiplomaticTradeAgreementService.TradeAgreementEndReason.TERM_EXPIRED
        )) {
            CapitalAgreementDataAccess.endTradeAgreement(
                    level,
                    agreement.getFirstCapitalId(),
                    agreement.getSecondCapitalId()
            );
        }

        String firstName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );
        String secondName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        Set<UUID> recipients = new LinkedHashSet<>();
        UUID firstDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        first
                );
        UUID secondDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        second
                );
        if (firstDecisionMaker != null) {
            recipients.add(firstDecisionMaker);
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    firstDecisionMaker,
                    net.minecraft.network.chat.Component.translatable(
                            "mcacapitals.diplomacy.trade.expired_title"
                    ),
                    net.minecraft.network.chat.Component.translatable(
                            "mcacapitals.diplomacy.trade.expired_message",
                            secondName
                    )
            );
        }
        if (secondDecisionMaker != null && recipients.add(secondDecisionMaker)) {
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    secondDecisionMaker,
                    net.minecraft.network.chat.Component.translatable(
                            "mcacapitals.diplomacy.trade.expired_title"
                    ),
                    net.minecraft.network.chat.Component.translatable(
                            "mcacapitals.diplomacy.trade.expired_message",
                            firstName
                    )
            );
        }
    }

    private static void sendRenewalNotices(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        String firstName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        first
                );
        String secondName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        second
                );

        net.minecraft.network.chat.Component title =
                net.minecraft.network.chat.Component.translatable(
                        "mcacapitals.diplomacy.trade.renewal_title"
                );
        net.minecraft.network.chat.Component message =
                net.minecraft.network.chat.Component.translatable(
                        "mcacapitals.diplomacy.trade.renewal_message",
                        firstName,
                        secondName
                );

        Set<UUID> recipients = new LinkedHashSet<>();
        UUID firstDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        first
                );
        UUID secondDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        second
                );
        if (firstDecisionMaker != null) {
            recipients.add(firstDecisionMaker);
        }
        if (secondDecisionMaker != null) {
            recipients.add(secondDecisionMaker);
        }
        for (UUID recipient : recipients) {
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    recipient,
                    title,
                    message
            );
        }
    }

    private static boolean createNpcRenewalProposal(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        UUID firstDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        first
                );
        UUID secondDecisionMaker =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        second
                );

        CapitalRecord source;
        CapitalRecord target;
        if (firstDecisionMaker == null
                && CapitalDiplomaticAgreementValidation
                .getCurrentSovereignId(first) != null) {
            source = first;
            target = second;
        } else if (secondDecisionMaker == null
                && CapitalDiplomaticAgreementValidation
                .getCurrentSovereignId(second) != null) {
            source = second;
            target = first;
        } else {
            return false;
        }

        UUID sourceSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(source);
        UUID targetSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(target);
        if (sourceSovereignId == null || targetSovereignId == null) {
            return false;
        }

        DiplomaticProposal proposal = new DiplomaticProposal(
                UUID.randomUUID(),
                source.getCapitalId(),
                target.getCapitalId(),
                sourceSovereignId,
                targetSovereignId,
                DiplomaticProposalType.TRADE_AGREEMENT,
                level.getGameTime()
        );
        proposal.setAvailableAt(
                CapitalDiplomaticDelayService.schedule(level)
        );
        CapitalAgreementDataAccess.addProposal(level, proposal);

        CapitalChronicleService.addEvent(
                level,
                source,
                CapitalChronicleEventId.TRADE_AGREEMENT_RENEWAL_PROPOSED,
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        target
                )
        );
        return true;
    }
}
