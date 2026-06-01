package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.conczin.mca.resources.data.dialogue.Question", remap = false)
public abstract class DialogueCrownAuthorityAnswerMixin {

    private static final String PETITION_ANSWER = "mcacapitals_petition";
    private static final String REQUEST_ANSWER = "mcacapitals_request";
    private static final String SEIZE_THRONE_ANSWER = "mcacapitals_seize_throne";

    @Inject(
            method = "getValidAnswers(Lnet/minecraft/server/level/ServerPlayer;Lnet/conczin/mca/entity/VillagerEntityMCA;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$filterPetitionAnswersByCrownAuthority(
            ServerPlayer player,
            VillagerEntityMCA villager,
            CallbackInfoReturnable<List<String>> cir
    ) {
        List<String> currentAnswers = cir.getReturnValue();
        if (currentAnswers == null || currentAnswers.isEmpty()) {
            return;
        }

        if (!currentAnswers.contains(PETITION_ANSWER)
                && !currentAnswers.contains(REQUEST_ANSWER)
                && !currentAnswers.contains(SEIZE_THRONE_ANSWER)) {
            return;
        }

        if (player == null || villager == null) {
            cir.setReturnValue(removeCrownAuthorityAnswers(currentAnswers));
            return;
        }

        if (hasCrownAuthority(player.serverLevel(), villager.getUUID())) {
            return;
        }

        cir.setReturnValue(removeCrownAuthorityAnswers(currentAnswers));
    }

    private static List<String> removeCrownAuthorityAnswers(List<String> currentAnswers) {
        List<String> filtered = new ArrayList<>(currentAnswers);
        filtered.remove(PETITION_ANSWER);
        filtered.remove(REQUEST_ANSWER);
        filtered.remove(SEIZE_THRONE_ANSWER);
        return filtered;
    }

    private static boolean hasCrownAuthority(ServerLevel level, UUID villagerId) {
        if (level == null || villagerId == null) {
            return false;
        }

        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, villagerId);
        if (capital == null) {
            Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
            capital = CapitalManager.getCapitalByVillageId(villageId);
        }

        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        return villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getHand());
    }
}