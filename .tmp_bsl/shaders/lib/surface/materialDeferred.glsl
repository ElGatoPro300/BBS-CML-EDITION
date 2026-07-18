void GetMaterials(out float smoothness, out float skyOcclusion, out vec3 normal, out vec3 fresnel3,
                  vec2 coord) {
    vec3 specularData = texture2D(colortex3, coord).rgb;

    smoothness = specularData.r;
    smoothness *= smoothness;
    smoothness /= 2.0 - smoothness;

    skyOcclusion = specularData.g;
    #if REFLECTION_SKY_FALLOFF > 1
    skyOcclusion = clamp(1.0 - (1.0 - skyOcclusion) * REFLECTION_SKY_FALLOFF, 0.0, 1.0);
    #endif
    skyOcclusion *= skyOcclusion;

	normal = DecodeNormal(texture2D(colortex6, coord).xy);

	fresnel3 = texture2D(colortex7, coord).rgb * smoothness;
}