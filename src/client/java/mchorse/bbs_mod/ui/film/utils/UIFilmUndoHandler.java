package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.utils.undo.UIViewUndo;
import mchorse.bbs_mod.ui.film.utils.undo.ValueChangeUndo;
import mchorse.bbs_mod.ui.forms.editors.UIFormUndoHandler;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.undo.CompoundUndo;
import mchorse.bbs_mod.utils.undo.IUndo;

import java.util.HashSet;
import java.util.Set;

public class UIFilmUndoHandler extends UIFormUndoHandler
{
    private Timer actionsTimer = new Timer(100);
    private Set<BaseValue> syncData = new HashSet<>();
    private boolean isUndoing;

    public UIFilmUndoHandler(UIFilmPanel panel)
    {
        super(panel);
    }

    public boolean isUndoing()
    {
        return this.isUndoing;
    }

    /**
     * Records a UI-only undo step (embedded view navigation, scroll, selection, etc.)
     * so it is not bundled with the next data change undo.
     */
    public void pushUIViewUndo(MapType uiBefore, MapType uiAfter)
    {
        if (this.isUndoing || uiBefore == null || uiAfter == null)
        {
            return;
        }

        this.undoManager.pushUndo(new UIViewUndo((MapType) uiBefore.copy(), (MapType) uiAfter.copy()).noMerging());
    }

    public boolean undo(ValueGroup context)
    {
        this.isUndoing = true;

        try
        {
            return this.undoManager.undoNext(context, (undo) -> this.shouldDeferEmbeddedUndo(undo, false));
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    public boolean redo(ValueGroup context)
    {
        this.isUndoing = true;

        try
        {
            return this.undoManager.redoNext(context, (undo) -> this.shouldDeferEmbeddedUndo(undo, true));
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    @Override
    public void reset()
    {
        super.reset();

        this.undoManager.setCallback(this::handleFilmUndos);
    }

    private void handleFilmUndos(IUndo<ValueGroup> undo, boolean redo)
    {
        this.isUndoing = true;

        try
        {
            IUndo<ValueGroup> anotherUndo = undo;

            if (anotherUndo instanceof CompoundUndo)
            {
                anotherUndo = ((CompoundUndo<ValueGroup>) anotherUndo).getFirst(ValueChangeUndo.class);
            }

            MapType uiData = this.resolveUndoUIData(anotherUndo, redo);

            if (uiData != null)
            {
                if (this.uiElement instanceof UIFilmPanel panel)
                {
                    uiData = this.preserveEmbeddedContext(uiData, panel, anotherUndo, redo);
                    panel.applyFilmUndoData(uiData);
                }
                else
                {
                    UIElement root = this.uiElement.getRoot();

                    if (root != null)
                    {
                        root.applyAllUndoData(uiData);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    @Override
    public void handlePreValues(BaseValue baseValue, int flag)
    {
        if (this.isUndoing || this.isFilmMetadata(baseValue))
        {
            return;
        }

        super.handlePreValues(baseValue, flag);
    }

    @Override
    public void submitUndo()
    {
        this.cachedValues.keySet().removeIf(this::isFilmMetadata);

        super.submitUndo();
    }

    @Override
    protected void handleValue(BaseValue value)
    {
        if (!this.isUndoing)
        {
            super.handleValue(value);
        }

        if (this.isReplayActions(value))
        {
            this.syncData.add(value);
            this.actionsTimer.mark();
        }
    }

    @Override
    protected void handleTimers()
    {
        super.handleTimers();

        if (this.actionsTimer.checkReset())
        {
            this.submitUndo();

            for (BaseValue syncData : this.syncData)
            {
                ClientNetwork.sendSyncData(((UIFilmPanel) this.uiElement).getData().getId(), syncData);
            }

            this.syncData.clear();
        }
    }

    private boolean isFilmMetadata(BaseValue value)
    {
        String path = value.getPath().toString();

        return path.endsWith("/totalTimeWorked") || path.endsWith("/contributors") || path.contains("/contributors/");
    }

    private boolean isReplayActions(BaseValue value)
    {
        String path = value.getPath().toString();

        if (
            path.endsWith("/replays") ||
            path.contains("/keyframes/") ||
            path.contains("/properties/") ||
            path.endsWith("/drop_items_on_death") ||
            path.endsWith("/actor") ||
            path.endsWith("/enabled") ||
            path.endsWith("/form") ||
            path.endsWith("/inventory") ||
            path.contains("/drop_velocity_")
        ) {
            return true;
        }

        /* Specifically for overwriting full replay like what's done when recording
         * data in the world! */
        if (value.getParent() != null && value.getParent().getId().equals("replays"))
        {
            return true;
        }

        while (value != null)
        {
            if (value instanceof Clips clips && clips.getFactory() == BBSMod.getFactoryActionClips())
            {
                return true;
            }

            value = value.getParent();
        }

        return false;
    }

    private MapType resolveUndoUIData(IUndo<ValueGroup> undo, boolean redo)
    {
        if (undo instanceof UIViewUndo viewUndo)
        {
            return viewUndo.getUIData(redo);
        }
        else if (undo instanceof ValueChangeUndo change)
        {
            return change.getUIData(redo);
        }

        return null;
    }

    /**
     * While an embedded clip editor is open, keep it open when undoing/redoing data
     * changes whose snapshots were captured without embedded context.
     */
    private MapType preserveEmbeddedContext(MapType uiData, UIFilmPanel panel, IUndo<ValueGroup> undo, boolean redo)
    {
        if (!(undo instanceof ValueChangeUndo) || !panel.hasEmbeddedClipView())
        {
            return uiData;
        }

        String editorId = panel.getEmbeddedClipEditorUndoId();

        if (editorId.isEmpty())
        {
            return uiData;
        }

        MapType editorData = uiData.getMap(editorId);

        if (editorData != null && !editorData.getString("embedded_id").isEmpty())
        {
            return uiData;
        }

        MapType reference = panel.collectFilmUndoSnapshot();
        MapType referenceEditor = reference.getMap(editorId);

        if (referenceEditor == null || referenceEditor.getString("embedded_id").isEmpty())
        {
            return uiData;
        }

        MapType merged = (MapType) uiData.copy();
        MapType mergedEditor = merged.getMap(editorId);

        if (mergedEditor == null)
        {
            mergedEditor = new MapType();
            merged.put(editorId, mergedEditor);
        }

        MapType embeddedState = referenceEditor.getMap("embedded_state");

        mergedEditor.putString("embedded_id", referenceEditor.getString("embedded_id"));
        mergedEditor.putBool("embedded_stacked", referenceEditor.getBool("embedded_stacked"));

        if (embeddedState != null)
        {
            mergedEditor.put("embedded_state", embeddedState.copy());
        }

        return merged;
    }

    /**
     * Skip legacy "enter embedded view" UI steps while already inside an embedded editor,
     * so data undos can be processed first.
     */
    private boolean shouldDeferEmbeddedUndo(IUndo<ValueGroup> undo, boolean redo)
    {
        if (!(this.uiElement instanceof UIFilmPanel panel))
        {
            return false;
        }

        if (!(undo instanceof UIViewUndo viewUndo))
        {
            return false;
        }

        if (!panel.hasEmbeddedClipView())
        {
            return false;
        }

        MapType uiData = viewUndo.getUIData(!redo);

        return uiData != null && !this.hasEmbeddedClipView(uiData);
    }

    private boolean hasEmbeddedClipView(MapType uiData)
    {
        return this.hasEmbeddedClipView(uiData, "camera_editor") || this.hasEmbeddedClipView(uiData, "action_editor");
    }

    private boolean hasEmbeddedClipView(MapType uiData, String editorId)
    {
        MapType editorData = uiData.getMap(editorId);

        return editorData != null && !editorData.getString("embedded_id").isEmpty();
    }
}
