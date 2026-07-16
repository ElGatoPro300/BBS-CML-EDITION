#version 150

uniform sampler2D Sampler0;

uniform mat4 PaintEffectInverse;
uniform float PaintEffectActive;
uniform vec3 PaintMaskHalf;
uniform float PaintMaskBottomAnchored;
uniform vec4 GlowOverlayColor;

in vec4 vertexColor;
in vec2 texCoord0;
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

void main()
{
    vec4 tex = texture(Sampler0, texCoord0);
    float mask = bbsPaintEffectMask(formRootPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf, PaintMaskBottomAnchored);
    float alpha = tex.a * vertexColor.a * mask;

    if (alpha < 0.01)
    {
        discard;
    }

    vec3 color = vertexColor.rgb;
    float glowStrength = GlowOverlayColor.a;

    if (glowStrength > 0.001)
    {
        if (glowStrength >= 1.0)
        {
            color += GlowOverlayColor.rgb * glowStrength * 8.0;
        }
        else
        {
            vec3 emissive = color + GlowOverlayColor.rgb * 8.0;

            color = mix(color, emissive, glowStrength);
        }
    }

    fragColor = vec4(color, alpha);
}
