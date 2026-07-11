package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active keyframe duplicate interaction (preview + click to confirm).
 */
public final class KeyframeDuplicateInteractionState
{
    public final IKey hint;
    /**
     * When {@code >= 0}, the anchor tick is fixed (playhead). When {@code -1},
     * the anchor tick follows the cursor horizontally.
     */
    public final int lockedTick;

    public KeyframeDuplicateInteractionState(IKey hint, int lockedTick)
    {
        this.hint = hint;
        this.lockedTick = lockedTick;
    }

    public static KeyframeDuplicateInteractionState atCursor(IKey hint)
    {
        return new KeyframeDuplicateInteractionState(hint, -1);
    }

    public static KeyframeDuplicateInteractionState atPlayhead(IKey hint, int tick)
    {
        return new KeyframeDuplicateInteractionState(hint, tick);
    }
}
