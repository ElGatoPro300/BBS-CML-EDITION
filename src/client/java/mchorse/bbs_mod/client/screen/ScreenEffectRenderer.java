package mchorse.bbs_mod.client.screen;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.camera.clips.screen.GrainClip;
import mchorse.bbs_mod.camera.clips.screen.GrainEffect;
import mchorse.bbs_mod.camera.clips.screen.LetterboxClip;
import mchorse.bbs_mod.camera.clips.screen.LetterboxEffect;
import mchorse.bbs_mod.camera.clips.screen.ScreenNodeEffect;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

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

        int color = effect.color;
        int smoothH = (int)(screenH * effect.smoothness);

        if (smoothH > 0 && smoothH < barH)
        {
            int solidH = barH - smoothH;
            int transparent = Colors.setA(color, 0F);

            /* Top bar: solid part + inner gradient */
            batcher.box(0, 0, screenW, solidH, color);
            batcher.gradientVBox(0, solidH, screenW, barH, color, transparent);

            /* Bottom bar: inner gradient + solid part */
            batcher.gradientVBox(0, screenH - barH, screenW, screenH - solidH, transparent, color);
            batcher.box(0, screenH - solidH, screenW, screenH, color);
        }
        else
        {
            /* Hard-edge bars */
            batcher.box(0, 0, screenW, barH, color);
            batcher.box(0, screenH - barH, screenW, screenH, color);
        }
    }
}
