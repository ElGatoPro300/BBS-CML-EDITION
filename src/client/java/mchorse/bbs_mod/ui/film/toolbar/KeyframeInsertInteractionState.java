package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;

/**
 * Active keyframe insert interaction (column or single-track preview).
 */
public final class KeyframeInsertInteractionState
{
    public final IKey hint;
    public final boolean column;
    /**
     * When {@code >= 0}, the preview tick is fixed (playhead). When {@code -1},
     * the tick follows the cursor horizontally.
     */
    public final int lockedTick;
    public final IColumnConfirm columnConfirm;
    public final IIndividualConfirm individualConfirm;

    public KeyframeInsertInteractionState(IKey hint, boolean column, int lockedTick,
        IColumnConfirm columnConfirm, IIndividualConfirm individualConfirm)
    {
        this.hint = hint;
        this.column = column;
        this.lockedTick = lockedTick;
        this.columnConfirm = columnConfirm;
        this.individualConfirm = individualConfirm;
    }

    public static KeyframeInsertInteractionState columnAtCursor(IKey hint, IColumnConfirm confirm)
    {
        return new KeyframeInsertInteractionState(hint, true, -1, confirm, null);
    }

    public static KeyframeInsertInteractionState columnAtTick(IKey hint, int tick, IColumnConfirm confirm)
    {
        return new KeyframeInsertInteractionState(hint, true, tick, confirm, null);
    }

    public static KeyframeInsertInteractionState individualAtPlayhead(IKey hint, int tick,
        IIndividualConfirm confirm)
    {
        return new KeyframeInsertInteractionState(hint, false, tick, null, confirm);
    }

    public static KeyframeInsertInteractionState individualAtCursor(IKey hint, IIndividualConfirm confirm)
    {
        return new KeyframeInsertInteractionState(hint, false, -1, null, confirm);
    }

    @FunctionalInterface
    public interface IColumnConfirm
    {
        void insertAt(int tick);
    }

    @FunctionalInterface
    public interface IIndividualConfirm
    {
        void insertAt(UIKeyframeSheet sheet, float tick);
    }
}
