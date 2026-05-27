package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.nodes.BrightnessContrastNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ColorGradeEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.DistortionEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GammaCorrectionNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GlitchNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GrainEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.HueSaturationNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LayerNode;
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
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.panels.shape.UIShapeNodeEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
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
        if (node instanceof ScreenOutputNode)   return L10n.lang("bbs.ui.screen_node.screen_output").get();
        if (node instanceof ScreenUVNode)       return L10n.lang("bbs.ui.screen_node.screen_uv").get();
        if (node instanceof ColorGradeEffectNode) return L10n.lang("bbs.ui.screen_node.color_grade").get();
        if (node instanceof LayerNode)          return L10n.lang("bbs.ui.screen_node.layer").get();
        if (node instanceof DistortionEffectNode) return L10n.lang("bbs.ui.screen_node.distortion").get();
        if (node instanceof VignetteEffectNode) return L10n.lang("bbs.ui.screen_node.vignette").get();
        if (node instanceof GrainEffectNode)    return L10n.lang("bbs.ui.screen_node.grain").get();
        if (node instanceof LetterboxEffectNode) return L10n.lang("bbs.ui.screen_node.letterbox").get();
        if (node instanceof OverlayEffectNode)  return L10n.lang("bbs.ui.screen_node.overlay").get();
        if (node instanceof SineWaveNode)     return L10n.lang("bbs.ui.screen_node.sine_wave").get();
        if (node instanceof SquareWaveNode)   return L10n.lang("bbs.ui.screen_node.square_wave").get();
        if (node instanceof ScreenBlendNode)  return L10n.lang("bbs.ui.screen_node.screen_blend").get();
        if (node instanceof OverlayBlendNode) return L10n.lang("bbs.ui.screen_node.overlay_blend").get();
        if (node instanceof GammaCorrectionNode)  return L10n.lang("bbs.ui.screen_node.gamma_correction").get();
        if (node instanceof HueSaturationNode)    return L10n.lang("bbs.ui.screen_node.hue_saturation").get();
        if (node instanceof BrightnessContrastNode) return L10n.lang("bbs.ui.screen_node.brightness_contrast").get();
        if (node instanceof LevelsNode)       return L10n.lang("bbs.ui.screen_node.levels").get();
        if (node instanceof GlitchNode)       return L10n.lang("bbs.ui.screen_node.glitch").get();
        if (node instanceof PosterizeNode)
        {
            return ((PosterizeNode) node).mode == 1 ? L10n.lang("bbs.ui.screen_node.posterize_color").get() : L10n.lang("bbs.ui.screen_node.posterize").get();
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

            menu.action(Icons.GEAR, L10n.lang("bbs.ui.raw.mode_scalar"), p.mode == 0 ? Colors.ACTIVE : 0,
                () -> p.mode = 0);
            menu.action(Icons.GEAR, L10n.lang("bbs.ui.raw.mode_color"),  p.mode == 1 ? Colors.ACTIVE : 0,
                () -> p.mode = 1);
        }
    }

    @Override
    protected void populateAddMenu(UIContext context, ContextMenuManager menu)
    {
        /* Input — green */
        ContextMenuManager inputSub = new ContextMenuManager();
        inputSub.action(Icons.MAXIMIZE,       L10n.lang("bbs.ui.raw.value"),      Colors.POSITIVE, () -> this.addNode("value"));
        inputSub.action(Icons.MATERIAL,       L10n.lang("bbs.ui.raw.color"),      Colors.POSITIVE, () -> this.addNode("color"));
        inputSub.action(Icons.TIME,           L10n.lang("bbs.ui.raw.time"),       Colors.POSITIVE, () -> this.addNode("time"));
        inputSub.action(Icons.ALL_DIRECTIONS, L10n.lang("bbs.ui.raw.coordinate"), Colors.POSITIVE, () -> this.addNode("coordinate"));
        inputSub.action(Icons.IMAGE,          L10n.lang("bbs.ui.raw.texture"),    Colors.POSITIVE, () -> this.addNode("texture"));
        menu.action(Icons.DOWNLOAD, L10n.lang("bbs.ui.raw.input"), Colors.POSITIVE, () -> context.replaceContextMenu(inputSub.create()));

        /* Math — blue */
        ContextMenuManager mathSub = new ContextMenuManager();
        mathSub.action(Icons.GEAR,            L10n.lang("bbs.ui.raw.math"),       Colors.ACTIVE, () -> this.addNode("math"));
        mathSub.action(Icons.ALL_DIRECTIONS,  L10n.lang("bbs.ui.raw.vector_math"),Colors.ACTIVE, () -> this.addNode("vector_math"));
        mathSub.action(Icons.GEAR,            L10n.lang("bbs.ui.raw.remap"),      Colors.ACTIVE, () -> this.addNode("remap"));
        mathSub.action(Icons.GEAR,            L10n.lang("bbs.ui.raw.clamp"),      Colors.ACTIVE, () -> this.addNode("clamp"));
        mathSub.action(Icons.GEAR,            L10n.lang("bbs.ui.raw.smoothstep"), Colors.ACTIVE, () -> this.addNode("smoothstep"));
        mathSub.action(Icons.REFRESH,         L10n.lang("bbs.ui.raw.invert"),     Colors.ACTIVE, () -> this.addNode("invert"));
        mathSub.action(Icons.GEAR,            L10n.lang("bbs.ui.raw.posterize"),  Colors.ACTIVE, () -> this.addNode("posterize"));
        menu.action(Icons.GEAR, L10n.lang("bbs.ui.raw.math"), Colors.ACTIVE, () -> context.replaceContextMenu(mathSub.create()));

        /* Color — orange */
        ContextMenuManager colorSub = new ContextMenuManager();
        colorSub.action(Icons.REFRESH, L10n.lang("bbs.ui.raw.mix_color"),      Colors.ORANGE, () -> this.addNode("mix_color"));
        colorSub.action(Icons.FILTER,  L10n.lang("bbs.ui.raw.split_color"),    Colors.ORANGE, () -> this.addNode("split_color"));
        colorSub.action(Icons.FILTER,  L10n.lang("bbs.ui.raw.combine_color"),  Colors.ORANGE, () -> this.addNode("combine_color"));
        colorSub.action(Icons.FILTER,  L10n.lang("bbs.ui.raw.screen_blend"),   Colors.ORANGE, () -> this.addNode("screen_blend"));
        colorSub.action(Icons.FILTER,  L10n.lang("bbs.ui.raw.overlay_blend"),  Colors.ORANGE, () -> this.addNode("overlay_blend"));
        menu.action(Icons.MATERIAL, L10n.lang("bbs.ui.raw.color"), Colors.ORANGE, () -> context.replaceContextMenu(colorSub.create()));

        /* Noise — yellow */
        ContextMenuManager noiseSub = new ContextMenuManager();
        noiseSub.action(Icons.SOUND, L10n.lang("bbs.ui.raw.perlin_noise"), Colors.INACTIVE, () -> this.addNode("noise"));
        noiseSub.action(Icons.SOUND, L10n.lang("bbs.ui.raw.voronoi"),      Colors.INACTIVE, () -> this.addNode("voronoi"));
        noiseSub.action(Icons.SOUND, L10n.lang("bbs.ui.raw.flow_noise"),   Colors.INACTIVE, () -> this.addNode("flow_noise"));
        menu.action(Icons.SOUND, L10n.lang("bbs.ui.raw.noise"), Colors.INACTIVE, () -> context.replaceContextMenu(noiseSub.create()));

        /* Wave */
        ContextMenuManager waveSub = new ContextMenuManager();
        waveSub.action(Icons.ARC,  L10n.lang("bbs.ui.raw.sine_wave"),   Colors.BLUE, () -> this.addNode("sine_wave"));
        waveSub.action(Icons.ARC,  L10n.lang("bbs.ui.raw.square_wave"), Colors.BLUE, () -> this.addNode("square_wave"));
        menu.action(Icons.ARC, L10n.lang("bbs.ui.raw.wave"), Colors.BLUE, () -> context.replaceContextMenu(waveSub.create()));

        /* Adjust / color grade */
        ContextMenuManager adjustSub = new ContextMenuManager();
        adjustSub.action(Icons.FILTER, L10n.lang("bbs.ui.raw.gamma_correction"),    Colors.YELLOW, () -> this.addNode("gamma_correction"));
        adjustSub.action(Icons.FILTER, L10n.lang("bbs.ui.raw.hue_saturation"),    Colors.YELLOW, () -> this.addNode("hue_saturation"));
        adjustSub.action(Icons.FILTER, L10n.lang("bbs.ui.raw.brightness_contrast"), Colors.YELLOW, () -> this.addNode("brightness_contrast"));
        adjustSub.action(Icons.FILTER, L10n.lang("bbs.ui.raw.levels"),              Colors.YELLOW, () -> this.addNode("levels"));
        adjustSub.action(Icons.EXCHANGE, L10n.lang("bbs.ui.raw.glitch"),            Colors.MAGENTA, () -> this.addNode("glitch"));
        menu.action(Icons.FILTER, L10n.lang("bbs.ui.raw.adjust"), Colors.YELLOW, () -> context.replaceContextMenu(adjustSub.create()));

        /* Utility — no color */
        ContextMenuManager utilitySub = new ContextMenuManager();
        utilitySub.action(Icons.GEAR, L10n.lang("bbs.ui.raw.trigger"), () -> this.addNode("trigger"));
        utilitySub.action(Icons.EDIT, L10n.lang("bbs.ui.raw.comment"), () -> this.addNode("comment"));
        menu.action(Icons.MORE, L10n.lang("bbs.ui.raw.utility"), () -> context.replaceContextMenu(utilitySub.create()));

        /* Effects — standalone effect nodes */
        ContextMenuManager effectSub = new ContextMenuManager();
        effectSub.action(Icons.FILM,     L10n.lang("bbs.ui.raw.vignette"),   0xFF226644, () -> this.addNode("screen_vignette"));
        effectSub.action(Icons.FILM,     L10n.lang("bbs.ui.raw.grain"),      0xFF226644, () -> this.addNode("screen_grain"));
        effectSub.action(Icons.FILM,     L10n.lang("bbs.ui.raw.letterbox"),  0xFF226644, () -> this.addNode("screen_letterbox"));
        effectSub.action(Icons.FILM,     L10n.lang("bbs.ui.raw.overlay"),    0xFF226644, () -> this.addNode("screen_overlay"));
        effectSub.action(Icons.EXCHANGE, L10n.lang("bbs.ui.raw.distortion"),   0xFF226644, () -> this.addNode("screen_distortion"));
        effectSub.action(Icons.FILTER,   L10n.lang("bbs.ui.raw.color_grade"),  0xFF226644, () -> this.addNode("screen_color_grade"));
        menu.action(Icons.FILM, L10n.lang("bbs.ui.raw.effects"), 0xFF226644, () -> context.replaceContextMenu(effectSub.create()));

        /* Output — screen_output + screen_uv */
        ContextMenuManager outputSub = new ContextMenuManager();
        outputSub.action(Icons.DOWNLOAD, L10n.lang("bbs.ui.raw.screen_output"), Colors.NEGATIVE, () -> this.addNode("screen_output"));
        outputSub.action(Icons.GLOBE,    L10n.lang("bbs.ui.raw.screen_uv"),     Colors.NEGATIVE, () -> this.addNode("screen_uv"));
        outputSub.action(Icons.COPY,     L10n.lang("bbs.ui.raw.layer"),         Colors.INACTIVE, () -> this.addNode("screen_layer"));
        menu.action(Icons.UPLOAD, L10n.lang("bbs.ui.raw.output"), Colors.NEGATIVE, () -> context.replaceContextMenu(outputSub.create()));
    }
}
