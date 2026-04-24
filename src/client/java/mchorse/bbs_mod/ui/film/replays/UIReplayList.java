package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.modifiers.EntityClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.Replays;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This GUI is responsible for drawing replays available in the 
 * director thing
 */
public class UIReplayList extends UIList<Replay>
{
    private static String LAST_PROCESS = "v";
    private static String LAST_OFFSET = "0";
    private static List<String> LAST_PROCESS_PROPERTIES = Arrays.asList("x");

    public UIFilmPanel panel;
    public UIReplaysOverlayPanel overlay;

    private Map<String, Boolean> expandedGroups = new java.util.HashMap<>();
    private List<Replay> visualList = new ArrayList<>();

    public UIReplayList(Consumer<List<Replay>> callback, UIReplaysOverlayPanel overlay, UIFilmPanel panel)
    {
        super(callback);

        this.overlay = overlay;
        this.panel = panel;

        this.multi().sorting();
        this.context((menu) ->
        {
            menu.action(Icons.ADD, UIKeys.SCENE_REPLAYS_CONTEXT_ADD, this::addReplay);

            if (this.isSelected())
            {
                menu.action(Icons.COPY, UIKeys.SCENE_REPLAYS_CONTEXT_COPY, this::copyReplay);
            }

            MapType copyReplay = Window.getClipboardMap("_CopyReplay");

            if (copyReplay != null)
            {
                menu.action(Icons.PASTE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE, () -> this.pasteReplay(copyReplay));
            }

            int duration = this.panel.getData().camera.calculateDuration();

            if (duration > 0)
            {
                menu.action(Icons.PLAY, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_CAMERA, () -> this.fromCamera(duration));
            }

            menu.action(Icons.BLOCK, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK, this::fromModelBlock);

            if (this.isSelected())
            {
                boolean isGroup = this.getCurrentFirst().isGroup.get();
                boolean shift = Window.isShiftPressed();
                MapType data = Window.getClipboardMap("_CopyKeyframes");

                if (!isGroup)
                {
                    menu.action(Icons.ALL_DIRECTIONS, UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS, this::processReplays);
                    menu.action(Icons.TIME, UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME, this::offsetTimeReplays);
                }
                
                menu.action(Icons.FOLDER, UIKeys.SCENE_REPLAYS_CONTEXT_ADD_GROUP, this::addGroup);

                if (!isGroup && data != null)
                {
                    menu.action(Icons.PASTE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES, () -> this.pasteToReplays(data));
                }

                if (!isGroup)
                {
                    menu.action(Icons.DUPE, UIKeys.SCENE_REPLAYS_CONTEXT_DUPE, () ->
                    {
                        if (Window.isShiftPressed() || shift)
                        {
                            this.dupeReplay();
                        }
                        else
                        {
                            UINumberOverlayPanel numberPanel = new UINumberOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_DUPE, UIKeys.SCENE_REPLAYS_CONTEXT_DUPE_DESCRIPTION, (n) ->
                            {
                                for (int i = 0; i < n; i++)
                                {
                                    this.dupeReplay();
                                }
                            });

                            numberPanel.value.limit(1).integer();
                            numberPanel.value.setValue(1D);

                            UIOverlay.addOverlay(this.getContext(), numberPanel);
                        }
                    });
                }
                
                menu.action(Icons.REMOVE, UIKeys.SCENE_REPLAYS_CONTEXT_REMOVE, this::removeReplay);
            }
        });
    }

    @Override
    protected void handleSwap(int from, int to)
    {
        Replay src = this.list.get(from);
        Replay dest = this.list.get(to);

        if (src.isGroup.get())
        {
            String srcPath = getReplayPath(src);
            String srcFullPath = srcPath.isEmpty() ? src.uuid.get() : srcPath + "/" + src.uuid.get();

            String destPath = getReplayPath(dest);
            String destFullPath = destPath.isEmpty() ? dest.uuid.get() : destPath + "/" + dest.uuid.get();

            if (destFullPath.equals(srcFullPath) || destFullPath.startsWith(srcFullPath + "/") ||
                dest.group.get().equals(srcFullPath) || dest.group.get().startsWith(srcFullPath + "/"))
            {
                return;
            }
        }

        if (dest.isGroup.get())
        {
            String destPath = getReplayPath(dest);
            String destGroupPath = destPath.isEmpty() ? dest.uuid.get() : destPath + "/" + dest.uuid.get();
            String srcGroup = src.group.get();

            if (!srcGroup.equals(destGroupPath))
            {
                
                Replay insertionAnchor = dest;
                List<Replay> allReplays = this.panel.getData().replays.getAllTyped();
                
                String srcPathForCheck = getReplayPath(src);
                String srcFullPathForCheck = srcPathForCheck.isEmpty() ? src.uuid.get() : srcPathForCheck + "/" + src.uuid.get();

                for (Replay r : allReplays)
                {
                    if (r == src) continue;

                    if (src.isGroup.get())
                    {
                        String g = r.group.get();
                        if (g.equals(srcFullPathForCheck) || g.startsWith(srcFullPathForCheck + "/"))
                        {
                            continue;
                        }
                    }

                    String g = r.group.get();
                    if (g.equals(destGroupPath) || g.startsWith(destGroupPath + "/"))
                    {
                        insertionAnchor = r;
                    }
                }

                if (src.isGroup.get())
                {
                    String oldPath = getReplayPath(src);
                    String oldFullPath = oldPath.isEmpty() ? src.uuid.get() : oldPath + "/" + src.uuid.get();

                    src.group.set(destGroupPath);

                    String newPath = getReplayPath(src);
                    String newFullPath = newPath.isEmpty() ? src.uuid.get() : newPath + "/" + src.uuid.get();

                    this.updateGroupPath(oldFullPath, newFullPath);
                }
                else
                {
                    src.group.set(destGroupPath);
                }

                this.moveReplayAndChildren(src, insertionAnchor, true);

                this.expandedGroups.put(destGroupPath, true);
                this.buildVisualList();
                this.updateFilmEditor();

                return;
            }
        }

        String destGroup = dest.group.get();

        if (src.isGroup.get())
        {
            String oldPath = getReplayPath(src);
            String oldFullPath = oldPath.isEmpty() ? src.uuid.get() : oldPath + "/" + src.uuid.get();

            src.group.set(destGroup);

            String newPath = getReplayPath(src);
            String newFullPath = newPath.isEmpty() ? src.uuid.get() : newPath + "/" + src.uuid.get();

            if (!oldFullPath.equals(newFullPath))
            {
                this.updateGroupPath(oldFullPath, newFullPath);
            }
        }
        else
        {
            src.group.set(destGroup);
        }

        this.moveReplayAndChildren(src, dest, from < to);
    }

    private void moveReplayAndChildren(Replay src, Replay dest, boolean insertAfter)
    {
        Film data = this.panel.getData();
        List<Replay> list = data.replays.getAllTyped();
        List<Replay> toMove = new ArrayList<>();

        toMove.add(src);

        if (src.isGroup.get())
        {
            String srcPath = getReplayPath(src);
            String srcFullPath = srcPath.isEmpty() ? src.uuid.get() : srcPath + "/" + src.uuid.get();

            for (Replay r : list)
            {
                if (r == src) continue;

                String g = r.group.get();
                if (g.equals(srcFullPath) || g.startsWith(srcFullPath + "/"))
                {
                    toMove.add(r);
                }
            }
        }

        data.preNotify(IValueListener.FLAG_UNMERGEABLE);

        list.removeAll(toMove);

        int destIndex = list.indexOf(dest);

        if (destIndex != -1)
        {
            int insertIndex = insertAfter ? destIndex + 1 : destIndex;
            insertIndex = Math.max(0, Math.min(insertIndex, list.size()));
            list.addAll(insertIndex, toMove);
        }
        else
        {
            list.addAll(toMove);
        }

        data.replays.sync();
        data.postNotify(IValueListener.FLAG_UNMERGEABLE);

        this.buildVisualList();
        this.updateFilmEditor();

        int newIndex = this.visualList.indexOf(src);
        if (newIndex != -1)
        {
            this.setIndex(newIndex);
        }
    }

    private void pasteToReplays(MapType data)
    {
        UIReplaysEditor replayEditor = this.panel.replayEditor;
        List<Replay> selectedReplays = replayEditor.replays.replays.getCurrent();

        if (data == null)
        {
            return;
        }

        Map<String, UIKeyframes.PastedKeyframes> parsedKeyframes = UIKeyframes.parseKeyframes(data);

        if (parsedKeyframes.isEmpty())
        {
            return;
        }

        UINumberOverlayPanel offsetPanel = new UINumberOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES_DESCRIPTION, (n) ->
        {
            int tick = this.panel.getCursor();

            for (Replay replay : selectedReplays)
            {
                int randomOffset = (int) (n.intValue() * Math.random());

                for (Map.Entry<String, UIKeyframes.PastedKeyframes> entry : parsedKeyframes.entrySet())
                {
                    String id = entry.getKey();
                    UIKeyframes.PastedKeyframes pastedKeyframes = entry.getValue();
                    KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(id);

                    if (channel == null || channel.getFactory() != pastedKeyframes.factory)
                    {
                        channel = replay.properties.getOrCreate(replay.form.get(), id);
                    }

                    float min = Integer.MAX_VALUE;

                    for (Keyframe kf : pastedKeyframes.keyframes)
                    {
                        min = Math.min(kf.getTick(), min);
                    }

                    for (Keyframe kf : pastedKeyframes.keyframes)
                    {
                        float finalTick = tick + (kf.getTick() - min) + randomOffset;
                        int index = channel.insert(finalTick, kf.getValue());
                        Keyframe inserted = channel.get(index);

                        inserted.copy(kf);
                        inserted.setTick(finalTick);
                    }

                    channel.sort();
                }
            }
        });

        UIOverlay.addOverlay(this.getContext(), offsetPanel);
    }

    private void processReplays()
    {
        UITextbox expression = new UITextbox((t) -> LAST_PROCESS = t);
        UIStringList properties = new UIStringList(null);
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_DESCRIPTION, (b) ->
        {
            if (b)
            {
                MathBuilder builder = new MathBuilder();
                int min = Integer.MAX_VALUE;

                builder.register("i");
                builder.register("o");
                builder.register("v");
                builder.register("ki");

                IExpression parse;

                try
                {
                    parse = builder.parse(expression.getText());
                }
                catch (Exception e)
                {
                    return;
                }

                LAST_PROCESS_PROPERTIES = new ArrayList<>(properties.getCurrent());

                for (int index : this.current)
                {
                    min = Math.min(min, index);
                }

                for (int index : this.current)
                {
                    Replay replay = this.list.get(index);

                    builder.variables.get("i").set(index);
                    builder.variables.get("o").set(index - min);

                    for (String s : properties.getCurrent())
                    {
                        KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(s);
                        List keyframes = channel.getKeyframes();

                        for (int i = 0; i < keyframes.size(); i++)
                        {
                            Keyframe kf = (Keyframe) keyframes.get(i);

                            builder.variables.get("v").set(kf.getFactory().getY(kf.getValue()));
                            builder.variables.get("ki").set(i);

                            kf.setValue(kf.getFactory().yToValue(parse.doubleValue()), true);
                        }
                    }
                }
            }
        });

        for (KeyframeChannel<?> channel : this.getCurrentFirst().keyframes.getChannels())
        {
            if (KeyframeFactories.isNumeric(channel.getFactory()))
            {
                properties.add(channel.getId());
            }
        }

        properties.background().multi().sort();
        properties.relative(expression).y(-5).w(1F).h(16 * 9).anchor(0F, 1F);

        if (!LAST_PROCESS_PROPERTIES.isEmpty())
        {
            properties.setCurrentScroll(LAST_PROCESS_PROPERTIES.get(0));
        }

        for (String property : LAST_PROCESS_PROPERTIES)
        {
            properties.addIndex(properties.getList().indexOf(property));
        }

        expression.setText(LAST_PROCESS);
        expression.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_EXPRESSION_TOOLTIP);
        expression.relative(panel.confirm).y(-1F, -5).w(1F).h(20);

        panel.confirm.w(1F, -10);
        panel.content.add(expression, properties);

        UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
    }

    private void offsetTimeReplays()
    {
        UITextbox tick = new UITextbox((t) -> LAST_OFFSET = t);
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_DESCRIPTION, (b) ->
        {
            if (b)
            {
                MathBuilder builder = new MathBuilder();
                int min = Integer.MAX_VALUE;

                builder.register("i");
                builder.register("o");

                IExpression parse = null;

                try
                {
                    parse = builder.parse(tick.getText());
                }
                catch (Exception e)
                {}

                for (int index : this.current)
                {
                    min = Math.min(min, index);
                }

                for (int index : this.current)
                {
                    Replay replay = this.list.get(index);

                    builder.variables.get("i").set(index);
                    builder.variables.get("o").set(index - min);

                    float tickv = parse == null ? 0F : (float) parse.doubleValue();

                    BaseValue.edit(replay, (r) -> r.shift(tickv));
                }
            }
        });

        tick.setText(LAST_OFFSET);
        tick.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_EXPRESSION_TOOLTIP);
        tick.relative(panel.confirm).y(-1F, -5).w(1F).h(20);

        panel.confirm.w(1F, -10);
        panel.content.add(tick);

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void copyReplay()
    {
        MapType replays = new MapType();
        ListType replayList = new ListType();

        replays.put("replays", replayList);

        for (Replay replay : this.getCurrent())
        {
            replayList.add(replay.toData());
        }

        Window.setClipboard(replays, "_CopyReplay");
    }

    private void pasteReplay(MapType data)
    {
        Film film = this.panel.getData();
        ListType replays = data.getList("replays");
        Replay last = null;

        for (BaseType replayType : replays)
        {
            Replay replay = film.replays.addReplay();

            BaseValue.edit(replay, (r) -> r.fromData(replayType));
            replay.uuid.set(java.util.UUID.randomUUID().toString());

            last = replay;
        }

        if (last != null)
        {
            this.buildVisualList();
            this.setCurrentDirect(last);
            this.panel.replayEditor.setReplay(last);
            this.updateFilmEditor();
        }
    }

    public void openFormEditor(ValueForm form, boolean editing, Consumer<Form> consumer)
    {
        UIElement target = this.panel;

        if (this.getRoot() != null)
        {
            target = this.getParentContainer();
        }

        UIFormPalette palette = UIFormPalette.open(target, editing, form.get(), (f) ->
        {
            for (Replay replay : this.getCurrent())
            {
                replay.form.set(FormUtils.copy(f));
            }

            this.updateFilmEditor();

            if (consumer != null)
            {
                consumer.accept(f);
            }
            else
            {
                this.overlay.pickEdit.setForm(f);
            }
        });

        palette.updatable();
    }

    private void addReplay()
    {
        World world = MinecraftClient.getInstance().world;
        Camera camera = this.panel.getCamera();

        BlockHitResult blockHitResult = RayTracing.rayTrace(world, camera, 64F);
        Vec3d p = blockHitResult.getPos();
        Vector3d position = new Vector3d(p.x, p.y, p.z);

        if (blockHitResult.getType() == HitResult.Type.MISS)
        {
            position.set(camera.getLookDirection()).mul(5F).add(camera.position);
        }

        this.addReplay(position, camera.rotation.x, camera.rotation.y + MathUtils.PI);
    }

    private void fromCamera(int duration)
    {
        Position position = new Position();
        Clips camera = this.panel.getData().camera;
        CameraClipContext context = new CameraClipContext();

        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();

        context.clips = camera;

        for (int i = 0; i < duration; i++)
        {
            context.clipData.clear();
            context.setup(i, 0F);

            for (Clip clip : context.clips.getClips(i))
            {
                context.apply(clip, position);
            }

            context.currentLayer = 0;

            float yaw = position.angle.yaw - 180;

            replay.keyframes.x.insert(i, position.point.x);
            replay.keyframes.y.insert(i, position.point.y);
            replay.keyframes.z.insert(i, position.point.z);
            replay.keyframes.yaw.insert(i, (double) yaw);
            replay.keyframes.headYaw.insert(i, (double) yaw);
            replay.keyframes.bodyYaw.insert(i, (double) yaw);
            replay.keyframes.pitch.insert(i, (double) position.angle.pitch);
        }

        this.buildVisualList();
        this.setCurrentDirect(replay);
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();

        this.openFormEditor(replay.form, false, null);
    }

    private void fromModelBlock()
    {
        ArrayList<ModelBlockEntity> modelBlocks = new ArrayList<>(BBSRendering.capturedModelBlocks);
        UISearchList<String> search = new UISearchList<>(new UIStringList(null));
        UIList<String> list = search.list;
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK_DESCRIPTION, (b) ->
        {
            if (b)
            {
                int index = list.getIndex();
                ModelBlockEntity modelBlock = CollectionUtils.getSafe(modelBlocks, index);

                if (modelBlock != null)
                {
                    this.fromModelBlock(modelBlock);
                }
            }
        });

        modelBlocks.sort(Comparator.comparing(ModelBlockEntity::getName));

        for (ModelBlockEntity modelBlock : modelBlocks)
        {
            list.add(modelBlock.getName());
        }

        list.background();
        search.relative(panel.confirm).y(-5).w(1F).h(16 * 9 + 20).anchor(0F, 1F);

        panel.confirm.w(1F, -10);
        panel.content.add(search);

        UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
    }

    private void fromModelBlock(ModelBlockEntity modelBlock)
    {
        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();
        BlockPos blockPos = modelBlock.getPos();
        ModelProperties properties = modelBlock.getProperties();
        Transform transform = properties.getTransform().copy();
        double x = blockPos.getX() + transform.translate.x + 0.5D;
        double y = blockPos.getY() + transform.translate.y;
        double z = blockPos.getZ() + transform.translate.z + 0.5D;

        transform.translate.set(0, 0, 0);

        replay.shadow.set(properties.isShadow());
        replay.form.set(FormUtils.copy(properties.getForm()));
        replay.keyframes.x.insert(0, x);
        replay.keyframes.y.insert(0, y);
        replay.keyframes.z.insert(0, z);

        if (!transform.isDefault())
        {
            if (
                transform.rotate.x == 0 && transform.rotate.z == 0 &&
                transform.rotate2.x == 0 && transform.rotate2.y == 0 && transform.rotate2.z == 0 &&
                transform.scale.x == 1 && transform.scale.y == 1 && transform.scale.z == 1
            ) {
                double yaw = -Math.toDegrees(transform.rotate.y);

                replay.keyframes.yaw.insert(0, yaw);
                replay.keyframes.headYaw.insert(0, yaw);
                replay.keyframes.bodyYaw.insert(0, yaw);
            }
            else
            {
                AnchorForm form = new AnchorForm();
                BodyPart part = new BodyPart("");

                part.setForm(replay.form.get());
                form.transform.set(transform);
                form.parts.addBodyPart(part);

                replay.form.set(form);
            }
        }

        this.buildVisualList();
        this.setCurrentDirect(replay);
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();
    }

    public void addReplay(Vector3d position, float pitch, float yaw)
    {
        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();

        replay.keyframes.x.insert(0, position.x);
        replay.keyframes.y.insert(0, position.y);
        replay.keyframes.z.insert(0, position.z);

        replay.keyframes.pitch.insert(0, (double) pitch);
        replay.keyframes.yaw.insert(0, (double) yaw);
        replay.keyframes.headYaw.insert(0, (double) yaw);
        replay.keyframes.bodyYaw.insert(0, (double) yaw);

        this.buildVisualList();
        this.setCurrentDirect(replay);
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();

        this.openFormEditor(replay.form, false, null);
    }

    private void updateFilmEditor()
    {
        this.panel.getController().createEntities();
        this.panel.replayEditor.updateChannelsList();
    }

    private void dupeReplay()
    {
        if (this.isDeselected())
        {
            return;
        }

        Replay last = null;

        for (Replay replay : this.getCurrent())
        {
            Film film = this.panel.getData();
            Replay newReplay = film.replays.addReplay();

            newReplay.copy(replay);
            newReplay.uuid.set(java.util.UUID.randomUUID().toString());

            last = newReplay;
        }

        if (last != null)
        {
            this.buildVisualList();
            this.setCurrentDirect(last);
            this.panel.replayEditor.setReplay(last);
            this.updateFilmEditor();
        }
    }

    private void removeReplay()
    {
        if (this.isDeselected())
        {
            return;
        }

        Film film = this.panel.getData();
        int index = this.getIndex();

        for (Replay replay : this.getCurrent())
        {
            if (replay.isGroup.get())
            {
                this.reparentChildren(replay);
            }

            film.replays.remove(replay);
        }

        int size = this.list.size();
        index = MathUtils.clamp(index, 0, size - 1);

        this.buildVisualList();
        size = this.list.size();
        this.panel.replayEditor.setReplay(size == 0 ? null : CollectionUtils.getSafe(this.list, index));
        this.updateFilmEditor();
    }

    private void reparentChildren(Replay groupToDelete)
    {
        Film data = this.panel.getData();
        List<Replay> allReplays = data.replays.getAllTyped();

        String targetPath = getReplayPath(groupToDelete);
        String targetID = groupToDelete.uuid.get();
        String childPrefix = targetPath.isEmpty() ? targetID : targetPath + "/" + targetID;
        String newParentPath = targetPath;

        for (Replay r : allReplays)
        {
            if (r == groupToDelete) continue;

            String g = r.group.get();

            if (g.equals(childPrefix) || g.startsWith(childPrefix + "/"))
            {
                String suffix = g.substring(childPrefix.length());
                String newPath;

                if (newParentPath.isEmpty())
                {
                    newPath = suffix.startsWith("/") ? suffix.substring(1) : suffix;
                }
                else
                {
                    newPath = newParentPath + suffix;
                }

                r.group.set(newPath);
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        if (this.panel != null && this.panel.getData() != null)
        {
            this.buildVisualList();
        }

        super.render(context);
    }

    @Override
    protected String elementToString(UIContext context, int i, Replay element)
    {
        return context.batcher.getFont().limitToWidth(element.getName(), this.area.w - 20);
    }

    @Override
    protected void renderElementPart(UIContext context, Replay element, int i, int x, int y, boolean hover, boolean selected)
    {
        int depth = getReplayDepth(element);
        int indent = depth * 10;
        int textX = x + indent;

        if (element.isGroup.get())
        {
            String path = getReplayPath(element);
            String myPath = path.isEmpty() ? element.uuid.get() : path + "/" + element.uuid.get();
            boolean expanded = this.expandedGroups.getOrDefault(myPath, true);
            Icon icon = expanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT;

            context.batcher.icon(icon, textX, y + 2);
            textX += 12;
        }

        if (element.enabled.get())
        {
            super.renderElementPart(context, element, i, textX, y, hover, selected);
        }
        else
        {
            context.batcher.textShadow(this.elementToString(context, i, element), textX + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.mulRGB(Colors.HIGHLIGHT, 0.75F) : Colors.GRAY);
        }

        Form form = element.form.get();

        if (form != null)
        {
            x += this.area.w - 30;

            context.batcher.clip(x, y, 40, 20, context);

            y -= 10;

            FormUtilsClient.renderUI(form, context, x, y, x + 40, y + 40);

            context.batcher.unclip(context);

            if (element.fp.get())
            {
                context.batcher.outlinedIcon(Icons.ARROW_UP, x, y + 20, 0.5F, 0.5F);
            }
        }
    }

    private void addGroup()
    {
        Film film = this.panel.getData();
        Replay group = new Replay("replay");

        group.uuid.set(java.util.UUID.randomUUID().toString());
        group.isGroup.set(true);
        group.label.set("New Group");

        List<Replay> selected = this.getCurrent();

        if (!selected.isEmpty())
        {
            List<Replay> list = film.replays.getAllTyped();
            Replay first = selected.get(0);
            
            int insertionIndex = list.size();

            for (Replay r : selected)
            {
                int index = list.indexOf(r);

                if (index != -1 && index < insertionIndex)
                {
                    insertionIndex = index;
                }
            }
            
            String parentPath = first.group.get();

            group.group.set(parentPath);
            
            String newGroupPath = parentPath.isEmpty() ? group.uuid.get() : parentPath + "/" + group.uuid.get();
            
            list.removeAll(selected);
            
            for (Replay r : selected)
            {
                r.group.set(newGroupPath);
            }
            
            if (insertionIndex > list.size())
            {
                insertionIndex = list.size();
            }
            
            list.add(insertionIndex, group);
            list.addAll(insertionIndex + 1, selected);
            
            this.expandedGroups.put(newGroupPath, true);
        }
        else
        {
            film.replays.add(group);
        }

        film.replays.sync();

        this.buildVisualList();
        this.updateFilmEditor();
    }

    public void buildVisualList()
    {
        if (this.panel == null || this.panel.getData() == null) return;

        List<Replay> selected = new ArrayList<>();

        if (this.list != null && !this.list.isEmpty())
        {
             selected = this.getCurrent();
        }

        List<Replay> all = this.panel.getData().replays.getList();

        this.visualList.clear();

        for (Replay r : all)
        {
            String path = getReplayPath(r);

            if (path.isEmpty() || isPathExpanded(path))
            {
                this.visualList.add(r);
            }
        }

        this.setList(this.visualList);
        this.current.clear();

        for (Replay r : selected)
        {
            int index = this.visualList.indexOf(r);

            if (index != -1)
            {
                this.current.add(index);
            }
        }
    }

    private boolean isPathExpanded(String path)
    {
        String[] parts = path.split("/");
        String current = "";

        for (String part : parts)
        {
            current = current.isEmpty() ? part : current + "/" + part;

            if (!this.expandedGroups.getOrDefault(current, true))
            {
                return false;
            }
        }

        return true;
    }

    public void updateGroupPath(String oldFullPath, String newFullPath)
    {
        Film film = this.panel.getData();
        List<Replay> all = film.replays.getList();
        boolean changed = false;

        if (this.expandedGroups.containsKey(oldFullPath))
        {
            this.expandedGroups.put(newFullPath, this.expandedGroups.remove(oldFullPath));
        }

        for (Replay r : all)
        {
            String group = r.group.get();
            
            if (group.equals(oldFullPath) || group.startsWith(oldFullPath + "/"))
            {
                String suffix = group.substring(oldFullPath.length());
                r.group.set(newFullPath + suffix);
                changed = true;
            }
        }

        if (changed)
        {
            film.replays.sync();
            this.buildVisualList();
            this.updateFilmEditor();
        }
    }

    public String getReplayPath(Replay r)
    {
        return r.group.get();
    }

    private int getReplayDepth(Replay r)
    {
        String path = getReplayPath(r);
        return path.isEmpty() ? 0 : path.split("/").length;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (context.mouseButton == 0)
        {
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);

            if (this.exists(index))
            {
                Replay r = this.list.get(index);
                int depth = getReplayDepth(r);
                int indent = depth * 10;
                int x = this.area.x + indent;

                if (r.isGroup.get() && context.mouseX >= x && context.mouseX < x + 16)
                {
                    String path = getReplayPath(r);
                    String myPath = path.isEmpty() ? r.uuid.get() : path + "/" + r.uuid.get();

                    boolean expanded = this.expandedGroups.getOrDefault(myPath, true);

                    this.expandedGroups.put(myPath, !expanded);
                    this.buildVisualList();

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

}
