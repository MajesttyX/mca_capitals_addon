package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "fabric.net.mca.resources.data.dialogue.Question", remap = false)
public abstract class DialogueCrownAuthorityAnswerMixin {

    private static final String PETITION_ANSWER = "mcacapitals_petition";
    private static final String REQUEST_ANSWER = "mcacapitals_request";
    private static final String SEIZE_THRONE_ANSWER = "mcacapitals_seize_throne";

    @Inject(
            method = "getValidAnswers",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void mcacapitals$filterCrownAuthorityAnswers(
            ServerPlayer player,
            @Coerce Object villagerObj,
            CallbackInfoReturnable<List<String>> cir
    ) {
        List<String> currentAnswers = cir.getReturnValue();
        if (currentAnswers == null || currentAnswers.isEmpty()) {
            return;
        }

        boolean hasCrownAnswer = false;
        for (String answer : currentAnswers) {
            if (isCrownAuthorityAnswer(answer)) {
                hasCrownAnswer = true;
                break;
            }
        }

        if (!hasCrownAnswer) {
            return;
        }

        if (player == null || !(villagerObj instanceof Entity villager)) {
            List<String> filtered = new ArrayList<>(currentAnswers);
            removeCrownAuthorityAnswers(filtered);
            cir.setReturnValue(filtered);
            return;
        }

        if (hasCrownAuthority(player.serverLevel(), villager.getUUID())) {
            return;
        }

        List<String> filtered = new ArrayList<>(currentAnswers);
        removeCrownAuthorityAnswers(filtered);
        cir.setReturnValue(filtered);
    }

    private static boolean isCrownAuthorityAnswer(String answer) {
        return answer != null
                && (answer.equals(PETITION_ANSWER)
                || answer.equals(REQUEST_ANSWER)
                || answer.equals(SEIZE_THRONE_ANSWER)
                || answer.startsWith(PETITION_ANSWER + "_")
                || answer.startsWith(REQUEST_ANSWER + "_")
                || answer.startsWith(SEIZE_THRONE_ANSWER + "_"));
    }

    private static void removeCrownAuthorityAnswers(List<String> answers) {
        answers.removeIf(DialogueCrownAuthorityAnswerMixin::isCrownAuthorityAnswer);
    }

    private static boolean hasCrownAuthority(ServerLevel level, UUID villagerId) {
        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return true;
        }

        return villagerId.equals(capital.getHand());
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }
}