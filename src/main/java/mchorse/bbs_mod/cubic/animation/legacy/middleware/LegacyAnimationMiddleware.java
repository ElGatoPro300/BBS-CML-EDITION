package mchorse.bbs_mod.cubic.animation.legacy.middleware;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.forms.entities.IEntity;

public interface LegacyAnimationMiddleware
{
    public boolean allow(IEntity entity, LegacyAnimationsConfig config);
}
