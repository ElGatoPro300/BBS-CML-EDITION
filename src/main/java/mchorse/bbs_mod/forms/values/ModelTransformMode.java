package mchorse.bbs_mod.forms.values;

public enum ModelTransformMode
{
    NONE("none"),
    GUI("gui"),
    THIRD_PERSON_LEFT_HAND("third_person_left_hand"),
    THIRD_PERSON_RIGHT_HAND("third_person_right_hand"),
    FIRST_PERSON_LEFT_HAND("first_person_left_hand"),
    FIRST_PERSON_RIGHT_HAND("first_person_right_hand"),
    GROUND("ground");

    private final String id;

    ModelTransformMode(String id)
    {
        this.id = id;
    }

    public String asString()
    {
        return this.id;
    }
}
