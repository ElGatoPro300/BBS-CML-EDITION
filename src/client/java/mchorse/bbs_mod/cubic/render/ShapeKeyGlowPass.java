package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Shape-key models cannot use the VAO + single-pass BBS shader path. Emission is applied as a
 * dedicated additive pass with {@code EmissionOnly} in model.fsh so glow is true emissive light,
 * not flat CPU-brightened paint.
 */
public final class ShapeKeyGlowPass
{
    private ShapeKeyGlowPass()
    {}

    public static boolean hasShapeKeyGlow(ModelInstance model, GlowSettings glow, Color legacyGlow)
    {
        return model != null && model.hasShapeKeys() && glow != null && glow.resolveIntensity(legacyGlow) != 0F;
    }

    /**
     * Split base + additive emission inline unless glow is deferred into the Iris paint overlay callback.
     */
    public static boolean shouldSplitBaseEmission(ModelInstance model, GlowSettings glow, Color legacyGlow, boolean glowDeferredToOverlay)
    {
        return hasShapeKeyGlow(model, glow, legacyGlow) && !glowDeferredToOverlay;
    }

    public static void beginAdditive()
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    public static void endAdditive()
    {
        RenderSystem.defaultBlendFunc();
    }
}
