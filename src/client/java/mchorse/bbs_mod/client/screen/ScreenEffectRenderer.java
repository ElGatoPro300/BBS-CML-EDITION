package mchorse.bbs_mod.client.screen;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.camera.clips.screen.GrainClip;
import mchorse.bbs_mod.camera.clips.screen.GrainEffect;
import mchorse.bbs_mod.camera.clips.screen.LetterboxClip;
import mchorse.bbs_mod.camera.clips.screen.LetterboxEffect;
import mchorse.bbs_mod.camera.clips.screen.ScreenNodeEffect;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

public class ScreenEffectRenderer
{
    public static void render(Batcher2D batcher, ClipContext context, int screenW, int screenH)
    {
        List<ColorEffect> effects = ColorClip.getEffects(context);
        List<LetterboxEffect> letterboxEffects = LetterboxClip.getEffects(context);
        List<GrainEffect> grainEffects = GrainClip.getEffects(context);

        /* Convert ScreenNodeEffect entries into the standard effect structs */
        List<ScreenNodeEffect> nodeEffects = ScreenNodeEffect.getEffects(context);

        for (ScreenNodeEffect ne : nodeEffects)
        {
            ColorEffect ce = new ColorEffect();

            if (ne.brightness != 0F || ne.contrast != 0F || ne.saturation != 0F)
            {
                ce.hasGrade   = true;
                ce.brightness = ne.brightness;
                ce.contrast   = ne.contrast;
                ce.saturation = ne.saturation;
            }

            if (ne.vignetteStrength > 0F)
            {
                ce.hasVignette        = true;
                ce.vignetteStrength   = ne.vignetteStrength;
                ce.vignetteSmoothness = ne.vignetteSmoothness;
                ce.vignetteColor      = ne.vignetteColor;
            }

            if (ne.overlayAlpha > 0F)
            {
                ce.hasOverlay   = true;
                ce.overlayColor = Colors.setA(ne.overlayColor, ne.overlayAlpha);
            }

            if (ne.distortX != 0F || ne.distortY != 0F)
            {
                ce.hasDistort = true;
                ce.distortX   = ne.distortX;
                ce.distortY   = ne.distortY;
            }

            if (ce.hasGrade || ce.hasVignette || ce.hasOverlay || ce.hasDistort)
            {
                effects.add(ce);
            }

            if (ne.grainStrength > 0F)
            {
                GrainEffect ge = new GrainEffect();
                ge.strength = ne.grainStrength;
                ge.size     = ne.grainSize;
                grainEffects.add(ge);
            }

            if (ne.letterboxSize > 0F)
            {
                LetterboxEffect le = new LetterboxEffect();
                le.size  = ne.letterboxSize;
                le.color = ne.letterboxColor;
                letterboxEffects.add(le);
            }
        }

        nodeEffects.clear();

        /* Overlay color pass */
        for (ColorEffect effect : effects)
        {
            if (effect.hasOverlay)
            {
                batcher.box(0, 0, screenW, screenH, effect.overlayColor);
            }
        }

        /* Vignette, color grade and film grain via shader pass */
        ColorGradeRenderer.apply(effects, grainEffects);

        /* Letterbox bars */
        for (LetterboxEffect effect : letterboxEffects)
        {
            renderLetterbox(batcher, effect, screenW, screenH);
        }

        /* Clear all effect lists to prevent accumulation across frames */
        effects.clear();
        letterboxEffects.clear();
        grainEffects.clear();
    }

    private static void renderLetterbox(Batcher2D batcher, LetterboxEffect effect, int screenW, int screenH)
    {
        int barH = (int)(screenH * effect.size);

        if (barH <= 0)
        {
            return;
        }

        float zoom = effect.zoom <= 0F ? 1F : effect.zoom;
        boolean transformed = effect.rotation != 0F || zoom != 1F || effect.offsetX != 0F || effect.offsetY != 0F;

        if (transformed)
        {
            MatrixStack stack = batcher.getContext().getMatrices();

            stack.push();
            stack.translate(effect.offsetX * screenW, effect.offsetY * screenH, 0F);
            stack.translate(screenW / 2F, screenH / 2F, 0F);
            stack.multiply(RotationAxis.POSITIVE_Z.rotation(MathUtils.toRad(effect.rotation)));
            stack.scale(zoom, zoom, 1F);
            stack.translate(-screenW / 2F, -screenH / 2F, 0F);
            renderLetterboxBars(batcher, effect, screenW, screenH, barH);
            stack.pop();
        }
        else
        {
            renderLetterboxBars(batcher, effect, screenW, screenH, barH);
        }
    }

    private static void renderLetterboxBars(Batcher2D batcher, LetterboxEffect effect, int screenW, int screenH, int barH)
    {
        int color = effect.color;
        int smoothH = (int)(screenH * effect.smoothness);
        float barWidthFactor = effect.width <= 0F ? 1F : effect.width;
        int barW = Math.max(1, Math.round(screenW * barWidthFactor));
        int barX = (screenW - barW) / 2;

        if (smoothH > 0 && smoothH < barH)
        {
            int solidH = barH - smoothH;
            int transparent = Colors.setA(color, 0F);

            /* Top bar: solid part + inner gradient */
            batcher.box(barX, 0, barX + barW, solidH, color);
            batcher.gradientVBox(barX, solidH, barX + barW, barH, color, transparent);

            /* Bottom bar: inner gradient + solid part */
            batcher.gradientVBox(barX, screenH - barH, barX + barW, screenH - solidH, transparent, color);
            batcher.box(barX, screenH - solidH, barX + barW, screenH, color);
        }
        else
        {
            /* Hard-edge bars */
            batcher.box(barX, 0, barX + barW, barH, color);
            batcher.box(barX, screenH - barH, barX + barW, screenH, color);
        }
    }
}
