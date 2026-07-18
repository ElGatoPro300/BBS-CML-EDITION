package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
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

                channel.setModel(true);
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
        if (property instanceof BaseKeyframeFactoryValue<?> keyframeFactoryValue)
        {
            String key = FormUtils.getPropertyPath(property);
            boolean allowed = property.isVisible() || FormUtils.isRenderPropertyPath(key);

            if (allowed)
            {
                KeyframeChannel channel = new KeyframeChannel(key, keyframeFactoryValue.getFactory());

                channel.setModel(true);
                this.properties.put(key, channel);
                this.add(channel);

                return channel;
            }
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
                        poseTransform.pivot.add(transform.pivot.x * blend, transform.pivot.y * blend, transform.pivot.z * blend);
                    }
                    else
                    {
                        poseTransform.translate.add(transform.translate);
                        poseTransform.scale.mul(transform.scale);
                        poseTransform.rotate.add(transform.rotate);
                        poseTransform.rotate2.add(transform.rotate2);
                        poseTransform.pivot.add(transform.pivot);
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
                            poseTransform.paintColor.r = Lerps.lerp(poseTransform.paintColor.r, sourcePose.paintColor.r, blend);
                            poseTransform.paintColor.g = Lerps.lerp(poseTransform.paintColor.g, sourcePose.paintColor.g, blend);
                            poseTransform.paintColor.b = Lerps.lerp(poseTransform.paintColor.b, sourcePose.paintColor.b, blend);
                            poseTransform.paintColor.a = Lerps.lerp(poseTransform.paintColor.a, sourcePose.paintColor.a, blend);
                            poseTransform.glowingColor.r = Lerps.lerp(poseTransform.glowingColor.r, sourcePose.glowingColor.r, blend);
                            poseTransform.glowingColor.g = Lerps.lerp(poseTransform.glowingColor.g, sourcePose.glowingColor.g, blend);
                            poseTransform.glowingColor.b = Lerps.lerp(poseTransform.glowingColor.b, sourcePose.glowingColor.b, blend);
                            poseTransform.glowingColor.a = 1F;
                            poseTransform.glowIntensity = Lerps.lerp(poseTransform.glowIntensity, sourcePose.glowIntensity, blend);
                            poseTransform.glowRadius = Lerps.lerp(poseTransform.glowRadius, sourcePose.glowRadius, blend);
                            poseTransform.lighting = Lerps.lerp(poseTransform.lighting, sourcePose.lighting, blend);
                            poseTransform.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseTransform.paintColor.a);
                        }
                        else
                        {
                            poseTransform.fix = sourcePose.fix;
                            poseTransform.color.copy(sourcePose.color);
                            poseTransform.paintColor.copy(sourcePose.paintColor);
                            poseTransform.glowingColor.copy(sourcePose.glowingColor);
                            poseTransform.glowingColor.a = 1F;
                            poseTransform.glowIntensity = sourcePose.glowIntensity;
                            poseTransform.glowRadius = sourcePose.glowRadius;
                            poseTransform.lighting = sourcePose.lighting;
                            poseTransform.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseTransform.paintColor.a);
                        }

                        this.applyPoseBoneTexture(poseTransform, segment, transform);
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

        if (FormUtils.isRenderPropertyPath(id) && value.getFactory() == KeyframeFactories.BOOLEAN)
        {
            @SuppressWarnings("unchecked")
            KeyframeChannel<Boolean> render = (KeyframeChannel<Boolean>) value;

            this.applyRenderProperty(tick, property, render);

            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            if ("texture".equals(id))
            {
                this.applyTextureBlend(form, segment);
            }

            if (id.startsWith("illusion"))
            {
                this.applyIllusionTextureBlend(form, segment);
            }

            if ("color".equals(id))
            {
                form.noshadingOpacity.setRuntimeValue(segment.getClosest().isNoshadingOpacity());
            }

            if (blend < 1F)
            {
                IKeyframeFactory factory = value.getFactory();
                Object v = factory.copy(property.get());
                Object a = factory.copy(segment.createInterpolated());
                Object interpolated = factory.interpolate(v, v, a, a, Interpolations.LINEAR, MathUtils.clamp(blend, 0F, 1F));

                property.setRuntimeValue(coerceRuntimeValue(property, factory.copy(interpolated)));
            }
            else
            {
                property.setRuntimeValue(coerceRuntimeValue(property, segment.createInterpolated()));
            }
        }
        else
        {
            if ("texture".equals(id))
            {
                form.textureBlend = null;
            }

            if (id.startsWith("illusion"))
            {
                form.illusionTextureBlend = null;
            }

            if ("color".equals(id))
            {
                form.noshadingOpacity.setRuntimeValue(null);
            }

            property.setRuntimeValue(null);
        }
    }

    private void applyRenderProperty(float tick, BaseValueBasic property, KeyframeChannel<Boolean> channel)
    {
        if (channel.isEmpty())
        {
            property.setRuntimeValue(null);

            return;
        }

        Keyframe<Boolean> first = channel.get(0);

        if (first != null && tick < first.getTick())
        {
            property.setRuntimeValue(null);

            return;
        }

        property.setRuntimeValue(channel.interpolate(tick, true));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object coerceRuntimeValue(BaseValueBasic property, Object value)
    {
        if (property instanceof ValueBoolean)
        {
            if (value instanceof Boolean)
            {
                return value;
            }

            if (value instanceof Number)
            {
                return ((Number) value).floatValue() > 0.001F;
            }

            return value != null;
        }

        return value;
    }

    private void applyTextureBlend(Form form, KeyframeSegment segment)
    {
        if (segment.a.isBend() && !segment.isSame())
        {
            float blendFactor = getSegmentBlendFactor(segment);
            Link from = LinkUtils.copy((Link) segment.a.getValue());
            Link to = LinkUtils.copy((Link) segment.b.getValue());

            if (form.textureBlend == null)
            {
                form.textureBlend = new TextureBlend();
            }

            form.textureBlend.from = from;
            form.textureBlend.to = to;
            form.textureBlend.blend = blendFactor;
        }
        else
        {
            form.textureBlend = null;
        }
    }

    private void applyIllusionTextureBlend(Form form, KeyframeSegment segment)
    {
        if (segment.a.isBend() && !segment.isSame())
        {
            Illusion fromIllusion = (Illusion) segment.a.getValue();
            Illusion toIllusion = (Illusion) segment.b.getValue();
            Link from = this.getIllusionPrimaryTexture(fromIllusion);
            Link to = this.getIllusionPrimaryTexture(toIllusion);

            if (from != null && to != null && !from.equals(to))
            {
                float blendFactor = getSegmentBlendFactor(segment);

                if (form.illusionTextureBlend == null)
                {
                    form.illusionTextureBlend = new TextureBlend();
                }

                form.illusionTextureBlend.from = LinkUtils.copy(from);
                form.illusionTextureBlend.to = LinkUtils.copy(to);
                form.illusionTextureBlend.blend = blendFactor;

                return;
            }
        }

        form.illusionTextureBlend = null;
    }

    private Link getIllusionPrimaryTexture(Illusion illusion)
    {
        if (illusion == null || illusion.textures.isEmpty())
        {
            return null;
        }

        return illusion.textures.get(0);
    }

    private static float getSegmentBlendFactor(KeyframeSegment segment)
    {
        return (float) segment.a.getInterpolation().interpolate(0D, 1D, segment.x);
    }

    private static PoseTransform getKeyframeBoneTransform(Keyframe<?> keyframe, String boneName)
    {
        if (keyframe == null)
        {
            return null;
        }

        Object value = keyframe.getValue();

        if (value instanceof PoseTransform poseTransform)
        {
            return poseTransform;
        }

        if (value instanceof Pose pose)
        {
            return pose.get(boneName);
        }

        return null;
    }

    /**
     * Applies bone texture crossfade for pose overlay limb tracks, matching the
     * form-level texture timeline blend (from fades out, to fades in).
     */
    private void applyPoseBoneTexture(PoseTransform target, KeyframeSegment segment, Transform transform)
    {
        if (segment.a.isBend() && !segment.isSame()
            && segment.a.getValue() instanceof PoseTransform aPose
            && segment.b.getValue() instanceof PoseTransform bPose
            && aPose.texture != null && bPose.texture != null
            && !aPose.texture.equals(bPose.texture))
        {
            target.texture = LinkUtils.copy(aPose.texture);
            target.textureBlendTo = LinkUtils.copy(bPose.texture);
            target.textureBlend = getSegmentBlendFactor(segment);

            return;
        }

        if (transform instanceof PoseTransform pose)
        {
            PoseTransform pick = pose;

            if (segment.a.getValue() instanceof PoseTransform aPose)
            {
                pick = aPose;

                if (segment.x >= 1F && segment.b.getValue() instanceof PoseTransform bPose)
                {
                    pick = bPose;
                }
            }

            target.texture = pick.texture != null ? LinkUtils.copy(pick.texture) : null;
            target.textureBlendTo = pose.textureBlendTo != null ? LinkUtils.copy(pose.textureBlendTo) : null;
            target.textureBlend = pose.textureBlend;
        }
    }

    public void resetProperties(Form form)
    {
        if (form == null)
        {
            return;
        }

        form.textureBlend = null;
        form.illusionTextureBlend = null;

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

            property.setModel(true);
            property.fromData(mapType);

            /* Patch 1.1.1 changes to lighting property */
            if (key.endsWith("lighting") && property.getFactory() == KeyframeFactories.BOOLEAN)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.FLOAT);

                newProperty.setModel(true);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Boolean v = (Boolean) kf.getValue();

                    newProperty.insert(kf.getTick(), v ? 1F : 0F);
                }

                property = newProperty;
            }

            /* shaderShadow was briefly a float; migrate float keyframes back to boolean */
            if (key.equals("shaderShadow") && property.getFactory() == KeyframeFactories.FLOAT)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.BOOLEAN);

                newProperty.setModel(true);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Object raw = kf.getValue();
                    float f = raw instanceof Number ? ((Number) raw).floatValue() : 0F;

                    newProperty.insert(kf.getTick(), f > 0.001F);
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
                    merged.setModel(true);
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

        /* Migration: rename glow_settings -> glow, merge glowing_color, synthesize from glow_intensity */
        try
        {
            KeyframeChannel<?> renamed = this.properties.remove("glow_settings");

            if (renamed != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("glow");
                @SuppressWarnings("unchecked")
                KeyframeChannel<GlowSettings> merged = mergedAny != null
                    ? (KeyframeChannel<GlowSettings>) mergedAny
                    : new KeyframeChannel<>("glow", KeyframeFactories.GLOW_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("glow", merged);
                    this.add(merged);
                }

                for (Object kfObj : renamed.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    Object v = kf.getValue();

                    if (v instanceof GlowSettings settings)
                    {
                        merged.insert(kf.getTick(), settings.copy());
                    }
                }

                this.remove(renamed);
            }

            KeyframeChannel<?> glowingColorChannel = this.properties.remove("glowing_color");

            if (glowingColorChannel != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("glow");
                @SuppressWarnings("unchecked")
                KeyframeChannel<GlowSettings> merged = mergedAny != null
                    ? (KeyframeChannel<GlowSettings>) mergedAny
                    : new KeyframeChannel<>("glow", KeyframeFactories.GLOW_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("glow", merged);
                    this.add(merged);
                }

                for (Object kfObj : glowingColorChannel.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    float t = kf.getTick();
                    GlowSettings settings = this.getGlowSettingsAt(merged, t);
                    Object v = kf.getValue();

                    if (v instanceof Color color)
                    {
                        settings.r = color.r;
                        settings.g = color.g;
                        settings.b = color.b;
                    }

                    merged.insert(t, settings);
                }

                this.remove(glowingColorChannel);
            }

            KeyframeChannel<?> legacyGlow = this.properties.get("glow_intensity");

            if (legacyGlow != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("glow");
                @SuppressWarnings("unchecked")
                KeyframeChannel<GlowSettings> merged = mergedAny != null
                    ? (KeyframeChannel<GlowSettings>) mergedAny
                    : new KeyframeChannel<>("glow", KeyframeFactories.GLOW_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("glow", merged);
                    this.add(merged);
                }

                for (Object kfObj : legacyGlow.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    float t = kf.getTick();
                    float intensity = 0F;
                    Object v = kf.getValue();

                    if (v instanceof Number n)
                    {
                        intensity = n.floatValue();
                    }

                    GlowSettings settings = this.getGlowSettingsAt(merged, t);

                    settings.intensity = intensity;
                    merged.insert(t, settings);
                }
            }
        }
        catch (Throwable ignored) {}

        /* Migration: paint_color -> paint (PaintSettings) */
        try
        {
            KeyframeChannel<?> paintColorChannel = this.properties.remove("paint_color");

            if (paintColorChannel != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("paint");
                @SuppressWarnings("unchecked")
                KeyframeChannel<PaintSettings> merged = mergedAny != null
                    ? (KeyframeChannel<PaintSettings>) mergedAny
                    : new KeyframeChannel<>("paint", KeyframeFactories.PAINT_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("paint", merged);
                    this.add(merged);
                }

                for (Object kfObj : paintColorChannel.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    float t = kf.getTick();
                    PaintSettings settings = this.getPaintSettingsAt(merged, t);
                    Object v = kf.getValue();

                    if (v instanceof Color color)
                    {
                        settings.r = color.r;
                        settings.g = color.g;
                        settings.b = color.b;
                        settings.intensity = color.a;
                    }

                    merged.insert(t, settings);
                }

                this.remove(paintColorChannel);
            }
        }
        catch (Throwable ignored) {}

        /* Migration: copy legacy visible render-gating keyframes into render while keeping visible */
        try
        {
            KeyframeChannel<?> legacyVisible = this.properties.get("visible");
            KeyframeChannel<?> renderChannel = this.properties.get("render");

            if (renderChannel == null && legacyVisible != null && legacyVisible.getFactory() == KeyframeFactories.BOOLEAN && !legacyVisible.isEmpty())
            {
                KeyframeChannel<Boolean> render = new KeyframeChannel<>("render", KeyframeFactories.BOOLEAN);

                render.setModel(true);

                for (Object kfObj : legacyVisible.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;

                    render.insert(kf.getTick(), (Boolean) kf.getValue());
                }

                this.properties.put("render", render);
                this.add(render);
            }
        }
        catch (Throwable ignored) {}
    }

    private PaintSettings getPaintSettingsAt(KeyframeChannel<PaintSettings> channel, float tick)
    {
        KeyframeSegment segment = channel.find(tick);
        PaintSettings settings;

        if (segment != null)
        {
            Object interpolated = segment.createInterpolated();

            settings = interpolated instanceof PaintSettings paint ? paint.copy() : new PaintSettings();
        }
        else
        {
            settings = new PaintSettings();
        }

        return settings;
    }

    private GlowSettings getGlowSettingsAt(KeyframeChannel<GlowSettings> channel, float tick)
    {
        KeyframeSegment segment = channel.find(tick);
        GlowSettings settings;

        if (segment != null)
        {
            Object interpolated = segment.createInterpolated();

            settings = interpolated instanceof GlowSettings glow ? glow.copy() : new GlowSettings();
        }
        else
        {
            settings = new GlowSettings();
        }

        return settings;
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