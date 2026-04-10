package mchorse.bbs_mod.cubic.animation.legacy.services;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.model.LegacyAnimationContext;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyLimbRole;
import mchorse.bbs_mod.cubic.animation.legacy.utils.LegacyAnimationMath;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.WeakHashMap;

public class LegacyBOBJLimbService
{
    private final WeakHashMap<BOBJBone, WheelState> wheelStates = new WeakHashMap<>();

    public void apply(BOBJBone bone, LegacyLimbRole role, LegacyLimbAnimationConfig config, LegacyAnimationContext context)
    {
        float direction = config.invert ? -1F : 1F;

        if (config.lookX && role == LegacyLimbRole.HEAD)
        {
            bone.transform.rotate.x += MathUtils.toRad(-context.pitch * direction);
        }

        if (config.lookY)
        {
            bone.transform.rotate.y += MathUtils.toRad(-context.yaw * direction);
        }

        if (config.idle)
        {
            float armDirection = role == LegacyLimbRole.LEFT_ARM ? -1F : 1F;
            float idleFactor = config.invert ? -1F : 1F;

            bone.transform.rotate.z += LegacyAnimationMath.idleRoll(context.age, armDirection) * idleFactor;
            bone.transform.rotate.x += LegacyAnimationMath.idlePitch(context.age, armDirection) * idleFactor;
        }

        if (config.swiping && context.handSwing > 0F)
        {
            float progress = config.invert ? 1F - context.handSwing : context.handSwing;

            bone.transform.rotate.x += LegacyAnimationMath.swipePitch(progress) * direction;
            bone.transform.rotate.z += LegacyAnimationMath.swipeRoll(progress) * direction;
        }

        if (config.swinging && (role == LegacyLimbRole.RIGHT_ARM || role == LegacyLimbRole.LEFT_ARM))
        {
            boolean left = role == LegacyLimbRole.LEFT_ARM;
            float swing = LegacyAnimationMath.swingPitch(context.limbPhase, context.limbSpeed, context.movementCoefficient, left) * direction;

            bone.transform.rotate.x += swing;
        }

        if (config.wheel)
        {
            float wheelDirection = config.wheelReverse ? -1F : 1F;
            WheelState state = this.wheelStates.computeIfAbsent(bone, key -> new WheelState());
            state.direction = LegacyAnimationMath.wheelDirection(context.forwardSpeed, state.direction);
            float linearSpeed = LegacyAnimationMath.wheelLinearSpeed(context.forwardSpeed, context.horizontalSpeed, state.direction);
            float targetVelocity = LegacyAnimationMath.wheelTargetAngularSpeed(linearSpeed, config.wheelSpeed) * direction * wheelDirection;
            float lerpFactor = LegacyAnimationMath.wheelLerpFactor(context.horizontalSpeed);
            float angularVelocity = LegacyAnimationMath.wheelRotation(state.angularVelocity, targetVelocity, lerpFactor);
            float rotation = angularVelocity;

            if ("y".equals(config.wheelAxis))
            {
                bone.transform.rotate.y += rotation;
            }
            else if ("z".equals(config.wheelAxis))
            {
                bone.transform.rotate.z += rotation;
            }
            else
            {
                bone.transform.rotate.x += rotation;
            }

            state.angularVelocity = angularVelocity;
        }
        else
        {
            this.wheelStates.remove(bone);
        }
    }

    private static class WheelState
    {
        private float angularVelocity;
        private float direction = 1F;
    }
}
