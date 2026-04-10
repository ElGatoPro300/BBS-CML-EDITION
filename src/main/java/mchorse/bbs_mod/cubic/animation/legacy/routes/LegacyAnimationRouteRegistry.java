package mchorse.bbs_mod.cubic.animation.legacy.routes;

public class LegacyAnimationRouteRegistry
{
    public LegacyLimbRole resolve(String limbId)
    {
        String key = limbId == null ? "" : limbId.toLowerCase();

        if (key.equals("head") || key.equals("helmet") || key.contains("head"))
        {
            return LegacyLimbRole.HEAD;
        }

        if (key.equals("right_arm") || key.equals("arm_right") || key.contains("rarm") || key.contains("rightarm"))
        {
            return LegacyLimbRole.RIGHT_ARM;
        }

        if (key.equals("left_arm") || key.equals("arm_left") || key.contains("larm") || key.contains("leftarm"))
        {
            return LegacyLimbRole.LEFT_ARM;
        }

        if (key.equals("right_leg") || key.equals("leg_right") || key.contains("rleg") || key.contains("rightleg"))
        {
            return LegacyLimbRole.RIGHT_LEG;
        }

        if (key.equals("left_leg") || key.equals("leg_left") || key.contains("lleg") || key.contains("leftleg"))
        {
            return LegacyLimbRole.LEFT_LEG;
        }

        if (key.equals("torso") || key.equals("body") || key.equals("chest"))
        {
            return LegacyLimbRole.TORSO;
        }

        return LegacyLimbRole.OTHER;
    }
}
