package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.CubicLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIModelGeometryPanel extends UIElement
{
    private final UIModelPanel parent;
    private final UIList<GeometryEntry> hierarchyList;
    private final UISearchList<GeometryEntry> hierarchySearch;
    private final UILabel selectedBoneLabel;
    private final UITrackpad originX;
    private final UITrackpad originY;
    private final UITrackpad originZ;
    private final UITrackpad rotateX;
    private final UITrackpad rotateY;
    private final UITrackpad rotateZ;
    private final UITrackpad pivotX;
    private final UITrackpad pivotY;
    private final UITrackpad pivotZ;
    private final UITrackpad scaleX;
    private final UITrackpad scaleY;
    private final UITrackpad scaleZ;
    private final UIButton saveButton;
    private final UIButton reloadButton;

    private final UILabel selectedCubeLabel;
    private final UITrackpad cubeOriginX;
    private final UITrackpad cubeOriginY;
    private final UITrackpad cubeOriginZ;
    private final UITrackpad cubeSizeX;
    private final UITrackpad cubeSizeY;
    private final UITrackpad cubeSizeZ;
    private final UITrackpad cubePivotX;
    private final UITrackpad cubePivotY;
    private final UITrackpad cubePivotZ;
    private final UITrackpad cubeInflate;
    private final UITrackpad cubeUvX;
    private final UITrackpad cubeUvY;
    private final UIToggle cubeMirror;
    private final UIButton addCubeButton;
    private final UIButton removeCubeButton;
    private final UIToggle autoSaveToggle;
    private final Set<String> collapsedGroupIds = new HashSet<>();

    private ModelConfig config;
    private ModelInstance instance;
    private ModelGroup selectedGroup;
    private ModelCube selectedCube;
    private boolean cubeMirrorValue;
    private boolean filling;

    public UIModelGeometryPanel(UIModelPanel parent)
    {
        this.parent = parent;
        this.relative(parent.mainView).w(1F).h(1F);

        int sideMargin = 10;
        int leftWidth = 260;
        int rightWidth = 280;

        UILabel hierarchyTitle = UI.label(UIKeys.MODELS_IK_HIERARCHY).background();
        hierarchyTitle.relative(this).x(sideMargin).y(10).w(leftWidth).h(12);

        this.hierarchyList = new UIList<>((l) -> this.selectCurrentHierarchyEntry())
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }

            @Override
            protected void renderElementPart(UIContext context, GeometryEntry element, int i, int x, int y, boolean hover, boolean selected)
            {
                int textY = y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2;
                int offset = element.depth * 10;
                Icon icon = element.type == GeometryEntryType.BONE ? Icons.FOLDER : Icons.BLOCK;

                context.batcher.icon(icon, x + 4 + offset, y + 1);
                context.batcher.textShadow(element.label, x + 20 + offset, textY, hover ? Colors.HIGHLIGHT : Colors.WHITE);
            }

            @Override
            protected String elementToString(UIContext context, int i, GeometryEntry element)
            {
                return element.label + " " + element.groupId;
            }

            @Override
            protected void handleSwap(int from, int to)
            {
                UIModelGeometryPanel.this.handleHierarchySwap(from, to);
            }

            @Override
            public boolean subMouseClicked(UIContext context)
            {
                if (!this.isFiltering() && this.area.isInside(context) && context.mouseButton == 0)
                {
                    int visibleIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

                    if (this.exists(visibleIndex))
                    {
                        GeometryEntry entry = this.getList().get(visibleIndex);
                        int y = this.area.y + visibleIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
                        int offset = entry.depth * 10;
                        int iconX = this.area.x + 4 + offset;

                        if (entry.type == GeometryEntryType.BONE && context.mouseX >= iconX && context.mouseX < iconX + 16 && context.mouseY >= y + 1 && context.mouseY < y + 17)
                        {
                            UIModelGeometryPanel.this.toggleGroupCollapsed(entry.groupId);

                            return true;
                        }
                    }
                }

                return super.subMouseClicked(context);
            }
        };
        this.hierarchyList.background();
        this.hierarchyList.sorting();
        this.hierarchyList.scroll.scrollItemSize = 18;
        this.hierarchySearch = new UISearchList<>(this.hierarchyList);
        this.hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        this.hierarchySearch.relative(this).x(sideMargin).y(26).w(leftWidth).h(1F, -94);

        UILabel editorTitle = UI.label(UIKeys.MODELS_GEOMETRY_EDITOR).background();
        editorTitle.relative(this).x(1F, -rightWidth - sideMargin).y(10).w(rightWidth).h(12);

        this.selectedBoneLabel = UI.label(IKey.raw("-"));
        this.selectedBoneLabel.relative(editorTitle).y(1F, 4).w(1F).h(12);

        UILabel originLabel = UI.label(IKey.raw("Origin"));
        originLabel.relative(this.selectedBoneLabel).y(1F, 8).w(1F).h(12);
        this.originX = this.trackpad((v) -> this.updateVector(0, 0, v.floatValue()));
        this.originY = this.trackpad((v) -> this.updateVector(0, 1, v.floatValue()));
        this.originZ = this.trackpad((v) -> this.updateVector(0, 2, v.floatValue()));
        UIElement originRow = UI.row(6, this.originX, this.originY, this.originZ);
        originRow.relative(originLabel).y(1F, 2).w(1F).h(20);

        UILabel rotateLabel = UI.label(IKey.raw("Rotate"));
        rotateLabel.relative(originRow).y(1F, 8).w(1F).h(12);
        this.rotateX = this.trackpad((v) -> this.updateVector(1, 0, v.floatValue()));
        this.rotateY = this.trackpad((v) -> this.updateVector(1, 1, v.floatValue()));
        this.rotateZ = this.trackpad((v) -> this.updateVector(1, 2, v.floatValue()));
        UIElement rotateRow = UI.row(6, this.rotateX, this.rotateY, this.rotateZ);
        rotateRow.relative(rotateLabel).y(1F, 2).w(1F).h(20);

        UILabel pivotLabel = UI.label(IKey.raw("Pivot"));
        pivotLabel.relative(rotateRow).y(1F, 8).w(1F).h(12);
        this.pivotX = this.trackpad((v) -> this.updateVector(2, 0, v.floatValue()));
        this.pivotY = this.trackpad((v) -> this.updateVector(2, 1, v.floatValue()));
        this.pivotZ = this.trackpad((v) -> this.updateVector(2, 2, v.floatValue()));
        UIElement pivotRow = UI.row(6, this.pivotX, this.pivotY, this.pivotZ);
        pivotRow.relative(pivotLabel).y(1F, 2).w(1F).h(20);

        UILabel scaleLabel = UI.label(IKey.raw("Scale"));
        scaleLabel.relative(pivotRow).y(1F, 8).w(1F).h(12);
        this.scaleX = this.trackpad((v) -> this.updateVector(3, 0, v.floatValue()));
        this.scaleY = this.trackpad((v) -> this.updateVector(3, 1, v.floatValue()));
        this.scaleZ = this.trackpad((v) -> this.updateVector(3, 2, v.floatValue()));
        UIElement scaleRow = UI.row(6, this.scaleX, this.scaleY, this.scaleZ);
        scaleRow.relative(scaleLabel).y(1F, 2).w(1F).h(20);

        this.saveButton = new UIButton(UIKeys.GENERAL_SAVE, (b) -> this.saveModelFile());
        this.reloadButton = new UIButton(UIKeys.GENERAL_EDIT, (b) -> this.reloadModelData());
        this.saveButton.w(0.5F, -4).h(20);
        this.reloadButton.w(0.5F, -4).h(20);
        UIElement buttons = UI.row(8, this.saveButton, this.reloadButton);
        buttons.relative(scaleRow).y(1F, 10).w(1F).h(20);

        this.selectedCubeLabel = UI.label(IKey.raw("-"));
        this.selectedCubeLabel.relative(buttons).y(1F, 8).w(1F).h(12);

        UILabel cubeOriginLabel = UI.label(IKey.raw("Cube Origin"));
        cubeOriginLabel.relative(this.selectedCubeLabel).y(1F, 8).w(1F).h(12);
        this.cubeOriginX = this.trackpad((v) -> this.updateCubeVector(0, 0, v.floatValue()));
        this.cubeOriginY = this.trackpad((v) -> this.updateCubeVector(0, 1, v.floatValue()));
        this.cubeOriginZ = this.trackpad((v) -> this.updateCubeVector(0, 2, v.floatValue()));
        UIElement cubeOriginRow = UI.row(6, this.cubeOriginX, this.cubeOriginY, this.cubeOriginZ);
        cubeOriginRow.relative(cubeOriginLabel).y(1F, 2).w(1F).h(20);

        UILabel cubeSizeLabel = UI.label(IKey.raw("Cube Size"));
        cubeSizeLabel.relative(cubeOriginRow).y(1F, 8).w(1F).h(12);
        this.cubeSizeX = this.trackpad((v) -> this.updateCubeVector(1, 0, v.floatValue()));
        this.cubeSizeY = this.trackpad((v) -> this.updateCubeVector(1, 1, v.floatValue()));
        this.cubeSizeZ = this.trackpad((v) -> this.updateCubeVector(1, 2, v.floatValue()));
        UIElement cubeSizeRow = UI.row(6, this.cubeSizeX, this.cubeSizeY, this.cubeSizeZ);
        cubeSizeRow.relative(cubeSizeLabel).y(1F, 2).w(1F).h(20);

        UILabel cubePivotLabel = UI.label(IKey.raw("Cube Pivot"));
        cubePivotLabel.relative(cubeSizeRow).y(1F, 8).w(1F).h(12);
        this.cubePivotX = this.trackpad((v) -> this.updateCubeVector(2, 0, v.floatValue()));
        this.cubePivotY = this.trackpad((v) -> this.updateCubeVector(2, 1, v.floatValue()));
        this.cubePivotZ = this.trackpad((v) -> this.updateCubeVector(2, 2, v.floatValue()));
        UIElement cubePivotRow = UI.row(6, this.cubePivotX, this.cubePivotY, this.cubePivotZ);
        cubePivotRow.relative(cubePivotLabel).y(1F, 2).w(1F).h(20);

        UILabel cubeInflateLabel = UI.label(IKey.raw("Cube Inflate"));
        cubeInflateLabel.relative(cubePivotRow).y(1F, 8).w(60).h(12);
        this.cubeInflate = this.trackpad((v) -> this.updateCubeInflate(v.floatValue()));
        this.cubeInflate.w(0.333F, -6);
        UIElement cubeInflateRow = UI.row(this.cubeInflate);
        cubeInflateRow.relative(cubeInflateLabel).y(1F, 2).w(1F).h(20);

        UILabel cubeUvLabel = UI.label(IKey.raw("Cube UV"));
        cubeUvLabel.relative(cubeInflateRow).y(1F, 8).w(1F).h(12);
        this.cubeUvX = this.trackpad((v) -> this.updateCubeUV(0, v.floatValue()));
        this.cubeUvY = this.trackpad((v) -> this.updateCubeUV(1, v.floatValue()));
        this.cubeMirror = new UIToggle(IKey.raw("Mirror"), (b) -> this.updateCubeMirror(b.getValue()));
        this.cubeUvX.w(0.333F, -6);
        this.cubeUvY.w(0.333F, -6);
        this.cubeMirror.w(0.333F, -6).h(20);
        UIElement cubeUvRow = UI.row(6, this.cubeUvX, this.cubeUvY, this.cubeMirror);
        cubeUvRow.relative(cubeUvLabel).y(1F, 2).w(1F).h(20);

        this.autoSaveToggle = new UIToggle(IKey.raw("Auto Save"), false, (b) -> {});
        this.autoSaveToggle.relative(cubeUvRow).y(1F, 8).w(1F).h(14);

        UIElement editor = new UIElement();
        editor.relative(this).x(1F, -rightWidth - sideMargin).y(26).w(rightWidth).h(1F, -36);
        editor.add(editorTitle, this.selectedBoneLabel, originLabel, originRow, rotateLabel, rotateRow, pivotLabel, pivotRow, scaleLabel, scaleRow, buttons, this.selectedCubeLabel, cubeOriginLabel, cubeOriginRow, cubeSizeLabel, cubeSizeRow, cubePivotLabel, cubePivotRow, cubeInflateLabel, cubeInflateRow, cubeUvLabel, cubeUvRow, this.autoSaveToggle);

        this.addCubeButton = new UIButton(UIKeys.GENERAL_ADD, (b) -> this.addCube());
        this.removeCubeButton = new UIButton(UIKeys.GENERAL_REMOVE, (b) -> this.removeCube());
        this.addCubeButton.w(0.5F, -4).h(20);
        this.removeCubeButton.w(0.5F, -4).h(20);
        UIElement cubeButtons = UI.row(8, this.addCubeButton, this.removeCubeButton);
        cubeButtons.relative(this.hierarchySearch).y(1F, 6).w(leftWidth).h(20);

        this.add(hierarchyTitle, this.hierarchySearch, cubeButtons, editor);

        this.fillControls();
        this.fillCubeControls();
    }

    private void fillCubeControls()
    {
        this.filling = true;

        if (this.selectedCube == null)
        {
            this.selectedCubeLabel.label = IKey.raw("-");
            this.cubeMirrorValue = false;
            this.setCubePads(new Vector3f(), new Vector3f(), new Vector3f(), 0, new Vector2f(), false);
        }
        else
        {
            int index = this.selectedGroup.cubes.indexOf(this.selectedCube);
            Vector2f uv = this.getBoxUV(this.selectedCube);

            this.selectedCubeLabel.label = IKey.raw(index < 0 ? "-" : this.getCubeLabel(this.selectedCube));
            this.cubeMirrorValue = this.isCubeMirrored(this.selectedCube);
            this.setCubePads(this.selectedCube.origin, this.selectedCube.size, this.selectedCube.pivot, this.selectedCube.inflate, uv, this.cubeMirrorValue);
        }

        this.filling = false;
    }

    private UITrackpad trackpad(java.util.function.Consumer<Double> callback)
    {
        UITrackpad pad = new UITrackpad((v) -> callback.accept(v.doubleValue())).increment(1);

        pad.w(0.333F, -6);

        return pad;
    }

    public void setConfig(ModelConfig config)
    {
        this.config = config;
        this.reloadModelData();
    }

    public void selectBone(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return;
        }

        for (GeometryEntry entry : this.hierarchyList.getList())
        {
            if (entry.type == GeometryEntryType.BONE && entry.groupId.equals(bone))
            {
                this.hierarchyList.setCurrentDirect(entry);
                this.selectCurrentHierarchyEntry();

                break;
            }
        }
    }

    private void reloadModelData()
    {
        this.instance = null;
        this.selectedGroup = null;
        this.selectedCube = null;
        this.hierarchyList.clear();
        this.parent.renderer.setSelectedCube(null);

        if (this.config == null)
        {
            this.fillControls();
            this.fillCubeControls();
            return;
        }

        this.instance = this.parent.renderer.getPreviewModelInstance();

        if (this.instance == null)
        {
            this.instance = BBSModClient.getModels().loadModel(this.config.getId());
            this.parent.renderer.invalidatePreviewModel();
            this.instance = this.parent.renderer.getPreviewModelInstance();
        }

        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            this.fillControls();
            this.fillCubeControls();
            return;
        }

        for (ModelGroup group : model.topGroups)
        {
            this.collectHierarchy(group, 0);
        }

        if (!this.hierarchyList.getList().isEmpty())
        {
            this.hierarchyList.setCurrent(this.hierarchyList.getList().get(0));
            this.selectCurrentHierarchyEntry();
        }
        else
        {
            this.fillControls();
            this.fillCubeControls();
        }
    }

    private void collectHierarchy(ModelGroup group, int depth)
    {
        this.hierarchyList.add(new GeometryEntry(GeometryEntryType.BONE, group.id, -1, depth, group.id));

        if (this.collapsedGroupIds.contains(group.id))
        {
            return;
        }

        for (int i = 0; i < group.cubes.size(); i++)
        {
            this.hierarchyList.add(new GeometryEntry(GeometryEntryType.CUBE, group.id, i, depth + 1, this.getCubeLabel(group.cubes.get(i))));
        }

        for (ModelGroup child : group.children)
        {
            this.collectHierarchy(child, depth + 1);
        }
    }

    private void selectCurrentHierarchyEntry()
    {
        this.selectedGroup = null;
        this.selectedCube = null;

        if (this.instance != null && this.instance.model instanceof Model model)
        {
            GeometryEntry entry = this.hierarchyList.getCurrentFirst();

            if (entry != null)
            {
                this.selectedGroup = model.getGroup(entry.groupId);

                if (this.selectedGroup != null)
                {
                    this.parent.renderer.setSelectedBone(this.selectedGroup.id);

                    if (entry.type == GeometryEntryType.CUBE && entry.cubeIndex >= 0 && entry.cubeIndex < this.selectedGroup.cubes.size())
                    {
                        this.selectedCube = this.selectedGroup.cubes.get(entry.cubeIndex);
                    }
                }
            }
        }

        this.parent.renderer.setSelectedCube(this.selectedCube);
        this.fillControls();
        this.fillCubeControls();
    }

    private void fillControls()
    {
        this.filling = true;

        if (this.selectedGroup == null)
        {
            this.selectedBoneLabel.label = IKey.raw("-");
            this.setPads(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(1F, 1F, 1F));
        }
        else
        {
            this.selectedBoneLabel.label = IKey.raw(this.selectedGroup.id);
            this.setPads(this.selectedGroup.initial.translate, this.selectedGroup.initial.rotate, this.selectedGroup.initial.pivot, this.selectedGroup.initial.scale);
        }

        this.filling = false;
    }

    private void setPads(Vector3f origin, Vector3f rotate, Vector3f pivot, Vector3f scale)
    {
        this.originX.setValue(origin.x);
        this.originY.setValue(origin.y);
        this.originZ.setValue(origin.z);
        this.rotateX.setValue(rotate.x);
        this.rotateY.setValue(rotate.y);
        this.rotateZ.setValue(rotate.z);
        this.pivotX.setValue(pivot.x);
        this.pivotY.setValue(pivot.y);
        this.pivotZ.setValue(pivot.z);
        this.scaleX.setValue(scale.x);
        this.scaleY.setValue(scale.y);
        this.scaleZ.setValue(scale.z);
    }

    private void setCubePads(Vector3f origin, Vector3f size, Vector3f pivot, float inflate, Vector2f uv, boolean mirror)
    {
        this.cubeOriginX.setValue(origin.x);
        this.cubeOriginY.setValue(origin.y);
        this.cubeOriginZ.setValue(origin.z);
        this.cubeSizeX.setValue(size.x);
        this.cubeSizeY.setValue(size.y);
        this.cubeSizeZ.setValue(size.z);
        this.cubePivotX.setValue(pivot.x);
        this.cubePivotY.setValue(pivot.y);
        this.cubePivotZ.setValue(pivot.z);
        this.cubeInflate.setValue(inflate);
        this.cubeUvX.setValue(uv.x);
        this.cubeUvY.setValue(uv.y);
        this.cubeMirror.setValue(mirror);
    }

    private void updateVector(int type, int axis, float value)
    {
        if (this.filling || this.selectedGroup == null)
        {
            return;
        }

        Vector3f vector = switch (type)
        {
            case 0 -> this.selectedGroup.initial.translate;
            case 1 -> this.selectedGroup.initial.rotate;
            case 2 -> this.selectedGroup.initial.pivot;
            default -> this.selectedGroup.initial.scale;
        };

        if (axis == 0)
        {
            vector.x = value;
        }
        else if (axis == 1)
        {
            vector.y = value;
        }
        else
        {
            vector.z = value;
        }

        this.selectedGroup.current.copy(this.selectedGroup.initial);
        this.refreshCubeRenderAndSave();
    }

    private void updateCubeVector(int type, int axis, float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        Vector3f vector = switch (type)
        {
            case 0 -> this.selectedCube.origin;
            case 1 -> this.selectedCube.size;
            default -> this.selectedCube.pivot;
        };

        if (axis == 0)
        {
            vector.x = value;
        }
        else if (axis == 1)
        {
            vector.y = value;
        }
        else
        {
            vector.z = value;
        }

        this.refreshCubeRenderAndSave();
    }

    private void updateCubeInflate(float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        this.selectedCube.inflate = value;
        this.refreshCubeRenderAndSave();
    }

    private void updateCubeUV(int axis, float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        Vector2f uv = this.getBoxUV(this.selectedCube);

        if (axis == 0)
        {
            uv.x = value;
        }
        else
        {
            uv.y = value;
        }

        this.selectedCube.setupBoxUV(uv, this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
    }

    private void updateCubeMirror(boolean mirror)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        this.cubeMirrorValue = mirror;
        this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
    }

    private void addCube()
    {
        if (this.selectedGroup == null)
        {
            return;
        }

        ModelCube cube = this.selectedCube == null ? new ModelCube() : this.selectedCube.copy();

        if (this.selectedCube == null)
        {
            cube.size.set(1F, 1F, 1F);
            cube.pivot.set(this.selectedGroup.initial.pivot);
            cube.setupBoxUV(new Vector2f(0F, 0F), false);
        }

        this.selectedGroup.cubes.add(cube);
        this.selectedCube = cube;
        this.reloadModelData();

        for (GeometryEntry entry : this.hierarchyList.getList())
        {
            if (entry.type == GeometryEntryType.CUBE && entry.groupId.equals(this.selectedGroup.id) && entry.cubeIndex == this.selectedGroup.cubes.indexOf(cube))
            {
                this.hierarchyList.setCurrentDirect(entry);
                this.selectCurrentHierarchyEntry();

                break;
            }
        }

        this.refreshCubeRenderAndSave();
    }

    private void removeCube()
    {
        if (this.selectedGroup == null || this.selectedCube == null)
        {
            return;
        }

        int index = this.selectedGroup.cubes.indexOf(this.selectedCube);

        if (index < 0)
        {
            return;
        }

        this.selectedGroup.cubes.remove(index);
        this.selectedCube = null;
        this.reloadModelData();
        this.refreshCubeRenderAndSave();
    }

    private void refreshCubeRenderAndSave()
    {
        if (this.selectedCube != null && this.selectedGroup != null && this.selectedGroup.owner != null)
        {
            int tw = Math.max(1, this.selectedGroup.owner.textureWidth);
            int th = Math.max(1, this.selectedGroup.owner.textureHeight);

            this.selectedCube.generateQuads(tw, th);
        }

        if (this.instance != null)
        {
            this.instance.delete();
            this.instance.setup();
        }

        this.parent.dirty();
        this.autoSaveIfEnabled();
    }

    private void toggleGroupCollapsed(String groupId)
    {
        if (this.collapsedGroupIds.contains(groupId))
        {
            this.collapsedGroupIds.remove(groupId);
        }
        else
        {
            this.collapsedGroupIds.add(groupId);
        }

        GeometryEntry current = this.hierarchyList.getCurrentFirst();
        GeometryEntry preferred = current;

        if (current != null && current.type == GeometryEntryType.CUBE && current.groupId.equals(groupId))
        {
            preferred = new GeometryEntry(GeometryEntryType.BONE, groupId, -1, 0, groupId);
        }

        this.reloadHierarchyPreserveSelection(preferred);
    }

    private void reloadHierarchyPreserveSelection(GeometryEntry preferred)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        this.hierarchyList.clear();

        for (ModelGroup top : model.topGroups)
        {
            this.collectHierarchy(top, 0);
        }

        GeometryEntry selected = null;

        if (preferred != null)
        {
            for (GeometryEntry entry : this.hierarchyList.getList())
            {
                if (entry.type == preferred.type && entry.groupId.equals(preferred.groupId) && entry.cubeIndex == preferred.cubeIndex)
                {
                    selected = entry;
                    break;
                }
            }
        }

        if (selected == null && !this.hierarchyList.getList().isEmpty())
        {
            selected = this.hierarchyList.getList().get(0);
        }

        if (selected != null)
        {
            this.hierarchyList.setCurrentDirect(selected);
            this.selectCurrentHierarchyEntry();
        }
        else
        {
            this.selectedGroup = null;
            this.selectedCube = null;
            this.parent.renderer.setSelectedCube(null);
            this.fillControls();
            this.fillCubeControls();
        }
    }

    private void handleHierarchySwap(int from, int to)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        List<GeometryEntry> entries = this.hierarchyList.getList();

        if (from < 0 || from >= entries.size() || to < 0 || to >= entries.size() || from == to)
        {
            return;
        }

        GeometryEntry source = entries.get(from);
        GeometryEntry destination = entries.get(to);
        GeometryEntry preferred = null;

        if (source.type == GeometryEntryType.BONE)
        {
            preferred = this.reorderBoneByDrag(model, source, destination, to > from);
        }
        else if (source.type == GeometryEntryType.CUBE)
        {
            preferred = this.reorderCubeByDrag(model, source, destination, to > from);
        }

        if (preferred == null)
        {
            this.reloadHierarchyPreserveSelection(source);
            return;
        }

        model.initialize();
        this.reloadHierarchyPreserveSelection(preferred);
        this.refreshCubeRenderAndSave();
    }

    private GeometryEntry reorderBoneByDrag(Model model, GeometryEntry source, GeometryEntry destination, boolean moveAfter)
    {
        ModelGroup sourceGroup = model.getGroup(source.groupId);

        if (sourceGroup == null)
        {
            return null;
        }

        ModelGroup destinationGroup = model.getGroup(destination.groupId);

        if (destinationGroup == null || sourceGroup == destinationGroup || this.isDescendantGroup(sourceGroup, destinationGroup))
        {
            return null;
        }

        this.removeGroupFromParent(model, sourceGroup);

        if (destination.type == GeometryEntryType.BONE)
        {
            sourceGroup.parent = destinationGroup;

            if (moveAfter)
            {
                destinationGroup.children.add(sourceGroup);
            }
            else
            {
                destinationGroup.children.add(0, sourceGroup);
            }
        }
        else
        {
            sourceGroup.parent = destinationGroup;

            if (moveAfter)
            {
                destinationGroup.children.add(sourceGroup);
            }
            else
            {
                destinationGroup.children.add(0, sourceGroup);
            }
        }

        return new GeometryEntry(GeometryEntryType.BONE, sourceGroup.id, -1, 0, sourceGroup.id);
    }

    private GeometryEntry reorderCubeByDrag(Model model, GeometryEntry source, GeometryEntry destination, boolean moveAfter)
    {
        ModelGroup sourceGroup = model.getGroup(source.groupId);
        ModelGroup destinationGroup = model.getGroup(destination.groupId);

        if (sourceGroup == null || destinationGroup == null || source.cubeIndex < 0 || source.cubeIndex >= sourceGroup.cubes.size())
        {
            return null;
        }

        ModelCube cube = sourceGroup.cubes.remove(source.cubeIndex);
        int insertIndex;

        if (destination.type == GeometryEntryType.BONE)
        {
            insertIndex = moveAfter ? destinationGroup.cubes.size() : 0;
        }
        else
        {
            int destinationIndex = destination.cubeIndex;

            if (destinationIndex < 0 || destinationIndex >= destinationGroup.cubes.size())
            {
                sourceGroup.cubes.add(Math.min(source.cubeIndex, sourceGroup.cubes.size()), cube);

                return null;
            }

            insertIndex = destinationIndex + (moveAfter ? 1 : 0);

            if (sourceGroup == destinationGroup && source.cubeIndex < destinationIndex)
            {
                insertIndex -= 1;
            }
        }

        if (insertIndex < 0)
        {
            insertIndex = 0;
        }

        if (insertIndex > destinationGroup.cubes.size())
        {
            insertIndex = destinationGroup.cubes.size();
        }

        destinationGroup.cubes.add(insertIndex, cube);

        return new GeometryEntry(GeometryEntryType.CUBE, destinationGroup.id, insertIndex, 0, this.getCubeLabel(cube));
    }

    private void removeGroupFromParent(Model model, ModelGroup group)
    {
        if (group.parent == null)
        {
            model.topGroups.remove(group);
        }
        else
        {
            group.parent.children.remove(group);
        }
    }

    private boolean isDescendantGroup(ModelGroup source, ModelGroup candidateParent)
    {
        for (ModelGroup cursor = candidateParent; cursor != null; cursor = cursor.parent)
        {
            if (cursor == source)
            {
                return true;
            }
        }

        return false;
    }

    private String getCubeLabel(ModelCube cube)
    {
        if (cube == null || cube.name == null || cube.name.isBlank())
        {
            return "Cube";
        }

        return cube.name;
    }

    private void autoSaveIfEnabled()
    {
        if (this.autoSaveToggle.getValue())
        {
            this.saveModelFile();
        }
    }

    private boolean isCubeMirrored(ModelCube cube)
    {
        return cube.front != null && cube.front.size.x < 0;
    }

    private Vector2f getBoxUV(ModelCube cube)
    {
        Vector2f uv = new Vector2f();

        if (cube.front != null)
        {
            float depth = (float) Math.floor(Math.abs(cube.size.z));
            float width = (float) Math.floor(Math.abs(cube.size.x));

            uv.x = this.isCubeMirrored(cube) ? cube.front.origin.x - depth - width : cube.front.origin.x - depth;
            uv.y = cube.front.origin.y - depth;
        }

        return uv;
    }

    private void saveModelFile()
    {
        if (this.config == null || this.instance == null)
        {
            return;
        }

        File file = this.findModelFile(this.config.getId());

        if (file == null)
        {
            return;
        }

        MapType map = CubicLoader.toData(this.instance);

        try
        {
            IOUtils.writeText(file, DataToString.toString(map, true));
            BBSModClient.getModels().loadModel(this.config.getId());
            this.parent.renderer.invalidatePreviewModel();
            this.parent.renderer.setModel(this.config.getId());
            this.parent.renderer.setConfig(this.config);
            this.reloadModelData();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private File findModelFile(String id)
    {
        Link root = Link.assets("models/" + id);
        File direct = BBSMod.getProvider().getFile(root.combine("model.bbs.json"));

        if (direct != null && direct.exists())
        {
            return direct;
        }

        File folder = BBSMod.getProvider().getFile(root);

        if (folder == null || !folder.exists())
        {
            return null;
        }

        return this.findBbsRecursively(folder);
    }

    private File findBbsRecursively(File folder)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return null;
        }

        for (File file : files)
        {
            if (file.isFile() && file.getName().endsWith(".bbs.json"))
            {
                return file;
            }
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                File result = this.findBbsRecursively(file);

                if (result != null)
                {
                    return result;
                }
            }
        }

        return null;
    }

    private enum GeometryEntryType
    {
        BONE,
        CUBE
    }

    private static class GeometryEntry
    {
        private final GeometryEntryType type;
        private final String groupId;
        private final int cubeIndex;
        private final int depth;
        private final String label;

        private GeometryEntry(GeometryEntryType type, String groupId, int cubeIndex, int depth, String label)
        {
            this.type = type;
            this.groupId = groupId;
            this.cubeIndex = cubeIndex;
            this.depth = depth;
            this.label = label;
        }
    }
}
