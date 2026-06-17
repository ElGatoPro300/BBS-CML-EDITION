package mchorse.bbs_mod.cubic.physics;

public class PhysBoneState
{
    public float yaw;
    public float pitch;
    public float yawVelocity;
    public float pitchVelocity;
    public float prevStrafeMotion;
    public float prevForwardMotion;
    public float prevVerticalVelocity;
    public float prevParentYaw;
    public float prevParentPitch;
    public boolean parentInitialized;
    public boolean initialized;
}
