package mchorse.bbs_mod.ui.triggers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.client.renderer.TriggerBlockEntityRenderer;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class UITriggerBlockPanel extends UIDashboardPanel implements IFlightSupported
{
    public UIScrollView scrollView;
    public UITriggerBlockEntityList list;
    public UITriggerEditor editor;

    private TriggerBlockEntity entity;
    private TriggerBlockEntity hovered;
    private Set<TriggerBlockEntity> toSave = new HashSet<>();

    public UITriggerBlockPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.list = new UITriggerBlockEntityList((l) -> this.fill(l.get(0), false));
        this.list.context((menu) ->
        {
            if (this.entity != null) menu.action(UIKeys.MODEL_BLOCKS_KEYS_TELEPORT, this::teleport);
        });
        this.list.background();
        this.list.h(UIStringList.DEFAULT_HEIGHT * 9);

        this.editor = new UITriggerEditor();
        this.editor.setVisible(false);

        this.scrollView = UI.scrollView(5, 10, this.list, this.editor);
        this.scrollView.scroll.opposite().cancelScrolling();
        this.scrollView.relative(this).w(200).h(1F);

        this.add(this.scrollView);

        this.keys().register(Keys.MODEL_BLOCKS_TELEPORT, this::teleport);
    }

    private void teleport()
    {
        if (this.entity != null)
        {
            BlockPos pos = this.entity.getPos();

            PlayerUtils.teleport(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            UIUtils.playClick();
        }
    }

    public void fill(TriggerBlockEntity entity, boolean select)
    {
        if (this.entity != null)
        {
            this.toSave.add(this.entity);
        }

        this.entity = entity;
        this.editor.setEntity(entity);
        this.editor.setVisible(entity != null);

        if (select)
        {
            this.list.setCurrentScroll(entity);
        }
    }

    @Override
    public void close()
    {
        super.close();

        if (this.entity != null)
        {
            this.toSave.add(this.entity);
        }

        for (TriggerBlockEntity entity : this.toSave)
        {
            this.save(entity);
        }

        this.toSave.clear();
    }

    private void save(TriggerBlockEntity entity)
    {
        if (entity != null)
        {
            ClientNetwork.sendTriggerBlockUpdate(entity.getPos(), entity);
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
    public boolean supportsRollFOVControl()
    {
        return false;
    }

    @Override
    public void open()
    {
        super.open();
        this.updateList();
    }

    @Override
    public void appear()
    {
        super.appear();
    }

    @Override
    public void disappear()
    {
        super.disappear();
    }

    private void updateList()
    {
        this.list.clear();
        this.list.add(TriggerBlockEntityRenderer.capturedTriggerBlocks);

        if (this.entity != null && !this.entity.isRemoved())
        {
            if (!this.list.getList().contains(this.entity))
            {
                this.list.add(this.entity);
            }

            this.list.setCurrentScroll(this.entity);
        }
        else
        {
            this.fill(null, false);
        }
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

        this.hovered = null;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = camera.getPos();

        Vector3f mouseDirection = CameraUtils.getMouseDirection(
            RenderSystem.getProjectionMatrix(),
            context.matrixStack().peek().getPositionMatrix(),
            (int) mc.mouse.getX(), (int) mc.mouse.getY(), 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()
        );

        this.hovered = this.getClosestObject(new Vector3d(pos.x, pos.y, pos.z), mouseDirection);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.matrixStack().push();
        context.matrixStack().translate(-pos.x, -pos.y, -pos.z);

        if (this.entity != null)
        {
            this.renderBox(context.matrixStack(), this.entity, 0F, 1F, 0F);
        }

        for (TriggerBlockEntity entity : TriggerBlockEntityRenderer.capturedTriggerBlocks)
        {
            if (this.entity == entity)
            {
                continue;
            }

            if (this.hovered == entity)
            {
                this.renderBox(context.matrixStack(), entity, 0F, 1F, 0F);
            }
            else
            {
                this.renderBox(context.matrixStack(), entity, -1F, -1F, -1F);
            }
        }

        context.matrixStack().pop();

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderBox(net.minecraft.client.util.math.MatrixStack stack, TriggerBlockEntity entity, float r, float g, float b)
    {
        BlockPos bp = entity.getPos();
        Vector3f p1 = entity.pos1.get();
        Vector3f p2 = entity.pos2.get();
        
        double minX = Math.min(p1.x, p2.x);
        double minY = Math.min(p1.y, p2.y);
        double minZ = Math.min(p1.z, p2.z);
        double maxX = Math.max(p1.x, p2.x);
        double maxY = Math.max(p1.y, p2.y);
        double maxZ = Math.max(p1.z, p2.z);

        double x = bp.getX() + minX;
        double y = bp.getY() + minY;
        double z = bp.getZ() + minZ;
        double w = maxX - minX;
        double h = maxY - minY;
        double d = maxZ - minZ;
        
        if (r == -1)
            Draw.renderBox(stack, x, y, z, w, h, d);
        else
            Draw.renderBox(stack, x, y, z, w, h, d, r, g, b);
    }

    protected double getDistance(TriggerBlockEntity object, Vector3d pos, Vector3f dir)
    {
        return RayTracing.intersect(pos, dir, this.getHitbox(object));
    }

    protected TriggerBlockEntity getClosestObject(Vector3d pos, Vector3f dir)
    {
        TriggerBlockEntity closest = null;
        double current = Double.POSITIVE_INFINITY;

        for (TriggerBlockEntity entity : TriggerBlockEntityRenderer.capturedTriggerBlocks)
        {
            double result = this.getDistance(entity, pos, dir);

            if (result >= 0 && result < current)
            {
                current = result;
                closest = entity;
            }
        }

        return closest;
    }

    private AABB getHitbox(TriggerBlockEntity closest)
    {
        BlockPos pos = closest.getPos();
        Vector3f p1 = closest.pos1.get();
        Vector3f p2 = closest.pos2.get();

        double minX = Math.min(p1.x, p2.x);
        double minY = Math.min(p1.y, p2.y);
        double minZ = Math.min(p1.z, p2.z);
        double maxX = Math.max(p1.x, p2.x);
        double maxY = Math.max(p1.y, p2.y);
        double maxZ = Math.max(p1.z, p2.z);

        return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, maxX - minX, maxY - minY, maxZ - minZ);
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (super.subMouseClicked(context))
        {
            return true;
        }

        if (this.hovered != null && context.mouseButton == 0)
        {
            this.fill(this.hovered, true);
            return true;
        }

        return false;
    }
}
