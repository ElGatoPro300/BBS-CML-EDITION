package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.camera.OrbitDistanceCamera;
import mchorse.bbs_mod.camera.controller.OrbitCameraController;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCamera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

public class UIModelArmorTransformEditor extends UIDashboardPanel
{
    public UIModelPanel parent;
    public ModelConfig config;

    public UIPropTransform transform;
    public UILabel armorLabel;
    public UISearchList<String> armorSearch;
    public UIStringList armorList;
    public UIIcon back;

    /* Camera */
    public UIOrbitCamera uiOrbitCamera;
    public OrbitCameraController orbitCameraController;

    private Perspective lastPerspective;
    private Form lastForm;
    private boolean changed;
    private ModelInstance cachedModel;

    public UIModelArmorTransformEditor(UIModelPanel parent, ModelConfig config)
    {
        super(parent.dashboard);

        this.parent = parent;
        this.config = config;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        OrbitDistanceCamera orbit = new OrbitDistanceCamera();

        orbit.distance.setX(30);
        orbit.setFovRoll(false);
        this.uiOrbitCamera = new UIOrbitCamera();
        this.uiOrbitCamera.setControl(true);
        this.uiOrbitCamera.orbit = orbit;

        this.orbitCameraController = new OrbitCameraController(this.uiOrbitCamera.orbit);
        this.orbitCameraController.camera.position.set(player.getPos().x, player.getPos().y + 1D, player.getPos().z);
        this.orbitCameraController.camera.rotation.set(0, MathUtils.toRad(player.bodyYaw), 0);

        this.armorLabel = UI.label(UIKeys.MODELS_ARMOR).background(() -> Colors.A50 | BBSSettings.primaryColor.get());
        this.armorList = new UIStringList((l) ->
        {
            int index = this.armorList.getCurrentIndices().isEmpty() ? 0 : this.armorList.getCurrentIndices().get(0);
            
            if (index >= 0 && index < ArmorType.values().length)
            {
                this.setSlot(this.config.armorSlots.get(ArmorType.values()[index]));
            }
        })
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }
        };
        this.armorList.background = 0x88000000;
        
        for (ArmorType type : ArmorType.values())
        {
            this.armorList.add(type.name().toLowerCase());
        }
        
        this.armorList.setIndex(0);

        this.armorSearch = new UISearchList<>(this.armorList);
        this.armorSearch.label(UIKeys.GENERAL_SEARCH);

        this.transform = new UIPropTransform();
        this.transform.callbacks(null, () ->
        {
            this.syncModel();
        });
        this.transform.relative(this).x(1F, -200).y(0.5F, 10).w(190).h(70);

        this.back = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.parent.renderer.dirty();
            this.dashboard.setPanel(this.parent);
        });
        this.back.relative(this).x(1F, -26).y(6);

        this.armorSearch.relative(this.transform).x(0.5F).y(0F, -5).w(1F).h(80).anchor(0.5F, 1F);
        this.armorLabel.relative(this.armorSearch).y(-12).w(1F).h(12);

        this.add(this.uiOrbitCamera, this.transform, this.armorSearch, this.armorLabel, this.back);

        this.setSlot(this.config.armorSlots.get(ArmorType.values()[0]));
    }

    private void setSlot(ArmorSlot slot)
    {
        this.transform.setTransform(slot.transform);
    }

    private void acquireModel()
    {
        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        if (morph != null && morph.getForm() instanceof ModelForm)
        {
            FormRenderer renderer = FormUtilsClient.getRenderer(morph.getForm());

            if (renderer instanceof ModelFormRenderer)
            {
                this.cachedModel = ((ModelFormRenderer) renderer).getModel();
                this.syncModel();
            }
        }
    }

    private void syncModel()
    {
        if (this.cachedModel != null)
        {
            for (ArmorType type : ArmorType.values())
            {
                ArmorSlot slot = this.cachedModel.armorSlots.get(type);
                ArmorSlot configSlot = this.config.armorSlots.get(type);

                if (slot != null && configSlot != null)
                {
                    slot.transform.copy(configSlot.transform);
                }
            }
        }
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canHideHUD()
    {
        return false;
    }

    @Override
    public UIDashboardPanel getMainPanel()
    {
        return this.parent;
    }

    @Override
    public void render(UIContext context)
    {
        if (this.cachedModel == null)
        {
            this.acquireModel();
        }

        super.render(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.getKeyCode() == GLFW.GLFW_KEY_ESCAPE)
        {
            this.parent.renderer.dirty();
            this.dashboard.setPanel(this.parent);
            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void appear()
    {
        super.appear();

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        this.lastPerspective = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        mc.options.hudHidden = false;

        BBSModClient.getCameraController().remove(this.dashboard.camera);
        BBSModClient.getCameraController().add(this.orbitCameraController);

        this.orbitCameraController.camera.position.set(player.getPos().x, player.getPos().y + 1D, player.getPos().z);
        this.orbitCameraController.camera.rotation.set(0, MathUtils.toRad(player.bodyYaw), 0);
        ((OrbitDistanceCamera) this.uiOrbitCamera.orbit).distance.setX(14);

        Morph morph = Morph.getMorph(mc.player);

        if (morph != null)
        {
            this.lastForm = morph.getForm();
            this.changed = true;

            ModelForm form = new ModelForm();

            form.model.set(this.config.getId());
            morph.setForm(form);
        }

        this.acquireModel();
    }

    @Override
    public void disappear()
    {
        super.disappear();

        this.parent.forceSave();
        this.restore();

        MinecraftClient.getInstance().options.hudHidden = true;

        BBSModClient.getCameraController().remove(this.orbitCameraController);
        BBSModClient.getCameraController().add(this.dashboard.camera);
    }

    private void restore()
    {
        if (this.changed)
        {
            Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

            if (morph != null)
            {
                morph.setForm(this.lastForm);
            }
        }

        MinecraftClient.getInstance().options.setPerspective(this.lastPerspective);
    }
}
