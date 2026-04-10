package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.validation.LegacyAnimationValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacyAnimationValidatorTest
{
    @Test
    public void removesInvalidAndEmptyLimbEntries()
    {
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig rightArm = new LegacyLimbAnimationConfig();

        config.enabled = true;
        rightArm.swinging = true;
        rightArm.swiping = true;
        rightArm.wheel = true;
        rightArm.wheelAxis = "invalid";
        rightArm.wheelSpeed = 240F;
        config.limbs.put("right_arm", rightArm);
        config.limbs.put("", new LegacyLimbAnimationConfig());

        LegacyAnimationValidator validator = new LegacyAnimationValidator();
        LegacyAnimationsConfig sanitized = validator.sanitize(config);

        Assertions.assertEquals(1, sanitized.limbs.size());
        Assertions.assertTrue(sanitized.limbs.containsKey("right_arm"));
        Assertions.assertEquals("x", sanitized.limbs.get("right_arm").wheelAxis);
        Assertions.assertEquals(100F, sanitized.limbs.get("right_arm").wheelSpeed);
    }

    @Test
    public void rejectsInvalidWheelAxisBeforeSanitization()
    {
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig rightArm = new LegacyLimbAnimationConfig();

        rightArm.swiping = true;
        rightArm.wheel = true;
        rightArm.wheelAxis = "pitch";
        rightArm.wheelSpeed = 1F;
        config.enabled = true;
        config.limbs.put("right_arm", rightArm);

        LegacyAnimationValidator validator = new LegacyAnimationValidator();
        Assertions.assertFalse(validator.validate(config).isEmpty());
    }

    @Test
    public void generatedJavascriptIsBalancedAndStable()
    {
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig head = new LegacyLimbAnimationConfig();

        config.enabled = true;
        head.swinging = true;
        head.lookX = true;
        head.lookY = true;
        head.invert = true;
        config.limbs.put("head", head);

        LegacyAnimationValidator validator = new LegacyAnimationValidator();
        String js = validator.toJavascript(config);

        Assertions.assertTrue(js.contains("legacyAnimations"));
        Assertions.assertTrue(js.contains("swinging: true"));
        Assertions.assertTrue(js.contains("lookX: true"));
        Assertions.assertTrue(validator.isValidJavascript(js));
    }
}
