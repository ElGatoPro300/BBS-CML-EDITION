package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.forms.values.ModelTransformMode;

public class ValueModelTransformationMode extends BaseValueBasic<ModelTransformMode>
{
    public ValueModelTransformationMode(String id, ModelTransformMode value)
    {
        super(id, value);
    }

    @Override
    public BaseType toData()
    {
        return new StringType((this.value == null ? ModelTransformMode.NONE : this.value).asString());
    }

    @Override
    public void fromData(BaseType data)
    {
        String string = data.isString() ? data.asString() : "";

        this.set(ModelTransformMode.NONE);

        for (ModelTransformMode value : ModelTransformMode.values())
        {
            if (value.asString().equals(string))
            {
                this.set(value);

                break;
            }
        }
    }
}
