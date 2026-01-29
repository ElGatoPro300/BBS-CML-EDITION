package mchorse.bbs_mod.ui.addons;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.util.Util;

import java.util.stream.Collectors;

public class UIAddonsPanel extends UIDashboardPanel
{
    public UIScrollView addons;
    public UIIcon reload;

    public UIAddonsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.addons = new UIScrollView();
        this.addons.relative(this).w(1F).h(1F);

        this.reload = new UIIcon(Icons.REFRESH, (b) -> this.reload());
        this.reload.relative(this).x(1F, -20).y(0).w(20).h(20);
        this.reload.tooltip(UIKeys.ADDONS_RELOAD);

        this.add(this.addons, this.reload);

        this.reload();
    }

    public void reload()
    {
        this.addons.removeAll();

        FabricLoader.getInstance().getEntrypointContainers("bbs-addon", BBSAddonMod.class).forEach(container -> {
            this.addons.add(new UIAddonEntry(container.getProvider()));
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

        public UIAddonEntry(ModContainer mod)
        {
            this.mod = mod;
            ModMetadata meta = mod.getMetadata();

            this.h(70);
            
            UILabel name = new UILabel(IKey.raw(meta.getName()).format(Colors.WHITE));
            name.relative(this).x(10).y(10);
            
            UILabel version = new UILabel(IKey.raw("v" + meta.getVersion().getFriendlyString()).format(Colors.GRAY));
            version.relative(name).x(1F, 5).y(0);
            
            UILabel description = new UILabel(IKey.raw(meta.getDescription()).format(Colors.LIGHTER_GRAY));
            description.relative(name).x(0).y(1F, 5).w(200); // Limit width maybe?
            
            String authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
            UILabel authorLabel = new UILabel(UIKeys.ADDONS_AUTHOR.format(IKey.raw(authors)).format(Colors.LIGHTER_GRAY));
            authorLabel.relative(description).x(0).y(1F, 5);

            this.add(name, version, description, authorLabel);
            
            // Buttons
            int x = 0;
            ContactInformation contact = meta.getContact();
            
            if (contact.get("homepage").isPresent())
            {
                UIIcon web = new UIIcon(Icons.GLOBE, (b) -> openLink(contact.get("homepage").get()));
                web.tooltip(UIKeys.ADDONS_WEBSITE);
                web.relative(this).x(1F, -24 - x).y(10).w(20).h(20);
                this.add(web);
                x += 24;
            }
            
            if (contact.get("issues").isPresent())
            {
                UIIcon issues = new UIIcon(Icons.EXCLAMATION, (b) -> openLink(contact.get("issues").get()));
                issues.tooltip(UIKeys.ADDONS_ISSUES);
                issues.relative(this).x(1F, -24 - x).y(10).w(20).h(20);
                this.add(issues);
                x += 24;
            }
            
            if (contact.get("sources").isPresent())
            {
                UIIcon source = new UIIcon(Icons.CODE, (b) -> openLink(contact.get("sources").get()));
                source.tooltip(UIKeys.ADDONS_SOURCE);
                source.relative(this).x(1F, -24 - x).y(10).w(20).h(20);
                this.add(source);
                x += 24;
            }
        }
        
        private void openLink(String url)
        {
             try {
                Util.getOperatingSystem().open(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void render(UIContext context)
        {
            // Background
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A25 | Colors.DARKEST_GRAY);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50 | Colors.LIGHTER_GRAY);
            
            super.render(context);
        }
    }
}