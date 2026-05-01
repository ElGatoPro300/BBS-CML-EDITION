package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class UIModelPartsSection extends UIModelSection
{
    public UIButton texture;
    public UIButton openModel;
    public UIColor color;
    public UIPoseEditor poseEditor;

    public UIModelPartsSection(UIModelPanel editor)
    {
        super(editor);
        
        this.texture = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            if (this.config != null)
            {
                UITexturePicker.open(b.getContext(), this.config.texture.get(), (l) ->
                {
                    this.config.texture.set(l);
                    this.editor.dirty();
                });
            }
        });

        this.openModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_OPEN_IN, this::openModel);

        this.color = new UIColor((c) ->
        {
            if (this.config != null)
            {
                this.config.color.set(Colors.A100 | c);
                this.editor.dirty();
            }
        });
        
        this.poseEditor = new UIPoseEditor();
        this.poseEditor.onChange = this.editor::dirty;
        this.poseEditor.pickCallback = (bone) ->
        {
            this.editor.renderer.setSelectedBone(bone);

            for (UIModelSection section : this.editor.sections)
            {
                if (section != this)
                {
                    section.deselect();
                }

                section.onBoneSelected(bone);
            }
        };
        this.poseEditor.prepend(this.color);
        this.poseEditor.prepend(this.texture);
        this.poseEditor.prepend(this.openModel);
    }
    
    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.title.area.isInside(context) && context.mouseButton == 0)
        {
            this.editor.setRight(this.poseEditor);
        }
        
        return super.subMouseClicked(context);
    }

    public void selectBone(String bone)
    {
        this.poseEditor.selectBone(bone);
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_PARTS;
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        String oldId = this.config == null ? null : this.config.getId();

        super.setConfig(config);
        
        if (config != null)
        {
            this.color.setColor(config.color.get());

            String group = config.poseGroup.get();

            this.poseEditor.setPose(config.parts.get(), group.isEmpty() ? config.getId() : group);
            
            ModelInstance model = BBSModClient.getModels().getModel(config.getId());
            
            if (model != null)
            {
                this.poseEditor.fillGroups(model.getModel().getAllGroupKeys(), oldId == null || !oldId.equals(config.getId()));
                this.poseEditor.setDefaultTextureSupplier(() -> model.texture);
            }
        }
    }

    private void openModel(UIButton b)
    {
        if (this.config == null)
        {
            return;
        }

        String modelId = this.config.getId();
        File folder = BBSMod.getProvider().getFile(Link.assets("models/" + modelId));

        if ((folder == null || !folder.exists()) && BBSMod.class.getClassLoader() != null)
        {
            URL url = BBSMod.class.getClassLoader().getResource("assets/bbs/assets/models/" + modelId);

            if (url != null)
            {
                try
                {
                    folder = Paths.get(url.toURI()).toFile();
                }
                catch (Exception e)
                {}
            }
        }

        if (folder != null && folder.isDirectory())
        {
            File target = this.findModelFile(folder);

            if (target != null)
            {
                try
                {
                    Desktop.getDesktop().open(target);
                }
                catch (Throwable e)
                {
                    UIUtils.openFolder(target);
                }
            }
        }
    }

    private File findModelFile(File folder)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return null;
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".bbmodel"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".geo.json"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().equals("model.json"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".json") && !f.getName().equals("config.json"))
            {
                return f;
            }
        }

        return null;
    }
}
