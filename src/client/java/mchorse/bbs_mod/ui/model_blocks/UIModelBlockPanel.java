package mchorse.bbs_mod.ui.model_blocks;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.forms.UIToggleEditorEvent;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.events.UIRemovedEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.model_blocks.camera.ImmersiveModelBlockCameraController;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIModelBlockPanel extends UIDashboardPanel implements IFlightSupported
{
    public static boolean toggleRendering;
    private static int sidebarWidth = 220;

    public UIScrollView scrollView;
    public UIElement editor;
    public UIModelBlockEntityList modelBlocks;
    public UINestedEdit pickEdit;
    public UIToggle enabled;
    public UIToggle shadow;
    public UIToggle hitbox;
    public UIToggle global;
    public UIToggle lookAt;
    public UITrackpad lightLevel;
    public UITrackpad hardness;
    public UIPropTransform transform;
    public UIItemStack mainHand;
    public UIItemStack offHand;
    public UIItemStack armorHead;
    public UIItemStack armorChest;
    public UIItemStack armorLegs;
    public UIItemStack armorFeet;
    public UIElement properties;
    public UIDraggable sidebarResizer;

    private ModelBlockEntity modelBlock;
    private ModelBlockEntity hovered;
    private Vector3f mouseDirection = new Vector3f();

    private Set<ModelBlockEntity> toSave = new HashSet<>();

    private ImmersiveModelBlockCameraController cameraController;
    private UIElement keyDude;

    private UndoManager<UIModelBlockPanel> undoManager = new UndoManager<>(100);
    private MapType pendingUndoBefore;

    public UIModelBlockPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.keyDude = new UIElement().noCulling();
        this.keyDude.keys().register(Keys.MODEL_BLOCKS_MOVE_TO, () ->
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            Camera camera = mc.gameRenderer.getCamera();
            BlockHitResult blockHitResult = RayTracing.rayTrace(mc.world, camera.getPos(), RayTracing.fromVector3f(this.mouseDirection), 512F);

            if (blockHitResult.getType() != HitResult.Type.MISS)
            {
                Vec3d hit = blockHitResult.getPos();
                BlockPos pos = this.modelBlock.getPos();

                this.modelBlock.getProperties().getTransform().translate.set(hit.x - pos.getX() - 0.5F, hit.y - pos.getY(), hit.z - pos.getZ() - 0.5F);
                this.fillData();
            }
        }).active(() -> this.modelBlock != null);

        this.modelBlocks = new UIModelBlockEntityList((l) -> this.fill(l.get(0), false));
        this.modelBlocks.context((menu) ->
        {
            if (this.modelBlock != null)
            {
                menu.action(Icons.EDIT, UIKeys.GENERAL_RENAME, this::renameModelBlock);
                menu.action(UIKeys.MODEL_BLOCKS_KEYS_TELEPORT, this::teleport);
            }
        });
        this.modelBlocks.background();
        this.modelBlocks.h(UIStringList.DEFAULT_HEIGHT * 7);

        this.pickEdit = new UINestedEdit((editing) ->
        {
            UIFormPalette palette = UIFormPalette.open(this, editing, this.modelBlock.getProperties().getForm(), (f) ->
            {
                this.beginUndoCapture();
                this.pickEdit.setForm(f);
                this.modelBlock.getProperties().setForm(f);
                this.endUndoCapture();
            });

            palette.immersive();
            palette.editor.keys().register(Keys.MODEL_BLOCKS_TOGGLE_RENDERING, () -> toggleRendering = !toggleRendering);
            palette.editor.renderer.full(dashboard.getRoot());
            palette.editor.renderer.setTarget(this.modelBlock.getEntity());
            palette.editor.renderer.setRenderForm(() -> !toggleRendering);
            palette.getEvents().register(UIToggleEditorEvent.class, (e) ->
            {
                if (e.editing)
                {
                    this.addCameraController(palette);
                }
                else
                {
                    this.removeCameraController();
                }
            });
            palette.getEvents().register(UIRemovedEvent.class, (e) ->
            {
                this.scrollView.setVisible(true);
                this.sidebarResizer.setVisible(true);
            });

            palette.resize();

            if (editing)
            {
                this.addCameraController(palette);
            }

            this.scrollView.setVisible(false);
            this.sidebarResizer.setVisible(false);
        });
        this.pickEdit.keybinds();

        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setEnabled(b.getValue());
            this.endUndoCapture();
        });
        this.shadow = new UIToggle(UIKeys.MODEL_BLOCKS_SHADOW, (b) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setShadow(b.getValue());
            this.endUndoCapture();
        });
        this.hitbox = new UIToggle(UIKeys.MODEL_BLOCKS_HITBOX, (b) ->
        {
            if (this.modelBlock == null) return;

            this.beginUndoCapture();
            this.modelBlock.getProperties().setHitbox(b.getValue());
            this.endUndoCapture();
        });
        this.global = new UIToggle(UIKeys.MODEL_BLOCKS_GLOBAL, (b) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setGlobal(b.getValue());
            MinecraftClient.getInstance().worldRenderer.reload();
            this.endUndoCapture();
        });
        this.lookAt = new UIToggle(UIKeys.CAMERA_PANELS_LOOK_AT, (b) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setLookAt(b.getValue());
            this.endUndoCapture();
        });

        this.lightLevel = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            this.beginUndoCapture();
            int lvl = v.intValue();

            this.modelBlock.getProperties().setLightLevel(lvl);

            try
            {
                MinecraftClient mc = MinecraftClient.getInstance();

                if (mc.world != null)
                {
                    BlockPos p = this.modelBlock.getPos();
                    BlockState state = mc.world.getBlockState(p);

                    mc.world.setBlockState(p, state.with(ModelBlock.LIGHT_LEVEL, lvl), Block.NOTIFY_LISTENERS);
                }
            }
            catch (Exception e)
            {

            }

            this.endUndoCapture();
        }).integer().limit(0, 15);

        /* Make the trackpad visually distinct: wider and yellow numbers */
        this.lightLevel.textbox.setColor(Colors.YELLOW);
        this.lightLevel.w(1F);

        this.hardness = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            this.beginUndoCapture();
            this.modelBlock.getProperties().setHardness(v.floatValue());
            this.endUndoCapture();
        }).limit(0, 50);
        this.hardness.w(1F);
        this.hardness.textbox.setColor(Colors.PINK);

        this.transform = new UIPropTransform();
        this.transform.callbacks(this::beginUndoCapture, this::endUndoCapture);
        this.transform.enableHotkeys().marginBottom(4);

        this.mainHand = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setItemMainHand(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.MAIN_HAND);
        this.offHand = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setItemOffHand(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.SECONDARY_HAND);
        this.armorHead = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setArmorHead(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.HELMET);
        this.armorChest = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setArmorChest(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.CHESTPLATE);
        this.armorLegs = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setArmorLegs(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.LEGINGS);
        this.armorFeet = new UIItemStack((stack) ->
        {
            if (this.modelBlock == null) return;
            this.beginUndoCapture();
            this.modelBlock.getProperties().setArmorFeet(stack);
            this.endUndoCapture();
        }).optionsOnLeft(true).optionsIcon(Icons.BOOTS);

        UIElement mainHandColumn = UI.column(2, UI.label(UIKeys.MODELS_ITEMS_MAIN), this.mainHand);
        UIElement offHandColumn = UI.column(2, UI.label(UIKeys.MODELS_ITEMS_OFF), this.offHand);
        UIElement armorHeadColumn = UI.column(2, UI.label(IKey.constant("Head")), this.armorHead);
        UIElement armorChestColumn = UI.column(2, UI.label(IKey.constant("Chest")), this.armorChest);
        UIElement armorLegsColumn = UI.column(2, UI.label(IKey.constant("Legs")), this.armorLegs);
        UIElement armorFeetColumn = UI.column(2, UI.label(IKey.constant("Feet")), this.armorFeet);

        mainHandColumn.w(0.5F, -2);
        offHandColumn.w(0.5F, -2);
        armorHeadColumn.w(0.25F, -3);
        armorChestColumn.w(0.25F, -3);
        armorLegsColumn.w(0.25F, -3);
        armorFeetColumn.w(0.25F, -3);

        this.properties = UI.column(4,
            UI.row(5, 0, 20, new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    super.render(context);

                    context.batcher.icon(Icons.LIGHT, Colors.WHITE, this.area.mx(), this.area.my(), 0.5F, 0.5F);
                }
            }.w(20).h(20), this.lightLevel),
            UI.row(5, 0, 20, new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    super.render(context);

                    context.batcher.icon(Icons.PICKAXE, Colors.WHITE, this.area.mx(), this.area.my(), 0.5F, 0.5F);
                }
            }.w(20).h(20), this.hardness),
            UI.row(4, mainHandColumn, offHandColumn),
            UI.row(4, armorHeadColumn, armorChestColumn, armorLegsColumn, armorFeetColumn));
        this.properties.setVisible(true);

        this.editor = UI.column(4,
            this.pickEdit,
            this.enabled,
            this.shadow,
            this.global,
            this.lookAt,
            this.hitbox,
            this.transform,
            new UIButton(UIKeys.MODEL_BLOCKS_PROPERTIES, (b) ->
            {
                properties.toggleVisible();
                UIModelBlockPanel.this.resize();
            })
            {
                @Override
                protected void renderSkin(UIContext context)
                {
                    this.area.render(context.batcher, properties.isVisible() ? Colors.A50 : Colors.A25);

                    if (this.hover)
                    {
                        this.area.render(context.batcher, Colors.A25);
                    }

                    FontRenderer font = context.batcher.getFont();
                    context.batcher.text(this.label.get(), this.area.x + 10, this.area.my(font.getHeight()), Colors.WHITE);

                    context.batcher.icon(properties.isVisible() ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT, Colors.WHITE, this.area.ex() - 10, this.area.my(), 0.5F, 0.5F);
                }
            }.h(16).marginTop(4).marginBottom(2),
            this.properties);

        this.lightLevel.tooltip(UIKeys.MODEL_BLOCKS_LIGHT_LEVEL, Direction.BOTTOM);
        this.hardness.tooltip(UIKeys.MODEL_BLOCKS_HARDNESS, Direction.BOTTOM);

        this.scrollView = UI.scrollView(5, 12, this.modelBlocks, this.editor);
        this.scrollView.scroll.opposite().cancelScrolling();
        this.scrollView.relative(this).w(sidebarWidth).h(1F);
        this.sidebarResizer = new UIDraggable((context) ->
        {
            int min = 180;
            int max = Math.max(min, this.area.w / 2);
            int width = Math.max(min, Math.min(max, context.mouseX - this.area.x));

            sidebarWidth = width;
            this.scrollView.w(width);
            this.scrollView.resize();
            this.sidebarResizer.resize();
        });
        this.sidebarResizer.relative(this.scrollView).x(1F).y(0.5F).w(6).h(40).anchor(0.5F, 0.5F);

        this.fill(null, false);

        this.keys().register(Keys.MODEL_BLOCKS_TELEPORT, this::teleport);
        this.keys().register(Keys.UNDO, this::undoModelBlock).active(() -> this.modelBlock != null);
        this.keys().register(Keys.REDO, this::redoModelBlock).active(() -> this.modelBlock != null);

        this.add(this.scrollView, this.sidebarResizer);
    }

    @Override
    public void resize()
    {
        super.resize();

        int min = 180;
        int max = Math.max(min, this.area.w / 2);

        sidebarWidth = Math.max(min, Math.min(max, sidebarWidth));

        this.scrollView.w(sidebarWidth);
        this.sidebarResizer.resize();
    }

    private void beginUndoCapture()
    {
        if (this.modelBlock == null)
        {
            return;
        }

        if (this.pendingUndoBefore == null)
        {
            this.pendingUndoBefore = this.modelBlock.getProperties().toData();
        }
    }

    private void endUndoCapture()
    {
        if (this.modelBlock == null || this.pendingUndoBefore == null)
        {
            return;
        }

        MapType before = this.pendingUndoBefore;
        this.pendingUndoBefore = null;

        MapType after = this.modelBlock.getProperties().toData();

        if (before.toString().equals(after.toString()))
        {
            return;
        }

        this.undoManager.pushUndo(new ModelBlockPropertiesUndo(this.modelBlock.getPos(), before, after));
        this.toSave.add(this.modelBlock);
    }

    private void applyPropertiesSnapshot(BlockPos pos, MapType data)
    {
        if (this.modelBlock == null || !this.modelBlock.getPos().equals(pos))
        {
            for (ModelBlockEntity candidate : this.modelBlocks.getList())
            {
                if (candidate != null && candidate.getPos().equals(pos))
                {
                    this.modelBlock = candidate;
                    break;
                }
            }
        }

        if (this.modelBlock == null || !this.modelBlock.getPos().equals(pos))
        {
            return;
        }

        this.modelBlock.getProperties().fromData(data);
        this.toSave.add(this.modelBlock);
        this.fillData();
    }

    private void undoModelBlock()
    {
        UIContext context = this.getContext();
        if (context != null && context.isFocused())
        {
            return;
        }

        boolean ok = this.undoManager.undo(this);
        if (ok) UIUtils.playClick();
    }

    private void redoModelBlock()
    {
        UIContext context = this.getContext();
        if (context != null && context.isFocused())
        {
            return;
        }

        boolean ok = this.undoManager.redo(this);
        if (ok) UIUtils.playClick();
    }

    private static class ModelBlockPropertiesUndo implements IUndo<UIModelBlockPanel>
    {
        private final BlockPos pos;
        private final MapType before;
        private MapType after;
        private boolean mergable = true;

        private ModelBlockPropertiesUndo(BlockPos pos, MapType before, MapType after)
        {
            this.pos = pos;
            this.before = before;
            this.after = after;
        }

        @Override
        public IUndo<UIModelBlockPanel> noMerging()
        {
            this.mergable = false;
            return this;
        }

        @Override
        public boolean isMergeable(IUndo<UIModelBlockPanel> undo)
        {
            return this.mergable && undo instanceof ModelBlockPropertiesUndo other && this.pos.equals(other.pos);
        }

        @Override
        public void merge(IUndo<UIModelBlockPanel> undo)
        {
            ModelBlockPropertiesUndo other = (ModelBlockPropertiesUndo) undo;
            this.after = other.after;
        }

        @Override
        public void undo(UIModelBlockPanel context)
        {
            context.applyPropertiesSnapshot(this.pos, this.before);
        }

        @Override
        public void redo(UIModelBlockPanel context)
        {
            context.applyPropertiesSnapshot(this.pos, this.after);
        }
    }

    private void teleport()
    {
        if (this.modelBlock != null)
        {
            BlockPos pos = this.modelBlock.getPos();

            PlayerUtils.teleport(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            UIUtils.playClick();
        }
    }

    private void renameModelBlock()
    {
        if (this.modelBlock == null || this.getContext() == null)
        {
            return;
        }

        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.GENERAL_RENAME,
            UIKeys.PANELS_MODALS_RENAME,
            this::applyModelBlockName
        );

        panel.text.setText(this.modelBlock.getProperties().getName());

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void applyModelBlockName(String name)
    {
        if (this.modelBlock == null)
        {
            return;
        }

        this.modelBlock.getProperties().setName(name);
        this.toSave.add(this.modelBlock);
        this.modelBlocks.update();
        this.save(this.modelBlock);
    }

    @Override
    public boolean supportsRollFOVControl()
    {
        return false;
    }

    @Override
    public void appear()
    {
        super.appear();

        this.getContext().menu.main.add(this.keyDude);
        this.dashboard.orbitKeysUI.setEnabled(() -> this.getChildren(UIFormPalette.class).isEmpty());

        if (this.cameraController != null)
        {
            BBSModClient.getCameraController().add(this.cameraController);
        }
    }

    @Override
    public void disappear()
    {
        super.disappear();

        this.keyDude.removeFromParent();
        this.dashboard.orbitKeysUI.setEnabled(null);

        if (this.cameraController != null)
        {
            BBSModClient.getCameraController().remove(this.cameraController);
        }
    }

    public ModelBlockEntity getModelBlock()
    {
        return this.modelBlock;
    }


    private void addCameraController(UIFormPalette palette)
    {
        if (this.cameraController == null)
        {
            this.cameraController = new ImmersiveModelBlockCameraController(palette.editor.renderer, this.modelBlock);

            BBSModClient.getCameraController().add(this.cameraController);

            Transform transform = this.modelBlock.getProperties().getTransform().copy();

            transform.translate.set(0F, 0F, 0F);
            palette.editor.renderer.setTransform(new Matrix4f(transform.createMatrix()));
        }
    }

    private void removeCameraController()
    {
        if (this.cameraController != null)
        {
            BBSModClient.getCameraController().remove(this.cameraController);

            this.cameraController = null;
        }
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canPause()
    {
        return false;
    }

    @Override
    public void open()
    {
        super.open();

        this.updateList();

        if (this.modelBlock != null && this.modelBlock.isRemoved())
        {
            this.fill(null, true);
        }
    }

    @Override
    public void close()
    {
        super.close();

        this.removeCameraController();

        for (ModelBlockEntity entity : this.toSave)
        {
            this.save(entity);
        }

        this.toSave.clear();
    }

    private void updateList()
    {
        this.modelBlocks.clear();

        for (ModelBlockEntity modelBlock : BBSRendering.capturedModelBlocks)
        {
            this.modelBlocks.add(modelBlock);
        }

        this.fill(this.modelBlock, true);
    }

    public void fill(ModelBlockEntity modelBlock, boolean select)
    {
        if (modelBlock != null)
        {
            this.toSave.add(modelBlock);
        }

        if (this.modelBlock != modelBlock)
        {
            this.undoManager = new UndoManager<>(100);
            this.pendingUndoBefore = null;
        }

        this.modelBlock = modelBlock;

        if (modelBlock != null)
        {
            this.fillData();
        }

        this.editor.setVisible(modelBlock != null);

        if (select)
        {
            this.modelBlocks.setCurrentScroll(modelBlock);
        }
    }

    private void fillData()
    {
        ModelProperties properties = this.modelBlock.getProperties();

        this.pickEdit.setForm(properties.getForm());
        this.transform.setTransform(properties.getTransform());
        this.enabled.setValue(properties.isEnabled());
        this.shadow.setValue(properties.isShadow());
        this.hitbox.setValue(properties.isHitbox());
        this.global.setValue(properties.isGlobal());
        this.lookAt.setValue(properties.isLookAt());
        this.lightLevel.setValue(properties.getLightLevel());
        this.hardness.setValue(properties.getHardness());

        this.mainHand.setStack(properties.getItemMainHand());
        this.offHand.setStack(properties.getItemOffHand());
        this.armorHead.setStack(properties.getArmorHead());
        this.armorChest.setStack(properties.getArmorChest());
        this.armorLegs.setStack(properties.getArmorLegs());
        this.armorFeet.setStack(properties.getArmorFeet());
    }

    private void save(ModelBlockEntity modelBlock)
    {
        if (modelBlock != null)
        {
            ClientNetwork.sendModelBlockForm(modelBlock.getPos(), modelBlock);
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (super.subMouseClicked(context))
        {
            return true;
        }

        if (this.hovered != null && context.mouseButton == 0 && BBSSettings.clickModelBlocks.get())
        {
            this.fill(this.hovered, true);
        }

        return false;
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        String label = UIKeys.FILM_CONTROLLER_SPEED.format(this.dashboard.orbit.speed.getValue()).get();
        FontRenderer font = context.batcher.getFont();
        int w = font.getWidth(label);
        int x = this.area.w - w - 5;
        int y = this.area.ey() - font.getHeight() - 5;

        context.batcher.textCard(label, x, y, Colors.WHITE, Colors.A50);
        super.render(context);
    }

    @Override
    public void renderInWorld(WorldRenderContext context)
    {
        super.renderInWorld(context);

        Camera camera = context.camera();
        Vec3d pos = camera.getPos();

        MinecraftClient mc = MinecraftClient.getInstance();
        double x = mc.mouse.getX();
        double y = mc.mouse.getY();

        this.mouseDirection.set(CameraUtils.getMouseDirection(
            RenderSystem.getProjectionMatrix(),
            context.matrixStack().peek().getPositionMatrix(),
            (int) x, (int) y, 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()
        ));
        this.hovered = this.getClosestObject(new Vector3d(pos.x, pos.y, pos.z), this.mouseDirection);

        RenderSystem.enableDepthTest();

        for (ModelBlockEntity entity : this.modelBlocks.getList())
        {
            BlockPos blockPos = entity.getPos();

            if (!this.isEditing(entity))
            {
                context.matrixStack().push();
                context.matrixStack().translate(blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z);

                if (this.hovered == entity || entity == this.modelBlock)
                {
                    Draw.renderBox(context.matrixStack(), 0D, 0D, 0D, 1D, 1D, 1D, 0, 0.5F, 1F);
                }
                else
                {
                    Draw.renderBox(context.matrixStack(), 0D, 0D, 0D, 1D, 1D, 1D);
                }

                context.matrixStack().pop();
            }
        }

        RenderSystem.disableDepthTest();
    }

    private ModelBlockEntity getClosestObject(Vector3d finalPosition, Vector3f mouseDirection)
    {
        ModelBlockEntity closest = null;

        for (ModelBlockEntity object : this.modelBlocks.getList())
        {
            AABB aabb = this.getHitbox(object);

            if (aabb.intersectsRay(finalPosition, mouseDirection))
            {
                if (closest == null)
                {
                    closest = object;
                }
                else
                {
                    AABB aabb2 = this.getHitbox(closest);

                    if (finalPosition.distanceSquared(aabb.x, aabb.y, aabb.z) < finalPosition.distanceSquared(aabb2.x, aabb2.y, aabb2.z))
                    {
                        closest = object;
                    }
                }
            }
        }
        return closest;
    }

    private AABB getHitbox(ModelBlockEntity closest)
    {
        BlockPos pos = closest.getPos();

        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        double w = 1D;
        double h = 1D;
        double d = 1D;

        if (closest.getProperties().isHitbox())
        {
            Form form = closest.getProperties().getForm();

            if (form != null && form.hitbox.get())
            {
                float width = form.hitboxWidth.get();
                float height = form.hitboxHeight.get();

                if (width > 0F && height > 0F)
                {
                    float halfWidth = width / 2F;

                    double minX = x + 0.5D - halfWidth;
                    double maxX = x + 0.5D + halfWidth;
                    double minZ = z + 0.5D - halfWidth;
                    double maxZ = z + 0.5D + halfWidth;
                    double minY = y;
                    double maxY = y + height;

                    double clampedMinX = Math.max(x, minX);
                    double clampedMinZ = Math.max(z, minZ);
                    double clampedMaxX = Math.min(x + 1D, maxX);
                    double clampedMaxZ = Math.min(z + 1D, maxZ);
                    double clampedMaxY = Math.min(y + 1D, maxY);

                    if (clampedMinX < clampedMaxX && clampedMinZ < clampedMaxZ && clampedMaxY > minY)
                    {
                        x = clampedMinX;
                        y = minY;
                        z = clampedMinZ;
                        w = clampedMaxX - clampedMinX;
                        h = clampedMaxY - minY;
                        d = clampedMaxZ - clampedMinZ;
                    }
                }
            }
        }

        return new AABB(x, y, z, w, h, d);
    }

    public boolean isEditing(ModelBlockEntity entity)
    {
        if (this.modelBlock == entity)
        {
            List<UIFormPalette> children = this.getChildren(UIFormPalette.class);

            if (!children.isEmpty())
            {
                return children.get(0).editor.isEditing();
            }
        }

        return false;
    }
}
