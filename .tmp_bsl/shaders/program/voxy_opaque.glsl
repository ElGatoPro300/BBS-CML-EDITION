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
vec3 sunVec = normalize((vxModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);
vec3 upVec = normalize(vxModelView[1].xyz);
vec3 eastVec = normalize(vxModelView[0].xyz);

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

float GetBlueNoise3D(vec3 pos, vec3 normal) {
	pos = (floor(pos + 0.01) + 0.5) / 512.0;

	vec3 worldNormal = (vxModelViewInv * vec4(normal, 0.0)).xyz;
	vec3 noise3D = vec3(
	texture2D(noisetex, pos.yz).b,
	texture2D(noisetex, pos.xz).b,
	texture2D(noisetex, pos.xy).b
	);

	float noiseX = noise3D.x * abs(worldNormal.x);
	float noiseY = noise3D.y * abs(worldNormal.y);
	float noiseZ = noise3D.z * abs(worldNormal.z);
	float noise = noiseX + noiseY + noiseZ;

	return noise - 0.5;
}

////Includes//
#include "/lib/color/blocklightColor.glsl"
#include "/lib/color/dimensionColor.glsl"
#include "/lib/color/lightSkyColor.glsl"
#include "/lib/color/skyColor.glsl"
#include "/lib/color/specularColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/atmospherics/sky.glsl"
#include "/lib/atmospherics/fog.glsl"
#include "/lib/surface/ggx.glsl"
#include "/lib/surface/hardcodedEmission.glsl"
#include "/lib/lighting/forwardLighting.glsl"

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
	
	vec4 albedo = parameters.sampledColour * vec4(color.rgb, 1.0);
	float shadowMask = 0.0;
	vec3 lightAlbedo = vec3(0.0);
	
	float foliage   = float(blockID >= 100 && blockID < 150);
	float leaves    = float(blockID == 105 || blockID == 106);
	float emissive  = float(blockID >= 150 && blockID < 200);
	float lava      = float(blockID == 153);
	float candle    = float(blockID == 158);
	float ore       = float(blockID == 159);
	float netherOre = float(blockID == 160);
	float portal = float(blockID == 203);

	foliage -= leaves;
	emissive -= (lava + ore + netherOre);

	float metalness       = 0.0;
	float emission        = (emissive + candle + lava + portal);
	float subsurface      = 0.0;
	float basicSubsurface = (foliage + candle + leaves) * 0.5;
	vec3 baseReflectance  = vec3(0.04);

	vec2 lightmap = clamp((parameters.lightMap - 0.03125) * 1.06667, vec2(0.0), vec2(0.9333, 1.0));

	float rawEmission = emission;

	if(leaves > 0.5){
		albedo.rgb *= 1.225;
		albedo.a = 1.0;
	}

	if (lava > 0.5) {
		lightmap.x = 1.0;
	}

	{
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);

		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = mat3(vxModelViewInv) * viewPos + vxModelViewInv[3].xyz;

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

		#ifdef TOON_LIGHTMAP
		lightmap = floor(lightmap * 14.999) / 14.0;
		lightmap = clamp(lightmap, vec2(0.0), vec2(1.0));
		#endif

		albedo.rgb = pow(albedo.rgb, vec3(2.2));

		#ifdef EMISSIVE_RECOLOR
		float ec = GetLuminance(albedo.rgb) * 1.7;
		if (recolor > 0.5) {
			albedo.rgb = blocklightCol * pow(ec, 1.5) / (BLOCKLIGHT_I * BLOCKLIGHT_I);
			albedo.rgb /= 0.7 * albedo.rgb + 0.7;
		}
		if (lava > 0.5) {
			albedo.rgb = pow(blocklightCol * ec / BLOCKLIGHT_I, vec3(2.0));
			albedo.rgb /= 0.5 * albedo.rgb + 0.5;
		}
		#endif

		#ifdef MCBL_SS
		lightAlbedo = albedo.rgb + 0.00001;
		if (lava > 0.5) {
			lightAlbedo = pow(lightAlbedo, vec3(0.25));
		}
		lightAlbedo = sqrt(normalize(lightAlbedo) * emission);

		#ifdef MULTICOLORED_BLOCKLIGHT
		lightAlbedo *= GetMCBLLegacyMask(worldPos);
		#endif
		#endif

		#ifdef WHITE_WORLD
		albedo.rgb = vec3(0.35);
		#endif

		vec3 outNormal = newNormal;
		
		#ifdef NORMAL_PLANTS
		if (foliage > 0.5){
			newNormal = upVec;
		}
		#endif

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

		#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
		blocklightCol = ApplyMultiColoredBlocklight(blocklightCol, screenPos, worldPos, newNormal, 0.0);
		#endif

		float parallaxShadow = 1.0;
		vec3 shadow = vec3(0.0);

		GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, color.a, NoL,
			vanillaDiffuse, parallaxShadow, emission, subsurface, basicSubsurface);

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		#endif

		shadowMask = shadow.r * mix(NoL, 1.0, sqrt(basicSubsurface) * 0.7);
		shadowMask *= 1.0 - rawEmission;
		shadowMask *= lightmap.y * lightmap.y;

		#ifdef OVERWORLD
		shadowMask *= (1.0 - 0.95 * rainStrength) * shadowFade;
		#endif
	}

	gbufferData0 = albedo;
	gbufferData1 = vec4(shadowMask, 0.0, float(gl_FragCoord.z < 1.0), 1.0);

	#ifdef MCBL_SS
	gbufferData2 = vec4(lightAlbedo, 1.0);
	#endif
}

#endif