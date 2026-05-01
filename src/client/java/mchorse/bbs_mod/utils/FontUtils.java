package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FontUtils
{
    public static final File FONTS_FOLDER = BBSMod.getAssetsPath("fonts");
    private static final Map<String, TextureFont> fonts = new HashMap<>();

    public static void createFontsFolder()
    {
        if (!FONTS_FOLDER.exists())
        {
            FONTS_FOLDER.mkdirs();
        }
    }

    public static TextureFont getFont(String name)
    {
        if (fonts.containsKey(name))
        {
            return fonts.get(name);
        }
        
        File[] files = FONTS_FOLDER.listFiles((dir, f) -> f.startsWith(name + ".") && (f.endsWith(".ttf") || f.endsWith(".otf")));
        
        if (files != null && files.length > 0)
        {
            TextureFont font = new TextureFont(files[0]);
            fonts.put(name, font);
            return font;
        }
        
        return null;
    }

    public static List<String> getAvailableFonts()
    {
        createFontsFolder();
        
        File[] files = FONTS_FOLDER.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
        
        if (files == null)
        {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
            .map(File::getName)
            .map(name -> name.substring(0, name.lastIndexOf('.')))
            .collect(Collectors.toList());
    }
}