package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.Waveform;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIRenameSheetOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIAnimationToPoseOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class UIReplaysEditor extends UIElement
{
    private static final Map<String, Integer> COLORS = new HashMap<>();
    private static final Map<String, Icon> ICONS = new HashMap<>();
    private static String lastFilm = "";
    private static int lastReplay;

    public UIReplaysOverlayPanel replays;

    /* Keyframes */
    public UIKeyframeEditor keyframeEditor;

    /* Clips */
    private UIFilmPanel filmPanel;
    private Film film;
    private Replay replay;
    private Set<String> keys = new LinkedHashSet<>();
    private Keyframe lastPickedKeyframe;
    private final Map<String, Boolean> collapsedModelTracks = new HashMap<>();

    static
    {
        COLORS.put("x", Colors.RED);
        COLORS.put("y", Colors.GREEN);
        COLORS.put("z", Colors.BLUE);
        COLORS.put("vX", Colors.RED);
        COLORS.put("vY", Colors.GREEN);
        COLORS.put("vZ", Colors.BLUE);
        COLORS.put("yaw", Colors.YELLOW);
        COLORS.put("pitch", Colors.CYAN);
        COLORS.put("headYaw", Colors.WHITE);
        COLORS.put("bodyYaw", Colors.MAGENTA);

        COLORS.put("stick_lx", Colors.RED);
        COLORS.put("stick_ly", Colors.GREEN);
        COLORS.put("stick_rx", Colors.RED);
        COLORS.put("stick_ry", Colors.GREEN);
        COLORS.put("trigger_l", Colors.RED);
        COLORS.put("trigger_r", Colors.GREEN);
        COLORS.put("extra1_x", Colors.RED);
        COLORS.put("extra1_y", Colors.GREEN);
        COLORS.put("extra2_x", Colors.RED);
        COLORS.put("extra2_y", Colors.GREEN);
        COLORS.put("shadow_size", Colors.MAGENTA);
        COLORS.put("shadow_opacity", Colors.ORANGE);

        COLORS.put("visible", Colors.WHITE & Colors.RGB);
        COLORS.put("pose", Colors.RED);
        COLORS.put("pose_overlay", Colors.ORANGE);
        COLORS.put("transform", Colors.GREEN);
        COLORS.put("transform_overlay", 0xaaff00);
        COLORS.put("color", Colors.INACTIVE);
        COLORS.put("lighting", Colors.YELLOW);
        COLORS.put("structure_light", Colors.YELLOW);
        COLORS.put("shape_keys", Colors.PINK);
        COLORS.put("actions", Colors.MAGENTA);

        COLORS.put("item_main_hand", Colors.ORANGE);
        COLORS.put("item_off_hand", Colors.ORANGE);
        COLORS.put("item_head", Colors.ORANGE);
        COLORS.put("item_chest", Colors.ORANGE);
        COLORS.put("item_legs", Colors.ORANGE);
        COLORS.put("item_feet", Colors.ORANGE);

        COLORS.put("user1", Colors.RED);
        COLORS.put("user2", Colors.ORANGE);
        COLORS.put("user3", Colors.GREEN);
        COLORS.put("user4", Colors.BLUE);
        COLORS.put("user5", Colors.RED);
        COLORS.put("user6", Colors.ORANGE);

        COLORS.put("frequency", Colors.RED);
        COLORS.put("count", Colors.GREEN);

        COLORS.put("settings", Colors.MAGENTA);
        COLORS.put("block_state", Colors.ACTIVE);
        COLORS.put("item_stack", Colors.ORANGE);
        COLORS.put("modelTransform", Colors.YELLOW);
        COLORS.put("enabled", Colors.WHITE & Colors.RGB);
        COLORS.put("level", Colors.YELLOW);
        COLORS.put("emit_light", Colors.YELLOW);
        COLORS.put("light_intensity", Colors.YELLOW);
        COLORS.put("structure_light", Colors.YELLOW);
        COLORS.put("biome_id", Colors.GREEN);
        COLORS.put("effect", Colors.MAGENTA);
        COLORS.put("offset_x", Colors.RED);
        COLORS.put("offset_y", Colors.GREEN);
        COLORS.put("offset_z", Colors.BLUE);

        ICONS.put("x", Icons.X);
        ICONS.put("y", Icons.Y);
        ICONS.put("z", Icons.Z);
        ICONS.put("yaw", Icons.Y);
        ICONS.put("pitch", Icons.X);
        ICONS.put("headYaw", Icons.Y);
        ICONS.put("bodyYaw", Icons.Y);

        ICONS.put("visible", Icons.VISIBLE);
        ICONS.put("texture", Icons.MATERIAL);
        ICONS.put("pose", Icons.POSE);
        ICONS.put("transform", Icons.ALL_DIRECTIONS);
        ICONS.put("color", Icons.BUCKET);
        ICONS.put("lighting", Icons.LIGHT);
        ICONS.put("structure_light", Icons.LIGHT);
        ICONS.put("actions", Icons.CONVERT);
        ICONS.put("shape_keys", Icons.HEART_ALT);
        ICONS.put("text", Icons.FONT);

        ICONS.put("stick_lx", Icons.LEFT_STICK);
        ICONS.put("stick_rx", Icons.RIGHT_STICK);
        ICONS.put("trigger_l", Icons.TRIGGER);
        ICONS.put("extra1_x", Icons.CURVES);
        ICONS.put("extra2_x", Icons.CURVES);
        ICONS.put("shadow_size", Icons.SCALE);
        ICONS.put("shadow_opacity", Icons.VISIBLE);
        ICONS.put("item_main_hand", Icons.LIMB);

        ICONS.put("user1", Icons.PARTICLE);

        ICONS.put("paused", Icons.TIME);
        ICONS.put("frequency", Icons.STOPWATCH);
        ICONS.put("count", Icons.BUCKET);

        ICONS.put("settings", Icons.GEAR);
        ICONS.put("block_state", Icons.BLOCK);
        ICONS.put("item_stack", Icons.LIMB);
        ICONS.put("modelTransform", Icons.ALL_DIRECTIONS);
        ICONS.put("enabled", Icons.VISIBLE);
        ICONS.put("level", Icons.LIGHT);
        ICONS.put("emit_light", Icons.LIGHT);
        ICONS.put("light_intensity", Icons.LIGHT);
        ICONS.put("structure_light", Icons.LIGHT);
        ICONS.put("biome_id", Icons.MATERIAL);
        ICONS.put("effect", Icons.PARTICLE);

        /* Structure selection icon for structure_file property */
        ICONS.put("structure_file", Icons.FILE);
    }

    private void collectLimbTracks(Form form, Set<String> propertyPaths)
    {
        if (form == null || !form.animatable.get())
        {
            return;
        }

        if (form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model != null)
            {
                String path = FormUtils.getPath(modelForm);
                List<Pair<String, Integer>> orderedBones = this.collectBoneOrder(model.model);

                for (Pair<String, Integer> bone : orderedBones)
                {
                    if (bone.a.startsWith("armor_") || bone.a.endsWith("_item"))
                    {
                        continue;
                    }

                    propertyPaths.add(StringUtils.combinePaths(path, "pose") + ":" + bone.a);
                    propertyPaths.add(StringUtils.combinePaths(path, "pose_overlay") + ":" + bone.a);

                    for (int i = 0, c = modelForm.additionalOverlays.size(); i < c; i++)
                    {
                        propertyPaths.add(StringUtils.combinePaths(path, "pose_overlay" + i) + ":" + bone.a);
                    }
                }
            }
        }

        for (BodyPart part : form.parts.getAllTyped())
        {
            this.collectLimbTracks(part.getForm(), propertyPaths);
        }
    }

    private void orderLimbTracks(Form form, List<UIKeyframeSheet> limbs)
    {
        if (form == null || limbs.isEmpty())
        {
            return;
        }

        if (!(form instanceof ModelForm modelForm))
        {
            return;
        }

        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model == null)
        {
            return;
        }

        List<Pair<String, Integer>> orderedBones = this.collectBoneOrder(model.model);

        if (orderedBones.isEmpty())
        {
            return;
        }

        Map<String, UIKeyframeSheet> limbByBone = new HashMap<>();

        for (UIKeyframeSheet limb : limbs)
        {
            int colon = limb.id.indexOf(':');

            if (colon == -1)
            {
                continue;
            }

            limbByBone.put(limb.id.substring(colon + 1), limb);
        }

        Map<String, String> parentByBone = this.collectBoneParents(model.model);
        Map<String, List<String>> childrenByBone = this.collectChildren(parentByBone);

        int baseLevel = limbs.get(0).level;
        List<UIKeyframeSheet> reordered = new ArrayList<>();
        Set<UIKeyframeSheet> used = new HashSet<>();

        for (Pair<String, Integer> bone : orderedBones)
        {
            UIKeyframeSheet limb = limbByBone.get(bone.a);

            if (limb == null)
            {
                continue;
            }

            if (this.isAncestorCollapsed(limb, parentByBone))
            {
                /* Mark as handled so collapsed descendants don't get re-added by fallback pass. */
                used.add(limb);
                continue;
            }

            limb.level = baseLevel + bone.b;
            this.applyLimbExpandState(limb, bone.a, childrenByBone, limbByBone);
            reordered.add(limb);
            used.add(limb);
        }

        for (UIKeyframeSheet limb : limbs)
        {
            if (used.contains(limb))
            {
                continue;
            }

            limb.level = baseLevel;
            limb.toggleExpanded = null;
            reordered.add(limb);
        }

        limbs.clear();
        limbs.addAll(reordered);
    }

    private void applyLimbExpandState(UIKeyframeSheet limb, String boneName, Map<String, List<String>> childrenByBone, Map<String, UIKeyframeSheet> limbByBone)
    {
        if (!this.hasChildTrack(boneName, childrenByBone, limbByBone))
        {
            limb.toggleExpanded = null;
            return;
        }

        String key = this.replay.uuid.get() + ":" + limb.id;
        boolean expanded = !this.collapsedModelTracks.getOrDefault(key, false);

        limb.expanded = expanded;
        limb.toggleExpanded = () ->
        {
            this.collapsedModelTracks.put(key, !this.collapsedModelTracks.getOrDefault(key, false));
            this.updateChannelsList();
        };
    }

    private boolean hasChildTrack(String boneName, Map<String, List<String>> childrenByBone, Map<String, UIKeyframeSheet> limbByBone)
    {
        List<String> children = childrenByBone.get(boneName);

        if (children == null || children.isEmpty())
        {
            return false;
        }

        for (String child : children)
        {
            if (limbByBone.containsKey(child))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isAncestorCollapsed(UIKeyframeSheet limb, Map<String, String> parentByBone)
    {
        int colon = limb.id.indexOf(':');

        if (colon == -1)
        {
            return false;
        }

        String poseTrackId = limb.id.substring(0, colon);
        String boneName = limb.id.substring(colon + 1);
        String parent = parentByBone.get(boneName);

        while (parent != null)
        {
            String key = this.replay.uuid.get() + ":" + poseTrackId + ":" + parent;

            if (this.collapsedModelTracks.getOrDefault(key, false))
            {
                return true;
            }

            parent = parentByBone.get(parent);
        }

        return false;
    }

    private Map<String, String> collectBoneParents(IModel model)
    {
        Map<String, String> parentByBone = new HashMap<>();

        if (model instanceof Model cubicModel)
        {
            this.collectBoneParentsFromGroups(cubicModel.topGroups, null, parentByBone);
        }
        else
        {
            Collection<BOBJBone> bones = model.getAllBOBJBones();

            if (bones != null && !bones.isEmpty())
            {
                for (BOBJBone bone : bones)
                {
                    if (bone.parentBone != null)
                    {
                        parentByBone.put(bone.name, bone.parentBone.name);
                    }
                }
            }
        }

        return parentByBone;
    }

    private void collectBoneParentsFromGroups(List<ModelGroup> groups, String parent, Map<String, String> parentByBone)
    {
        for (ModelGroup group : groups)
        {
            if (parent != null)
            {
                parentByBone.put(group.id, parent);
            }

            if (!group.children.isEmpty())
            {
                this.collectBoneParentsFromGroups(group.children, group.id, parentByBone);
            }
        }
    }

    private Map<String, List<String>> collectChildren(Map<String, String> parentByBone)
    {
        Map<String, List<String>> childrenByBone = new HashMap<>();

        for (Map.Entry<String, String> entry : parentByBone.entrySet())
        {
            childrenByBone.computeIfAbsent(entry.getValue(), (key) -> new ArrayList<>()).add(entry.getKey());
        }

        return childrenByBone;
    }

    private List<Pair<String, Integer>> collectBoneOrder(IModel model)
    {
        List<Pair<String, Integer>> orderedBones = new ArrayList<>();

        if (model instanceof Model cubicModel)
        {
            this.collectBonesFromGroups(cubicModel.topGroups, 0, orderedBones);
        }
        else
        {
            Collection<BOBJBone> bones = model.getAllBOBJBones();

            if (bones != null && !bones.isEmpty())
            {
                for (BOBJBone bone : bones)
                {
                    int depth = 0;
                    BOBJBone parent = bone.parentBone;

                    while (parent != null)
                    {
                        depth += 1;
                        parent = parent.parentBone;
                    }

                    orderedBones.add(new Pair<>(bone.name, depth));
                }
            }
            else
            {
                for (String bone : model.getAllGroupKeys())
                {
                    orderedBones.add(new Pair<>(bone, 0));
                }
            }
        }

        return orderedBones;
    }

    private void collectBonesFromGroups(List<ModelGroup> groups, int depth, List<Pair<String, Integer>> orderedBones)
    {
        for (ModelGroup group : groups)
        {
            orderedBones.add(new Pair<>(group.id, depth));

            if (!group.children.isEmpty())
            {
                this.collectBonesFromGroups(group.children, depth + 1, orderedBones);
            }
        }
    }

    public static Icon getIcon(String key)
    {
        if (key.indexOf(':') != -1)
        {
            return null;
        }

        String topLevel = StringUtils.fileName(key);

        if (topLevel.startsWith("pose_overlay"))
        {
            return Icons.POSE;
        }

        if (topLevel.startsWith("transform_overlay"))
        {
            return Icons.ALL_DIRECTIONS;
        }

        return ICONS.getOrDefault(topLevel, Icons.NONE);
    }

    public static int getColor(String key)
    {
        int colon = key.indexOf(':');

        if (colon != -1)
        {
            String propertyPath = key.substring(0, colon);
            String propertyName = StringUtils.fileName(propertyPath);

            if (propertyName.equals("pose") || propertyName.startsWith("pose_overlay"))
            {
                return getPoseBoneColor(key.substring(colon + 1), propertyName.startsWith("pose_overlay"));
            }

            return 0xff3333;
        }

        String topLevel = StringUtils.fileName(key);

        if (topLevel.startsWith("pose_overlay")) return COLORS.get("pose_overlay");
        if (topLevel.startsWith("transform_overlay")) return COLORS.get("transform_overlay");

        return COLORS.getOrDefault(topLevel, Colors.ACTIVE);
    }

    private static int getPoseBoneColor(String boneName, boolean overlay)
    {
        int hash = Integer.rotateLeft(boneName.hashCode(), 13) ^ boneName.hashCode();
        float hue = (hash & 0xffff) / 65535F;
        float saturation = overlay ? 0.52F : 0.72F;
        float brightness = overlay ? 0.95F : 0.9F;
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0x00ffffff;

        return 0xff000000 | rgb;
    }

    public static void offerAdjacent(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getAdjacentGroups(bone))
                {
                    if (modelGroup.endsWith("_item"))
                    {
                        continue;
                    }

                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static void offerHierarchy(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getHierarchyGroups(bone))
                {
                    if (modelGroup.endsWith("_item"))
                    {
                        continue;
                    }

                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static final Form DUMMY_FORM = new StructureForm();

    public static boolean renderBackground(UIContext context, UIKeyframes keyframes, Clips camera, int clipOffset, Clip selectedClip)
    {
        Scale scale = keyframes.getXAxis();
        boolean renderedOnce = false;
        Area area = new Area();
        area.copy(keyframes.area);
        area.x += IUIKeyframeGraph.SIDEBAR_WIDTH;
        area.w -= IUIKeyframeGraph.SIDEBAR_WIDTH;

        context.batcher.clip(area, context);

        /* First pass: Render selected clip background */
        for (Clip clip : camera.get())
        {
            if (clip == selectedClip && !(clip instanceof AudioClip))
            {
                float offset = clip.tick.get() - clipOffset;
                int x1 = (int) scale.to(offset);
                int x2 = (int) scale.to(offset + clip.duration.get());
                int y = keyframes.area.y + 15;
                int h = 20;

                if (x2 > keyframes.area.x && x1 < keyframes.area.ex())
                {
                    ClipFactoryData data = camera.getFactory().getData(clip);
                    int color = data.color;
                    int primary = BBSSettings.primaryColor.get();

                    context.batcher.dropShadow(x1 + 2, y + 2, x2 - 2, y + h - 2, 8, Colors.A75 + primary, primary);
                    context.batcher.box(x1, y, x2, y + h, color | Colors.A100);
                    context.batcher.outline(x1, y, x2, y + h, Colors.WHITE);

                    if (x2 - x1 > 20)
                    {
                        context.batcher.icon(data.icon, Colors.mulA(Colors.mulRGB(Colors.WHITE, 0.75F), 0.5F), x2 - 2, y + h / 2, 1F, 0.5F);
                    }

                    renderedOnce = true;
                }
            }
        }

        /* Second pass: Render audio waveforms on top */
        for (Clip clip : camera.get())
        {
            if (clip instanceof AudioClip audioClip)
            {
                if (!BBSSettings.audioWaveformVisible.get())
                {
                    continue;
                }

                Link link = audioClip.audio.get();

                if (link == null)
                {
                    continue;
                }

                SoundBuffer buffer = BBSModClient.getSounds().get(link, true);

                if (buffer == null || buffer.getWaveform() == null)
                {
                    continue;
                }

                Waveform wave = buffer.getWaveform();

                if (wave != null)
                {
                    int audioOffset = audioClip.offset.get();
                    float offset = audioClip.tick.get() - clipOffset;
                    int duration = Math.min((int) (wave.getDuration() * 20), clip.duration.get());

                    int x1 = (int) scale.to(offset);
                    int x2 = (int) scale.to(offset + duration);

                    wave.render(context.batcher, Colors.WHITE, x1, keyframes.area.y + 15, x2 - x1, 20, TimeUtils.toSeconds(audioOffset), TimeUtils.toSeconds(audioOffset + duration));

                    renderedOnce = true;
                }
            }
        }

        context.batcher.unclip(context);

        return renderedOnce;
    }

    public UIReplaysEditor(UIFilmPanel filmPanel)
    {
        this.filmPanel = filmPanel;
        this.replays = new UIReplaysOverlayPanel(filmPanel, (replay) -> this.setReplay(replay, false, true));

        this.markContainer();
    }

    public void setFilm(Film film)
    {
        this.film = film;

        if (film != null)
        {
            List<Replay> replays = film.replays.getList();
            int index = film.getId().equals(lastFilm) ? lastReplay : 0;

            if (!CollectionUtils.inRange(replays, index))
            {
                index = 0;
            }

            this.replays.replays.setList(replays);
            this.setReplay(replays.isEmpty() ? null : replays.get(index));
        }
    }

    public Replay getReplay()
    {
        return this.replay;
    }

    public void setReplay(Replay replay)
    {
        this.setReplay(replay, true, true);
    }

    public void setReplay(Replay replay, boolean select, boolean resetOrbit)
    {
        this.replay = replay;

        BBSModClient.setSelectedReplay(replay);

        if (resetOrbit)
        {
            this.filmPanel.getController().orbit.reset();
        }

        this.replays.setReplay(replay);
        this.filmPanel.actionEditor.setClips(replay == null ? null : replay.actions);
        this.updateChannelsList();

        if (select)
        {
            this.replays.replays.ensureVisible(replay);
            this.replays.replays.setCurrentScroll(replay);
        }
    }

    public void moveReplay(double x, double y, double z)
    {
        if (this.replay != null)
        {
            int cursor = this.filmPanel.getCursor();

            this.replay.keyframes.x.insert(cursor, x);
            this.replay.keyframes.y.insert(cursor, y);
            this.replay.keyframes.z.insert(cursor, z);
        }
    }

    private static final List<String> WORLD_CHANNELS = Arrays.asList("x", "y", "z", "vX", "vY", "vZ", "yaw", "pitch", "headYaw", "bodyYaw", "grounded", "damage", "fall", "sneaking", "sprinting", "item_main_hand", "item_off_hand", "item_head", "item_chest", "item_legs", "item_feet", "selected_slot", "stick_lx", "stick_ly", "stick_rx", "stick_ry", "trigger_l", "trigger_r", "extra1_x", "extra1_y", "extra2_x", "extra2_y", "shadow_size", "shadow_opacity");
    private static final List<String> MODEL_PROPERTIES = Arrays.asList("visible", "lighting", "transform", "transform_overlay", "pose", "pose_overlay", "anchor", "color", "texture", "pbr_normal_intensity", "pbr_specular_intensity", "model", "actions", "shape_keys", "block_state", "item_stack", "modelTransform", "settings", "paused", "frequency", "count", "structure_file", "biome_id", "emit_light", "light_intensity", "structure_light", "enabled", "level", "effect");

    public void updateChannelsList()
    {
        UIKeyframes lastEditor = null;

        if (this.keyframeEditor != null)
        {
            this.keyframeEditor.removeFromParent();

            lastEditor = this.keyframeEditor.view;
        }

        if (this.replay == null)
        {
            return;
        }

        if (!this.replay.isGroup.get() && this.replay.form.get() == null)
        {
            return;
        }

        /* Replay keyframes */
        List<UIKeyframeSheet> sheets = new ArrayList<>();

        if (this.replay.isGroup.get())
        {
            /* Add only visible, color and transform properties for groups */
            String[] properties = {"visible", "color", "transform"};

            for (String key : properties)
            {
                KeyframeChannel property = this.replay.properties.getOrCreate(DUMMY_FORM, key);

                if (property != null)
                {
                    BaseValueBasic formProperty = FormUtils.getProperty(DUMMY_FORM, key);
                    UIKeyframeSheet sheet = new UIKeyframeSheet(getColor(key), false, property, formProperty);

                    sheets.add(sheet.icon(getIcon(key)));
                }
            }
        }
        else
        {
            for (String key : ReplayKeyframes.CURATED_CHANNELS)
            {
                BaseValue value = this.replay.keyframes.get(key);
                KeyframeChannel channel = (KeyframeChannel) value;

                String customTitle = this.replay.getCustomSheetTitle(key);
                Integer customColor = this.replay.getSheetColor(key);
                int baseColor = getColor(key);
                int sheetColor = customColor != null ? customColor : baseColor;

                UIKeyframeSheet sheet = customTitle != null && !customTitle.isEmpty()
                    ? new UIKeyframeSheet(key, IKey.constant(customTitle), sheetColor, false, channel, null)
                    : new UIKeyframeSheet(sheetColor, false, channel, null);

                sheets.add(sheet.icon(ICONS.get(key)));
            }

            /* Form properties */
            java.util.Set<String> propertyPaths = new java.util.LinkedHashSet<>(FormUtils.collectPropertyPaths(this.replay.form.get()));
            for (String key : this.replay.properties.properties.keySet())
            {
                if (this.isCompatiblePropertyPath(this.replay.form.get(), key))
                {
                    propertyPaths.add(key);
                }
            }

            this.collectLimbTracks(this.replay.form.get(), propertyPaths);

            for (String key : propertyPaths)
            {
                /* Ocultar/omitir la pista tint_block_entities y huesos de item */
                if (key.endsWith("tint_block_entities") || key.endsWith("_item"))
                {
                    continue;
                }
                KeyframeChannel property = this.replay.properties.getOrCreate(this.replay.form.get(), key);

                if (property != null)
                {
                    BaseValueBasic formProperty = FormUtils.getProperty(this.replay.form.get(), key);
                    String customTitle = this.replay.getCustomSheetTitle(key);
                    Integer customColor = this.replay.getSheetColor(key);
                    int baseColor = getColor(key);
                    int sheetColor = customColor != null ? customColor : baseColor;

                    String title = key;
                    int colon = key.indexOf(':');

                    if (colon != -1)
                    {
                        String boneName = key.substring(colon + 1);

                        title = boneName;
                    }

                    UIKeyframeSheet sheet = customTitle != null && !customTitle.isEmpty()
                        ? new UIKeyframeSheet(key, IKey.constant(customTitle), sheetColor, false, property, formProperty)
                        : new UIKeyframeSheet(key, IKey.constant(title), sheetColor, false, property, formProperty);

                    sheets.add(sheet.icon(getIcon(key)));
                }
            }
        }

        /* Sort sheets by form path and priority */
        sheets.sort((a, b) ->
        {
            Form formA = a.property == null ? null : FormUtils.getForm(a.property);
            Form formB = b.property == null ? null : FormUtils.getForm(b.property);
            String pathA = formA == null ? "" : FormUtils.getPath(formA);
            String pathB = formB == null ? "" : FormUtils.getPath(formB);

            int pathComp = comparePathsNaturally(pathA, pathB);

            if (pathComp != 0)
            {
                return pathComp;
            }

            java.util.function.ToIntFunction<UIKeyframeSheet> getPriority = (sheet) ->
            {
                String id = sheet.id;
                String name = StringUtils.fileName(id);

                int curatedIndex = ReplayKeyframes.CURATED_CHANNELS.indexOf(id);

                if (curatedIndex != -1)
                {
                    return -100 + curatedIndex;
                }

                if (name.equals("visible")) return 0;
                if (name.equals("lighting")) return 1;

                if (name.equals("transform")) return 10;
                if (name.startsWith("transform_overlay"))
                {
                    String suffix = name.substring("transform_overlay".length());
                    if (suffix.isEmpty()) return 11;
                    try { return 12 + Integer.parseInt(suffix); } catch (Exception e) { return 19; }
                }

                if (name.equals("pose")) return 20;
                if (name.startsWith("pose_overlay"))
                {
                    if (name.indexOf(':') != -1) return 29;
                    String suffix = name.substring("pose_overlay".length());
                    if (suffix.isEmpty()) return 21;
                    try { return 22 + Integer.parseInt(suffix); } catch (Exception e) { return 28; }
                }

                if (name.indexOf(':') != -1) return 29;

                if (name.equals("anchor")) return 30;
                if (name.equals("structure_file")) return 31;
                if (name.equals("pivot")) return 32;
                if (name.equals("biome_id")) return 33;
                if (name.equals("structure_light")) return 34;
                if (name.equals("color")) return 35;
                if (name.equals("texture")) return 36;
                if (name.equals("pbr_normal_intensity")) return 37;
                if (name.equals("pbr_specular_intensity")) return 38;
                if (name.equals("model")) return 39;

                return 500;
            };

            int priorityA = getPriority.applyAsInt(a);
            int priorityB = getPriority.applyAsInt(b);

            if (priorityA != priorityB)
            {
                return Integer.compare(priorityA, priorityB);
            }

            if (priorityA == 29)
            {
                String boneA = a.id.substring(a.id.indexOf(':') + 1);
                String boneB = b.id.substring(b.id.indexOf(':') + 1);

                return compareNaturally(boneA, boneB);
            }

            return 0;
        });

        this.keys.clear();

        for (UIKeyframeSheet sheet : sheets)
        {
            this.keys.add(StringUtils.fileName(sheet.id));
        }

        sheets.removeIf((v) ->
        {
            for (String s : BBSSettings.disabledSheets.get())
            {
                if (v.id.equals(s) || v.id.endsWith("/" + s))
                {
                    return true;
                }
            }

            return false;
        });

        List<UIKeyframeSheet> grouped = new ArrayList<>();
        Set<String> addedGroups = new HashSet<>();

        final boolean legacyOriginalLayout = false;

        if (legacyOriginalLayout && !this.replay.isGroup.get())
        {
            Form formObj = this.replay.form.get();

            if (formObj == null)
            {
                sheets = grouped;

                return;
            }

            Form rootForm = FormUtils.getRoot(formObj);
            String rootPath = FormUtils.getPath(rootForm);
            String rootKey = this.replay.uuid.get() + ":" + rootPath;
            boolean rootExpanded = !this.collapsedModelTracks.getOrDefault(rootKey, false);

            UIKeyframeSheet rootHeader = UIKeyframeSheet.groupHeader(
                "__group__" + rootKey,
                IKey.constant(rootForm.getDisplayName()),
                Colors.LIGHTEST_GRAY & Colors.RGB,
                rootKey,
                rootExpanded,
                () ->
                {
                    this.collapsedModelTracks.put(rootKey, !this.collapsedModelTracks.getOrDefault(rootKey, false));
                    this.updateChannelsList();
                }
            );

            rootHeader.level = 0;
            grouped.add(rootHeader);

            if (rootExpanded)
            {
                FormTracks rootTracks = new FormTracks(rootForm);
                Map<String, FormTracks> subForms = new LinkedHashMap<>();
                List<UIKeyframeSheet> otherTracks = new ArrayList<>();

                for (UIKeyframeSheet sheet : sheets)
                {
                    Form form = null;

                    if (sheet.property != null)
                    {
                        form = FormUtils.getForm(sheet.property);
                    }
                    else
                    {
                        int colon = sheet.id.indexOf(':');
                        String path = "";

                        if (colon != -1)
                        {
                            String propertyPath = sheet.id.substring(0, colon);
                            int lastSlash = propertyPath.lastIndexOf('/');

                            if (lastSlash != -1)
                            {
                                path = propertyPath.substring(0, lastSlash);
                            }
                        }

                        form = FormUtils.getForm(this.replay.form.get(), path);
                    }

                    if (form != null)
                    {
                        String path = FormUtils.getPath(form);

                        if (path.equals(rootPath))
                        {
                            this.processTrack(sheet, "", 1, rootTracks.before, rootTracks.pose, rootTracks.limbs, rootTracks.overlayRoots, rootTracks.overlayLimbs, rootTracks.after);
                        }
                        else
                        {
                            if (!subForms.containsKey(path))
                            {
                                subForms.put(path, new FormTracks(form));
                            }

                            this.processTrack(sheet, "", 1, subForms.get(path).before, subForms.get(path).pose, subForms.get(path).limbs, subForms.get(path).overlayRoots, subForms.get(path).overlayLimbs, subForms.get(path).after);
                        }
                    }
                    else
                    {
                        otherTracks.add(sheet);
                    }
                }

                this.orderLimbTracks(rootTracks.form, rootTracks.limbs);
                List<UIKeyframeSheet> orderedRootOverlays = this.orderOverlayTracks(rootTracks.form, rootTracks.overlayRoots, rootTracks.overlayLimbs);

                /* Add root tracks first */
                grouped.addAll(rootTracks.before);
                grouped.addAll(rootTracks.pose);
                grouped.addAll(orderedRootOverlays);
                grouped.addAll(rootTracks.limbs);
                grouped.addAll(rootTracks.after);

                /* Add sub-form tracks next */
                for (FormTracks subForm : subForms.values())
                {
                    /* Add sub-form pose tracks renamed to form name */
                    for (UIKeyframeSheet poseSheet : subForm.pose)
                    {
                        poseSheet.title = IKey.constant(subForm.form.getDisplayName());
                        grouped.add(poseSheet);
                    }

                    this.orderLimbTracks(subForm.form, subForm.limbs);
                    List<UIKeyframeSheet> orderedSubOverlays = this.orderOverlayTracks(subForm.form, subForm.overlayRoots, subForm.overlayLimbs);

                    grouped.addAll(subForm.before);
                    grouped.addAll(orderedSubOverlays);
                    grouped.addAll(subForm.limbs);
                    grouped.addAll(subForm.after);
                }

                grouped.addAll(otherTracks);

                /* Flatten levels and disable toggles */
                for (UIKeyframeSheet sheet : grouped)
                {
                    if (sheet != rootHeader)
                    {
                        sheet.level = 1;
                        sheet.toggleExpanded = null;
                    }
                }
            }

            sheets = grouped;
        }
        else if (!this.replay.isGroup.get())
        {
            Form rootForm = FormUtils.getRoot(this.replay.form.get());
            String rootPath = FormUtils.getPath(rootForm);
            String rootKey = this.replay.uuid.get() + ":" + rootPath;
            boolean rootExpanded = !this.collapsedModelTracks.getOrDefault(rootKey, false);

            UIKeyframeSheet rootHeader = UIKeyframeSheet.groupHeader(
                "__group__" + rootKey,
                IKey.constant(rootForm.getDisplayName()),
                Colors.LIGHTEST_GRAY & Colors.RGB,
                rootKey,
                rootExpanded,
                () ->
                {
                    this.collapsedModelTracks.put(rootKey, !this.collapsedModelTracks.getOrDefault(rootKey, false));
                    this.updateChannelsList();
                }
            );

            rootHeader.level = 0;
            grouped.add(rootHeader);

            if (rootExpanded)
            {
                String worldKey = this.replay.uuid.get() + ":__world__";
                boolean worldExpanded = !this.collapsedModelTracks.getOrDefault(worldKey, false);
                UIKeyframeSheet worldHeader = UIKeyframeSheet.groupHeader(
                    "__group__" + worldKey,
                    IKey.constant("World"),
                    Colors.LIGHTEST_GRAY & Colors.RGB,
                    worldKey,
                    worldExpanded,
                    () ->
                    {
                        this.collapsedModelTracks.put(worldKey, !this.collapsedModelTracks.getOrDefault(worldKey, false));
                        this.updateChannelsList();
                    }
                );

                worldHeader.level = 1;

                String modelPropsKey = this.replay.uuid.get() + ":__model__";
                boolean modelPropsExpanded = !this.collapsedModelTracks.getOrDefault(modelPropsKey, false);
                UIKeyframeSheet modelPropsHeader = UIKeyframeSheet.groupHeader(
                    "__group__" + modelPropsKey,
                    IKey.constant("Model"),
                    Colors.LIGHTEST_GRAY & Colors.RGB,
                    modelPropsKey,
                    modelPropsExpanded,
                    () ->
                    {
                        this.collapsedModelTracks.put(modelPropsKey, !this.collapsedModelTracks.getOrDefault(modelPropsKey, false));
                        this.updateChannelsList();
                    }
                );

                modelPropsHeader.level = 1;

                grouped.add(worldHeader);

                List<UIKeyframeSheet> worldTracks = new ArrayList<>();
                List<UIKeyframeSheet> modelTracksBeforePose = new ArrayList<>();
                List<UIKeyframeSheet> poseTrack = new ArrayList<>();
                List<UIKeyframeSheet> poseLimbTracks = new ArrayList<>();
                List<UIKeyframeSheet> overlayTracks = new ArrayList<>();
                List<UIKeyframeSheet> overlayLimbTracks = new ArrayList<>();
                List<UIKeyframeSheet> modelTracksAfterPose = new ArrayList<>();

                Map<String, FormTracks> subForms = new LinkedHashMap<>();

                for (UIKeyframeSheet sheet : sheets)
                {
                    if (WORLD_CHANNELS.contains(sheet.id))
                    {
                        if (!this.collapsedModelTracks.getOrDefault(worldKey, false))
                        {
                            sheet.level = 2;
                            worldTracks.add(sheet);
                        }
                    }
                    else if (MODEL_PROPERTIES.contains(sheet.id) || sheet.id.startsWith("pose") || sheet.id.startsWith("transform_overlay"))
                    {
                        if (!this.collapsedModelTracks.getOrDefault(modelPropsKey, false))
                        {
                            this.processTrack(sheet, modelPropsKey, 2, modelTracksBeforePose, poseTrack, poseLimbTracks, overlayTracks, overlayLimbTracks, modelTracksAfterPose);
                        }
                    }
                    else
                    {
                        Form form = null;

                        if (sheet.property != null)
                        {
                            form = FormUtils.getForm(sheet.property);
                        }
                        else
                        {
                            int colon = sheet.id.indexOf(':');
                            String path = "";

                            if (colon != -1)
                            {
                                String propertyPath = sheet.id.substring(0, colon);
                                int lastSlash = propertyPath.lastIndexOf('/');

                                if (lastSlash != -1)
                                {
                                    path = propertyPath.substring(0, lastSlash);
                                }
                            }

                            form = FormUtils.getForm(this.replay.form.get(), path);
                        }

                        if (form != null)
                        {
                            String path = FormUtils.getPath(form);

                            if (path.equals(rootPath))
                            {
                                if (!this.collapsedModelTracks.getOrDefault(modelPropsKey, false))
                                {
                                    this.processTrack(sheet, modelPropsKey, 2, modelTracksBeforePose, poseTrack, poseLimbTracks, overlayTracks, overlayLimbTracks, modelTracksAfterPose);
                                }

                                continue;
                            }

                            if (!subForms.containsKey(path))
                            {
                                subForms.put(path, new FormTracks(form));
                            }

                            String groupKey = this.replay.uuid.get() + ":" + path;

                            this.processTrack(sheet, groupKey, path.split("/").length, subForms.get(path).before, subForms.get(path).pose, subForms.get(path).limbs, subForms.get(path).overlayRoots, subForms.get(path).overlayLimbs, subForms.get(path).after);
                        }
                    }
                }

                this.orderLimbTracks(rootForm, poseLimbTracks);
                List<UIKeyframeSheet> orderedOverlayTracks = this.orderOverlayTracks(rootForm, overlayTracks, overlayLimbTracks);

                grouped.addAll(worldTracks);
                grouped.add(modelPropsHeader);
                grouped.addAll(modelTracksBeforePose);
                grouped.addAll(poseTrack);
                grouped.addAll(poseLimbTracks);
                grouped.addAll(orderedOverlayTracks);
                grouped.addAll(modelTracksAfterPose);

                List<String> subFormPaths = new ArrayList<>(subForms.keySet());
                subFormPaths.sort(UIReplaysEditor::comparePathsNaturally);

                for (String path : subFormPaths)
                {
                    FormTracks subForm = subForms.get(path);
                    String groupKey = this.replay.uuid.get() + ":" + path;
                    int level = path.split("/").length;

                    if (addedGroups.add(groupKey))
                    {
                        boolean expanded = !this.collapsedModelTracks.getOrDefault(groupKey, false);
                        UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
                            "__group__" + groupKey,
                            IKey.constant(subForm.form.getDisplayName()),
                            Colors.LIGHTEST_GRAY & Colors.RGB,
                            groupKey,
                            expanded,
                            () ->
                            {
                                this.collapsedModelTracks.put(groupKey, !this.collapsedModelTracks.getOrDefault(groupKey, false));
                                this.updateChannelsList();
                            }
                        );

                        header.level = level;
                        grouped.add(header);
                    }

                    if (!this.collapsedModelTracks.getOrDefault(groupKey, false))
                    {
                        this.orderLimbTracks(subForm.form, subForm.limbs);
                        List<UIKeyframeSheet> orderedSubOverlays = this.orderOverlayTracks(subForm.form, subForm.overlayRoots, subForm.overlayLimbs);

                        grouped.addAll(subForm.before);
                        grouped.addAll(subForm.pose);
                        grouped.addAll(subForm.limbs);
                        grouped.addAll(orderedSubOverlays);
                        grouped.addAll(subForm.after);
                    }
                }
            }

            sheets = grouped;
        }

        Object lastForm = null;

        for (UIKeyframeSheet sheet : sheets)
        {
            if (sheet.groupHeader)
            {
                sheet.separator = false;
                lastForm = null;
                continue;
            }

            Object form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (!Objects.equals(lastForm, form))
            {
                sheet.separator = true;
            }

            lastForm = form;
        }

        for (UIKeyframeSheet sheet : sheets)
        {
            if (sheet.property == null && "shadow_size".equals(sheet.id))
            {
                sheet.separator = false;
            }
        }

        if (!sheets.isEmpty())
        {
            this.lastPickedKeyframe = null;
            this.keyframeEditor = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.filmPanel.cameraEditor, (keyframe) ->
            {
                this.cleanupUntouchedAutomaticKeyframe(this.lastPickedKeyframe, keyframe);
                this.lastPickedKeyframe = keyframe;
                consumer.accept(keyframe);
            }).absolute()).target(this.filmPanel.editArea);
            this.keyframeEditor.full(this);
            this.keyframeEditor.setUndoId("replay_keyframe_editor");

            /* Reset */
            if (lastEditor != null)
            {
                this.keyframeEditor.view.copyViewport(lastEditor);
            }

            this.keyframeEditor.view.backgroundRenderer((context) ->
            {
                UIKeyframes view = this.keyframeEditor.view;

                context.batcher.flush();
                renderBackground(context, view, this.film.camera, 0, this.filmPanel.cameraEditor.getClip());
            });
            this.keyframeEditor.view.duration(() -> this.film.camera.calculateDuration());
            this.keyframeEditor.view.context((menu) ->
            {
                int mouseY = this.getContext().mouseY;
                UIKeyframeSheet sheet = this.keyframeEditor.view.getGraph().getSheet(mouseY);

                if (sheet != null && sheet.channel.getFactory() == KeyframeFactories.POSE)
                {
                    String trackName = StringUtils.fileName(sheet.id);

                    if (trackName.equals("pose") || trackName.startsWith("pose_overlay"))
                    {
                        Form form = sheet.property != null ? FormUtils.getForm(sheet.property) : this.replay.form.get();

                        if (form instanceof ModelForm modelForm)
                        {
                            menu.action(Icons.POSE, UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES, () -> this.animationToPoses(modelForm, sheet));
                        }
                        menu.action(Icons.CONVERT, UIKeys.FILM_REPLAY_CONTEXT_POSE_TO_LIMBS, () -> this.convertToLimbs(sheet));
                    }
                }

                int mouseY2 = this.getContext().mouseY;
                UIKeyframeSheet clickedSheet = this.keyframeEditor.view.getGraph().getSheet(mouseY2);
                if (clickedSheet != null && !clickedSheet.groupHeader)
                {
                    menu.action(Icons.FONT, UIKeys.FILM_REPLAY_RENAME_SHEET, () ->
                    {
                        UIRenameSheetOverlayPanel panel = new UIRenameSheetOverlayPanel(
                            UIKeys.FILM_REPLAY_RENAME_SHEET_TITLE,
                            UIKeys.FILM_REPLAY_RENAME_SHEET_MESSAGE,
                            this.replay,
                            clickedSheet.id,
                            (str, color) ->
                            {
                                this.replay.setCustomSheetTitle(clickedSheet.id, str);
                                this.replay.setSheetColor(clickedSheet.id, color);
                                this.updateChannelsList();
                            }
                        );

                        panel.text.setText(clickedSheet.title.get());
                        UIOverlay.addOverlay(this.getContext(), panel, 300, 0.25F);
                    });
                }

                if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet && (clickedSheet == null || !clickedSheet.groupHeader))
                {
                    menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, () ->
                    {
                        UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(BBSSettings.disabledSheets.get(), this.keys);

                        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);

                        panel.onClose((e) ->
                        {
                            this.updateChannelsList();
                            BBSSettings.disabledSheets.set(BBSSettings.disabledSheets.get());
                        });
                    });
                }
            });

            for (UIKeyframeSheet sheet : sheets)
            {
                this.keyframeEditor.view.addSheet(sheet);
            }

            this.add(this.keyframeEditor);
        }

        this.resize();

        if (this.keyframeEditor != null && lastEditor == null)
        {
            this.keyframeEditor.view.resetView();
        }
    }

    private boolean isCompatiblePropertyPath(Form rootForm, String key)
    {
        if (rootForm == null || key == null || key.isEmpty())
        {
            return false;
        }

        int colon = key.indexOf(':');
        String path = colon == -1 ? key : key.substring(0, colon);
        BaseValueBasic property = FormUtils.getProperty(rootForm, path);

        if (property == null)
        {
            return false;
        }

        if (colon == -1)
        {
            return true;
        }

        String boneName = key.substring(colon + 1);

        return this.isCompatibleBoneProperty(property, boneName);
    }

    private boolean isCompatibleBoneProperty(BaseValueBasic property, String boneName)
    {
        if (boneName == null || boneName.isEmpty() || !(property.getParent() instanceof Form parentForm))
        {
            return false;
        }

        if (!(parentForm instanceof ModelForm modelForm))
        {
            return false;
        }

        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model == null)
        {
            return false;
        }

        IModel modelDef = model.model;

        if (modelDef instanceof Model cubicModel)
        {
            return cubicModel.getAllGroupKeys().contains(boneName);
        }

        Collection<BOBJBone> bones = modelDef.getAllBOBJBones();

        if (bones != null)
        {
            for (BOBJBone bone : bones)
            {
                if (boneName.equals(bone.name))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void processTrack(UIKeyframeSheet sheet, String groupKey, int level, List<UIKeyframeSheet> before, List<UIKeyframeSheet> pose, List<UIKeyframeSheet> limbs, List<UIKeyframeSheet> overlayRoots, List<UIKeyframeSheet> overlayLimbs, List<UIKeyframeSheet> after)
    {
        sheet.level = level;
        String customTitle = this.replay.getCustomSheetTitle(sheet.id);

        /* Reset title in case it was changed by originalKeyframeUI mode */
        if ((customTitle == null || customTitle.isEmpty()) && sheet.property != null)
        {
            Form trackForm = FormUtils.getForm(sheet.property);

            if (trackForm != null)
            {
                sheet.title = IKey.constant(trackForm.getTrackName(sheet.channel.getId()));
            }
        }

        int colon = sheet.id.indexOf(':');
        String trackName = StringUtils.fileName(sheet.id);
        String scopeKey = groupKey == null || groupKey.isEmpty() ? this.replay.uuid.get() + ":__model__" : groupKey;
        String textureParentKey = scopeKey + ":texture";
        boolean isPbrTrack = trackName.equals("pbr_normal_intensity") || trackName.equals("pbr_specular_intensity");

        if (isPbrTrack)
        {
            if (this.collapsedModelTracks.getOrDefault(textureParentKey, true))
            {
                return;
            }

            sheet.level += 1;

            if ((customTitle == null || customTitle.isEmpty()) && trackName.equals("pbr_normal_intensity"))
            {
                sheet.title = UIKeys.FILM_REPLAY_TRACK_PBR_NORMAL_INTENSITY;
            }
            else if ((customTitle == null || customTitle.isEmpty()) && trackName.equals("pbr_specular_intensity"))
            {
                sheet.title = UIKeys.FILM_REPLAY_TRACK_PBR_SPECULAR_INTENSITY;
            }
        }

        if (colon != -1)
        {
            String parentId = sheet.id.substring(0, colon);
            String parentKey = this.replay.uuid.get() + ":" + parentId;

            if (this.collapsedModelTracks.getOrDefault(parentKey, true))
            {
                return;
            }

            sheet.level += 1;

            String parentTrackName = StringUtils.fileName(parentId);

            if (parentTrackName.startsWith("pose_overlay"))
            {
                overlayLimbs.add(sheet);
            }
            else
            {
                limbs.add(sheet);
            }
        }
        else if (trackName.equals("pose"))
        {
            String parentKey = this.replay.uuid.get() + ":" + sheet.id;
            boolean expanded = !this.collapsedModelTracks.getOrDefault(parentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                this.collapsedModelTracks.put(parentKey, !this.collapsedModelTracks.getOrDefault(parentKey, true));
                this.updateChannelsList();
            };

            pose.add(sheet);
        }
        else if (trackName.startsWith("pose_overlay"))
        {
            String parentKey = this.replay.uuid.get() + ":" + sheet.id;
            boolean expanded = !this.collapsedModelTracks.getOrDefault(parentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                this.collapsedModelTracks.put(parentKey, !this.collapsedModelTracks.getOrDefault(parentKey, true));
                this.updateChannelsList();
            };

            overlayRoots.add(sheet);
        }
        else if (trackName.equals("texture"))
        {
            boolean expanded = !this.collapsedModelTracks.getOrDefault(textureParentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                this.collapsedModelTracks.put(textureParentKey, !this.collapsedModelTracks.getOrDefault(textureParentKey, true));
                this.updateChannelsList();
            };

            this.addTrackByPriority(trackName, before, after, sheet);
        }
        else if (trackName.startsWith("transform_overlay") || trackName.equals("transform"))
        {
            before.add(sheet);
        }
        else
        {
            this.addTrackByPriority(trackName, before, after, sheet);
        }

        if (customTitle != null && !customTitle.isEmpty())
        {
            sheet.title = IKey.constant(customTitle);
        }
    }

    private void addTrackByPriority(String trackName, List<UIKeyframeSheet> before, List<UIKeyframeSheet> after, UIKeyframeSheet sheet)
    {
        int poseIndex = MODEL_PROPERTIES.indexOf("pose");
        int currentIndex = MODEL_PROPERTIES.indexOf(trackName);

        if (WORLD_CHANNELS.contains(trackName))
        {
            before.add(sheet);

            return;
        }

        if (currentIndex != -1 && currentIndex < poseIndex)
        {
            before.add(sheet);
        }
        else
        {
            after.add(sheet);
        }
    }

    private List<UIKeyframeSheet> orderOverlayTracks(Form form, List<UIKeyframeSheet> overlayRoots, List<UIKeyframeSheet> overlayLimbs)
    {
        List<UIKeyframeSheet> ordered = new ArrayList<>();

        if (overlayRoots.isEmpty() && overlayLimbs.isEmpty())
        {
            return ordered;
        }

        Set<UIKeyframeSheet> used = new HashSet<>();

        for (UIKeyframeSheet root : overlayRoots)
        {
            ordered.add(root);
            used.add(root);

            List<UIKeyframeSheet> limbs = new ArrayList<>();
            String prefix = root.id + ":";

            for (UIKeyframeSheet limb : overlayLimbs)
            {
                if (limb.id.startsWith(prefix))
                {
                    limbs.add(limb);
                    used.add(limb);
                }
            }

            this.orderLimbTracks(form, limbs);
            ordered.addAll(limbs);
        }

        for (UIKeyframeSheet limb : overlayLimbs)
        {
            if (!used.contains(limb))
            {
                ordered.add(limb);
            }
        }

        return ordered;
    }

    private static class FormTracks
    {
        public final Form form;
        public final List<UIKeyframeSheet> before = new ArrayList<>();
        public final List<UIKeyframeSheet> pose = new ArrayList<>();
        public final List<UIKeyframeSheet> limbs = new ArrayList<>();
        public final List<UIKeyframeSheet> overlayRoots = new ArrayList<>();
        public final List<UIKeyframeSheet> overlayLimbs = new ArrayList<>();
        public final List<UIKeyframeSheet> after = new ArrayList<>();

        public FormTracks(Form form)
        {
            this.form = form;
        }
    }

    private static int comparePathsNaturally(String a, String b)
    {
        if (a.equals(b))
        {
            return 0;
        }

        String[] left = a.split("/");
        String[] right = b.split("/");
        int min = Math.min(left.length, right.length);

        for (int i = 0; i < min; i++)
        {
            int cmp = compareNaturally(left[i], right[i]);

            if (cmp != 0)
            {
                return cmp;
            }
        }

        return Integer.compare(left.length, right.length);
    }

    private static int compareNaturally(String a, String b)
    {
        int i = 0;
        int j = 0;

        while (i < a.length() && j < b.length())
        {
            char ca = a.charAt(i);
            char cb = b.charAt(j);

            if (Character.isDigit(ca) && Character.isDigit(cb))
            {
                int startI = i;
                int startJ = j;

                while (i < a.length() && Character.isDigit(a.charAt(i)))
                {
                    i++;
                }

                while (j < b.length() && Character.isDigit(b.charAt(j)))
                {
                    j++;
                }

                String numberA = a.substring(startI, i);
                String numberB = b.substring(startJ, j);
                int numericCmp = compareNumericStrings(numberA, numberB);

                if (numericCmp != 0)
                {
                    return numericCmp;
                }

                continue;
            }

            int charCmp = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));

            if (charCmp != 0)
            {
                return charCmp;
            }

            i++;
            j++;
        }

        return Integer.compare(a.length(), b.length());
    }

    private static int compareNumericStrings(String a, String b)
    {
        int ai = 0;
        int bi = 0;

        while (ai < a.length() && a.charAt(ai) == '0')
        {
            ai++;
        }

        while (bi < b.length() && b.charAt(bi) == '0')
        {
            bi++;
        }

        String trimmedA = a.substring(ai);
        String trimmedB = b.substring(bi);

        if (trimmedA.length() != trimmedB.length())
        {
            return Integer.compare(trimmedA.length(), trimmedB.length());
        }

        int cmp = trimmedA.compareTo(trimmedB);

        if (cmp != 0)
        {
            return cmp;
        }

        return Integer.compare(a.length(), b.length());
    }

    private void animationToPoses(ModelForm modelForm, UIKeyframeSheet sheet)
    {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model != null)
        {
            UIAnimationToPoseOverlayPanel.IUIAnimationPoseCallback cb = (animationKey, onlyKeyframes, length, step) ->
                this.animationToPoseKeyframes(modelForm, sheet, animationKey, onlyKeyframes, length, step);

            UIOverlay.addOverlay(this.getContext(), new UIAnimationToPoseOverlayPanel(cb, modelForm, sheet), 260, 260);
        }
    }

    public void animationToPoseKeyframes(ModelForm modelForm, UIKeyframeSheet sheet, String animationKey, boolean onlyKeyframes, int length, int step)
    {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);
        Animation animation = model.animations.get(animationKey);

        if (animation != null)
        {
            int current = this.filmPanel.getCursor();
            IEntity entity = this.filmPanel.getController().getCurrentEntity();

            this.keyframeEditor.view.getDopeSheet().clearSelection();

            if (onlyKeyframes)
            {
                List<Float> list = this.getTicks(animation);

                for (float i : list)
                {
                    this.fillAnimationPose(sheet, i, model, entity, animation, current);
                }
            }
            else
            {
                for (int i = 0; i < length; i += step)
                {
                    this.fillAnimationPose(sheet, i, model, entity, animation, current);
                }
            }

            this.keyframeEditor.view.getDopeSheet().pickSelected();
        }
    }

    private List<Float> getTicks(Animation animation)
    {
        Set<Float> integers = new HashSet<>();

        for (AnimationPart value : animation.parts.values())
        {
            for (KeyframeChannel<MolangExpression> channel : value.channels)
            {
                for (Keyframe<MolangExpression> keyframe : channel.getKeyframes())
                {
                    integers.add(keyframe.getTick());
                }
            }
        }

        ArrayList<Float> ticks = new ArrayList<>(integers);

        Collections.sort(ticks);

        return ticks;
    }

    private void convertToLimbs(UIKeyframeSheet sheet)
    {
        List<Keyframe> selected = new ArrayList<>(sheet.selection.getSelected());

        if (selected.isEmpty())
        {
            return;
        }

        Form form = sheet.property != null ? FormUtils.getForm(sheet.property) : this.replay.form.get();

        if (form == null)
        {
            return;
        }

        BaseValue.edit(this.replay, IValueListener.FLAG_UNMERGEABLE, (r) ->
        {
            Set<Float> convertedTicks = new HashSet<>();

            for (Keyframe kf : selected)
            {
                Pose pose = (Pose) kf.getValue();

                if (pose == null)
                {
                    continue;
                }

                convertedTicks.add(kf.getTick());

                for (Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
                {
                    String boneName = entry.getKey();
                    PoseTransform transform = entry.getValue();
                    String key = sheet.id + ":" + boneName;

                    KeyframeChannel<Transform> channel = this.replay.properties.getOrCreate(this.replay.form.get(), key);

                    if (channel != null)
                    {
                        int index = channel.insert(kf.getTick(), transform.copy());
                        Keyframe<Transform> newKf = channel.get(index);

                        newKf.copyOverExtra(kf);
                    }
                }
            }

            if (!convertedTicks.isEmpty())
            {
                KeyframeChannel<Pose> poseChannel = null;
                BaseValue liveProperty = this.replay.properties.get(sheet.id);

                if (liveProperty instanceof KeyframeChannel<?> channel)
                {
                    poseChannel = (KeyframeChannel<Pose>) channel;
                }

                KeyframeChannel<Pose> targetChannel = poseChannel != null ? poseChannel : sheet.channel;

                for (int i = targetChannel.getList().size() - 1; i >= 0; i--)
                {
                    Keyframe existing = (Keyframe) targetChannel.getList().get(i);

                    for (Float tick : convertedTicks)
                    {
                        if (Math.abs(existing.getTick() - tick) < 0.0001F)
                        {
                            targetChannel.remove(i);
                            break;
                        }
                    }
                }
            }

            this.replay.properties.cleanUp();
        });

        this.replay.properties.resetProperties(this.replay.form.get());

        if (this.filmPanel.getUndoHandler() != null)
        {
            this.filmPanel.getUndoHandler().submitUndo();
        }

        this.updateChannelsList();
    }

    private void fillAnimationPose(UIKeyframeSheet sheet, float i, ModelInstance model, IEntity entity, Animation animation, int current)
    {
        model.model.resetPose();
        model.model.apply(entity, animation, i, 1F, 0F, false);

        int insert = sheet.channel.insert(current + i, model.model.createPose());

        sheet.selection.add(insert);
    }

    public void pickForm(Form form, String bone)
    {
        if (this.keyframeEditor == null || bone.isEmpty())
        {
            return;
        }

        String formPath = FormUtils.getPath(form);
        String propertyPath = null;
        IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();
        Keyframe selected = graph.getSelected();

        if (selected != null)
        {
            UIKeyframeSheet sheet = graph.getSheet(selected);

            if (sheet != null)
            {
                String sheetId = sheet.id;
                int colon = sheetId.indexOf(':');
                String pathWithProperty = colon != -1 ? sheetId.substring(0, colon) : sheetId;
                String propertyId = StringUtils.fileName(pathWithProperty);

                if (StringUtils.parentPath(pathWithProperty).equals(formPath) && (propertyId.equals("pose") || propertyId.startsWith("pose_overlay")))
                {
                    propertyPath = pathWithProperty;
                }
            }
        }

        if (propertyPath == null)
        {
            UIKeyframeSheet lastSheet = graph.getLastSheet();

            if (lastSheet != null)
            {
                String sheetId = lastSheet.id;
                int colon = sheetId.indexOf(':');
                String pathWithProperty = colon != -1 ? sheetId.substring(0, colon) : sheetId;
                String propertyId = StringUtils.fileName(pathWithProperty);

                if (StringUtils.parentPath(pathWithProperty).equals(formPath) && (propertyId.equals("pose") || propertyId.startsWith("pose_overlay")))
                {
                    propertyPath = pathWithProperty;
                }
            }
        }

        if (propertyPath == null)
        {
            String activeOverlayPath = null;
            String posePath = null;
            double minPoseDist = Double.MAX_VALUE;
            double minOverlayDist = Double.MAX_VALUE;
            int currentTick = this.filmPanel.getCursor();

            for (UIKeyframeSheet sheet : graph.getSheets())
            {
                String sheetId = sheet.id;
                int colon = sheetId.indexOf(':');
                String pathWithProperty = colon != -1 ? sheetId.substring(0, colon) : sheetId;

                if (StringUtils.parentPath(pathWithProperty).equals(formPath))
                {
                    String propertyId = StringUtils.fileName(pathWithProperty);

                    if (propertyId.equals("pose"))
                    {
                        posePath = pathWithProperty;

                        if (!sheet.channel.isEmpty())
                        {
                            KeyframeSegment segment = sheet.channel.find(currentTick);

                            if (segment != null)
                            {
                                minPoseDist = Math.abs(segment.getClosest().getTick() - currentTick);
                            }
                        }
                    }
                    else if (propertyId.startsWith("pose_overlay"))
                    {
                        if (!sheet.channel.isEmpty())
                        {
                            KeyframeSegment segment = sheet.channel.find(currentTick);

                            if (segment != null)
                            {
                                double dist = Math.abs(segment.getClosest().getTick() - currentTick);

                                if (activeOverlayPath == null)
                                {
                                    activeOverlayPath = pathWithProperty;
                                    minOverlayDist = dist;
                                }
                            }
                        }
                    }
                }
            }

            if (activeOverlayPath != null && minOverlayDist < minPoseDist)
            {
                propertyPath = activeOverlayPath;
            }
            else if (posePath != null)
            {
                propertyPath = posePath;
            }
        }

        if (propertyPath == null)
        {
            propertyPath = StringUtils.combinePaths(formPath, "pose");
        }

        this.pickProperty(bone, propertyPath, false);
    }

    public void pickFormProperty(Form form, String bone)
    {
        String path = FormUtils.getPath(form);
        boolean shift = Window.isShiftPressed();
        ContextMenuManager manager = new ContextMenuManager();

        manager.autoKeys();

        for (BaseValueBasic formProperty : form.getAllMap().values())
        {
            if (!formProperty.isVisible())
            {
                continue;
            }

            manager.action(getIcon(formProperty.getId()), IKey.constant(formProperty.getId()), () ->
            {
                this.pickProperty(bone, StringUtils.combinePaths(path, formProperty.getId()), shift);
            });
        }

        this.getContext().replaceContextMenu(manager.create());
    }

    private void pickProperty(String bone, String key, boolean insert)
    {
        IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();
        Keyframe selected = graph.getSelected();
        UIKeyframeSheet activeSheet = selected != null ? graph.getSheet(selected) : null;

        if (activeSheet != null)
        {
            String id = activeSheet.id;
            int colon = id.indexOf(':');
            String baseId = colon != -1 ? id.substring(0, colon) : id;
            String boneId = colon != -1 ? id.substring(colon + 1) : null;

            if (baseId.equals(key))
            {
                if (boneId == null || boneId.equals(bone))
                {
                    this.pickProperty(bone, activeSheet, insert);

                    return;
                }
            }
        }

        /* Redirección al sheet anclado si el hueso seleccionado está anclado y no hay override */
        /* Redirección a la pista de limb track si existe */
        if (bone != null && !bone.isEmpty())
        {
            String limbTrackId = key + ":" + bone;

            for (UIKeyframeSheet sheet : this.keyframeEditor.view.getGraph().getSheets())
            {
                if (sheet.id.equals(limbTrackId))
                {
                    this.pickProperty(bone, sheet, insert);

                    return;
                }
            }
        }

        for (UIKeyframeSheet sheet : this.keyframeEditor.view.getGraph().getSheets())
        {
            if (sheet.id.equals(key))
            {
                this.pickProperty(bone, sheet, insert);

                return;
            }
        }
    }

    private void pickProperty(String bone, UIKeyframeSheet sheet, boolean insert)
    {
        int tick = this.filmPanel.getRunner().ticks;

        if (insert)
        {
            Keyframe keyframe = this.keyframeEditor.view.getGraph().addKeyframe(sheet, tick, null);

            this.keyframeEditor.view.getGraph().selectKeyframe(keyframe);

            return;
        }

        Keyframe provisionalKeyframe = this.ensureAutomaticLimbKeyframe(sheet, bone, tick);

        KeyframeSegment segment = sheet.channel.find(tick);
        Keyframe closest = null;

        if (segment != null)
        {
            closest = segment.getClosest();
        }
        else if (!sheet.channel.isEmpty())
        {
            closest = sheet.channel.get(0);
        }

        if (closest == null)
        {
            closest = provisionalKeyframe;
        }

        if (closest != null)
        {
            if (this.keyframeEditor.view.getGraph().getSelected() != closest)
            {
                this.keyframeEditor.view.getGraph().selectKeyframe(closest);
            }

            if (this.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                poseFactory.poseEditor.selectBone(bone);
            }

            this.filmPanel.setCursor((int) closest.getTick());
        }
    }

    private boolean isPoseTrackId(String id)
    {
        String property = StringUtils.fileName(id);

        return property.equals("pose") || property.startsWith("pose_overlay");
    }

    private boolean isProvisionalAutomaticKeyframe(Keyframe keyframe, UIKeyframeSheet sheet)
    {
        if (keyframe == null || sheet == null || keyframe.getColor() == null)
        {
            return false;
        }

        int colon = sheet.id.indexOf(':');

        if (colon == -1)
        {
            return false;
        }

        String propertyId = sheet.id.substring(0, colon);

        return this.isPoseTrackId(propertyId) && keyframe.getColor().a < 0.99F;
    }

    private void cleanupUntouchedAutomaticKeyframe(Keyframe previous, Keyframe current)
    {
        if (previous == null || previous == current || this.keyframeEditor == null)
        {
            return;
        }

        IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();
        UIKeyframeSheet sheet = graph.getSheet(previous);

        if (!this.isProvisionalAutomaticKeyframe(previous, sheet))
        {
            return;
        }

        if (sheet.channel.removeSilently(previous))
        {
            graph.pickSelected();
        }
    }

    private Keyframe ensureAutomaticLimbKeyframe(UIKeyframeSheet sheet, String bone, int tick)
    {
        if (sheet == null || !BBSSettings.autoKeyframes.get())
        {
            return null;
        }

        if (bone == null || bone.isEmpty())
        {
            return null;
        }

        int colon = sheet.id.indexOf(':');

        if (colon == -1)
        {
            return null;
        }

        String propertyId = sheet.id.substring(0, colon);
        String boneId = sheet.id.substring(colon + 1);

        if (!this.isPoseTrackId(propertyId) || !bone.equals(boneId))
        {
            return null;
        }

        for (Object o : sheet.channel.getList())
        {
            Keyframe keyframe = (Keyframe) o;

            if (Math.abs(keyframe.getTick() - tick) < 0.0001F)
            {
                return keyframe;
            }
        }

        KeyframeSegment segment = sheet.channel.find(tick);
        Keyframe source = null;
        Transform transform = null;

        if (segment != null)
        {
            source = segment.a;
            Object interpolated = segment.createInterpolated();

            if (interpolated instanceof Transform value)
            {
                transform = (Transform) sheet.channel.getFactory().copy(value);
            }
        }

        if (transform == null)
        {
            transform = (Transform) sheet.channel.getFactory().createEmpty();
        }

        int index = sheet.channel.insert(tick, transform);
        Keyframe keyframe = (Keyframe) sheet.channel.get(index);

        if (keyframe != null)
        {
            if (source != null)
            {
                keyframe.getInterpolation().copy(source.getInterpolation());
            }

            keyframe.setColor(Color.rgba(Colors.setA(sheet.color, 0.35F)));
        }

        return keyframe;
    }

    public boolean clickViewport(UIContext context, Area area)
    {
        if (this.filmPanel.isFlying())
        {
            return false;
        }

        StencilFormFramebuffer stencil = this.filmPanel.getController().getStencil();

        if (stencil.hasPicked())
        {
            Pair<Form, String> pair = stencil.getPicked();

            if (pair != null && context.mouseButton < 2)
            {
                boolean allowPick = true;

                if (BBSSettings.replayMarkedBonesOnly.get() && !Window.isShiftPressed() && pair.a instanceof ModelForm modelForm)
                {
                    ModelInstance model = ModelFormRenderer.getModel(modelForm);
                    String poseGroup = model == null ? modelForm.model.get() : model.poseGroup;

                    if (poseGroup == null || poseGroup.isEmpty())
                    {
                        poseGroup = model == null ? modelForm.model.get() : model.id;
                    }

                    if (UIPoseEditor.hasMarkedBones(poseGroup) && !UIPoseEditor.isMarkedBone(poseGroup, pair.b))
                    {
                        allowPick = false;
                    }
                }

                if (allowPick)
                {
                    if (!this.isVisible())
                    {
                        this.filmPanel.showPanel(this);
                    }

                    if (Gizmo.INSTANCE.start(stencil.getIndex(), context.mouseX, context.mouseY, UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor)))
                    {
                        return true;
                    }

                    if (context.mouseButton == 0)
                    {
                        if (Window.isCtrlPressed()) offerAdjacent(this.getContext(), pair.a, pair.b, (bone) -> this.pickForm(pair.a, bone));
                        else if (Window.isShiftPressed()) offerHierarchy(this.getContext(), pair.a, pair.b, (bone) -> this.pickForm(pair.a, bone));
                        else this.pickForm(pair.a, pair.b);

                        return true;
                    }
                    else if (context.mouseButton == 1)
                    {
                        this.pickFormProperty(pair.a, pair.b);

                        return true;
                    }
                }
            }
        }
        else if (context.mouseButton == 1 && this.isVisible())
        {
            World world = MinecraftClient.getInstance().world;
            Camera camera = this.filmPanel.getCamera();

            BlockHitResult blockHitResult = RayTracing.rayTrace(
                world,
                RayTracing.fromVector3d(camera.position),
                RayTracing.fromVector3f(CameraUtils.getMouseDirection(camera.projection, camera.view, context.mouseX, context.mouseY, area.x, area.y, area.w, area.h)),
                256F
            );

            if (blockHitResult.getType() != HitResult.Type.MISS)
            {
                Vector3d vec = new Vector3d(blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z);

                if (Window.isShiftPressed())
                {
                    vec = new Vector3d(Math.floor(vec.x) + 0.5D, Math.round(vec.y), Math.floor(vec.z) + 0.5D);
                }

                final Vector3d finalVec = vec;

                context.replaceContextMenu((menu) ->
                {
                    float pitch = 0F;
                    float yaw = MathUtils.toDeg(camera.rotation.y);

                    menu.action(Icons.ADD, UIKeys.FILM_REPLAY_CONTEXT_ADD, () -> this.replays.replays.addReplay(finalVec, pitch, yaw));
                    menu.action(Icons.POINTER, UIKeys.FILM_REPLAY_CONTEXT_MOVE_HERE, () -> this.moveReplay(finalVec.x, finalVec.y, finalVec.z));
                });

                return true;
            }
        }

        if (area.isInside(context) && this.filmPanel.getController().orbit.enabled)
        {
            this.filmPanel.getController().orbit.start(context);

            return true;
        }

        return false;
    }

    public void close()
    {
        if (this.film != null)
        {
            lastFilm = this.film.getId();
            lastReplay = this.replays.replays.getIndex();
        }
    }

    public void teleport()
    {
        if (this.filmPanel.getData() == null)
        {
            return;
        }

        Replay replay = this.getReplay();

        if (replay != null)
        {
            int tick = this.filmPanel.getCursor();
            double x = replay.keyframes.x.interpolate(tick);
            double y = replay.keyframes.y.interpolate(tick);
            double z = replay.keyframes.z.interpolate(tick);
            float yaw = replay.keyframes.yaw.interpolate(tick).floatValue();
            float headYaw = replay.keyframes.headYaw.interpolate(tick).floatValue();
            float bodyYaw = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
            float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            PlayerUtils.teleport(x, y, z, headYaw, pitch);
            player.setYaw(yaw);
            player.setHeadYaw(headYaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        }
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        List<Integer> selection = DataStorageUtils.intListFromData(data.getList("selection"));
        List<Integer> currentIndices = this.replays.replays.getCurrentIndices();

        this.setReplay(CollectionUtils.getSafe(this.film.replays.getList(), data.getInt("replay")), true, false);

        currentIndices.clear();
        currentIndices.addAll(selection);
        this.replays.replays.update();
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        int index = this.film.replays.getList().indexOf(this.getReplay());

        data.putInt("replay", index);
        data.put("selection", DataStorageUtils.intListToData(this.replays.replays.getCurrentIndices()));
    }
}
