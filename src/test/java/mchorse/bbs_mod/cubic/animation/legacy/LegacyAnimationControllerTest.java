package mchorse.bbs_mod.cubic.animation.legacy;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.controllers.LegacyAnimationController;
import mchorse.bbs_mod.cubic.animation.legacy.model.LegacyAnimationContext;
import mchorse.bbs_mod.cubic.animation.legacy.routes.LegacyAnimationRouteRegistry;
import mchorse.bbs_mod.cubic.animation.legacy.services.LegacyAnimationService;
import mchorse.bbs_mod.cubic.animation.legacy.services.LegacyBOBJLimbService;
import mchorse.bbs_mod.cubic.animation.legacy.services.LegacyModelLimbService;
import mchorse.bbs_mod.forms.entities.IEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

public class LegacyAnimationControllerTest
{
    @Test
    public void skipsApplicationWhenModuleDisabled()
    {
        TrackingService service = new TrackingService();
        LegacyAnimationController controller = new LegacyAnimationController(service);
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig head = new LegacyLimbAnimationConfig();

        head.lookX = true;
        config.enabled = false;
        config.limbs.put("head", head);

        controller.apply(stub(IEntity.class), stub(IModel.class), config, new LegacyAnimationContext());

        Assertions.assertEquals(0, service.applyCount);
    }

    @Test
    public void appliesWhenEnabledAndConfigured()
    {
        TrackingService service = new TrackingService();
        LegacyAnimationController controller = new LegacyAnimationController(service);
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();
        LegacyLimbAnimationConfig rightArm = new LegacyLimbAnimationConfig();

        rightArm.swiping = true;
        config.enabled = true;
        config.limbs.put("right_arm", rightArm);

        controller.apply(stub(IEntity.class), stub(IModel.class), config, new LegacyAnimationContext());

        Assertions.assertEquals(1, service.applyCount);
        Assertions.assertNotNull(service.lastConfig);
        Assertions.assertTrue(service.lastConfig.enabled);
        Assertions.assertTrue(service.lastConfig.limbs.containsKey("right_arm"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, (proxy, method, args) ->
        {
            Class<?> returnType = method.getReturnType();

            if (!returnType.isPrimitive())
            {
                return null;
            }

            if (returnType == boolean.class)
            {
                return false;
            }
            if (returnType == byte.class)
            {
                return (byte) 0;
            }
            if (returnType == short.class)
            {
                return (short) 0;
            }
            if (returnType == int.class)
            {
                return 0;
            }
            if (returnType == long.class)
            {
                return 0L;
            }
            if (returnType == float.class)
            {
                return 0F;
            }
            if (returnType == double.class)
            {
                return 0D;
            }
            if (returnType == char.class)
            {
                return '\0';
            }

            return null;
        });
    }

    private static class TrackingService extends LegacyAnimationService
    {
        private int applyCount;
        private LegacyAnimationsConfig lastConfig;

        private TrackingService()
        {
            super(new LegacyAnimationRouteRegistry(), new LegacyModelLimbService(), new LegacyBOBJLimbService());
        }

        @Override
        public void apply(IModel model, LegacyAnimationsConfig config, LegacyAnimationContext context)
        {
            this.applyCount += 1;
            this.lastConfig = config;
        }
    }
}
