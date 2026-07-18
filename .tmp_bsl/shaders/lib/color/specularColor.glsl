vec3 GetMetalCol(float f0) {
    int metalidx = int(f0 * 255.0);

    if (metalidx == 230) return vec3(0.24867, 0.22965, 0.21366);
    if (metalidx == 231) return vec3(0.88140, 0.57256, 0.11450);
    if (metalidx == 232) return vec3(0.81715, 0.82021, 0.83177);
    if (metalidx == 233) return vec3(0.27446, 0.27330, 0.27357);
    if (metalidx == 234) return vec3(0.84430, 0.48677, 0.22164);
    if (metalidx == 235) return vec3(0.36501, 0.35675, 0.37653);
    if (metalidx == 236) return vec3(0.42648, 0.37772, 0.31138);
    if (metalidx == 237) return vec3(0.91830, 0.89219, 0.83662);
    return vec3(1.0);
}

vec3 GetSpecularColor(float skylight, float metalness, vec3 baseReflectance){
    vec3 specularColor = vec3(0.0);
    #ifdef OVERWORLD
    vec3 lightME = mix(lightMorning, lightEvening, mefade);
    vec3 lightDaySpec = mix(sqrt(lightME), sqrt(lightDay), dfade * 0.7);
    #ifdef NIGHT_MOON_PHASE
    vec3 lightNightSpec = sqrt(lightNight * moonPhaseMultiplier[moonPhase] * LIGHT_NI * 0.2);
    #else
    vec3 lightNightSpec = sqrt(lightNight * LIGHT_NI * 0.2) * moonPhaseMultiplier[moonPhase];
    #endif
    specularColor = mix(lightNightSpec, lightDaySpec * lightDaySpec, sunVisibility);
    specularColor *= specularColor * skylight;
    #endif
    #ifdef END
    specularColor = endCol.rgb * 0.35;
    #endif
    
    specularColor = pow(specularColor, vec3(1.0 - 0.5 * metalness)) *
                    pow(max(length(specularColor), 0.0001), 0.5 * metalness);

    return specularColor;
}
