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
uniform float PaintMaskShape;
uniform mat4 ColorEffectInverse;
uniform float ColorEffectActive;
uniform vec3 ColorMaskHalf;
uniform float ColorMaskBottomAnchored;
uniform float ColorMaskShape;
uniform vec4 FormColorTint;
uniform float ColorTintMasked;

in float vertexDistance;
in vec4 vertexColor;
in vec4 rawVertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 formRootPos;

out vec4 fragColor;

float bbsSdTriangle2D(vec2 p, vec2 a, vec2 b, vec2 c)
{
    vec2 e0 = b - a;
    vec2 e1 = c - b;
    vec2 e2 = a - c;
    vec2 v0 = p - a;
    vec2 v1 = p - b;
    vec2 v2 = p - c;
    vec2 pq0 = v0 - e0 * clamp(dot(v0, e0) / max(dot(e0, e0), 0.0001), 0.0, 1.0);
    vec2 pq1 = v1 - e1 * clamp(dot(v1, e1) / max(dot(e1, e1), 0.0001), 0.0, 1.0);
    vec2 pq2 = v2 - e2 * clamp(dot(v2, e2) / max(dot(e2, e2), 0.0001), 0.0, 1.0);
    float s = sign(e0.x * e2.y - e0.y * e2.x);
    vec2 d = min(min(vec2(dot(pq0, pq0), s * (v0.x * e0.y - v0.y * e0.x)),
                     vec2(dot(pq1, pq1), s * (v1.x * e1.y - v1.y * e1.x))),
                     vec2(dot(pq2, pq2), s * (v2.x * e2.y - v2.y * e2.x)));

    return -sqrt(max(d.x, 0.0)) * sign(d.y);
}

float bbsPaintEffectMask(vec3 rootPos, mat4 effectInverse, float active, vec3 halfExtents, float bottomAnchored, float shape)
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

    float dist;
    float maxHalf = max(halfExtents.x, max(halfExtents.y, halfExtents.z));

    if (shape > 1.5)
    {
        /* Front-facing triangle in XY (apex up), thickness along Z. */
        vec2 halfXY = max(halfExtents.xy, vec2(0.001));
        vec2 a = vec2(0.0, halfXY.y);
        vec2 b = vec2(-halfXY.x, -halfXY.y);
        vec2 c = vec2(halfXY.x, -halfXY.y);
        float dTri = bbsSdTriangle2D(local.xy, a, b, c);
        float dZ = abs(local.z) - halfExtents.z;

        dist = length(max(vec2(dTri, dZ), 0.0)) + min(max(dTri, dZ), 0.0);
    }
    else if (shape > 0.5)
    {
        /* Soft ellipsoid sized by half extents. */
        vec3 safeHalf = max(halfExtents, vec3(0.001));
        float radius = length(local / safeHalf);

        dist = (radius - 1.0) * maxHalf;
    }
    else
    {
        vec3 d = abs(local) - halfExtents;

        dist = length(max(d, 0.0)) + min(max(max(d.x, d.y), d.z), 0.0);
    }

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
        paintStrength *= bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored, PaintMaskShape);
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

    /* Texture cutout only (same as no-shader). Form/vertex opacity is kept — Iris packs
     * discard low vertex alpha; this shader must not. */
    if (texSample.a < 0.1)
    {
        discard;
    }

    vec4 color = texSample;
    vec4 formTint = vec4(1.0);

    if (ColorTintMasked > 0.5)
    {
        float cmask = bbsPaintEffectMask(formRootPos, ColorEffectInverse, ColorEffectActive, ColorMaskHalf, ColorMaskBottomAnchored, ColorMaskShape);

        formTint.rgb = mix(vec3(1.0), FormColorTint.rgb, cmask);
        formTint.a = mix(1.0, FormColorTint.a, cmask);
        color *= vertexColor * formTint * ColorModulator;
    }
    else
    {
        color *= vertexColor * ColorModulator;
    }

    /* Paint strength must not change geometry alpha; only texture + vertex tint define opacity. */
    float modelAlpha = texSample.a * rawVertexColor.a * formTint.a * ColorModulator.a;

    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    float paintStrength = clamp(abs(PaintColor.a), 0.0, 1.0);
    paintStrength *= bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored, PaintMaskShape);

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
        float mask = bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored, PaintMaskShape);

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
