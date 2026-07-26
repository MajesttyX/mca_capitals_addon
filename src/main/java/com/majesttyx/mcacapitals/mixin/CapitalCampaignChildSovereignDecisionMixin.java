package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignCourtDecisionService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "com.majesttyx.mcacapitals.capital.CapitalCampaignBattleService",
        remap = false
)
public abstract class CapitalCampaignChildSovereignDecisionMixin {

    @Inject(
            method = "resolveDefendingSovereignDecision",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$letCourtSpeakForChildSovereign(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            CallbackInfo ci
    ) {
        if (CapitalCampaignCourtDecisionService
                .resolveIfChildSovereign(
                        level,
                        campaign,
                        attackingCapital,
                        defendingCapital
                )) {
            ci.cancel();
        }
    }
}