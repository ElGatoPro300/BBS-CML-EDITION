package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSSettings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TrueTypeFont;
import net.minecraft.util.Identifier;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;

/**
 * Loads a user-selected TrueType (.ttf) font at runtime using LWJGL STB bindings
 * and exposes it as a Minecraft {@link TextRenderer} for 1.20.4 compatibility.
 */
public class CustomFontManager
{
    private static final Identifier FONT_ID = Identifier.of("bbs", "custom_ui_font");

    private static TextRenderer customRenderer;

    private static FontStorage fontStorage;

    private static String attemptedPath;

    private static float attemptedSize;

    public static float getFontScale()
    {
        return BBSSettings.uiFontSize == null ? 1F : BBSSettings.uiFontSize.get();
    }

    private static float getFontPointSize()
    {
        return 11F * getFontScale();
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
        float size = getFontScale();

        if (BBSSettings.uiFont == null || BBSSettings.uiFont.get().trim().isEmpty())
        {
            if (attemptedPath != null && attemptedPath.isEmpty() && attemptedSize == size && customRenderer == null)
            {
                return;
            }
        }

        applyPath(BBSSettings.uiFont == null ? "" : BBSSettings.uiFont.get(), size);
    }

    public static void invalidate()
    {
        attemptedPath = null;
        attemptedSize = -1F;
    }

    private static void applyPath(String path, float size)
    {
        String normalized = path == null ? "" : path.trim();

        if (normalized.equals(attemptedPath) && size == attemptedSize)
        {
            return;
        }

        attemptedPath = normalized;
        attemptedSize = size;

        if (normalized.isEmpty())
        {
            disposeFont();
            customRenderer = null;

            return;
        }

        File file = new File(normalized);

        if (!file.isFile())
        {
            disposeFont();
            customRenderer = null;

            return;
        }

        ByteBuffer buffer = null;
        STBTTFontinfo info = null;
        boolean ownedByFont = false;

        try
        {
            byte[] bytes = Files.readAllBytes(file.toPath());

            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            info = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(info, buffer))
            {
                throw new IOException("Failed to initialize TrueType font via STB Truetype");
            }

            TrueTypeFont font = new TrueTypeFont(buffer, info, getFontPointSize(), 2F, 0F, 0F, "");

            ownedByFont = true;

            FontStorage storage = new FontStorage(MinecraftClient.getInstance().getTextureManager(), FONT_ID);

            storage.setFonts(List.of(font));

            TextRenderer renderer = new TextRenderer((id) -> storage, false);

            disposeFont();

            fontStorage = storage;
            customRenderer = renderer;
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            if (info != null && !ownedByFont)
            {
                info.free();
            }

            if (buffer != null && !ownedByFont)
            {
                MemoryUtil.memFree(buffer);
            }

            disposeFont();
            customRenderer = null;
        }
    }

    private static void disposeFont()
    {
        if (fontStorage != null)
        {
            try
            {
                fontStorage.close();
            }
            catch (Exception e)
            {}

            fontStorage = null;
        }
    }
}
