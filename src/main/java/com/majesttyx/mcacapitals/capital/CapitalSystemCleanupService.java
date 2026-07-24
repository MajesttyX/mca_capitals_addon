package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalInterregnumDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class CapitalSystemCleanupService {

    private CapitalSystemCleanupService() {
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return false;
        }

        CapitalRecord capital =
                CapitalManager.getCapital(capitalId);

        boolean changed = capital != null;

        if (capital != null) {
            clearPlayerTitles(level, capital);
        }

        changed |= CapitalDiplomacyDataAccess
                .removeCapital(
                        level,
                        capitalId
                );

        changed |= CapitalAgreementDataAccess
                .removeCapital(
                        level,
                        capitalId
                );

        changed |= CapitalCampaignDataAccess
                .removeCapital(
                        level,
                        capitalId
                );

        changed |= CapitalInterregnumDataAccess
                .remove(
                        level,
                        capitalId
                );

        changed |= CapitalJusticeDataAccess
                .removeCapital(
                        level,
                        capitalId
                );

        changed |= CapitalRefugeeDataAccess
                .removeCapital(
                        level,
                        capitalId
                );

        CapitalAmbassadorService.clearCapital(
                level,
                capitalId
        );

        CapitalCourtWatcher.clearFingerprint(
                capitalId
        );

        CapitalManager.removeCapital(capitalId);

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    private static void clearPlayerTitles(
            ServerLevel level,
            CapitalRecord capital
    ) {
        UUID playerSovereignId =
                capital.getPlayerSovereignId();

        if (playerSovereignId != null) {
            PlayerCapitalTitleService.clear(
                    level,
                    playerSovereignId,
                    capital.getCapitalId()
            );
        }

        UUID playerConsortId =
                capital.getPlayerConsortId();

        if (playerConsortId != null) {
            PlayerCapitalTitleService.clear(
                    level,
                    playerConsortId,
                    capital.getCapitalId()
            );
        }
    }
}