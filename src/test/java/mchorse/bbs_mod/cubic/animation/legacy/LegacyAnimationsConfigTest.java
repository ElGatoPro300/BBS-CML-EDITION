package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.data.types.MapType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacyAnimationsConfigTest
{
    @Test
    public void serializesAndDeserializesLimbConfig()
    {
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig rightArm = new LegacyLimbAnimationConfig();

        config.enabled = true;
        rightArm.swinging = true;
        rightArm.swiping = true;
        rightArm.idle = true;
        rightArm.wheel = true;
        rightArm.wheelAxis = "y";
        rightArm.wheelSpeed = 4F;
        rightArm.wheelReverse = true;
        config.limbs.put("right_arm", rightArm);

        MapType data = config.toData();
        LegacyAnimationsConfig loaded = new LegacyAnimationsConfig();

        loaded.fromData(data);

        Assertions.assertEquals(config, loaded);
    }
}
