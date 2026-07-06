package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSSettings;
import net.minecraft.client.font.TextRenderer;

/**
 * Stub implementation of CustomFontManager for Minecraft 1.20.4.
 * Runtime TrueType/FreeType font loading is not supported in this version.
 */
public class CustomFontManager
{
    private static final TextRenderer customRenderer = null;

    public static float getFontScale()
    {
        return BBSSettings.uiFontSize == null ? 1F : BBSSettings.uiFontSize.get();
    }

    public static TextRenderer getCustomRenderer()
    {
        return customRenderer;
    }

    public static boolean hasCustomFont()
    {
        return customRenderer != null;
    }

    public static void ensureLoaded()
    {
        // No-op for 1.20.4
    }

    public static void invalidate()
    {
        // No-op for 1.20.4
    }
}
