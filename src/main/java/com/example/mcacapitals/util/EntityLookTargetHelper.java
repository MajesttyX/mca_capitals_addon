package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class EntityLookTargetHelper {

    private EntityLookTargetHelper() {
    }

    public static Entity getLookedAtEntity(ServerPlayer player, double reach) {
        if (player == null || reach <= 0.0D) {
            return null;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(reach));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(reach))
                .inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                target -> !target.isSpectator() && target.isPickable(),
                reach * reach
        );

        return hit != null ? hit.getEntity() : null;
    }
}