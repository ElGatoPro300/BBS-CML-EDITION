void GetMaterials(out float smoothness, out float metalness, out float f0, inout float emission,
                  inout float subsurface, out float porosity, out float ao, out vec3 normalMap,
                  vec2 newCoord, vec2 dcdx, vec2 dcdy) {
    #if MATERIAL_FORMAT == 0
    #ifdef PARALLAX
    vec4 specularMap = texture2DGradARB(specular, newCoord, dcdx, dcdy);
    #else
    vec4 specularMap = texture2D(specular, texCoord);
    #endif
    
    smoothness = specularMap.r;
    
    f0 = 0.04;
    metalness = specularMap.g;

    float emissionMat = specularMap.b * specularMap.b;

    porosity = 0.5 - 0.5 * smoothness;
    subsurface = specularMap.a > 0.0 ? 1.0 - specularMap.a : 0.0;

    #ifdef PARALLAX
	normalMap = texture2DGradARB(normals, newCoord, dcdx, dcdy).xyz * 2.0 - 1.0;
    #else
	normalMap = texture2D(normals, texCoord).xyz * 2.0 - 1.0;
    #endif
    ao = 1.0;

    if (normalMap.x + normalMap.y < -1.999) normalMap = vec3(0.0, 0.0, 1.0);
    #endif

    #if MATERIAL_FORMAT == 1
    vec4 specularMap = texture2DLod(specular, newCoord, 0);
    smoothness = specularMap.r;

    f0 = specularMap.g;
    metalness = f0 >= 0.9 ? 1.0 : 0.0;

    float emissionMat = specularMap.a < 1.0 ? clamp(specularMap.a * 1.004 - 0.004, 0.0, 1.0) : 0.0;
    emissionMat *= emissionMat;

    porosity = specularMap.b <= 0.251 ? specularMap.b * 3.984 : 0.0;
    subsurface = specularMap.b > 0.251 ? clamp(specularMap.b * 1.335 - 0.355, 0.0, 1.0) : 0.0;

    #ifdef PARALLAX
	normalMap = vec3(texture2DGradARB(normals, newCoord, dcdx, dcdy).xy, 0.0) * 2.0 - 1.0;
    ao = texture2DGradARB(normals, newCoord, dcdx, dcdy).z;
    #else
	normalMap = vec3(texture2D(normals, texCoord).xy, 0.0) * 2.0 - 1.0;
    ao = texture2D(normals, texCoord).z;
    #endif

    if (normalMap.x + normalMap.y > -1.999) {
        if (length(normalMap.xy) > 1.0) normalMap.xy = normalize(normalMap.xy);
        normalMap.z = sqrt(1.0 - dot(normalMap.xy, normalMap.xy));
        normalMap = normalize(clamp(normalMap, vec3(-1.0), vec3(1.0)));
    }else{
        normalMap = vec3(0.0, 0.0, 1.0);
        ao = 1.0;
    }
    #endif

    #if EMISSIVE == 2
    emission = mix(emissionMat, 1.0, emission);
    #else
    emission = emissionMat;
    #endif

    #ifdef NORMAL_DAMPENING
    vec2 mipx = dcdx * atlasSize;
    vec2 mipy = dcdy * atlasSize;
    float delta = max(dot(mipx, mipx), dot(mipy, mipy));
    float miplevel = max(0.25 * log2(delta), 0.0);
    
    normalMap = normalize(mix(vec3(0.0, 0.0, 1.0), normalMap, 1.0 / exp2(miplevel)));
    #endif
}