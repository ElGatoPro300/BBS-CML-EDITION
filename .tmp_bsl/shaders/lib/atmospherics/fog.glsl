#ifdef OVERWORLD
vec3 GetFogColor(vec3 viewPos) {
	vec3 nViewPos = normalize(viewPos);
	float lViewPos = length(viewPos) / 64.0;
	lViewPos = 1.0 - exp(-lViewPos * lViewPos);

    float VoU = clamp(dot(nViewPos,  upVec), -1.0, 1.0);
    float VoL = clamp(dot(nViewPos, sunVec), -1.0, 1.0);

	float density = 0.4;
    float nightDensity = 1.0;
    float weatherDensity = 1.5;
    float groundDensity = 0.08 * (4.0 - 3.0 * sunSkyVisibility) *
                          (10.0 * rainStrength * rainStrength + 1.0);
    
    float exposure = exp2(timeBrightness * 0.75 - 0.75);
    float nightExposure = exp2(-3.5);

	float baseGradient = exp(-(VoU * 0.5 + 0.5) * 0.5 / density);

	float groundVoU = clamp(-VoU * 0.5 + 0.5, 0.0, 1.0);
    float ground = 1.0 - exp(-groundDensity / groundVoU);

    vec3 fog = skyCol;
	#ifdef USE_FOG_COLOR
    fog = mix(skyCol, fogCol, FOG_BLEND);
	#endif
	fog *= baseGradient / (SKY_I * SKY_I);
    fog = fog / sqrt(fog * fog + 1.0) * exposure * sunSkyVisibility * (SKY_I * SKY_I);

	float sunMix = pow((VoL * 0.5 + 0.5) * clamp(1.0 - VoU, 0.0, 1.0), 2.0 - sunSkyVisibility) *
                   pow(1.0 - timeBrightness * 0.6, 3.0);
    float horizonMix = pow(1.0 - abs(VoU), 2.5) * 0.125;
    float lightMix = (1.0 - (1.0 - sunMix) * (1.0 - horizonMix)) * lViewPos;

	vec3 lightFog = pow(lightSun, vec3(4.0 - sunSkyVisibility)) * baseGradient;
	lightFog = lightFog / (1.0 + lightFog * rainStrength);

    fog = mix(
        sqrt(fog * (1.0 - lightMix)), 
        sqrt(lightFog), 
        lightMix
    );
    fog *= fog;

	float nightGradient = exp(-(VoU * 0.5 + 0.5) * 0.35 / nightDensity);
    vec3 nightFog = lightNight * lightNight * nightGradient * nightExposure;

    fog = mix(nightFog, fog, sunSkyVisibility * sunSkyVisibility);

    float rainGradient = exp(-(VoU * 0.5 + 0.5) * 0.125 / weatherDensity);
    vec3 weatherFog = weatherCol.rgb * weatherCol.rgb;
    weatherFog *= GetLuminance(ambientCol / (weatherFog)) * (0.2 * sunSkyVisibility + 0.2);
    fog = mix(fog, weatherFog * rainGradient, rainStrength);

	float exteriorFactor = eBS;
	#ifdef FOG_INTERIOR
	exteriorFactor = max(exteriorFactor, clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0));
	#endif
	
	fog = mix(minLightCol * 0.5, fog * exteriorFactor, exteriorFactor);


	#ifdef IS_IRIS
	fog *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	fog *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	fog *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif

	return fog;
}
#endif

void NormalFog(inout vec3 color, vec3 viewPos) {
	float viewLength = length(viewPos);
	
	vec4 worldPos = gbufferModelViewInverse * vec4(viewPos, 1.0);
	worldPos.xyz /= worldPos.w;

	#ifdef FAR_VANILLA_FOG_ENABLED
	#if FAR_VANILLA_FOG_STYLE == 0
	float fogFactor = viewLength;
	#else
	float fogFactor = length(worldPos.xz);
	#endif
	#endif
	
	#ifdef OVERWORLD
	float fog = viewLength * fogDensity / 1024.0;
	float clearDay = sunSkyVisibility * (1.0 - rainStrength);

	#if defined DISTANT_HORIZONS || defined VOXY
	fog *= FOG_DENSITY_LOD;
	#endif

	float exteriorFactor = eBS;
	#ifdef FOG_INTERIOR
	exteriorFactor = max(exteriorFactor, clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0));
	#endif

	float fogDensityMult = mix(1.0, FOG_DENSITY_WEATHER, rainStrength) / mix(1.0 / FOG_DENSITY_NIGHT, 1.0, clearDay);
	fogDensityMult = mix(FOG_DENSITY_INDOOR, fogDensityMult * exteriorFactor, exteriorFactor);
	
	fog *= fogDensityMult;
	float fogDampen = 0.3 * rainStrength + 0.5;
	fog = min(fog, (fog - fogDampen) * 0.25 + fogDampen);

	#ifdef FOG_HEIGHT
	fog *= exp2(-max(worldPos.y + cameraPosition.y - FOG_HEIGHT_Y, 0.0) / exp2(FOG_HEIGHT_FALLOFF));
	#endif

	fog = 1.0 - exp(-2.0 * pow(fog, 0.35 * clearDay * exteriorFactor + 1.25));

	vec3 fogColor = GetFogColor(viewPos);

	#ifdef FAR_VANILLA_FOG_OVERWORLD
	if(isEyeInWater == 0.0){
		#if MC_VERSION >= 11800
		float fogOffset = 0.0;
		#else
		float fogOffset = 12.0;
		#endif

		float fogFar = far;
		float vanillaDensity = 0.2 * FOG_DENSITY_VANILLA;

		#ifdef VOXY
		fogFar = max(fogFar, vxRenderDistance * 16.0);
		vanillaDensity = 0.4 * sqrt(FOG_DENSITY_VANILLA);
		#endif

		#ifdef DISTANT_HORIZONS
		fogFar = max(fogFar, float(dhRenderDistance));
		vanillaDensity = 0.4 * sqrt(FOG_DENSITY_VANILLA);
		#endif

		float vanillaFog = 1.0 - (fogFar - (fogFactor + fogOffset)) / (vanillaDensity * fogFar);
		vanillaFog = clamp(vanillaFog, 0.0, 1.0);
	
		if(vanillaFog > 0.0){
			vec3 vanillaFogColor = GetSkyColor(viewPos, false);
			
			vanillaFogColor *= 1.0 + nightVision;
			#ifdef CLASSIC_EXPOSURE
			vanillaFogColor *= 4.0 - 3.0 * eBS;
			#endif

			fogColor *= fog;
			
			fog = mix(fog, 1.0, vanillaFog);
			if(fog > 0.0) fogColor = mix(fogColor, vanillaFogColor, vanillaFog) / fog;
		}
	}
	#endif
	#endif

	#ifdef NETHER
	float fog = 2.0 * pow(viewLength * fogDensity / 256.0, 1.5);

	#ifdef FAR_VANILLA_FOG_NETHER_END
	float fogFar = far / 1.5;

	#ifdef VOXY
	fogFar = max(fogFar, vxRenderDistance * 16.0 / 3.0);
	#endif
	
	#ifdef DISTANT_HORIZONS
	fogFar = max(fogFar, dhFarPlane / 3.0);
	#endif

	fog += 6.0 * pow(fogFactor / fogFar, 4.0);
	#endif

	fog = 1.0 - exp(-fog);

	vec3 fogColor = netherCol.rgb * 0.0425;
	#endif

	#ifdef END
	float fog = viewLength * fogDensity / 512.0;

	#ifdef FAR_VANILLA_FOG_NETHER_END
	float fogFar = far / 1.5;

	#ifdef VOXY
	fogFar = max(fogFar, vxRenderDistance * 16.0 / 3.0);
	#endif
	
	#ifdef DISTANT_HORIZONS
	fogFar = max(fogFar, dhFarPlane / 3.0);
	#endif

	fog += 2.0 * pow(fogFactor / fogFar, 4.0);
	#endif

	fog = 1.0 - exp(-fog);

	vec3 fogColor = endCol.rgb * 0.003;
	#ifndef LIGHT_SHAFT
	fogColor *= 4.0;
	#endif
	#endif
	
	color = mix(color, fogColor, fog);
}

void BlindFog(inout vec3 color, vec3 viewPos) {
	float fog = length(viewPos) * max(blindFactor * 0.2, darknessFactor * 0.075);
	fog = (1.0 - exp(-6.0 * fog * fog * fog)) * max(blindFactor, darknessFactor);
	color = mix(color, vec3(0.0), fog);
}

vec3 denseFogColor[2] = vec3[2](
	vec3(1.0, 0.3, 0.01),
	vec3(0.1, 0.16, 0.2)
);

void DenseFog(inout vec3 color, vec3 viewPos) {
	float fog = length(viewPos) * 0.5;
	fog = (1.0 - exp(-4.0 * fog * fog * fog));
	color = mix(color, denseFogColor[isEyeInWater - 2], fog);
}

void Fog(inout vec3 color, vec3 viewPos) {
	NormalFog(color, viewPos);
	if (isEyeInWater > 1) DenseFog(color, viewPos);
	if (blindFactor > 0.0 || darknessFactor > 0.0) BlindFog(color, viewPos);
}