package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalResidentScanner {

    private CapitalResidentScanner() {
    }

    public static Set<UUID> scanResidents(ServerLevel level, UUID capitalId) {
        Set<UUID> residents = new HashSet<>();

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital != null && capital.getVillageId() != null) {
            residents.addAll(MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId()));
            return residents;
        }

        Iterable<Entity> allEntities = level.getEntities().getAll();
        for (Entity entity : allEntities) {
            UUID entityId = entity.getUUID();
            if (MCAIntegrationBridge.isMCAVillager(level, entityId)) {
                residents.add(entityId);
            }
        }

        return residents;
    }
}