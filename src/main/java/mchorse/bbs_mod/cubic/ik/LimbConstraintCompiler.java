package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.data.types.MapType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Compiles form-embedded limb constraints into root-to-tip bone id chains.
 */
public final class LimbConstraintCompiler
{
    private LimbConstraintCompiler()
    {
    }

    public record CompiledLimb(String tipBone, String controllerBone, boolean poleEnabled, String poleBone, float bendOffset, float flexibility, float influence, boolean orientTip, boolean extensible, List<String> chainRootToEffector)
    {
    }

    public record Compiled(List<CompiledLimb> limbs)
    {
    }

    private static final WeakHashMap<MapType, EmbeddedCompiled> EMBEDDED = new WeakHashMap<>();

    private record EmbeddedCompiled(IModel model, List<CompiledLimb> limbs)
    {
    }

    public static void clear()
    {
        EMBEDDED.clear();
    }

    public static Compiled getFromData(IModel model, MapType data)
    {
        if (model == null || data == null)
        {
            return null;
        }

        EmbeddedCompiled cached = EMBEDDED.get(data);

        if (cached != null && cached.model == model)
        {
            return new Compiled(cached.limbs);
        }

        LimbConstraintDef config = LimbConstraintSerializer.fromData(data);
        List<CompiledLimb> compiled = compile(model, config);
        EmbeddedCompiled next = new EmbeddedCompiled(model, compiled);

        EMBEDDED.put(data, next);

        return new Compiled(compiled);
    }

    private static List<CompiledLimb> compile(IModel model, LimbConstraintDef config)
    {
        if (config == null || config.limbs() == null || config.limbs().isEmpty())
        {
            return Collections.emptyList();
        }

        List<CompiledLimb> out = new ArrayList<>(config.limbs().size());

        for (LimbConstraintDef.Limb limb : config.limbs())
        {
            if (limb == null || !limb.active())
            {
                continue;
            }

            if (!model.getAllGroupKeys().contains(limb.tipBone()) || !model.getAllGroupKeys().contains(limb.controllerBone()))
            {
                continue;
            }

            List<String> chainIds = buildChainIds(model, limb.tipBone(), limb.depth());

            if (chainIds.size() < 2)
            {
                continue;
            }

            String poleBone = limb.poleBone();

            if (poleBone != null && !poleBone.isEmpty() && !model.getAllGroupKeys().contains(poleBone))
            {
                poleBone = "";
            }

            out.add(new CompiledLimb(limb.tipBone(), limb.controllerBone(), limb.poleEnabled(), poleBone, limb.bendOffset(), limb.flexibility(), limb.influence(), limb.orientTip(), limb.extensible(), chainIds));
        }

        return out;
    }

    private static List<String> buildChainIds(IModel model, String tip, int depth)
    {
        List<String> list = new ArrayList<>();
        String group = tip;

        while (group != null && !group.isEmpty())
        {
            list.add(group);

            if (depth > 0 && list.size() >= depth)
            {
                break;
            }

            String parent = model.getParentGroupKey(group);

            if (parent == null || parent.equals(group))
            {
                break;
            }

            group = parent;
        }

        Collections.reverse(list);

        return list;
    }
}
