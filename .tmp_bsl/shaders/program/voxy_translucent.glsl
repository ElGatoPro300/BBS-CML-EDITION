/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH
#define VOXY_PATCH
#define texture2D texture
#define texture2DLod textureLod

layout(location = 0) out vec4 gbufferData0;
layout(location = 1) out vec4 gbufferData1;
#ifdef MCBL_SS
layout(location = 2) out vec4 gbufferData2;
#endif

#undef MULTICOLORED_BLOCKLIGHT

/*
struct VoxyFragmentParameters {
	vec4 sampledColour;
	vec2 tile;
	vec2 uv;
	uint face;
	uint modelId;
	vec2 lightMap;
	vec4 tinting;
	uint customId; // Same as iris's modelId
};
*/

//Common Variables//
mat4 gbufferModelView = vxModelView;
mat4 gbufferModelViewInverse = vxModelViewInv;
mat4 gbufferPreviousModelView = vxModelViewPrev;
mat4 gbufferProjection = vxProj;
mat4 gbufferProjectionInverse = vxProjInv;
mat4 gbufferPreviousProjection = vxProjPrev;

const vec2 sunRotationData = vec2(cos(sunPathRotation * 0.01745329251994), -sin(sunPathRotation * 0.01745329251994));
float ang1 = fract(timeAngle - 0.25);
float ang = (ang1 + (cos(ang1 * 3.14159265358979) * -0.5 + 0.5 - ang1) / 3.0) * 6.28318530717959;
vec3 sunVec = normalize((gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);
vec3 upVec = normalize(gbufferModelView[1].xyz);
vec3 eastVec = normalize(gbufferModelView[0].xyz);

float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);
float moonVisibility = clamp(dot(-sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

vec3 lightVec = sunVec * ((timeAngle < 0.5325 || timeAngle > 0.9675) ? 1.0 : -1.0);

////Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

float GetWaterHeightMap(vec3 worldPos, vec2 offset) {
	float noise = 0.0, noiseA = 0.0, noiseB = 0.0;

	vec2 wind = vec2(time) * 0.5 * WATER_SPEED;

	worldPos.xz += worldPos.y * 0.2;

	#if WATER_NORMALS == 1
	offset /= 256.0;
	noiseA = texture2D(noisetex, (worldPos.xz - wind) / 256.0 + offset).g;
	noiseB = texture2D(noisetex, (worldPos.xz + wind) / 48.0 + offset).g;
	#elif WATER_NORMALS == 2
	offset /= 256.0;
	noiseA = texture2D(noisetex, (worldPos.xz - wind) / 256.0 + offset).r;
	noiseB = texture2D(noisetex, (worldPos.xz + wind) / 96.0 + offset).r;
	noiseA *= noiseA; noiseB *= noiseB;
	#endif

	#if WATER_NORMALS > 0
	noise = mix(noiseA, noiseB, WATER_DETAIL);
	#endif

	return noise * WATER_BUMP;
}

vec3 GetParallaxWaves(vec3 worldPos, vec3 viewVector, float dist) {

	vec3 parallaxPos = worldPos;

	for(int i = 0; i < 4; i++) {
		float height = -1.25 * GetWaterHeightMap(parallaxPos, vec2(0.0)) + 0.25;
		parallaxPos.xz += height * viewVector.xy / dist;
	}
	return parallaxPos;
}

vec3 GetWaterNormal(vec3 worldPos, vec3 viewPos, vec3 viewVector, vec3 normal) {
	vec3 waterPos = worldPos + cameraPosition;

	#if WATER_PIXEL > 0
	waterPos = floor(waterPos * WATER_PIXEL) / WATER_PIXEL;
	#endif

	#ifdef WATER_PARALLAX
	float dist = length(viewVector);
	waterPos = GetParallaxWaves(waterPos, viewVector, dist);
	#endif

	float normalOffset = WATER_SHARPNESS;

	float fresnel = pow(clamp(1.0 + dot(normalize(normal), normalize(viewPos)), 0.0, 1.0), 8.0);
	float normalStrength = 0.35 * (1.0 - fresnel);

	float h1 = GetWaterHeightMap(waterPos, vec2( normalOffset, 0.0));
	float h2 = GetWaterHeightMap(waterPos, vec2(-normalOffset, 0.0));
	float h3 = GetWaterHeightMap(waterPos, vec2(0.0,  normalOffset));
	float h4 = GetWaterHeightMap(waterPos, vec2(0.0, -normalOffset));

	float xDelta = (h2 - h1) / normalOffset;
	float yDelta = (h4 - h3) / normalOffset;

	vec3 normalMap = vec3(xDelta, yDelta, 1.0 - (xDelta * xDelta + yDelta * yDelta));
	return normalMap * normalStrength + vec3(0.0, 0.0, 1.0 - normalStrength);
}

//Includes//
#include "/lib/color/blocklightColor.glsl"
#include "/lib/color/dimensionColor.glsl"
#include "/lib/color/lightSkyColor.glsl"
#include "/lib/color/skyColor.glsl"
#include "/lib/color/specularColor.glsl"
#include "/lib/color/waterColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/atmospherics/sky.glsl"
#include "/lib/atmospherics/clouds.glsl"
#include "/lib/atmospherics/fog.glsl"
#include "/lib/atmospherics/waterFog.glsl"
#include "/lib/lighting/forwardLighting.glsl"

#include "/lib/reflections/raytrace.glsl"
#include "/lib/reflections/simpleReflections.glsl"
#include "/lib/surface/ggx.glsl"
#include "/lib/surface/hardcodedEmission.glsl"

#ifdef TAA
#include "/lib/util/jitter.glsl"
#endif

#ifdef MCBL_SS
#include "/lib/util/voxelMapHelper.glsl"
#include "/lib/lighting/coloredBlocklight.glsl"
#endif

//Program//
void voxy_emitFragment(VoxyFragmentParameters parameters) {
	uint blockID = parameters.customId / 100;
	vec4 color = parameters.tinting;

	vec4 albedo = parameters.sampledColour * vec4(color.rgb,1.0);

	float smoothness = 0.0;
	vec3 lightAlbedo = vec3(0.0);

	vec3 vlAlbedo = vec3(1.0);
	vec3 refraction = vec3(0.0);

	float cloudBlendOpacity = 1.0;

	{
		vec2 lightmap = clamp((parameters.lightMap - 0.03125) * 1.06667, vec2(0.0), vec2(0.9333, 1.0));

		float water       = float(blockID == 200 || blockID == 204);
		float glass       = float(blockID == 201);
		float translucent = float(blockID == 202);
		float portal      = 0.0; // Portal is rendered in opaque pass 

		float metalness       = 0.0;
		float emission        = portal;
		float subsurface      = 0.0;
		float basicSubsurface = water;
		vec3 baseReflectance  = vec3(0.04);

		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);

		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = mat3(vxModelViewInv) * viewPos + vxModelViewInv[3].xyz;

		float dither = Bayer8(gl_FragCoord.xy);

		vec3 normal = vec3(0.0);

		switch (uint(parameters.face) >> 1u) {
			case 0u:
			normal = vxModelView[1].xyz;
			break;
			case 1u:
			normal = vxModelView[2].xyz;
			break;
			case 2u:
			normal = vxModelView[0].xyz;
			break;
		}
		if ((parameters.face & 1) == 0) {
			normal = -normal;
		}

		vec3 newNormal = normal;

		#if WATER_NORMALS > 0
		vec3 tangent = vxModelView[0].xyz;
		vec3 binormal = vxModelView[2].xyz;

		vec3 normalMap = vec3(0.0, 0.0, 1.0);

		mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
							  tangent.y, binormal.y, normal.y,
							  tangent.z, binormal.z, normal.z);

		vec3 viewVector = vec3(worldPos.x,worldPos.z,0);
		#endif

		#if WATER_NORMALS > 0
		if (water > 0.5) {
			normalMap = GetWaterNormal(worldPos, viewPos, viewVector,normal);
			newNormal = clamp(normalize(normalMap * tbnMatrix), vec3(-1.0), vec3(1.0));
		}
		#endif

		#ifdef TOON_LIGHTMAP
		lightmap = floor(lightmap * 14.999) / 14.0;
		lightmap = clamp(lightmap, vec2(0.0), vec2(1.0));
		#endif

		albedo.rgb = pow(albedo.rgb, vec3(2.2));

		vlAlbedo = albedo.rgb;

		#ifdef MCBL_SS
		vec3 opaquelightAlbedo = texture2D(colortex8, screenPos.xy).rgb;
		if (water < 0.5) {
			opaquelightAlbedo *= vlAlbedo;
		}
		lightAlbedo = albedo.rgb + 0.00001;

		lightAlbedo = normalize(lightAlbedo + 0.00001) * emission;
		lightAlbedo = mix(opaquelightAlbedo, sqrt(lightAlbedo), albedo.a);

		#ifdef MULTICOLORED_BLOCKLIGHT
		lightAlbedo *= GetMCBLLegacyMask(worldPos);
		#endif
		#endif

		#ifndef REFLECTION_TRANSLUCENT
		glass = 0.0;
		translucent = 0.0;
		#endif

		#ifdef WHITE_WORLD
		albedo.rgb = vec3(0.35);
		#endif

		if (water > 0.5) {
			#if WATER_MODE == 0
			albedo.rgb = waterColor.rgb * waterColor.a;
			#elif WATER_MODE == 1
			albedo.rgb *= WATER_VI * WATER_VI;
			#elif WATER_MODE == 2
			float waterLuma = length(albedo.rgb / pow(color.rgb, vec3(2.2))) * 2.0;
			albedo.rgb = waterLuma * waterColor.rgb * waterColor.a;
			#elif WATER_MODE == 3
			albedo.rgb = color.rgb * color.rgb * WATER_VI * WATER_VI;
			#endif
			#if WATER_ALPHA_MODE == 0
			albedo.a = waterAlpha;
			#else
			albedo.a = pow(albedo.a, WATER_VA);
			#endif
			vlAlbedo = sqrt(albedo.rgb);
			baseReflectance = vec3(0.02);
		}

		vlAlbedo = mix(vec3(1.0), vlAlbedo, sqrt(albedo.a)) * (1.0 - pow(albedo.a, 64.0));

		#ifndef HALF_LAMBERT
		float NoL = clamp(dot(newNormal, lightVec), 0.0, 1.0);
		#else
		float NoL = clamp(dot(newNormal, lightVec) * 0.5 + 0.5, 0.0, 1.0);
		NoL *= NoL;
		#endif

		float NoU = clamp(dot(newNormal, upVec), -1.0, 1.0);
		float NoE = clamp(dot(newNormal, eastVec), -1.0, 1.0);
		float vanillaDiffuse = (0.25 * NoU + 0.75) + (0.667 - abs(NoE)) * (1.0 - abs(NoU)) * 0.15;
		vanillaDiffuse*= vanillaDiffuse;

		vec3 shadow = vec3(0.0);
			GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, 1.0, NoL,
			vanillaDiffuse, 1.0, emission, subsurface, basicSubsurface);


		float fresnel = pow(clamp(1.0 + dot(newNormal, normalize(viewPos)), 0.0, 1.0), 5.0);

		if (water > 0.5 || ((translucent + glass) > 0.5 && albedo.a < 0.95)) {
			#if REFLECTION > 0
			vec4 reflection = vec4(0.0);
			vec3 skyReflection = vec3(0.0);
			float reflectionMask = 0.0;

			fresnel = fresnel * 0.98 + 0.02;
			fresnel*= max(1.0 - isEyeInWater * 0.5 * water, 0.5);

			#if REFLECTION == 2
			reflection = SimpleReflection(viewPos, newNormal, dither, reflectionMask);
			reflection.rgb = pow(reflection.rgb * 2.0, vec3(8.0));
			#endif

			if (reflection.a < 1.0) {
				#ifdef OVERWORLD
				vec3 skyRefPos = reflect(normalize(viewPos), newNormal);
				skyReflection = GetSkyColor(skyRefPos, true);

				#if AURORA > 0
				skyReflection += DrawAurora(skyRefPos * 100.0, dither, 12);
				#endif

				#if CLOUDS == 1
				vec4 cloud = DrawCloudSkybox(skyRefPos * 100.0, 1.0, dither, lightCol, ambientCol, true);
				skyReflection = mix(skyReflection, cloud.rgb, cloud.a);
				#endif
				#if CLOUDS == 2
				vec3 cameraPos = GetReflectedCameraPos(worldPos, newNormal);
				float cloudViewLength = 0.0;

				vec4 cloud = DrawCloudVolumetric(skyRefPos * 8192.0, cameraPos, 1.0, dither, lightCol, ambientCol, cloudViewLength, true);
				skyReflection = mix(skyReflection, cloud.rgb, cloud.a);
				#endif

				#ifdef CLASSIC_EXPOSURE
				skyReflection *= 4.0 - 3.0 * eBS;
				#endif

				float waterSkyOcclusion = lightmap.y;
				#if REFLECTION_SKY_FALLOFF > 1
				waterSkyOcclusion = clamp(1.0 - (1.0 - waterSkyOcclusion) * REFLECTION_SKY_FALLOFF, 0.0, 1.0);
				#endif
				waterSkyOcclusion *= waterSkyOcclusion;
				skyReflection *= waterSkyOcclusion;
				#endif

				#ifdef NETHER
				skyReflection = netherCol.rgb * 0.04;
				#endif

				#ifdef END
				skyReflection = endCol.rgb * 0.01;
				#endif

				skyReflection *= clamp(1.0 - isEyeInWater, 0.0, 1.0);
			}

			reflection.rgb = max(mix(skyReflection, reflection.rgb, reflection.a), vec3(0.0));

			#if (defined OVERWORLD || defined END) && SPECULAR_HIGHLIGHT == 2
			vec3 specularColor = GetSpecularColor(lightmap.y, 0.0, vec3(1.0));

				vec3 specular = GetSpecularHighlight(newNormal, viewPos,  0.9, vec3(0.02),
				specularColor, shadow, color.a);
			#if ALPHA_BLEND == 0
			float specularAlpha = pow(mix(albedo.a, 1.0, fresnel), 2.2) * fresnel;
			#else
			float specularAlpha = mix(albedo.a , 1.0, fresnel) * fresnel;
			#endif

				reflection.rgb += specular * (1.0 - reflectionMask) / specularAlpha;
			#endif

			albedo.rgb = mix(albedo.rgb, reflection.rgb, fresnel);
			albedo.a = mix(albedo.a, 1.0, fresnel);

			#endif
		}

		Fog(albedo.rgb, viewPos);

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		#endif

	}
	albedo.a *= cloudBlendOpacity;

	gbufferData0 = albedo;
	gbufferData1 = vec4(vlAlbedo, 1.0);

	#ifdef MCBL_SS
	gbufferData2 = vec4(lightAlbedo, 1.0);
	#endif
}

#endif