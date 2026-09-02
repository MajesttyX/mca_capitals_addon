package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticGiftService;
import com.majesttyx.mcacapitals.capital.CapitalForeignAffairsService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerWarrantDialogueService;
import com.majesttyx.mcacapitals.capital.CapitalSovereignDeclarationPromptService;
import com.majesttyx.mcacapitals.dialogue.CapitalPetitionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(
        targets =
                "net.conczin.mca.entity.interaction.VillagerCommandHandler",
        remap = false
)
public class VillagerCommandHandlerMixin {

    @Inject(
            method = "handle",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$handleCustomCommand(
            ServerPlayer player,
            String command,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (command == null
                || !command.startsWith(
                "mcacapitals_"
        )) {
            return;
        }

        Entity entity =
                resolveEntity();

        MCACapitals.LOGGER.info(
                "[MCACapitals] VillagerCommandHandler.handle intercepted. command='{}', entity='{}', player='{}'",
                command,
                entity != null
                        ? entity.getName()
                        .getString()
                        : "null",
                player != null
                        ? player.getName()
                        .getString()
                        : "null"
        );

        if (entity == null
                || player == null) {
            return;
        }

        boolean handled;

        if (CapitalSovereignDeclarationPromptService
                .handleCommand(
                        player,
                        entity,
                        command
                )) {

            handled = true;

        } else if (
                CapitalPlayerWarrantDialogueService
                        .handleCommand(
                                player,
                                entity,
                                command
                        )
        ) {

            handled = true;

        } else if (
                CapitalCrownJusticeService
                        .DIALOGUE_COMMAND
                        .equals(command)
        ) {

            handled =
                    CapitalCrownJusticeService
                            .openReview(
                                    player,
                                    entity.getUUID()
                            ) > 0;

        } else if (
                CapitalForeignAffairsService
                        .DIALOGUE_COMMAND
                        .equals(command)
        ) {

            handled =
                    CapitalForeignAffairsService
                            .showReport(
                                    player,
                                    entity
                            );

        } else if (
                CapitalDiplomaticGiftService
                        .DIALOGUE_COMMAND
                        .equals(command)
        ) {

            handled =
                    CapitalDiplomaticGiftService
                            .openDestinationList(
                                    player,
                                    entity
                            );

        } else if (
                CapitalDiplomaticAgreementService
                        .DIALOGUE_COMMAND
                        .equals(command)
        ) {

            handled =
                    CapitalDiplomaticAgreementService
                            .openCapitalList(
                                    player,
                                    entity
                            );

        } else {

            handled =
                    CapitalPetitionService
                            .handleCustomCommand(
                                    player,
                                    entity,
                                    command
                            );
        }

        MCACapitals.LOGGER.info(
                "[MCACapitals] Custom villager command handled={}",
                handled
        );

        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private Entity resolveEntity() {
        Class<?> type =
                this.getClass();

        while (type != null) {
            try {
                Field field =
                        type.getDeclaredField(
                                "entity"
                        );

                field.setAccessible(
                        true
                );

                Object value =
                        field.get(this);

                if (value instanceof Entity entity) {
                    return entity;
                }

            } catch (NoSuchFieldException ignored) {

                type =
                        type.getSuperclass();

                continue;

            } catch (Throwable ignored) {

                return null;
            }

            type =
                    type.getSuperclass();
        }

        return null;
    }
}
