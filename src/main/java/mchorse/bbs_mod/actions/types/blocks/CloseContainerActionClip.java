package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;

public class CloseContainerActionClip extends BlockActionClip
{
    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        player.closeReplayChest(replay.getId());
        player.closeHandledScreen();
    }

    @Override
    protected Clip create()
    {
        return new CloseContainerActionClip();
    }
}
