package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class CapitalRankConflictService {

    private CapitalRankConflictService() {
    }

    public static boolean normalize(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null) {
            return false;
        }

        boolean changed = false;

        Set<UUID> exclusiveRanks =
                new LinkedHashSet<>();

        addIfPresent(
                exclusiveRanks,
                capital.getSovereign()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getPlayerSovereignId()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getConsort()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getPlayerConsortId()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getDowager()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getHeir()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getGrandMaester()
        );

        addIfPresent(
                exclusiveRanks,
                capital.getHerald()
        );

        addIfPresent(
                exclusiveRanks,
                CapitalAmbassadorService.getAmbassador(
                        level,
                        capital
                )
        );

        exclusiveRanks.addAll(
                capital.getRoyalChildren()
        );

        exclusiveRanks.addAll(
                capital.getLegitimizedRoyalChildren()
        );

        exclusiveRanks.addAll(
                capital.getPrinceConsortSources()
                        .keySet()
        );

        exclusiveRanks.addAll(
                capital.getDowagerPrinceSources()
                        .keySet()
        );

        exclusiveRanks.addAll(
                capital.getMarriageDukeSources()
                        .keySet()
        );

        exclusiveRanks.addAll(
                capital.getDowagerDukeSources()
                        .keySet()
        );

        for (UUID entityId : exclusiveRanks) {
            changed |= removeDirectRanks(
                    capital,
                    entityId
            );
        }

        for (UUID lord :
                new ArrayList<>(
                        capital.getLords()
                )) {
            if (MCAIntegrationBridge.isMasterClericVillager(
                    level,
                    lord
            )) {
                capital.removeLord(lord);
                changed = true;
            }
        }

        for (UUID knight :
                new ArrayList<>(
                        capital.getKnights()
                )) {
            if (MCAIntegrationBridge.isMasterClericVillager(
                    level,
                    knight
            )) {
                capital.removeKnight(knight);
                changed = true;
            }
        }

        for (UUID royalGuard :
                new ArrayList<>(
                        capital.getRoyalGuards()
                )) {
            if (capital.isDuke(royalGuard)) {
                capital.removeDuke(royalGuard);
                changed = true;
            }

            if (capital.isLord(royalGuard)) {
                capital.removeLord(royalGuard);
                changed = true;
            }
        }

        for (UUID duke :
                new ArrayList<>(
                        capital.getDukes()
                )) {
            if (capital.isLord(duke)) {
                capital.removeLord(duke);
                changed = true;
            }

            if (capital.isKnight(duke)) {
                capital.removeKnight(duke);
                changed = true;
            }
        }

        for (UUID lord :
                new ArrayList<>(
                        capital.getLords()
                )) {
            if (capital.isKnight(lord)) {
                capital.removeKnight(lord);
                changed = true;
            }
        }

        return changed;
    }

    public static boolean canReceiveDirectNobleRank(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null
                || capital == null
                || entityId == null) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getPlayerSovereignId())
                || entityId.equals(capital.getConsort())
                || entityId.equals(capital.getPlayerConsortId())
                || entityId.equals(capital.getDowager())
                || entityId.equals(capital.getHeir())
                || entityId.equals(capital.getGrandMaester())
                || entityId.equals(capital.getHerald())
                || capital.isRoyalChild(entityId)
                || capital.isLegitimizedRoyalChild(entityId)
                || capital.isPrinceConsort(entityId)
                || capital.isDowagerPrince(entityId)
                || capital.isMarriageDuke(entityId)
                || capital.isDowagerDuke(entityId)
                || capital.isRoyalGuard(entityId)
                || CapitalAmbassadorService.isAmbassador(
                level,
                capital,
                entityId
        )) {
            return false;
        }

        return true;
    }

    private static boolean removeDirectRanks(
            CapitalRecord capital,
            UUID entityId
    ) {
        if (entityId == null) {
            return false;
        }

        boolean changed = false;

        if (capital.isDuke(entityId)) {
            capital.removeDuke(entityId);
            changed = true;
        }

        if (capital.isLord(entityId)) {
            capital.removeLord(entityId);
            changed = true;
        }

        if (capital.isKnight(entityId)) {
            capital.removeKnight(entityId);
            changed = true;
        }

        return changed;
    }

    private static void addIfPresent(
            Set<UUID> values,
            UUID value
    ) {
        if (value != null) {
            values.add(value);
        }
    }
}