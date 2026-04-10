package mchorse.bbs_mod.cubic.animation.legacy.middleware;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.forms.entities.IEntity;

public class LegacyAnimationEnabledMiddleware implements LegacyAnimationMiddleware
{
    @Override
    public boolean allow(IEntity entity, LegacyAnimationsConfig config)
    {
        return entity != null && config != null && config.enabled && !config.limbs.isEmpty();
    }
}
