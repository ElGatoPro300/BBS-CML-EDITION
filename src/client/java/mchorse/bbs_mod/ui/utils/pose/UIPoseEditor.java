package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.io.File;
import java.io.IOException;

public class UIPoseEditor extends UIElement
{
    private static final Map<String, String> LAST_LIMB_CACHE = new HashMap<>();
    private static final Map<String, Set<String>> MARKED_BONES_CACHE = new HashMap<>();
    private static final String MARKED_BONES_FILE = "marked_bones.json";
    private static boolean MARKED_BONES_LOADED = false;

    public UISearchList<String> groups;
    public UIElement extra;
    public UIStringList groupsList;
    public UIStringList categories;
    public UITrackpad fix;
    public UIButton pickTexture;
    public UIColor color;
    public UIToggle lighting;
    public UIPropTransform transform;
    public Runnable onChange;

    private String group = "";
    private Pose pose;
    protected IModel model;
    protected Map<String, String> flippedParts;
    /** Proveedor opcional para obtener la textura base del modelo cuando no hay override por hueso. */
    protected Supplier<Link> defaultTextureSupplier;
    /** Gestor de categorías de huesos (por grupo de pose). */
    protected BoneCategoriesManager boneCategories = new BoneCategoriesManager();
    private final List<String> allBones = new ArrayList<>();
    private final Set<String> markedBones = new HashSet<>();
    private boolean showOnlyMarked;
    private UIIcon showOnlyMarkedButton;
    private String currentBone;

    public UIPoseEditor()
    {
        this.extra = new UIElement();
        this.extra.column().vertical().stretch();

        this.groupsList = new MarkableBoneList((l) -> this.pickBone(l.get(0)));
        this.groups = new UISearchList<>(this.groupsList);
        this.groups.label(UIKeys.GENERAL_SEARCH);
        this.groups.h(UIStringList.DEFAULT_HEIGHT * 8 + 12); // 20px search box + list height
        this.groups.list.background();
        this.groups.list.scroll.cancelScrolling();
        this.groups.search.w(1F, -20);
        this.showOnlyMarkedButton = new UIIcon(() -> this.showOnlyMarked ? Icons.VISIBLE : Icons.FILTER, (b) -> this.toggleShowOnlyMarked());
        this.showOnlyMarked = BBSSettings.poseBonesFilterMarked != null && BBSSettings.poseBonesFilterMarked.get();
        this.showOnlyMarkedButton.active(this.showOnlyMarked);
        this.showOnlyMarkedButton.tooltip(UIKeys.POSE_BONES_FILTER_MARKED_TOOLTIP);
        this.showOnlyMarkedButton.relative(this.groups).x(1F, -20).y(0).w(20).h(20);
        this.groups.add(this.showOnlyMarkedButton);
        this.groups.list.context(() ->
        {
            UIDataContextMenu menu = new UIDataContextMenu(PoseManager.INSTANCE, this.group, () -> this.pose != null ? this.pose.toData() : new MapType(), this::pastePose);
            UIIcon flip = new UIIcon(Icons.CONVERT, (b) -> this.flipPose());

            flip.tooltip(UIKeys.POSE_CONTEXT_FLIP_POSE);
            menu.row.addBefore(menu.save, flip);

            return menu;
        });
        /* Lista de categorías a la derecha */
        this.categories = new UIStringList((l) -> {});
        this.categories.background().h(UIStringList.DEFAULT_HEIGHT * 8 - 8);
        this.categories.scroll.cancelScrolling();
        this.categories.context((menu) ->
        {
            String selectedCategory = this.categories.getCurrentFirst();

            menu.action(Icons.ADD, L10n.lang("bbs.ui.forms.categories.context.add_category"), () ->
            {
                UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                    L10n.lang("bbs.ui.pose.categories.manage_title"),
                    L10n.lang("bbs.ui.pose.categories.manage_category_name"),
                    (str) ->
                    {
                        if (str != null && !str.isEmpty())
                        {
                            this.boneCategories.addCategory(this.group, str);
                            this.refreshCategories();
                        }
                    }
                );
                UIOverlay.addOverlay(this.getContext(), panel);
            });

            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                menu.action(Icons.EDIT, L10n.lang("bbs.ui.forms.categories.context.rename_category"), () ->
                {
                    UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                        L10n.lang("bbs.ui.pose.categories.manage_title"),
                        L10n.lang("bbs.ui.pose.categories.manage_new_name"),
                        (str) ->
                        {
                            if (str != null && !str.isEmpty())
                            {
                                this.boneCategories.renameCategory(this.group, selectedCategory, str);
                                this.refreshCategories();
                            }
                        }
                    );
                    UIOverlay.addOverlay(this.getContext(), panel);
                });

                menu.action(Icons.TRASH, L10n.lang("bbs.ui.forms.categories.context.remove_category"), Colors.RED, () ->
                {
                    this.boneCategories.removeCategory(this.group, selectedCategory);
                    this.refreshCategories();
                });

                /* Ver huesos que pertenecen a la categoría seleccionada */
                menu.action(Icons.LIST, L10n.lang("bbs.ui.pose.categories.context.view_bones"), () ->
                {
                    String group = this.group;
                    java.util.List<String> bones = this.boneCategories.getBones(group, selectedCategory);

                    UISearchList<String> search = new UISearchList<>(new UIStringList(null));
                    UIList<String> list = search.list;

                    for (String g : bones) { list.add(g); }

                    UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
                        L10n.lang("bbs.ui.pose.categories.view_bones_title"),
                        L10n.lang("bbs.ui.pose.categories.view_bones_description"),
                        (confirm) ->
                        {
                            if (confirm)
                            {
                                int index = list.getIndex();
                                String bone = CollectionUtils.getSafe(bones, index);
                                if (bone != null)
                                {
                                    this.selectBone(bone);
                                }
                            }
                        }
                    );

                    list.background();
                    /* Lista más alta y sin botones adicionales */
                    search.relative(panel.confirm).y(-5).w(1F).h(UIStringList.DEFAULT_HEIGHT * 12 + 20).anchor(0F, 1F);

                    /* Click derecho para eliminar el hueso de la categoría */
                    list.context((ctx) ->
                    {
                        ctx.action(Icons.TRASH, IKey.constant("Eliminar hueso"), Colors.RED, () ->
                        {
                            int idx = list.getIndex();
                            String bone = CollectionUtils.getSafe(bones, idx);
                            if (bone != null)
                            {
                                this.boneCategories.removeBone(group, selectedCategory, bone);
                                list.remove(bone);
                            }
                        });
                        ctx.autoKeys();
                    });

                    panel.content.add(search);
                    UIOverlay.addOverlay(this.getContext(), panel, 340, 360);
                });

                /* Separador visual no soportado por ContextMenuManager; omitido */

                String selectedBone = this.groups.list.getCurrentFirst();
                if (selectedBone != null && !selectedBone.isEmpty())
                {
                    menu.action(Icons.ADD, IKey.constant("Añadir hueso seleccionado"), () ->
                    {
                        this.boneCategories.addBone(this.group, selectedCategory, selectedBone);
                    });
                    menu.action(Icons.REMOVE, IKey.constant("Quitar hueso seleccionado"), () ->
                    {
                        this.boneCategories.removeBone(this.group, selectedCategory, selectedBone);
                    });
                }
            }

            menu.autoKeys();
        });
        this.fix = new UITrackpad((v) ->
        {
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setFix(p, v.floatValue()));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setFix(poseTransform, v.floatValue());
            }

            if (this.onChange != null) this.onChange.run();
        });
        this.fix.limit(0D, 1D).increment(1D).values(0.1, 0.05D, 0.2D);
        this.fix.tooltip(UIKeys.POSE_CONTEXT_FIX_TOOLTIP);
        this.fix.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setFix(p, (float) this.fix.getValue()));
                if (this.onChange != null) this.onChange.run();
            });

            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setFix(p, (float) this.fix.getValue()));
                if (this.onChange != null) this.onChange.run();
            });
        });
        /* Botón para elegir textura de hueso (etiqueta fija ES/EN) */
        this.pickTexture = new UIButton(UIKeys.TEXTURE_PICK_BONE_TEXTURE, (b) ->
        {
            PoseTransform poseTransform = (PoseTransform) this.transform.getTransform();
            Link current = null;

            if (poseTransform != null && poseTransform.texture != null)
            {
                current = poseTransform.texture;
            }
            else if (this.defaultTextureSupplier != null)
            {
                current = this.defaultTextureSupplier.get();
            }

            UITexturePicker.open(this.getContext(), current, (l) ->
            {
                String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
                if (selectedCategory != null && !selectedCategory.isEmpty())
                {
                    this.applyCategory((p) -> this.setTexture(p, l));
                }
                else if (this.transform.getTransform() instanceof PoseTransform pt)
                {
                    this.setTexture(pt, l);
                }

                if (this.onChange != null) this.onChange.run();
            });
        });
        this.pickTexture.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                Link chosen = t != null ? t.texture : null;
                this.applyChildren((p) -> this.setTexture(p, chosen));
                if (this.onChange != null) this.onChange.run();
            });
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                Link chosen = t != null ? t.texture : null;
                this.applyCategory((p) -> this.setTexture(p, chosen));
                if (this.onChange != null) this.onChange.run();
            });

            menu.action(Icons.CLOSE, UIKeys.GENERAL_NONE, () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                if (t != null)
                {
                    this.setTexture(t, null);
                    if (this.onChange != null) this.onChange.run();
                }
            });
        });
        this.color = new UIColor((c) ->
        {
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setColor(p, c));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setColor(poseTransform, c);
            }

            if (this.onChange != null) this.onChange.run();
        });
        this.color.withAlpha();
        this.color.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setColor(p, this.color.picker.color.getARGBColor()));
                if (this.onChange != null) this.onChange.run();
            });
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setColor(p, this.color.picker.color.getARGBColor()));
                if (this.onChange != null) this.onChange.run();
            });
        });
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
        {
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setLighting(p, b.getValue()));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setLighting(poseTransform, b.getValue());
            }

            if (this.onChange != null) this.onChange.run();
        });
        this.lighting.h(20);
        this.lighting.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setLighting(p, this.lighting.getValue()));
                if (this.onChange != null) this.onChange.run();
            });
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setLighting(p, this.lighting.getValue()));
                if (this.onChange != null) this.onChange.run();
            });
        });
        this.transform = this.createTransformEditor();
        this.transform.setModel();
        this.transform.callbacks(null, () ->
        {
            if (this.onChange != null)
            {
                this.onChange.run();
            }
        });

        this.column().vertical().stretch();
        boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
        if (categoriesEnabled)
        {
            this.add(UI.row(this.groups, this.categories), this.extra, UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, this.pickTexture, UI.row(this.color, this.lighting), this.transform);
        }
        else
        {
            this.add(this.groups, this.extra, UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, this.pickTexture, UI.row(this.color, this.lighting), this.transform);
        }
    }

    /**
     * Establece un proveedor de textura por defecto para usar cuando no exista
     * una textura específica del hueso. Devuelve this para permitir chaining.
     */
    public UIPoseEditor setDefaultTextureSupplier(Supplier<Link> supplier)
    {
        this.defaultTextureSupplier = supplier;

        return this;
    }

    private void applyChildren(Consumer<PoseTransform> consumer)
    {
        if (this.model == null || this.pose == null || !(this.transform.getTransform() instanceof PoseTransform))
        {
            return;
        }

        PoseTransform t = (PoseTransform) this.transform.getTransform();
        Collection<String> keys = this.model.getAllChildrenKeys(CollectionUtils.getKey(this.pose.transforms, t));

        for (String key : keys)
        {
            consumer.accept(this.pose.get(key));
        }
    }

    public Pose getPose()
    {
        return this.pose;
    }

    public String getGroup()
    {
        return this.groups.list.getCurrentFirst();
    }

    protected void pastePose(MapType data)
    {
        if (this.pose == null)
        {
            return;
        }

        String current = this.groups.list.getCurrentFirst();

        this.pose.fromData(data);
        this.pickBone(current);
        
        if (this.onChange != null)
        {
            this.onChange.run();
        }
    }

    protected void flipPose()
    {
        String current = this.groups.list.getCurrentFirst();

        this.pose.flip(this.flippedParts);
        this.pickBone(current);
        
        if (this.onChange != null)
        {
            this.onChange.run();
        }
    }

    public void setPose(Pose pose, String group)
    {
        this.pose = pose;
        this.group = group;
        this.loadMarkedBonesCache();
        this.refreshCategories();
    }

    /* Accesor público del grupo de pose (para fábricas y pistas) */
    public String getPoseGroupKey()
    {
        return this.group;
    }

    public void fillGroups(Collection<String> groups, boolean reset)
    {
        this.model = null;
        this.flippedParts = null;

        this.fillInGroups(groups, reset);
    }

    public void fillGroups(IModel model, Map<String, String> flippedParts, boolean reset)
    {
        this.model = model;
        this.flippedParts = flippedParts;

        this.fillInGroups(model == null ? Collections.emptyList() : model.getAllGroupKeys(), reset);
    }

    private void fillInGroups(Collection<String> groups, boolean reset)
    {
        double scroll = this.groups.list.scroll.getScroll();

        this.groups.list.clear();
        this.groups.list.add(groups);
        this.groups.list.sort();
        this.allBones.clear();
        this.allBones.addAll(this.groups.list.getList());
        if (!this.allBones.isEmpty())
        {
            this.markedBones.retainAll(this.allBones);
            this.saveMarkedBonesCache();
        }

        this.fix.setVisible(!groups.isEmpty());
        this.color.setVisible(!groups.isEmpty());
        this.transform.setVisible(!groups.isEmpty());

        boolean persistedFilter = BBSSettings.poseBonesFilterMarked != null && BBSSettings.poseBonesFilterMarked.get();
        if (persistedFilter != this.showOnlyMarked)
        {
            this.showOnlyMarked = persistedFilter;
            this.showOnlyMarkedButton.active(this.showOnlyMarked);
        }

        String preferred = this.getLastSelectedBone();
        this.applyMarkedFilter(reset, preferred, scroll);
    }

    public void selectBone(String bone)
    {
        this.cacheLastSelectedBone(bone);

        if (this.showOnlyMarked && bone != null && !bone.isEmpty() && !this.markedBones.contains(bone))
        {
            this.showOnlyMarked = false;
            this.showOnlyMarkedButton.active(false);
            if (BBSSettings.poseBonesFilterMarked != null)
            {
                BBSSettings.poseBonesFilterMarked.set(false);
            }

            double scroll = this.groups.list.scroll.getScroll();
            this.applyMarkedFilter(false, bone, scroll);
        }
        else
        {
            this.groups.list.setCurrentScroll(bone);
            this.pickBone(bone);
        }

        this.selectCategoryForBone(bone);
    }

    private void selectCategoryForBone(String bone)
    {
        if (this.categories != null && this.model != null)
        {
            List<String> cats = this.boneCategories.getCategories(this.group);
            for (String cat : cats)
            {
                List<String> bones = this.boneCategories.getBones(this.group, cat);
                if (bones.contains(bone))
                {
                    this.categories.setCurrentScroll(cat);
                    break;
                }
            }
        }
    }

    /* Subclass overridable methods */

    protected UIPropTransform createTransformEditor()
    {
        return new CategoryPropTransform(this).enableHotkeys();
    }

    /* Transformaciones aplicables por categoría */
    private static class CategoryPropTransform extends UIPropTransform
    {
        private final UIPoseEditor editor;

        private CategoryPropTransform(UIPoseEditor editor)
        {
            this.editor = editor;
        }

        private List<String> targets()
        {
            boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
            String selectedCategory = (categoriesEnabled && this.editor.categories != null) ? this.editor.categories.getCurrentFirst() : null;
            if (selectedCategory == null || selectedCategory.isEmpty())
            {
                String current = this.editor.groups.list.getCurrentFirst();
                return current == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(current);
            }

            return this.editor.boneCategories.getBones(this.editor.group, selectedCategory);
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            if (!(this.getTransform() instanceof PoseTransform) || this.editor.pose == null || CollectionUtils.getKey(this.editor.pose.transforms, (PoseTransform) this.getTransform()) == null)
            {
                super.setT(axis, x, y, z);
                return;
            }

            this.preCallback();
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.translate.x += dx;
                    t.translate.y += dy;
                    t.translate.z += dz;
                }
            }
            this.postCallback();
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            if (!(this.getTransform() instanceof PoseTransform) || this.editor.pose == null || CollectionUtils.getKey(this.editor.pose.transforms, (PoseTransform) this.getTransform()) == null)
            {
                super.setS(axis, x, y, z);
                return;
            }

            this.preCallback();
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.scale.x += dx;
                    t.scale.y += dy;
                    t.scale.z += dz;
                }
            }
            this.postCallback();
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            if (!(this.getTransform() instanceof PoseTransform) || this.editor.pose == null || CollectionUtils.getKey(this.editor.pose.transforms, (PoseTransform) this.getTransform()) == null)
            {
                super.setR(axis, x, y, z);
                return;
            }

            this.preCallback();
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.rotate.x += dx;
                    t.rotate.y += dy;
                    t.rotate.z += dz;
                }
            }
            this.postCallback();
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            if (!(this.getTransform() instanceof PoseTransform) || this.editor.pose == null || CollectionUtils.getKey(this.editor.pose.transforms, (PoseTransform) this.getTransform()) == null)
            {
                super.setR2(axis, x, y, z);
                return;
            }

            this.preCallback();
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.rotate2.x += dx;
                    t.rotate2.y += dy;
                    t.rotate2.z += dz;
                }
            }
            this.postCallback();
        }

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            if (!(this.getTransform() instanceof PoseTransform) || this.editor.pose == null || CollectionUtils.getKey(this.editor.pose.transforms, (PoseTransform) this.getTransform()) == null)
            {
                super.setP(axis, x, y, z);
                return;
            }

            this.preCallback();
            Transform transform = this.getTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.pivot.x += dx;
                    t.pivot.y += dy;
                    t.pivot.z += dz;
                }
            }
            this.postCallback();
        }
    }

    public void setGlobalTexture(UIElement element)
    {
        this.prepend(element);
        this.resize();
    }

    public void setTransform(Transform transform)
    {
        this.transform.setTransform(transform);

        boolean isPoseTransform = transform instanceof PoseTransform;

        this.fix.setVisible(true);
        this.color.setVisible(true);
        this.lighting.setVisible(true);
        this.pickTexture.setVisible(true);

        this.fix.setEnabled(isPoseTransform);
        this.color.setEnabled(isPoseTransform);
        this.lighting.setEnabled(isPoseTransform);
        this.pickTexture.setEnabled(isPoseTransform);

        if (!isPoseTransform || this.pose == null || CollectionUtils.getKey(this.pose.transforms, (PoseTransform) transform) == null)
        {
             this.groups.list.setIndex(-1);
        }
    }

    public Consumer<String> pickCallback;

    protected void pickBone(String bone)
    {
        this.currentBone = bone;

        if (this.pickCallback != null)
        {
            this.pickCallback.accept(bone);
        }

        this.cacheLastSelectedBone(bone);

        this.fix.setVisible(true);
        this.color.setVisible(true);
        this.lighting.setVisible(true);
        this.pickTexture.setVisible(true);

        this.fix.setEnabled(true);
        this.color.setEnabled(true);
        this.lighting.setEnabled(true);
        this.pickTexture.setEnabled(true);

        PoseTransform poseTransform = this.pose != null ? this.pose.get(bone) : null;

        if (poseTransform != null)
        {
            this.fix.setValue(poseTransform.fix);
            this.color.setColor(poseTransform.color.getARGBColor());
            this.lighting.setValue(poseTransform.lighting == 0F);
            this.transform.setTransform(poseTransform);
        }
        else
        {
            this.fix.setValue(0F);
            this.color.setColor(Colors.WHITE);
            this.lighting.setValue(false);
            this.transform.setTransform(null);
        }
    }

    protected void setFix(PoseTransform transform, float value)
    {
        transform.fix = value;
    }

    protected void setColor(PoseTransform transform, int value)
    {
        transform.color.set(value);
    }

    protected void setLighting(PoseTransform poseTransform, boolean value)
    {
        poseTransform.lighting = value ? 0F : 1F;
    }

    protected void setTexture(PoseTransform transform, Link value)
    {
        transform.texture = LinkUtils.copy(value);
    }

    /* Categorías */

    protected void refreshCategories()
    {
        if (this.categories == null)
        {
            return;
        }

        this.categories.clear();
        if (this.group != null)
        {
            this.categories.add(this.boneCategories.getCategories(this.group));
            this.categories.sort();
        }
    }

    protected void applyCategory(java.util.function.Consumer<PoseTransform> consumer)
    {
        boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
        String selectedCategory = categoriesEnabled ? this.categories.getCurrentFirst() : null;
        if (this.model == null || this.pose == null || selectedCategory == null || selectedCategory.isEmpty())
        {
            return;
        }

        List<String> bones = this.boneCategories.getBones(this.group, selectedCategory);
        for (String key : bones)
        {
            PoseTransform t = this.pose.get(key);
            if (t != null)
            {
                consumer.accept(t);
            }
        }
    }

    private void toggleShowOnlyMarked()
    {
        this.showOnlyMarked = !this.showOnlyMarked;
        this.showOnlyMarkedButton.active(this.showOnlyMarked);
        if (BBSSettings.poseBonesFilterMarked != null)
        {
            BBSSettings.poseBonesFilterMarked.set(this.showOnlyMarked);
        }

        String current = this.groups.list.getCurrentFirst();
        double scroll = this.groups.list.scroll.getScroll();
        this.applyMarkedFilter(false, current, scroll);
    }

    private void toggleBoneMarked(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return;
        }

        if (this.markedBones.contains(bone))
        {
            this.markedBones.remove(bone);
        }
        else
        {
            this.markedBones.add(bone);
        }
        this.saveMarkedBonesCache();

        if (this.showOnlyMarked)
        {
            String current = this.groups.list.getCurrentFirst();
            double scroll = this.groups.list.scroll.getScroll();
            this.applyMarkedFilter(false, current, scroll);
        }
    }

    private void applyMarkedFilter(boolean reset, String preferredBone, double scroll)
    {
        List<String> source = this.showOnlyMarked ? this.getMarkedBonesInOrder() : this.allBones;
        this.groups.list.clear();
        this.groups.list.add(source);
        this.groups.list.sort();

        this.applySearchFilter();

        List<String> list = this.groups.list.getList();
        String element = preferredBone != null && list.contains(preferredBone) ? preferredBone : CollectionUtils.getSafe(list, 0);

        if (element != null)
        {
            if (reset)
            {
                this.groups.list.setCurrentScroll(element);
            }
            else
            {
                this.groups.list.setCurrent(element);
                this.groups.list.scroll.setScroll(scroll);
            }

            this.pickBone(element);
        }
        else
        {
            this.groups.list.setIndex(-1);
        }

        this.refreshCategories();
    }

    private void applySearchFilter()
    {
        String filter = this.groups.search.getText();

        this.groups.list.filter("");
        if (!filter.isEmpty())
        {
            this.groups.list.filter(filter);
        }
    }

    private List<String> getMarkedBonesInOrder()
    {
        List<String> marked = new ArrayList<>();

        for (String bone : this.allBones)
        {
            if (this.markedBones.contains(bone))
            {
                marked.add(bone);
            }
        }

        return marked;
    }

    private void loadMarkedBonesCache()
    {
        this.ensureMarkedBonesLoaded();
        this.markedBones.clear();

        Set<String> cached = MARKED_BONES_CACHE.get(this.getMarkedBonesCacheKey());
        if (cached != null)
        {
            this.markedBones.addAll(cached);
        }
    }

    private void saveMarkedBonesCache()
    {
        this.ensureMarkedBonesLoaded();

        String key = this.getMarkedBonesCacheKey();
        if (key.isEmpty())
        {
            return;
        }

        if (this.markedBones.isEmpty())
        {
            MARKED_BONES_CACHE.remove(key);
        }
        else
        {
            MARKED_BONES_CACHE.put(key, new HashSet<>(this.markedBones));
        }

        this.saveMarkedBonesToFile();
    }

    public static boolean hasMarkedBones(String groupKey)
    {
        if (groupKey == null || groupKey.isEmpty())
        {
            return false;
        }

        ensureMarkedBonesLoadedStatic();

        Set<String> cached = MARKED_BONES_CACHE.get(groupKey);
        return cached != null && !cached.isEmpty();
    }

    public static Set<String> getMarkedBones(String groupKey)
    {
        if (groupKey == null || groupKey.isEmpty())
        {
            return Collections.emptySet();
        }

        ensureMarkedBonesLoadedStatic();

        Set<String> cached = MARKED_BONES_CACHE.get(groupKey);
        return cached == null ? Collections.emptySet() : new HashSet<>(cached);
    }

    public String getCurrentBone()
    {
        return this.currentBone;
    }

    public static boolean isMarkedBone(String groupKey, String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return false;
        }

        ensureMarkedBonesLoadedStatic();

        Set<String> cached = groupKey == null ? null : MARKED_BONES_CACHE.get(groupKey);
        return cached != null && cached.contains(bone);
    }

    private String getMarkedBonesCacheKey()
    {
        return this.group == null ? "" : this.group;
    }

    private String getLastSelectedBone()
    {
        String key = this.getMarkedBonesCacheKey();
        String preferred = LAST_LIMB_CACHE.get(key);
        if (preferred == null || preferred.isEmpty())
        {
            preferred = LAST_LIMB_CACHE.get("");
        }

        if (preferred != null && !preferred.isEmpty())
        {
            return preferred;
        }

        return null;
    }

    private void cacheLastSelectedBone(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return;
        }

        String key = this.getMarkedBonesCacheKey();
        if (!key.isEmpty())
        {
            LAST_LIMB_CACHE.put(key, bone);
        }
        LAST_LIMB_CACHE.put("", bone);
    }

    private void ensureMarkedBonesLoaded()
    {
        ensureMarkedBonesLoadedStatic();
    }

    private static void ensureMarkedBonesLoadedStatic()
    {
        if (MARKED_BONES_LOADED)
        {
            return;
        }

        MARKED_BONES_LOADED = true;

        try
        {
            BaseType type = DataToString.read(getMarkedBonesFileStatic());

            if (type != null && type.isMap())
            {
                MapType map = (MapType) type;

                for (String key : map.keys())
                {
                    ListType list = map.getList(key);
                    if (list == null)
                    {
                        continue;
                    }

                    Set<String> bones = new HashSet<>();
                    for (int i = 0; i < list.size(); i++)
                    {
                        bones.add(list.getString(i));
                    }

                    if (!bones.isEmpty())
                    {
                        MARKED_BONES_CACHE.put(key, bones);
                    }
                }
            }
        }
        catch (IOException e)
        {
        }
    }

    private void saveMarkedBonesToFile()
    {
        MapType root = new MapType();

        for (Map.Entry<String, Set<String>> entry : MARKED_BONES_CACHE.entrySet())
        {
            ListType list = new ListType();
            for (String bone : entry.getValue())
            {
                list.addString(bone);
            }
            root.put(entry.getKey(), list);
        }

        DataToString.writeSilently(this.getMarkedBonesFile(), root, true);
    }

    private File getMarkedBonesFile()
    {
        return getMarkedBonesFileStatic();
    }

    private static File getMarkedBonesFileStatic()
    {
        return BBSMod.getSettingsPath(MARKED_BONES_FILE);
    }

    private class MarkableBoneList extends UIStringList
    {
        public MarkableBoneList(Consumer<List<String>> callback)
        {
            super(callback);
        }

        @Override
        public void render(UIContext context)
        {
            super.render(context);

            if (!UIPoseEditor.this.showOnlyMarked || !UIPoseEditor.this.markedBones.isEmpty())
            {
                return;
            }

            String line1 = L10n.lang("bbs.ui.pose.bones.empty_line1").get();
            String line2 = L10n.lang("bbs.ui.pose.bones.empty_line2").get();
            int lineHeight = context.batcher.getFont().getHeight() + 4;
            int totalHeight = lineHeight * 2 - 4;
            int y = this.area.my() - totalHeight / 2;
            int color = Colors.setA(Colors.WHITE, 0.6F);

            context.batcher.clip(this.area, context);
            int x1 = this.area.mx() - context.batcher.getFont().getWidth(line1) / 2;
            context.batcher.textShadow(line1, x1, y, color);
            y += lineHeight;

            int iconSize = 16;
            int iconSpacing = 4;
            int line2TextWidth = context.batcher.getFont().getWidth(line2);
            int totalLine2Width = line2TextWidth + iconSpacing + iconSize;
            int x2 = this.area.mx() - totalLine2Width / 2;
            context.batcher.textShadow(line2, x2, y, color);
            int iconX = x2 + line2TextWidth + iconSpacing;
            int iconY = y + (context.batcher.getFont().getHeight() - iconSize) / 2;
            RenderSystem.enableBlend();
            context.batcher.icon(Icons.VISIBLE, color, iconX, iconY);
            context.batcher.unclip(context);
        }

        @Override
        protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
        {
            int iconX = x + 2;
            int iconY = y + (this.scroll.scrollItemSize - 16) / 2;
            boolean marked = UIPoseEditor.this.markedBones.contains(element);
            int iconColor = marked ? Colors.WHITE : Colors.setA(Colors.WHITE, 0.35F);

            RenderSystem.enableBlend();
            context.batcher.icon(Icons.CHECKMARK, iconColor, iconX, iconY);

            int textX = x + 22;
            int maxWidth = this.area.w - 24;
            String displayText = element;
            int textWidth = context.batcher.getFont().getWidth(displayText);

            if (textWidth > maxWidth)
            {
                displayText = context.batcher.getFont().limitToWidth(displayText, maxWidth);
            }

            context.batcher.textShadow(displayText, textX, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (!this.area.isInside(context) || context.mouseButton != 0)
            {
                return super.subMouseClicked(context);
            }

            int scrollIndex = this.scroll.getIndex(context.mouseX, context.mouseY);
            String element = this.getElementAt(scrollIndex);

            if (element == null)
            {
                return super.subMouseClicked(context);
            }

            int y = this.area.y + scrollIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
            int iconY = y + (this.scroll.scrollItemSize - 16) / 2;
            int iconX = this.area.x + 2;

            if (context.mouseX >= iconX && context.mouseX < iconX + 16 && context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (Window.isShiftPressed())
                {
                    UIPoseEditor.this.toggleBoneMarked(element);
                    return true;
                }
            }

            return super.subMouseClicked(context);
        }
    }
}
