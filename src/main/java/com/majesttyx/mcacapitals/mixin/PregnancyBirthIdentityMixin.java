package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.BirthIdentityService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "forge.net.mca.entity.ai.Pregnancy", remap = false)
public abstract class PregnancyBirthIdentityMixin {

    @Inject(
            method = "createChild(Lforge/net/mca/entity/ai/relationship/Gender;Lforge/net/mca/entity/VillagerEntityMCA;)Lforge/net/mca/entity/VillagerEntityMCA;",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mcacapitals$assignBirthIdentity(
            @Coerce Object gender,
            @Coerce Object partnerObj,
            CallbackInfoReturnable<Object> cir
    ) {
        Object childObj = cir.getReturnValue();

        if (!(childObj instanceof Entity child)) {
            return;
        }

        if (!(partnerObj instanceof Entity partner)) {
            return;
        }

        Entity mother = resolveMother();
        if (mother == null) {
            return;
        }

        if (!(mother.level() instanceof ServerLevel level)) {
            return;
        }

        BirthIdentityService.applyBirthIdentity(level, child, mother, partner);
    }

    private Entity resolveMother() {
        Class<?> current = this.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField("mother");
                field.setAccessible(true);
                Object value = field.get(this);
                return value instanceof Entity entity ? entity : null;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }
}