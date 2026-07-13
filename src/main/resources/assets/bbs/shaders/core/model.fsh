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
        float outAlpha = paintStrength * texSample.a * rawVertexColor.a * ColorModulator.a;

        if (outAlpha < 0.001)
        {
            discard;
        }

        vec4 color = vec4(PaintColor.rgb, outAlpha);

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

    color.rgb *= lightMapColor.rgb;

    /* Paint replaces the lit texture toward a flat paint color; strength 1 = no skin/texture visible. */
    if (PaintColor.a > 0.001)
    {
        color.rgb = mix(color.rgb, PaintColor.rgb, paintStrength);
    }
    else if (PaintColor.a < -0.001)
    {
        /* Negative darken like glow */
        float factor = max(0.0, 1.0 + PaintColor.a);

        color.rgb *= factor;
    }

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
