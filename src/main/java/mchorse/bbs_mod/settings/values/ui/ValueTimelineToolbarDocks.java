package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.toolbar.TimelineToolbarDock;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists per-timeline toolbar dock positions (camera, action, replay panels).
 */
public class ValueTimelineToolbarDocks extends BaseValue
{
    private final Map<String, String> docks = new LinkedHashMap<>();

    public ValueTimelineToolbarDocks(String id)
    {
        super(id);
    }

    public TimelineToolbarDock getDock(String panelId)
    {
        return TimelineToolbarDock.fromId(this.docks.get(panelId));
    }

    public void setDock(String panelId, TimelineToolbarDock dock)
    {
        if (panelId == null || panelId.isEmpty() || dock == null)
        {
            return;
        }

        BaseValue.edit(this, (v) -> this.docks.put(panelId, dock.toId()));
    }

    @Override
    public BaseType toData()
    {
        MapType map = new MapType();

        for (Map.Entry<String, String> entry : this.docks.entrySet())
        {
            map.putString(entry.getKey(), entry.getValue());
        }

        return map;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.docks.clear();

        if (!data.isMap())
        {
            return;
        }

        for (String key : data.asMap().keys())
        {
            BaseType value = data.asMap().get(key);

            if (value.isString())
            {
                this.docks.put(key, value.asString());
            }
        }
    }
}
