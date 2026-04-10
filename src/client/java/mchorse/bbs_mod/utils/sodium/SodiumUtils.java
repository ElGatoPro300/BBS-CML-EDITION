package mchorse.bbs_mod.utils.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexSodiumConsumer;
import mchorse.bbs_mod.utils.colors.Color;

public class SodiumUtils
{
    public static VertexConsumer createVertexBuffer(VertexConsumer b, Color color)
    {
        return new RecolorVertexSodiumConsumer(b, color);
    }
}