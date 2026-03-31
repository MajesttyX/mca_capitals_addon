package com.example.mcacapitals.mixin;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.dialogue.CapitalPetitionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "forge.net.mca.entity.interaction.EntityCommandHandler", remap = false)
public class VillagerCommandHandlerMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$handlePetitionCommand(ServerPlayer player, String command, CallbackInfoReturnable<Boolean> cir) {
        if (command == null || !command.startsWith("mcacapitals_")) {
            return;
        }

        Entity entity = resolveEntity();
        MCACapitals.LOGGER.info(
                "[MCACapitals] EntityCommandHandler.handle intercepted. command='{}', entity='{}', player='{}'",
                command,
                entity != null ? entity.getName().getString() : "null",
                player != null ? player.getName().getString() : "null"
        );

        if (entity == null || player == null) {
            return;
        }

        boolean handled = CapitalPetitionService.handleCustomCommand(player, entity, command);
        MCACapitals.LOGGER.info("[MCACapitals] Petition command handled={}", handled);

        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private Entity resolveEntity() {
        Class<?> type = this.getClass();

        while (type != null) {
            try {
                Field field = type.getDeclaredField("entity");
                field.setAccessible(true);
                Object value = field.get(this);
                if (value instanceof Entity entity) {
                    return entity;
                }
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
                continue;
            } catch (Throwable ignored) {
                return null;
            }

            type = type.getSuperclass();
        }

        return null;
    }
}