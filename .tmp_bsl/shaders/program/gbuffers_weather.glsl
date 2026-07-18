/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying vec2 texCoord, lmCoord;

varying vec3 upVec, sunVec;

//Uniforms//
uniform int frameCounter;
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float nightVision;
uniform float rainStrength;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition;
uniform vec3 relativeEyePosition;

uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

uniform sampler2D texture;
uniform sampler2D depthtex0;

#if DYNAMIC_HANDLIGHT > 0
uniform int heldBlockLightValue, heldBlockLightValue2;
#endif

#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
uniform int heldItemId, heldItemId2;
#endif

#ifdef MULTICOLORED_BLOCKLIGHT
uniform sampler3D lighttex0;
uniform sampler3D lighttex1;
#endif

//Common Variables//
float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

//Common Functions//
void Defog(inout vec3 albedo, float fogDensity) {
	float z = texture2D(depthtex0,gl_FragCoord.xy/vec2(viewWidth,viewHeight)).r;
	if (z == 1.0) return;

    vec4 screenPos = vec4(gl_FragCoord.xy / vec2(viewWidth, viewHeight), z, 1.0);
    vec4 viewPos = gbufferProjectionInverse * (screenPos * 2.0 - 1.0);
    viewPos /= viewPos.w;

    float fog = length(viewPos) * fogDensity / 256.0;
	float clearDay = sunVisibility * (1.0 - rainStrength);
	fog *= (0.5 * rainStrength + 1.0) / (3.0 * clearDay + 1.0);
	fog = 1.0 - exp(-2.0 * pow(fog, 0.25 * clearDay + 1.25) * eBS);
    albedo.rgb /= 1.0 - fog;
}

//Includes//
#include "/lib/color/lightColor.glsl"
#include "/lib/color/blocklightColor.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/lighting/dynamicHandlight.glsl"

#ifdef MULTICOLORED_BLOCKLIGHT
#undef MCBL_SS

#include "/lib/util/voxelMapHelper.glsl"
#include "/lib/lighting/coloredBlocklight.glsl"
#endif

//Program//
void main() {
	#if defined NETHER || defined END
	discard;
	#endif

    vec4 albedo = texture2D(texture, texCoord);
		
	if (albedo.a > 0.001) {
		vec2 lightmap = lmCoord;
		
		#if DYNAMIC_HANDLIGHT == 2 || defined MULTICOLORED_BLOCKLIGHT
		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		vec3 viewPos = ToNDC(screenPos);
		vec3 worldPos = ToWorld(viewPos);
		#endif
		
		#if DYNAMIC_HANDLIGHT == 2
		lightmap = ApplyDynamicHandlight(lightmap, worldPos);
		#endif
		
		#ifdef MULTICOLORED_BLOCKLIGHT
		blocklightCol = ApplyMultiColoredBlocklight(blocklightCol, screenPos, worldPos, vec3(0.0), 0.0);
		#endif

		albedo.a *= 0.35 * rainStrength * length(albedo.rgb / 3.0) * float(albedo.a > 0.1);
		albedo.rgb = sqrt(albedo.rgb);
		albedo.rgb *= (ambientCol + lightmap.x * lightmap.x * blocklightCol) * WEATHER_OPACITY;

		#if MC_VERSION < 10800
		albedo.a *= 4.0;
		albedo.rgb *= 0.525;
		#endif
		
		#if defined FOG && MC_VERSION < 11500
		if (gl_FragCoord.z > 0.991) Defog(albedo.rgb, fogDensity);
		#endif

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		albedo.a *= 1.4;
		#endif
	}
	
/* DRAWBUFFERS:0 */
	gl_FragData[0] = albedo;
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord, lmCoord;

varying vec3 sunVec, upVec;

//Uniforms//
uniform float timeAngle;

uniform mat4 gbufferModelView;

//Program//
void main() {
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;

	lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
	lmCoord = clamp(lmCoord * 2.0 - 1.0, 0.0, 1.0);

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
	
	gl_Position = ftransform();
}

#endif