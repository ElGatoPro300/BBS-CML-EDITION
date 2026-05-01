package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.repos.IRepository;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

public class ModelRepository implements IRepository<ModelConfig>
{
    private ModelManager manager;

    public ModelRepository(ModelManager manager)
    {
        this.manager = manager;
    }

    @Override
    public ModelConfig create(String id, MapType data)
    {
        return null;
    }

    @Override
    public void load(String id, Consumer<ModelConfig> callback)
    {
        ModelInstance model = this.manager.loadModel(id);

        if (model != null)
        {
            ModelConfig config = new ModelConfig(id);

            config.fromData(model.toConfig());
            callback.accept(config);
        }
        else
        {
            callback.accept(null);
        }
    }

    @Override
    public void save(String id, MapType data)
    {
        this.manager.saveConfig(id, data);
        this.manager.loadModel(id);
    }

    @Override
    public void rename(String id, String name)
    {
        this.manager.renameModel(id, name);
    }

    @Override
    public void delete(String id)
    {
        File folder = BBSMod.getProvider().getFile(Link.assets(ModelManager.MODELS_PREFIX + id));

        if (folder != null && folder.exists())
        {
            IOUtils.deleteFolder(folder);
        }
    }

    @Override
    public void requestKeys(Consumer<Collection<String>> callback)
    {
        callback.accept(this.manager.getAvailableKeys());
    }

    @Override
    public File getFolder()
    {
        return BBSMod.getProvider().getFile(Link.assets("models"));
    }

    @Override
    public void addFolder(String path, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        boolean result = folder.mkdirs();

        if (callback != null)
        {
            callback.accept(result);
        }
    }

    @Override
    public void renameFolder(String path, String name, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        File newFolder = new File(this.getFolder(), name);
        boolean result = folder.renameTo(newFolder);

        if (callback != null)
        {
            callback.accept(result);
        }
    }

    @Override
    public void deleteFolder(String path, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        
        if (folder.exists())
        {
            IOUtils.deleteFolder(folder);
        }

        boolean result = !folder.exists();

        if (callback != null)
        {
            callback.accept(result);
        }
    }
}
