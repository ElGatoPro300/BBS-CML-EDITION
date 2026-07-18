#ifdef SHADOW
#ifndef VOXY_PATCH
uniform sampler2DShadow shadowtex0;

#ifdef SHADOW_COLOR
uniform sampler2DShadow shadowtex1;
uniform sampler2D shadowcolor0;
#endif

// uniform sampler2D shadowtex0;

// #ifdef SHADOW_COLOR
// uniform sampler2D shadowtex1;
// uniform sampler2D shadowcolor0;
// #endif

vec2 shadowOffsets[9] = vec2[9](
    vec2( 0.0, 0.0),
    vec2( 0.0, 1.0),
    vec2( 0.7, 0.7),
    vec2( 1.0, 0.0),
    vec2( 0.7,-0.7),
    vec2( 0.0,-1.0),
    vec2(-0.7,-0.7),
    vec2(-1.0, 0.0),
    vec2(-0.7, 0.7)
);

// float biasDistribution[10] = float[10](
//     0.0, 0.057, 0.118, 0.184, 0.255, 0.333, 0.423, 0.529, 0.667, 1.0
// );

// float getShadow(sampler2D shadowtex, vec2 shadowPosXY, float shadowPosZ) {
//     float shadow = texture2D(shadowtex,shadowPosXY).x;
//           shadow = clamp((shadow - shadowPosZ)*16384.0+0.5,0.0,1.0);
//     return shadow;
// }

// float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
//     shadowPos.xy -= 0.5 / shadowMapResolution;
//     vec2 frac = fract(shadowPos.xy * shadowMapResolution);
//     shadowPos.xy = (floor(shadowPos.xy * shadowMapResolution) + 0.5) / shadowMapResolution;

//     float shadow0 = getShadow(shadowtex,shadowPos.st + vec2(0.0, 0.0) / shadowMapResolution, shadowPos.z);
//     float shadow1 = getShadow(shadowtex,shadowPos.st + vec2(0.0, 1.0) / shadowMapResolution, shadowPos.z);
//     float shadow2 = getShadow(shadowtex,shadowPos.st + vec2(1.0, 0.0) / shadowMapResolution, shadowPos.z);
//     float shadow3 = getShadow(shadowtex,shadowPos.st + vec2(1.0, 1.0) / shadowMapResolution, shadowPos.z);

//     float shadowx0 = mix(shadow0, shadow1, frac.y);
//     float shadowx1 = mix(shadow2, shadow3, frac.y);

//     float shadow = mix(shadowx0, shadowx1, frac.x);

//     return shadow;
// }

// float texture2DShadow(sampler2D shadowtex, vec3 shadowPos) {
//     return texture2DShadow2x2(shadowtex, shadowPos);
// }

float texture2DShadow(sampler2DShadow shadowtex, vec3 shadowPos) {
    return shadow2D(shadowtex, shadowPos).x;
}

vec3 DistortShadow(vec3 shadowPos, float distortFactor) {
	shadowPos.xy /= distortFactor;
	shadowPos.z *= 0.2;
	shadowPos = shadowPos * 0.5 + 0.5;

    return shadowPos;
}

// float GetCurvedBias(int i, float dither) {
//     return mix(biasDistribution[i], biasDistribution[i+1], dither);
// }

float InterleavedGradientNoise() {
	float n = 52.9829189 * fract(0.06711056 * gl_FragCoord.x + 0.00583715 * gl_FragCoord.y);
	return fract(n + frameCounter * 1.618);
}

vec3 SampleBasicShadow(vec3 shadowPos, float subsurface) {
    float shadow0 = texture2DShadow(shadowtex0, vec3(shadowPos.st, shadowPos.z));

    vec3 shadowCol = vec3(0.0);
    #ifdef SHADOW_COLOR
    if (shadow0 < 1.0) {
        shadowCol = texture2D(shadowcolor0, shadowPos.st).rgb *
                    texture2DShadow(shadowtex1, vec3(shadowPos.st, shadowPos.z));
        #ifdef WATER_CAUSTICS
        shadowCol *= 4.0;
        #endif
    }
    #endif

    shadow0 *= mix(shadow0, 1.0, subsurface);
    shadowCol *= shadowCol;

    return clamp(shadowCol * (1.0 - shadow0) + shadow0, vec3(0.0), vec3(16.0));
}

vec3 SampleFilteredShadow(vec3 shadowPos, float offset, float subsurface) {
    float shadow0 = 0.0;
    
    for (int i = 0; i < 9; i++) {
        vec2 shadowOffset = shadowOffsets[i] * offset;
        shadow0 += texture2DShadow(shadowtex0, vec3(shadowPos.st + shadowOffset, shadowPos.z));
    }
    shadow0 /= 9.0;

    vec3 shadowCol = vec3(0.0);
    #ifdef SHADOW_COLOR
    if (shadow0 < 0.999) {
        for (int i = 0; i < 9; i++) {
            vec2 shadowOffset = shadowOffsets[i] * offset;
            vec3 shadowColSample = texture2D(shadowcolor0, shadowPos.st + shadowOffset).rgb *
                         texture2DShadow(shadowtex1, vec3(shadowPos.st + shadowOffset, shadowPos.z));
            #ifdef WATER_CAUSTICS
            shadowColSample *= 4.0;
            #endif
            shadowCol += shadowColSample;
        }
        shadowCol /= 9.0;
    }
    #endif

    shadow0 *= mix(shadow0, 1.0, subsurface);
    shadowCol *= shadowCol;

    return clamp(shadowCol * (1.0 - shadow0) + shadow0, vec3(0.0), vec3(16.0));
}

vec3 GetShadow(vec3 worldPos, vec3 normal, float NoL, float subsurface, float skylight) {
    #if SHADOW_PIXEL > 0
    worldPos = (floor((worldPos + cameraPosition) * SHADOW_PIXEL + 0.01) + 0.5) /
               SHADOW_PIXEL - cameraPosition;
    #endif
    
    vec3 rawShadowPos = ToShadow(worldPos);

    float distb = sqrt(dot(rawShadowPos.xy, rawShadowPos.xy));
    float distortFactor = distb * shadowMapBias + (1.0 - shadowMapBias);

    #if SHADOW_BIAS == 1 || defined HALF_LAMBERT
    if (subsurface == 0) {
        float distortNBias = distortFactor * shadowDistance / 256.0;
        distortNBias *= distortNBias;
        
        vec3 worldNormal = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
        worldPos += worldNormal * distortNBias * 5.0 * (2048.0 / shadowMapResolution);
        rawShadowPos = ToShadow(worldPos);

        distb = sqrt(dot(rawShadowPos.xy, rawShadowPos.xy));
        distortFactor = distb * shadowMapBias + (1.0 - shadowMapBias);
    }
    #endif

    vec3 shadowPos = DistortShadow(rawShadowPos, distortFactor);

    float shadowFade = clamp(100.0 - 100.0 * max(abs(rawShadowPos.x), abs(rawShadowPos.y)), 0.0, 1.0);

    #ifdef OVERWORLD
    shadowFade *= clamp(skylight * 1000.0 - 1.0, 0.0, 1.0);
    #endif

    #if defined DISTANT_HORIZONS && !defined VOXY
    #ifdef SHADOW_LOD
    float dither = Bayer8(gl_FragCoord.xy);

    float viewLength = length(worldPos);
    float minDist = (dither - 0.75) * 16.0 + far;
    if (viewLength > minDist) {
        shadowFade *= 0.0;
    }
    #else
    shadowFade *= smoothstep(far + 4.0, far - 12.0, length(worldPos));
    #endif
    #endif

    if (shadowFade < 0.00001) return vec3(1.0);
    
    float bias = 0.0;
    float offset = 1.0 / shadowMapResolution;

    #if !(SHADOW_BIAS == 1 || defined HALF_LAMBERT)
    float biasFactor = sqrt(1.0 - NoL * NoL) / NoL;
    float distortBias = distortFactor * shadowDistance / 256.0;
    distortBias *= 8.0 * distortBias;
    float distanceBias = sqrt(dot(worldPos.xyz, worldPos.xyz)) * 0.005;

    bias = (distortBias * biasFactor + distanceBias + 0.05) / shadowMapResolution;
    #else
    bias = 0.03125 / shadowMapResolution;
    #endif
    
    if (subsurface > 0.0) {
        float blurFadeIn = clamp(distb * 20.0, 0.0, 1.0);
        float blurFadeOut = 1.0 - clamp(distb * 10.0 - 2.0, 0.0, 1.0);
        float blurMult = blurFadeIn * blurFadeOut * (1.0 - NoL);
        blurMult = blurMult * 1.5 + 1.0;

        offset = 0.0007 * blurMult;
        bias = 0.0002;
    }
    
    #if SHADOW_PIXEL > 0
    bias += 0.0025 / SHADOW_PIXEL;
    #endif

    shadowPos.z -= bias;

    #ifdef SHADOW_FILTER
    vec3 shadow = SampleFilteredShadow(shadowPos, offset, subsurface);
    #else
    vec3 shadow = SampleBasicShadow(shadowPos, subsurface);
    #endif

    shadow = mix(vec3(1.0), shadow, shadowFade);

    return shadow;
}

vec3 GetSubsurfaceShadow(vec3 worldPos, float subsurface, float skylight) {
    float gradNoise = InterleavedGradientNoise();
    
    vec3 shadowPos = ToShadow(worldPos);

    float distb = sqrt(dot(shadowPos.xy, shadowPos.xy));
    float distortFactor = distb * shadowMapBias + (1.0 - shadowMapBias);
    shadowPos = DistortShadow(shadowPos, distortFactor);

    vec3 subsurfaceShadow = vec3(0.0);
    
    vec3 offsetScale = vec3(0.002 / distortFactor, 0.002 / distortFactor, 0.001) * (subsurface * 0.75 + 0.25);

    for(int i = 0; i < 12; i++) {
        gradNoise = fract(gradNoise + 1.618);
        float rot = gradNoise * 6.283;
        float dist = (i + gradNoise) / 12.0;

        vec2 offset2D = vec2(cos(rot), sin(rot)) * dist;
        float offsetZ = -(dist * dist + 0.025);

        vec3 offset = vec3(offset2D, offsetZ) * offsetScale;

        vec3 samplePos = shadowPos + offset;
        float shadow0 = texture2DShadow(shadowtex0, samplePos);

        vec3 shadowCol = vec3(0.0);
        #ifdef SHADOW_COLOR
        if (shadow0 < 1.0) {
            shadowCol = texture2D(shadowcolor0, samplePos.st).rgb *
                        texture2DShadow(shadowtex1, samplePos);
            #ifdef WATER_CAUSTICS
            shadowCol *= 4.0;
            #endif
        }
        #endif

        subsurfaceShadow += clamp(shadowCol * (1.0 - shadow0) + shadow0, vec3(0.0), vec3(1.0));
    }
    subsurfaceShadow /= 12.0;
    subsurfaceShadow *= subsurfaceShadow;

    return subsurfaceShadow;
}
#else
vec3 GetShadow(vec3 worldPos, vec3 normal, float NoL, float subsurface, float skylight) {
    return vec3(1.0);
}

vec3 GetSubsurfaceShadow(vec3 worldPos, float subsurface, float skylight) {
    return vec3(0.0);
}
#endif
#else
vec3 GetShadow(vec3 worldPos, vec3 normal, float NoL, float subsurface, float skylight) {
    #ifdef OVERWORLD
    float skylightShadow = smoothstep(SHADOW_SKY_FALLOFF, 1.0, skylight);
    skylightShadow *= skylightShadow;

    return vec3(skylightShadow);
    #else
    return vec3(1.0);
    #endif
}

vec3 GetSubsurfaceShadow(vec3 worldPos, float subsurface, float skylight) {
    return vec3(0.0);
}
#endif

float GetCloudShadow(vec3 worldPos) {
	vec2 wind = vec2(
		time * CLOUD_SPEED * 0.0005,
		sin(time * CLOUD_SPEED * 0.001) * 0.005
	) * 0.667;

    vec3 coveragePos = worldPos;
    worldPos += cameraPosition;

    vec3 worldLightVec = (gbufferModelViewInverse * vec4(lightVec, 0.0)).xyz;

    #if CLOUD_HEIGHT == -1
	#ifdef IS_IRIS
	float cloudLowerY = cloudHeight;
	#else
	float cloudLowerY = 192.0;
	#endif
	#else
	float cloudLowerY = float(CLOUD_HEIGHT);
	#endif

    worldPos.xz += worldLightVec.xz / worldLightVec.y * max(cloudLowerY - worldPos.y, 0.0);
    coveragePos.xz += worldLightVec.xz / worldLightVec.y * -coveragePos.y;

    float scaledThickness = CLOUD_THICKNESS * CLOUD_SCALE;
    float cloudFadeOut = 1.0 - clamp((worldPos.y - cloudLowerY) / scaledThickness, 0.0, 1.0);
    float coverageFadeOut = 1.0 - clamp((cameraPosition.y - cloudLowerY) / scaledThickness, 0.0, 1.0);

    vec2 coord = worldPos.xz / CLOUD_SCALE;
    
	#ifdef CLOUD_REVEAL
    float sunCoverageSize = CLOUD_SCALE * 3.0 / worldLightVec.y;
    float sunCoverage = max(1.0 - length(coveragePos.xz) / sunCoverageSize, 0.0) * coverageFadeOut;
    #else
    float sunCoverage = 0.0;
    #endif

	coord *= 0.004 * CLOUD_STRETCH;

	#if CLOUD_BASE == 0
    coord = coord * 0.25 + wind;
	float noiseBase = texture2D(noisetex, coord).r;
    
	float noise = mix(noiseBase, 1.0, 0.33 * rainStrength) * 21.0;
	noise = max(noise - (sunCoverage * 3.0 + CLOUD_AMOUNT), 0.0);
	#elif CLOUD_BASE == 1
    coord = coord * 0.5 + wind * 2.0;

	float noiseBase = texture2D(noisetex, coord).g;
	noiseBase = pow(1.0 - noiseBase, 2.0) * 0.5 + 0.25;
    
	float noise = mix(noiseBase, 1.0, 0.33 * rainStrength) * 21.0;
	noise = max(noise - (sunCoverage * 3.0 + CLOUD_AMOUNT), 0.0);
    #else
    coord = coord * 0.15625 + wind * 0.5;
    
	float noiseRes = 512.0;

	coord.xy = coord.xy * noiseRes - 0.5;

	vec2 flr = floor(coord.xy);
	vec2 frc = coord.xy - flr;

	frc = clamp(frc * 2.0 - 0.5, vec2(0.0), vec2(1.0));
	frc = frc * frc * (3.0 - 2.0 * frc);

	coord.xy = (flr + frc + 0.5) / noiseRes;

	float noiseBase = texture2D(noisetex, coord).a;
	noiseBase = (1.0 - noiseBase) * 4.0;

    float noise = max(noiseBase * 2.0, 0.0);
	#endif

	noise *= CLOUD_DENSITY * 0.125;
	noise *= (1.0 - 0.75 * rainStrength);
	noise = noise / sqrt(noise * noise + 0.5);
    noise *= cloudFadeOut;

	return 1.0 - noise * CLOUD_OPACITY * 0.85;
}