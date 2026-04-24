package mchorse.bbs_mod.ui.addons;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UISidebarDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.Optional;
import java.util.stream.Collectors;

public class UIAddonsPanel extends UISidebarDashboardPanel
{
    public UIScrollView addons;
    public UIIcon reload;

    private static boolean registeredIcons = false;

    public UIAddonsPanel(UIDashboard dashboard)
    {
        super(dashboard);
        
        if (!registeredIcons)
        {
            BBSMod.getProvider().register(new InternalAssetsSourcePack("mod_icons", "assets", BBSMod.class));
            registeredIcons = true;
        }

        this.addons = new UIScrollView();
        this.addons.relative(this.editor).w(1F).h(1F);
        this.addons.column(5).vertical().stretch().scroll().padding(10);
        
        this.editor.add(this.addons);

        this.reload = new UIIcon(Icons.REFRESH, (b) -> this.reload());
        this.reload.tooltip(UIKeys.ADDONS_RELOAD);
        
        this.iconBar.add(this.reload);

        this.reload();
    }

    @Override
    public void requestNames()
    {}

    public void reload()
    {
        this.addons.removeAll();

        var containers = FabricLoader.getInstance().getEntrypointContainers("bbs-addon", BBSAddonMod.class);
        System.out.println("BBS Addons Debug: Found " + containers.size() + " containers for 'bbs-addon'");

        containers.forEach(container -> {
            System.out.println("BBS Addons Debug: Adding container " + container.getProvider().getMetadata().getId());
            UIAddonEntry entry = new UIAddonEntry(container.getProvider());
            this.addons.add(entry);
        });

        if (this.addons.getChildren().isEmpty())
        {
             UILabel noAddons = new UILabel(UIKeys.ADDONS_NO_ADDONS);
             noAddons.color(Colors.LIGHTER_GRAY);
             noAddons.relative(this.addons).x(0.5F).y(0.5F).anchor(0.5F, 0.5F);
             this.addons.add(noAddons);
        }
        
        this.addons.resize();
    }

    public static class UIAddonEntry extends UIElement
    {
        public ModContainer mod;
        public Texture icon;

        public UIAddonEntry(ModContainer mod)
        {
            this.mod = mod;
            ModMetadata meta = mod.getMetadata();

            this.h(80);
            
            /* Try to load icon */
            Optional<String> iconPath = meta.getIconPath(64);
            if (iconPath.isPresent())
            {
                String path = iconPath.get();
                if (path.startsWith("assets/"))
                {
                    String relative = path.substring("assets/".length());
                    this.icon = BBSModClient.getTextures().getTexture(new Link("mod_icons", relative));
                }
            }

            int textX = this.icon != null ? 80 : 10;
            
            UILabel version = new UILabel(IKey.raw("v" + meta.getVersion().getFriendlyString()).format(Colors.GRAY));
            version.relative(this).x(1F, -10).y(10).anchorX(1F);
            
            UILabel description = new UILabel(IKey.raw(meta.getDescription()).format(Colors.LIGHTER_GRAY));
            description.relative(this).x(textX).y(30).w(1F, -10 - textX);
            
            String authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
            UILabel authorLabel = new UILabel(UIKeys.ADDONS_AUTHOR.format(IKey.raw(authors)).format(Colors.LIGHTER_GRAY));
            authorLabel.relative(this).x(textX).y(1F, -10).anchorY(1F).w(1F, -10 - textX);

            this.add(version, description, authorLabel);
            
            // Buttons
            int x = 0;
            ContactInformation contact = meta.getContact();
            
            if (contact.get("homepage").isPresent())
            {
                UIIcon web = new UIIcon(Icons.GLOBE, (b) -> openLink(contact.get("homepage").get()));
                web.tooltip(UIKeys.ADDONS_WEBSITE);
                web.relative(this).x(1F, -10 - x).y(1F, -5).anchor(1F, 1F).w(20).h(20);
                this.add(web);
                x += 24;
            }
            
            if (contact.get("issues").isPresent())
            {
                UIIcon issues = new UIIcon(Icons.EXCLAMATION, (b) -> openLink(contact.get("issues").get()));
                issues.tooltip(UIKeys.ADDONS_ISSUES);
                issues.relative(this).x(1F, -10 - x).y(1F, -5).anchor(1F, 1F).w(20).h(20);
                this.add(issues);
                x += 24;
            }
            
            if (contact.get("sources").isPresent())
            {
                UIIcon source = new UIIcon(Icons.CODE, (b) -> openLink(contact.get("sources").get()));
                source.tooltip(UIKeys.ADDONS_SOURCE);
                source.relative(this).x(1F, -10 - x).y(1F, -5).anchor(1F, 1F).w(20).h(20);
                this.add(source);
                x += 24;
            }
        }
        
        private void openLink(String url)
        {
             try {
                net.minecraft.util.Util.getOperatingSystem().open(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void render(UIContext context)
        {
            // Background
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50 | Colors.LIGHTER_GRAY);
            
            if (this.icon != null)
            {
                context.batcher.fullTexturedBox(this.icon, this.area.x + 10, this.area.y + 10, 60, 60);
            }
            
            /*  Draw Name Scaled */
            int textX = this.icon != null ? 80 : 10;
            String name = this.mod.getMetadata().getName();
            
            context.batcher.getContext().getMatrices().push();
            context.batcher.getContext().getMatrices().translate(this.area.x + textX, this.area.y + 10, 0);
            context.batcher.getContext().getMatrices().scale(1.5F, 1.5F, 1.5F);
            context.batcher.text(name, 0, 0, Colors.WHITE);
            context.batcher.getContext().getMatrices().pop();

            super.render(context);
        }
    }
}