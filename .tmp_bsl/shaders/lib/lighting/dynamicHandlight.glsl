#if DYNAMIC_HANDLIGHT > 0
vec2 ApplyDynamicHandlight(vec2 lightmap, vec3 worldPos) {
    float heldLightValue = max(float(heldBlockLightValue), float(heldBlockLightValue2));

    if (heldLightValue == 0.0) return lightmap;
    
    vec3 heldLightPos = worldPos + relativeEyePosition + vec3(0.0, 0.5, 0.0);

    float handlight = min((heldLightValue - 2.0 * length(heldLightPos)) / 15.0,  0.9333);    
    lightmap.x = log2(exp2(lightmap.x * 32.0) + exp2(handlight * 32.0)) / 32.0;

    return lightmap;
}

vec2 ApplyDynamicHandlightHand(vec2 lightmap) {
    float heldLightValue = max(float(heldBlockLightValue), float(heldBlockLightValue2));

    float handlight = min(heldLightValue / 15.0,  0.9333);    
    lightmap.x = max(lightmap.x, handlight);

    return lightmap;
}
#endif