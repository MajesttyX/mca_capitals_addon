package com.majesttyx.mcacapitals.ai;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

public class RoyalGuardFollowGoal extends Goal {

    private static final double FOLLOW_START_DISTANCE = 12.0D;
    private static final double MAX_IDLE_DISTANCE = 18.0D;
    private static final double WALK_FOLLOW_SPEED = 0.7D;
    private static final double FAR_FOLLOW_SPEED = 0.7D;

    private final PathfinderMob guard;
    private final UUID capitalId;

    private Entity sovereign;

    public RoyalGuardFollowGoal(PathfinderMob guard, UUID capitalId) {
        this.guard = guard;
        this.capitalId = capitalId;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || !capital.isRoyalGuard(guard.getUUID())) {
            return false;
        }

        if (capital.getRoyalGuardDutyMode(guard.getUUID()) == CapitalRecord.GuardDutyMode.PATROL_ANCHOR) {
            return false;
        }

        if (capital.getSovereign() == null || guard.level().isClientSide) {
            return false;
        }

        if (!(guard.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Entity target = MCAIntegrationBridge.getEntityByUuid(serverLevel, capital.getSovereign());
        if (target == null || !target.isAlive()) {
            return false;
        }

        this.sovereign = target;
        return guard.distanceToSqr(target) > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || !capital.isRoyalGuard(guard.getUUID())) {
            return false;
        }

        if (capital.getRoyalGuardDutyMode(guard.getUUID()) == CapitalRecord.GuardDutyMode.PATROL_ANCHOR) {
            return false;
        }

        return sovereign != null
                && sovereign.isAlive()
                && guard.distanceToSqr(sovereign) > 9.0D;
    }

    @Override
    public void start() {
        moveTowardSovereign();
    }

    @Override
    public void tick() {
        moveTowardSovereign();
    }

    @Override
    public void stop() {
        sovereign = null;
        guard.getNavigation().stop();
        guard.setSprinting(false);
    }

    private void moveTowardSovereign() {
        if (sovereign == null) {
            return;
        }

        guard.setSprinting(false);

        double distance = guard.distanceToSqr(sovereign);
        double speed = distance > MAX_IDLE_DISTANCE * MAX_IDLE_DISTANCE
                ? FAR_FOLLOW_SPEED
                : WALK_FOLLOW_SPEED;

        guard.getNavigation().moveTo(sovereign, speed);
        guard.getLookControl().setLookAt(sovereign, 30.0F, 30.0F);
        guard.setSprinting(false);
    }
}