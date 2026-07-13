#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/* rgb = paint color, a = paint strength (0 = off, 1 = full override). PaintOverlay = 1 during Iris second pass. */
uniform vec4 PaintColor;
uniform float PaintOverlay;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

void main()
{
    vec4 texSample = texture(Sampler0, texCoord0);

    if (texSample.a < 0.1)
    {
        discard;
    }

    /* Shader-pack paint overlay pass: blend paint over the first pass on textured pixels only. */
    if (PaintOverlay > 0.5)
    {
        if (PaintColor.a < 0.001 || texSample.a < 0.001)
        {
            discard;
        }

        vec4 color = vec4(PaintColor.rgb, PaintColor.a * texSample.a);
        color *= lightMapColor;

        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);

        return;
    }

    vec4 color = texSample;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    /* No-shader path: paint strength blends over texture RGB; model alpha stays untouched. */
    if (PaintColor.a > 0.001)
    {
        color.rgb = mix(color.rgb, PaintColor.rgb, PaintColor.a);
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
