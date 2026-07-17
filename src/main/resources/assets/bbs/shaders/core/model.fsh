#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float TextureBlendFactor;
uniform float TextureBlendActive;

/* rgb = paint color, a = paint strength (0 = off, 1 = full override). PaintOverlay = 1 during Iris second pass. */
uniform vec4 PaintColor;
uniform vec4 GlowingColor;
uniform float PaintOverlay;
uniform float GlowPaintOnly;

uniform mat4 PaintEffectInverse;
uniform float PaintEffectActive;
uniform vec3 PaintMaskHalf;
uniform float PaintMaskBottomAnchored;

in float vertexDistance;
in vec4 vertexColor;
in vec4 rawVertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 formRootPos;

out vec4 fragColor;

float bbsPaintEffectMask(vec3 rootPos, mat4 effectInverse, float active, vec3 halfExtents, float bottomAnchored)
{
    if (active < 0.5)
    {
        return 1.0;
    }

    vec3 local = (effectInverse * vec4(rootPos, 1.0)).xyz;

    if (bottomAnchored > 0.5)
    {
        local.y -= halfExtents.y;
    }

    vec3 d = abs(local) - halfExtents;
    float dist = length(max(d, 0.0)) + min(max(max(d.x, d.y), d.z), 0.0);
    float maxHalf = max(halfExtents.x, max(halfExtents.y, halfExtents.z));
    float falloff = max(maxHalf * 0.15, 0.1);

    return 1.0 - smoothstep(0.0, falloff, dist);
}

vec3 bbsApplyGlow(vec3 color, float strength)
{
    if (abs(strength) < 0.001)
    {
        return color;
    }

    vec3 glowRgb = GlowingColor.rgb;

    if (strength > 0.0)
    {
        if (strength >= 1.0)
        {
            return color + glowRgb * strength * 8.0;
        }

        vec3 emissive = color + glowRgb * 8.0;

        return mix(color, emissive, strength);
    }

    float factor = max(0.0, 1.0 + strength);

    return color * factor;
}

void main()
{
    vec4 texSample = texture(Sampler0, texCoord0);

    if (TextureBlendActive > 0.5)
    {
        vec4 texBlend = texture(Sampler3, texCoord0);
        float blendFactor = TextureBlendFactor;
        float fromA = texSample.a;
        float toA = texBlend.a;

        texSample.rgb = mix(texSample.rgb, texBlend.rgb, blendFactor);
        /* Per-pixel crossfade: shared opaque pixels stay solid; exclusive pixels fade independently. */
        texSample.a = fromA * (1.0 - blendFactor) + toA * blendFactor;
    }

    /* Shader-pack paint overlay pass: alpha-blend paint RGB over the Iris first pass.
       Matches the no-shader mix: final = mix(litTextureRgb, paintRgb, paintStrength). */
    if (PaintOverlay > 0.5)
    {
        if (abs(PaintColor.a) < 0.001)
        {
            discard;
        }

        if (texSample.a < 0.1)
        {
            discard;
        }

        float paintStrength = clamp(abs(PaintColor.a), 0.0, 1.0);
        paintStrength *= bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored);
        float outAlpha = paintStrength * texSample.a * rawVertexColor.a * ColorModulator.a;

        if (outAlpha < 0.001)
        {
            discard;
        }

        vec3 outRgb = PaintColor.rgb;

        if (abs(GlowingColor.a) > 0.001)
        {
            float glowStrength = GlowingColor.a;

            if (GlowPaintOnly > 0.5)
            {
                glowStrength *= paintStrength;
            }

            outRgb = bbsApplyGlow(outRgb, glowStrength);
        }

        vec4 color = vec4(outRgb, outAlpha);

        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);

        return;
    }

    if (texSample.a < 0.1)
    {
        discard;
    }

    /* Paint strength must not change geometry alpha; only texture + vertex tint define opacity. */
    float modelAlpha = texSample.a * rawVertexColor.a * ColorModulator.a;

    vec4 color = texSample;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    float paintStrength = clamp(abs(PaintColor.a), 0.0, 1.0);
    paintStrength *= bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored);

    color.rgb *= lightMapColor.rgb;

    /* Paint replaces the lit texture toward a flat paint color; strength 1 = no skin/texture visible. */
    if (PaintColor.a > 0.001)
    {
        color.rgb = mix(color.rgb, PaintColor.rgb, paintStrength);
    }
    else if (PaintColor.a < -0.001)
    {
        /* Negative darken like glow; mask 0 must leave the surface unchanged (same as positive). */
        float factor = max(0.0, 1.0 + PaintColor.a);
        float mask = bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored);

        color.rgb = mix(color.rgb, color.rgb * factor, mask);
    }

    color.a = modelAlpha;

    float strength = GlowingColor.a;

    if (GlowPaintOnly > 0.5)
    {
        strength *= paintStrength;
    }

    color.rgb = bbsApplyGlow(color.rgb, strength);

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
