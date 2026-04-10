package mchorse.bbs_mod.cubic.animation.legacy.config;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LegacyAnimationsConfig implements IMapSerializable
{
    public boolean enabled;
    public final Map<String, LegacyLimbAnimationConfig> limbs = new HashMap<>();

    public boolean isDefault()
    {
        return !this.enabled || this.limbs.isEmpty();
    }

    public LegacyLimbAnimationConfig getConfig(String key)
    {
        LegacyLimbAnimationConfig output = this.limbs.get(key);

        return output == null ? new LegacyLimbAnimationConfig() : output;
    }

    public void copy(LegacyAnimationsConfig config)
    {
        this.enabled = config.enabled;
        this.limbs.clear();

        for (Map.Entry<String, LegacyLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            this.limbs.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("enabled", this.enabled);

        MapType limbData = new MapType();

        for (Map.Entry<String, LegacyLimbAnimationConfig> entry : this.limbs.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }

            MapType map = new MapType();
            entry.getValue().toData(map);
            limbData.put(entry.getKey(), map);
        }

        if (!limbData.isEmpty())
        {
            data.put("limbs", limbData);
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.enabled = data.getBool("enabled");
        this.limbs.clear();

        for (Map.Entry<String, BaseType> entry : data.getMap("limbs"))
        {
            if (!entry.getValue().isMap())
            {
                continue;
            }

            LegacyLimbAnimationConfig config = new LegacyLimbAnimationConfig();

            config.fromData(entry.getValue().asMap());
            this.limbs.put(entry.getKey(), config);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof LegacyAnimationsConfig config))
        {
            return false;
        }

        return this.enabled == config.enabled && Objects.equals(this.limbs, config.limbs);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.enabled, this.limbs);
    }
}
