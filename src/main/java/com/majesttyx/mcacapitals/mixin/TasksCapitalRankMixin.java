package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerAuthorityResolver;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import fabric.net.conczin.mca.resources.Rank;
import fabric.net.conczin.mca.server.world.data.Village;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Pseudo
@Mixin(targets = "fabric.net.conczin.mca.resources.Tasks", remap = false)
public abstract class TasksCapitalRankMixin {

    @Inject(method = "getRank", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcacapitals$useCapitalAuthority(
            Village village,
            ServerPlayer player,
            CallbackInfoReturnable<Rank> cir
    ) {
        if (village == null || player == null) {
            cir.setReturnValue(Rank.PEASANT);
            return;
        }

        CapitalRecord capital = CapitalManager.getCapitalByVillageId(player.serverLevel(), village.getId());
        cir.setReturnValue(CapitalPlayerAuthorityResolver.resolveCompatibilityRank(
                player.serverLevel(),
                capital,
                player.getUUID()
        ));
    }

    @Inject(method = "getCompletedIds", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcacapitals$suppressMcaRankTasks(
            Village village,
            ServerPlayer player,
            CallbackInfoReturnable<Set<String>> cir
    ) {
        cir.setReturnValue(Set.of());
    }
}