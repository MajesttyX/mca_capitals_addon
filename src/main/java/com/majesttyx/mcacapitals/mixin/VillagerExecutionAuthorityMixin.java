package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalExecutionAuthorityService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        targets = "forge.net.conczin.mca.entity.interaction.VillagerCommandHandler",
        remap = false
)
public abstract class VillagerExecutionAuthorityMixin {

    private static final String EXECUTE_COMMAND =
            "execute";

    @Inject(
            method = "handle(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$validateDirectExecutionAuthority(
            ServerPlayer player,
            String command,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EXECUTE_COMMAND.equals(
                rootCommand(command)
        )) {
            return;
        }

        Entity target = resolveEntity();

        if (CapitalExecutionAuthorityService
                .mayIssueDirectExecution(
                        player,
                        target
                )) {
            return;
        }

        if (player != null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.villager_execution_authority_mixin.only_the_reigning_sovereign_of_this_capital_may_order_an_execution").withStyle(ChatFormatting.RED)
            );
        }

        cir.setReturnValue(true);
    }

    private static String rootCommand(
            String command
    ) {
        if (command == null || command.isBlank()) {
            return "";
        }

        int separator = command.indexOf('.');

        return separator < 0
                ? command
                : command.substring(
                0,
                separator
        );
    }

    private Entity resolveEntity() {
        Class<?> type = this.getClass();

        while (type != null) {
            try {
                Field field =
                        type.getDeclaredField(
                                "entity"
                        );

                field.setAccessible(true);

                Object value = field.get(this);

                return value instanceof Entity entity
                        ? entity
                        : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }
}