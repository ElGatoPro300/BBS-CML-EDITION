package mchorse.bbs_mod.ui.particles.sections;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.particles.ParticleMaterial;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.components.appearance.CameraFacing;
import mchorse.bbs_mod.particles.components.appearance.ParticleComponentAppearanceBillboard;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIParticleSchemeGeneralSection extends UIParticleSchemeSection
{
    public UITextbox identifier;
    public UIButton pick;
    public UIIcons material;
    public UIIcons facing;

    public UIParticleSchemeGeneralSection(UIParticleSchemePanel parent)
    {
        super(parent);

        this.identifier = new UITextbox(100, (str) ->
        {
            this.scheme.identifier = str;
            this.editor.dirty();
        });
        this.identifier.tooltip(UIKeys.SNOWSTORM_GENERAL_IDENTIFIER);

        this.pick = new UIButton(UIKeys.SNOWSTORM_GENERAL_PICK, (b) ->
        {
            UITexturePicker.open(this.getContext(), this.scheme.texture, (link) ->
            {
                if (link == null)
                {
                    link = ParticleScheme.DEFAULT_TEXTURE;
                }

                this.setTextureSize(link);
                this.scheme.texture = link;
                this.editor.dirty();
            });
        });

        this.material = new UIIcons((b) ->
        {
            this.scheme.material = ParticleMaterial.values()[this.material.getValue()];
            this.editor.dirty();
        });
        this.material.add(Icons.SQUARE, UIKeys.SNOWSTORM_GENERAL_PARTICLES_OPAQUE);
        this.material.add(Icons.DROP, UIKeys.SNOWSTORM_GENERAL_PARTICLES_ALPHA);
        this.material.add(Icons.FADING, UIKeys.SNOWSTORM_GENERAL_PARTICLES_BLEND);

        this.facing = new UIIcons((b) ->
        {
            this.getBillboard().facing = CameraFacing.values()[this.facing.getValue()];
            this.editor.dirty();
        });
        this.facing.add(Icons.ALL_DIRECTIONS, UIKeys.C_CAMERA_FACING.get(CameraFacing.ROTATE_XYZ.id));
        this.facing.add(Icons.ORBIT, UIKeys.C_CAMERA_FACING.get(CameraFacing.ROTATE_Y.id));
        this.facing.add(Icons.LOOKING, UIKeys.C_CAMERA_FACING.get(CameraFacing.LOOKAT_XYZ.id));
        this.facing.add(Icons.CAMERA, UIKeys.C_CAMERA_FACING.get(CameraFacing.LOOKAT_Y.id));
        this.facing.add(Icons.X, UIKeys.C_CAMERA_FACING.get(CameraFacing.DIRECTION_X.id));
        this.facing.add(Icons.Y, UIKeys.C_CAMERA_FACING.get(CameraFacing.DIRECTION_Y.id));
        this.facing.add(Icons.Z, UIKeys.C_CAMERA_FACING.get(CameraFacing.DIRECTION_Z.id));

        this.fields.add(this.identifier, UI.row(5, 0, 20, this.pick, this.material));
        this.fields.add(UI.label(UIKeys.SNOWSTORM_GENERAL_FACING, 20).labelAnchor(0, 1F).marginTop(UIConstants.MARGIN));
        this.fields.add(this.facing);
    }

    private ParticleComponentAppearanceBillboard getBillboard()
    {
        return this.scheme.getOrCreate(ParticleComponentAppearanceBillboard.class);
    }

    private void setTextureSize(Link link)
    {
        ParticleComponentAppearanceBillboard component = this.scheme.get(ParticleComponentAppearanceBillboard.class);

        if (component == null)
        {
            return;
        }

        Texture texture = BBSModClient.getTextures().getTexture(link);

        component.textureWidth = texture.width;
        component.textureHeight = texture.height;
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.SNOWSTORM_GENERAL_TITLE;
    }

    @Override
    public void setScheme(ParticleScheme scheme)
    {
        super.setScheme(scheme);

        this.identifier.setText(scheme.identifier);
        this.material.setValue(scheme.material.ordinal());
        this.facing.setValue(this.getBillboard().facing.ordinal());
    }
}