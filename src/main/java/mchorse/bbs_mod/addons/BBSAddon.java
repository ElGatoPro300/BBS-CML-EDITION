package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterActionClipsEvent;
import mchorse.bbs_mod.events.register.RegisterCameraClipsEvent;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;

/**
 * Base class for BBS addons.
 *
 * <p>Extend this class to create a BBS addon. This class provides convenient methods
 * to register content to the mod.</p>
 */
public abstract class BBSAddon implements BBSAddonMod
{
    @Subscribe
    public void onRegisterForms(RegisterFormsEvent event)
    {
        this.registerForms(event);
    }

    @Subscribe
    public void onRegisterCameraClips(RegisterCameraClipsEvent event)
    {
        this.registerCameraClips(event);
    }

    @Subscribe
    public void onRegisterActionClips(RegisterActionClipsEvent event)
    {
        this.registerActionClips(event);
    }

    @Subscribe
    public void onRegisterSettings(RegisterSettingsEvent event)
    {
        this.registerSettings(event);
    }

    @Subscribe
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        this.registerSourcePacks(event);
    }

    protected void registerForms(RegisterFormsEvent event)
    {}

    protected void registerCameraClips(RegisterCameraClipsEvent event)
    {}

    protected void registerActionClips(RegisterActionClipsEvent event)
    {}

    protected void registerSettings(RegisterSettingsEvent event)
    {}

    protected void registerSourcePacks(RegisterSourcePacksEvent event)
    {}
}
