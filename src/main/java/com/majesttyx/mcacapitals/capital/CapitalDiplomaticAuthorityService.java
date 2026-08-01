package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

final class CapitalDiplomaticAuthorityService {

    private CapitalDiplomaticAuthorityService() {
    }

    static boolean maySendGift(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        return mayExerciseSovereignAuthority(
                level,
                capital,
                playerId
        )
                || hasNaturalLordship(
                level,
                capital,
                playerId
        )
                || hasNaturalDukedom(
                level,
                capital,
                playerId
        );
    }

    static boolean mayManageForeignRelations(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        return mayExerciseSovereignAuthority(
                level,
                capital,
                playerId
        );
    }

    static boolean mayExerciseSovereignAuthority(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        if (level == null
                || capital == null
                || playerId == null) {
            return false;
        }

        UUID playerSovereignId =
                capital.getPlayerSovereignId();

        if (playerSovereignId != null) {
            return playerId.equals(
                    playerSovereignId
            );
        }

        return capital.getSovereign() != null
                && isCurrentPlayerHand(
                level,
                capital,
                playerId
        );
    }

    static UUID getPlayerDecisionMaker(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null || capital == null) {
            return null;
        }

        UUID playerSovereignId =
                capital.getPlayerSovereignId();

        if (playerSovereignId != null) {
            return playerSovereignId;
        }

        if (capital.getSovereign() == null) {
            return null;
        }

        UUID playerHandId =
                PlayerCapitalTitleService
                        .getHandHolder(
                                level,
                                capital
                        );

        if (playerHandId == null
                || !playerHandId.equals(
                capital.getHand()
        )
                || !PlayerCapitalTitleService
                .isHand(
                        level,
                        capital,
                        playerHandId
                )) {
            return null;
        }

        return playerHandId;
    }

    private static boolean isCurrentPlayerHand(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        return playerId.equals(
                capital.getHand()
        )
                && PlayerCapitalTitleService.isHand(
                level,
                capital,
                playerId
        );
    }

    private static boolean hasNaturalLordship(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        if (level == null
                || capital == null
                || playerId == null) {
            return false;
        }

        NobleTitle grantedTitle =
                PlayerCapitalTitleService
                        .getGrantedTitle(
                                level,
                                capital,
                                playerId
                        );

        return capital.isLord(playerId)
                || grantedTitle == NobleTitle.LORD
                || grantedTitle == NobleTitle.LADY;
    }

    private static boolean hasNaturalDukedom(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        if (level == null
                || capital == null
                || playerId == null) {
            return false;
        }

        NobleTitle grantedTitle =
                PlayerCapitalTitleService
                        .getGrantedTitle(
                                level,
                                capital,
                                playerId
                        );

        return capital.isDuke(playerId)
                || grantedTitle == NobleTitle.DUKE
                || grantedTitle == NobleTitle.DUCHESS;
    }
}