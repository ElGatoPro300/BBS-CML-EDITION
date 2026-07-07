package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active keyframe paste-at-cursor interaction (preview + click to confirm).
 */
public final class KeyframePasteInteractionState
{
    public final IKey hint;

    public KeyframePasteInteractionState(IKey hint)
    {
        this.hint = hint;
    }
}
