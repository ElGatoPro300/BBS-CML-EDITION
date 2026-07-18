#ifdef OVERWORLD

float CloudSampleBasePerlin(vec2 coord) {
	float noiseBase = texture2D(noisetex, coord).r;

	return noiseBase;
}

float CloudSampleBaseWorley(vec2 coord) {
	float noiseBase = texture2D(noisetex, coord).g;
	noiseBase = pow(1.0 - noiseBase, 2.0) * 0.5 + 0.25;

	return noiseBase;
}

float CloudSampleBaseBlocky(vec2 coord) {
	float noiseRes = 512.0;

	coord.xy = coord.xy * noiseRes - 0.5;

	vec2 flr = floor(coord.xy);
	vec2 frc = coord.xy - flr;

	frc = clamp(frc * 5.0 - 2.0, vec2(0.0), vec2(1.0));
	frc = frc * frc * (3.0 - 2.0 * frc);

	coord.xy = (flr + frc + 0.5) / noiseRes;

	float noiseBase = texture2D(noisetex, coord).a;
	noiseBase = (1.0 - noiseBase) * 4.0;

	float noiseRain = texture2D(noisetex, coord + vec2(0.5,0.0)).a;
	noiseRain = (1.0 - noiseRain) * 4.0 * smoothstep(0.0, 0.5, rainStrength);

	noiseBase = min(noiseBase + noiseRain, 1.0);

	return noiseBase;
}

float CloudSampleDetail(vec2 coord, float cloudGradient) {
	float detailZ = floor(cloudGradient * float(CLOUD_THICKNESS)) * 0.04;
	float detailFrac = fract(cloudGradient * float(CLOUD_THICKNESS));

	float noiseDetailLow = texture2D(noisetex, coord.xy + detailZ).b;
	float noiseDetailHigh = texture2D(noisetex, coord.xy + detailZ + 0.04).b;

	float noiseDetail = mix(noiseDetailLow, noiseDetailHigh, detailFrac);

	return noiseDetail;
}

float CloudCoverageDefault(float cloudGradient) {
	float noiseCoverage = abs(cloudGradient - 0.125);

	noiseCoverage *= cloudGradient > 0.125 ? (2.14 - CLOUD_AMOUNT * 0.1) : 8.0;
	noiseCoverage = noiseCoverage * noiseCoverage * 4.0;

	return noiseCoverage;
}

float CloudCoverageBlocky(float cloudGradient) {
	float noiseCoverage = abs(cloudGradient - 0.5) * 2.0;

	noiseCoverage *= noiseCoverage;
	noiseCoverage *= noiseCoverage;

	return noiseCoverage;
}

float CloudApplyDensity(float noise) {
	noise *= CLOUD_DENSITY * 0.125;
	noise *= (1.0 - 0.75 * rainStrength);
	noise = noise / sqrt(noise * noise + 0.5);

	return noise;
}

float CloudCombineDefault(float noiseBase, float noiseDetail, float noiseCoverage, float sunCoverage) {
	float noise = mix(noiseBase, noiseDetail, 0.0476 * CLOUD_DETAIL) * 21.0;

	noise = mix(noise - noiseCoverage, 21.0 - noiseCoverage * 2.5, 0.33 * rainStrength);
	noise = max(noise - (sunCoverage * 3.0 + CLOUD_AMOUNT), 0.0);

	noise = CloudApplyDensity(noise);

	return noise;
}

float CloudCombineBlocky(float noiseBase, float noiseCoverage) {
	float noise = (noiseBase - noiseCoverage) * 2.0;

	noise = max(noise, 0.0);
	
	noise = CloudApplyDensity(noise);

	return noise;
}

float CloudSample(vec2 coord, vec2 wind, float cloudGradient, float sunCoverage, float dither) {
	coord *= 0.004 * CLOUD_STRETCH;

	#if CLOUD_BASE == 0
	vec2 baseCoord = coord * 0.25 + wind;
	vec2 detailCoord = coord.xy * 0.5 - wind * 2.0;

	float noiseBase = CloudSampleBasePerlin(baseCoord);
	float noiseDetail = CloudSampleDetail(detailCoord, cloudGradient);
	float noiseCoverage = CloudCoverageDefault(cloudGradient);

	float noise = CloudCombineDefault(noiseBase, noiseDetail, noiseCoverage, sunCoverage);
	
	return noise;
	#elif CLOUD_BASE == 1
	vec2 baseCoord = coord * 0.5 + wind * 2.0;
	vec2 detailCoord = coord.xy * 0.5 - wind * 2.0;

	float noiseBase = CloudSampleBaseWorley(baseCoord);
	float noiseDetail = CloudSampleDetail(detailCoord, cloudGradient);
	float noiseCoverage = CloudCoverageDefault(cloudGradient);

	float noise = CloudCombineDefault(noiseBase, noiseDetail, noiseCoverage, sunCoverage);
	
	return noise;
	#else
	vec2 baseCoord = coord * 0.15625 + wind * 0.5;

	float noiseBase = CloudSampleBaseBlocky(baseCoord);
	float noiseCoverage = CloudCoverageBlocky(cloudGradient);

	float noise = CloudCombineBlocky(noiseBase, noiseCoverage);

	return noise;
	#endif
}

float InvLerp(float v, float l, float h) {
	return clamp((v - l) / (h - l), 0.0, 1.0);
}

float GetCloudMask(vec3 viewPos, float z) {
	if (z == 1.0) return 1.0;

	#ifdef FAR_VANILLA_FOG_OVERWORLD
	#if FAR_VANILLA_FOG_STYLE == 0
	float fogFactor = length(viewPos);
	#else
	vec4 worldPos = gbufferModelViewInverse * vec4(viewPos, 1.0);
	worldPos.xyz /= worldPos.w;

	float fogFactor = length(worldPos.xz);
	#endif

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
	
	return vanillaFog;
	#else
	return 0.0;
	#endif
}

vec4 DrawCloudSkybox(vec3 viewPos, float z, float dither, vec3 lightCol, vec3 ambientCol, bool fadeFaster) {
	float cloudMask = GetCloudMask(viewPos, z);
	if (cloudMask == 0.0) return vec4(0.0);

	#ifdef TAA
	#if TAA_MODE == 0
	dither = fract(dither + frameCounter * 0.618);
	#else
	dither = fract(dither + frameCounter * 0.5);
	#endif
	#endif

	int samples = CLOUD_THICKNESS * 2;
	
	float cloud = 0.0, cloudLighting = 0.0;

	float sampleStep = 1.0 / samples;
	float currentStep = dither * sampleStep;
	
	vec3 nViewPos = normalize(viewPos);
	float VoU = dot(nViewPos, upVec);
	float VoL = dot(nViewPos, lightVec);
	
	#ifdef CLOUD_REVEAL
	float sunCoverage = mix(abs(VoL), max(VoL, 0.0), shadowFade);
	sunCoverage = pow(clamp(sunCoverage * 2.0 - 1.0, 0.0, 1.0), 12.0) * (1.0 - rainStrength);
	#else
	float sunCoverage = 0.0;
	#endif

	float convertedHeight = (max(float(CLOUD_HEIGHT), 128.0) - 72.0) / CLOUD_SCALE;

	vec2 wind = vec2(
		time * CLOUD_SPEED * 0.0005,
		sin(time * CLOUD_SPEED * 0.001) * 0.005
	) * 0.667;

	vec3 cloudColor = vec3(0.0);

	if (VoU > 0.025) {
		vec3 wpos = normalize((gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz);

		float halfVoL = mix(abs(VoL) * 0.8, VoL, shadowFade) * 0.5 + 0.5;
		float halfVoLSqr = halfVoL * halfVoL;
		float scattering = pow(halfVoL, 6.0);
		float noiseLightFactor = (2.0 - 1.5 * VoL * shadowFade) * CLOUD_DENSITY * 0.5;

		for(int i = 0; i < samples; i++) {
			if (cloud > 0.99) break;

			#if CLOUD_BASE == 2
			float planeY = convertedHeight + currentStep * CLOUD_THICKNESS * 0.4;
			#else 
			float planeY = convertedHeight + currentStep * CLOUD_THICKNESS;
			#endif

			vec3 planeCoord = wpos * (planeY / wpos.y);
			vec2 cloudCoord = cameraPosition.xz * 0.0625 * 16.0 / CLOUD_SCALE + planeCoord.xz;

			float noise = CloudSample(cloudCoord, wind, currentStep, sunCoverage, dither);

			float sampleLighting = pow(currentStep, 1.125 * halfVoLSqr + 0.875) * 0.8 + 0.2;
			sampleLighting *= 1.0 - pow(noise, noiseLightFactor);

			cloudLighting = mix(cloudLighting, sampleLighting, noise * (1.0 - cloud * cloud));
			cloud = mix(cloud, 1.0, noise);

			currentStep += sampleStep;
		}
		cloudLighting = mix(cloudLighting, 1.0, (1.0 - cloud * cloud) * scattering * 0.5);
		cloudLighting *= (1.0 - 0.9 * rainStrength);

		cloudColor = mix(
			ambientCol * (0.3 * sunSkyVisibility + 0.5),
			lightCol * (0.85 + 1.15 * scattering),
			cloudLighting
		);
		cloudColor *= 1.0 - 0.4 * rainStrength;

		cloud *= clamp(1.0 - exp(-16.0 / max(fogDensity, 0.5) * VoU + 0.5), 0.0, 1.0);

		if (fadeFaster) {
			cloud *= 1.0 - pow(1.0 - VoU, 4.0);
		}
	}
	cloudColor *= CLOUD_BRIGHTNESS * (0.5 - 0.25 * (1.0 - sunSkyVisibility) * (1.0 - rainStrength));
	
	#ifdef IS_IRIS
	cloudColor *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	cloudColor *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	cloudColor *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif
	
	#ifdef SKY_UNDERGROUND
	cloud *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif
	
	cloud *= cloud;
	cloud *= cloudMask;
	cloud *= CLOUD_OPACITY;
	
	return vec4(cloudColor, cloud);
}

vec3 GetReflectedCameraPos(vec3 worldPos, vec3 normal) {
	vec3 cameraPos = cameraPosition + worldPos; //it just works???

	// vec4 worldNormal = gbufferModelViewInverse * vec4(normal, 1.0);
	// worldNormal.xyz /= worldNormal.w;

	// vec3 cameraPos = cameraPosition + 2.0 * worldNormal.xyz * dot(worldPos, worldNormal.xyz);
	// cameraPos = cameraPos + reflect(worldPos, worldNormal.xyz); //bobbing stabilizer, works like magic

	return cameraPos;
}

vec4 DrawCloudVolumetric(vec3 viewPos, vec3 cameraPos, float z, float dither, vec3 lightCol, vec3 ambientCol, inout float cloudViewLength, bool fadeFaster) {
	float cloudMask = GetCloudMask(viewPos, z);
	
	#ifdef TAA
	#if TAA_MODE == 0
	dither = fract(dither + frameCounter * 0.618);
	#else
	dither = fract(dither + frameCounter * 0.5);
	#endif
	#endif

	vec3 nViewPos = normalize(viewPos);
	vec3 worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz;
	vec3 nWorldPos = normalize(worldPos);
	float viewLength = length(viewPos);

	float cloudThickness = CLOUD_THICKNESS;

	int maxSamples = 32;

	#if CLOUD_BASE == 2
	cloudThickness *= 0.4;
	maxSamples = 64;
	#endif

	#if CLOUD_HEIGHT == -1
	#ifdef IS_IRIS
	float lowerY = cloudHeight;
	#else
	float lowerY = 192.0;
	#endif
	#else
	float lowerY = float(CLOUD_HEIGHT);
	#endif

	float upperY = lowerY + cloudThickness * CLOUD_SCALE;

	float lowerPlane = (lowerY - cameraPos.y) / nWorldPos.y;
	float upperPlane = (upperY - cameraPos.y) / nWorldPos.y;

	float nearestPlane = max(min(lowerPlane, upperPlane), 0.0);
	float furthestPlane = max(lowerPlane, upperPlane);

	float maxcloudViewLength = cloudViewLength;

	if (furthestPlane < 0) return vec4(0.0);

	float planeDifference = furthestPlane - nearestPlane;

	vec3 startPos = cameraPos + nearestPlane * nWorldPos;

	float lengthScaling = abs(cameraPosition.y - (upperY + lowerY) * 0.5) / ((upperY - lowerY) * 0.5);
	lengthScaling = clamp((lengthScaling - 1.0) * cloudThickness * 0.125, 0.0, 1.0);

	float sampleLength = cloudThickness * CLOUD_SCALE / 2.0;

	sampleLength /= (4.0 * nWorldPos.y * nWorldPos.y) * lengthScaling + 1.0;
	vec3 sampleStep = nWorldPos * sampleLength;
	int samples = int(min(planeDifference / sampleLength, maxSamples) + 1);
	
	vec3 samplePos = startPos + sampleStep * dither;
	float sampleTotalLength = nearestPlane + sampleLength * dither;

	vec2 wind = vec2(
		time * CLOUD_SPEED * 0.0005,
		sin(time * CLOUD_SPEED * 0.001) * 0.005
	) * 0.667;

	float cloud = 0.0;
	float cloudFaded = 0.0;
	float cloudLighting = 0.0;

	float VoU = dot(nViewPos, upVec);
	float VoL = dot(nViewPos, lightVec);
	float VoS = dot(nViewPos, sunVec);
	
	#ifdef CLOUD_REVEAL
	float sunCoverage = mix(abs(VoL), max(VoL, 0.0), shadowFade);
	sunCoverage = pow(clamp(sunCoverage * 2.0 - 1.0, 0.0, 1.0), 12.0) * (1.0 - rainStrength);
	#else
	float sunCoverage = 0.0;
	#endif

	float halfVoL = mix(abs(VoL) * 0.8, VoL, shadowFade) * 0.5 + 0.5;
	float halfVoLSqr = halfVoL * halfVoL;
	float halfVoS = VoS * 0.5 + 0.5;
	float halfVoSSqr = halfVoS * halfVoS;

	float scattering = pow(halfVoL, 6.0);
	float noiseLightFactor = (2.0 - 1.5 * VoL * shadowFade) * CLOUD_DENSITY * 0.5;

	float viewLengthSoftMin = viewLength - sampleLength * 0.5;
	float viewLengthSoftMax = viewLength + sampleLength * 0.5;

	float distanceFade = 1.0;
	float fadeStart = 32.0 / max(fogDensity, 0.5);
	float fadeEnd = (fadeFaster ? 80.0 : 240.0) / max(fogDensity, 0.5);

	float xzNormalizeFactor = 10.0 / max(abs(float(CLOUD_HEIGHT) - 72.0), 56.0);

	for (int i = 0; i < samples; i++) {
		if (cloud > 0.99) break;
		if (sampleTotalLength > viewLengthSoftMax && cloudMask == 0.0) break;

		float cloudGradient = InvLerp(samplePos.y, lowerY, upperY);
		float xzNormalizedDistance = length(samplePos.xz - cameraPos.xz) * xzNormalizeFactor;
		vec2 cloudCoord = samplePos.xz / CLOUD_SCALE;

		float noise = CloudSample(cloudCoord, wind, cloudGradient, sunCoverage, dither);
		noise *= step(lowerY, samplePos.y) * step(samplePos.y, upperY);

		float sampleLighting = pow(cloudGradient, 1.125 * halfVoLSqr + 0.875) * 0.8 + 0.2;
		sampleLighting *= 1.0 - pow(noise, noiseLightFactor);

		float sampleFade = InvLerp(xzNormalizedDistance, fadeEnd, fadeStart);
		distanceFade *= mix(1.0, sampleFade, noise * (1.0 - cloud));
		noise *= step(xzNormalizedDistance, fadeEnd);

		cloudLighting = mix(cloudLighting, sampleLighting, noise * (1.0 - cloud * cloud));

		cloud = mix(cloud, 1.0, noise);
		
		if (z < 1.0) {
			float softFade = InvLerp(sampleTotalLength, viewLengthSoftMax, viewLengthSoftMin);
			noise *= mix(softFade, 1.0, cloudMask * cloudMask);
		}

		cloudFaded = mix(cloudFaded, 1.0, noise);

		if (cloudViewLength == maxcloudViewLength && cloud > 0.5) {
			cloudViewLength = sampleTotalLength;
		}
		
		samplePos += sampleStep;
		sampleTotalLength += sampleLength;
	}

	cloudFaded *= distanceFade;

	cloudLighting = mix(cloudLighting, 1.0, (1.0 - cloud * cloud) * scattering * 0.5);
	cloudLighting *= (1.0 - 0.9 * rainStrength);

	float horizonSunFactor = sunSkyVisibility * (1.0 - sunVisibility) * halfVoSSqr * 0.5;
	vec3 horizonSunCol = pow(lightSun, vec3(4.0 - 3.0 * sunSkyVisibility));

	vec3 cloudAmbientCol = ambientCol * (0.3 * sunSkyVisibility + 0.5);
	vec3 cloudLightCol = mix(lightCol, horizonSunCol, horizonSunFactor);
	cloudLightCol *= 0.85 + 1.15 * scattering;
	
	vec3 cloudColor = mix(cloudAmbientCol, cloudLightCol, cloudLighting);

	cloudColor *= 1.0 - 0.4 * rainStrength;
	cloudColor *= CLOUD_BRIGHTNESS * (0.5 - 0.25 * (1.0 - sunSkyVisibility) * (1.0 - rainStrength));

	cloudFaded *= cloudFaded * CLOUD_OPACITY;

	if (cloudFaded < dither) {
		cloudViewLength = maxcloudViewLength;
	}
	
	#ifdef IS_IRIS
	cloudColor *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	cloudColor *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	cloudColor *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif
	
	#ifdef SKY_UNDERGROUND
	cloudFaded *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif

	return vec4(cloudColor, cloudFaded);
}

float GetNoise(vec2 pos) {
	return fract(sin(dot(pos, vec2(12.9898, 4.1414))) * 43758.5453);
}

void DrawStars(inout vec3 color, vec3 viewPos) {
	vec3 wpos = vec3(gbufferModelViewInverse * vec4(viewPos * 100.0, 1.0));
	vec3 planeCoord = wpos / (wpos.y + length(wpos.xz));
	vec2 wind = vec2(time, 0.0);
	vec2 coord = planeCoord.xz * 0.4 + cameraPosition.xz * 0.0001 + wind * 0.00125;
	coord = floor(coord * 1024.0) / 1024.0;
	
	float VoU = clamp(dot(normalize(viewPos), upVec), 0.0, 1.0);
	float VoL = dot(normalize(viewPos), sunVec);
	float multiplier = sqrt(sqrt(VoU)) * 5.0 * (1.0 - rainStrength) * moonVisibility;
	
	float star = 1.0;
	if (VoU > 0.0) {
		star *= GetNoise(coord.xy);
		star *= GetNoise(coord.xy + 0.10);
		star *= GetNoise(coord.xy + 0.23);
	}
	star = clamp(star - 0.8125, 0.0, 1.0) * multiplier;
	
	#ifdef IS_IRIS
	star *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	star *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	star *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif

	#ifdef SKY_UNDERGROUND
	star *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif

	float moonFade = smoothstep(-0.997,-0.992, VoL);
	star *= moonFade;
		
	color += star * pow(lightNight, vec3(0.8));
}

#if AURORA > 0
#include "/lib/color/auroraColor.glsl"

float AuroraSample(vec2 coord, vec2 wind, float VoU) {
	float noise = texture2D(noisetex, coord * 0.0625  + wind * 0.25).b * 3.0;
		  noise+= texture2D(noisetex, coord * 0.03125 + wind * 0.15).b * 3.0;

	noise = max(1.0 - 4.0 * (0.5 * VoU + 0.5) * abs(noise - 3.0), 0.0);

	return noise;
}

vec3 DrawAurora(vec3 viewPos, float dither, int samples) {
	#ifdef TAA
	#if TAA_MODE == 0
	dither = fract(dither + frameCounter * 0.618);
	#else
	dither = fract(dither + frameCounter * 0.5);
	#endif
	#endif
	
	float sampleStep = 1.0 / samples;
	float currentStep = dither * sampleStep;

	float VoU = dot(normalize(viewPos), upVec);

	float visibility = moonVisibility * (1.0 - rainStrength) * (1.0 - rainStrength);

	#if AURORA == 1 && defined WEATHER_PERBIOME
	visibility *= isCold * isCold;
	#endif

	vec2 wind = vec2(
		time * CLOUD_SPEED * 0.000125,
		sin(time * CLOUD_SPEED * 0.05) * 0.00025
	);

	vec3 aurora = vec3(0.0);

	if (VoU > 0.0 && visibility > 0.0) {
		vec3 wpos = normalize((gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz);
		for(int i = 0; i < samples; i++) {
			vec3 planeCoord = wpos * ((8.0 + currentStep * 7.0) / wpos.y) * 0.004;

			vec2 coord = cameraPosition.xz * 0.00004 + planeCoord.xz;
			coord += vec2(coord.y, -coord.x) * 0.3;

			float noise = AuroraSample(coord, wind, VoU);
			
			if (noise > 0.0) {
				noise *= texture2D(noisetex, coord * 0.125 + wind * 0.25).b;
				noise *= 0.5 * texture2D(noisetex, coord + wind * 16.0).b + 0.75;
				noise = noise * noise * 3.0 * sampleStep;
				noise *= max(sqrt(1.0 - length(planeCoord.xz) * 3.75), 0.0);

				vec3 auroraColor = mix(auroraLowCol, auroraHighCol, pow(currentStep, 0.4));
				aurora += noise * auroraColor * exp2(-6.0 * i * sampleStep);
			}
			currentStep += sampleStep;
		}
	}
	
	#ifdef IS_IRIS
	visibility *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	visibility *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	visibility *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif

	#ifdef SKY_UNDERGROUND
	visibility *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif

	return aurora * visibility;
}
#endif
#endif