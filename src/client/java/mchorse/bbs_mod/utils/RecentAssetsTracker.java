package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.ContentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class RecentAssetsTracker
{
    public static final List<Entry> RECENT = new ArrayList<>();
    private static final int MAX_SIZE = 10;

    public static void add(ContentType type, String id)
    {
        if (type == null || id == null || id.isEmpty())
        {
            return;
        }

        RECENT.removeIf(e -> e.type == type && e.id.equals(id));
        RECENT.add(0, new Entry(type, id));
        if (RECENT.size() > MAX_SIZE)
        {
            RECENT.remove(RECENT.size() - 1);
        }

        save();
    }

    public static void load()
    {
        File file = BBSMod.getSettingsPath("recent_assets.dat");

        if (!file.exists())
        {
            return;
        }

        try (InputStream stream = new FileInputStream(file))
        {
            BaseType type = DataStorage.readFromStream(stream);

            if (type != null && type.isList())
            {
                RECENT.clear();
                for (BaseType entry : type.asList())
                {
                    if (entry.isMap())
                    {
                        MapType map = entry.asMap();
                        String typeId = map.getString("type");
                        String id = map.getString("id");

                        ContentType contentType = ContentType.fromId(typeId);
                        if (contentType != null)
                        {
                            RECENT.add(new Entry(contentType, id));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void save()
    {
        File file = BBSMod.getSettingsPath("recent_assets.dat");
        ListType list = new ListType();

        for (Entry entry : RECENT)
        {
            MapType map = new MapType();
            map.putString("type", entry.type.getId());
            map.putString("id", entry.id);
            list.add(map);
        }

        try (OutputStream stream = new FileOutputStream(file))
        {
            DataStorage.writeToStream(stream, list);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static class Entry
    {
        public ContentType type;
        public String id;

        public Entry(ContentType type, String id)
        {
            this.type = type;
            this.id = id;
        }
    }
}
