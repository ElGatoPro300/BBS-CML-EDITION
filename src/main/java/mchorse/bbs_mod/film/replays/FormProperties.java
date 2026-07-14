package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class FormProperties extends ValueGroup
{
    public final Map<String, KeyframeChannel> properties = new HashMap<>();

    public FormProperties(String id)
    {
        super(id);
    }

    public void shift(float tick)
    {
        for (KeyframeChannel<?> value : this.properties.values())
        {
            for (Keyframe<?> keyframe : value.getKeyframes())
            {
                keyframe.setTick(keyframe.getTick() + tick);
            }
        }
    }

    public KeyframeChannel getOrCreate(Form form, String key)
    {
        BaseValue value = this.get(key);

        if (value instanceof KeyframeChannel channel)
        {
            return channel;
        }

        int colon = key.indexOf(':');

        if (colon != -1)
        {
            String propertyId = key.substring(0, colon);
            BaseValue property = FormUtils.getProperty(form, propertyId);

            if (property instanceof ValuePose)
            {
                KeyframeChannel channel = new KeyframeChannel(key, KeyframeFactories.TRANSFORM);

                this.properties.put(key, channel);
                this.add(channel);

                return channel;
            }
        }

        BaseValue property = FormUtils.getProperty(form, key);

        return property != null ? this.create(property) : null;
    }

    public KeyframeChannel create(BaseValue property)
    {
        if (property.isVisible() && property instanceof BaseKeyframeFactoryValue<?> keyframeFactoryValue)
        {
            String key = FormUtils.getPropertyPath(property);
            KeyframeChannel channel = new KeyframeChannel(key, keyframeFactoryValue.getFactory());

            this.properties.put(key, channel);
            this.add(channel);

            return channel;
        }

        return null;
    }

    public void applyProperties(Form form, float tick)
    {
        this.applyProperties(form, tick, 1F);
    }

    public void applyProperties(Form form, float tick, float blend)
    {
        if (form == null)
        {
            return;
        }

        /* First pass: apply standard properties */
        for (KeyframeChannel value : this.properties.values())
        {
            if (value.getId().indexOf(':') == -1)
            {
                this.applyProperty(tick, form, value, blend);
            }
        }

        /* Second pass: apply limb tracks (which override standard properties) */
        for (KeyframeChannel value : this.properties.values())
        {
            if (value.getId().indexOf(':') != -1)
            {
                this.applyProperty(tick, form, value, blend);
            }
        }
    }

    private void applyProperty(float tick, Form form, KeyframeChannel value, float blend)
    {
        String id = value.getId();
        int colon = id.indexOf(':');

        if (colon != -1)
        {
            String propertyId = id.substring(0, colon);
            String boneName = id.substring(colon + 1);
            BaseValueBasic property = FormUtils.getProperty(form, propertyId);

            if (property instanceof ValuePose valuePose)
            {
                KeyframeSegment segment = value.find(tick);

                if (segment != null)
                {
                    Transform transform = (Transform) segment.createInterpolated();
                    Pose pose = valuePose.getRuntimeValue();

                    if (pose == null)
                    {
                        pose = new Pose();
                        valuePose.setRuntimeValue(pose);
                    }

                    PoseTransform poseTransform = pose.get(boneName);

                    if (blend < 1F)
                    {
                        poseTransform.translate.add(transform.translate.x * blend, transform.translate.y * blend, transform.translate.z * blend);
                        poseTransform.scale.mul(1F + (transform.scale.x - 1F) * blend, 1F + (transform.scale.y - 1F) * blend, 1F + (transform.scale.z - 1F) * blend);
                        poseTransform.rotate.add(transform.rotate.x * blend, transform.rotate.y * blend, transform.rotate.z * blend);
                        poseTransform.rotate2.add(transform.rotate2.x * blend, transform.rotate2.y * blend, transform.rotate2.z * blend);
                    }
                    else
                    {
                        poseTransform.translate.add(transform.translate);
                        poseTransform.scale.mul(transform.scale);
                        poseTransform.rotate.add(transform.rotate);
                        poseTransform.rotate2.add(transform.rotate2);
                    }

                    PoseTransform sourcePose = null;

                    if (transform instanceof PoseTransform transformPose)
                    {
                        sourcePose = transformPose;
                    }
                    else
                    {
                        Object closestValue = segment.getClosest().getValue();

                        if (closestValue instanceof PoseTransform closestPose)
                        {
                            sourcePose = closestPose;
                        }
                    }

                    if (sourcePose != null)
                    {
                        if (blend < 1F)
                        {
                            poseTransform.fix = Lerps.lerp(poseTransform.fix, sourcePose.fix, blend);
                            poseTransform.color.r = Lerps.lerp(poseTransform.color.r, sourcePose.color.r, blend);
                            poseTransform.color.g = Lerps.lerp(poseTransform.color.g, sourcePose.color.g, blend);
                            poseTransform.color.b = Lerps.lerp(poseTransform.color.b, sourcePose.color.b, blend);
                            poseTransform.color.a = Lerps.lerp(poseTransform.color.a, sourcePose.color.a, blend);
                            poseTransform.lighting = Lerps.lerp(poseTransform.lighting, sourcePose.lighting, blend);

                            if (sourcePose.texture != null && blend >= 0.5F)
                            {
                                poseTransform.texture = LinkUtils.copy(sourcePose.texture);
                            }
                        }
                        else
                        {
                            poseTransform.fix = sourcePose.fix;
                            poseTransform.color.copy(sourcePose.color);
                            poseTransform.lighting = sourcePose.lighting;
                            poseTransform.texture = LinkUtils.copy(sourcePose.texture);
                        }
                    }
                }

                return;
            }
        }

        BaseValueBasic property = FormUtils.getProperty(form, id);

        if (property == null)
        {
            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            if (blend < 1F)
            {
                IKeyframeFactory factory = value.getFactory();
                Object v = factory.copy(property.get());
                Object a = factory.copy(segment.createInterpolated());
                Object interpolated = factory.interpolate(v, v, a, a, Interpolations.LINEAR, MathUtils.clamp(blend, 0F, 1F));

                property.setRuntimeValue(factory.copy(interpolated));
            }
            else
            {
                property.setRuntimeValue(segment.createInterpolated());
            }
        }
        else
        {
            property.setRuntimeValue(null);
        }
    }

    public void resetProperties(Form form)
    {
        if (form == null)
        {
            return;
        }

        for (KeyframeChannel value : this.properties.values())
        {
            String id = value.getId();
            int colon = id.indexOf(':');

            if (colon != -1)
            {
                String propertyId = id.substring(0, colon);
                BaseValueBasic property = FormUtils.getProperty(form, propertyId);

                if (property instanceof ValuePose valuePose)
                {
                    valuePose.setRuntimeValue(null);
                }
            }

            BaseValueBasic property = FormUtils.getProperty(form, id);

            if (property == null)
            {
                continue;
            }

            property.setRuntimeValue(null);
        }
    }

    public void cleanUp()
    {
        Iterator<KeyframeChannel> it = this.properties.values().iterator();

        while (it.hasNext())
        {
            KeyframeChannel next = it.next();

            if (next.isEmpty())
            {
                it.remove();
                this.remove(next);
            }
        }
    }

    @Override
    public void fromData(BaseType data)
    {
        /* FormProperties stores dynamic channels; rebuild from serialized data to avoid stale channels. */
        this.properties.clear();
        this.removeAll();

        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        for (String key : map.keys())
        {
            MapType mapType = map.getMap(key);

            if (mapType.isEmpty())
            {
                continue;
            }

            KeyframeChannel property = new KeyframeChannel(key, null);

            property.fromData(mapType);

            /* Patch 1.1.1 changes to lighting property */
            if (key.endsWith("lighting") && property.getFactory() == KeyframeFactories.BOOLEAN)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.FLOAT);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Boolean v = (Boolean) kf.getValue();

                    newProperty.insert(kf.getTick(), v ? 1F : 0F);
                }

                property = newProperty;
            }

            if (property.getFactory() != null)
            {
                this.properties.put(key, property);
                this.add(property);
            }
        }

        /* Migration: synthesize structure_light from legacy emit_light and light_intensity channels */
        try
        {
            KeyframeChannel<?> emit = this.properties.get("emit_light");
            KeyframeChannel<?> intensity = this.properties.get("light_intensity");

            if (emit != null || intensity != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("structure_light");
                @SuppressWarnings("unchecked")
                KeyframeChannel<StructureLightSettings> merged = mergedAny != null
                    ? (KeyframeChannel<StructureLightSettings>) mergedAny
                    : new KeyframeChannel<>("structure_light", KeyframeFactories.STRUCTURE_LIGHT_SETTINGS);

                if (mergedAny == null)
                {
                    this.properties.put("structure_light", merged);
                    this.add(merged);
                }

                TreeSet<Float> ticks = new TreeSet<>();
                if (emit != null) for (Object kfObj : emit.getKeyframes()) { ticks.add(((Keyframe<?>) kfObj).getTick()); }
                if (intensity != null) for (Object kfObj : intensity.getKeyframes()) { ticks.add(((Keyframe<?>) kfObj).getTick()); }

                for (float t : ticks)
                {
                    boolean enabled = false;
                    int value = 0;

                    if (emit != null)
                    {
                        KeyframeSegment seg = emit.find(t);
                        if (seg != null)
                        {
                            Object v = seg.createInterpolated();
                            if (v instanceof Boolean b) enabled = b;
                            else if (v instanceof Number n) enabled = n.floatValue() >= 0.5F;
                        }
                    }

                    if (intensity != null)
                    {
                        KeyframeSegment seg = intensity.find(t);
                        if (seg != null)
                        {
                            Object v = seg.createInterpolated();
                            if (v instanceof Number n) value = Math.round(n.floatValue());
                        }
                    }

                    StructureLightSettings payload = new StructureLightSettings(
                        enabled,
                        Math.max(0, Math.min(15, value))
                    );

                    merged.insert(t, payload);
                }
            }
        }
        catch (Throwable ignored) {}
    }

    @Override
    protected boolean canPersist(BaseValue value)
    {
        if (value instanceof KeyframeChannel<?> channel)
        {
            return !channel.isEmpty();
        }

        return super.canPersist(value);
    }
}