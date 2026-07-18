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

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

#ifdef ADVANCED_MATERIALS
varying float dist;

varying vec3 binormal, tangent;
varying vec3 viewVector;

varying vec4 vTexCoord, vTexCoordAM;
#endif

//Uniforms//
uniform int entityId;
uniform int frameCounter;
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float cloudHeight;
uniform float endFlashIntensity;
uniform float frameTimeCounter;
uniform float near, far;
uniform float nightVision;
uniform float rainStrength;
uniform float screenBrightness; 
uniform float shadowFade;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition;
uniform vec3 relativeEyePosition;

uniform vec4 entityColor;

uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelView, gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

uniform sampler2D texture;
uniform sampler2D noisetex;

#ifdef ADVANCED_MATERIALS
uniform ivec2 atlasSize;

uniform sampler2D specular;
uniform sampler2D normals;
#endif

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

#ifdef MCBL_SS
uniform mat4 gbufferPreviousModelView;
uniform mat4 gbufferPreviousProjection;
uniform vec3 previousCameraPosition;

uniform sampler2D colortex9;
#endif

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

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

//Includes//
#include "/lib/color/blocklightColor.glsl"
#include "/lib/color/dimensionColor.glsl"
#include "/lib/color/specularColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/lighting/dynamicHandlight.glsl"
#include "/lib/lighting/forwardLighting.glsl"
#include "/lib/surface/ggx.glsl"
#include "/lib/surface/hardcodedEmission.glsl"

#ifdef TAA
#include "/lib/util/jitter.glsl"
#endif

#ifdef ADVANCED_MATERIALS
#include "/lib/util/encode.glsl"
#include "/lib/reflections/complexFresnel.glsl"
#include "/lib/surface/materialGbuffers.glsl"
#include "/lib/surface/parallax.glsl"
#endif

#ifdef MULTICOLORED_BLOCKLIGHT
#include "/lib/util/voxelMapHelper.glsl"
#endif

#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
#include "/lib/lighting/coloredBlocklight.glsl"
#endif

#ifdef NORMAL_SKIP
#undef PARALLAX
#undef SELF_SHADOW
#endif

#if MC_VERSION <= 10710
#undef PARALLAX
#undef SELF_SHADOW
#endif

//Program//
void main() {
    vec4 albedo = texture2D(texture, texCoord) * color;
	vec3 vlAlbedo = vec3(1.0);
	vec3 newNormal = normal;
	float smoothness = 0.0;

	#ifdef ADVANCED_MATERIALS
	vec2 newCoord = vTexCoord.st * vTexCoordAM.pq + vTexCoordAM.st;
	float surfaceDepth = 1.0;
	float parallaxFade = clamp((dist - PARALLAX_DISTANCE) / 32.0, 0.0, 1.0);
	float skipParallax = float(entityId == 10100);
	
	#ifdef PARALLAX
	if (skipParallax < 0.5) {
		newCoord = GetParallaxCoord(texCoord, parallaxFade, surfaceDepth);
		albedo = texture2DGradARB(texture, newCoord, dcdx, dcdy) * color;
	}
	#endif

	float skyOcclusion = 0.0;
	vec3 fresnel3 = vec3(0.0);
	#endif

	#ifdef ENTITY_FLASH
	albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
	#endif

	// Exclude blob shadow and shulker from selective TAA
	float entityMask = (color.a < 0.999 || entityId == 10104) ? 0.0 : 0.25;
	
	float lightningBolt = float(entityId == 10101);
	if(lightningBolt > 0.5) {
		#ifdef OVERWORLD
		albedo.rgb = weatherCol.rgb / weatherCol.a;
		albedo.rgb *= albedo.rgb * albedo.rgb;
		#endif
		#ifdef NETHER
		albedo.rgb = sqrt(netherCol.rgb / netherCol.a);
		#endif
		#ifdef END
		albedo.rgb = endCol.rgb / endCol.a;
		#endif
		albedo.a = 1.0;
	}

	if (lightningBolt < 0.5) {
		vec2 lightmap = clamp(lmCoord, vec2(0.0), vec2(1.0));

		float endCrystal = float(entityId == 10102);
		float endBeam = float(entityId == 10103);
		
		float metalness      = 0.0;
		float emission       = float(entityColor.a > 0.05) * 0.125 + endCrystal;
		float subsurface     = 0.0;
		vec3 baseReflectance = vec3(0.04);
		
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);
		emission += endBeam;

		#ifndef ENTITY_FLASH
		emission = 0.0;
		#endif

		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#if defined TAA && !defined TAA_SELECTIVE
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = ToWorld(viewPos);

		#ifdef ADVANCED_MATERIALS
		float f0 = 0.0, porosity = 0.5, ao = 1.0;
		vec3 normalMap = vec3(0.0, 0.0, 1.0);
		
		GetMaterials(smoothness, metalness, f0, emission, subsurface, porosity, ao, normalMap,
					 newCoord, dcdx, dcdy);
					 
		#ifdef NORMAL_SKIP
		normalMap = vec3(0.0, 0.0, 1.0);
		#endif
		
		mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
							  tangent.y, binormal.y, normal.y,
							  tangent.z, binormal.z, normal.z);

		if ((normalMap.x > -0.999 || normalMap.y > -0.999) && viewVector == viewVector && skipParallax < 0.5)
			newNormal = clamp(normalize(normalMap * tbnMatrix), vec3(-1.0), vec3(1.0));
		#endif
		
		#if DYNAMIC_HANDLIGHT == 2
		lightmap = ApplyDynamicHandlight(lightmap, worldPos);
		#endif

		#ifdef TOON_LIGHTMAP
		lightmap = floor(lightmap * 14.999 * (0.75 + 0.25 * color.a)) / 14.0;
		lightmap = clamp(lightmap, vec2(0.0), vec2(1.0));
		#endif
		
    	albedo.rgb = pow(albedo.rgb, vec3(2.2));
		
		vlAlbedo = mix(vec3(1.0), albedo.rgb, sqrt(albedo.a)) * (1.0 - pow(albedo.a, 64.0));

		#ifdef END
		if (endBeam > 0.5) {
			albedo.rgb *= endCol.rgb;
		}
		#endif

		#ifdef WHITE_WORLD
		albedo.rgb = vec3(0.35);
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

		float parallaxShadow = 1.0;
		#ifdef ADVANCED_MATERIALS
		vec3 rawAlbedo = albedo.rgb * 0.999 + 0.001;
		albedo.rgb *= ao;

		#ifdef REFLECTION_SPECULAR
		albedo.rgb *= 1.0 - metalness * smoothness;
		#endif

		float doParallax = 0.0;
		#ifdef SELF_SHADOW
		#ifdef OVERWORLD
		doParallax = float(lightmap.y > 0.0 && NoL > 0.0);
		#endif
		#ifdef END
		doParallax = float(NoL > 0.0);
		#endif
		
		if (doParallax > 0.5) {
			parallaxShadow = GetParallaxShadow(surfaceDepth, parallaxFade, newCoord, lightVec,
											   tbnMatrix);
		}
		#endif
		#endif

		#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
		blocklightCol = ApplyMultiColoredBlocklight(blocklightCol, screenPos, worldPos, newNormal, 0.0);
		#endif

		vec3 shadow = vec3(0.0);
		GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, 1.0, NoL, 
					vanillaDiffuse, parallaxShadow, emission, subsurface, 0.0);

		#ifdef ADVANCED_MATERIALS
		skyOcclusion = lightmap.y;

		baseReflectance = mix(vec3(f0), rawAlbedo, metalness);
		float fresnel = pow(clamp(1.0 + dot(newNormal, normalize(viewPos.xyz)), 0.0, 1.0), 5.0);

		fresnel3 = mix(baseReflectance, vec3(1.0), fresnel);
		#if MATERIAL_FORMAT == 1
		if (f0 >= 0.9 && f0 < 1.0) {
			baseReflectance = GetMetalCol(f0);
			fresnel3 = ComplexFresnel(pow(fresnel, 0.2), f0);
			#ifdef ALBEDO_METAL
			fresnel3 *= rawAlbedo;
			#endif
		}
		#endif
		
		float aoSquared = ao * ao;
		shadow *= aoSquared; fresnel3 *= aoSquared;
		albedo.rgb = albedo.rgb * (1.0 - fresnel3 * smoothness * smoothness * (1.0 - metalness));
		#endif

		#if (defined OVERWORLD || defined END) && defined ADVANCED_MATERIALS && SPECULAR_HIGHLIGHT > 0
		vec3 specularColor = GetSpecularColor(lightmap.y, metalness, baseReflectance);
		
		albedo.rgb += GetSpecularHighlight(newNormal, viewPos, smoothness, baseReflectance,
										   specularColor, shadow * vanillaDiffuse, 1.0);
		#endif

		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR && defined REFLECTION_ROUGH
		normalMap = mix(vec3(0.0, 0.0, 1.0), normalMap, smoothness);
		newNormal = clamp(normalize(normalMap * tbnMatrix), vec3(-1.0), vec3(1.0));
		#endif

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		#endif
	}

    /* DRAWBUFFERS:01 */
    gl_FragData[0] = albedo;
    gl_FragData[1] = vec4(vlAlbedo, 1.0);

	#ifdef MCBL_SS
		/* DRAWBUFFERS:018 */
		gl_FragData[2] = vec4(0.0,0.0,0.0,1.0);
		
		#if defined TAA_SELECTIVE && !(defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR)
		/* DRAWBUFFERS:0183 */
		gl_FragData[3] = vec4(0.0, 0.0, entityMask, 1.0);
		#endif

		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR
		/* DRAWBUFFERS:018367 */
		gl_FragData[3] = vec4(smoothness, skyOcclusion, entityMask, 1.0);
		gl_FragData[4] = vec4(EncodeNormal(newNormal), float(gl_FragCoord.z < 1.0), 1.0);
		gl_FragData[5] = vec4(fresnel3, 1.0);
		#endif
	#else
		#if defined TAA_SELECTIVE && !(defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR)
		/* DRAWBUFFERS:013 */
		gl_FragData[2] = vec4(0.0, 0.0, entityMask, 1.0);
		#endif

		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR
		/* DRAWBUFFERS:01367 */
		gl_FragData[2] = vec4(smoothness, skyOcclusion, entityMask, 1.0);
		gl_FragData[3] = vec4(EncodeNormal(newNormal), float(gl_FragCoord.z < 1.0), 1.0);
		gl_FragData[4] = vec4(fresnel3, 1.0);
		#endif
	#endif
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord, lmCoord;

varying vec3 normal;
varying vec3 sunVec, upVec, eastVec;

varying vec4 color;

#ifdef ADVANCED_MATERIALS
varying float dist;

varying vec3 binormal, tangent;
varying vec3 viewVector;

varying vec4 vTexCoord, vTexCoordAM;
#endif

//Uniforms//
uniform int entityId;
uniform int worldTime;

uniform float frameTimeCounter;
uniform float timeAngle;

uniform vec3 cameraPosition;

uniform mat4 gbufferModelView, gbufferModelViewInverse;

#ifdef TAA
uniform int frameCounter;

uniform float viewWidth, viewHeight;
#endif

//Attributes//
attribute vec4 mc_Entity;

#ifdef ADVANCED_MATERIALS
attribute vec4 mc_midTexCoord;
attribute vec4 at_tangent;
#endif

//Common Variables//
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

//Includes//
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

	normal = normalize(gl_NormalMatrix * gl_Normal);

	#ifdef ADVANCED_MATERIALS
	tangent  = normalize(gl_NormalMatrix * at_tangent.xyz);
	binormal = normalize(gl_NormalMatrix * cross(at_tangent.xyz, gl_Normal.xyz) * at_tangent.w);
	
	mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
						  tangent.y, binormal.y, normal.y,
						  tangent.z, binormal.z, normal.z);
								  
	viewVector = tbnMatrix * (gl_ModelViewMatrix * gl_Vertex).xyz;
	
	dist = length(gl_ModelViewMatrix * gl_Vertex);

	vec2 midCoord = (gl_TextureMatrix[0] *  mc_midTexCoord).st;
	vec2 texMinMidCoord = texCoord - midCoord;

	vTexCoordAM.pq  = abs(texMinMidCoord) * 2;
	vTexCoordAM.st  = min(texCoord, midCoord - texMinMidCoord);

	vTexCoord.xy    = sign(texMinMidCoord) * 0.5 + 0.5;
	#endif
    
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
	eastVec = normalize(gbufferModelView[0].xyz);

    #ifdef WORLD_CURVATURE
	vec4 position = gbufferModelViewInverse * gl_ModelViewMatrix * gl_Vertex;
	position.y -= WorldCurvature(position.xz);
	gl_Position = gl_ProjectionMatrix * gbufferModelView * position;
	#else
	gl_Position = ftransform();
    #endif
	
	#if defined TAA 
	#if defined TAA_SELECTIVE
	if (color.a < 0.999 || entityId == 10104) {
		gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	}
	#else
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
	#endif
}

#endif