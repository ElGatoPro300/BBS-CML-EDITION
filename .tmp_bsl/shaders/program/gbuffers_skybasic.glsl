/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying float alpha;

varying vec3 upVec, sunVec;

//Uniforms//
uniform int bedrockLevel;
uniform int frameCounter;
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float blindFactor, darknessFactor;
uniform float cloudHeight;
uniform float far, near;
uniform float frameTimeCounter;
uniform float nightVision;
uniform float rainStrength;
uniform float shadowFade;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjectionInverse;

uniform sampler2D noisetex;

#ifdef VOXY
uniform int vxRenderDistance;
#endif

#ifdef DISTANT_HORIZONS
uniform int dhRenderDistance;
#endif

//Common Variables//
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);
float moonVisibility = clamp(dot(-sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

vec3 lightVec = sunVec * (1.0 - 2.0 * float(timeAngle > 0.5325 && timeAngle < 0.9675));

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

//Includes//
#include "/lib/color/dimensionColor.glsl"
#include "/lib/color/lightSkyColor.glsl"
#include "/lib/color/skyColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/atmospherics/clouds.glsl"
#include "/lib/atmospherics/sky.glsl"
#include "/lib/atmospherics/sunmoon.glsl"

//Program//
void main() {
	vec3 albedo = vec3(0.0);
	
	#if defined OVERWORLD && !defined SKY_DEFERRED
	vec4 screenPos = vec4(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z, 1.0);
	vec4 viewPos = gbufferProjectionInverse * (screenPos * 2.0 - 1.0);
	viewPos /= viewPos.w;
	
	albedo = GetSkyColor(viewPos.xyz, false);
	
	#ifdef SHADER_SUN_MOON
	vec3 lightMA = mix(lightMorning, lightEvening, mefade);
    vec3 sunColor = mix(lightMA, sqrt(lightDay * lightMA * LIGHT_DI), timeBrightness);
    vec3 moonColor = lightNight * 1.25;

	ShaderSunMoon(albedo, viewPos.xyz, sunColor, moonColor);
	#endif

	#ifdef STARS
	if (moonVisibility > 0.0) DrawStars(albedo.rgb, viewPos.xyz);
	#endif

	float dither = Bayer8(gl_FragCoord.xy);

	#if AURORA > 0
	albedo.rgb += DrawAurora(viewPos.xyz, dither, 24);
	#endif

	SunGlare(albedo, viewPos.xyz, lightCol);

	albedo.rgb *= 1.0 + nightVision;
	#ifdef CLASSIC_EXPOSURE
	albedo.rgb *= 4.0 - 3.0 * eBS;
	#endif

	#if ALPHA_BLEND == 0
	albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
	albedo.rgb = albedo.rgb + dither / vec3(128.0);
	#endif
	#endif
	
    /* DRAWBUFFERS:0 */
	gl_FragData[0] = vec4(albedo, alpha);
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying float alpha;

varying vec3 sunVec, upVec;

//Uniforms//
uniform int renderStage;

uniform float timeAngle;

uniform mat4 gbufferModelView;

#ifndef MC_RENDER_STAGE_MOON
#define MC_RENDER_STAGE_MOON 1
#endif

//Program//
void main() {
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

	alpha = 1.0;
	
	#if MC_VERSION >= 11605
	if (renderStage == MC_RENDER_STAGE_STARS) {
		alpha = 0.0;
	}
	#else
	if (gl_Color.r == gl_Color.g && gl_Color.g == gl_Color.b && gl_Color.r > 0.0) {
		alpha = 0.0;
	}
	#endif

	// remove vanilla sunset gradient
	if (renderStage == MC_RENDER_STAGE_MOON) {
		alpha = 0.0;
	}
}

#endif