#version 150

uniform sampler2D Sampler0;

uniform mat4 PaintEffectInverse;
uniform float PaintEffectActive;
uniform vec3 PaintMaskHalf;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;

out vec4 fragColor;

float bbsBlockPaintMask(vec3 blockPos, mat4 effectInverse, float active, vec3 halfExtents)
{
    if (active < 0.5)
    {
        return 1.0;
    }

    vec3 local = (effectInverse * vec4(blockPos.x - 0.5, blockPos.y, blockPos.z - 0.5, 1.0)).xyz;
    local.y -= halfExtents.y;
    vec3 d = abs(local) - halfExtents;
    float dist = length(max(d, 0.0)) + min(max(max(d.x, d.y), d.z), 0.0);
    float maxHalf = max(halfExtents.x, max(halfExtents.y, halfExtents.z));
    float falloff = max(maxHalf * 0.15, 0.1);

    return 1.0 - smoothstep(0.0, falloff, dist);
}

void main()
{
    vec4 tex = texture(Sampler0, texCoord0);
    float mask = bbsBlockPaintMask(localPos, PaintEffectInverse, PaintEffectActive, PaintMaskHalf);
    float alpha = tex.a * vertexColor.a * mask;

    if (alpha < 0.01)
    {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, alpha);
}
