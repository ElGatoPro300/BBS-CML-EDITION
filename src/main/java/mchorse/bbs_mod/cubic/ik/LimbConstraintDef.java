package mchorse.bbs_mod.cubic.ik;

import java.util.List;

/**
 * Static limb IK constraint definitions keyed by tip bone on the model form.
 */
public record LimbConstraintDef(List<Limb> limbs)
{
    public static final float DEFAULT_INFLUENCE = 1F;
    public static final String DEFAULT_POLE_BONE = "";
    public static final float DEFAULT_BEND_OFFSET = 0F;
    public static final float DEFAULT_FLEXIBILITY = 0.05F;
    public static final int DEFAULT_DEPTH = 0;
    public static final boolean DEFAULT_ORIENT_TIP = false;
    public static final boolean DEFAULT_EXTENSIBLE = false;

    /**
     * One IK constraint living on {@code tipBone}, reaching {@code controllerBone},
     * spanning {@code depth} bones up the hierarchy ({@code 0} = to the root).
     */
    public record Limb(String tipBone, String controllerBone, int depth, boolean poleEnabled, String poleBone, float bendOffset, float flexibility, float influence, boolean active, boolean orientTip, boolean extensible)
    {
        public Limb
        {
            tipBone = tipBone == null ? "" : tipBone;
            controllerBone = controllerBone == null ? "" : controllerBone;
            poleBone = poleBone == null ? "" : poleBone;
            depth = Math.max(0, depth);
            flexibility = clamp01(flexibility);
            influence = clamp01(influence);
        }

        private static float clamp01(float value)
        {
            if (value < 0F)
            {
                return 0F;
            }

            return Math.min(value, 1F);
        }
    }
}
