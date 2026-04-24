package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.mixin.client.ClientPlayerEntityAccessor;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BaseFilmController
{
    public final Film film;

    protected IntObjectMap<IEntity> entities = new IntObjectHashMap<>();
    protected Map<String, Replay> replayMap = new java.util.HashMap<>();

    public boolean paused;
    public int exception = -1;

    /* Rendering helpers */

    public static void renderEntity(FilmControllerContext context)
    {
        IntObjectMap<IEntity> entities = context.entities;
        IEntity entity = context.entity;
        Camera camera = context.camera;
        MatrixStack stack = context.stack;
        float transition = context.transition;

        Form form = entity.getForm();

        if (form == null)
        {
            return;
        }

        Vector3d position = Vectors.TEMP_3D.set(
            Lerps.lerp(entity.getPrevX(), entity.getX(), transition),
            Lerps.lerp(entity.getPrevY(), entity.getY(), transition),
            Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition)
        );

        double cx = camera.getPos().x;
        double cy = camera.getPos().y;
        double cz = camera.getPos().z;

        boolean relative = context.replay != null && context.relative;

        if (relative)
        {
            cx = context.replay.keyframes.x.interpolate(0F) + context.replay.relativeOffset.get().x;
            cy = context.replay.keyframes.y.interpolate(0F) + context.replay.relativeOffset.get().y;
            cz = context.replay.keyframes.z.interpolate(0F) + context.replay.relativeOffset.get().z;
        }

        Matrix4f target = null;
        Matrix4f defaultMatrix = getMatrixForRenderWithRotation(entity, cx, cy, cz, transition);
        float opacity = 1F;

        if (!relative)
        {
            Pair<Matrix4f, Float> pair = getTotalMatrix(entities, form.anchor.get(), defaultMatrix, cx, cy, cz, transition, 0);

            target = pair.a;
            opacity = pair.b;
        }

        if (target != null)
        {
            Vector3f v = target.getTranslation(new Vector3f());
            Vector3f v2 = defaultMatrix.getTranslation(new Vector3f());

            position.x += v.x - v2.x;
            position.y += v.y - v2.y;
            position.z += v.z - v2.z;
        }
        else
        {
            target = defaultMatrix;
        }

        if (context.localGroupTransform != null)
        {
            target.mul(context.localGroupTransform);
        }

        BlockPos pos = BlockPos.ofFloored(position.x, position.y + 0.5D, position.z);
        int sky = entity.getWorld().getLightLevel(LightType.SKY, pos);
        int torch = entity.getWorld().getLightLevel(LightType.BLOCK, pos);
        int light = LightmapTextureManager.pack(torch, sky);
        int overlay = OverlayTexture.packUv(OverlayTexture.getU(0F), OverlayTexture.getV(entity.getHurtTimer() > 0));

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.ENTITY, entity, stack, light, overlay, transition)
            .camera(camera)
            .stencilMap(context.map)
            .color(context.color);

        stack.push();

        if (relative)
        {
            stack.peek().getPositionMatrix().identity();
            stack.peek().getNormalMatrix().identity();
        }

        MatrixStackUtils.multiply(stack, target);
        FormUtilsClient.render(form, formContext);

        if (UIBaseMenu.renderAxes)
        {
            if (context.bone != null && !context.local)
            {
                Form root = FormUtils.getRoot(form);
                MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
                MatrixCacheEntry entry = map.get(context.bone);

                Matrix4f matrix = entry.origin();

                if (matrix == null)
                {
                    matrix = entry.matrix();
                }

                if (matrix != null)
                {
                    stack.push();
                    MatrixStackUtils.multiply(stack, matrix);

                    if (context.map == null)
                    {
                        Gizmo.INSTANCE.render(stack);
                    }
                    else
                    {
                        Gizmo.INSTANCE.renderStencil(stack, context.map);
                    }

                    RenderSystem.enableDepthTest();
                    stack.pop();
                }
            }
            if (context.bone != null) renderAxes(context.bone, context.local, context.map, form, entity, transition, stack);
            if (context.bone2 != null && context.map == null) renderAxes(context.bone2, context.local2, context.map, form, entity, transition, stack);
        }

        stack.pop();

        if (!relative && context.map == null && opacity > 0F && context.shadowRadius > 0F && form.visible.get())
        {
            stack.push();
            stack.translate(position.x - cx, position.y - cy, position.z - cz);

            ModelBlockEntityRenderer.renderShadow(context.consumers, stack, transition, position.x, position.y, position.z, 0F, 0F, 0F, context.shadowRadius, opacity);

            stack.pop();
        }

        if (!relative && !context.nameTag.isEmpty() && context.map == null)
        {
            stack.push();
            stack.translate(position.x - cx, position.y - cy, position.z - cz);

            renderNameTag(entity, Text.literal(StringUtils.processColoredText(context.nameTag)), stack, context.consumers, light);

            stack.pop();
        }

        RenderSystem.enableDepthTest();
    }

    private static void renderAxes(String bone, boolean local, StencilMap stencilMap, Form form, IEntity entity, float transition, MatrixStack stack)
    {
        Form root = FormUtils.getRoot(form);
        MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
        MatrixCacheEntry entry = map.get(bone);

        if (entry == null)
        {
            return;
        }

        Matrix4f matrix;

        if (local)
        {
            Matrix4f localMatrix = entry.matrix();
            Matrix4f originMatrix = entry.origin();

            if (localMatrix != null && originMatrix != null)
            {
                matrix = new Matrix4f(localMatrix);
                matrix.setTranslation(originMatrix.getTranslation(new org.joml.Vector3f()));
            }
            else
            {
                matrix = localMatrix != null ? localMatrix : originMatrix;
            }
        }
        else
        {
            matrix = entry.origin() != null ? entry.origin() : entry.matrix();
        }

        if (matrix != null)
        {
            stack.push();
            MatrixStackUtils.multiply(stack, matrix);

            if (stencilMap == null)
            {
                Gizmo.INSTANCE.render(stack);
            }
            else
            {
                Gizmo.INSTANCE.renderStencil(stack, stencilMap);
            }

            RenderSystem.enableDepthTest();
            stack.pop();
        }
    }

    public static Pair<Matrix4f, Float> getTotalMatrix(IntObjectMap<IEntity> entities, Anchor value, Matrix4f defaultMatrix, double cx, double cy, double cz, float transition, int i)
    {
        /* Stupid recursion stop, I don't think anyone would need more than that */
        if (i > 5)
        {
            return new Pair<>(defaultMatrix, 1F);
        }

        boolean same = value.previous == null || Objects.equals(value, value.previous);
        boolean only = value.x <= 0F && value.previous != null;
        Pair<Matrix4f, Float> result = new Pair<>(null, 1F);

        if (same || only)
        {
            Matrix4f matrix = getEntityMatrix(entities, cx, cy, cz, same ? value : value.previous, defaultMatrix, transition, i);

            if (matrix != defaultMatrix)
            {
                result.a = matrix;
                result.b = 0F;
            }
        }
        else
        {
            Matrix4f matrix = getEntityMatrix(entities, cx, cy, cz, value, defaultMatrix, transition, i);
            Matrix4f lastMatrix = getEntityMatrix(entities, cx, cy, cz, value.previous, defaultMatrix, transition, i);

            result.a = value.x >= 1F ? matrix : Matrices.lerp(lastMatrix, matrix, value.x);

            if (value.isFadeOut()) result.b = value.x;
            else if (value.isFadeIn()) result.b = 1F - value.x;
            else result.b = 0F;
        }

        return result;
    }

    public static Matrix4f getEntityMatrix(IntObjectMap<IEntity> entities, double cameraX, double cameraY, double cameraZ, Anchor anchor, Matrix4f defaultMatrix, float transition, int i)
    {
        IEntity entity = entities.get(anchor.replay);

        if (entity != null)
        {
            Matrix4f basic = getMatrixForRenderWithRotation(entity, cameraX, cameraY, cameraZ, transition);

            Form form = entity.getForm();

            if (form != null)
            {
                Pair<Matrix4f, Float> totalMatrix = getTotalMatrix(entities, form.anchor.get(), basic, cameraX, cameraY, cameraZ, transition, i + 1);

                if (totalMatrix.a != null)
                {
                    basic = totalMatrix.a;
                }

                MatrixCache map = FormUtilsClient.getRenderer(form).collectMatrices(entity, transition);
                boolean forceOrigin = anchor.attachment != null && anchor.attachment.endsWith("#origin");
                String core = anchor.attachment == null ? null : anchor.attachment.replace("#origin", "");
                
                MatrixCacheEntry entry = map.get(core);
                Matrix4f matrix = null;

                if (entry != null)
                {
                    if (forceOrigin)
                    {
                        matrix = entry.origin();
                    }
                    else if (anchor.translate)
                    {
                        matrix = entry.origin();
                        if (matrix == null)
                        {
                            matrix = entry.matrix();
                        }
                    }
                    else
                    {
                        matrix = entry.matrix();
                        if (matrix == null)
                        {
                            matrix = entry.origin();
                        }
                    }
                }

                if (matrix != null)
                {
                    basic.mul(matrix);

                    if (anchor.scale)
                    {
                        Matrix3f mat = new Matrix3f();
                        Vector3f v = new Vector3f();
                        basic.get3x3(mat);

                        mat.getColumn(0, v); v.normalize(); mat.setColumn(0, v);
                        mat.getColumn(1, v); v.normalize(); mat.setColumn(1, v);
                        mat.getColumn(2, v); v.normalize(); mat.setColumn(2, v);

                        basic.set3x3(mat);
                    }

                    if (anchor.translate)
                    {
                        Vector3f t = new Vector3f();
                        basic.getTranslation(t);
                        basic.set(defaultMatrix);
                        basic.setTranslation(t);
                    }
                }
            }

            return basic;
        }

        return defaultMatrix;
    }

    public static Matrix4f getMatrixForRenderWithRotation(IEntity entity, double cameraX, double cameraY, double cameraZ, float tickDelta)
    {
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), tickDelta) - cameraX;
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), tickDelta) - cameraY;
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), tickDelta) - cameraZ;

        Matrix4f matrix = new Matrix4f();

        float bodyYaw = Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), tickDelta);

        matrix.translate((float) x, (float) y, (float) z);
        matrix.rotateY(MathUtils.toRad(-bodyYaw));

        return matrix;
    }

    private static void renderNameTag(IEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        boolean sneaking = !entity.isSneaking();
        float hitboxH = (float) entity.getPickingHitbox().h + (entity.isSneaking() ? 0.25F : 0.5F);


        matrices.push();
        matrices.translate(0F, hitboxH, 0F);
        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        float opacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int background = (int) (opacity * 255F) << 24;
        float h = (float) (-textRenderer.getWidth(text) / 2);

        textRenderer.draw(text, h, 0, 0x20ffffff, false, matrix4f, vertexConsumers, sneaking ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, background, light);

        if (sneaking)
        {
            textRenderer.draw(text, h, 0, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
        }

        matrices.pop();
    }

    /* Film controller */

    public BaseFilmController(Film film)
    {
        this.film = film;
    }

    public IntObjectMap<IEntity> getEntities()
    {
        return this.entities;
    }

    public void togglePause()
    {
        this.paused = !this.paused;
    }

    public void createEntities()
    {
        this.entities.clear();
        this.replayMap.clear();

        if (this.film == null)
        {
            return;
        }

        int i = 0;

        for (Replay replay : this.film.replays.getList())
        {
            this.replayMap.put(replay.uuid.get(), replay);
            this.replayMap.put(replay.getId(), replay);

            if (replay.enabled.get())
            {
                World world = MinecraftClient.getInstance().world;
                IEntity entity = new StubEntity(world);

                entity.setForm(FormUtils.copy(replay.form.get()));
                replay.keyframes.apply(0, entity);
                entity.setPrevX(entity.getX());
                entity.setPrevY(entity.getY());
                entity.setPrevZ(entity.getZ());

                entity.setPrevYaw(entity.getYaw());
                entity.setPrevHeadYaw(entity.getHeadYaw());
                entity.setPrevPitch(entity.getPitch());
                entity.setPrevBodyYaw(entity.getBodyYaw());

                this.entities.put(i, entity);
            }

            i += 1;
        }
    }

    public abstract Map<String, Integer> getActors();

    public abstract int getTick();

    public boolean hasFinished()
    {
        return false;
    }

    public void update()
    {
        this.updateEntities(this.getTick());
    }

    protected void updateEntities(int ticks)
    {
        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            List<Replay> replays = this.film.replays.getList();
            Replay replay = CollectionUtils.getSafe(replays, i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.UPDATE))
            {
                continue;
            }

            if (replay != null)
            {
                ticks = replay.getTick(ticks);

                this.updateEntityAndForm(entity, ticks);
                this.applyReplay(replay, ticks, entity);

                boolean spawned = false;

                Map<String, Integer> actors = this.getActors();

                if (actors != null)
                {
                    Integer entityId = actors.get(replay.getId());

                    if (entityId != null)
                    {
                        Entity anEntity = MinecraftClient.getInstance().world.getEntityById(entityId);

                        if (anEntity instanceof ActorEntity actor)
                        {
                            /* Force synchronize entity angles */
                            actor.setYaw(replay.keyframes.yaw.interpolate(ticks).floatValue());
                            actor.setHeadYaw(replay.keyframes.headYaw.interpolate(ticks).floatValue());
                            actor.setBodyYaw(replay.keyframes.bodyYaw.interpolate(ticks).floatValue());
                            actor.setPitch(replay.keyframes.pitch.interpolate(ticks).floatValue());
                            replay.applyClientActions(ticks, new MCEntity(anEntity), this.film);

                            spawned = true;
                        }
                        else if (anEntity instanceof PlayerEntity player)
                        {
                            double x = replay.keyframes.x.interpolate(ticks);
                            double y = replay.keyframes.y.interpolate(ticks);
                            double z = replay.keyframes.z.interpolate(ticks);
                            double prevX = replay.keyframes.x.interpolate(ticks - 1);
                            double prevY = replay.keyframes.y.interpolate(ticks - 1);
                            double prevZ = replay.keyframes.z.interpolate(ticks - 1);

                            player.setVelocity(x - prevX, y - prevY, z - prevZ);

                            this.spawnSprintParticles(replay, ticks, player);
                            spawned = true;
                        }
                    }
                }

                if (!spawned)
                {
                    World world = MinecraftClient.getInstance().world;
                    Form form = replay.form.get();
                    double width = form != null ? form.hitboxWidth.get() : 0.6D;

                    this.spawnSprintParticles(replay, ticks, world, width);
                }
            }
        }
    }

    public void updateEndWorld()
    {
        int ticks = this.getTick();

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            List<Replay> replays = this.film.replays.getList();
            Replay replay = CollectionUtils.getSafe(replays, i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.UPDATE))
            {
                continue;
            }

            if (replay != null)
            {
                ticks = replay.getTick(ticks);

                Map<String, Integer> actors = this.getActors();

                if (actors != null)
                {
                    Integer entityId = actors.get(replay.getId());

                    if (entityId != null)
                    {
                        Entity anEntity = MinecraftClient.getInstance().world.getEntityById(entityId);

                        if (anEntity instanceof PlayerEntity player)
                        {
                            double x = replay.keyframes.x.interpolate(ticks);
                            double y = replay.keyframes.y.interpolate(ticks);
                            double z = replay.keyframes.z.interpolate(ticks);
                            boolean sneaking = replay.keyframes.sneaking.interpolate(ticks) > 0;

                            Vec3d pos = player.getPos();

                            player.move(MovementType.SELF, new Vec3d(x - pos.x, y - pos.y, z - pos.z));
                            player.setPosition(x, y, z);

                            player.setSneaking(sneaking);
                            player.setOnGround(replay.keyframes.grounded.interpolate(ticks) > 0);

                            if (player instanceof ClientPlayerEntityAccessor accessor)
                            {
                                accessor.bbs$setIsSneakingPose(sneaking);
                            }

                            if (player instanceof ClientPlayerEntity playerEntity)
                            {
                                playerEntity.input.sneaking = sneaking;
                            }

                            player.fallDistance = replay.keyframes.fall.interpolate(ticks).floatValue();
                        }
                    }
                }
            }
        }
    }

    protected void updateEntityAndForm(IEntity entity, int tick)
    {
        entity.update();

        if (entity.getForm() != null)
        {
            entity.getForm().update(entity);
        }
    }

    protected void applyReplay(Replay replay, int ticks, IEntity entity)
    {
        replay.keyframes.apply(ticks, entity);
        replay.applyClientActions(ticks, entity, this.film);
    }

      private void spawnSprintParticles(Replay replay, int ticks, Entity entity)
    {
        if (entity == null)
        {
            return;
        }

        this.spawnSprintParticles(replay, ticks, entity.getWorld(), entity.getWidth());
    }

    private void spawnSprintParticles(Replay replay, int ticks, World world, double width)
    {
        if (!BBSSettings.editorReplaySprintParticles.get() || replay == null || world == null)
        {
            return;
        }

        if (replay.keyframes.sprinting.interpolate(ticks) <= 0D)
        {
            return;
        }

        if (replay.keyframes.grounded.interpolate(ticks) <= 0D)
        {
            return;
        }

        double vX = replay.keyframes.vX.interpolate(ticks);
        double vZ = replay.keyframes.vZ.interpolate(ticks);

        if ((vX * vX + vZ * vZ) < 0.001D)
        {
            return;
        }

        double xPos = replay.keyframes.x.interpolate(ticks);
        double yPos = replay.keyframes.y.interpolate(ticks);
        double zPos = replay.keyframes.z.interpolate(ticks);

        BlockPos pos = BlockPos.ofFloored(xPos, yPos - 0.2D, zPos);

        if (world.isAir(pos))
        {
            return;
        }

        double x = xPos + (world.random.nextDouble() - 0.5D) * width;
        double y = yPos + 0.1D;
        double z = zPos + (world.random.nextDouble() - 0.5D) * width;

        world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(pos)), x, y, z, 0D, 0.1D, 0D);
    }


    public void startRenderFrame(float transition)
    {
        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = this.film.replays.getList().get(i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.PROPERTIES))
            {
                continue;
            }

            float delta = this.getTransition(entity, transition);
            int tick = replay.getTick(this.getTick());

            /* Apply property */
            Form form1 = entity.getForm();
            replay.properties.applyProperties(form1, tick + delta);

            Map<String, Integer> actors = this.getActors();

            if (actors != null)
            {
                Integer entityId = actors.get(replay.getId());

                if (entityId != null)
                {
                    Entity anEntity = MinecraftClient.getInstance().world.getEntityById(entityId);

                    if (anEntity instanceof ActorEntity actor)
                    {
                        Form form = actor.getForm();
                        replay.properties.applyProperties(form, tick + delta);
                    }
                    else if (anEntity instanceof PlayerEntity player)
                    {
                        Morph morph = Morph.getMorph(player);

                        if (morph != null)
                        {
                            Form form = morph.getForm();
                            replay.properties.applyProperties(form, tick + delta);
                        }

                        float yawHead = replay.keyframes.headYaw.interpolate(tick + delta).floatValue();
                        float yawBody = replay.keyframes.bodyYaw.interpolate(tick + delta).floatValue();
                        float pitch = replay.keyframes.pitch.interpolate(tick + delta).floatValue();

                        player.setYaw(yawHead);
                        player.setHeadYaw(yawHead);
                        player.setPitch(pitch);
                        player.setBodyYaw(yawBody);
                        player.prevYaw = yawHead;
                        player.prevHeadYaw = yawHead;
                        player.prevPitch = pitch;
                        player.prevBodyYaw = yawBody;
                    }
                }
            }
        }
    }

    protected float getTransition(IEntity entity, float transition)
    {
        return this.paused ? 0F : transition;
    }

    protected boolean canUpdate(int i, Replay replay, IEntity entity, UpdateMode updateMode)
    {
        if (this.paused && (updateMode == UpdateMode.UPDATE))
        {
            return false;
        }

        return i != this.exception;
    }

    public void render(WorldRenderContext context)
    {
        RenderSystem.enableDepthTest();

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = this.film.replays.getList().get(i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.RENDER))
            {
                continue;
            }

            this.renderEntity(context, replay, entity);
        }
    }

    protected void renderEntity(WorldRenderContext context, Replay replay, IEntity entity)
    {
        if (!replay.actor.get())
        {
            FilmControllerContext filmContext = getFilmControllerContext(context, replay, entity);

            filmContext.transition = getTransition(entity, context.tickDelta());

            filmContext.stack.push();

            if (!this.applyGroupProperties(replay, filmContext))
            {
                filmContext.stack.pop();
                return;
            }

            renderEntity(filmContext);

            filmContext.stack.pop();
        }
    }

    protected Replay getGroupPivot(String groupUuid)
    {
        for (Replay replay : this.film.replays.getList())
        {
            if (replay.group.get().contains(groupUuid))
            {
                return replay;
            }
        }

        return null;
    }

    protected boolean applyGroupProperties(Replay replay, FilmControllerContext context)
    {
        if (replay.group.get().isEmpty())
        {
            return true;
        }

        String[] groups = replay.group.get().split("/");
        int finalColor = Colors.WHITE;
        Matrix4f globalTranslate = new Matrix4f().identity();
        Matrix4f localTransform = new Matrix4f().identity();

        for (String uuid : groups)
        {
            Replay groupReplay = this.replayMap.get(uuid);

            if (groupReplay != null)
            {
                double tick = groupReplay.getTick(this.getTick()) + context.transition;

                BaseValue visibleValue = groupReplay.properties.get("visible");

                if (visibleValue instanceof KeyframeChannel)
                {
                    KeyframeChannel<Boolean> visible = (KeyframeChannel<Boolean>) visibleValue;

                    if (!visible.isEmpty() && !visible.interpolate((float) tick))
                    {
                        return false;
                    }
                }

                BaseValue colorValue = groupReplay.properties.get("color");

                if (colorValue instanceof KeyframeChannel)
                {
                    KeyframeChannel<Color> color = (KeyframeChannel<Color>) colorValue;

                    if (!color.isEmpty())
                    {
                        int groupColor = color.interpolate((float) tick).getARGBColor();
                        finalColor = this.mulColors(finalColor, groupColor);
                    }
                }

                BaseValue transformValue = groupReplay.properties.get("transform");

                if (transformValue instanceof KeyframeChannel)
                {
                    KeyframeChannel<Transform> transform = (KeyframeChannel<Transform>) transformValue;

                    if (!transform.isEmpty())
                    {
                        Transform t = transform.interpolate((float) tick);
                        
                        globalTranslate.translate(t.translate.x, t.translate.y, t.translate.z);
                        
                        Matrix4f local = new Matrix4f();
                        
                        if (t.pivot.x != 0F || t.pivot.y != 0F || t.pivot.z != 0F)
                        {
                            local.translate(t.pivot);
                        }
                        
                        local.rotateZ(t.rotate.z);
                        local.rotateY(t.rotate.y);
                        local.rotateX(t.rotate.x);
                        local.rotateZ(t.rotate2.z);
                        local.rotateY(t.rotate2.y);
                        local.rotateX(t.rotate2.x);
                        local.scale(t.scale);
                        
                        if (t.pivot.x != 0F || t.pivot.y != 0F || t.pivot.z != 0F)
                        {
                            local.translate(-t.pivot.x, -t.pivot.y, -t.pivot.z);
                        }
                        
                        localTransform.mul(local);
                    }
                }
            }
        }

        if (finalColor != Colors.WHITE)
        {
            context.color(this.mulColors(context.color, finalColor));
        }

        if (!globalTranslate.equals(new Matrix4f().identity()))
        {
            context.stack.peek().getPositionMatrix().mul(globalTranslate);
        }
        
        if (!localTransform.equals(new Matrix4f().identity()))
        {
            context.localGroupTransform = localTransform;
        }

        return true;
    }

    private int mulColors(int c1, int c2)
    {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = (c1) & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = (c2) & 0xFF;

        int a = (a1 * a2) / 255;
        int r = (r1 * r2) / 255;
        int g = (g1 * g2) / 255;
        int b = (b1 * b2) / 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected FilmControllerContext getFilmControllerContext(WorldRenderContext context, Replay replay, IEntity entity)
    {
        return FilmControllerContext.instance
            .setup(this.entities, entity, replay, context)
            .shadow(replay.shadow.get(), replay.shadowSize.get())
            .nameTag(replay.nameTag.get())
            .relative(replay.relative.get());
    }

    public void shutdown()
    {}

    public static enum UpdateMode
    {
        UPDATE, RENDER, PROPERTIES;
    }
}
