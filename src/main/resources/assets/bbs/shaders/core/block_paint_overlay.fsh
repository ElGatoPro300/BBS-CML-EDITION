#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 tex = texture(Sampler0, texCoord0);
    float alpha = tex.a * vertexColor.a;

    if (alpha < 0.01)
    {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, alpha);
}
