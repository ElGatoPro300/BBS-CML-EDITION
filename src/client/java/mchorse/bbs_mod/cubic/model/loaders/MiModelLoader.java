package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.Collection;

public class MiModelLoader implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        System.out.println("[MiModelLoader] Loading model: " + id);
        Link miLink = null;

        for (Link l : links)
        {
            if (l.path.endsWith(".mimodel"))
            {
                miLink = l;
                break;
            }
        }

        if (miLink == null)
        {
            System.err.println("[MiModelLoader] No .mimodel file found in links for: " + id);
            return null;
        }

        System.out.println("[MiModelLoader] Found .mimodel file: " + miLink.toString());

        try (InputStream stream = models.provider.getAsset(miLink))
        {
            String text = IOUtils.readText(stream);
            BaseType data = DataToString.fromString(text);

            if (data.isMap())
            {
                MapType map = data.asMap();
                Model cubicModel = new Model(models.parser);
                
                // Parse texture size
                if (map.has("texture_size"))
                {
                    ListType size = map.getList("texture_size");
                    if (size.size() >= 2)
                    {
                        cubicModel.textureWidth = size.getInt(0);
                        cubicModel.textureHeight = size.getInt(1);
                        System.out.println("[MiModelLoader] Texture size: " + cubicModel.textureWidth + "x" + cubicModel.textureHeight);
                    }
                }
                
                // Parse parts
                if (map.has("parts"))
                {
                    for (BaseType part : map.getList("parts"))
                    {
                        if (part.isMap())
                        {
                            ModelGroup group = this.parseGroup(part.asMap(), cubicModel, null);
                            if (group != null)
                            {
                                cubicModel.topGroups.add(group);
                            }
                        }
                    }
                }

                if (cubicModel.topGroups.isEmpty()) {
                    System.err.println("[MiModelLoader] WARNING: No top-level groups loaded! Model will be invisible.");
                } else {
                    System.out.println("[MiModelLoader] Loaded " + cubicModel.topGroups.size() + " top-level groups.");
                }
                
                cubicModel.initialize();
                
                // Find texture
                Link texture = null;
                if (map.has("texture"))
                {
                    String textureName = map.getString("texture");
                    System.out.println("[MiModelLoader] Looking for texture: " + textureName);
                    // Try to find matching png in links
                    for (Link l : links)
                    {
                        if (l.path.endsWith(textureName))
                        {
                            texture = l;
                            System.out.println("[MiModelLoader] Found texture: " + l.toString());
                            break;
                        }
                    }
                }
                
                // Fallback: Try to find ANY png if explicit texture is missing
                if (texture == null) {
                    System.out.println("[MiModelLoader] Explicit texture not found. Searching for any PNG...");
                    for (Link l : links) {
                        if (l.path.endsWith(".png")) {
                            texture = l;
                            System.out.println("[MiModelLoader] Using fallback texture: " + l.toString());
                            break;
                        }
                    }
                }

                if (texture == null) {
                     System.err.println("[MiModelLoader] CRITICAL: No texture found for model " + id + ". It might render black or invisible.");
                }
                
                ModelInstance instance = new ModelInstance(id, cubicModel, new Animations(models.parser), texture);
                instance.applyConfig(config);
                
                return instance;
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to load .mimodel: " + miLink);
            e.printStackTrace();
        }

        return null;
    }

    private Vector3f swapYZ(Vector3f v)
    {
        return new Vector3f(v.x, v.z, v.y);
    }

    private ModelGroup parseGroup(MapType data, Model model, ModelGroup parent)
    {
        String name = data.getString("name");
        System.out.println("[MiModelLoader] Parsing group: " + name);
        ModelGroup group = new ModelGroup(name);
        
        // Position
        if (data.has("position"))
        {
            Vector3f pos = DataStorageUtils.vector3fFromData(data.getList("position"));
            // Swap Y and Z for Mine-imator -> Minecraft conversion
            group.initial.translate.set(this.swapYZ(pos));
            // In Mine-imator, position is the pivot
            group.initial.pivot.set(group.initial.translate); 
        }
        
        // Rotation
        if (data.has("rotation"))
        {
             Vector3f rot = DataStorageUtils.vector3fFromData(data.getList("rotation"));
             // Swap Y and Z for rotation as well (Euler angles)
             group.initial.rotate.set(this.swapYZ(rot));
        }
        
        // Shapes (Cubes)
        if (data.has("shapes"))
        {
            for (BaseType shapeData : data.getList("shapes"))
            {
                if (shapeData.isMap())
                {
                    ModelCube cube = this.parseCube(shapeData.asMap());
                    if (cube != null)
                    {
                        group.cubes.add(cube);
                    }
                }
            }
        }
        
        // Children parts
        if (data.has("parts"))
        {
            for (BaseType part : data.getList("parts"))
            {
                if (part.isMap())
                {
                    ModelGroup child = this.parseGroup(part.asMap(), model, group);
                    if (child != null)
                    {
                        group.children.add(child);
                    }
                }
            }
        }
        
        return group;
    }

    private ModelCube parseCube(MapType data)
    {
        String type = data.has("type") ? data.getString("type") : "unknown";
        // System.out.println("[MiModelLoader] Parsing cube type: " + type);

        ModelCube cube = new ModelCube();
        
        // From/To -> Origin/Size
        Vector3f from = new Vector3f();
        Vector3f to = new Vector3f();
        
        if (data.has("from")) from.set(DataStorageUtils.vector3fFromData(data.getList("from")));
        if (data.has("to")) to.set(DataStorageUtils.vector3fFromData(data.getList("to")));
        
        // Inflate (Mine-imator feature)
        float inflate = data.has("inflate") ? data.getFloat("inflate") : 0;
        if (inflate != 0)
        {
            from.sub(inflate, inflate, inflate);
            to.add(inflate, inflate, inflate);
        }

        // Apply local scale (Shape scale)
        if (data.has("scale"))
        {
            Vector3f scale = DataStorageUtils.vector3fFromData(data.getList("scale"));
            // Apply scale to the bounds (relative to what? usually 0,0,0 if from/to are absolute coords)
            // But wait, model_load_shape applies it to (from-inflate).
            // So we scale the coordinates.
            from.mul(scale);
            to.mul(scale);
        }

        // Swap Y/Z for coords
        from = this.swapYZ(from);
        to = this.swapYZ(to);

        // Ensure from < to for size calculation
        Vector3f min = new Vector3f(Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z));
        Vector3f max = new Vector3f(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z));
        
        cube.origin.set(min);
        cube.size.set(max).sub(min);

        // Debug log for suspicious sizes
        if (cube.size.lengthSquared() < 0.0001f) {
             System.out.println("[MiModelLoader] WARNING: Cube has zero size! " + cube.size + " (Original from: " + from + ", to: " + to + ")");
        } else {
             // System.out.println("[MiModelLoader] Cube created. Origin: " + cube.origin + ", Size: " + cube.size);
        }
        
        // Position offset (swapped)
        if (data.has("position")) 
        {
            Vector3f pos = DataStorageUtils.vector3fFromData(data.getList("position"));
            cube.origin.add(this.swapYZ(pos));
        }
        
        // Rotation (swapped)
        if (data.has("rotation"))
        {
            cube.rotate.set(this.swapYZ(DataStorageUtils.vector3fFromData(data.getList("rotation"))));
        }
        
        // UV setup BEFORE inflating for planes to ensure correct mapping
        if (data.has("uv"))
        {
            ListType uvList = data.getList("uv");
            if (uvList.size() >= 2)
            {
                Vector2f uv = new Vector2f(uvList.getFloat(0), uvList.getFloat(1));
                cube.setupBoxUV(uv, false);
            }
        }

        // Enforce minimum thickness for planes (after swapping, Y became Z or Y?)
        // If type is plane, in MI it's usually flat on one axis.
        if (type.equals("plane"))
        {
             float minThickness = 0.01f;
             if (Math.abs(cube.size.x) < minThickness) cube.size.x = minThickness;
             if (Math.abs(cube.size.y) < minThickness) cube.size.y = minThickness;
             if (Math.abs(cube.size.z) < minThickness) cube.size.z = minThickness;
        }
        
        return cube;
    }
}
