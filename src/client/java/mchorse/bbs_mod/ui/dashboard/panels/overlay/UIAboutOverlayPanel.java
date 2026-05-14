package mchorse.bbs_mod.ui.dashboard.panels.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.supporters.Supporter;
import mchorse.bbs_mod.ui.supporters.UISupporterBanner;
import mchorse.bbs_mod.ui.supporters.Supporters;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class UIAboutOverlayPanel extends UIOverlayPanel
{
    private static final int BANNER_H = 70;
    private static final int NAME_H = 20;
    private static final int CARD_H = BANNER_H + NAME_H;
    private static final int GRID_COLS = 3;
    private static final int SECTION_H = 20;

    public UIAboutOverlayPanel(IKey title, UIDashboard dashboard)
    {
        super(title);
        this.resizable();

        Supporters supporters = new Supporters();
        supporters.setup();

        UIScrollView scroll = new UIScrollView();
        scroll.relative(this.content).w(1F).h(1F);
        scroll.column(0).vertical().stretch().scroll().padding(0);

        /* Header */
        UIElement header = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                int primary = BBSSettings.primaryColor.get();
                int y = this.area.y;
                int h = this.area.h;

                context.batcher.gradientVBox(this.area.x, y, this.area.ex(), y + h / 2, primary | Colors.A100, primary | Colors.A50);
                context.batcher.gradientVBox(this.area.x, y + h / 2, this.area.ex(), this.area.ey(), primary | Colors.A50, 0);

                /* Brand */
                int scale2y = y + 14;
                context.batcher.getContext().getMatrices().push();
                context.batcher.getContext().getMatrices().translate(this.area.mx(), scale2y, 0);
                context.batcher.getContext().getMatrices().scale(2.5F, 2.5F, 1F);
                context.batcher.textShadow("BBS CML Edition", -context.batcher.getFont().getWidth("BBS CML Edition") / 2, 0, Colors.WHITE);
                context.batcher.getContext().getMatrices().pop();

                /* Version */
                String ver = "Version " + BBSMod.VERSION;
                int vw = context.batcher.getFont().getWidth(ver);
                context.batcher.textShadow(ver, this.area.mx() - vw / 2, y + 40, 0xAAFFFFFF);

                super.render(context);
            }
        };
        header.h(70);

        /* Social icon row */
        UIElement socials = new UIElement();
        socials.h(24).row(6).resize();

        socials.add(this.socialIcon(Icons.GLOBE, UIKeys.SUPPORTERS_COMMUNITY_LINK));
        socials.add(this.socialIcon(Icons.YOUTUBE, UIKeys.SUPPORTERS_TUTORIALS_LINK));
        socials.add(this.socialIcon(Icons.FILE, UIKeys.SUPPORTERS_WIKI_LINK));
        socials.add(this.socialIcon(Icons.HEART_ALT, UIKeys.SUPPORTERS_DONATE_LINK));

        UIElement socialsWrapper = new UIElement();
        socialsWrapper.h(34).column(5).vertical().stretch().padding(5);
        socialsWrapper.add(socials);

        /* Section: Developers */
        UIElement devSection = this.buildSection(
            UIKeys.SUPPORTERS_CML_DEVELOPERS,
            0xFF_9932CC,
            supporters.getCMLDevelopers()
        );

        /* Section: Animators / Founders */
        UIElement animSection = this.buildSection(
            UIKeys.SUPPORTERS_CML_ANIMATORS,
            0xFF_FF8C00,
            supporters.getCMLSupporters()
        );

        /* Section: Special Thanks */
        UIElement thanksSection = this.buildSection(
            UIKeys.SUPPORTERS_SPECIAL_THANKS,
            0xFF_1E90FF,
            supporters.getSpecialThanksSupporters()
        );

        scroll.add(header, socialsWrapper, devSection, animSection, thanksSection);
        this.content.add(scroll);
        this.markContainer();
    }

    private UIIcon socialIcon(Icon icon, IKey linkKey)
    {
        UIIcon btn = new UIIcon(icon, (b) -> UIUtils.openWebLink(linkKey.get()));
        btn.w(20).h(20);
        return btn;
    }

    private UIElement buildSection(IKey labelKey, int color, List<Supporter> list)
    {
        UIElement section = new UIElement();
        section.column(4).vertical().stretch().padding(8);

        UIElement sectionLabel = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, color | Colors.A25);

                String text = labelKey.get().toUpperCase();
                int tw = context.batcher.getFont().getWidth(text);
                int ty = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(text, this.area.mx() - tw / 2, ty, Colors.WHITE);
                super.render(context);
            }
        };
        sectionLabel.h(SECTION_H);

        UIElement grid = new UIElement();
        grid.grid(4).items(GRID_COLS);

        for (Supporter supporter : list)
        {
            grid.add(new UIBannerCard(supporter));
        }

        section.add(sectionLabel, grid);

        return section;
    }

    /* ------------------------------------------------------------------ */

    public static class UIBannerCard extends UIClickable<UIBannerCard>
    {
        private final Supporter supporter;

        public UIBannerCard(Supporter supporter)
        {
            super(null);

            this.supporter = supporter;
            this.callback = (b) ->
            {
                if (!supporter.link.isEmpty())
                {
                    UIUtils.openWebLink(supporter.link);
                }
            };
            this.h(CARD_H);
        }

        @Override
        protected UIBannerCard get()
        {
            return this;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            int bx = this.area.x;
            int by = this.area.y;
            int bw = this.area.w;

            /* Banner image or gradient placeholder */
            if (this.supporter.banner != null)
            {
                Texture texture = BBSModClient.getTextures().getTexture(this.supporter.banner, GL11.GL_LINEAR);

                context.batcher.fullTexturedBox(texture, bx, by, bw, BANNER_H);
            }
            else
            {
                int c = Colors.HSVtoRGB((float) Math.abs(this.supporter.name.hashCode() % 360) / 360F, 0.7F, 0.8F).getARGBColor() | Colors.A100;
                context.batcher.box(bx, by, bx + bw, by + BANNER_H, Colors.mulRGB(c, 0.75F));
            }

            /* Hover dim */
            if (this.hover)
            {
                context.batcher.box(bx, by, bx + bw, by + BANNER_H, Colors.A25);
            }

            /* Name strip */
            int stripY = by + BANNER_H;
            context.batcher.box(bx, stripY, bx + bw, stripY + NAME_H, Colors.A75);

            String name = this.supporter.name;
            int maxW = bw - 4;
            int nameW = context.batcher.getFont().getWidth(name);

            if (nameW > maxW)
            {
                while (name.length() > 1 && context.batcher.getFont().getWidth(name + "..") > maxW)
                {
                    name = name.substring(0, name.length() - 1);
                }
                name += "..";
            }

            int ty = stripY + (NAME_H - context.batcher.getFont().getHeight()) / 2;
            context.batcher.textShadow(name, bx + 4, ty, Colors.WHITE);

            /* Link icon */
            Icon linkIcon = UISupporterBanner.getFromLink(this.supporter.link);
            if (linkIcon != Icons.NONE)
            {
                context.batcher.icon(linkIcon, Colors.WHITE, bx + bw - 16, stripY + (NAME_H - 16) / 2);
            }

            /* Outline */
            int border = this.hover
                ? BBSSettings.primaryColor(Colors.A100)
                : Colors.setA(Colors.WHITE, 0.1F);
            context.batcher.outline(bx, by, bx + bw, by + CARD_H, border);
        }
    }
}
