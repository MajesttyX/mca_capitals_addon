package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PlayerCapitalTitleSavedData;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleRecord;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalDeathTransitionService {

    private CapitalDeathTransitionService() {
    }

    public static void handleVillagerDeath(ServerLevel level, UUID deadId) {
        if (level == null || deadId == null) {
            return;
        }

        boolean changedAny = false;

        PendingVillagerBetrothalAccess.removeVillager(level, deadId);

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            boolean changed = false;

            if (CapitalAmbassadorService.isAmbassador(level, capital, deadId)) {
                CapitalAmbassadorService.clearCapital(level, capital.getCapitalId());
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            }

            for (UUID holder : Set.copyOf(capital.getPrinceConsortSources().keySet())) {
                UUID source = capital.getPrinceConsortSource(holder);
                if (!deadId.equals(source)) {
                    continue;
                }

                boolean holderFemale = capital.isPrinceConsortFemale(holder);
                capital.removePrinceConsortSource(holder);
                capital.setDowagerPrinceSource(holder, deadId, holderFemale);
                changed = true;
            }

            for (UUID holder : Set.copyOf(capital.getMarriageDukeSources().keySet())) {
                UUID source = capital.getMarriageDukeSource(holder);
                if (!deadId.equals(source)) {
                    continue;
                }

                boolean holderFemale = capital.isMarriageDukeFemale(holder);
                capital.removeMarriageDukeSource(holder);
                capital.setDowagerDukeSource(holder, deadId, holderFemale);
                changed = true;
            }

            List<PlayerCapitalTitleRecord> playerRecords = new ArrayList<>(
                    PlayerCapitalTitleSavedData.get(level).getRecords().values()
            );

            for (PlayerCapitalTitleRecord record : playerRecords) {
                if (record == null) {
                    continue;
                }
                if (!capital.getCapitalId().equals(record.getCapitalId())) {
                    continue;
                }
                if (!deadId.equals(record.getMarriageSourceSpouseId())) {
                    continue;
                }

                PlayerCapitalTitleService.transitionMarriageToDowager(
                        level,
                        capital,
                        record.getPlayerId(),
                        deadId
                );
                changed = true;
            }

            if (changed) {
                changedAny = true;
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            }
        }

        PlayerCapitalTitleService.clearMarriageTitlesFromDeadSpouse(level, deadId);

        if (changedAny) {
            CapitalDataAccess.markDirty(level);
        }
    }
}