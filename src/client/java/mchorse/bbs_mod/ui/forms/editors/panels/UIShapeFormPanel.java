package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.ShapeForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.shape.UIShapeNodeEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

public class UIShapeFormPanel extends UIFormPanel<ShapeForm>
{
    public UIShapeNodeEditor nodeEditor;

    public UICirculate type;
    public UIIcon toggleNodeEditor;
    public UITrackpad sizeX;
    public UITrackpad sizeY;
    public UITrackpad sizeZ;
    public UITrackpad subdivisions;
    
    public UIButton pickTexture;
    public UIColor color;
    public UITrackpad textureScale;
    public UITrackpad textureScrollX;
    public UITrackpad textureScrollY;
    public UIToggle lighting;

    public UIToggle particles;
    public UICirculate particleType;
    public UITrackpad particleScale;
    public UITrackpad particleDensity;
    public UITrackpad particleSize;

    public UIShapeFormPanel(UIForm editor)
    {
        super(editor);

        this.nodeEditor = new UIShapeNodeEditor();
        this.nodeEditor.relative(this).w(1F).h(1F).setVisible(true);
        this.prepend(this.nodeEditor);

        /* Geometry */
        this.toggleNodeEditor = new UIIcon(Icons.GRAPH, (b) -> this.nodeEditor.toggleVisible());
        this.toggleNodeEditor.tooltip(IKey.raw("Toggle Node Editor"));

        this.type = new UICirculate((b) -> this.form.type.set(ShapeForm.ShapeType.values()[b.getValue()]));
        for (ShapeForm.ShapeType type : ShapeForm.ShapeType.values())
        {
            IKey label = IKey.raw(type.name());

            if (type == ShapeForm.ShapeType.BOX) label = UIKeys.FORMS_EDITORS_SHAPE_TYPE_BOX;
            else if (type == ShapeForm.ShapeType.SPHERE) label = UIKeys.FORMS_EDITORS_SHAPE_TYPE_SPHERE;
            else if (type == ShapeForm.ShapeType.CYLINDER) label = UIKeys.FORMS_EDITORS_SHAPE_TYPE_CYLINDER;
            else if (type == ShapeForm.ShapeType.CAPSULE) label = UIKeys.FORMS_EDITORS_SHAPE_TYPE_CAPSULE;

            this.type.addLabel(label);
        }
        
        this.sizeX = new UITrackpad((value) -> this.form.sizeX.set(value.floatValue()));
        this.sizeX.tooltip(UIKeys.FORMS_EDITORS_SHAPE_SIZE_X);
        
        this.sizeY = new UITrackpad((value) -> this.form.sizeY.set(value.floatValue()));
        this.sizeY.tooltip(UIKeys.FORMS_EDITORS_SHAPE_SIZE_Y);
        
        this.sizeZ = new UITrackpad((value) -> this.form.sizeZ.set(value.floatValue()));
        this.sizeZ.tooltip(UIKeys.FORMS_EDITORS_SHAPE_SIZE_Z);
        
        this.subdivisions = new UITrackpad((value) -> this.form.subdivisions.set(value.intValue()));
        this.subdivisions.integer();
        this.subdivisions.tooltip(UIKeys.FORMS_EDITORS_SHAPE_SUBDIVISIONS);

        /* Appearance */
        this.pickTexture = new UIButton(UIKeys.FORMS_EDITORS_SHAPE_PICK_TEXTURE, (b) ->
        {
            UITexturePicker.open(this.getContext(), this.form.texture.get(), (l) -> this.form.texture.set(l));
        });
        
        this.color = new UIColor((value) -> this.form.color.set(Color.rgba(value))).direction(Direction.LEFT).withAlpha();
        
        this.textureScale = new UITrackpad((value) -> this.form.textureScale.set(value.floatValue()));
        this.textureScale.tooltip(UIKeys.FORMS_EDITORS_SHAPE_TEXTURE_SCALE);
        
        this.textureScrollX = new UITrackpad((value) -> this.form.textureScrollX.set(value.floatValue()));
        this.textureScrollX.tooltip(UIKeys.FORMS_EDITORS_SHAPE_TEXTURE_SCROLL_X);
        
        this.textureScrollY = new UITrackpad((value) -> this.form.textureScrollY.set(value.floatValue()));
        this.textureScrollY.tooltip(UIKeys.FORMS_EDITORS_SHAPE_TEXTURE_SCROLL_Y);
        
        this.lighting = new UIToggle(IKey.raw("Additive"), (b) -> this.form.lighting.set(b.getValue()));
        this.lighting.tooltip(IKey.raw("Enables additive blending (glowing)"));

        /* Particles */
        this.particles = new UIToggle(IKey.raw("Particles"), (b) ->
        {
            this.form.particles.set(b.getValue());
            this.updateParticleVisibility();
        });

        this.particleType = new UICirculate((b) -> this.form.particleType.set(ShapeForm.ParticleType.values()[b.getValue()]));
        for (ShapeForm.ParticleType type : ShapeForm.ParticleType.values())
        {
            this.particleType.addLabel(IKey.raw(type.name()));
        }

        this.particleScale = new UITrackpad((value) -> this.form.particleScale.set(value.floatValue()));
        this.particleScale.tooltip(IKey.raw("Particle Scale"));

        this.particleDensity = new UITrackpad((value) -> this.form.particleDensity.set(value.floatValue()));
        this.particleDensity.tooltip(IKey.raw("Particle Density"));

        this.particleSize = new UITrackpad((value) -> this.form.particleSize.set(value.floatValue()));
        this.particleSize.tooltip(IKey.raw("Particle Size"));

        /* Layout */
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_SHAPE_GEOMETRY).marginTop(8), UI.row(this.type, this.toggleNodeEditor), UI.row(this.sizeX, this.sizeY, this.sizeZ), this.subdivisions);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_SHAPE_APPEARANCE).marginTop(8), this.pickTexture, this.color, this.textureScale, UI.row(this.textureScrollX, this.textureScrollY), this.lighting);
        this.options.add(UI.label(IKey.raw("Particles")).marginTop(8), this.particles, this.particleType, this.particleScale, this.particleDensity, this.particleSize);
    }

    private void updateParticleVisibility()
    {
        boolean visible = this.particles.getValue();
        
        this.particleType.setVisible(visible);
        this.particleScale.setVisible(visible);
        this.particleDensity.setVisible(visible);
        this.particleSize.setVisible(visible);
    }

    @Override
    public void resize()
    {
        super.resize();

        if (this.nodeEditor != null && this.options != null)
        {
            this.nodeEditor.w(this.options.area.x - this.area.x);
            this.nodeEditor.resize();
        }
    }

    @Override
    public void startEdit(ShapeForm form)
    {
        super.startEdit(form);

        this.nodeEditor.setVisible(true);
        this.nodeEditor.setGraph(form.graph.get());

        this.type.setValue(form.type.get().ordinal());
        this.sizeX.setValue(form.sizeX.get());
        this.sizeY.setValue(form.sizeY.get());
        this.sizeZ.setValue(form.sizeZ.get());
        this.subdivisions.setValue(form.subdivisions.get());

        this.color.setColor(form.color.get().getARGBColor());
        this.textureScale.setValue(form.textureScale.get());
        this.textureScrollX.setValue(form.textureScrollX.get());
        this.textureScrollY.setValue(form.textureScrollY.get());
        this.lighting.setValue(form.lighting.get());
        
        this.particles.setValue(form.particles.get());
        this.particleType.setValue(form.particleType.get().ordinal());
        this.particleScale.setValue(form.particleScale.get());
        this.particleDensity.setValue(form.particleDensity.get());
        this.particleSize.setValue(form.particleSize.get());
        this.updateParticleVisibility();
    }
}
