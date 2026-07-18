vec4 ReadNormal(vec2 coord) {
    coord = fract(coord) * vTexCoordAM.pq + vTexCoordAM.st;
	return texture2DGradARB(normals, coord, dcdx, dcdy);
}

vec2 GetParallaxCoord(vec2 texCoord, float parallaxFade, out float surfaceDepth) {
    vec2 coord = vTexCoord.st;
    surfaceDepth = 1.0;

    if (viewVector != viewVector) {
        return texCoord;
    }

    float sampleStep = 1.0 / PARALLAX_QUALITY;
    float currentStep = 1.0;

    vec2 scaledDir = viewVector.xy * PARALLAX_DEPTH / -viewVector.z;
    vec2 stepDir = scaledDir * sampleStep * (1.0 - parallaxFade);

    vec3 normalMap = ReadNormal(coord).xyz * 2.0 - 1.0;
    float normalCheck = normalMap.x + normalMap.y;
    if (parallaxFade >= 1.0 || normalCheck < -1.999) return texCoord;

    float depth = ReadNormal(coord).a;

    for(int i = 0; i < PARALLAX_QUALITY; i++){
        if (currentStep <= depth) break;
        coord += stepDir;
        depth = ReadNormal(coord).a;
        currentStep -= sampleStep;
    }

    coord = fract(coord.st) * vTexCoordAM.pq + vTexCoordAM.st;
    surfaceDepth = currentStep;

    return coord;
}

float GetParallaxShadow(float surfaceDepth, float parallaxFade, vec2 coord, vec3 lightVec,
                        mat3 tbn) {
    float parallaxshadow = 1.0;
    if(parallaxFade >= 1.0) return 1.0;

    float height = surfaceDepth;
    if(height > 1.0 - 0.5 / PARALLAX_QUALITY) return 1.0;

    vec3 parallaxdir = tbn * lightVec;
    parallaxdir.xy *= PARALLAX_DEPTH * SELF_SHADOW_ANGLE;
    vec2 newvTexCoord = (coord - vTexCoordAM.st) / vTexCoordAM.pq;
    float sampleStep = 0.32 / SELF_SHADOW_QUALITY;

    vec2 ptexCoord = fract(newvTexCoord + parallaxdir.xy * sampleStep) * 
                     vTexCoordAM.pq + vTexCoordAM.st;

    float texHeight = texture2DGradARB(normals, coord, dcdx, dcdy).a;
    float texHeightOffset = texture2DGradARB(normals, ptexCoord, dcdx, dcdy).a;

    float texFactor = clamp((height - texHeightOffset) / sampleStep + 1.0, 0.0, 1.0);

    height = mix(height, texHeight, texFactor);
    
    for(int i = 0; i < SELF_SHADOW_QUALITY; i++) {
        float currentHeight = height + parallaxdir.z * sampleStep * i;
        vec2 parallaxCoord = fract(newvTexCoord + parallaxdir.xy * i * sampleStep) * 
                             vTexCoordAM.pq + vTexCoordAM.st;
        float offsetHeight = texture2DGradARB(normals, parallaxCoord, dcdx, dcdy).a;
        float sampleShadow = clamp(1.0 - (offsetHeight - currentHeight) * SELF_SHADOW_STRENGTH, 0.0, 1.0);
        parallaxshadow = min(parallaxshadow, sampleShadow);
        if (parallaxshadow < 0.01) break;
    }
    parallaxshadow *= parallaxshadow;
    
    parallaxshadow = mix(parallaxshadow, 1.0, parallaxFade);

    return parallaxshadow;
}