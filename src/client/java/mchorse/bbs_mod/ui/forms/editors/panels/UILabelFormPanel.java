package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.FontUtils;

import java.util.List;

public class UILabelFormPanel extends UIFormPanel<LabelForm>
{
    public UITextbox text;
    public UIToggle billboard;
    public UIColor color;
    public UITrackpad max;
    public UITrackpad anchorX;
    public UITrackpad anchorY;
    public UIToggle anchorLines;

    public UITrackpad shadowX;
    public UITrackpad shadowY;
    public UIColor shadowColor;

    public UIColor background;
    public UITrackpad offset;

    /* Advanced */
    public UICirculate font;
    private List<String> availableFonts;
    public UITrackpad fontSize;
    public UITrackpad fontWeight;
    public UICirculate fontStyle;
    public UITrackpad letterSpacing;
    public UITrackpad lineHeight;
    public UICirculate textAlign;
    public UITrackpad opacity;
    
    public UIToggle underline;
    public UIToggle strikethrough;
    public UITrackpad shadowBlur;
    public UIToggle outline;
    public UIColor outlineColor;
    public UITrackpad outlineWidth;
    public UIToggle gradient;
    public UIColor gradientEndColor;

    public UILabelFormPanel(UIForm editor)
    {
        super(editor);

        this.text = new UITextbox(10000, (t) -> this.form.text.set(t));
        this.billboard = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_TITLE, (b) -> this.form.billboard.set(b.getValue()));
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.max = new UITrackpad((value) -> this.form.max.set(value.intValue()));
        this.max.limit(-1, Integer.MAX_VALUE, true).increment(10);
        this.anchorX = new UITrackpad((value) -> this.form.anchorX.set(value.floatValue()));
        this.anchorX.values(0.01F);
        this.anchorY = new UITrackpad((value) -> this.form.anchorY.set(value.floatValue()));
        this.anchorY.values(0.01F);
        this.anchorLines = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_ANCHOR_LINES, (value) -> this.form.anchorLines.set(value.getValue()));

        this.shadowX = new UITrackpad((value) -> this.form.shadowX.set(value.floatValue()));
        this.shadowX.limit(-100, 100).values(0.1F, 0.01F, 0.5F).increment(0.1F);
        this.shadowY = new UITrackpad((value) -> this.form.shadowY.set(value.floatValue()));
        this.shadowY.limit(-100, 100).values(0.1F, 0.01F, 0.5F).increment(0.1F);
        this.shadowColor = new UIColor((value) -> this.form.shadowColor.set(Color.rgba(value))).withAlpha();

        this.background = new UIColor((value) -> this.form.background.set(Color.rgba(value))).withAlpha();
        this.offset = new UITrackpad((value) -> this.form.offset.set(value.floatValue()));

        /* Advanced Inits */
        this.availableFonts = FontUtils.getAvailableFonts();
        this.font = new UICirculate((b) ->
        {
            int v = b.getValue();
            if (v == 0) this.form.font.set("");
            else if (v - 1 < this.availableFonts.size()) this.form.font.set(this.availableFonts.get(v - 1));
        });
        
        this.font.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_DEFAULT);
        for (String fontName : this.availableFonts)
        {
            this.font.addLabel(IKey.raw(fontName));
        }

        this.fontSize = new UITrackpad((v) -> this.form.fontSize.set(v.floatValue()));
        this.fontSize.limit(0.1F, 100F).values(0.1F, 0.1F, 2F);
        
        this.fontWeight = new UITrackpad((v) -> this.form.fontWeight.set(v.intValue()));
        this.fontWeight.limit(100, 900, true).increment(100);
        
        this.fontStyle = new UICirculate((b) -> this.form.fontStyle.set(b.getValue()));
        this.fontStyle.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_STYLE_NORMAL);
        this.fontStyle.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_STYLE_ITALIC);
        this.fontStyle.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_STYLE_OBLIQUE);
        
        this.textAlign = new UICirculate((b) -> this.form.textAlign.set(b.getValue()));
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_LEFT);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_CENTER);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_RIGHT);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_JUSTIFY);

        this.letterSpacing = new UITrackpad((v) -> this.form.letterSpacing.set(v.floatValue()));
        this.letterSpacing.limit(-10F, 50F).values(0.1F);
        
        this.lineHeight = new UITrackpad((v) -> this.form.lineHeight.set(v.floatValue()));
        this.lineHeight.limit(0F, 100F).values(0.1F);
        
        this.opacity = new UITrackpad((v) -> this.form.opacity.set(v.floatValue()));
        this.opacity.limit(0F, 1F).values(0.05F);

        this.underline = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_UNDERLINE, (b) -> this.form.underline.set(b.getValue()));
        this.strikethrough = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_STRIKETHROUGH, (b) -> this.form.strikethrough.set(b.getValue()));
        
        this.shadowBlur = new UITrackpad((v) -> this.form.shadowBlur.set(v.floatValue()));
        this.shadowBlur.limit(0F, 20F).values(0.1F);
        
        this.outline = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_OUTLINE, (b) -> this.form.outline.set(b.getValue()));
        this.outlineColor = new UIColor((c) -> this.form.outlineColor.set(Color.rgba(c))).withAlpha();
        this.outlineWidth = new UITrackpad((v) -> this.form.outlineWidth.set(v.floatValue()));
        this.outlineWidth.limit(0F, 10F).values(0.1F);
        
        this.gradient = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_GRADIENT, (b) -> this.form.gradient.set(b.getValue()));
        this.gradientEndColor = new UIColor((c) -> this.form.gradientEndColor.set(Color.rgba(c))).withAlpha();

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_LABEL), this.text, this.billboard, this.color, this.max);

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_ANCHOR).marginTop(8), UI.row(this.anchorX, this.anchorY), this.anchorLines);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_OFFSET).marginTop(8), this.shadowX, this.shadowY);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_COLOR).marginTop(8), this.shadowColor);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_BACKGROUND).marginTop(8), this.background, this.offset);

        /* Advanced Layout */
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_ADVANCED_TEXT).marginTop(12));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_FONT), this.font);
        this.options.add(UI.row(this.fontSize, this.fontWeight));
        this.options.add(UI.row(this.fontStyle, this.textAlign));
        this.options.add(UI.row(this.letterSpacing, this.lineHeight));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_OPACITY), this.opacity);
        this.options.add(UI.row(this.underline, this.strikethrough));
        
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_EFFECTS).marginTop(8));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_BLUR), this.shadowBlur);
        this.options.add(this.outline, this.outlineColor, this.outlineWidth);
        this.options.add(this.gradient, this.gradientEndColor);
    }

    @Override
    public void startEdit(LabelForm form)
    {
        super.startEdit(form);

        this.text.setText(form.text.get());
        this.billboard.setValue(form.billboard.get());
        this.color.setColor(form.color.get().getARGBColor());
        this.max.setValue(form.max.get());
        this.anchorX.setValue(form.anchorX.get());
        this.anchorY.setValue(form.anchorY.get());
        this.anchorLines.setValue(form.anchorLines.get());

        this.shadowX.setValue(form.shadowX.get());
        this.shadowY.setValue(form.shadowY.get());
        this.shadowColor.setColor(form.shadowColor.get().getARGBColor());

        this.background.setColor(form.background.get().getARGBColor());
        this.offset.setValue(form.offset.get());

        /* Advanced Sync */
        String currentFont = form.font.get();
        int fontIndex = this.availableFonts.indexOf(currentFont);
        this.font.setValue(fontIndex == -1 ? 0 : fontIndex + 1);

        this.fontSize.setValue(form.fontSize.get());
        this.fontWeight.setValue(form.fontWeight.get());
        this.fontStyle.setValue(form.fontStyle.get());
        this.textAlign.setValue(form.textAlign.get());
        this.letterSpacing.setValue(form.letterSpacing.get());
        this.lineHeight.setValue(form.lineHeight.get());
        this.opacity.setValue(form.opacity.get());
        this.underline.setValue(form.underline.get());
        this.strikethrough.setValue(form.strikethrough.get());
        this.shadowBlur.setValue(form.shadowBlur.get());
        this.outline.setValue(form.outline.get());
        this.outlineColor.setColor(form.outlineColor.get().getARGBColor());
        this.outlineWidth.setValue(form.outlineWidth.get());
        this.gradient.setValue(form.gradient.get());
        this.gradientEndColor.setColor(form.gradientEndColor.get().getARGBColor());
    }

    @Override
    public void finishEdit()
    {
        super.finishEdit();

        this.color.picker.removeFromParent();
        this.shadowColor.picker.removeFromParent();
        this.background.picker.removeFromParent();
        
        this.outlineColor.picker.removeFromParent();
        this.gradientEndColor.picker.removeFromParent();
    }
}