package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

public class SelectorOwner
{
    public IEntity entity;

    private Form form;
    private long check;

    private LivingEntity mcEntity;

    public SelectorOwner(LivingEntity mcEntity)
    {
        this.mcEntity = mcEntity;
        this.entity = new MCEntity(mcEntity);
    }

    public Form getForm()
    {
        return form;
    }

    public void update()
    {
        World world = this.entity.getWorld();

        if (!world.isClient())
        {
            return;
        }

        this.check();
        this.entity.update();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }
    }

    public void check()
    {
        EntitySelectors selectors = BBSModClient.getSelectors();

        if (this.check < selectors.getLastUpdate())
        {
            this.check = selectors.getLastUpdate();

            EntitySelector selectorFor = selectors.getSelectorFor(this.mcEntity);

            if (selectorFor != null)
            {
                this.form = FormUtils.copy(selectorFor.form);

                if (this.form != null)
                {
                    this.form.playMain();
                }
            }
            else
            {
                this.form = null;
            }
        }
    }
}