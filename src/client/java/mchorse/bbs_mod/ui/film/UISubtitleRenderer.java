package mchorse.bbs_mod.ui.film;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UISubtitleRenderer
{
    private static Framebuffer getTextFramebuffer()
    {
        return BBSModClient.getFramebuffers().getFramebuffer(Link.bbs("camera_subtitles"), (f) ->
        {
            Texture texture = BBSModClient.getTextures().createTexture(Link.bbs("test"));

            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            f.deleteTextures();
            f.attach(texture, GL30.GL_COLOR_ATTACHMENT0);

            f.unbind();
        });
    }

    public static void renderSubtitles(MatrixStack stack, Batcher2D batcher, List<Subtitle> subtitles)
    {
        if (subtitles.isEmpty())
        {
            return;
        }

        ShaderProgram program = BBSShaders.getSubtitlesProgram();
        GlUniform blur = program.getUniform("Blur");
        GlUniform textureSize = program.getUniform("TextureSize");
        Supplier<ShaderProgram> supplier = () -> program;

        net.minecraft.client.gl.Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        int width = fb.textureWidth;
        int height = fb.textureHeight;

        Matrix4f cache = new Matrix4f(RenderSystem.getModelViewMatrix());

        width /= 2;
        height /= 2;

        Framebuffer framebuffer = getTextFramebuffer();
        Texture texture = framebuffer.getMainTexture();
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -100, 100);
        FontRenderer font = Batcher2D.getDefaultTextRenderer();

        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._disableCull();

        for (Subtitle subtitle : subtitles)
        {
            float alpha = Colors.getA(subtitle.color);

            if (alpha <= 0)
            {
                continue;
            }

            String label = StringUtils.processColoredText(subtitle.label);
            int w = 0;
            int h = 0;
            int x = (int) (width * subtitle.windowX + subtitle.x);
            int y = (int) (height * subtitle.windowY + subtitle.y);
            float scale = subtitle.size;
            int subColor = subtitle.color;

            List<String> strings = subtitle.maxWidth <= 10 ? Arrays.asList(label) : font.wrap(label, subtitle.maxWidth);

            for (String string : strings)
            {
                w = Math.max(w, font.getWidth(string.trim()));
            }

            h = (strings.size() - 1) * subtitle.lineHeight + font.getHeight();

            int fw = (int) ((w + 10) * scale);
            int fh = (int) ((h + 10) * scale);

            /* projection matrix state managed by 1.21.11 renderer */

            framebuffer.resize(fw, fh);
            framebuffer.applyClear();

            int yy = 5;

            for (String string : strings)
            {
                string = string.trim();

                int xx = 5 + (w - font.getWidth(string)) / 2;

                if (Colors.getA(subtitle.backgroundColor) > 0)
                {
                    batcher.textCard(string, xx, yy, Colors.setA(subColor, 1F), Colors.mulA(subtitle.backgroundColor, alpha), subtitle.backgroundOffset, subtitle.textShadow);
                }
                else
                {
                    batcher.text(string, xx, yy, Colors.setA(subColor, 1F), subtitle.textShadow);
                }

                yy += subtitle.lineHeight;
            }

            /* Render the texture */

            /* projection matrix state managed by 1.21.11 renderer */

            Transform transform = new Transform();

            transform.lerp(subtitle.transform, 1F - subtitle.factor);

            stack.push();
            stack.translate(x, y, 0);
            MatrixStackUtils.applyTransform(stack, transform);

            if (blur != null)
            {
                // Uniform setter API changed in 1.21.11.
            }

            if (textureSize != null)
            {
                // Uniform setter API changed in 1.21.11.
            }

            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(770, 771, 1, 771);

            batcher.texturedBox(program, texture.id, Colors.setA(Colors.WHITE, alpha), -fw * subtitle.anchorX, -fh * subtitle.anchorY, texture.width, texture.height, 0, 0, texture.width, texture.height, texture.width, texture.height);

            stack.pop();
        }

        /* projection matrix state managed by 1.21.11 renderer */
        GlStateManager._enableCull();
    }

    public static void renderSubtitle(MatrixStack stack, Batcher2D batcher, Subtitle subtitle)
    {
        if (subtitle == null)
        {
            return;
        }

        renderSubtitles(stack, batcher, Collections.singletonList(subtitle));
    }
}
