package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;

public class UIIntegerKeyframeFactory extends UIKeyframeFactory<Integer>
{
    private UITrackpad value;
    private UIBezierHandles handles;
    private UIElement hotbarPreview;
    private Replay replay;
    private Film film;

    public UIIntegerKeyframeFactory(Keyframe<Integer> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        boolean isSelectedSlot = sheet != null && ("selected_slot".equals(sheet.id) || sheet.id.endsWith("/selected_slot"));

        this.value = new UITrackpad(this::setValue);
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);

        if (isSelectedSlot)
        {
            this.replay = this.findReplay(keyframe.getParent());
            this.film = this.findFilm(keyframe.getParent());
            this.value.limit(0, 8).integer();
            this.hotbarPreview = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    super.render(context);

                    int selected = MathUtils.clamp(UIIntegerKeyframeFactory.this.keyframe.getValue(), 0, 8);
                    int padding = 4;
                    int gap = 2;
                    int available = this.area.w - padding * 2 - gap * 8;
                    int slotW = Math.max(6, available / 9);
                    int slotH = Math.max(24, this.area.h - padding * 2);
                    int startX = this.area.x + padding;
                    int y = this.area.y + (this.area.h - slotH) / 2;

                    for (int i = 0; i < 9; i++)
                    {
                        int x = startX + i * (slotW + gap);
                        int bg = Colors.setA(Colors.DARKER_GRAY, 0.45F);

                        if (i == selected)
                        {
                            context.batcher.box(x - 1, y - 1, x + slotW + 1, y + slotH + 1, Colors.ACTIVE | Colors.A100);
                        }

                        context.batcher.box(x, y, x + slotW, y + slotH, bg);

                        ItemStack stack = ItemStack.EMPTY;

                        if (UIIntegerKeyframeFactory.this.replay != null)
                        {
                            java.util.List<ItemStack> stacks = UIIntegerKeyframeFactory.this.replay.inventory.getStacks();
                            if (!stacks.isEmpty())
                            {
                                stack = stacks.size() > i ? stacks.get(i) : ItemStack.EMPTY;
                            }
                        }

                        if ((stack == null || stack.isEmpty()) && UIIntegerKeyframeFactory.this.film != null)
                        {
                            java.util.List<ItemStack> stacks = UIIntegerKeyframeFactory.this.film.inventory.getStacks();
                            if (!stacks.isEmpty())
                            {
                                stack = stacks.size() > i ? stacks.get(i) : ItemStack.EMPTY;
                            }
                        }

                        if ((stack == null || stack.isEmpty()))
                        {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null)
                            {
                                stack = client.player.getInventory().getStack(i);
                            }
                        }

                        if (stack != null && !stack.isEmpty())
                        {
                            MatrixStack matrices = context.batcher.getContext().getMatrices();
                            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
                            int itemX = x + Math.max(0, (slotW - 16) / 2);
                            int itemY = y + Math.max(0, (slotH - 16) / 2);

                            matrices.push();
                            consumers.setUI(true);
                            context.batcher.getContext().drawItem(stack, itemX, itemY);
                            context.batcher.getContext().drawStackOverlay(context.batcher.getFont().getRenderer(), stack, itemX, itemY);
                            consumers.setUI(false);
                            matrices.pop();
                        }
                    }
                }
            };

            this.hotbarPreview.h(32);
        }

        if (this.hotbarPreview != null)
        {
            this.scroll.add(this.value, this.handles.createColumn(), this.hotbarPreview);
        }
        else
        {
            this.scroll.add(this.value, this.handles.createColumn());
        }
    }

    @Override
    public void update()
    {
        super.update();

        this.value.setValue(this.keyframe.getValue());
        this.handles.update();
    }

    private Replay findReplay(BaseValue value)
    {
        BaseValue current = value;

        while (current != null)
        {
            if (current instanceof ReplayKeyframes keyframes)
            {
                BaseValue parent = keyframes.getParent();

                if (parent instanceof Replay r)
                {
                    return r;
                }
            }

            current = current.getParent();
        }

        return null;
    }

    private Film findFilm(BaseValue value)
    {
        BaseValue current = value;

        while (current != null)
        {
            if (current instanceof Film filmValue)
            {
                return filmValue;
            }

            current = current.getParent();
        }

        return null;
    }
}
