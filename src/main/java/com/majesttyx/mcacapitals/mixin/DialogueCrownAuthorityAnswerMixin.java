package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticGiftService;
import com.majesttyx.mcacapitals.capital.CapitalForeignAffairsService;
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
@Mixin(targets = "net.conczin.mca.resources.data.dialogue.Question", remap = false)
public abstract class DialogueCrownAuthorityAnswerMixin {

    private static final String PETITION_ANSWER = "mcacapitals_petition";
    private static final String REQUEST_ANSWER = "mcacapitals_request";
    private static final String SEIZE_THRONE_ANSWER = "mcacapitals_seize_throne";
    private static final String MANAGE_DIPLOMACY_ANSWER =
            "mcacapitals_manage_diplomacy";
    private static final String ASK_FOREIGN_AFFAIRS_ANSWER =
            "mcacapitals_ask_foreign_affairs";
    private static final String SEND_GIFT_ANSWER =
            "mcacapitals_send_gift";

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

        boolean hasManagedAnswer = false;
        for (String answer : currentAnswers) {
            if (isManagedAnswer(answer)) {
                hasManagedAnswer = true;
                break;
            }
        }
        if (!hasManagedAnswer) {
            return;
        }

        List<String> filtered = new ArrayList<>(currentAnswers);
        if (player == null || !(villagerObj instanceof Entity villager)) {
            removeManagedAnswers(filtered);
            cir.setReturnValue(filtered);
            return;
        }

        if (!hasCrownAuthority(player.serverLevel(), villager.getUUID())) {
            removeAnswerFamily(filtered, PETITION_ANSWER);
            removeAnswerFamily(filtered, REQUEST_ANSWER);
            removeAnswerFamily(filtered, SEIZE_THRONE_ANSWER);
        }

        if (!CapitalDiplomaticAgreementService.canShowDialogueAnswer(
                player,
                villager
        )) {
            removeAnswerFamily(filtered, MANAGE_DIPLOMACY_ANSWER);
        }

        if (!CapitalForeignAffairsService.canShowDialogueAnswer(
                player,
                villager
        )) {
            removeAnswerFamily(filtered, ASK_FOREIGN_AFFAIRS_ANSWER);
        }

        if (!CapitalDiplomaticGiftService.canShowDialogueAnswer(
                player,
                villager
        )) {
            removeAnswerFamily(filtered, SEND_GIFT_ANSWER);
        }

        cir.setReturnValue(filtered);
    }

    private static boolean isManagedAnswer(String answer) {
        return belongsToFamily(answer, PETITION_ANSWER)
                || belongsToFamily(answer, REQUEST_ANSWER)
                || belongsToFamily(answer, SEIZE_THRONE_ANSWER)
                || belongsToFamily(answer, MANAGE_DIPLOMACY_ANSWER)
                || belongsToFamily(answer, ASK_FOREIGN_AFFAIRS_ANSWER)
                || belongsToFamily(answer, SEND_GIFT_ANSWER);
    }

    private static boolean belongsToFamily(
            String answer,
            String baseAnswer
    ) {
        return answer != null
                && (answer.equals(baseAnswer)
                || answer.startsWith(baseAnswer + "_"));
    }

    private static void removeManagedAnswers(List<String> answers) {
        answers.removeIf(DialogueCrownAuthorityAnswerMixin::isManagedAnswer);
    }

    private static void removeAnswerFamily(
            List<String> answers,
            String baseAnswer
    ) {
        answers.removeIf(answer -> belongsToFamily(answer, baseAnswer));
    }

    private static boolean hasCrownAuthority(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return true;
        }
        return villagerId.equals(capital.getHand());
    }

    private static CapitalRecord resolveCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(
                level,
                villagerId
        );
        return CapitalManager.getCapitalByVillageId(villageId);
    }
}
