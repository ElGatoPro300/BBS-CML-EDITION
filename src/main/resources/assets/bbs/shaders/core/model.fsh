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

in float vertexDistance;
in vec4 vertexColor;
in vec4 rawVertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

float bbsLuminance(vec3 rgb)
{
    return dot(rgb, vec3(0.2126, 0.7152, 0.0722));
}

vec3 bbsApplyWrapPaint(vec3 litRgb, float paintStrength)
{
    if (paintStrength <= 0.001)
    {
        return litRgb;
    }

    /* Preserve fully lit luminance (diffuse + lightmap + texture AO) and apply wrap hue. */
    float lum = bbsLuminance(litRgb);
    vec3 wrapped = PaintColor.rgb * lum;

    return mix(litRgb, wrapped, paintStrength);
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

    if (texSample.a < 0.1)
    {
        discard;
    }

    /* Paint strength must not change geometry alpha; only texture + vertex tint define opacity. */
    float modelAlpha = texSample.a * rawVertexColor.a * ColorModulator.a;

    vec4 color = texSample;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color.rgb *= lightMapColor.rgb;

    vec3 litRgb = color.rgb;
    float paintStrength = clamp(abs(PaintColor.a), 0.0, 1.0);

    if (PaintColor.a > 0.001)
    {
        litRgb = bbsApplyWrapPaint(litRgb, paintStrength);
    }
    else if (PaintColor.a < -0.001)
    {
        /* Negative darken like glow */
        float factor = max(0.0, 1.0 + PaintColor.a);

        litRgb *= factor;
    }

    /* Shader-pack paint overlay pass: rgb already contains the final mix; alpha only masks geometry. */
    if (PaintOverlay > 0.5)
    {
        if (abs(PaintColor.a) < 0.001)
        {
            discard;
        }

        if (modelAlpha < 0.001)
        {
            discard;
        }

        fragColor = linear_fog(vec4(litRgb, modelAlpha), vertexDistance, FogStart, FogEnd, FogColor);

        return;
    }

    color.rgb = litRgb;
    color.a = modelAlpha;

    float strength = GlowingColor.a;

    if (abs(strength) > 0.001)
    {
        vec3 glowRgb = GlowingColor.rgb;

        if (strength > 0.0)
        {
            if (strength >= 1.0)
            {
                color.rgb += glowRgb * strength * 8.0;
            }
            else
            {
                /* Smooth ramp 0..1: blend lit surface toward emissive without double-brightening */
                vec3 emissive = color.rgb + glowRgb * 8.0;

                color.rgb = mix(color.rgb, emissive, strength);
            }
        }
        else
        {
            /* Linear darken: 0 = unchanged, -1 = fully black (smooth for keyframe animation). */
            float factor = max(0.0, 1.0 + strength);

            color.rgb *= factor;
        }
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
