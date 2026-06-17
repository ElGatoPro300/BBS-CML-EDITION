package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.camera.clips.misc.ChromaSkyCurveSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIChromaSkyCurveSettingsKeyframeFactory extends UIKeyframeFactory<ChromaSkyCurveSettings>
{
    private static final IKey TITLE = L10n.lang("bbs.config.chroma_sky.title");
    private static final IKey ENABLED = L10n.lang("bbs.config.chroma_sky.enabled");
    private static final IKey COLOR = L10n.lang("bbs.config.chroma_sky.color");
    private static final IKey TERRAIN = L10n.lang("bbs.config.chroma_sky.terrain");
    private static final IKey CLOUDS = L10n.lang("bbs.config.chroma_sky.clouds");
    private static final IKey BILLBOARD = L10n.lang("bbs.config.chroma_sky.billboard");

    private UIToggle enabled;
    private UIColor color;
    private UIToggle terrain;
    private UIToggle clouds;
    private UITrackpad billboard;

    public UIChromaSkyCurveSettingsKeyframeFactory(Keyframe<ChromaSkyCurveSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.enabled = new UIToggle(ENABLED, false, (t) -> this.setEnabledValue(t.getValue()));
        this.color = new UIColor((value) -> this.setColor(value));
        this.terrain = new UIToggle(TERRAIN, true, (t) -> this.setTerrain(t.getValue()));
        this.clouds = new UIToggle(CLOUDS, true, (t) -> this.setClouds(t.getValue()));
        this.billboard = new UITrackpad((v) -> this.setBillboard(v.floatValue()));
        this.billboard.limit(0, 256).tooltip(BILLBOARD);

        this.scroll.add(UI.label(TITLE));
        this.scroll.add(this.enabled);
        this.scroll.add(UI.label(COLOR));
        this.scroll.add(this.color);
        this.scroll.add(this.terrain);
        this.scroll.add(this.clouds);
        this.scroll.add(UI.label(BILLBOARD));
        this.scroll.add(this.billboard);

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        ChromaSkyCurveSettings value = this.keyframe.getValue();

        if (value == null)
        {
            value = new ChromaSkyCurveSettings();
        }

        this.enabled.setValue(value.enabled);
        this.color.setColor(value.color.getRGBColor());
        this.terrain.setValue(value.terrain);
        this.clouds.setValue(value.clouds);
        this.billboard.setValue(value.billboard);
    }

    private ChromaSkyCurveSettings copyValue()
    {
        ChromaSkyCurveSettings value = this.keyframe.getValue();

        return value == null ? new ChromaSkyCurveSettings() : value.copy();
    }

    private void setEnabledValue(boolean v)
    {
        ChromaSkyCurveSettings value = this.copyValue();

        value.enabled = v;
        this.setValue(value);
    }

    private void setColor(int color)
    {
        ChromaSkyCurveSettings value = this.copyValue();

        value.color.set(color, false);
        this.setValue(value);
    }

    private void setTerrain(boolean v)
    {
        ChromaSkyCurveSettings value = this.copyValue();

        value.terrain = v;
        this.setValue(value);
    }

    private void setClouds(boolean v)
    {
        ChromaSkyCurveSettings value = this.copyValue();

        value.clouds = v;
        this.setValue(value);
    }

    private void setBillboard(float v)
    {
        ChromaSkyCurveSettings value = this.copyValue();

        value.billboard = v;
        this.setValue(value);
    }
}
