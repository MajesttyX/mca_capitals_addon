package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class CapitalDiplomaticWarService {

    private static final int
            WAR_DECLARATION_RELATIONSHIP_CHANGE = -200;

    private CapitalDiplomaticWarService() {
    }

    static int declareWar(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null
                || ambassadorId == null
                || targetCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            player.sendSystemMessage(
                    Component.literal(
                            audience.failureMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord source =
                audience.sourceCapital();

        CapitalRecord target =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        String targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                source,
                                target
                        );

        if (targetFailure != null) {
            player.sendSystemMessage(
                    Component.literal(targetFailure)
            );

            return 0;
        }

        CapitalDiplomaticTruceService
                .refreshExpiredTruce(
                        level,
                        source,
                        target
                );

        CapitalRelationRecord relation =
                CapitalDiplomacyDataAccess
                        .getOrCreateRelationship(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        if (relation == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "The diplomatic relationship could not be resolved."
                    )
            );

            return 0;
        }

        if (relation.getDiplomaticState()
                == CapitalDiplomaticState.WAR) {
            player.sendSystemMessage(
                    Component.literal(
                            "These capitals are already at war."
                    )
            );

            return 0;
        }

        if (relation.getDiplomaticState()
                == CapitalDiplomaticState.TRUCE
                && relation.getTruceUntil()
                > level.getGameTime()) {
            player.sendSystemMessage(
                    Component.literal(
                            "War cannot be declared directly while the active truce remains in force. A player-led attack may still break it when the authorized player enters the target capital."
                    )
            );

            return 0;
        }

        if (!applyWarState(
                level,
                source,
                target,
                false
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "War could not be declared."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "War has been declared on "
                                + CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        target
                                )
                                + "."
                )
        );

        return 1;
    }

    static boolean beginCampaignWar(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || source == null
                || target == null
                || source.getCapitalId() == null
                || target.getCapitalId() == null
                || source.getCapitalId().equals(
                target.getCapitalId()
        )) {
            return false;
        }

        CapitalDiplomaticState currentState =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        if (currentState
                == CapitalDiplomaticState.WAR) {
            return true;
        }

        return applyWarState(
                level,
                source,
                target,
                true
        );
    }

    private static boolean applyWarState(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            boolean beganWithAttack
    ) {
        CapitalDiplomaticState previousState =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        CapitalAgreementDataAccess
                .removeProposalsBetween(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                );

        CapitalDiplomaticTradeAgreementService.end(
                level,
                source,
                target,
                beganWithAttack
                        ? "because a military attack began."
                        : "because war was declared."
        );

        CapitalDiplomacyDataAccess
                .setDiplomaticState(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId(),
                        CapitalDiplomaticState.WAR,
                        0L
                );

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId(),
                        WAR_DECLARATION_RELATIONSHIP_CHANGE,
                        beganWithAttack
                                ? "Military attack begun"
                                : "War declared",
                        source.getCapitalId()
                );

        String sourceName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                source
                        );

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                target
                        );

        String previousAgreement =
                previousState
                        == CapitalDiplomaticState.PEACE
                        ? ""
                        : " The attack broke the existing "
                        + CapitalDiplomaticAgreementText
                        .stateDisplay(previousState)
                        + ".";

        String entry =
                beganWithAttack
                        ? sourceName
                        + " began a military attack on "
                        + targetName
                        + ", bringing the capitals to war."
                        + previousAgreement
                        : sourceName
                        + " declared war on "
                        + targetName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                source,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                target,
                entry
        );

        UUID targetDecisionMaker =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetDecisionMaker != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetDecisionMaker,
                            beganWithAttack
                                    ? "Military Attack"
                                    : "Declaration of War",
                            entry
                    );
        }

        return true;
    }
}