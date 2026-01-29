package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;

/**
 * Base class for BBS client addons.
 *
 * <p>Use this class for client-side only addons.
 * In fabric.mod.json, register this using "bbs-addon-client" entrypoint.</p>
 */
public abstract class BBSClientAddon implements BBSAddonMod
{
    @Subscribe
    public void onRegisterClientSettings(RegisterClientSettingsEvent event)
    {
        this.registerClientSettings(event);
    }

    @Subscribe
    public void onRegisterDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        this.registerDashboardPanels(event);
    }

    @Subscribe
    public void onRegisterFormCategories(RegisterFormCategoriesEvent event)
    {
        this.registerFormCategories(event);
    }

    @Subscribe
    public void onRegisterL10n(RegisterL10nEvent event)
    {
        this.registerL10n(event);
    }

    protected void registerClientSettings(RegisterClientSettingsEvent event)
    {}

    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {}

    protected void registerFormCategories(RegisterFormCategoriesEvent event)
    {}

    protected void registerL10n(RegisterL10nEvent event)
    {}
}
