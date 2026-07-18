/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying vec2 texCoord;

varying vec3 upVec, sunVec;

varying vec4 color;

//Uniforms//
uniform int moonPhase;
uniform int renderStage;

uniform float frameTimeCounter;
uniform float nightVision;
uniform float rainStrength;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition;

uniform mat4 gbufferProjectionInverse;

uniform sampler2D texture;
uniform sampler2D gaux1;

#ifndef MC_RENDER_STAGE_SUN
#define MC_RENDER_STAGE_SUN 1
#endif

#ifndef MC_RENDER_STAGE_MOON
#define MC_RENDER_STAGE_MOON 1
#endif

#ifndef MC_RENDER_STAGE_CUSTOM_SKY
#define MC_RENDER_STAGE_CUSTOM_SKY 1
#endif

//Common Variables//
float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);
float moonVisibility = clamp(dot(-sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

//Includes//
#include "/lib/color/dimensionColor.glsl"
#include "/lib/util/dither.glsl"

//Program//
void main() {
	vec4 albedo = texture2D(texture, texCoord) * color;

	#ifdef OVERWORLD
	albedo.rgb = pow(albedo.rgb, vec3(2.2)) * albedo.a;

	#if MC_VERSION >= 11605
	vec4 screenPos = vec4(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z, 1.0);
	vec4 viewPos = gbufferProjectionInverse * (screenPos * 2.0 - 1.0);
	viewPos /= viewPos.w;
	
	float VoU = dot(normalize(viewPos.xyz), upVec);

	#ifdef SUN_MOON_GROUND
	float sunFade = smoothstep(0.0, 1.0, 1.0 - pow(1.0 - max(VoU * 0.975 + 0.025, 0.0), 8.0));
	sunFade *= sunFade;
	#else
	float sunFade = 1.0;
	#endif

	if (renderStage == MC_RENDER_STAGE_CUSTOM_SKY) {
		albedo.rgb *= SKYBOX_INTENSITY * SKYBOX_INTENSITY;
		albedo.a *= SKYBOX_OPACITY;
	}
	if (renderStage == MC_RENDER_STAGE_SUN) {
		albedo.rgb *= SUN_INTENSITY * SUN_INTENSITY * sunFade;
	}
	if (renderStage == MC_RENDER_STAGE_MOON) {
		albedo.rgb *= MOON_INTENSITY * MOON_INTENSITY * sunFade;
	}
	#else 
	albedo.rgb *= SKYBOX_INTENSITY * SKYBOX_INTENSITY;
	albedo.a *= SKYBOX_OPACITY;
	#endif
	
	#ifdef SHADER_SUN_MOON
	if (renderStage == MC_RENDER_STAGE_SUN || renderStage == MC_RENDER_STAGE_MOON) {
		albedo *= 0.0;
	}
	#endif
	
	#ifdef SKY_DESATURATION
	if (renderStage != MC_RENDER_STAGE_SUN) {
		vec3 desat = GetLuminance(albedo.rgb) * pow(lightNight, vec3(1.6)) * 4.0;
		albedo.rgb = mix(desat, albedo.rgb, sunVisibility);
	}
	#endif

	#ifdef SKY_UNDERGROUND
	albedo.rgb *= mix(clamp((cameraPosition.y - 48.0) / 16.0, 0.0, 1.0), 1.0, eBS);
	#endif
	#endif

	#ifdef END
	albedo.rgb = pow(albedo.rgb, vec3(2.2));

	#ifdef SKY_DESATURATION
	albedo.rgb = GetLuminance(albedo.rgb) * endCol.rgb;
	#endif

	albedo.rgb *= SKYBOX_INTENSITY * 0.02 * 16.0 / 16.0;
	#endif

	#if ALPHA_BLEND == 0
	albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
	#endif
	
    /* DRAWBUFFERS:0 */
	gl_FragData[0] = albedo;
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord;

varying vec3 sunVec, upVec;

varying vec4 color;

//Uniforms//
uniform float timeAngle;

uniform mat4 gbufferModelView;

#ifdef TAA
uniform int frameCounter;

uniform float viewWidth;
uniform float viewHeight;
#include "/lib/util/jitter.glsl"
#endif

//Program//
void main() {
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;

	color = gl_Color;
	
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
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif