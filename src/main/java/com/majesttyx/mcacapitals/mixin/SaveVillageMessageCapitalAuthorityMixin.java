package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerAuthorityResolver;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.conczin.mca.network.c2s.SaveVillageMessage;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.conczin.mca.network.c2s.SaveVillageMessage", remap = false)
public abstract class SaveVillageMessageCapitalAuthorityMixin {

    @Inject(method = "handleServer", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$enforceCapitalAuthority(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();

        if (player == null) {
            return;
        }

        SaveVillageMessage message = (SaveVillageMessage) (Object) this;
        VillageManager.get(player.serverLevel())
                .getOrEmpty(message.id())
                .ifPresent(village -> mcacapitals$applyAuthorizedChanges(player, message, village));
    }

    private static void mcacapitals$applyAuthorizedChanges(
            ServerPlayer player,
            SaveVillageMessage message,
            Village village
    ) {
        CapitalRecord capital = CapitalManager.getCapitalByVillageId(village.getId());
        CapitalPlayerAuthorityResolver.ResolvedAuthority authority =
                CapitalPlayerAuthorityResolver.resolve(player.serverLevel(), capital, player.getUUID());

        if (Float.compare(village.getTaxes(), message.taxes()) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_TAXES)) {
            village.setTaxes(message.taxes());
        }

        if (Float.compare(village.getPopulationThreshold(), message.populationThreshold()) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_BIRTH_POLICY)) {
            village.setPopulationThreshold(message.populationThreshold());
        }

        if (Float.compare(village.getMarriageThreshold(), message.marriageThreshold()) != 0
                && authority.has(CapitalPlayerAuthorityResolver.Permission.CHANGE_MARRIAGE_POLICY)) {
            village.setMarriageThreshold(message.marriageThreshold());
        }
    }
}