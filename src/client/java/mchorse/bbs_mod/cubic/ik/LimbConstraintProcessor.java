package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.constraints.JointLimitConfig.JointLimit;
import mchorse.bbs_mod.cubic.constraints.JointLimitEnforcer;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.ModelForm;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime entry point that compiles form-embedded limb constraints and writes
 * the solved pose onto the live model instance.
 */
public final class LimbConstraintProcessor
{
    private LimbConstraintProcessor()
    {
    }

    public static void clearCache()
    {
        LimbConstraintCompiler.clear();
    }

    public static void invalidate(String modelId)
    {
        clearCache();
    }

    public static void process(ModelInstance instance, Map<String, Vector3f> targets, Map<String, Vector3f> poles)
    {
        if (instance == null || instance.model == null)
        {
            return;
        }

        IModel model = instance.model;
        LimbConstraintCompiler.Compiled compiled = null;
        MapType map = null;

        if (instance.form instanceof ModelForm form && form.ik.get() instanceof MapType m)
        {
            map = m;
        }
        else if (instance.limbConstraints != null)
        {
            map = instance.limbConstraints;
        }

        if (map != null)
        {
            compiled = LimbConstraintCompiler.getFromData(model, map);
        }

        if (compiled == null)
        {
            return;
        }

        List<LimbConstraintCompiler.CompiledLimb> limbs = compiled.limbs();

        if (limbs == null || limbs.isEmpty())
        {
            return;
        }

        Map<String, JointLimit> boneLimits = JointLimitEnforcer.getJoints(instance);
        Map<String, LimbDynamicParams> controlOverrides = null;
        Map<String, Float> targetWeights = null;
        Map<String, Float> poleWeights = null;

        if (instance.form instanceof ModelForm form)
        {
            controlOverrides = form.limbParamOverrides;
            targetWeights = form.ikTargetWeights;
            poleWeights = form.poleTargetWeights;
        }

        SkeletonPoseWriter.apply(model, limbs, targets, poles, targetWeights, poleWeights, controlOverrides, boneLimits);
    }

    public static List<String> getControllers(ModelInstance instance)
    {
        LimbConstraintCompiler.Compiled compiled = compiledOf(instance);

        if (compiled == null || compiled.limbs() == null || compiled.limbs().isEmpty())
        {
            return Collections.emptyList();
        }

        Set<String> unique = new LinkedHashSet<>();

        for (LimbConstraintCompiler.CompiledLimb limb : compiled.limbs())
        {
            if (limb != null && limb.controllerBone() != null && !limb.controllerBone().isEmpty())
            {
                unique.add(limb.controllerBone());
            }
        }

        return unique.isEmpty() ? Collections.emptyList() : new ArrayList<>(unique);
    }

    public static List<String> getPoleControllers(ModelInstance instance)
    {
        LimbConstraintCompiler.Compiled compiled = compiledOf(instance);

        if (compiled == null || compiled.limbs() == null || compiled.limbs().isEmpty())
        {
            return Collections.emptyList();
        }

        Set<String> unique = new LinkedHashSet<>();

        for (LimbConstraintCompiler.CompiledLimb limb : compiled.limbs())
        {
            if (limb != null && limb.poleEnabled() && limb.poleBone() != null && !limb.poleBone().isEmpty())
            {
                unique.add(limb.poleBone());
            }
        }

        return unique.isEmpty() ? Collections.emptyList() : new ArrayList<>(unique);
    }

    private static LimbConstraintCompiler.Compiled compiledOf(ModelInstance instance)
    {
        if (instance == null || instance.model == null)
        {
            return null;
        }

        MapType map = null;

        if (instance.form instanceof ModelForm form && form.ik.get() instanceof MapType m)
        {
            map = m;
        }
        else if (instance.limbConstraints != null)
        {
            map = instance.limbConstraints;
        }

        if (map != null)
        {
            return LimbConstraintCompiler.getFromData(instance.model, map);
        }

        return null;
    }
}
