package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class BBSShaders
{
    public static final List<Runnable> LOADERS = new ArrayList<>();

    private static GlProgram model;
    private static GlProgram multiLink;
    private static GlProgram subtitles;

    private static GlProgram pickerPreview;
    private static GlProgram pickerBillboard;
    private static GlProgram pickerBillboardNoShading;
    private static GlProgram pickerParticles;
    private static GlProgram pickerModels;
    
    public static RenderPipeline MODEL_PIPELINE;
    public static RenderPipeline MULTILINK_PIPELINE;
    public static RenderPipeline SUBTITLES_PIPELINE;

    static
    {
        setup();
    }

    public static void setup()
    {
        if (model != null) model.close();
        if (subtitles != null) subtitles.close();
        if (subtitles != null) subtitles.close();

        if (pickerPreview != null) pickerPreview.close();
        if (pickerBillboard != null) pickerBillboard.close();
        if (pickerBillboardNoShading != null) pickerBillboardNoShading.close();
        if (pickerParticles != null) pickerParticles.close();
        if (pickerModels != null) pickerModels.close();

        ShaderManager loader = Minecraft.getInstance().getShaderManager();

        model = loadProgram(loader, "core/model", DefaultVertexFormat.NEW_ENTITY);
        multiLink = loadProgram(loader, "core/multilink", DefaultVertexFormat.POSITION_TEX_COLOR);
        subtitles = loadProgram(loader, "core/subtitles", DefaultVertexFormat.POSITION_TEX_COLOR);

        pickerPreview = loadProgram(loader, "core/picker_preview", DefaultVertexFormat.POSITION_TEX_COLOR);
        pickerBillboard = loadProgram(loader, "core/picker_billboard", DefaultVertexFormat.NEW_ENTITY);
        pickerBillboardNoShading = loadProgram(loader, "core/picker_billboard_no_shading", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR);
        pickerParticles = loadProgram(loader, "core/picker_particles", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        pickerModels = loadProgram(loader, "core/picker_models", DefaultVertexFormat.NEW_ENTITY);
        
        MODEL_PIPELINE = registerPipeline("pipeline/model", "core/model", DefaultVertexFormat.NEW_ENTITY);
        MULTILINK_PIPELINE = registerPipeline("pipeline/multilink", "core/multilink", DefaultVertexFormat.POSITION_TEX_COLOR);
        SUBTITLES_PIPELINE = registerPipeline("pipeline/subtitles", "core/subtitles", DefaultVertexFormat.POSITION_TEX_COLOR);

        for (Runnable runnable : LOADERS)
        {
            runnable.run();
        }
    }

    public static GlProgram getModel()
    {
        return model;
    }

    public static GlProgram getMultilinkProgram()
    {
        return multiLink;
    }

    public static GlProgram getSubtitlesProgram()
    {
        return subtitles;
    }

    public static GlProgram getPickerPreviewProgram()
    {
        return pickerPreview;
    }

    public static GlProgram getPickerBillboardProgram()
    {
        return pickerBillboard;
    }

    public static GlProgram getPickerBillboardNoShadingProgram()
    {
        return pickerBillboardNoShading;
    }

    public static GlProgram getPickerParticlesProgram()
    {
        return pickerParticles;
    }

    public static GlProgram getPickerModelsProgram()
    {
        return pickerModels;
    }

    private static GlProgram loadProgram(ShaderManager loader, String path, Object vertexFormat)
    {
        Identifier id = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, path);

        try
        {
            Method m = loader.getClass().getMethod("getOrCreateProgram", Identifier.class, vertexFormat.getClass());
            return (GlProgram) m.invoke(loader, id, vertexFormat);
        }
        catch (Exception ignored) {}

        try
        {
            Method m = loader.getClass().getMethod("getOrCreateProgram", Identifier.class);
            return (GlProgram) m.invoke(loader, id);
        }
        catch (Exception ignored) {}

        return null;
    }

    private static RenderPipeline registerPipeline(String pipelinePath, String shaderPath, Object vertexFormat)
    {
        try
        {
            Class<?> renderPipelineClass = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline");
            Object builder = renderPipelineClass.getMethod("builder").invoke(null);
            builder = invokeBest(builder, "withLocation", Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, pipelinePath));
            builder = invokeBest(builder, "withVertexShader", Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, shaderPath));
            builder = invokeBest(builder, "withFragmentShader", Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, shaderPath));

            Object mode = null;
            try
            {
                Class<?> modeClass = Class.forName("com.mojang.blaze3d.vertex.VertexFormat$DrawMode");
                mode = Enum.valueOf((Class<Enum>) modeClass, "TRIANGLES");
            }
            catch (Exception ignored) {}

            if (mode != null)
            {
                builder = invokeBest(builder, "withVertexFormat", vertexFormat, mode);
            }
            else
            {
                builder = invokeBest(builder, "withVertexFormat", vertexFormat);
            }

            Object pipeline = renderPipelineClass.getMethod("build").invoke(builder);
            Method register = RenderPipelines.class.getMethod("register", renderPipelineClass);

            return (RenderPipeline) register.invoke(null, pipeline);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static Object invokeBest(Object target, String methodName, Object... args) throws Exception
    {
        Method[] methods = target.getClass().getMethods();

        for (Method method : methods)
        {
            if (!method.getName().equals(methodName))
            {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length != args.length)
            {
                continue;
            }

            boolean ok = true;

            for (int i = 0; i < params.length; i++)
            {
                if (args[i] != null && !params[i].isAssignableFrom(args[i].getClass()))
                {
                    ok = false;
                    break;
                }
            }

            if (ok)
            {
                return method.invoke(target, args);
            }
        }

        throw new NoSuchMethodException(methodName);
    }
}