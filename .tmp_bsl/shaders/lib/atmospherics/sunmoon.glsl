vec3 moonPhaseVecs[8] = vec3[8](
	vec3( 0.0 ,  0.0 ,  1.0 ),
	vec3( 0.0 , -0.89,  0.45),
	vec3( 0.0 , -1.0 ,  0.0 ),
	vec3( 0.0 , -0.45, -0.89),
	vec3( 0.0 ,  0.0 , -1.0 ),
	vec3( 0.0 ,  0.45, -0.89),
	vec3( 0.0 ,  1.0 ,  0.0 ),
	vec3( 0.0 ,  0.89,  0.45)
);

vec2 moonDiffuse[8] = vec2[8](
	vec2( 0.0  ,  0.0  ),
	vec2(-0.125,  1.0  ),
	vec2(-0.2  ,  0.625),
	vec2(-0.8  ,  0.375),
	vec2(-0.75 , -0.25 ),
	vec2(-0.8  ,  0.375),
	vec2(-0.2  ,  0.625),
	vec2(-0.125,  1.0  )
);

void ShaderSunMoon(inout vec3 color, vec3 viewPos, vec3 sunColor, vec3 moonColor) {
	vec3 nViewPos = normalize(viewPos);

	float VoL = dot(nViewPos, sunVec);
	float VoU = dot(nViewPos, upVec);

	if (VoU < -0.1) {
		return;
	}

	float isMoon = float(VoL < 0.0);

	const vec2 sunRotationData = vec2(cos(sunPathRotation * 0.01745329251994), -sin(sunPathRotation * 0.01745329251994));
	float ang = fract(timeAngle + 0.0001 - 0.25);
	ang = (ang + (cos(ang * 3.14159265358979) * -0.5 + 0.5 - ang) / 3.0) * 6.28318530717959;

	vec3 nextSunVec = normalize((gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);
	vec3 sunTangent = normalize(nextSunVec - sunVec);
	vec3 sunBinormal = -cross(sunVec, sunTangent);

	float VoLt = dot(nViewPos, sunTangent);
	float VoLb = dot(nViewPos, sunBinormal);
	
	#if SHADER_SUN_MOON_SHAPE == 0
    float sunMoonSize = (0.007 - 0.004 * isMoon) * SHADER_SUN_MOON_SIZE;
	float sun = pow(smoothstep(1.0 - sunMoonSize, 1.0, abs(VoL)), 3.0);
	#else
    float sunMoonSize = (0.08 - 0.03 * isMoon) * SHADER_SUN_MOON_SIZE;
    vec2 sdfCoord = abs(vec2(VoLt, VoLb) / sunMoonSize * 1.667) - 1.0;
    float squareSDF = length(max(sdfCoord, 0.0));
	float sun = pow(smoothstep(0.667, 0.0, squareSDF), 3.0);
	#endif

	float miniGlare = pow(abs(VoL), 192.0 + 64.0 * isMoon) * (0.05 - 0.04 * isMoon);

	#ifdef SUN_MOON_GROUND
	float sunFade = smoothstep(0.0, 1.0, 1.0 - pow(1.0 - max(VoU * 0.98 + 0.02, 0.0), 8.0));
	float glareFade = smoothstep(0.0, 1.0, 1.0 - pow(1.0 - max(VoU * 0.93 + 0.07, 0.0), 8.0));
	#else
	float sunFade = 1.0;
	float glareFade = 1.0;
	#endif
	float rainVisibility = 1.0 - sqrt(rainStrength);

	vec3 moonNormal = vec3(0.0);

	if (sun > 0.0 && isMoon > 0.5 && moonPhase > 0) {
		float rad = 0.08 * sqrt(SHADER_SUN_MOON_SIZE);

		float moonNormalX = clamp(VoLt / rad, -1.0, 1.0);
		float moonNormalY = clamp(VoLb / rad, -1.0, 1.0);
		float moonNormalZ = sqrt(1.0 - moonNormalX * moonNormalX - moonNormalY * moonNormalY);
		moonNormal = vec3(moonNormalX, moonNormalY, moonNormalZ);
		if (sun > 0.0 && (moonNormalX * moonNormalX + moonNormalY * moonNormalY) > 1) color.r += 8.0;

		vec3 moonPhaseVec = moonPhaseVecs[moonPhase];
		float moonPhase = smoothstep(moonDiffuse[moonPhase].x, moonDiffuse[moonPhase].y, dot(moonNormal, moonPhaseVec));
		float glaremoonPhase = (1.0 - moonPhase) * pow(sun,0.25);
		sun *= moonPhase;
		miniGlare *= 1.0 - glaremoonPhase;
	}

	sun *= sunFade * rainVisibility;
	miniGlare *= glareFade * rainVisibility;

	float sunColorPower = pow(1.0 - max(VoU, 0.0), 16.0) * 4.0 * sunVisibility + 1.0;

	vec3 sunCol = pow(sunColor, vec3(sunColorPower)) * sunVisibility * SUN_INTENSITY * SUN_INTENSITY;
	vec3 moonCol = moonColor * moonVisibility * MOON_INTENSITY * MOON_INTENSITY;
	vec3 sunMoonCol = mix(sunCol, moonCol, isMoon);

	color += (sun + miniGlare) * sunMoonCol * 4.0;
}

void SunGlare(inout vec3 color, vec3 viewPos, vec3 lightCol) {
	float VoL = dot(normalize(viewPos), lightVec);
	
	float visfactor = mix(LIGHT_SHAFT_MORNING_FALLOFF, LIGHT_SHAFT_DAY_FALLOFF, timeBrightness);
		  visfactor = mix(LIGHT_SHAFT_NIGHT_FALLOFF, visfactor, sunVisibility);
		  visfactor*= mix(1.0, LIGHT_SHAFT_WEATHER_FALLOFF, rainStrength) * 0.1;
		  visfactor = min(visfactor, 0.999);

	float invvisfactor = 1.0 - visfactor;

	float visibility = clamp(VoL * 0.5 + 0.5, 0.0, 1.0);
    visibility = visfactor / (1.0 - invvisfactor * visibility) - visfactor;
	visibility = clamp(visibility * 1.015 / invvisfactor - 0.015, 0.0, 1.0);
	visibility = mix(1.0, visibility, 0.03125 * eBS + 0.96875) * (1.0 - rainStrength * eBS * 0.875);
	visibility *= shadowFade * LIGHT_SHAFT_STRENGTH;
	
	#ifdef IS_IRIS
	visibility *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	visibility *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	visibility *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif

	#ifdef LIGHT_SHAFT
	if (isEyeInWater == 1) color += 0.25 * lightCol * visibility;
	#else
	color += 0.25 * lightCol * visibility * (1.0 + 0.25 * isEyeInWater);
	#endif
}