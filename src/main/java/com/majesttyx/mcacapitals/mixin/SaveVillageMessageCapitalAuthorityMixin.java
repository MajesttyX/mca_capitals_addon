package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerAuthorityResolver;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import forge.net.conczin.mca.server.world.data.Village;
import forge.net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "forge.net.conczin.mca.network.c2s.SaveVillageMessage", remap = false)
public abstract class SaveVillageMessageCapitalAuthorityMixin {

    @Shadow(remap = false)
    @Final
    private int id;

    @Shadow(remap = false)
    @Final
    private float taxes;

    @Shadow(remap = false)
    @Final
    private float populationThreshold;

    @Shadow(remap = false)
    @Final
    private float marriageThreshold;

    @Inject(
            method = "receive(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void mcacapitals$enforceCapitalAuthority(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();

        if (player == null) {
            return;
        }

        VillageManager.get(player.serverLevel())
                .getOrEmpty(id)
                .ifPresent(village -> mcacapitals$applyAuthorizedChanges(player, village));
    }

    private void mcacapitals$applyAuthorizedChanges(
            ServerPlayer player,
            Village village
    ) {
        CapitalRecord capital = CapitalManager.getCapitalByVillageId(village.getId());
        CapitalPlayerAuthorityResolver.ResolvedAuthority authority =
                CapitalPlayerAuthorityResolver.resolve(player.serverLevel(), capital, player.getUUID());

        if (Float.compare(village.getTaxes(), taxes) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_TAXES)) {
            village.setTaxes(taxes);
        }

        if (Float.compare(village.getPopulationThreshold(), populationThreshold) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_BIRTH_POLICY)) {
            village.setPopulationThreshold(populationThreshold);
        }

        if (Float.compare(village.getMarriageThreshold(), marriageThreshold) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_MARRIAGE_POLICY)) {
            village.setMarriageThreshold(marriageThreshold);
        }

        village.markDirty();
    }
}