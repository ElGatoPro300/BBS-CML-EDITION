#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform int Target;
uniform vec4 BoneHighlight;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

vec4 gizmoPreviewColor(int index)
{
    if (index == 1)
    {
        return vec4(1.0, 0.0, 0.0, 0.75);
    }

    if (index == 2)
    {
        return vec4(0.0, 1.0, 0.0, 0.75);
    }

    if (index == 3)
    {
        return vec4(0.0, 0.0, 1.0, 0.75);
    }

    if (index >= 7)
    {
        return BoneHighlight;
    }

    return vec4(1.0, 1.0, 1.0, 0.5);
}

void main()
{
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a < 1.0)
    {
        discard;
    }

    int totalIndex = int(color.r * 255.0) | (int(color.g * 255.0) << 8) | (int(color.b * 255.0) << 16);

    if (totalIndex == Target)
    {
        color = gizmoPreviewColor(Target);
    }
    else
    {
        discard;
    }

    fragColor = color * ColorModulator;
}
