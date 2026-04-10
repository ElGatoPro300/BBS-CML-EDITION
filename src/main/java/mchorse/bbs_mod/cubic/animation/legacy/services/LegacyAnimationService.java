package mchorse.bbs_mod.cubic.animation.legacy.services;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.model.LegacyAnimationContext;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyAnimationRouteRegistry;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyLimbRole;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;

public class LegacyAnimationService
{
    private final LegacyAnimationRouteRegistry routes;
    private final LegacyModelLimbService modelService;
    private final LegacyBOBJLimbService bobjService;

    public LegacyAnimationService(LegacyAnimationRouteRegistry routes, LegacyModelLimbService modelService, LegacyBOBJLimbService bobjService)
    {
        this.routes = routes;
        this.modelService = modelService;
        this.bobjService = bobjService;
    }

    public void apply(IModel model, LegacyAnimationsConfig config, LegacyAnimationContext context)
    {
        for (ModelGroup group : model.getAllGroups())
        {
            LegacyLimbAnimationConfig limb = config.limbs.get(group.id);

            if (limb == null)
            {
                continue;
            }

            LegacyLimbRole role = this.routes.resolve(group.id);

            this.modelService.apply(group, role, limb, context);
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            LegacyLimbAnimationConfig limb = config.limbs.get(bone.name);

            if (limb == null)
            {
                continue;
            }

            LegacyLimbRole role = this.routes.resolve(bone.name);

            this.bobjService.apply(bone, role, limb, context);
        }
    }
}
