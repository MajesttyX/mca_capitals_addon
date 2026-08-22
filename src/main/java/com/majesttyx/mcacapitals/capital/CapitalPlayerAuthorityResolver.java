package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import fabric.net.conczin.mca.resources.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class CapitalPlayerAuthorityResolver {

    private CapitalPlayerAuthorityResolver() {
    }

    public static ResolvedAuthority resolve(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getState() != CapitalState.ACTIVE) {
            return ResolvedAuthority.stranger();
        }

        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);

        NobleTitle grantedTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, playerId);
        NobleTitle marriageTitle = PlayerCapitalTitleService.getMarriageTitle(level, capital, playerId);
        NobleTitle dowagerTitle = PlayerCapitalTitleService.getDowagerBaseTitle(level, capital, playerId);

        boolean lord = capital.isLord(playerId)
                || grantedTitle == NobleTitle.LORD
                || grantedTitle == NobleTitle.LADY
                || marriageTitle == NobleTitle.LORD
                || marriageTitle == NobleTitle.LADY;

        boolean duke = capital.isDuke(playerId)
                || capital.isMarriageDuke(playerId)
                || capital.isDowagerDuke(playerId)
                || grantedTitle == NobleTitle.DUKE
                || grantedTitle == NobleTitle.DUCHESS
                || marriageTitle == NobleTitle.DUKE
                || marriageTitle == NobleTitle.DUCHESS
                || dowagerTitle == NobleTitle.DUKE
                || dowagerTitle == NobleTitle.DUCHESS;

        boolean prince = playerId.equals(capital.getHeir())
                || capital.isRoyalChild(playerId)
                || capital.isLegitimizedRoyalChild(playerId);

        boolean princeConsort = capital.isPrinceConsort(playerId)
                || capital.isDowagerPrince(playerId)
                || marriageTitle == NobleTitle.PRINCE
                || marriageTitle == NobleTitle.PRINCESS
                || dowagerTitle == NobleTitle.PRINCE
                || dowagerTitle == NobleTitle.PRINCESS;

        boolean sovereign = playerId.equals(capital.getSovereign())
                || playerId.equals(capital.getPlayerSovereignId());
        boolean highSovereign = sovereign && countPlayerSovereignties(playerId) >= 2;
        boolean sovereignConsort = playerId.equals(capital.getConsort())
                || playerId.equals(capital.getPlayerConsortId());
        boolean commander = playerId.equals(capital.getCommander())
                || PlayerCapitalTitleService.isCommander(level, capital, playerId);
        boolean hand = playerId.equals(capital.getHand())
                || PlayerCapitalTitleService.isHand(level, capital, playerId);

        if (lord) {
            permissions.add(Permission.CHANGE_TAXES);
        }

        if (duke || prince || sovereign || highSovereign || hand) {
            permissions.add(Permission.CHANGE_TAXES);
            permissions.add(Permission.CHANGE_BIRTH_POLICY);
            permissions.add(Permission.CHANGE_MARRIAGE_POLICY);
        }

        PlayerRank playerRank;
        if (highSovereign) {
            playerRank = PlayerRank.HIGH_SOVEREIGN;
        } else if (sovereign) {
            playerRank = PlayerRank.SOVEREIGN;
        } else if (sovereignConsort) {
            playerRank = PlayerRank.SOVEREIGN_CONSORT;
        } else if (prince) {
            playerRank = PlayerRank.PRINCE;
        } else if (princeConsort) {
            playerRank = PlayerRank.PRINCE_CONSORT;
        } else if (hand) {
            playerRank = PlayerRank.HAND;
        } else if (duke) {
            playerRank = PlayerRank.DUKE;
        } else if (commander) {
            playerRank = PlayerRank.LORD_COMMANDER;
        } else if (lord) {
            playerRank = PlayerRank.LORD;
        } else {
            playerRank = PlayerRank.COMMONER;
        }

        Component displayTitle = CapitalTitleResolver.getDisplayTitleComponent(level, capital, playerId);
        if (CapitalTitleResolver.getResolvedTitleId(level, capital, playerId)
                == CapitalTitleResolver.ResolvedTitleId.NONE) {
            displayTitle = playerRank.defaultDisplayTitleComponent();
        }

        return new ResolvedAuthority(
                playerRank,
                displayTitle,
                Collections.unmodifiableSet(EnumSet.copyOf(permissions)),
                capital.getCapitalId()
        );
    }

    public static Rank resolveCompatibilityRank(ServerLevel level, CapitalRecord capital, UUID playerId) {
        ResolvedAuthority authority = resolve(level, capital, playerId);
        if (authority.rank() == PlayerRank.SOVEREIGN || authority.rank() == PlayerRank.HIGH_SOVEREIGN) {
            return Rank.MONARCH;
        }
        if (authority.has(Permission.CHANGE_MARRIAGE_POLICY)) {
            return Rank.MAYOR;
        }
        if (authority.has(Permission.CHANGE_BIRTH_POLICY)) {
            return Rank.NOBLE;
        }
        if (authority.has(Permission.CHANGE_TAXES)) {
            return Rank.MERCHANT;
        }
        return Rank.PEASANT;
    }

    private static int countPlayerSovereignties(UUID playerId) {
        int count = 0;
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState() == CapitalState.ACTIVE
                    && playerId.equals(capital.getPlayerSovereignId())) {
                count++;
            }
        }
        return count;
    }

    public enum PlayerRank {
        STRANGER("mcacapitals.dynamic.rank.stranger"),
        COMMONER("mcacapitals.dynamic.rank.commoner"),
        LORD("mcacapitals.dynamic.rank.lord"),
        DUKE("mcacapitals.dynamic.rank.duke"),
        PRINCE("mcacapitals.dynamic.rank.prince"),
        PRINCE_CONSORT("mcacapitals.dynamic.rank.prince_consort"),
        SOVEREIGN("mcacapitals.dynamic.rank.sovereign"),
        SOVEREIGN_CONSORT("mcacapitals.dynamic.rank.sovereign_consort"),
        LORD_COMMANDER("mcacapitals.dynamic.rank.lord_commander"),
        HAND("mcacapitals.dynamic.rank.hand"),
        HIGH_SOVEREIGN("mcacapitals.dynamic.rank.high_sovereign");

        private final String translationKey;

        PlayerRank(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component defaultDisplayTitleComponent() {
            return Component.translatable(translationKey);
        }
    }

    public enum Permission {
        CHANGE_TAXES,
        CHANGE_BIRTH_POLICY,
        CHANGE_MARRIAGE_POLICY
    }

    public record ResolvedAuthority(
            PlayerRank rank,
            Component displayTitle,
            Set<Permission> permissions,
            UUID capitalId
    ) {
        public static ResolvedAuthority stranger() {
            return new ResolvedAuthority(
                    PlayerRank.STRANGER,
                    PlayerRank.STRANGER.defaultDisplayTitleComponent(),
                    Set.of(),
                    null
            );
        }

        public boolean has(Permission permission) {
            return permission != null && permissions.contains(permission);
        }

        public int permissionMask() {
            int mask = 0;
            for (Permission permission : permissions) {
                mask |= 1 << permission.ordinal();
            }
            return mask;
        }
    }
}