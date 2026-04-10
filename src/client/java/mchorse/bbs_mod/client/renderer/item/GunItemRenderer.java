package mchorse.bbs_mod.client.renderer.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import org.joml.Vector3fc;

public class GunItemRenderer implements SpecialModelRenderer<ItemStack>
{
    private Map<ItemStack, Item> map = new HashMap<>();

    public void update()
    {
        Iterator<Item> it = this.map.values().iterator();

        while (it.hasNext())
        {
            Item item = it.next();

            if (item.expiration <= 0)
            {
                it.remove();
            }

            item.expiration -= 1;
            item.properties.update(item.formEntity);
            item.formEntity.update();
        }
    }

    @Override
    public ItemStack extractArgument(ItemStack stack)
    {
        return stack;
    }

    @Override
    public void submit(ItemStack data, PoseStack matrices, SubmitNodeCollector queue, int light, int overlay, boolean hasGlint, int seed)
    {
        Item item = this.get(data);
        ItemDisplayContext mode = ItemDisplayContext.FIXED;

        if (item != null)
        {
            GunProperties properties = item.properties;
            Form form = properties.getForm(mode);
            Transform transform = properties.getTransform(mode);
            boolean zoom = mode.firstPerson() && BBSModClient.getGunZoom() != null && properties.getZoomForm() != null;

            if (zoom)
            {
                form = properties.getZoomForm();
                transform = properties.zoomTransform;
            }

            /* Preview zoom form */
            if (UIScreen.getCurrentMenu() instanceof UIModelBlockEditorMenu editorMenu && editorMenu.currentSection == editorMenu.sectionZoom)
            {
                form = editorMenu.getGunProperties().getZoomForm();
                transform = editorMenu.getGunProperties().zoomTransform;
            }

            if (form != null)
            {
                item.expiration = 20;

                matrices.pushPose();
                matrices.translate(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                GlStateManager._enableDepthTest();

                if (mode == ItemDisplayContext.GUI)
                {
                    // GUI diffuse helper moved in 1.21.11 pipeline.
                }

                int maxLight = 240;
                FormUtilsClient.render(form, new FormRenderingContext()
                    .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, maxLight, overlay, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))
                    .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));

                if (mode == ItemDisplayContext.GUI)
                {
                    // Keep compatibility with newer pipeline API where GUI depth-light toggle was removed.
                }

                GlStateManager._disableDepthTest();

                matrices.popPose();
            }
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer)
    {}

    public Item get(ItemStack stack)
    {
        if (stack == null || stack.getItem() != BBSMod.GUN_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        Item item = new Item(GunProperties.get(stack));

        this.map.put(stack, item);

        return item;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack>
    {
        public static final MapCodec<mchorse.bbs_mod.client.renderer.item.GunItemRenderer.Unbaked> CODEC = MapCodec.unit(new mchorse.bbs_mod.client.renderer.item.GunItemRenderer.Unbaked());

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<ItemStack>> type()
        {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext config)
        {
            return BBSModClient.getGunItemRenderer();
        }
    }

    public static class Item
    {
        public GunProperties properties;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(GunProperties properties)
        {
            this.properties = properties;
            this.formEntity = new StubEntity(Minecraft.getInstance().level);
        }
    }
}

