/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying float mat;

varying vec2 texCoord, lmCoord;

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

//Uniforms//
uniform int bedrockLevel;
uniform int frameCounter;
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float blindFactor, darknessFactor, nightVision;
uniform float cloudHeight;
uniform float endFlashIntensity;
uniform float far;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform float screenBrightness;
uniform float shadowFade;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition;

uniform mat4 dhProjectionInverse;
uniform mat4 gbufferModelView, gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

uniform sampler2D noisetex;

//Common Variables//
float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);
float moonVisibility = clamp(dot(-sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

#ifdef ADVANCED_MATERIALS
vec2 dcdx = dFdx(texCoord);
vec2 dcdy = dFdy(texCoord);
#endif

vec3 lightVec = sunVec * ((timeAngle < 0.5325 || timeAngle > 0.9675) ? 1.0 : -1.0);

mat4 gbufferProjectionInverse = dhProjectionInverse;

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

float GetBlueNoise3D(vec3 pos, vec3 normal) {
	pos = (floor(pos + 0.01) + 0.5) / 512.0;

	vec3 worldNormal = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
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

//Includes//
#include "/lib/color/blocklightColor.glsl"
#include "/lib/color/dimensionColor.glsl"
#include "/lib/color/lightSkyColor.glsl"
#include "/lib/color/skyColor.glsl"
#include "/lib/color/specularColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/atmospherics/sky.glsl"
#include "/lib/lighting/forwardLighting.glsl"
#include "/lib/surface/ggx.glsl"
#include "/lib/surface/hardcodedEmission.glsl"

#ifdef TAA
#include "/lib/util/jitter.glsl"
#endif

//Program//
void main() {
    vec4 albedo = color;
	vec3 newNormal = normal;
	float shadowMask = 0.0;

	{
		vec2 lightmap = clamp(lmCoord, vec2(0.0), vec2(1.0));
		
		float foliage  = float(mat > 0.98 && mat < 1.02);
		float leaves   = float(mat > 1.98 && mat < 2.02);
		float emissive = float(mat > 2.98 && mat < 3.02);
		float lava     = float(mat > 3.98 && mat < 4.02);
		float cloud   = float(mat > 4.98 && mat < 5.02);

		#if CLOUDS != 3
		if (cloud > 0.5) {
			discard;
		}
		#endif

		float metalness       = 0.0;
		float emission        = emissive + lava;
		float subsurface      = 0.0;
		float basicSubsurface = leaves * 0.5;
		vec3 baseReflectance  = vec3(0.04);

		float rawEmission = emission;
		
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);
		
		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = ToWorld(viewPos);

		float dither = Bayer8(gl_FragCoord.xy);

		float viewLength = length(viewPos);
		float minDist = (dither - DH_OVERDRAW - 0.75) * 16.0 + far;
		if (viewLength <= minDist) {
			discard;
		}

		vec3 noisePos = (worldPos + cameraPosition) * 4.0;
		float albedoLuma = GetLuminance(albedo.rgb);
		float noiseAmount = (1.0 - albedoLuma * albedoLuma) * 0.05;
		float albedoNoise = GetBlueNoise3D(noisePos, normal);
		albedo.rgb = clamp(albedo.rgb + albedoNoise * noiseAmount, vec3(0.0), vec3(1.0));
		// albedo.rgb = vec3(albedoNoise + 0.5);

		#ifdef TOON_LIGHTMAP
		lightmap = floor(lmCoord * 14.999) / 14.0;
		lightmap = clamp(lightmap, vec2(0.0), vec2(1.0));
		#endif

    	albedo.rgb = pow(albedo.rgb, vec3(2.2));

		#ifdef WHITE_WORLD
		albedo.rgb = vec3(0.35);
		#endif
		
		vec3 outNormal = newNormal;
		
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
		
		#ifndef NORMAL_PLANTS
		if (foliage > 0.5) vanillaDiffuse *= 1.8;
		#endif
		if (leaves > 0.5) {
			float halfNoL = dot(newNormal, lightVec) * 0.5 + 0.5;
			basicSubsurface *= halfNoL * step(length(albedo.rgb), 1.7);
		}
		
		vec3 shadow = vec3(0.0);
		if (cloud < 0.5) {
			GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, 1.0, NoL, 
						vanillaDiffuse, 1.0, emission, subsurface, basicSubsurface);
		} else {
			#ifdef OVERWORLD
			albedo.rgb *= lightCol * vanillaDiffuse * CLOUD_BRIGHTNESS;
			albedo.rgb *= mix(0.4 - 0.25 * rainStrength, 0.5 - 0.425 * rainStrength, sunVisibility);

			vec3 sky = GetSkyColor(viewPos, false);
			float cloudAlpha = 0.125;
			
			#if FOG_VANILLA_CLOUD > 0
			#if FOG_VANILLA_CLOUD == 1
			float vanillaFogEnd = 4.0;
			#elif FOG_VANILLA_CLOUD == 2
			float vanillaFogEnd = 2.0;
			#elif FOG_VANILLA_CLOUD == 3
			float vanillaFogEnd = 1.0;
			#else
			#if MC_VERSION > 12106
			float vanillaFogEnd = 4.0;
			#else
			float vanillaFogEnd = 1.0;
			#endif
			#endif

			float worldDistance = length(worldPos.xz) / 4096.0;
			float vanillaFog = 1.0 - smoothstep(0.5, vanillaFogEnd, worldDistance);
			cloudAlpha *= vanillaFog;
			#endif

			albedo.rgb = mix(sky, albedo.rgb, cloudAlpha);
			#endif
		}

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

	/* DRAWBUFFERS:06 */
    gl_FragData[0] = albedo;
	gl_FragData[1] = vec4(shadowMask, 0.0, float(gl_FragCoord.z < 1.0), 1.0);
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying float mat;

varying vec2 texCoord, lmCoord;

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

//Uniforms//
uniform int heightLimit;
uniform int worldTime;

uniform float frameTimeCounter;
uniform float timeAngle;

uniform vec3 cameraPosition;
uniform vec3 relativeEyePosition;

uniform mat4 dhProjection;
uniform mat4 gbufferModelView, gbufferModelViewInverse;

#ifdef TAA
uniform int frameCounter;

uniform float viewWidth, viewHeight;
#endif

//Attributes//
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

//Common Variables//
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

//Includes//
#include "/lib/vertex/waving.glsl"

#ifdef TAA
#include "/lib/util/jitter.glsl"
#endif

#ifdef WORLD_CURVATURE
#include "/lib/vertex/worldCurvature.glsl"
#endif

//Program//
void main() {
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    
	lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
	lmCoord = clamp((lmCoord - 0.03125) * 1.06667, vec2(0.0), vec2(0.9333, 1.0));

	int blockID = dhMaterialId;

	normal = normalize(gl_NormalMatrix * gl_Normal);
    
	color = gl_Color;
	
	mat = 0.0;

	if (blockID == DH_BLOCK_LEAVES){
		mat = 2.0;
		color.rgb *= 1.225 * 1.1;
	}
	if (blockID == DH_BLOCK_ILLUMINATED)
		mat = 3.0;
	if (blockID == DH_BLOCK_LAVA) {
		mat = 4.0;
		lmCoord.x += 0.0667;
	}

	const vec2 sunRotationData = vec2(cos(sunPathRotation * 0.01745329251994), -sin(sunPathRotation * 0.01745329251994));
	#ifndef END
	float ang = fract(timeAngle - 0.25);
	#else
	#if defined IS_IRIS || MC_VERSION < 12111
	float ang = 0.0;
	#else
	float ang = 0.5;
	#endif
	#endif
	ang = (ang + (cos(ang * 3.14159265358979) * -0.5 + 0.5 - ang) / 3.0) * 6.28318530717959;
	sunVec = normalize((gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);

	upVec = normalize(gbufferModelView[1].xyz);
	eastVec = normalize(gbufferModelView[0].xyz);

	vec4 position = gbufferModelViewInverse * gl_ModelViewMatrix * gl_Vertex;

	float worldY = position.y + cameraPosition.y;

	if (worldY > (heightLimit + 192) && worldY < (heightLimit + 240)) {
		mat = 5.0;
	}

    #ifdef WORLD_CURVATURE
	if (mat != 5.0) {
		position.y -= WorldCurvature(position.xz);
	}
    #endif

	gl_Position = dhProjection * gbufferModelView * position;
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif