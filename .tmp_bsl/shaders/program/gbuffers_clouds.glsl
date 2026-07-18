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

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

//Uniforms//
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float rainStrength;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform sampler2D texture;

uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

#ifdef VOXY
uniform mat4 vxProjInv;

uniform sampler2D vxDepthTexOpaque;
#endif

#ifdef DISTANT_HORIZONS
uniform mat4 dhProjectionInverse;

uniform sampler2D dhDepthTex0;
#endif

//Common Variables//
float eBS = eyeBrightnessSmooth.y / 240.0;
float sunVisibility  = clamp(dot( sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);
float moonVisibility = clamp(dot(-sunVec, upVec) * 10.0 + 0.5, 0.0, 1.0);

//Includes//
#include "/lib/color/lightColor.glsl"
#include "/lib/util/spaceConversion.glsl"

#ifdef TAA
#include "/lib/util/jitter.glsl"
#endif

//Program//
void main() {
	#if CLOUDS != 3
	discard;
	#endif
	
	vec4 albedo = texture2D(texture, texCoord);
	albedo.rgb = pow(albedo.rgb,vec3(2.2));
	
	float NoU = clamp(dot(normal, upVec), -1.0, 1.0);
	float NoE = clamp(dot(normal, eastVec), -1.0, 1.0);
	float vanillaDiffuse = (0.25 * NoU + 0.75) + (-0.333 + abs(NoE)) * (1.0 - abs(NoU)) * 0.15;
		  vanillaDiffuse = mix(vanillaDiffuse, 0.75, rainStrength * 0.75);
		  vanillaDiffuse*= vanillaDiffuse;

	albedo.rgb *= lightCol * vanillaDiffuse * CLOUD_BRIGHTNESS;
	albedo.rgb *= mix(0.4 - 0.25 * rainStrength, 0.5 - 0.425 * rainStrength, sunVisibility);
	
	vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
	#ifdef TAA
	vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
	#else
	vec3 viewPos = ToNDC(screenPos);
	#endif
	vec3 worldPos = ToWorld(viewPos);

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

	float worldDistance = length(worldPos.xz) / 256.0;
	float vanillaFog = 1.0 - smoothstep(0.5, vanillaFogEnd, worldDistance);

	albedo.a *= color.a * vanillaFog;
	#endif

	#ifdef VOXY
	float vxZ = texture2D(vxDepthTexOpaque, gl_FragCoord.xy / vec2(viewWidth, viewHeight)).r;

	vec4 vxScreenPos = vec4(screenPos.xy, vxZ, 1.0);
	vec4 vxViewPos = vxProjInv * (vxScreenPos * 2.0 - 1.0);
	vxViewPos /= vxViewPos.w;
	if (length(vxViewPos) < length(viewPos)) {
		discard;
	}
	#endif

	#ifdef DISTANT_HORIZONS
	float dhZ = texture2D(dhDepthTex0, gl_FragCoord.xy / vec2(viewWidth, viewHeight)).r;

	vec4 dhScreenPos = vec4(screenPos.xy, dhZ, 1.0);
	vec4 dhViewPos = dhProjectionInverse * (dhScreenPos * 2.0 - 1.0);
	dhViewPos /= dhViewPos.w;
	if (length(dhViewPos) < length(viewPos)) {
		discard;
	}
	#endif

	#if ALPHA_BLEND == 0
	albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
	#endif
	
    /* DRAWBUFFERS:0 */
	gl_FragData[0] = albedo;

	#ifdef ADVANCED_MATERIALS
	/* DRAWBUFFERS:0367 */
	gl_FragData[1] = vec4(0.0, 0.0, 0.0, 1.0);
	gl_FragData[2] = vec4(0.0, 0.0, 0.0, 1.0);
	gl_FragData[3] = vec4(0.0, 0.0, 0.0, 1.0);
	#endif
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord;

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

//Uniforms//
#ifdef TAA
uniform int frameCounter;

uniform float viewWidth;
uniform float viewHeight;
#include "/lib/util/jitter.glsl"
#endif

uniform float timeAngle;

uniform mat4 gbufferModelView;

//Program//
void main() {
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;

	color = gl_Color;

	normal = normalize(gl_NormalMatrix * gl_Normal);
	
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
	
	gl_Position = ftransform();
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif