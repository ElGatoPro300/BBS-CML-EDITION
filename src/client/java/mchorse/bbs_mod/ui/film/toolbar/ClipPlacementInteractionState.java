package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active clip-placement interaction: preview follows the cursor until the user
 * confirms with a left click.
 */
public final class ClipPlacementInteractionState
{
    public final IKey hint;
    public final int duration;
    public final IClipPlacementConfirm onConfirm;

    public ClipPlacementInteractionState(IKey hint, int duration, IClipPlacementConfirm onConfirm)
    {
        this.hint = hint;
        this.duration = duration;
        this.onConfirm = onConfirm;
    }

    @FunctionalInterface
    public interface IClipPlacementConfirm
    {
        void place(int tick, int layer, int duration);
    }
}
