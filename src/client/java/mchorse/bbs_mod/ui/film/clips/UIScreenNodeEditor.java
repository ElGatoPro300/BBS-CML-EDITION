package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.nodes.BrightnessContrastNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ColorGradeEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.DistortionEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LayerNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GammaCorrectionNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GlitchNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GrainEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.HueSaturationNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LetterboxEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LevelsNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.OverlayBlendNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.OverlayEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.PosterizeNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenBlendNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenOutputNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenUVNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.SineWaveNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.SquareWaveNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.VignetteEffectNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.forms.editors.panels.shape.UIShapeNodeEditor;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Node editor for ScreenNodeGraph. Extends UIShapeNodeEditor to reuse all shared
 * rendering and interaction logic, overriding only what is specific to screen nodes.
 */
public class UIScreenNodeEditor extends UIShapeNodeEditor
{
    protected static final int HEADER_SCREEN_OUTPUT = Colors.A100 | 0xFF4400;
    protected static final int HEADER_WAVE          = Colors.A100 | 0x2277EE;
    protected static final int HEADER_BLEND         = Colors.A100 | 0xAA44CC;
    protected static final int HEADER_GRADE         = Colors.A100 | 0xFF9922;
    protected static final int HEADER_GLITCH        = Colors.A100 | 0xFF1166;
    protected static final int HEADER_EFFECT        = Colors.A100 | 0x226644;

    @Override
    protected String getNodeTitle(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode)   return "Screen Output";
        if (node instanceof ScreenUVNode)       return "Screen UV";
        if (node instanceof ColorGradeEffectNode) return "Color Grade";
        if (node instanceof LayerNode)          return "Layer";
        if (node instanceof DistortionEffectNode) return "Distortion";
        if (node instanceof VignetteEffectNode) return "Vignette";
        if (node instanceof GrainEffectNode)    return "Grain";
        if (node instanceof LetterboxEffectNode) return "Letterbox";
        if (node instanceof OverlayEffectNode)  return "Overlay";
        if (node instanceof SineWaveNode)     return "Sine Wave";
        if (node instanceof SquareWaveNode)   return "Square Wave";
        if (node instanceof ScreenBlendNode)  return "Screen Blend";
        if (node instanceof OverlayBlendNode) return "Overlay Blend";
        if (node instanceof GammaCorrectionNode)  return "Gamma Correction";
        if (node instanceof HueSaturationNode)    return "Hue / Saturation";
        if (node instanceof BrightnessContrastNode) return "Brightness / Contrast";
        if (node instanceof LevelsNode)       return "Levels";
        if (node instanceof GlitchNode)       return "Glitch";
        if (node instanceof PosterizeNode)
        {
            return ((PosterizeNode) node).mode == 1 ? "Posterize (color)" : "Posterize";
        }

        return super.getNodeTitle(node);
    }

    @Override
    protected int getNodeHeaderColor(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode)    return HEADER_SCREEN_OUTPUT;
        if (node instanceof ScreenUVNode)        return HEADER_INPUT;
        if (node instanceof VignetteEffectNode || node instanceof GrainEffectNode
            || node instanceof LetterboxEffectNode || node instanceof OverlayEffectNode
            || node instanceof DistortionEffectNode || node instanceof ColorGradeEffectNode) return HEADER_EFFECT;
        if (node instanceof LayerNode) return HEADER_DEFAULT;
        if (node instanceof SineWaveNode || node instanceof SquareWaveNode) return HEADER_WAVE;
        if (node instanceof ScreenBlendNode || node instanceof OverlayBlendNode) return HEADER_BLEND;
        if (node instanceof GammaCorrectionNode || node instanceof HueSaturationNode
            || node instanceof BrightnessContrastNode || node instanceof LevelsNode) return HEADER_GRADE;
        if (node instanceof GlitchNode)    return HEADER_GLITCH;
        if (node instanceof PosterizeNode) return HEADER_MATH;

        return super.getNodeHeaderColor(node);
    }

    @Override
    protected int getNodeWidth(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode) return 160;

        return super.getNodeWidth(node);
    }

    @Override
    protected void appendNodeContextMenu(UIContext context, ShapeNode node, ContextMenuManager menu)
    {
        if (node instanceof PosterizeNode)
        {
            PosterizeNode p = (PosterizeNode) node;

            menu.action(Icons.GEAR, IKey.raw("Mode: Scalar"), p.mode == 0 ? Colors.ACTIVE : 0,
                () -> p.mode = 0);
            menu.action(Icons.GEAR, IKey.raw("Mode: Color"),  p.mode == 1 ? Colors.ACTIVE : 0,
                () -> p.mode = 1);
        }
    }

    @Override
    protected void populateAddMenu(UIContext context, ContextMenuManager menu)
    {
        /* Input — green */
        ContextMenuManager inputSub = new ContextMenuManager();
        inputSub.action(Icons.MAXIMIZE,       IKey.raw("Value"),      Colors.POSITIVE, () -> this.addNode("value"));
        inputSub.action(Icons.MATERIAL,       IKey.raw("Color"),      Colors.POSITIVE, () -> this.addNode("color"));
        inputSub.action(Icons.TIME,           IKey.raw("Time"),       Colors.POSITIVE, () -> this.addNode("time"));
        inputSub.action(Icons.ALL_DIRECTIONS, IKey.raw("Coordinate"), Colors.POSITIVE, () -> this.addNode("coordinate"));
        inputSub.action(Icons.IMAGE,          IKey.raw("Texture"),    Colors.POSITIVE, () -> this.addNode("texture"));
        menu.action(Icons.DOWNLOAD, IKey.raw("Input"), Colors.POSITIVE, () -> context.replaceContextMenu(inputSub.create()));

        /* Math — blue */
        ContextMenuManager mathSub = new ContextMenuManager();
        mathSub.action(Icons.GEAR,            IKey.raw("Math"),       Colors.ACTIVE, () -> this.addNode("math"));
        mathSub.action(Icons.ALL_DIRECTIONS,  IKey.raw("Vector Math"),Colors.ACTIVE, () -> this.addNode("vector_math"));
        mathSub.action(Icons.GEAR,            IKey.raw("Remap"),      Colors.ACTIVE, () -> this.addNode("remap"));
        mathSub.action(Icons.GEAR,            IKey.raw("Clamp"),      Colors.ACTIVE, () -> this.addNode("clamp"));
        mathSub.action(Icons.GEAR,            IKey.raw("Smoothstep"), Colors.ACTIVE, () -> this.addNode("smoothstep"));
        mathSub.action(Icons.REFRESH,         IKey.raw("Invert"),     Colors.ACTIVE, () -> this.addNode("invert"));
        mathSub.action(Icons.GEAR,            IKey.raw("Posterize"),  Colors.ACTIVE, () -> this.addNode("posterize"));
        menu.action(Icons.GEAR, IKey.raw("Math"), Colors.ACTIVE, () -> context.replaceContextMenu(mathSub.create()));

        /* Color — orange */
        ContextMenuManager colorSub = new ContextMenuManager();
        colorSub.action(Icons.REFRESH, IKey.raw("Mix Color"),      Colors.ORANGE, () -> this.addNode("mix_color"));
        colorSub.action(Icons.FILTER,  IKey.raw("Split Color"),    Colors.ORANGE, () -> this.addNode("split_color"));
        colorSub.action(Icons.FILTER,  IKey.raw("Combine Color"),  Colors.ORANGE, () -> this.addNode("combine_color"));
        colorSub.action(Icons.FILTER,  IKey.raw("Screen Blend"),   Colors.ORANGE, () -> this.addNode("screen_blend"));
        colorSub.action(Icons.FILTER,  IKey.raw("Overlay Blend"),  Colors.ORANGE, () -> this.addNode("overlay_blend"));
        menu.action(Icons.MATERIAL, IKey.raw("Color"), Colors.ORANGE, () -> context.replaceContextMenu(colorSub.create()));

        /* Noise — yellow */
        ContextMenuManager noiseSub = new ContextMenuManager();
        noiseSub.action(Icons.SOUND, IKey.raw("Perlin Noise"), Colors.INACTIVE, () -> this.addNode("noise"));
        noiseSub.action(Icons.SOUND, IKey.raw("Voronoi"),      Colors.INACTIVE, () -> this.addNode("voronoi"));
        noiseSub.action(Icons.SOUND, IKey.raw("Flow Noise"),   Colors.INACTIVE, () -> this.addNode("flow_noise"));
        menu.action(Icons.SOUND, IKey.raw("Noise"), Colors.INACTIVE, () -> context.replaceContextMenu(noiseSub.create()));

        /* Wave */
        ContextMenuManager waveSub = new ContextMenuManager();
        waveSub.action(Icons.ARC,  IKey.raw("Sine Wave"),   Colors.BLUE, () -> this.addNode("sine_wave"));
        waveSub.action(Icons.ARC,  IKey.raw("Square Wave"), Colors.BLUE, () -> this.addNode("square_wave"));
        menu.action(Icons.ARC, IKey.raw("Wave"), Colors.BLUE, () -> context.replaceContextMenu(waveSub.create()));

        /* Adjust / color grade */
        ContextMenuManager adjustSub = new ContextMenuManager();
        adjustSub.action(Icons.FILTER, IKey.raw("Gamma Correction"),    Colors.YELLOW, () -> this.addNode("gamma_correction"));
        adjustSub.action(Icons.FILTER, IKey.raw("Hue / Saturation"),    Colors.YELLOW, () -> this.addNode("hue_saturation"));
        adjustSub.action(Icons.FILTER, IKey.raw("Brightness / Contrast"), Colors.YELLOW, () -> this.addNode("brightness_contrast"));
        adjustSub.action(Icons.FILTER, IKey.raw("Levels"),              Colors.YELLOW, () -> this.addNode("levels"));
        adjustSub.action(Icons.EXCHANGE, IKey.raw("Glitch"),            Colors.MAGENTA, () -> this.addNode("glitch"));
        menu.action(Icons.FILTER, IKey.raw("Adjust"), Colors.YELLOW, () -> context.replaceContextMenu(adjustSub.create()));

        /* Utility — no color */
        ContextMenuManager utilitySub = new ContextMenuManager();
        utilitySub.action(Icons.GEAR, IKey.raw("Trigger"), () -> this.addNode("trigger"));
        utilitySub.action(Icons.EDIT, IKey.raw("Comment"), () -> this.addNode("comment"));
        menu.action(Icons.MORE, IKey.raw("Utility"), () -> context.replaceContextMenu(utilitySub.create()));

        /* Effects — standalone effect nodes */
        ContextMenuManager effectSub = new ContextMenuManager();
        effectSub.action(Icons.FILM,     IKey.raw("Vignette"),   0xFF226644, () -> this.addNode("screen_vignette"));
        effectSub.action(Icons.FILM,     IKey.raw("Grain"),      0xFF226644, () -> this.addNode("screen_grain"));
        effectSub.action(Icons.FILM,     IKey.raw("Letterbox"),  0xFF226644, () -> this.addNode("screen_letterbox"));
        effectSub.action(Icons.FILM,     IKey.raw("Overlay"),    0xFF226644, () -> this.addNode("screen_overlay"));
        effectSub.action(Icons.EXCHANGE, IKey.raw("Distortion"),   0xFF226644, () -> this.addNode("screen_distortion"));
        effectSub.action(Icons.FILTER,   IKey.raw("Color Grade"),  0xFF226644, () -> this.addNode("screen_color_grade"));
        menu.action(Icons.FILM, IKey.raw("Effects"), 0xFF226644, () -> context.replaceContextMenu(effectSub.create()));

        /* Output — screen_output + screen_uv */
        ContextMenuManager outputSub = new ContextMenuManager();
        outputSub.action(Icons.DOWNLOAD, IKey.raw("Screen Output"), Colors.NEGATIVE, () -> this.addNode("screen_output"));
        outputSub.action(Icons.GLOBE,    IKey.raw("Screen UV"),     Colors.NEGATIVE, () -> this.addNode("screen_uv"));
        outputSub.action(Icons.COPY,     IKey.raw("Layer"),         Colors.INACTIVE, () -> this.addNode("screen_layer"));
        menu.action(Icons.UPLOAD, IKey.raw("Output"), Colors.NEGATIVE, () -> context.replaceContextMenu(outputSub.create()));
    }
}
