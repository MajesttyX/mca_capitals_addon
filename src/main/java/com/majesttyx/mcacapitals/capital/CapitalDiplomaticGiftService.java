package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class CapitalDiplomaticGiftService {

    public static final String DIALOGUE_COMMAND =
            "mcacapitals_send_gift";

    private CapitalDiplomaticGiftService() {
    }

    public static boolean openDestinationList(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        return CapitalDiplomaticGiftMenuService.openDestinationList(
                player,
                ambassadorEntity
        );
    }

    public static int dispatch(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        return CapitalDiplomaticGiftDispatchService.dispatch(
                player,
                ambassadorId,
                targetCapitalId
        );
    }

    public static boolean canShowDialogueAnswer(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        return CapitalDiplomaticGiftValidation.validateAudience(
                player,
                ambassadorEntity,
                true
        ).valid();
    }
}