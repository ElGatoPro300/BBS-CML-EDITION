package mchorse.bbs_mod.cubic.animation.legacy.services;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.model.LegacyAnimationContext;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyLimbRole;
import mchorse.bbs_mod.cubic.animation.legacy.utils.LegacyAnimationMath;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.WeakHashMap;

public class LegacyModelLimbService
{
    private final WeakHashMap<ModelGroup, WheelState> wheelStates = new WeakHashMap<>();

    public void apply(ModelGroup group, LegacyLimbRole role, LegacyLimbAnimationConfig config, LegacyAnimationContext context)
    {
        float direction = config.invert ? -1F : 1F;

        if (config.lookX && role == LegacyLimbRole.HEAD)
        {
            group.current.rotate.x += -context.pitch * direction;
        }

        if (config.lookY)
        {
            group.current.rotate.y += -context.yaw * direction;
        }

        if (config.idle)
        {
            float armDirection = role == LegacyLimbRole.LEFT_ARM ? -1F : 1F;
            float idleFactor = config.invert ? -1F : 1F;

            group.current.rotate.z += MathUtils.toDeg(LegacyAnimationMath.idleRoll(context.age, armDirection) * idleFactor);
            group.current.rotate.x += MathUtils.toDeg(LegacyAnimationMath.idlePitch(context.age, armDirection) * idleFactor);
        }

        if (config.swiping && context.handSwing > 0F)
        {
            float progress = config.invert ? 1F - context.handSwing : context.handSwing;

            group.current.rotate.x += MathUtils.toDeg(LegacyAnimationMath.swipePitch(progress) * direction);
            group.current.rotate.z += MathUtils.toDeg(LegacyAnimationMath.swipeRoll(progress) * direction);
        }

        if (config.swinging && (role == LegacyLimbRole.RIGHT_ARM || role == LegacyLimbRole.LEFT_ARM))
        {
            boolean left = role == LegacyLimbRole.LEFT_ARM;
            float swing = LegacyAnimationMath.swingPitch(context.limbPhase, context.limbSpeed, context.movementCoefficient, left) * direction;

            group.current.rotate.x += MathUtils.toDeg(swing);
        }

        if (config.wheel)
        {
            float wheelDirection = config.wheelReverse ? -1F : 1F;
            WheelState state = this.wheelStates.computeIfAbsent(group, key -> new WheelState());
            state.direction = LegacyAnimationMath.wheelDirection(context.forwardSpeed, state.direction);
            float linearSpeed = LegacyAnimationMath.wheelLinearSpeed(context.forwardSpeed, context.horizontalSpeed, state.direction);
            float targetVelocity = LegacyAnimationMath.wheelTargetAngularSpeed(linearSpeed, config.wheelSpeed) * direction * wheelDirection;
            float lerpFactor = LegacyAnimationMath.wheelLerpFactor(context.horizontalSpeed);
            float angularVelocity = LegacyAnimationMath.wheelRotation(state.angularVelocity, targetVelocity, lerpFactor);
            float rotation = MathUtils.toDeg(angularVelocity);

            if ("y".equals(config.wheelAxis))
            {
                group.current.rotate.y += rotation;
            }
            else if ("z".equals(config.wheelAxis))
            {
                group.current.rotate.z += rotation;
            }
            else
            {
                group.current.rotate.x += rotation;
            }

            state.angularVelocity = angularVelocity;
        }
        else
        {
            this.wheelStates.remove(group);
        }
    }

    private static class WheelState
    {
        private float angularVelocity;
        private float direction = 1F;
    }
}
