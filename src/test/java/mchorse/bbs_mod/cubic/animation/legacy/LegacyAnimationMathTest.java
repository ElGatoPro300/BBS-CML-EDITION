package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.animation.legacy.utils.LegacyAnimationMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacyAnimationMathTest
{
    @Test
    public void swingingUsesOppositeArmPhase()
    {
        float right = LegacyAnimationMath.swingPitch(1.0F, 0.8F, 1.0F, false);
        float left = LegacyAnimationMath.swingPitch(1.0F, 0.8F, 1.0F, true);

        Assertions.assertEquals(-right, left, 0.0001F);
    }

    @Test
    public void wheelTargetAngularSpeedRespectsDirection()
    {
        float forward = LegacyAnimationMath.wheelTargetAngularSpeed(3.0F, 1.0F);
        float backward = LegacyAnimationMath.wheelTargetAngularSpeed(-3.0F, 1.0F);

        Assertions.assertTrue(forward > 0F);
        Assertions.assertTrue(backward < 0F);
    }

    @Test
    public void wheelRotationSmoothlyStopsWhenTargetIsZero()
    {
        float speed = LegacyAnimationMath.wheelRotation(0.6F, 0F, 0.5F);

        Assertions.assertTrue(speed < 0.6F);
        Assertions.assertTrue(speed > 0F);
    }

    @Test
    public void wheelLinearSpeedUsesHorizontalSpeedWhenForwardProjectionIsZero()
    {
        float positive = LegacyAnimationMath.wheelLinearSpeed(0F, 2.8F, 1F);
        float negative = LegacyAnimationMath.wheelLinearSpeed(0F, 2.8F, -1F);

        Assertions.assertEquals(2.8F, positive, 0.0001F);
        Assertions.assertEquals(-2.8F, negative, 0.0001F);
    }

    @Test
    public void wheelRotationRemainsActiveDuringContinuousMovement()
    {
        float angularSpeed = 0F;
        float target = LegacyAnimationMath.wheelTargetAngularSpeed(LegacyAnimationMath.wheelLinearSpeed(0F, 3.0F, 1F), 1F);
        float lerp = LegacyAnimationMath.wheelLerpFactor(3.0F);

        for (int i = 0; i < 40; i++)
        {
            angularSpeed = LegacyAnimationMath.wheelRotation(angularSpeed, target, lerp);
            Assertions.assertTrue(angularSpeed > 0F);
        }
    }
}
