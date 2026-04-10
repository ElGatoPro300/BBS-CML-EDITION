package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyAnimationRouteRegistry;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyLimbRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacyAnimationRouteRegistryTest
{
    @Test
    public void resolvesKnownLimbIds()
    {
        LegacyAnimationRouteRegistry registry = new LegacyAnimationRouteRegistry();

        Assertions.assertEquals(LegacyLimbRole.HEAD, registry.resolve("head"));
        Assertions.assertEquals(LegacyLimbRole.RIGHT_ARM, registry.resolve("right_arm"));
        Assertions.assertEquals(LegacyLimbRole.LEFT_ARM, registry.resolve("left_arm"));
        Assertions.assertEquals(LegacyLimbRole.RIGHT_LEG, registry.resolve("right_leg"));
        Assertions.assertEquals(LegacyLimbRole.LEFT_LEG, registry.resolve("left_leg"));
        Assertions.assertEquals(LegacyLimbRole.OTHER, registry.resolve("cape"));
    }

    @Test
    public void resolvesUnknownAsOther()
    {
        LegacyAnimationRouteRegistry registry = new LegacyAnimationRouteRegistry();

        Assertions.assertEquals(LegacyLimbRole.OTHER, registry.resolve("tail"));
    }
}
