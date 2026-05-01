package mchorse.bbs_mod.client.screen;

import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.camera.clips.screen.GrainEffect;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureFormat;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

public class ColorGradeRenderer
{
    private static final String VERT = """
            #version 150

            in vec2 a_pos;
            in vec2 a_uv;
            out vec2 v_uv;

            void main()
            {
                v_uv = a_uv;
                gl_Position = vec4(a_pos, 0.0, 1.0);
            }
            """;

    private static final String FRAG = """
            #version 150

            in vec2 v_uv;
            out vec4 fragColor;

            uniform sampler2D u_sampler;

            /* Vignette */
            uniform float u_vigStr;
            uniform float u_vigSmooth;
            uniform vec3 u_vigColor;

            /* Color grade */
            uniform float u_brightness;
            uniform float u_contrast;
            uniform float u_saturation;
            uniform float u_hue;
            uniform vec3 u_lift;
            uniform vec3 u_gamma;
            uniform vec3 u_gain;

            /* Film grain */
            uniform float u_grainStr;
            uniform float u_grainSize;
            uniform float u_grainSeed;

            /* UV distortion */
            uniform vec2 u_distort;

            /* --- HSL helpers --- */

            vec3 rgb2hsl(vec3 c)
            {
                float maxC = max(c.r, max(c.g, c.b));
                float minC = min(c.r, min(c.g, c.b));
                float delta = maxC - minC;
                float l = (maxC + minC) * 0.5;
                float s = delta < 1e-5 ? 0.0 : delta / (1.0 - abs(2.0 * l - 1.0));
                float h = 0.0;
                if (delta > 1e-5)
                {
                    if      (maxC == c.r) h = mod((c.g - c.b) / delta, 6.0) / 6.0;
                    else if (maxC == c.g) h = ((c.b - c.r) / delta + 2.0) / 6.0;
                    else                  h = ((c.r - c.g) / delta + 4.0) / 6.0;
                }
                return vec3(h, s, l);
            }

            vec3 hsl2rgb(vec3 c)
            {
                float h = c.x, s = c.y, l = c.z;
                float C = (1.0 - abs(2.0 * l - 1.0)) * s;
                float X = C * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
                float m = l - C * 0.5;
                vec3 rgb;
                if      (h < 1.0 / 6.0) rgb = vec3(C, X, 0.0);
                else if (h < 2.0 / 6.0) rgb = vec3(X, C, 0.0);
                else if (h < 3.0 / 6.0) rgb = vec3(0.0, C, X);
                else if (h < 4.0 / 6.0) rgb = vec3(0.0, X, C);
                else if (h < 5.0 / 6.0) rgb = vec3(X, 0.0, C);
                else                     rgb = vec3(C, 0.0, X);
                return rgb + m;
            }

            float hash(vec2 p)
            {
                return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
            }

            void main()
            {
                vec2 sampleUV = v_uv + u_distort;
                vec4 color = texture(u_sampler, sampleUV);
                vec3 rgb = color.rgb;

                /* 1 — Lift / Gamma / Gain */
                rgb = rgb * (vec3(1.0) + u_gain);
                rgb = sign(rgb) * pow(max(abs(rgb), vec3(1e-4)), max(vec3(1e-4), vec3(1.0) / (vec3(1.0) + u_gamma)));
                rgb = rgb + u_lift;

                /* 2 — Brightness and contrast */
                rgb = rgb + u_brightness;
                rgb = vec3(0.5) + (1.0 + u_contrast) * (rgb - vec3(0.5));

                /* 3 — Saturation */
                float luma = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
                rgb = mix(vec3(luma), rgb, 1.0 + u_saturation);

                /* 4 — Hue rotation */
                if (abs(u_hue) > 0.01)
                {
                    vec3 hsl = rgb2hsl(rgb);
                    hsl.x = fract(hsl.x + u_hue / 360.0);
                    rgb = hsl2rgb(hsl);
                }

                /* 5 — Vignette (radial, smooth) */
                if (u_vigStr > 0.001)
                {
                    vec2 uv = v_uv - vec2(0.5);
                    float dist = length(uv) * 2.0 / sqrt(2.0);
                    float inner = max(0.0, 1.0 - u_vigSmooth);
                    float alpha = smoothstep(inner, 1.0, dist) * u_vigStr;
                    rgb = mix(rgb, u_vigColor, clamp(alpha, 0.0, 1.0));
                }

                /* 6 — Film grain */
                if (u_grainStr > 0.001)
                {
                    vec2 texSize = vec2(textureSize(u_sampler, 0));
                    vec2 grainUV = floor(v_uv * texSize / max(1.0, u_grainSize));
                    float noise = hash(grainUV + vec2(u_grainSeed));
                    rgb += (noise - 0.5) * u_grainStr * 2.0;
                }

                fragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
            }
            """;

    private static boolean initialized;
    private static boolean failed;
    private static int program;
    private static int vao;
    private static int vbo;
    private static Texture tempTex;

    private static int uSampler;
    private static int uVigStr;
    private static int uVigSmooth;
    private static int uVigColor;
    private static int uBrightness;
    private static int uContrast;
    private static int uSaturation;
    private static int uHue;
    private static int uLift;
    private static int uGamma;
    private static int uGain;
    private static int uGrainStr;
    private static int uGrainSize;
    private static int uGrainSeed;
    private static int uDistort;

    public static void apply(List<ColorEffect> effects, List<GrainEffect> grainEffects)
    {
        boolean needVignette = false;
        boolean needGrade = false;
        boolean needGrain = false;
        boolean needDistort = false;

        for (ColorEffect e : effects)
        {
            if (e.hasVignette) needVignette = true;
            if (e.hasGrade) needGrade = true;
            if (e.hasDistort) needDistort = true;
        }

        for (GrainEffect e : grainEffects)
        {
            if (e.strength > 0F) needGrain = true;
        }

        if (!needVignette && !needGrade && !needGrain && !needDistort)
        {
            return;
        }

        if (!initialized)
        {
            init();
        }

        if (failed)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        net.minecraft.client.gl.Framebuffer fb = mc.getFramebuffer();
        int fbW = fb.textureWidth;
        int fbH = fb.textureHeight;

        /* Copy current framebuffer content to tempTex */
        if (tempTex == null)
        {
            tempTex = new Texture();
            tempTex.setFormat(TextureFormat.RGB_U8);
            tempTex.setFilter(GL11.GL_LINEAR);
        }

        fb.beginWrite(false);
        tempTex.bind();

        if (tempTex.width != fbW || tempTex.height != fbH)
        {
            tempTex.setSize(fbW, fbH);
        }

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fbW, fbH);
        tempTex.unbind();

        /* Accumulate color effects */
        float vigStr = 0F;
        float vigSmooth = 0.5F;
        float vigR = 0F;
        float vigG = 0F;
        float vigB = 0F;
        float brightness = 0F;
        float contrast = 0F;
        float saturation = 0F;
        float hue = 0F;
        float liftR = 0F, liftG = 0F, liftB = 0F;
        float gammaR = 0F, gammaG = 0F, gammaB = 0F;
        float gainR = 0F, gainG = 0F, gainB = 0F;

        for (ColorEffect e : effects)
        {
            if (e.hasVignette)
            {
                vigStr = Math.max(vigStr, e.vignetteStrength);
                vigSmooth = e.vignetteSmoothness;
                vigR = ((e.vignetteColor >> 16) & 0xFF) / 255.0F;
                vigG = ((e.vignetteColor >> 8) & 0xFF) / 255.0F;
                vigB = (e.vignetteColor & 0xFF) / 255.0F;
            }

            if (e.hasGrade)
            {
                brightness += e.brightness;
                contrast += e.contrast;
                saturation += e.saturation;
                hue += e.hue;
                liftR += e.liftR;
                liftG += e.liftG;
                liftB += e.liftB;
                gammaR += e.gammaR;
                gammaG += e.gammaG;
                gammaB += e.gammaB;
                gainR += e.gainR;
                gainG += e.gainG;
                gainB += e.gainB;
            }
        }

        /* Accumulate grain effects */
        float grainStr = 0F;
        float grainSize = 1F;

        for (GrainEffect e : grainEffects)
        {
            grainStr += e.strength;
            grainSize = e.size;
        }

        float grainSeed = (System.nanoTime() & 0xFFFFL) / 65536.0F;

        /* Accumulate distortion */
        float distortX = 0F;
        float distortY = 0F;

        for (ColorEffect e : effects)
        {
            if (e.hasDistort)
            {
                distortX += e.distortX;
                distortY += e.distortY;
            }
        }

        /* Save and set viewport */
        int[] prevViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        GL11.glViewport(0, 0, fbW, fbH);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL20.glUseProgram(program);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        tempTex.bind();
        GL20.glUniform1i(uSampler, 0);

        GL20.glUniform1f(uVigStr, vigStr);
        GL20.glUniform1f(uVigSmooth, vigSmooth);
        GL20.glUniform3f(uVigColor, vigR, vigG, vigB);
        GL20.glUniform1f(uBrightness, brightness);
        GL20.glUniform1f(uContrast, contrast);
        GL20.glUniform1f(uSaturation, saturation);
        GL20.glUniform1f(uHue, hue);
        GL20.glUniform3f(uLift, liftR, liftG, liftB);
        GL20.glUniform3f(uGamma, gammaR, gammaG, gammaB);
        GL20.glUniform3f(uGain, gainR, gainG, gainB);
        GL20.glUniform1f(uGrainStr, grainStr);
        GL20.glUniform1f(uGrainSize, grainSize);
        GL20.glUniform1f(uGrainSeed, grainSeed);
        GL20.glUniform2f(uDistort, distortX, distortY);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);
        tempTex.unbind();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
    }

    private static void init()
    {
        initialized = true;

        int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vert, VERT);
        GL20.glCompileShader(vert);

        if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
        {
            System.err.println("[ColorGradeRenderer] Vertex shader failed:\n" + GL20.glGetShaderInfoLog(vert));
            GL20.glDeleteShader(vert);
            failed = true;

            return;
        }

        int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(frag, FRAG);
        GL20.glCompileShader(frag);

        if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
        {
            System.err.println("[ColorGradeRenderer] Fragment shader failed:\n" + GL20.glGetShaderInfoLog(frag));
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            failed = true;

            return;
        }

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glBindAttribLocation(program, 0, "a_pos");
        GL20.glBindAttribLocation(program, 1, "a_uv");
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            System.err.println("[ColorGradeRenderer] Link failed:\n" + GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            failed = true;

            return;
        }

        uSampler = GL20.glGetUniformLocation(program, "u_sampler");
        uVigStr = GL20.glGetUniformLocation(program, "u_vigStr");
        uVigSmooth = GL20.glGetUniformLocation(program, "u_vigSmooth");
        uVigColor = GL20.glGetUniformLocation(program, "u_vigColor");
        uBrightness = GL20.glGetUniformLocation(program, "u_brightness");
        uContrast = GL20.glGetUniformLocation(program, "u_contrast");
        uSaturation = GL20.glGetUniformLocation(program, "u_saturation");
        uHue = GL20.glGetUniformLocation(program, "u_hue");
        uLift = GL20.glGetUniformLocation(program, "u_lift");
        uGamma = GL20.glGetUniformLocation(program, "u_gamma");
        uGain = GL20.glGetUniformLocation(program, "u_gain");
        uGrainStr = GL20.glGetUniformLocation(program, "u_grainStr");
        uGrainSize = GL20.glGetUniformLocation(program, "u_grainSize");
        uGrainSeed = GL20.glGetUniformLocation(program, "u_grainSeed");
        uDistort = GL20.glGetUniformLocation(program, "u_distort");

        /* Fullscreen quad VAO/VBO (NDC coords + UV) */
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = MemoryUtil.memAllocFloat(24);

        buf.put(new float[] {
            -1F, -1F,  0F, 0F,
             1F, -1F,  1F, 0F,
             1F,  1F,  1F, 1F,
            -1F, -1F,  0F, 0F,
             1F,  1F,  1F, 1F,
            -1F,  1F,  0F, 1F,
        }).flip();

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(buf);

        int stride = 4 * Float.BYTES;

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, (long) (2 * Float.BYTES));

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}
