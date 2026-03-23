package mchorse.bbs_mod.ui.model_blocks;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.client.BBSRendering;
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
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.events.UIRemovedEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.model_blocks.camera.ImmersiveModelBlockCameraController;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.MathUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIModelBlockPanel extends UIDashboardPanel implements IFlightSupported
{
    public static boolean toggleRendering;

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
    public UITrackpad hitboxPos1X;
    public UITrackpad hitboxPos1Y;
    public UITrackpad hitboxPos1Z;
    public UITrackpad hitboxPos2X;
    public UITrackpad hitboxPos2Y;
    public UITrackpad hitboxPos2Z;
    public UIPropTransform transform;

    private ModelBlockEntity modelBlock;
    private ModelBlockEntity hovered;
    private Vector3f mouseDirection = new Vector3f();

    private Set<ModelBlockEntity> toSave = new HashSet<>();

    private ImmersiveModelBlockCameraController cameraController;
    private UIElement keyDude;

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
            if (this.modelBlock != null) menu.action(UIKeys.MODEL_BLOCKS_KEYS_TELEPORT, this::teleport);
        });
        this.modelBlocks.background();
        this.modelBlocks.h(UIStringList.DEFAULT_HEIGHT * 9);

        this.pickEdit = new UINestedEdit((editing) ->
        {
            UIFormPalette palette = UIFormPalette.open(this, editing, this.modelBlock.getProperties().getForm(), (f) ->
            {
                this.pickEdit.setForm(f);
                this.modelBlock.getProperties().setForm(f);
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
            });

            palette.resize();

            if (editing)
            {
                this.addCameraController(palette);
            }

            this.scrollView.setVisible(false);
        });
        this.pickEdit.keybinds();

        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) -> this.modelBlock.getProperties().setEnabled(b.getValue()));
        this.shadow = new UIToggle(UIKeys.MODEL_BLOCKS_SHADOW, (b) -> this.modelBlock.getProperties().setShadow(b.getValue()));
        this.hitbox = new UIToggle(UIKeys.MODEL_BLOCKS_HITBOX, (b) ->
        {
            if (this.modelBlock == null) return;

            this.modelBlock.getProperties().setHitbox(b.getValue());
            this.updateHitboxControls();
        });
        this.global = new UIToggle(UIKeys.MODEL_BLOCKS_GLOBAL, (b) ->
        {
            this.modelBlock.getProperties().setGlobal(b.getValue());
            MinecraftClient.getInstance().worldRenderer.reload();
        });
        this.lookAt = new UIToggle(UIKeys.CAMERA_PANELS_LOOK_AT, (b) -> this.modelBlock.getProperties().setLookAt(b.getValue()));

        this.lightLevel = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

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
        }).integer().limit(0, 15);

        /* Make the trackpad visually distinct: wider and yellow numbers */
        this.lightLevel.textbox.setColor(Colors.YELLOW);
        this.lightLevel.w(1F);

        this.hardness = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            this.modelBlock.getProperties().setHardness(v.floatValue());
        }).limit(0, 50);
        this.hardness.w(1F);
        this.hardness.textbox.setColor(Colors.PINK);

        IKey hitboxTooltip = IKey.constant("%s (%s)");

        this.hitboxPos1X = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p1 = this.modelBlock.getProperties().getHitboxPos1();
            this.modelBlock.getProperties().setHitboxPos1(v.floatValue(), p1.y, p1.z);
        }).limit(0, 1);
        this.hitboxPos1X.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS1, UIKeys.GENERAL_X));
        this.hitboxPos1X.textbox.setColor(Colors.RED);

        this.hitboxPos1Y = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p1 = this.modelBlock.getProperties().getHitboxPos1();
            this.modelBlock.getProperties().setHitboxPos1(p1.x, v.floatValue(), p1.z);
        }).limit(0, 1);
        this.hitboxPos1Y.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS1, UIKeys.GENERAL_Y));
        this.hitboxPos1Y.textbox.setColor(Colors.GREEN);

        this.hitboxPos1Z = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p1 = this.modelBlock.getProperties().getHitboxPos1();
            this.modelBlock.getProperties().setHitboxPos1(p1.x, p1.y, v.floatValue());
        }).limit(0, 1);
        this.hitboxPos1Z.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS1, UIKeys.GENERAL_Z));
        this.hitboxPos1Z.textbox.setColor(Colors.BLUE);

        this.hitboxPos2X = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p2 = this.modelBlock.getProperties().getHitboxPos2();
            this.modelBlock.getProperties().setHitboxPos2(v.floatValue(), p2.y, p2.z);
        }).limit(0, 1);
        this.hitboxPos2X.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS2, UIKeys.GENERAL_X));
        this.hitboxPos2X.textbox.setColor(Colors.RED);

        this.hitboxPos2Y = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p2 = this.modelBlock.getProperties().getHitboxPos2();
            this.modelBlock.getProperties().setHitboxPos2(p2.x, v.floatValue(), p2.z);
        }).limit(0, 1);
        this.hitboxPos2Y.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS2, UIKeys.GENERAL_Y));
        this.hitboxPos2Y.textbox.setColor(Colors.GREEN);

        this.hitboxPos2Z = new UITrackpad((v) ->
        {
            if (this.modelBlock == null) return;

            Vector3f p2 = this.modelBlock.getProperties().getHitboxPos2();
            this.modelBlock.getProperties().setHitboxPos2(p2.x, p2.y, v.floatValue());
        }).limit(0, 1);
        this.hitboxPos2Z.tooltip(hitboxTooltip.format(UIKeys.MODEL_BLOCKS_HITBOX_POS2, UIKeys.GENERAL_Z));
        this.hitboxPos2Z.textbox.setColor(Colors.BLUE);

        this.transform = new UIPropTransform();
        this.transform.enableHotkeys();

        UIIcon hitboxIcon1 = new UIIcon(Icons.BLOCK, null);
        UIIcon hitboxIcon2 = new UIIcon(Icons.BLOCK, null);
        hitboxIcon1.iconColor = hitboxIcon1.hoverColor = hitboxIcon1.activeColor = hitboxIcon1.disabledColor = Colors.WHITE;
        hitboxIcon2.iconColor = hitboxIcon2.hoverColor = hitboxIcon2.activeColor = hitboxIcon2.disabledColor = Colors.WHITE;

        this.editor = UI.column(
            this.pickEdit,
            this.enabled,
            this.shadow,
            this.global,
            this.lookAt,
            this.hitbox,
            this.transform,
            UI.row(hitboxIcon1, this.hitboxPos1X, this.hitboxPos1Y, this.hitboxPos1Z),
            UI.row(hitboxIcon2, this.hitboxPos2X, this.hitboxPos2Y, this.hitboxPos2Z),
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
            }.w(20).h(20), this.hardness));

        this.lightLevel.tooltip(UIKeys.MODEL_BLOCKS_LIGHT_LEVEL, Direction.BOTTOM);
        this.hardness.tooltip(UIKeys.MODEL_BLOCKS_HARDNESS, Direction.BOTTOM);

        this.scrollView = UI.scrollView(5, 10, this.modelBlocks, this.editor);
        this.scrollView.scroll.opposite().cancelScrolling();
        this.scrollView.relative(this).w(200).h(1F);

        this.fill(null, false);

        this.keys().register(Keys.MODEL_BLOCKS_TELEPORT, this::teleport);

        this.add(this.scrollView);
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

    private void updateHitboxControls()
    {
        if (this.modelBlock == null)
        {
            this.hitboxPos1X.setEnabled(false);
            this.hitboxPos1Y.setEnabled(false);
            this.hitboxPos1Z.setEnabled(false);
            this.hitboxPos2X.setEnabled(false);
            this.hitboxPos2Y.setEnabled(false);
            this.hitboxPos2Z.setEnabled(false);

            return;
        }

        this.hitboxPos1X.setEnabled(true);
        this.hitboxPos1Y.setEnabled(true);
        this.hitboxPos1Z.setEnabled(true);
        this.hitboxPos2X.setEnabled(true);
        this.hitboxPos2Y.setEnabled(true);
        this.hitboxPos2Z.setEnabled(true);
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

        Vector3f p1 = properties.getHitboxPos1();
        Vector3f p2 = properties.getHitboxPos2();

        this.hitboxPos1X.setValue(p1.x);
        this.hitboxPos1Y.setValue(p1.y);
        this.hitboxPos1Z.setValue(p1.z);

        this.hitboxPos2X.setValue(p2.x);
        this.hitboxPos2Y.setValue(p2.y);
        this.hitboxPos2Z.setValue(p2.z);

        this.updateHitboxControls();
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

        MatrixStack matrixStack = context.matrixStack();
        Matrix4f positionMatrix = matrixStack != null ? matrixStack.peek().getPositionMatrix() : RenderSystem.getModelViewMatrix();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        float m11 = projectionMatrix.m11();
        float tanHalfFov = 1.0f / m11;
        float aspect = m11 / projectionMatrix.m00();

        float ndcX = ((float) x / mc.getWindow().getWidth()) * 2.0f - 1.0f;
        float ndcY = -(((float) y / mc.getWindow().getHeight()) * 2.0f - 1.0f);

        float f = MathUtils.toRad(camera.getPitch());
        float g = MathUtils.toRad(-camera.getYaw());
        float h = (float) Math.cos(g);
        float i = (float) Math.sin(g);
        float j = (float) Math.cos(f);
        float k = (float) Math.sin(f);
        Vector3f forward = new Vector3f(i * j, -k, h * j);
        Vector3f upWorld = new Vector3f(0F, 1F, 0F);
        Vector3f right = new Vector3f(forward).cross(upWorld).normalize();
        Vector3f upCam = new Vector3f(right).cross(forward).normalize();

        Vector3f direction = new Vector3f(forward)
            .add(new Vector3f(right).mul(ndcX * tanHalfFov * aspect))
            .add(new Vector3f(upCam).mul(ndcY * tanHalfFov))
            .normalize();

        this.mouseDirection.set(direction);
        this.hovered = this.getClosestObject(new Vector3d(pos.x, pos.y, pos.z), this.mouseDirection);

        RenderSystem.enableDepthTest();

        for (ModelBlockEntity entity : this.modelBlocks.getList())
        {
            if (!this.isEditing(entity))
            {
                AABB aabb = this.getHitbox(entity);

                context.matrixStack().push();
                context.matrixStack().translate(aabb.x - pos.x, aabb.y - pos.y, aabb.z - pos.z);

                if (this.hovered == entity || entity == this.modelBlock)
                {
                    Draw.renderBox(context.matrixStack(), 0D, 0D, 0D, aabb.w, aabb.h, aabb.d, 0, 0.5F, 1F);
                }
                else
                {
                    Draw.renderBox(context.matrixStack(), 0D, 0D, 0D, aabb.w, aabb.h, aabb.d);
                }

                context.matrixStack().pop();
            }
        }

        RenderSystem.disableDepthTest();
    }

    private ModelBlockEntity getClosestObject(Vector3d finalPosition, Vector3f mouseDirection)
    {
        ModelBlockEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ModelBlockEntity object : this.modelBlocks.getList())
        {
            BlockPos pos = object.getPos();
            Vector3d relOrigin = new Vector3d(finalPosition).sub(pos.getX(), pos.getY(), pos.getZ());

            Matrix4f transform = object.getProperties().getTransform().createMatrix();
            Matrix4f invTransform = new Matrix4f(transform).invert();

            Vector4f origin4 = new Vector4f((float) relOrigin.x, (float) relOrigin.y, (float) relOrigin.z, 1.0F);
            Vector4f dir4 = new Vector4f(mouseDirection.x, mouseDirection.y, mouseDirection.z, 0.0F);

            /* Since the hitbox in the renderInWorld method is not transformed, we shouldn't
             * transform the ray either. This was causing the selection to fail when the
             * model block had a transformation. */

            Vector3d localOrigin = new Vector3d(origin4.x, origin4.y, origin4.z);
            Vector3f localDir = new Vector3f(dir4.x, dir4.y, dir4.z);

            AABB unitBox = new AABB(0, 0, 0, 1, 1, 1);
            Vector2d farNear = new Vector2d();

            if (unitBox.intersectsRay(localOrigin, localDir, farNear))
            {
                double t = farNear.x;

                if (t < 0)
                {
                    if (farNear.y < 0)
                    {
                        continue;
                    }

                    t = farNear.y;
                }

                Vector3f hitLocal = new Vector3f(localDir).mul((float) t).add(new Vector3f((float) localOrigin.x, (float) localOrigin.y, (float) localOrigin.z));
                Vector4f hitRel = new Vector4f(hitLocal, 1.0F);

                transform.transform(hitRel);

                Vector3d hitWorld = new Vector3d(hitRel.x, hitRel.y, hitRel.z).add(pos.getX(), pos.getY(), pos.getZ());
                double dist = finalPosition.distanceSquared(hitWorld);

                if (dist < closestDist)
                {
                    closestDist = dist;
                    closest = object;
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

        ModelProperties properties = closest.getProperties();

        Vector3f p1 = properties.getHitboxPos1();
        Vector3f p2 = properties.getHitboxPos2();

        double minX = Math.min(p1.x, p2.x);
        double minY = Math.min(p1.y, p2.y);
        double minZ = Math.min(p1.z, p2.z);
        double maxX = Math.max(p1.x, p2.x);
        double maxY = Math.max(p1.y, p2.y);
        double maxZ = Math.max(p1.z, p2.z);

        minX = Math.max(0D, minX);
        minY = Math.max(0D, minY);
        minZ = Math.max(0D, minZ);
        maxX = Math.min(1D, maxX);
        maxY = Math.min(1D, maxY);
        maxZ = Math.min(1D, maxZ);

        if (minX < maxX && minY < maxY && minZ < maxZ)
        {
            x += minX;
            y += minY;
            z += minZ;
            w = maxX - minX;
            h = maxY - minY;
            d = maxZ - minZ;
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
