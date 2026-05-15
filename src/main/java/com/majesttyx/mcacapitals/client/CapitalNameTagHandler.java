package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapitalNameTagHandler {

    private CapitalNameTagHandler() {
    }
}