package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationModuleConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.data.types.MapType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ActionsConfigLegacySerializationTest
{
    @Test
    public void roundTripKeepsLegacyAnimationConfiguration()
    {
        ActionsConfig original = new ActionsConfig();
        LegacyLimbAnimationConfig head = new LegacyLimbAnimationConfig();

        original.legacyAnimations.enabled = true;
        head.lookX = true;
        head.lookY = true;
        head.wheel = true;
        head.wheelAxis = "z";
        head.wheelSpeed = 2.5F;
        head.wheelReverse = true;
        original.legacyAnimations.limbs.put("Head", head);
        original.legacyAnimationsJavascript = "var legacyAnimations = { enabled: true, limbs: {} };";

        MapType data = new MapType();
        original.toData(data);

        ActionsConfig loaded = new ActionsConfig();
        loaded.fromData(data);

        Assertions.assertEquals(original, loaded);
    }

    @Test
    public void disabledLegacyAnimationsAreNotSerialized()
    {
        ActionsConfig original = new ActionsConfig();
        LegacyLimbAnimationConfig arm = new LegacyLimbAnimationConfig();

        arm.swiping = true;
        original.legacyAnimations.enabled = false;
        original.legacyAnimations.limbs.put("right_arm", arm);

        MapType data = new MapType();
        original.toData(data);

        Assertions.assertFalse(data.has(LegacyAnimationModuleConfig.DATA_KEY));
    }
}
