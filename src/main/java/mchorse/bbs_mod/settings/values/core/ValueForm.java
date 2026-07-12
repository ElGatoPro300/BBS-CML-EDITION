package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueForm extends BaseValueBasic<Form>
{
    private transient int revision = 0;

    public ValueForm(String id)
    {
        super(id, null);
    }

    public int getRevision()
    {
        return this.revision;
    }

    private void bumpRevision()
    {
        this.revision += 1;
    }

    @Override
    public BaseType toData()
    {
        return this.value == null ? null : FormUtils.toData(this.value);
    }

    @Override
    public void set(Form value, int flag)
    {
        super.set(value, flag);

        this.bumpRevision();
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data != null && data.isMap())
        {
            this.value = FormUtils.fromData(data.asMap());
        }
        else
        {
            this.value = null;
        }

        this.bumpRevision();
    }
}