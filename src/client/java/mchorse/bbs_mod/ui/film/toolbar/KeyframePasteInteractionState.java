package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active keyframe paste interaction (preview + click to confirm).
 */
public final class KeyframePasteInteractionState
{
    public final IKey hint;
    /**
     * When {@code >= 0}, the anchor tick is fixed (playhead). When {@code -1},
     * the anchor tick follows the cursor horizontally.
     */
    public final int lockedTick;

    public KeyframePasteInteractionState(IKey hint, int lockedTick)
    {
        this.hint = hint;
        this.lockedTick = lockedTick;
    }

    public static KeyframePasteInteractionState atCursor(IKey hint)
    {
        return new KeyframePasteInteractionState(hint, -1);
    }

    public static KeyframePasteInteractionState atPlayhead(IKey hint, int tick)
    {
        return new KeyframePasteInteractionState(hint, tick);
    }
}
