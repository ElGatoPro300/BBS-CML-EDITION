package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Illusions: purely visual duplicates of a form that spread away from it in the
 * picked directions (no extra entities, so they're cheap to render).
 */
public class Illusion implements IMapSerializable
{
    /* Direction bit flags */
    public static final int FRONT = 1;
    public static final int BACK = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int UP = 16;
    public static final int DOWN = 32;

    public int count;
    public float spread;
    public int directions;
    public float offset;
    public float opacity;
    public boolean invert;

    /* Equal gaps between illusions (instead of progressively shrinking ones) */
    public boolean uniform;
    public float spacing;

    /* Per illusion texture overrides */
    public final List<Link> textures = new ArrayList<>();
    public boolean randomTextures;

    /* "Real" illusions interact with world blocks (step up onto blocks in their way) */
    public boolean real;

    /* Illusions get placed right next to each other (no overlapping) */
    public boolean connected;

    /* The illusion transform track gets applied gradually from the first illusion (none) to the last one (full) */
    public boolean gradual;

    /* Staggered animation: illusion N lags behind the main model by N * delay ticks */
    public boolean delayEnabled;
    public float delay;

    /* Disintegration: illusions (from the last one to the first) fall apart into horizontal streaks */
    public float distort;

    /* Glow: illusions get emissive (positive) or darkened (negative), by default fading from the first illusion to the last */
    public boolean glowEnabled;
    public float glow;
    public boolean glowUniform;
    public boolean glowInvert;

    public Illusion()
    {}

    public boolean hasSameShape(Illusion illusion)
    {
        return illusion != null
            && this.directions == illusion.directions
            && this.invert == illusion.invert
            && this.uniform == illusion.uniform
            && this.randomTextures == illusion.randomTextures
            && this.real == illusion.real
            && this.connected == illusion.connected
            && this.gradual == illusion.gradual
            && this.delayEnabled == illusion.delayEnabled
            && this.glowEnabled == illusion.glowEnabled
            && this.glowUniform == illusion.glowUniform
            && this.glowInvert == illusion.glowInvert
            && this.textures.equals(illusion.textures);
    }

    public Illusion copy()
    {
        Illusion illusion = new Illusion();

        illusion.count = this.count;
        illusion.spread = this.spread;
        illusion.directions = this.directions;
        illusion.offset = this.offset;
        illusion.opacity = this.opacity;
        illusion.invert = this.invert;
        illusion.uniform = this.uniform;
        illusion.spacing = this.spacing;
        illusion.textures.addAll(this.textures);
        illusion.randomTextures = this.randomTextures;
        illusion.real = this.real;
        illusion.connected = this.connected;
        illusion.gradual = this.gradual;
        illusion.delayEnabled = this.delayEnabled;
        illusion.delay = this.delay;
        illusion.distort = this.distort;
        illusion.glowEnabled = this.glowEnabled;
        illusion.glow = this.glow;
        illusion.glowUniform = this.glowUniform;
        illusion.glowInvert = this.glowInvert;

        return illusion;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Illusion illusion)
        {
            return this.hasSameShape(illusion)
                && this.count == illusion.count
                && this.spread == illusion.spread
                && this.offset == illusion.offset
                && this.opacity == illusion.opacity
                && this.spacing == illusion.spacing
                && this.delay == illusion.delay
                && this.distort == illusion.distort
                && this.glow == illusion.glow;
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.count = data.getInt("count", 0);
        this.spread = data.getFloat("spread", 0F);
        this.directions = data.getInt("directions", 0);
        this.offset = data.getFloat("offset", 0F);
        this.opacity = data.getFloat("opacity", 0F);
        this.invert = data.getBool("invert", false);
        this.uniform = data.getBool("uniform", false);
        this.spacing = data.getFloat("spacing", 0F);
        this.randomTextures = data.getBool("random_textures", false);
        this.real = data.getBool("real", false);
        this.connected = data.getBool("connected", false);
        this.gradual = data.getBool("gradual", false);
        this.delayEnabled = data.getBool("delay_enabled", false);
        this.delay = data.getFloat("delay", 0F);
        this.distort = data.getFloat("distort", 0F);
        this.glowEnabled = data.getBool("glow_enabled", false);
        this.glow = data.getFloat("glow", 0F);
        this.glowUniform = data.getBool("glow_uniform", false);
        this.glowInvert = data.getBool("glow_invert", false);
        this.textures.clear();

        if (data.has("textures"))
        {
            for (BaseType type : data.getList("textures"))
            {
                if (type.isString())
                {
                    this.textures.add(Link.create(type.asString()));
                }
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putInt("count", this.count);
        data.putFloat("spread", this.spread);
        data.putInt("directions", this.directions);
        data.putFloat("offset", this.offset);
        data.putFloat("opacity", this.opacity);
        data.putBool("invert", this.invert);
        data.putBool("uniform", this.uniform);
        data.putFloat("spacing", this.spacing);
        data.putBool("random_textures", this.randomTextures);
        data.putBool("real", this.real);
        data.putBool("connected", this.connected);
        data.putBool("gradual", this.gradual);
        data.putBool("delay_enabled", this.delayEnabled);
        data.putFloat("delay", this.delay);
        data.putFloat("distort", this.distort);
        data.putBool("glow_enabled", this.glowEnabled);
        data.putFloat("glow", this.glow);
        data.putBool("glow_uniform", this.glowUniform);
        data.putBool("glow_invert", this.glowInvert);

        if (!this.textures.isEmpty())
        {
            ListType list = new ListType();

            for (Link link : this.textures)
            {
                list.addString(link.toString());
            }

            data.put("textures", list);
        }
    }
}
