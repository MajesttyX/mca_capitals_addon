package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.identity.HouseWordsLocalization;

import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.network.SyncVillagerIdentityPacket;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillagerIdentityClientCache {

    private static final Map<UUID, ClientVillagerIdentity> IDENTITIES = new HashMap<>();

    private VillagerIdentityClientCache() {
    }

    public static void put(SyncVillagerIdentityPacket packet) {
        if (packet == null || packet.villagerId() == null) {
            return;
        }

        IDENTITIES.put(packet.villagerId(), new ClientVillagerIdentity(
                packet.villagerId(),
                packet.originVillageName(),
                packet.originSource(),
                packet.currentSurname(),
                packet.baseName(),
                packet.displayTitleId(),
                packet.displayTitle(),
                packet.royalGuardOrderLine(),
                packet.courtOfficeId(),
                packet.courtOfficeLine(),
                packet.houseFounded(),
                packet.houseName(),
                packet.houseWords(),
                packet.houseWordsPersonality()
        ));
    }

    public static ClientVillagerIdentity get(UUID villagerId) {
        if (villagerId == null) {
            return null;
        }
        return IDENTITIES.get(villagerId);
    }

    public static void clear(UUID villagerId) {
        if (villagerId == null) {
            return;
        }
        IDENTITIES.remove(villagerId);
    }

    public record ClientVillagerIdentity(
            UUID villagerId,
            Component originVillageName,
            String originSource,
            String currentSurname,
            String baseName,
            String displayTitleId,
            Component displayTitle,
            Component royalGuardOrderLine,
            String courtOfficeId,
            Component courtOfficeLine,
            boolean houseFounded,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        public boolean hasOrigin() {
            return originVillageName != null && !originVillageName.getString().isBlank();
        }

        public boolean hasSurname() {
            return currentSurname != null && !currentSurname.isBlank();
        }

        public boolean hasFoundedHouse() {
            return houseFounded && houseName != null && !houseName.isBlank();
        }

        public boolean hasHouseWords() {
            return houseWords != null && !houseWords.isBlank();
        }

        public boolean hasTitle() {
            CapitalTitleResolver.ResolvedTitleId titleId = resolvedTitleId();
            return titleId != CapitalTitleResolver.ResolvedTitleId.NONE
                    && titleId != CapitalTitleResolver.ResolvedTitleId.COMMONER;
        }

        public boolean isRoyalGuardOrder() {
            return royalGuardOrderLine != null
                    && !royalGuardOrderLine.getString().isBlank();
        }

        public boolean hasCourtOfficeLine() {
            return courtOfficeLine != null
                    && !courtOfficeLine.getString().isBlank();
        }

        public CapitalTitleResolver.ResolvedTitleId resolvedTitleId() {
            if (displayTitleId == null || displayTitleId.isBlank()) {
                return CapitalTitleResolver.ResolvedTitleId.NONE;
            }

            try {
                return CapitalTitleResolver.ResolvedTitleId.valueOf(displayTitleId);
            } catch (IllegalArgumentException ignored) {
                return CapitalTitleResolver.ResolvedTitleId.NONE;
            }
        }

        public CapitalTitleResolver.SecondaryOfficeId resolvedCourtOfficeId() {
            if (courtOfficeId == null || courtOfficeId.isBlank()) {
                return CapitalTitleResolver.SecondaryOfficeId.NONE;
            }

            try {
                return CapitalTitleResolver.SecondaryOfficeId.valueOf(courtOfficeId);
            } catch (IllegalArgumentException ignored) {
                return CapitalTitleResolver.SecondaryOfficeId.NONE;
            }
        }

        public Component originDisplayLine() {
            if (!hasOrigin()) {
                return Component.empty();
            }

            Component originName = originVillageName;
            String source = originSource == null ? "" : originSource;
            return switch (source) {
                case "BIRTH" -> Component.translatable(
                        "mcacapitals.system.identity.origin.born_of",
                        originName
                );
                case "INN_SETTLED" -> Component.translatable(
                        "mcacapitals.system.identity.origin.sworn_to",
                        originName
                );
                case "DISCOVERED", "LEGACY_BACKFILL" -> Component.translatable(
                        "mcacapitals.system.identity.origin.native_of",
                        originName
                );
                case "DEBUG" -> Component.translatable(
                        "mcacapitals.system.identity.origin.generic",
                        originName
                );
                default -> Component.translatable(
                        "mcacapitals.system.identity.origin.generic",
                        originName
                );
            };
        }

        public Component surnameOrHouseDisplayLine() {
            if (hasFoundedHouse()) {
                return Component.translatable(
                        "mcacapitals.system.identity.house",
                        Component.literal(houseName)
                );
            }

            if (hasSurname()) {
                return Component.translatable(
                        "mcacapitals.system.identity.surname",
                        Component.literal(currentSurname)
                );
            }

            return Component.empty();
        }

        public Component houseWordsDisplayLine() {
            if (!hasHouseWords()) {
                return Component.empty();
            }
            return Component.translatable(
                    "mcacapitals.system.identity.house_words",
                    HouseWordsLocalization.displayComponent(houseWords)
            );
        }
    }
}
