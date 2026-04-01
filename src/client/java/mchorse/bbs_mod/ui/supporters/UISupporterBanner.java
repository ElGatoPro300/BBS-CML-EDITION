package mchorse.bbs_mod.ui.supporters;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.aprilfools.UIAprilFoolsOverlay;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class UISupporterBanner extends UIElement
{
    public static final String LINK_BILIBILI = "space.bilibili.com";
    public static final String LINK_TIKTOK = "douyin.com";
    public static final String LINK_TIKTOK2 = "tiktok.com";
    public static final String LINK_YOUTUBE = "youtube.com";
    public static final String LINK_TWITTER = "twitter.com";
    public static final String LINK_TWITTER2 = "x.com";
    public static final String LINK_TWITCH = "twitch.com";

    private UIElement placeholder;
    private UIElement nameRow;

    private Supporter supporter;
    private int randomColor;

    public static UIElement createLinkIcon(Supporter supporter)
    {
        return new UIIcon(getFromLink(supporter.link), (b) ->
        {
            if (!supporter.link.isEmpty() && !supporter.link.equals("..."))
            {
                UIUtils.openWebLink(supporter.link);
            }
        });
    }

    public static UIElement createLinkEntry(Supporter supporter)
    {
        return UI.row(
            UI.label(IKey.constant(supporter.name), 20).labelAnchor(0F, 0.5F),
            UISupporterBanner.createLinkIcon(supporter)
        );
    }

    public static Icon getFromLink(String link)
    {
        if (link.contains(LINK_BILIBILI))
        {
            return Icons.BILIBILI;
        }
        else if (link.contains(LINK_TIKTOK) || link.contains(LINK_TIKTOK2))
        {
            return Icons.TIKTOK;
        }
        else if (link.contains(LINK_YOUTUBE))
        {
            return Icons.YOUTUBE;
        }
        else if (link.contains(LINK_TWITTER) || link.contains(LINK_TWITTER2))
        {
            return Icons.TWITTER;
        }
        else if (link.contains(LINK_TWITCH))
        {
            return Icons.TWITCH;
        }
        else if (link.isEmpty() || link.equals("..."))
        {
            return Icons.NONE;
        }

        return Icons.LINK;
    }

    public UISupporterBanner(Supporter supporter)
    {
        super();

        this.supporter = supporter;

        this.column(0).vertical().stretch();

        UIElement row = createLinkEntry(supporter);
        this.nameRow = row;

        this.placeholder = new UIElement();
        this.placeholder.relative(this).w(1F).h(70);
        this.randomColor = Colors.HSVtoRGB((float) Math.random(), 1F, 1F).getARGBColor();

        this.add(row, this.placeholder);
    }

    private void renderGlitchText(UIContext context)
    {
        if (this.nameRow == null || this.nameRow.area.w <= 0)
        {
            return;
        }

        Area a = this.nameRow.area;
        long seed = (System.currentTimeMillis() / 80L) * (0xCAFEBABEL + this.supporter.name.hashCode());
        Random rng = new Random(seed);

        float yJitter = (rng.nextFloat() - 0.5F) * 5F;
        float xJitter = (rng.nextFloat() - 0.5F) * 8F;
        float ty = a.y + a.h * 0.5F - 4F + yJitter;
        float tx = a.x + 2F + xJitter;
        float split = 1.5F + rng.nextFloat() * 2.5F;

        /* RGB chromatic aberration split */
        context.batcher.text(this.supporter.name, tx - split, ty, 0xBBFF2222);
        context.batcher.text(this.supporter.name, tx + split, ty, 0xBB2222FF);
        context.batcher.text(this.supporter.name, tx, ty, Colors.WHITE);
    }

    private void renderGlitchIcon(UIContext context)
    {
        if (this.nameRow == null || this.nameRow.area.w <= 0)
        {
            return;
        }

        Area a = this.nameRow.area;
        long seed = (System.currentTimeMillis() / 100L) * (0xDEADF00DL + this.supporter.name.hashCode());
        Random rng = new Random(seed);

        /* icon sits in the last 20px on the right of the name row */
        float ix = a.ex() - 20 + (rng.nextFloat() - 0.5F) * 6F;
        float iy = a.y + (rng.nextFloat() - 0.5F) * 4F;
        float iSize = 20;

        int r = 0x44 + rng.nextInt(0x88);
        int g = 0x44 + rng.nextInt(0x88);
        int b = 0x44 + rng.nextInt(0x88);
        int color = (0xAA << 24) | (r << 16) | (g << 8) | b;

        /* displaced ghost of the icon area */
        context.batcher.box(ix, iy, ix + iSize, iy + iSize, color);

        /* second ghost at extra offset */
        float ix2 = a.ex() - 20 + (rng.nextFloat() - 0.5F) * 10F;
        float iy2 = a.y + (rng.nextFloat() - 0.5F) * 6F;
        context.batcher.box(ix2, iy2, ix2 + iSize, iy2 + iSize, color & 0x55FFFFFF);
    }

    private void renderGlitchBanner(UIContext context, Texture texture, Area a)
    {
        /* slow seed so glitch pattern changes every ~120ms, not every frame */
        Random rng = new Random((System.currentTimeMillis() / 120L) * (0xDEADBEEFL + this.supporter.name.hashCode()));
        int slices = 8 + rng.nextInt(5);
        float sliceH = a.h / (float) slices;

        for (int i = 0; i < slices; i++)
        {
            float sy = a.y + i * sliceH;
            float sh = sliceH + rng.nextFloat() * 2F;

            /* cut effect: some slices are narrower and start at the wrong x */
            float widthFactor = rng.nextFloat() < 0.25F ? (0.3F + rng.nextFloat() * 0.5F) : 1F;
            float sw = a.w * widthFactor;
            float xShift = (rng.nextFloat() - 0.5F) * a.w * 0.25F;
            float sx = a.x + xShift + (a.w - sw) * rng.nextFloat();

            /* corrupted UVs: offset the V origin and optionally flip axes */
            float vOffset = (rng.nextFloat() - 0.5F) * a.h * 0.35F;
            float u1 = rng.nextFloat() < 0.15F ? (rng.nextFloat() - 0.5F) * a.w * 0.3F : 0F;
            float v1 = i * sliceH + vOffset;
            float u2 = u1 + (rng.nextBoolean() ? sw : -sw);
            float v2 = v1 + (rng.nextBoolean() ? sh : -sh);

            context.batcher.texturedBox(texture, Colors.WHITE, sx, sy, sw, sh, u1, v1, u2, v2, (int) a.w, (int) a.h);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        Area a = this.placeholder.area;
        int color1 = this.randomColor | Colors.A100;
        int color2 = Colors.mulRGB(color1, 0.75F);

        if (this.supporter.banner != null && !this.supporter.banner.path.equals("..."))
        {
            Texture texture = BBSModClient.getTextures().getTexture(this.supporter.banner, GL11.GL_LINEAR);

            if (UIAprilFoolsOverlay.isAprilFoolsEnabled())
            {
                this.renderGlitchBanner(context, texture, a);
            }
            else
            {
                context.batcher.fullTexturedBox(texture, a.x, a.y, a.w, a.h);
            }
        }
        else
        {
            context.batcher.box(a.x, a.y, a.w, a.h, color2, color1, color1, color2);
        }
    }
}