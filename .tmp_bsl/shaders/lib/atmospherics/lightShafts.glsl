float GetLightShaftsFalloff(vec3 viewPos, float hand) {
	float viewLength = length(viewPos);
	viewPos = normalize(viewPos);

	vec3 lightVec = sunVec * ((timeAngle < 0.5325 || timeAngle > 0.9675) ? 1.0 : -1.0);
	float VoL = dot(viewPos, lightVec);

	#ifdef OVERWORLD
	float factor = mix(LIGHT_SHAFT_MORNING_FALLOFF, LIGHT_SHAFT_DAY_FALLOFF, timeBrightness);
		  factor = mix(LIGHT_SHAFT_NIGHT_FALLOFF, factor, sunVisibility);
		  factor*= mix(1.0, LIGHT_SHAFT_WEATHER_FALLOFF, rainStrength) * 0.1;
		  factor = min(factor, 0.999);

	float reverseFactor = 1.0 - factor;

	float falloff = clamp(VoL * 0.5 + 0.5, 0.0, 1.0);
	falloff = factor / (1.0 - reverseFactor * falloff) - factor;
	falloff = clamp(falloff * 1.015 / reverseFactor - 0.015, 0.0, 1.0);
	falloff = mix(1.0, falloff, 0.03125 * eBS + 0.96875);
	#endif

	#ifdef NETHER
	float falloff = 1.0;
	#endif
	
	#ifdef END
	VoL = pow(VoL * 0.5 + 0.5, 16.0) * 0.75 + 0.25;

	float VoE = 0.0;
	if (endFlashIntensity > 0.0) {
		VoE = dot(viewPos, normalize(endFlashPosition));
		VoE = pow(VoE * 0.5 + 0.5, 32.0) * endFlashIntensity * 2.0 * clamp(viewLength / 128.0, 0.0, 1.0);
		VoE /= sqrt(VoE * VoE + 1.0);
	}

	float falloff = VoL + VoE;
	#endif

	falloff *= 1.0 - hand;

	return falloff;
}

bool IsRayMarcherHit(float currentDist, float maxDist, float linearZ0, float linearZ1, vec3 translucent) {
	bool isMaxReached = currentDist >= maxDist;
	bool opaqueReached = currentDist > linearZ1;
	bool solidTransparentReached = currentDist > linearZ0 && translucent == vec3(0.0);
	
	return isMaxReached || opaqueReached || solidTransparentReached;
}

float GetLogarithmicDepth(float dist) {
	return (far * (dist - near)) / (dist * (far - near));
}

vec3 GetSampleWorldPos(vec2 texCoord, float linearDepth) {
	float depth = GetLogarithmicDepth(linearDepth);

	vec3 viewPos = ToNDC(vec3(texCoord, depth));
	vec3 worldPos = ToWorld(viewPos);

	return worldPos;
}

vec3 DistortShadow(vec3 shadowPos) {
	float distb = sqrt(dot(shadowPos.xy, shadowPos.xy));
	float distortFactor = 1.0 - shadowMapBias + distb * shadowMapBias;
	
	shadowPos.xy *= 1.0 / distortFactor;
	shadowPos.z = shadowPos.z * 0.2;
	shadowPos = shadowPos * 0.5 + 0.5;

	return shadowPos;
}

bool IsSampleInShadowmap(vec3 shadowPos) {
	return length(shadowPos.xy * 2.0 - 1.0) < 1.0 && shadowPos.z < 0.5;
}

vec3 SampleShadow(vec3 sampleShadowPos) {
	float shadow0 = shadow2D(shadowtex0, sampleShadowPos.xyz).z;
	
	vec3 shadowCol = vec3(0.0);
	#ifdef SHADOW_COLOR
	if (shadow0 < 1.0) {
		float shadow1 = shadow2D(shadowtex1, sampleShadowPos.xyz).z;
		if (shadow1 > 0.0) {
			shadowCol = texture2D(shadowcolor0, sampleShadowPos.xy).rgb;
			shadowCol *= shadowCol * shadow1;
			#ifdef WATER_CAUSTICS
			shadowCol *= 16.0 - 15.0 * (1.0 - (1.0 - shadow0) * (1.0 - shadow0));
			#endif
		}
	}
	#endif

	shadow0 *= shadow0;
	shadowCol *= shadowCol;
	
	vec3 shadow = clamp(shadowCol * (1.0 - shadow0) + shadow0, vec3(0.0), vec3(16.0));

	return shadow;
}

vec3 GetShadowTint(vec3 translucent, vec3 waterTint, float currentDist, float linearZ0) {
	vec3 shadowTint = vec3(1.0);
	if (currentDist > linearZ0) {
		shadowTint = translucent;
	} else {
		if (isEyeInWater == 1.0) {
			#ifdef WATER_SHADOW_COLOR
			shadowTint = vec3(currentDist) / 48.0 * (1.0 + eBS);
			#else
			shadowTint = waterTint * currentDist / 512.0 * (1.0 + eBS);
			#endif
		}
	}

	return shadowTint;
}

float Get3DNoise(vec3 shadow, vec3 worldPos) {
	vec3 noisePos = worldPos + cameraPosition;
	noisePos += vec3(time * 2.0, 0, 0);
	noisePos.xz /= 512.0;

	float yResolution = 3.0;
	float yOffsetScale = 0.35;
	float yLow  = floor(noisePos.y / yResolution) * yOffsetScale;
	float yHigh = yLow + yOffsetScale;
	float yBlend = fract(noisePos.y / yResolution);

	float noiseLow  = texture2D(noisetex, noisePos.xz + yLow).r;
	float noiseHigh = texture2D(noisetex, noisePos.xz + yHigh).r;

	float noise = mix(noiseLow, noiseHigh, yBlend);
	noise = sin(noise * 28.0 + time * 2.0) * 0.25 + 0.5;

	return noise;
}

vec3 HSV2RGB(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 ApplyLightShaftMultiplier(vec3 totalShadow, float falloff) {
	totalShadow *= falloff;
	
	#ifdef OVERWORLD
	totalShadow *= lightCol * 0.25;
	#endif

	#ifdef END
	vec3 flashColor = HSV2RGB(vec3(time * 0.05, 0.25, 1.0));
	vec3 shadowColor = endCol.rgb * mix(vec3(1.0), flashColor, endFlashIntensity);

	totalShadow *= shadowColor * 0.1;
	#endif

	totalShadow *= LIGHT_SHAFT_STRENGTH * (1.0 - rainStrength * eBS * 0.875) * shadowFade;
	
	#ifdef IS_IRIS
	totalShadow *= clamp((cameraPosition.y - bedrockLevel + 6.0) / 8.0, 0.0, 1.0);
	#else
	#if MC_VERSION >= 11800
	totalShadow *= clamp((cameraPosition.y + 70.0) / 8.0, 0.0, 1.0);
	#else
	totalShadow *= clamp((cameraPosition.y + 6.0) / 8.0, 0.0, 1.0);
	#endif
	#endif
	
	#ifdef SKY_UNDERGROUND
	totalShadow *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif

	return totalShadow;
}

vec3 ApplyBlocklightFogMultiplier(vec3 totalBlocklight, float maxDist) {
	float blocklightIntensity = length(totalBlocklight);

	vec3 blocklightColor = normalize(totalBlocklight * totalBlocklight + 1e-5);

	totalBlocklight = blocklightIntensity * blocklightColor;
	
	totalBlocklight *= MCBL_FOG_STRENGTH * 0.5 / maxDist;
	totalBlocklight *= BLOCKLIGHT_I * BLOCKLIGHT_I;

	#ifdef OVERWORLD
	totalBlocklight *= 1.0 + 3.0 * rainStrength * eBS;
	#endif

	#ifdef NETHER
	vec3 netherTint = netherColSqrt.rgb + 1e-5;
	netherTint /= max(max(netherTint.r, netherTint.g), netherTint.b);
	netherTint = netherTint + 0.25;

	totalBlocklight *= netherTint;
	#endif
	
	#ifdef END
	totalBlocklight *= 4.0;
	#endif

	return totalBlocklight;
}

//Light shafts based on Robobo1221's implementation
vec3 GetLightShafts(vec3 viewPos, float linZ0, float linZ1, float hand, float dither, vec3 translucent) {
	vec3 vl = vec3(0.0);
	vec3 totalShadow = vec3(0.0);
	vec3 totalBlocklight = vec3(0.0);

	#ifdef TAA
	#if TAA_MODE == 0
	dither = fract(dither + frameCounter * 0.618);
	#else
	dither = fract(dither + frameCounter * 0.5);
	#endif
	#endif
	
	float falloff = GetLightShaftsFalloff(viewPos, hand);

	float maxDist = 128.0;
	
	float linearZ0 = linZ0;
	float linearZ1 = linZ1;

	vec3 worldPos = ToWorld(viewPos);
	
	vec3 waterTint = waterColor.rgb / (waterColor.a * waterColor.a);
	waterTint = mix(vec3(1.0), waterTint, pow(waterAlpha, 0.25));
	
	#ifndef MCBL_FOG
	if (falloff < 0.0) {
		return vec3(0.0);
	}
	#endif

	#ifdef NETHER
	vec3 enableShadows = SampleShadow(vec3(0.0));
	totalShadow += max(enableShadows - vec3(1.0), vec3(0.0));
	#endif

	for(int i = 0; i < 7; i++) {
		float currentDist = exp2(i + dither) - 0.95;

		if (IsRayMarcherHit(currentDist, maxDist, linearZ0, linearZ1, translucent)) break;
		
		vec3 shadowTint = GetShadowTint(translucent, waterTint, currentDist, linearZ0);

		vec3 sampleWorldPos = GetSampleWorldPos(texCoord, currentDist);
		vec3 sampleShadowPos = ToShadow(sampleWorldPos);
		sampleShadowPos = DistortShadow(sampleShadowPos);

		sampleShadowPos.z += 0.0512 / shadowMapResolution;

		vec3 shadow = vec3(0.0);
		vec3 blocklight = vec3(0.0);

		#ifdef END
		float noise3D = Get3DNoise(shadow, sampleWorldPos);
		#endif

		#ifndef NETHER
		bool isShadowSampleValid = IsSampleInShadowmap(sampleShadowPos);
		#ifdef MCBL_FOG
		isShadowSampleValid = isShadowSampleValid && falloff > 0.0;
		#endif

		if (isShadowSampleValid) {
			shadow = SampleShadow(sampleShadowPos);
		} else {
			shadow = vec3(1.0);
		}
		shadow *= shadowTint;
		#ifdef END
		shadow *= mix(0.5, noise3D, LIGHT_SHAFT_END_SMOKE);
		#endif

		totalShadow += shadow;
		#endif

		#if defined MULTICOLORED_BLOCKLIGHT && defined MCBL_FOG
		vec3 blocklightSampleWorldPos = sampleWorldPos;
		#ifdef WORLD_CURVATURE
		blocklightSampleWorldPos.y += dot(blocklightSampleWorldPos.xz, blocklightSampleWorldPos.xz) / WORLD_CURVATURE_SIZE;
		#endif

		vec3 voxelMapPos = WorldToVoxel(blocklightSampleWorldPos);

		if (IsInVoxelMapVolume(voxelMapPos)) {
			voxelMapPos /= voxelMapSize;

			int iFrameMod2 = int(frameCounter % 2);

			if (iFrameMod2 == 0) {
				blocklight = texture3D(lighttex0, voxelMapPos).rgb;
			} else {
				blocklight = texture3D(lighttex1, voxelMapPos).rgb;
			}

			blocklight *= currentDist;
			blocklight *= step(length(sampleWorldPos.xz), maxDist);
		}
		blocklight *= shadowTint;
		#ifdef END
		blocklight *= noise3D;
		#endif

		totalBlocklight += blocklight;
		#endif
	}

	totalShadow = ApplyLightShaftMultiplier(totalShadow, falloff);
	
	#if defined MULTICOLORED_BLOCKLIGHT && defined MCBL_FOG
	totalBlocklight = ApplyBlocklightFogMultiplier(totalBlocklight, maxDist);
	#endif

	vl = (totalShadow + totalBlocklight) / 7.0;
	vl *= 1.0 - max(blindFactor, darknessFactor);

	vl = pow(vl / 32.0, vec3(0.25));
	if(dot(vl, vl) > 0.0) vl += (dither - 0.25) / 128.0;
	
	return vl;
}