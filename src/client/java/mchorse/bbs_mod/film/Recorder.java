package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.Inventory;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class Recorder extends WorldFilmController
{
    public ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public FormProperties properties = new FormProperties("properties");
    public Inventory inventory = new Inventory("inventory");
    public float hp;
    public float hunger;
    public int xpLevel;
    public float xpProgress;

    public Form lastForm;
    public Vector3d lastPosition;
    public Vector4f lastRotation;

    public int countdown;
    public final int initialTick;

    public static void renderCameraPreview(Position position, Camera camera, MatrixStack stack)
    {
        if (!BBSSettings.recordingOverlays.get())
        {
            return;
        }

        float x = (float) (position.point.x - camera.getPos().x);
        float y = (float) (position.point.y - camera.getPos().y);
        float z = (float) (position.point.z - camera.getPos().z);
        float fov = MathUtils.toRad(position.angle.fov);
        float aspect = BBSRendering.getVideoWidth() / (float) BBSRendering.getVideoHeight();
        float distance = 5.5F;
        float thickness = 0.025F;
        float halfHeight = (float) Math.tan(fov * 0.5F) * distance;
        float halfWidth = halfHeight * aspect;
        float yaw = MathUtils.toRad(position.angle.yaw + 180F);
        float pitch = MathUtils.toRad(position.angle.pitch);
        float fx = (float) (-Math.sin(yaw) * Math.cos(pitch));
        float fy = (float) (-Math.sin(pitch));
        float fz = (float) (Math.cos(yaw) * Math.cos(pitch));
        float rx = (float) Math.cos(yaw);
        float ry = 0F;
        float rz = (float) Math.sin(yaw);
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);

        if (rLen < 0.0001F)
        {
            rx = 1F;
            ry = 0F;
            rz = 0F;
            rLen = 1F;
        }

        rx /= rLen;
        ry /= rLen;
        rz /= rLen;

        float ux = fy * rz - fz * ry;
        float uy = fz * rx - fx * rz;
        float uz = fx * ry - fy * rx;
        float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);

        if (uLen < 0.0001F)
        {
            ux = 0F;
            uy = 1F;
            uz = 0F;
            uLen = 1F;
        }

        ux /= uLen;
        uy /= uLen;
        uz /= uLen;

        Vector4f topRight = frustumCorner(fx, fy, fz, rx, ry, rz, ux, uy, uz, distance, halfWidth, halfHeight);
        Vector4f topLeft = frustumCorner(fx, fy, fz, rx, ry, rz, ux, uy, uz, distance, -halfWidth, halfHeight);
        Vector4f bottomRight = frustumCorner(fx, fy, fz, rx, ry, rz, ux, uy, uz, distance, halfWidth, -halfHeight);
        Vector4f bottomLeft = frustumCorner(fx, fy, fz, rx, ry, rz, ux, uy, uz, distance, -halfWidth, -halfHeight);
        Vector4f forward = new Vector4f(fx * (distance + 100F), fy * (distance + 100F), fz * (distance + 100F), 1F);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillPreviewSegment(builder, stack, x, y, z, x + topRight.x, y + topRight.y, z + topRight.z, thickness, 1F, 1F, 1F, 0.85F);
        fillPreviewSegment(builder, stack, x, y, z, x + topLeft.x, y + topLeft.y, z + topLeft.z, thickness, 1F, 1F, 1F, 0.85F);
        fillPreviewSegment(builder, stack, x, y, z, x + bottomRight.x, y + bottomRight.y, z + bottomRight.z, thickness, 1F, 1F, 1F, 0.85F);
        fillPreviewSegment(builder, stack, x, y, z, x + bottomLeft.x, y + bottomLeft.y, z + bottomLeft.z, thickness, 1F, 1F, 1F, 0.85F);

        fillPreviewSegment(builder, stack, x + topRight.x, y + topRight.y, z + topRight.z, x + topLeft.x, y + topLeft.y, z + topLeft.z, thickness, 1F, 1F, 1F, 0.65F);
        fillPreviewSegment(builder, stack, x + topLeft.x, y + topLeft.y, z + topLeft.z, x + bottomLeft.x, y + bottomLeft.y, z + bottomLeft.z, thickness, 1F, 1F, 1F, 0.65F);
        fillPreviewSegment(builder, stack, x + bottomLeft.x, y + bottomLeft.y, z + bottomLeft.z, x + bottomRight.x, y + bottomRight.y, z + bottomRight.z, thickness, 1F, 1F, 1F, 0.65F);
        fillPreviewSegment(builder, stack, x + bottomRight.x, y + bottomRight.y, z + bottomRight.z, x + topRight.x, y + topRight.y, z + topRight.z, thickness, 1F, 1F, 1F, 0.65F);

        fillPreviewSegment(builder, stack, x, y, z, x + forward.x, y + forward.y, z + forward.z, thickness * 1.35F, 0F, 0.5F, 1F, 1F);

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();
    }

    private static Vector4f frustumCorner(float fx, float fy, float fz, float rx, float ry, float rz, float ux, float uy, float uz, float distance, float side, float up)
    {
        return new Vector4f(
            fx * distance + rx * side + ux * up,
            fy * distance + ry * side + uy * up,
            fz * distance + rz * side + uz * up,
            1F
        );
    }

    private static void fillPreviewSegment(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.0001F)
        {
            return;
        }

        float nx = dx / distance;
        float ny = dy / distance;
        float nz = dz / distance;
        Quaternionf rotation = new Quaternionf().rotationTo(0F, 0F, 1F, nx, ny, nz);

        stack.push();
        stack.translate(x1, y1, z1);
        stack.multiply(rotation);
        Draw.fillBox(builder, stack, -thickness / 2F, -thickness / 2F, 0F, thickness / 2F, thickness / 2F, distance, r, g, b, a);
        stack.pop();
    }

    public Recorder(Film film, Form form, int replayId, int tick)
    {
        super(film);

        this.lastForm = FormUtils.copy(form);
        this.exception = replayId;
        this.tick = tick;
        this.countdown = TimeUtils.toTick(BBSSettings.recordingCountdown.get());
        this.initialTick = tick;
    }

    public boolean hasNotStarted()
    {
        return this.countdown > 0;
    }

    public void update()
    {
        if (this.hasNotStarted())
        {
            this.countdown -= 1;

            return;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (this.lastPosition == null)
        {
            this.lastPosition = new Vector3d(player.getX(), player.getY(), player.getZ());
            this.lastRotation = new Vector4f(player.getYaw(), player.getPitch(), player.getHeadYaw(), player.getBodyYaw());
            this.inventory.fromPlayer(player);

            this.hp = player.getHealth();
            this.hunger = player.getHungerManager().getSaturationLevel();
            this.xpLevel = player.experienceLevel;
            this.xpProgress = player.experienceProgress;
        }

        if (this.tick >= 0)
        {
            Morph morph = Morph.getMorph(player);

            this.keyframes.record(this.tick, morph.entity, null);
        }

        super.update();
    }

    public void render(WorldRenderContext context)
    {
        super.render(context);

        renderCameraPreview(this.position, context.camera(), context.matrixStack());
    }

    @Override
    public void shutdown()
    {
        Vector3d pos = this.lastPosition;

        if (pos != null)
        {
            Vector4f rot = this.lastRotation;

            PlayerUtils.teleport(pos.x, pos.y, pos.z, rot.z, rot.y);
            ClientNetwork.sendPlayerForm(this.lastForm);
        }

        super.shutdown();
    }
}
