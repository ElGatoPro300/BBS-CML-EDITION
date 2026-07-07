package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active clip-placement interaction: preview follows the cursor until the user
 * confirms with a left click.
 */
public final class ClipPlacementInteractionState
{
    /**
     * When {@code >= 0}, the preview tick is fixed to this value and only the
     * layer follows the cursor vertically.
     */
    public final int lockedTick;
    public final IKey hint;
    public final int duration;
    public final IClipPlacementConfirm onConfirm;

    public ClipPlacementInteractionState(IKey hint, int duration, int lockedTick,
        IClipPlacementConfirm onConfirm)
    {
        this.hint = hint;
        this.duration = duration;
        this.lockedTick = lockedTick;
        this.onConfirm = onConfirm;
    }

    @FunctionalInterface
    public interface IClipPlacementConfirm
    {
        void place(int tick, int layer, int duration);
    }
}
