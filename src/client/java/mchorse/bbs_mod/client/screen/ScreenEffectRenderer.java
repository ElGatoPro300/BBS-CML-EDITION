package mchorse.bbs_mod.client.screen;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

public class ScreenEffectRenderer
{
    public static void render(Batcher2D batcher, ClipContext context, int screenW, int screenH)
    {
        List<ColorEffect> effects = ColorClip.getEffects(context);

        for (ColorEffect effect : effects)
        {
            if (effect.hasOverlay)
            {
                batcher.box(0, 0, screenW, screenH, effect.overlayColor);
            }

            if (effect.hasVignette && effect.vignetteStrength > 0F)
            {
                float gradW = screenW * 0.5F * effect.vignetteSmoothness;
                float gradH = screenH * 0.5F * effect.vignetteSmoothness;

                int opaqueColor = Colors.setA(effect.vignetteColor, effect.vignetteStrength);
                int transparent = Colors.setA(effect.vignetteColor, 0F);

                batcher.gradientHBox(0, 0, gradW, screenH, opaqueColor, transparent);
                batcher.gradientHBox(screenW - gradW, 0, screenW, screenH, transparent, opaqueColor);
                batcher.gradientVBox(0, 0, screenW, gradH, opaqueColor, transparent);
                batcher.gradientVBox(0, screenH - gradH, screenW, screenH, transparent, opaqueColor);
            }
        }
    }
}
