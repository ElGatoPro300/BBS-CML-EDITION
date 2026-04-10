package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AttackActionClip extends ActionClip
{
    public final ValueFloat damage = new ValueFloat("damage", 0F);

    public AttackActionClip()
    {
        super();

        this.add(this.damage);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        float damage = this.damage.get();

        if (damage <= 0F)
        {
            return;
        }

        this.applyPositionRotation(player, replay, tick);

        double distance = 6D;
        HitResult blockHit = player.pick(distance, 1F, false);
        Vec3 origin = player.getEyePosition(1F);
        Vec3 rotation = player.getViewVector(1F);
        Vec3 direction = origin.add(rotation.x * distance, rotation.y * distance, rotation.z * distance);

        double newDistance = blockHit != null ? blockHit.getLocation().distanceToSqr(origin) : distance * distance;
        AABB box = player.getBoundingBox().expandTowards(rotation.scale(distance)).inflate(1, 1, 1);
        EntityHitResult enittyHit = ProjectileUtil.getEntityHitResult(actor == null ? player : actor, origin, direction, box, entity -> !entity.isSpectator() && entity.isPickable(), newDistance);

        if (enittyHit != null)
        {
            Entity entity = enittyHit.getEntity();

            if (entity != null && player.level() instanceof ServerLevel serverWorld)
            {
                entity.hurtServer(serverWorld, player.level().damageSources().mobAttack(player), damage);
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new AttackActionClip();
    }
}