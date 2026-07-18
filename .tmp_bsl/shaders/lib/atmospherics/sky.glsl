#ifdef OVERWORLD
vec3 GetSkyColor(vec3 viewPos, bool isReflection) {
    vec3 nViewPos = normalize(viewPos);

    float VoU = clamp(dot(nViewPos,  upVec), -1.0, 1.0);
    float VoL = clamp(dot(nViewPos, sunVec), -1.0, 1.0);

    float groundDensity = 0.1 * (4.0 - 3.0 * sunSkyVisibility) *
                          (10.0 * rainStrength * rainStrength + 1.0);
    
    float exposure = exp2(timeBrightness * 0.75 - 0.75 + SKY_EXPOSURE_D);
    float nightExposure = exp2(-3.5 + SKY_EXPOSURE_N);
    float weatherExposure = exp2(SKY_EXPOSURE_W);

    float gradientCurve = mix(SKY_HORIZON_F, SKY_HORIZON_N, VoL);
    float baseGradient = exp(-(1.0 - pow(1.0 - max(VoU, 0.0), gradientCurve)) /
                             SKY_DENSITY_D);

    #if SKY_GROUND > 0
    float groundVoU = clamp(-VoU * 1.015 - 0.015, 0.0, 1.0);
    float ground = 1.0 - exp(-groundDensity * max(fogDensity, 0.125) / groundVoU);
    #if SKY_GROUND == 1
    if (!isReflection) ground = 1.0;
    #endif
    #else
    float ground = 1.0;
    #endif

    vec3 sky = skyCol;
    #ifdef USE_FOG_COLOR
    sky = mix(sky, fogCol, pow(1.0 - max(VoU, 0.0), 4.0) * SKY_BLEND);
    #endif
    sky *= baseGradient / (SKY_I * SKY_I);
    sky = sky / sqrt(sky * sky + 1.0) * exposure * sunSkyVisibility * (SKY_I * SKY_I);

    float sunMix = pow((VoL * 0.5 + 0.5) * clamp(1.0 - VoU, 0.0, 1.0), 2.0 - sunSkyVisibility) *
                   pow(1.0 - timeBrightness * 0.6, 3.0);
    float horizonMix = pow(1.0 - abs(VoU), 2.5) * 0.125;
    float lightMix = (1.0 - (1.0 - sunMix) * (1.0 - horizonMix));

    vec3 lightSky = pow(lightSun, vec3(4.0 - sunSkyVisibility)) * baseGradient;
    lightSky = lightSky / (1.0 + lightSky * rainStrength);

    sky = mix(
        sqrt(sky * (1.0 - lightMix)), 
        sqrt(lightSky), 
        lightMix
    );
    sky *= sky;

    float nightGradient = exp(-max(VoU, 0.0) / SKY_DENSITY_N);
    vec3 nightSky = lightNight * lightNight * nightGradient * nightExposure;

    float maxSunVisibility = max(sunVisibility, sunSkyVisibility);
    sky = mix(nightSky, sky, maxSunVisibility * maxSunVisibility);

    float rainGradient = exp(-max(VoU, 0.0) / SKY_DENSITY_W);
    vec3 weatherSky = weatherCol.rgb * weatherCol.rgb * weatherExposure;
    weatherSky *= GetLuminance(ambientCol / (weatherSky)) * (0.2 * sunSkyVisibility + 0.2);
    sky = mix(sky, weatherSky * rainGradient, rainStrength);

    sky *= ground;

    #ifdef SKY_UNDERGROUND
    float ug = mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
    sky = mix(minLightCol * 0.125, sky, ug);
    #endif

    #ifdef IS_IRIS
	sky *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
    #else
	#if MC_VERSION >= 11800
	sky *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	sky *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
    #endif

    return sky;
}

#endif