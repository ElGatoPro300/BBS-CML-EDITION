package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import net.minecraft.entity.LivingEntity;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Replay extends ValueGroup
{
    public final ValueForm form = new ValueForm("form");
    public final ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public final FormProperties properties = new FormProperties("properties");
    public final Clips actions = new Clips("actions", BBSMod.getFactoryActionClips());
    public final Inventory inventory = new Inventory("inventory");

    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString label = new ValueString("label", "");
    public final ValueString nameTag = new ValueString("name_tag", "");
    public final ValueString group = new ValueString("group", "");
    public final ValueBoolean shadow = new ValueBoolean("shadow", true);
    public final ValueFloat shadowSize = new ValueFloat("shadow_size", 0.5F);
    public final ValueFloat shadowOpacity = new ValueFloat("shadow_opacity", 1F, 0F, 1F);
    public final ValueInt looping = new ValueInt("looping", 0);

    public final ValueBoolean actor = new ValueBoolean("actor", false);
    public final ValueBoolean fp = new ValueBoolean("fp", false);
    public final ValueBoolean relative = new ValueBoolean("relative", false);
    public final ValuePoint relativeOffset = new ValuePoint("relativeOffset", new Point(0, 0, 0));

    private final Map<String, String> customSheetTitles = new HashMap<>();
    private final Map<String, String> anchoredBones = new HashMap<>();
    private final Map<String, Integer> sheetColors = new HashMap<>();
    public final ValueBoolean axesPreview = new ValueBoolean("axes_preview", false);
    public final ValueString axesPreviewBone = new ValueString("axes_preview_bone", "");
    public final ValueBoolean isGroup = new ValueBoolean("is_group", false);
    public final ValueString uuid = new ValueString("uuid", "");

    /* Item drop velocity configuration */
    public final ValueFloat dropVelocityMinX = new ValueFloat("drop_velocity_min_x", -0.1F);
    public final ValueFloat dropVelocityMaxX = new ValueFloat("drop_velocity_max_x", 0.1F);
    public final ValueFloat dropVelocityMinY = new ValueFloat("drop_velocity_min_y", 0.1F);
    public final ValueFloat dropVelocityMaxY = new ValueFloat("drop_velocity_max_y", 0.25F);
    public final ValueFloat dropVelocityMinZ = new ValueFloat("drop_velocity_min_z", -0.1F);
    public final ValueFloat dropVelocityMaxZ = new ValueFloat("drop_velocity_max_z", 0.1F);

    public Replay(String id)
    {
        super(id);

        this.add(this.form);
        this.add(this.keyframes);
        this.add(this.properties);
        this.add(this.actions);
        this.add(this.inventory);

        this.add(this.enabled);
        this.add(this.label);
        this.add(this.nameTag);
        this.add(this.group);
        this.add(this.shadow);
        this.add(this.shadowSize);
        this.add(this.shadowOpacity);
        this.add(this.looping);

        this.add(this.actor);
        this.add(this.fp);
        this.add(this.relative);
        this.add(this.relativeOffset);

        this.add(this.axesPreview);
        this.add(this.axesPreviewBone);
        this.add(this.dropVelocityMinX);
        this.add(this.dropVelocityMaxX);
        this.add(this.dropVelocityMinY);
        this.add(this.dropVelocityMaxY);
        this.add(this.dropVelocityMinZ);
        this.add(this.dropVelocityMaxZ);
        this.add(this.isGroup);
        this.add(this.uuid);
        
        this.uuid.set(java.util.UUID.randomUUID().toString());
    }

    public String getName()
    {
        if (this.isGroup.get())
        {
            return this.label.get().isEmpty() ? "New Group" : this.label.get();
        }

        String label = this.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = this.form.get();

        if (form == null)
        {
            return "-";
        }

        return form.getDisplayName();
    }

    public void shift(float tick)
    {
        this.keyframes.shift(tick);
        this.properties.shift(tick);
        this.actions.shift(tick);
    }

    public void applyActions(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, int tick)
    {
        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            ((ActionClip) clip).apply(actor, fakePlayer, film, this, tick);
        }
    }

    public void applyClientActions(int tick, IEntity entity, Film film)
    {
        tick = this.getTick(tick);

        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            if (clip instanceof ActionClip actionClip && actionClip.isClient())
            {
                actionClip.applyClient(entity, film, this, tick);
            }
        }
    }

    public int getTick(int tick)
    {
        return this.looping.get() > 0 ? tick % this.looping.get() : tick;
    }

    public String getCustomSheetTitle(String id)
    {
        return this.customSheetTitles.get(id);
    }

    public void setCustomSheetTitle(String id, String title)
    {
        if (title == null || title.isBlank())
        {
            this.customSheetTitles.remove(id);
        }
        else
        {
            this.customSheetTitles.put(id, title);
        }
    }

    public String getAnchoredBone(String id)
    {
        return this.anchoredBones.get(id);
    }

    public void setAnchoredBone(String id, String bone)
    {
        if (bone == null || bone.isBlank())
        {
            this.anchoredBones.remove(id);
        }
        else
        {
            this.anchoredBones.put(id, bone);
        }
    }

    public Integer getSheetColor(String id)
    {
        return this.sheetColors.get(id);
    }

    public void setSheetColor(String id, Integer color)
    {
        if (color == null)
        {
            this.sheetColors.remove(id);
        }
        else
        {
            this.sheetColors.put(id, color);
        }
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        super.copy(group);

        if (group instanceof Replay other)
        {
            this.customSheetTitles.clear();
            this.customSheetTitles.putAll(other.customSheetTitles);
            this.anchoredBones.clear();
            this.anchoredBones.putAll(other.anchoredBones);
            this.sheetColors.clear();
            this.sheetColors.putAll(other.sheetColors);
        }
    }

    @Override
    public BaseType toData()
    {
        MapType map = (MapType) super.toData();

        if (!this.customSheetTitles.isEmpty())
        {
            MapType titles = new MapType();

            for (Map.Entry<String, String> entry : this.customSheetTitles.entrySet())
            {
                titles.put(entry.getKey(), new mchorse.bbs_mod.data.types.StringType(entry.getValue()));
            }

            map.put("custom_sheet_titles", titles);
        }

        if (!this.anchoredBones.isEmpty())
        {
            MapType anchored = new MapType();

            for (Map.Entry<String, String> entry : this.anchoredBones.entrySet())
            {
                anchored.put(entry.getKey(), new mchorse.bbs_mod.data.types.StringType(entry.getValue()));
            }

            map.put("anchored_bones", anchored);
        }

        if (!this.sheetColors.isEmpty())
        {
            MapType colors = new MapType();

            for (Map.Entry<String, Integer> entry : this.sheetColors.entrySet())
            {
                colors.put(entry.getKey(), new mchorse.bbs_mod.data.types.IntType(entry.getValue()));
            }

            map.put("sheet_colors", colors);
        }

        return map;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data instanceof MapType map)
        {
            BaseType titlesType = map.get("custom_sheet_titles");

            if (titlesType instanceof MapType titles)
            {
                for (String key : titles.keys())
                {
                    BaseType value = titles.get(key);

                    if (value != null && value.isString())
                    {
                        this.customSheetTitles.put(key, value.asString());
                    }
                }
            }

            BaseType anchoredType = map.get("anchored_bones");

            if (anchoredType instanceof MapType anchored)
            {
                for (String key : anchored.keys())
                {
                    BaseType value = anchored.get(key);

                    if (value != null && value.isString())
                    {
                        this.anchoredBones.put(key, value.asString());
                    }
                }
            }

            BaseType colorsType = map.get("sheet_colors");

            if (colorsType instanceof MapType colors)
            {
                for (String key : colors.keys())
                {
                    BaseType value = colors.get(key);

                    if (value != null && value.isNumeric())
                    {
                        this.sheetColors.put(key, value.asNumeric().intValue());
                    }
                }
            }
        }

        this.ensureShadowKeyframes();
    }

    private void ensureShadowKeyframes()
    {
        if (this.keyframes.shadowSize.isEmpty())
        {
            this.keyframes.shadowSize.insert(0, 0.5D);
        }

        if (this.keyframes.shadowOpacity.isEmpty())
        {
            this.keyframes.shadowOpacity.insert(0, 1D);
        }
    }
}
