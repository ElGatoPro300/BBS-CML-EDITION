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
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.IOUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UIModelGeometryPanel extends UIElement
{
    private final UIModelPanel parent;
    private final UIStringList hierarchyList;
    private final UISearchList<String> hierarchySearch;
    private final List<String> hierarchyBoneIds = new ArrayList<>();
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

    private final UIStringList cubeList;
    private final UISearchList<String> cubeSearch;

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

        this.hierarchyList = new UIStringList((l) -> this.selectCurrentListBone())
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }
        };
        this.hierarchyList.background();
        this.hierarchySearch = new UISearchList<>(this.hierarchyList);
        this.hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        this.hierarchySearch.relative(this).x(sideMargin).y(26).w(leftWidth).h(1F, -36);

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

        this.add(hierarchyTitle, this.hierarchySearch, editor);

        UILabel cubesTitle = UI.label(IKey.raw("Cubes")).background();
        cubesTitle.relative(this).x(sideMargin).y(1F, -180).w(leftWidth).h(12);

        this.cubeList = new UIStringList((l) -> this.selectCurrentCube());
        this.cubeList.background();
        this.cubeSearch = new UISearchList<>(this.cubeList);
        this.cubeSearch.label(UIKeys.GENERAL_SEARCH);
        this.cubeSearch.relative(this).x(sideMargin).y(1F, -164).w(leftWidth).h(128);

        this.addCubeButton = new UIButton(UIKeys.GENERAL_ADD, (b) -> this.addCube());
        this.removeCubeButton = new UIButton(UIKeys.GENERAL_REMOVE, (b) -> this.removeCube());
        this.addCubeButton.w(0.5F, -4).h(20);
        this.removeCubeButton.w(0.5F, -4).h(20);
        UIElement cubeButtons = UI.row(8, this.addCubeButton, this.removeCubeButton);
        cubeButtons.relative(this.cubeSearch).y(1F, 6).w(1F).h(20);

        this.add(cubesTitle, this.cubeSearch, cubeButtons);

        this.fillControls();
        this.fillCubeList();
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

            this.selectedCubeLabel.label = IKey.raw("Cube " + (index + 1));
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

        int index = this.hierarchyBoneIds.indexOf(bone);

        if (index >= 0 && index < this.hierarchyList.getList().size())
        {
            this.hierarchyList.setCurrent(this.hierarchyList.getList().get(index));
        }
    }

    private void reloadModelData()
    {
        this.instance = null;
        this.selectedGroup = null;
        this.hierarchyList.clear();
        this.hierarchyBoneIds.clear();

        if (this.config == null)
        {
            this.fillControls();
            this.fillCubeList();
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
            this.fillCubeList();
            return;
        }

        for (ModelGroup group : model.topGroups)
        {
            this.collectHierarchy(group, 0);
        }

        if (!this.hierarchyList.getList().isEmpty())
        {
            this.hierarchyList.setCurrent(this.hierarchyList.getList().get(0));
        }
        else
        {
            this.fillControls();
            this.fillCubeList();
        }
    }

    private void collectHierarchy(ModelGroup group, int depth)
    {
        StringBuilder prefix = new StringBuilder();

        for (int i = 0; i < depth; i++)
        {
            prefix.append("  ");
        }

        this.hierarchyList.add(prefix + "└ " + group.id);
        this.hierarchyBoneIds.add(group.id);

        for (ModelGroup child : group.children)
        {
            this.collectHierarchy(child, depth + 1);
        }
    }

    private void selectCurrentListBone()
    {
        this.selectedGroup = null;

        if (this.instance != null && this.instance.model instanceof Model model)
        {
            int index = this.hierarchyList.getIndex();

            if (index >= 0 && index < this.hierarchyBoneIds.size())
            {
                String bone = this.hierarchyBoneIds.get(index);

                this.selectedGroup = model.getGroup(bone);
                this.parent.renderer.setSelectedBone(bone);
            }
        }

        this.fillControls();
        this.fillCubeList();
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

    private void fillCubeList()
    {
        this.fillCubeList(-1);
    }

    private void fillCubeList(int preferredIndex)
    {
        this.cubeList.clear();
        this.selectedCube = null;
        this.parent.renderer.setSelectedCube(null);

        if (this.selectedGroup != null)
        {
            for (int i = 0; i < this.selectedGroup.cubes.size(); i++)
            {
                this.cubeList.add("Cube " + (i + 1));
            }

            if (!this.selectedGroup.cubes.isEmpty())
            {
                int clamped = preferredIndex < 0 ? 0 : Math.min(preferredIndex, this.selectedGroup.cubes.size() - 1);
                this.cubeList.setCurrent(this.cubeList.getList().get(clamped));
            }
        }

        this.fillCubeControls();
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
        this.fillCubeList(this.selectedGroup.cubes.size() - 1);
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
        this.fillCubeList(index);
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

    private void selectCurrentCube()
    {
        this.selectedCube = null;

        if (this.selectedGroup != null)
        {
            int index = this.cubeList.getIndex();

            if (index >= 0 && index < this.selectedGroup.cubes.size())
            {
                this.selectedCube = this.selectedGroup.cubes.get(index);
            }
        }

        this.parent.renderer.setSelectedCube(this.selectedCube);
        this.fillCubeControls();
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
}
