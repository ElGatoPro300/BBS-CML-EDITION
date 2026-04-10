package mchorse.bbs_mod.cubic.animation.legacy.controllers;

import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.middleware.LegacyAnimationEnabledMiddleware;
import mchorse.bbs_mod.cubic.animation.legacy.middleware.LegacyAnimationMiddleware;
import mchorse.bbs_mod.cubic.animation.legacy.model.LegacyAnimationContext;
import mchorse.bbs_mod.cubic.animation.legacy.services.LegacyAnimationService;
import mchorse.bbs_mod.cubic.animation.legacy.validation.LegacyAnimationValidator;
import mchorse.bbs_mod.forms.entities.IEntity;
import org.slf4j.Logger;

import java.util.Objects;

public class LegacyAnimationController
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LegacyAnimationMiddleware middleware;
    private final LegacyAnimationValidator validator;
    private final LegacyAnimationService service;
    private String lastDecision;

    public LegacyAnimationController(LegacyAnimationService service)
    {
        this(new LegacyAnimationEnabledMiddleware(), new LegacyAnimationValidator(), service);
    }

    public LegacyAnimationController(LegacyAnimationMiddleware middleware, LegacyAnimationValidator validator, LegacyAnimationService service)
    {
        this.middleware = middleware;
        this.validator = validator;
        this.service = service;
    }

    public void apply(IEntity entity, IModel model, LegacyAnimationsConfig sourceConfig, LegacyAnimationContext context)
    {
        LegacyAnimationsConfig config = this.validator.sanitize(sourceConfig);

        if (entity == null)
        {
            this.logDecision("Legacy animation skipped: entity is null");
            return;
        }

        if (model == null)
        {
            this.logDecision("Legacy animation skipped: model is null");
            return;
        }

        if (config == null)
        {
            this.logDecision("Legacy animation skipped: config is null");
            return;
        }

        if (!config.enabled)
        {
            this.logDecision("Legacy animation skipped: module disabled");
            return;
        }

        if (config.limbs.isEmpty())
        {
            this.logDecision("Legacy animation skipped: no limb configurations");
            return;
        }

        if (!this.middleware.allow(entity, config))
        {
            this.logDecision("Legacy animation skipped: middleware denied state");
            return;
        }

        this.logDecision("Legacy animation applied: configuredLimbs=" + config.limbs.size());
        this.service.apply(model, config, context);
    }

    private void logDecision(String decision)
    {
        if (!Objects.equals(this.lastDecision, decision))
        {
            this.lastDecision = decision;
            LOGGER.debug(decision);
        }
    }
}
