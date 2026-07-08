package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIFileLinkList;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIIllusionKeyframeFactory extends UIKeyframeFactory<Illusion>
{
    private UITrackpad count;
    private UITrackpad spread;
    private UIToggle front;
    private UIToggle back;
    private UIToggle left;
    private UIToggle right;
    private UIToggle up;
    private UIToggle down;
    private UIToggle uniform;
    private UITrackpad spacing;
    private UITrackpad offset;
    private UITrackpad opacity;
    private UIToggle invert;
    private UIButton textures;
    private UIButton clearTextures;
    private UIToggle randomTextures;
    private UIToggle real;
    private UIToggle connected;
    private UIToggle gradual;
    private UIToggle delayEnabled;
    private UITrackpad delay;
    private UITrackpad distort;
    private UIToggle glowEnabled;
    private UITrackpad glow;
    private UIToggle glowUniform;
    private UIToggle glowInvert;

    /**
     * Opens the texture picker with multi-selection support. A plain click picks a
     * single texture, Shift + click appends more textures, and if the Shift +
     * clicked texture is in the same folder as the last picked one, the whole range
     * in between gets appended in click direction.
     */
    public static void pickTextures(UIContext context, Supplier<List<Link>> getter, Consumer<List<Link>> setter)
    {
        List<Link> textures = new ArrayList<>(getter.get());
        Link initial = textures.isEmpty() ? null : textures.get(textures.size() - 1);
        Link[] lastNotified = {null};
        UITexturePicker[] ui = new UITexturePicker[1];

        ui[0] = UITexturePicker.open(context, initial, (link) ->
        {
            if (link == null)
            {
                return;
            }

            if (Window.isShiftPressed())
            {
                List<Link> range = getTextureRange(ui[0], textures.isEmpty() ? null : textures.get(textures.size() - 1), link);

                if (range != null)
                {
                    textures.addAll(range);
                }
                else
                {
                    textures.add(link);
                }
            }
            else
            {
                /* The picker re-sends the current texture when it gets closed */
                if (link.equals(lastNotified[0]))
                {
                    return;
                }

                textures.clear();
                textures.add(link);
            }

            lastNotified[0] = link;
            setter.accept(new ArrayList<>(textures));
        });

        if (ui[0] != null)
        {
            ui[0].disableMultiSkin();

            /* Highlight all the picked textures in the file list (blue selection tint) */
            ui[0].highlighted(() -> textures);
        }
    }

    /**
     * Textures between the last picked one and the newly Shift + clicked one (in
     * click direction, excluding the anchor, including the clicked one), taken from
     * the picker's current folder listing.
     */
    private static List<Link> getTextureRange(UITexturePicker ui, Link from, Link to)
    {
        if (ui == null || from == null || from.equals(to))
        {
            return null;
        }

        List<UIFileLinkList.FileLink> files = ui.picker.getList();
        int fromIndex = -1;
        int toIndex = -1;

        for (int i = 0; i < files.size(); i++)
        {
            UIFileLinkList.FileLink file = files.get(i);

            if (file.folder)
            {
                continue;
            }

            if (file.link.equals(from)) fromIndex = i;
            if (file.link.equals(to)) toIndex = i;
        }

        if (fromIndex < 0 || toIndex < 0)
        {
            return null;
        }

        List<Link> range = new ArrayList<>();
        int step = fromIndex < toIndex ? 1 : -1;

        for (int i = fromIndex + step; i != toIndex + step; i += step)
        {
            UIFileLinkList.FileLink file = files.get(i);

            if (!file.folder)
            {
                range.add(file.link);
            }
        }

        return range.isEmpty() ? null : range;
    }

    public UIIllusionKeyframeFactory(Keyframe<Illusion> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        Illusion illusion = keyframe.getValue();

        this.count = new UITrackpad((v) -> this.editIllusion((i) -> i.count = v.intValue()));
        this.count.limit(0D).integer().setValue(illusion.count);
        this.spread = new UITrackpad((v) -> this.editIllusion((i) -> i.spread = v.floatValue()));
        this.spread.setValue(illusion.spread);
        this.spread.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPREAD_TOOLTIP);

        this.front = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_FRONT, (b) -> this.toggleDirection(Illusion.FRONT, b.getValue()));
        this.back = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_BACK, (b) -> this.toggleDirection(Illusion.BACK, b.getValue()));
        this.left = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_LEFT, (b) -> this.toggleDirection(Illusion.LEFT, b.getValue()));
        this.right = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_RIGHT, (b) -> this.toggleDirection(Illusion.RIGHT, b.getValue()));
        this.up = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UP, (b) -> this.toggleDirection(Illusion.UP, b.getValue()));
        this.down = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DOWN, (b) -> this.toggleDirection(Illusion.DOWN, b.getValue()));

        this.uniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UNIFORM, (b) -> this.editIllusion((i) -> i.uniform = b.getValue()));
        this.uniform.setValue(illusion.uniform);
        this.uniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UNIFORM_TOOLTIP);
        this.spacing = new UITrackpad((v) -> this.editIllusion((i) -> i.spacing = v.floatValue()));
        this.spacing.setValue(illusion.spacing);
        this.spacing.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPACING_TOOLTIP);

        this.offset = new UITrackpad((v) -> this.editIllusion((i) -> i.offset = v.floatValue()));
        this.offset.setValue(illusion.offset);
        this.offset.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OFFSET_TOOLTIP);
        this.opacity = new UITrackpad((v) -> this.editIllusion((i) -> i.opacity = v.floatValue() / 100F));
        this.opacity.limit(0D).setValue(illusion.opacity * 100F);
        this.opacity.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY_TOOLTIP);
        this.invert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_INVERT, (b) -> this.editIllusion((i) -> i.invert = b.getValue()));
        this.invert.setValue(illusion.invert);

        this.textures = new UIButton(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES, (b) ->
        {
            pickTextures(this.getContext(), () -> this.keyframe.getValue().textures, (list) -> this.editIllusion((i) ->
            {
                i.textures.clear();
                i.textures.addAll(list);
            }));
        });
        this.textures.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_TOOLTIP);
        this.clearTextures = new UIButton(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_CLEAR, (b) -> this.editIllusion((i) -> i.textures.clear()));
        this.randomTextures = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_RANDOM, (b) -> this.editIllusion((i) -> i.randomTextures = b.getValue()));
        this.randomTextures.setValue(illusion.randomTextures);
        this.randomTextures.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_RANDOM_TOOLTIP);

        this.real = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_REAL, (b) -> this.editIllusion((i) -> i.real = b.getValue()));
        this.real.setValue(illusion.real);
        this.real.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_REAL_TOOLTIP);

        this.connected = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_CONNECTED, (b) -> this.editIllusion((i) -> i.connected = b.getValue()));
        this.connected.setValue(illusion.connected);
        this.connected.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_CONNECTED_TOOLTIP);
        this.gradual = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL, (b) -> this.editIllusion((i) -> i.gradual = b.getValue()));
        this.gradual.setValue(illusion.gradual);
        this.gradual.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL_TOOLTIP);

        this.delayEnabled = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DELAY, (b) -> this.editIllusion((i) -> i.delayEnabled = b.getValue()));
        this.delayEnabled.setValue(illusion.delayEnabled);
        this.delayEnabled.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DELAY_TOOLTIP);
        this.delay = new UITrackpad((v) -> this.editIllusion((i) -> i.delay = v.floatValue()));
        this.delay.limit(0D).setValue(illusion.delay);
        this.delay.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DELAY_TOOLTIP);

        this.distort = new UITrackpad((v) -> this.editIllusion((i) -> i.distort = v.floatValue()));
        this.distort.limit(0D).setValue(illusion.distort);
        this.distort.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_TOOLTIP);

        this.glowEnabled = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW, (b) -> this.editIllusion((i) -> i.glowEnabled = b.getValue()));
        this.glowEnabled.setValue(illusion.glowEnabled);
        this.glowEnabled.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_TOOLTIP);
        this.glow = new UITrackpad((v) -> this.editIllusion((i) -> i.glow = v.floatValue()));
        this.glow.setValue(illusion.glow);
        this.glow.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_TOOLTIP);
        this.glowUniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_UNIFORM, (b) -> this.editIllusion((i) -> i.glowUniform = b.getValue()));
        this.glowUniform.setValue(illusion.glowUniform);
        this.glowUniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_UNIFORM_TOOLTIP);
        this.glowInvert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_INVERT, (b) -> this.editIllusion((i) -> i.glowInvert = b.getValue()));
        this.glowInvert.setValue(illusion.glowInvert);
        this.glowInvert.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_INVERT_TOOLTIP);

        this.updateDirections(illusion.directions);

        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_COUNT), this.count);
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPREAD), this.spread);
        this.scroll.add(UI.row(this.front, this.back));
        this.scroll.add(UI.row(this.left, this.right));
        this.scroll.add(UI.row(this.up, this.down));
        this.scroll.add(this.uniform, UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPACING), this.spacing);
        this.scroll.add(this.connected);
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OFFSET), this.offset);
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY), this.opacity, this.invert);
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT), this.distort);
        this.scroll.add(this.gradual);
        this.scroll.add(this.delayEnabled, this.delay);
        this.scroll.add(this.glowEnabled, this.glow);
        this.scroll.add(UI.row(this.glowUniform, this.glowInvert));
        this.scroll.add(this.textures, this.clearTextures, this.randomTextures);
        this.scroll.add(this.real);
    }

    private void editIllusion(Consumer<Illusion> consumer)
    {
        BaseValue.edit(this.keyframe, (value) -> consumer.accept(value.getValue()));
    }

    private void toggleDirection(int bit, boolean enabled)
    {
        this.editIllusion((illusion) -> illusion.directions = enabled ? illusion.directions | bit : illusion.directions & ~bit);
    }

    private void updateDirections(int directions)
    {
        this.front.setValue((directions & Illusion.FRONT) != 0);
        this.back.setValue((directions & Illusion.BACK) != 0);
        this.left.setValue((directions & Illusion.LEFT) != 0);
        this.right.setValue((directions & Illusion.RIGHT) != 0);
        this.up.setValue((directions & Illusion.UP) != 0);
        this.down.setValue((directions & Illusion.DOWN) != 0);
    }

    @Override
    public void update()
    {
        super.update();

        Illusion illusion = this.keyframe.getValue();

        this.count.setValue(illusion.count);
        this.spread.setValue(illusion.spread);
        this.uniform.setValue(illusion.uniform);
        this.spacing.setValue(illusion.spacing);
        this.offset.setValue(illusion.offset);
        this.opacity.setValue(illusion.opacity * 100F);
        this.invert.setValue(illusion.invert);
        this.randomTextures.setValue(illusion.randomTextures);
        this.real.setValue(illusion.real);
        this.connected.setValue(illusion.connected);
        this.gradual.setValue(illusion.gradual);
        this.delayEnabled.setValue(illusion.delayEnabled);
        this.delay.setValue(illusion.delay);
        this.distort.setValue(illusion.distort);
        this.glowEnabled.setValue(illusion.glowEnabled);
        this.glow.setValue(illusion.glow);
        this.glowUniform.setValue(illusion.glowUniform);
        this.glowInvert.setValue(illusion.glowInvert);
        this.updateDirections(illusion.directions);
    }
}
