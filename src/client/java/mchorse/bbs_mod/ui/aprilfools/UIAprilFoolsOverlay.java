package mchorse.bbs_mod.ui.aprilfools;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.loader.api.FabricLoader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UIAprilFoolsOverlay extends UIElement
{
    private static final Link[] TEXTURE_55 = new Link[] {
        Link.bbs("assets/textures/55.png"),
        Link.bbs("textures/55.png"),
        Link.assets("textures/55.png"),
        Link.assets("assets/textures/55.png")
    };
    private static final Link[] TEXTURE_ROCKET = new Link[] {
        Link.bbs("assets/textures/rocket.png"),
        Link.bbs("textures/rocket.png"),
        Link.assets("textures/rocket.png"),
        Link.assets("assets/textures/rocket.png")
    };
    private static final Link[] TEXTURE_ICON = new Link[] {
        Link.bbs("assets/textures/icon.png"),
        Link.bbs("textures/icon.png"),
        Link.assets("textures/icon.png"),
        Link.assets("assets/textures/icon.png")
    };
    private static final Link[] TEXTURE_BANNER = new Link[] {
        Link.bbs("assets/textures/banners/CML.png"),
        Link.bbs("textures/banners/CML.png"),
        Link.assets("textures/banners/CML.png"),
        Link.assets("assets/textures/banners/CML.png")
    };

    private static final Link[][] FLOWEY_RISE2 = new Link[9][];
    private static final Link[][] FLOWEY_IDLE = new Link[2][];

    static
    {
        for (int i = 0; i < 9; i++)
        {
            FLOWEY_RISE2[i] = floweyLink("rise2_" + i);
        }

        FLOWEY_IDLE[0] = floweyLink("idle_0");
        FLOWEY_IDLE[1] = floweyLink("idle_1");
    }

    private static Link[] floweyLink(String name)
    {
        return new Link[] {
            Link.bbs("assets/textures/flowey/" + name + ".png"),
            Link.bbs("textures/flowey/" + name + ".png"),
            Link.assets("textures/flowey/" + name + ".png"),
            Link.assets("assets/textures/flowey/" + name + ".png")
        };
    }

    public static boolean devUnlocked = false;

    /* 0 = hidden, 1 = rising, 2 = idle */
    private int floweyState = 0;
    private long floweyLastFrame = 0L;
    private int floweyFrame = 0;
    private int floweyAnchorBottom = 0;

    private final Random random = new Random();
    private final List<Sprite> sprites = new ArrayList<>();
    private final List<MemeText> memeTexts = new ArrayList<>();
    private int warmupTicks;
    private long lastFrameTime;

    private void spawnSprite()
    {
        float size = 20 + this.random.nextInt(36);
        float minX = this.area.x;
        float minY = this.area.y;
        float maxX = Math.max(minX, this.area.ex() - size);
        float maxY = Math.max(minY, this.area.ey() - size);
        Sprite sprite = new Sprite();

        sprite.x = minX + this.random.nextFloat() * (maxX - minX);
        sprite.y = minY + this.random.nextFloat() * (maxY - minY);
        sprite.vx = (this.random.nextFloat() * 2F - 1F) * 1.4F;
        sprite.vy = (this.random.nextFloat() * 2F - 1F) * 1.4F;
        sprite.size = size;
        sprite.rocket = this.random.nextBoolean();
        sprite.corrupted = isEnglish();

        if (Math.abs(sprite.vx) < 0.2F)
        {
            sprite.vx = sprite.vx < 0 ? -0.45F : 0.45F;
        }

        if (Math.abs(sprite.vy) < 0.2F)
        {
            sprite.vy = sprite.vy < 0 ? -0.45F : 0.45F;
        }

        this.sprites.add(sprite);
    }

    private void spawnMemeText()
    {
        MemeText meme = new MemeText();
        float minX = this.area.x + 8;
        float minY = this.area.y + 10;
        float maxX = Math.max(minX, this.area.ex() - 140);
        float maxY = Math.max(minY, this.area.ey() - 20);

        meme.x = minX + this.random.nextFloat() * (maxX - minX);
        meme.y = minY + this.random.nextFloat() * (maxY - minY);
        meme.vx = (this.random.nextFloat() * 2F - 1F) * 0.9F;
        meme.vy = (this.random.nextFloat() * 2F - 1F) * 0.9F;
        meme.big = this.random.nextBoolean();
        meme.corrupted = isEnglish();
        this.memeTexts.add(meme);
    }

    @Override
    public void render(UIContext context)
    {
        if (context.menu instanceof UIDashboard dashboard && dashboard.getPanels().panel instanceof UIAprilFoolsPanel)
        {
            super.render(context);
            return;
        }

        if (isAprilFoolsEnabled())
        {
            if (context.menu instanceof UIDashboard dashboard)
            {
                Texture banner = this.resolveTexture(TEXTURE_BANNER);
                Texture icon = this.resolveTexture(TEXTURE_ICON);

                this.renderGlitchTaskbar(context, dashboard.getPanels().panelButtons, banner, icon);
            }
            long now = System.currentTimeMillis();
            float dt = this.lastFrameTime == 0 ? 0.016F : Math.min(0.05F, (now - this.lastFrameTime) / 1000F);
            this.lastFrameTime = now;

            this.animate(dt);

            Texture texture55 = this.resolveTexture(TEXTURE_55);
            Texture rocket = this.resolveTexture(TEXTURE_ROCKET);
            Texture icon = this.resolveTexture(TEXTURE_ICON);
            Texture banner = this.resolveTexture(TEXTURE_BANNER);
            long seed = System.currentTimeMillis() / 100L;

            for (Sprite sprite : this.sprites)
            {
                if (sprite.corrupted)
                {
                    Texture texture = sprite.rocket ? icon : banner;

                    if (texture != null)
                    {
                        Random rng = new Random(seed ^ (long) (sprite.x * 1000));
                        float u1 = rng.nextFloat() * texture.width;
                        float v1 = rng.nextFloat() * texture.height;
                        float u2 = u1 + (rng.nextBoolean() ? texture.width : -texture.width);
                        float v2 = v1 + (rng.nextBoolean() ? texture.height : -texture.height);

                        context.batcher.texturedBox(texture, Colors.A75 | Colors.WHITE, sprite.x, sprite.y, sprite.size, sprite.size, u1, v1, u2, v2, texture.width, texture.height);
                    }
                }
                else
                {
                    Texture texture = sprite.rocket ? rocket : texture55;

                    if (texture != null)
                    {
                        context.batcher.texturedBox(texture, Colors.A75 | Colors.WHITE, sprite.x, sprite.y, sprite.size, sprite.size, 0, 0, texture.width, texture.height, texture.width, texture.height);
                    }
                }
            }

            for (MemeText meme : this.memeTexts)
            {
                String label = meme.corrupted ? "error 404" : (meme.big ? "Hombre De 55" : "55");
                int color = meme.corrupted ? (meme.big ? 0xFFFF4444 : 0xFFFF6666) : (meme.big ? 0xFFFFFF55 : 0xFFFFEE55);
                float tx = meme.x;
                float ty = meme.y;

                context.batcher.text(label, tx, ty - 1, 0xAA000000);
                context.batcher.text(label, tx, ty + 1, 0xAA000000);
                context.batcher.text(label, tx - 1, ty, 0xAA000000);
                context.batcher.text(label, tx + 1, ty, 0xAA000000);
                context.batcher.text(label, tx, ty, color);
            }

            if (this.floweyState > 0)
            {
                this.renderFlowey(context);
            }
        }

        super.render(context);
    }

    public void startFlowey()
    {
        this.floweyState = 1;
        this.floweyLastFrame = System.currentTimeMillis();
        this.floweyFrame = 0;
    }

    private void renderFlowey(UIContext context)
    {
        long now = System.currentTimeMillis();
        int scale = 2;

        if (this.floweyState == 1)
        {
            /* ~100ms per frame for 9 rise frames */
            if (now - this.floweyLastFrame >= 100L)
            {
                this.floweyFrame++;
                this.floweyLastFrame = now;

                if (this.floweyFrame >= 9)
                {
                    this.floweyFrame = 0;
                    this.floweyState = 2;
                }
            }

            int riseFrame = Math.min(this.floweyFrame, FLOWEY_RISE2.length - 1);
            Texture tex = this.resolveTexture(FLOWEY_RISE2[riseFrame]);

            if (tex != null)
            {
                int w = tex.width * scale;
                int h = tex.height * scale;
                int cx = this.area.mx() - w / 2;
                int cy = this.area.my() - h / 2;

                /* Record bottom anchor so idle aligns to the same position */
                this.floweyAnchorBottom = cy + h;

                context.batcher.texturedBox(tex, Colors.WHITE, cx, cy, w, h, 0, 0, tex.width, tex.height, tex.width, tex.height);
            }
        }
        else if (this.floweyState == 2)
        {
            /* ~500ms per frame for 2 idle frames */
            if (now - this.floweyLastFrame >= 500L)
            {
                this.floweyFrame = (this.floweyFrame + 1) % 2;
                this.floweyLastFrame = now;
            }

            Texture tex = this.resolveTexture(FLOWEY_IDLE[this.floweyFrame]);

            if (tex != null)
            {
                int w = tex.width * scale;
                int h = tex.height * scale;
                int cx = this.area.mx() - w / 2;
                int cy = this.floweyAnchorBottom - h;

                context.batcher.texturedBox(tex, Colors.WHITE, cx, cy, w, h, 0, 0, tex.width, tex.height, tex.width, tex.height);
            }
        }
    }

    private void renderGlitchTaskbar(UIContext context, UIElement taskbar, Texture banner, Texture icon)
    {
        if (taskbar == null || (banner == null && icon == null))
        {
            return;
        }

        int x = taskbar.area.x;
        int y = taskbar.area.y;
        int w = taskbar.area.w;
        int h = taskbar.area.h;
        int slices = 10;
        int sliceH = Math.max(1, h / slices);
        long seed = System.currentTimeMillis() / 100L;

        for (int i = 0; i < slices; i++)
        {
            Random rng = new Random(seed ^ (i * 0x9e3779b9L));
            Texture tex = rng.nextBoolean() ? banner : icon;

            if (tex == null)
            {
                continue;
            }

            int sy = y + i * sliceH;
            int sh = (i == slices - 1) ? (h - i * sliceH) : sliceH;
            int dx = (int) ((rng.nextFloat() * 2F - 1F) * 8F);
            int sw = (int) (w * (0.7F + rng.nextFloat() * 0.4F));

            float u1 = rng.nextFloat() * tex.width;
            float v1 = rng.nextFloat() * tex.height;
            float u2 = u1 + (rng.nextBoolean() ? tex.width : -tex.width);
            float v2 = v1 + (rng.nextBoolean() ? tex.height : -tex.height);

            context.batcher.texturedBox(tex, Colors.A75 | Colors.WHITE, x + dx, sy, sw, sh, u1, v1, u2, v2, tex.width, tex.height);
        }
    }

    private void animate(float dt)
    {
        if (this.area.w <= 0 || this.area.h <= 0)
        {
            return;
        }

        if (this.sprites.isEmpty() || this.warmupTicks % 240 == 0)
        {
            this.spawnSprite();
        }
        if (this.memeTexts.isEmpty() || this.warmupTicks % 90 == 0)
        {
            this.spawnMemeText();
        }

        this.warmupTicks++;

        for (Sprite sprite : this.sprites)
        {
            sprite.x += sprite.vx * dt * 60F;
            sprite.y += sprite.vy * dt * 60F;

            if (sprite.x < this.area.x)
            {
                sprite.x = this.area.x;
                sprite.vx = Math.abs(sprite.vx);
            }
            else if (sprite.x + sprite.size > this.area.ex())
            {
                sprite.x = this.area.ex() - sprite.size;
                sprite.vx = -Math.abs(sprite.vx);
            }

            if (sprite.y < this.area.y)
            {
                sprite.y = this.area.y;
                sprite.vy = Math.abs(sprite.vy);
            }
            else if (sprite.y + sprite.size > this.area.ey())
            {
                sprite.y = this.area.ey() - sprite.size;
                sprite.vy = -Math.abs(sprite.vy);
            }
        }

        for (MemeText meme : this.memeTexts)
        {
            meme.x += meme.vx * dt * 60F;
            meme.y += meme.vy * dt * 60F;

            if (meme.x < this.area.x)
            {
                meme.x = this.area.x;
                meme.vx = Math.abs(meme.vx);
            }
            else if (meme.x + 130 > this.area.ex())
            {
                meme.x = this.area.ex() - 130;
                meme.vx = -Math.abs(meme.vx);
            }

            if (meme.y < this.area.y + 8)
            {
                meme.y = this.area.y + 8;
                meme.vy = Math.abs(meme.vy);
            }
            else if (meme.y + 12 > this.area.ey())
            {
                meme.y = this.area.ey() - 12;
                meme.vy = -Math.abs(meme.vy);
            }
        }

        if (this.sprites.size() > 18)
        {
            this.sprites.remove(0);
        }
        if (this.memeTexts.size() > 24)
        {
            this.memeTexts.remove(0);
        }
    }

    public static boolean isAprilFoolsEnabled()
    {
        LocalDate now = LocalDate.now();

        return Boolean.getBoolean("bbs.april.fools")
            || FabricLoader.getInstance().isDevelopmentEnvironment()
            || (now.getMonthValue() == 4 && now.getDayOfMonth() == 1);
    }

    public static boolean isEnglish()
    {
        return !BBSModClient.getLanguageKey().startsWith("es");
    }

    private Texture resolveTexture(Link[] candidates)
    {
        Texture error = BBSModClient.getTextures().getError();

        for (Link candidate : candidates)
        {
            Texture texture = BBSModClient.getTextures().getTexture(candidate);

            if (texture != null && texture != error)
            {
                return texture;
            }
        }

        return null;
    }

    private static class Sprite
    {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float size;
        private boolean rocket;
        private boolean corrupted;
    }

    private static class MemeText
    {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private boolean big;
        private boolean corrupted;
    }
}
