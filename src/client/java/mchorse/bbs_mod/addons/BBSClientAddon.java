package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.events.register.RegisterImportersEvent;
import mchorse.bbs_mod.events.register.RegisterInterpolationsEvent;
import mchorse.bbs_mod.events.register.RegisterIconsEvent;
import mchorse.bbs_mod.events.register.RegisterUIKeyframeFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.events.register.RegisterFormEditorsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.events.register.RegisterParticleComponentsEvent;
import mchorse.bbs_mod.events.register.RegisterShadersEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.events.register.RegisterKeyframeShapesEvent;
import mchorse.bbs_mod.events.register.RegisterUIValueFactoriesEvent;

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

    @Subscribe
    public void onRegisterImporters(RegisterImportersEvent event)
    {
        this.registerImporters(event);
    }

    @Subscribe
    public void onRegisterParticleComponents(RegisterParticleComponentsEvent event)
    {
        this.registerParticleComponents(event);
    }

    protected void registerClientSettings(RegisterClientSettingsEvent event)
    {}

    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {}

    protected void registerFormCategories(RegisterFormCategoriesEvent event)
    {}

    protected void registerL10n(RegisterL10nEvent event)
    {}

    protected void registerImporters(RegisterImportersEvent event)
    {}

    protected void registerParticleComponents(RegisterParticleComponentsEvent event)
    {}

    @Subscribe
    public void onRegisterInterpolations(RegisterInterpolationsEvent event)
    {
        this.registerInterpolations(event);
    }

    @Subscribe
    public void onRegisterFormsRenderers(RegisterFormsRenderersEvent event)
    {
        this.registerFormsRenderers(event);
    }

    @Subscribe
    public void onRegisterIcons(RegisterIconsEvent event)
    {
        this.registerIcons(event);
    }

    @Subscribe
    public void onRegisterUIKeyframeFactories(RegisterUIKeyframeFactoriesEvent event)
    {
        this.registerUIKeyframeFactories(event);
    }

    protected void registerInterpolations(RegisterInterpolationsEvent event)
    {}

    protected void registerFormsRenderers(RegisterFormsRenderersEvent event)
    {}

    protected void registerIcons(RegisterIconsEvent event)
    {}

    protected void registerUIKeyframeFactories(RegisterUIKeyframeFactoriesEvent event)
    {}

    @Subscribe
    public void onRegisterFormEditors(RegisterFormEditorsEvent event)
    {
        this.registerFormEditors(event);
    }

    protected void registerFormEditors(RegisterFormEditorsEvent event)
    {}

    @Subscribe
    public void onRegisterKeyframeShapes(RegisterKeyframeShapesEvent event)
    {
        this.registerKeyframeShapes(event);
    }

    protected void registerKeyframeShapes(RegisterKeyframeShapesEvent event)
    {}

    @Subscribe
    public void onRegisterUIValueFactories(RegisterUIValueFactoriesEvent event)
    {
        this.registerUIValueFactories(event);
    }

    protected void registerUIValueFactories(RegisterUIValueFactoriesEvent event)
    {}
}
