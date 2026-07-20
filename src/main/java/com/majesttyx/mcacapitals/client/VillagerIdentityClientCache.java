package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.network.SyncVillagerIdentityPacket;

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
                packet.displayTitle(),
                packet.royalGuardOrderLine(),
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
            String originVillageName,
            String originSource,
            String currentSurname,
            String displayTitle,
            String royalGuardOrderLine,
            String courtOfficeLine,
            boolean houseFounded,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        public boolean hasOrigin() {
            return originVillageName != null && !originVillageName.isBlank();
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
            return displayTitle != null
                    && !displayTitle.isBlank()
                    && !"None".equals(displayTitle)
                    && !"Commoner".equals(displayTitle);
        }

        public boolean isRoyalGuardOrder() {
            return royalGuardOrderLine != null && !royalGuardOrderLine.isBlank();
        }

        public boolean hasCourtOfficeLine() {
            return courtOfficeLine != null && !courtOfficeLine.isBlank();
        }

        public String originDisplayLine() {
            if (!hasOrigin()) {
                return "";
            }

            String source = originSource == null ? "" : originSource;
            return switch (source) {
                case "BIRTH" -> "Born of " + originVillageName;
                case "INN_SETTLED" -> "Sworn to " + originVillageName;
                case "DISCOVERED", "LEGACY_BACKFILL" -> "Native of " + originVillageName;
                case "DEBUG" -> "Origin: " + originVillageName;
                default -> "Origin: " + originVillageName;
            };
        }

        public String surnameOrHouseDisplayLine() {
            if (hasFoundedHouse()) {
                return "House " + houseName;
            }

            if (hasSurname()) {
                return "Surname: " + currentSurname;
            }

            return "";
        }

        public String houseWordsDisplayLine() {
            if (!hasHouseWords()) {
                return "";
            }
            return "House Words: " + houseWords;
        }
    }
}