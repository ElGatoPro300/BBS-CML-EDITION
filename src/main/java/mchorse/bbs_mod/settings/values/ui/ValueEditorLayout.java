package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.MathUtils;

public class ValueEditorLayout extends BaseValue
{
    /* private boolean horizontal;
    private boolean layoutLocked;
    private boolean mainOnTop;
    private boolean mainOnLeft; */
    public static final int LAYOUT_HORIZONTAL_BOTTOM = 0;
    public static final int LAYOUT_HORIZONTAL_TOP = 1;
    public static final int LAYOUT_VERTICAL_LEFT = 2;
    public static final int LAYOUT_VERTICAL_RIGHT = 3;
    public static final int LAYOUT_VERTICAL_MIDDLE = 4;

    private int layout = LAYOUT_HORIZONTAL_BOTTOM;
    private boolean layoutLocked;
    private float mainSizeH = 0.66F;
    private float mainSizeV = 0.66F;
    private float editorSizeH = 0.5F;
    private float editorSizeV = 0.5F;
    private float stateEditorSizeH = 0.7F;
    private float stateEditorSizeV = 0.25F;
    private boolean middleLayoutInverted;
    private boolean verticalLayoutInverted;
    /* private float newFilmSidebarSize = 0.25F;
    private float newFilmMainSizeH = 0.5F;
    private int filmLayoutMode; */

    public ValueEditorLayout(String id)
    {
        super(id);
    }

    public void setHorizontal(boolean horizontal)
    {
        /* BaseValue.edit(this, (v) -> this.horizontal = horizontal); */
    }

    public void setLayout(int layout)
    {
        BaseValue.edit(this, (v) -> this.layout = clampLayout(layout));
    }

    public int getLayout()
    {
        return this.layout;
    }

    public void setLayoutLocked(boolean locked)
    {
        BaseValue.edit(this, (v) -> this.layoutLocked = locked);
    }

    public boolean isLayoutLocked()
    {
        return this.layoutLocked;
    }

    /* public void setLayoutLocked(boolean layoutLocked)
    {
        BaseValue.edit(this, (v) -> this.layoutLocked = layoutLocked);
    } */

    /* public void setMainOnTop(boolean mainOnTop)
    {
        BaseValue.edit(this, (v) -> this.mainOnTop = mainOnTop);
    } */

    /* public void setMainOnLeft(boolean mainOnLeft)
    {
        BaseValue.edit(this, (v) -> this.mainOnLeft = mainOnLeft);
    } */

    public void setMainSizeH(float mainSizeH)
    {
        BaseValue.edit(this, (v) -> this.mainSizeH = mainSizeH);
    }

    public void setMainSizeV(float mainSizeV)
    {
        BaseValue.edit(this, (v) -> this.mainSizeV = mainSizeV);
    }

    public void setEditorSizeH(float editorSizeH)
    {
        BaseValue.edit(this, (v) -> this.editorSizeH = editorSizeH);
    }

    public void setEditorSizeV(float editorSizeV)
    {
        BaseValue.edit(this, (v) -> this.editorSizeV = editorSizeV);
    }

    /* public void setNewFilmSidebarSize(float sidebarSize)
    {
        BaseValue.edit(this, (v) -> this.newFilmSidebarSize = sidebarSize);
    }

    public void setNewFilmMainSizeH(float mainSizeH)
    {
        BaseValue.edit(this, (v) -> this.newFilmMainSizeH = mainSizeH);
    }

    public void setFilmLayoutMode(int filmLayoutMode)
    {
        BaseValue.edit(this, (v) -> this.filmLayoutMode = filmLayoutMode);
    } */

    public void setStateEditorSizeH(float editorSizeH)
    {
        BaseValue.edit(this, (v) -> this.stateEditorSizeH = editorSizeH);
    }

    public void setStateEditorSizeV(float editorSizeV)
    {
        BaseValue.edit(this, (v) -> this.stateEditorSizeV = editorSizeV);
    }

    public boolean isHorizontal()
    {
        return this.layout == LAYOUT_HORIZONTAL_BOTTOM || this.layout == LAYOUT_HORIZONTAL_TOP;
        //return this.horizontal;
    }

    /* public boolean isLayoutLocked()
    {
        return this.layoutLocked;
    } */

    public boolean isMainOnTop()
    {
        return this.layout == LAYOUT_HORIZONTAL_TOP;
        //return this.mainOnTop;
    }

    public boolean isMainOnLeft()
    {
        return this.layout == LAYOUT_VERTICAL_LEFT || this.layout == LAYOUT_VERTICAL_MIDDLE;
        //return this.mainOnLeft;
    }

    public boolean isMiddleLayout()
    {
        return this.layout == LAYOUT_VERTICAL_MIDDLE;
        
    }

    public float getMainSizeH()
    {
        return this.mainSizeH;
    }

    public float getMainSizeV()
    {
        return this.mainSizeV;
    }

    public float getEditorSizeH()
    {
        return this.editorSizeH;
    }

    public float getEditorSizeV()
    {
        return this.editorSizeV;
    }

    /* public float getNewFilmSidebarSize()
    {
        return MathUtils.clamp(this.newFilmSidebarSize, 0.1F, 0.6F);
    }

    public float getNewFilmMainSizeH()
    {
        return MathUtils.clamp(this.newFilmMainSizeH, 0.15F, 0.7F);
    }

    public int getFilmLayoutMode()
    {
        return MathUtils.clamp(this.filmLayoutMode, 0, 2);
    } */

    public float getStateEditorSizeH()
    {
        return MathUtils.clamp(this.stateEditorSizeH, 0.1F, 0.9F);
    }

    public float getStateEditorSizeV()
    {
        return MathUtils.clamp(this.stateEditorSizeV, 0.1F, 0.9F);
    }

    public void setMiddleLayoutInverted(boolean middleLayoutInverted)
    {
        BaseValue.edit(this, (v) -> this.middleLayoutInverted = middleLayoutInverted);
    }

    public boolean isMiddleLayoutInverted()
    {
        return this.middleLayoutInverted;
    }

    public void setVerticalLayoutInverted(boolean verticalLayoutInverted)
    {
        BaseValue.edit(this, (v) -> this.verticalLayoutInverted = verticalLayoutInverted);
    }

    public boolean isVerticalLayoutInverted()
    {
        return this.verticalLayoutInverted;
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        // data.putBool("horizontal", this.horizontal);
        data.putInt("layout", this.layout);
        data.putBool("horizontal", this.isHorizontal());
        data.putBool("layout_locked", this.layoutLocked);
        /* data.putBool("main_on_top", this.mainOnTop);
        data.putBool("main_on_left", this.mainOnLeft); */
        data.putFloat("main_size_h", this.mainSizeH);
        data.putFloat("main_size_v", this.mainSizeV);
        data.putFloat("editor_size_h", this.editorSizeH);
        data.putFloat("editor_size_v", this.editorSizeV);
        data.putFloat("state_editor_size_h", this.stateEditorSizeH);
        data.putFloat("state_editor_size_v", this.stateEditorSizeV);
        data.putBool("middle_layout_inverted", this.middleLayoutInverted);
        data.putBool("vertical_layout_inverted", this.verticalLayoutInverted);
        /* data.putFloat("new_film_sidebar_size", this.newFilmSidebarSize);
        data.putFloat("new_film_main_size_h", this.newFilmMainSizeH);
        data.putInt("film_layout_mode", this.filmLayoutMode); */

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("layout"))
            {
                this.layout = clampLayout(map.getInt("layout"));
            }
            else
            {
                this.layout = map.getBool("horizontal") ? LAYOUT_HORIZONTAL_BOTTOM : LAYOUT_VERTICAL_LEFT;
            }
            this.layoutLocked = map.getBool("layout_locked", false);

            /* this.horizontal = map.getBool("horizontal");
            this.layoutLocked = map.getBool("layout_locked");
            this.mainOnTop = map.getBool("main_on_top");
            this.mainOnLeft = map.getBool("main_on_left"); */
            this.mainSizeH = map.getFloat("main_size_h", 0.66F);
            this.mainSizeV = map.getFloat("main_size_v", 0.66F);
            this.editorSizeH = map.getFloat("editor_size_h", 0.5F);
            this.editorSizeV = map.getFloat("editor_size_v", 0.5F);
            this.stateEditorSizeH = map.getFloat("state_editor_size_h", 0.7F);
            this.stateEditorSizeV = map.getFloat("state_editor_size_v", 0.25F);
            this.middleLayoutInverted = map.getBool("middle_layout_inverted", false);
            this.verticalLayoutInverted = map.getBool("vertical_layout_inverted", false);
            /* this.newFilmSidebarSize = map.getFloat("new_film_sidebar_size", 0.25F);
            this.newFilmMainSizeH = map.getFloat("new_film_main_size_h", 0.5F);
            this.filmLayoutMode = map.getInt("film_layout_mode", 0); */
        }
    }

    private int clampLayout(int layout)
    {
        //if (layout < LAYOUT_HORIZONTAL_BOTTOM || layout > LAYOUT_VERTICAL_RIGHT)
        if (layout < LAYOUT_HORIZONTAL_BOTTOM || layout > LAYOUT_VERTICAL_MIDDLE)
        {
            return LAYOUT_HORIZONTAL_BOTTOM;
        }

        return layout;
    }
}