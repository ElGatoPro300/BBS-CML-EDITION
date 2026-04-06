package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class BBSShaders
{
    public static final List<Runnable> LOADERS = new ArrayList<>();

    private static ShaderProgram model;
    private static ShaderProgram multiLink;
    private static ShaderProgram subtitles;

    private static ShaderProgram pickerPreview;
    private static ShaderProgram pickerBillboard;
    private static ShaderProgram pickerBillboardNoShading;
    private static ShaderProgram pickerParticles;
    private static ShaderProgram pickerModels;
    
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

        ShaderLoader loader = MinecraftClient.getInstance().getShaderLoader();

        model = loadProgram(loader, "core/model", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        multiLink = loadProgram(loader, "core/multilink", VertexFormats.POSITION_TEXTURE_COLOR);
        subtitles = loadProgram(loader, "core/subtitles", VertexFormats.POSITION_TEXTURE_COLOR);

        pickerPreview = loadProgram(loader, "core/picker_preview", VertexFormats.POSITION_TEXTURE_COLOR);
        pickerBillboard = loadProgram(loader, "core/picker_billboard", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        pickerBillboardNoShading = loadProgram(loader, "core/picker_billboard_no_shading", VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
        pickerParticles = loadProgram(loader, "core/picker_particles", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
        pickerModels = loadProgram(loader, "core/picker_models", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        
        MODEL_PIPELINE = registerPipeline("pipeline/model", "core/model", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        MULTILINK_PIPELINE = registerPipeline("pipeline/multilink", "core/multilink", VertexFormats.POSITION_TEXTURE_COLOR);
        SUBTITLES_PIPELINE = registerPipeline("pipeline/subtitles", "core/subtitles", VertexFormats.POSITION_TEXTURE_COLOR);

        for (Runnable runnable : LOADERS)
        {
            runnable.run();
        }
    }

    public static ShaderProgram getModel()
    {
        return model;
    }

    public static ShaderProgram getMultilinkProgram()
    {
        return multiLink;
    }

    public static ShaderProgram getSubtitlesProgram()
    {
        return subtitles;
    }

    public static ShaderProgram getPickerPreviewProgram()
    {
        return pickerPreview;
    }

    public static ShaderProgram getPickerBillboardProgram()
    {
        return pickerBillboard;
    }

    public static ShaderProgram getPickerBillboardNoShadingProgram()
    {
        return pickerBillboardNoShading;
    }

    public static ShaderProgram getPickerParticlesProgram()
    {
        return pickerParticles;
    }

    public static ShaderProgram getPickerModelsProgram()
    {
        return pickerModels;
    }

    private static ShaderProgram loadProgram(ShaderLoader loader, String path, Object vertexFormat)
    {
        Identifier id = Identifier.of(BBSMod.MOD_ID, path);

        try
        {
            Method m = loader.getClass().getMethod("getOrCreateProgram", Identifier.class, vertexFormat.getClass());
            return (ShaderProgram) m.invoke(loader, id, vertexFormat);
        }
        catch (Exception ignored) {}

        try
        {
            Method m = loader.getClass().getMethod("getOrCreateProgram", Identifier.class);
            return (ShaderProgram) m.invoke(loader, id);
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
            builder = invokeBest(builder, "withLocation", Identifier.of(BBSMod.MOD_ID, pipelinePath));
            builder = invokeBest(builder, "withVertexShader", Identifier.of(BBSMod.MOD_ID, shaderPath));
            builder = invokeBest(builder, "withFragmentShader", Identifier.of(BBSMod.MOD_ID, shaderPath));

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