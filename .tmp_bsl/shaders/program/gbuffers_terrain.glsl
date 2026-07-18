/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying float mat, recolor;

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

#ifdef IRIS_FEATURE_FADE_VARIABLE
varying float chunkFade;
#endif

//Uniforms//
uniform int bedrockLevel;
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

#ifdef REFLECTION_RAIN
uniform float wetness;
#endif
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
#include "/lib/color/lightSkyColor.glsl"
#include "/lib/color/skyColor.glsl"
#include "/lib/color/specularColor.glsl"
#include "/lib/util/dither.glsl"
#include "/lib/util/spaceConversion.glsl"
#include "/lib/atmospherics/weatherDensity.glsl"
#include "/lib/atmospherics/sky.glsl"
#include "/lib/atmospherics/chunkFade.glsl"
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
#include "/lib/surface/directionalLightmap.glsl"
#include "/lib/surface/materialGbuffers.glsl"
#include "/lib/surface/parallax.glsl"

#ifdef REFLECTION_RAIN
#include "/lib/reflections/rainPuddles.glsl"
#endif
#endif

#ifdef MULTICOLORED_BLOCKLIGHT
#include "/lib/util/voxelMapHelper.glsl"
#endif

#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
#include "/lib/lighting/coloredBlocklight.glsl"
#endif

//Program//
void main() {
    vec4 albedo = texture2D(texture, texCoord) * vec4(color.rgb, 1.0);
	vec3 newNormal = normal;
	float smoothness = 0.0;
	vec3 lightAlbedo = vec3(0.0);

	#ifdef ADVANCED_MATERIALS
	vec2 newCoord = vTexCoord.st * vTexCoordAM.pq + vTexCoordAM.st;
	float surfaceDepth = 1.0;
	float parallaxFade = clamp((dist - PARALLAX_DISTANCE) / 32.0, 0.0, 1.0);
	float skipParallax = float(mat > 3.98 && mat < 4.02);
		  skipParallax+= float(mat > 7.98 && mat < 8.02);
	
	#ifdef PARALLAX
	if(skipParallax < 0.5) {
		newCoord = GetParallaxCoord(texCoord, parallaxFade, surfaceDepth);
		albedo = texture2DGradARB(texture, newCoord, dcdx, dcdy) * vec4(color.rgb, 1.0);
	}
	#endif

	float skyOcclusion = 0.0;
	vec3 fresnel3 = vec3(0.0);
	#endif

	{
		vec2 lightmap = clamp(lmCoord, vec2(0.0), vec2(1.0));
		
		float foliage   = float(mat > 0.98 && mat < 1.02);
		float leaves    = float(mat > 1.98 && mat < 2.02);
		float emissive  = float(mat > 2.98 && mat < 3.02);
		float lava      = float(mat > 3.98 && mat < 4.02);
		float candle    = float(mat > 4.98 && mat < 5.02);
		float ore       = float(mat > 5.98 && mat < 6.02);
		float netherOre = float(mat > 6.98 && mat < 7.02);

		float metalness       = 0.0;
		float emission        = (emissive + candle + lava);
		float subsurface      = 0.0;
		float basicSubsurface = (foliage + candle) * 0.5 + leaves;
		vec3 baseReflectance  = vec3(0.04);
		
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);
		
		#ifdef GLOWING_ORES
		float oreEmission = GetOreEmission(hsv, ore, netherOre);
		#endif

		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = ToWorld(viewPos);
		
		#if defined DISTANT_HORIZONS && !defined VOXY
		float dither = Bayer8(gl_FragCoord.xy);

		float viewLength = length(viewPos);
		float minDist = (dither - 0.75) * 16.0 + far;
		if (viewLength > minDist) {
			discard;
		}
		#endif

		#ifdef ADVANCED_MATERIALS
		float f0 = 0.0, porosity = 0.5, ao = 1.0;
		vec3 normalMap = vec3(0.0, 0.0, 1.0);
		GetMaterials(smoothness, metalness, f0, emission, subsurface, porosity, ao, normalMap,
					 newCoord, dcdx, dcdy);
					 
		mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
							  tangent.y, binormal.y, normal.y,
							  tangent.z, binormal.z, normal.z);

		if ((normalMap.x > -0.999 || normalMap.y > -0.999) && viewVector == viewVector)
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
			
			#ifdef ADVANCED_MATERIALS
			newNormal = normalize(mix(outNormal, newNormal, normalMap.z * normalMap.z));
			#endif
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
		
		#ifndef NORMAL_PLANTS
		if (foliage > 0.5) vanillaDiffuse *= 1.8;
		#endif

		float parallaxShadow = 1.0;
		#ifdef ADVANCED_MATERIALS
		vec3 rawAlbedo = albedo.rgb * 0.999 + 0.001;
		albedo.rgb *= ao * ao;

		#ifdef REFLECTION_SPECULAR
		albedo.rgb *= 1.0 - metalness * smoothness;
		#endif

		float doParallax = 0.0;
		#ifdef SELF_SHADOW
		float parallaxNoL = dot(outNormal, lightVec);
		#ifdef OVERWORLD
		doParallax = float(lightmap.y > 0.0 && parallaxNoL > 0.0);
		#endif
		#ifdef END
		doParallax = float(parallaxNoL > 0.0);
		#endif
		
		if (doParallax > 0.5 && skipParallax < 0.5) {
			parallaxShadow = GetParallaxShadow(surfaceDepth, parallaxFade, newCoord, lightVec,
											   tbnMatrix);
		}
		#endif

		#ifdef DIRECTIONAL_LIGHTMAP
		mat3 lightmapTBN = GetLightmapTBN(viewPos);
		lightmap.x = DirectionalLightmap(lightmap.x, lmCoord.x, outNormal, lightmapTBN);
		lightmap.y = DirectionalLightmap(lightmap.y, lmCoord.y, outNormal, lightmapTBN);
		#endif
		#endif

		#if defined MULTICOLORED_BLOCKLIGHT || defined MCBL_SS
		blocklightCol = ApplyMultiColoredBlocklight(blocklightCol, screenPos, worldPos, newNormal, 0.0);
		#endif

		#ifdef GLOWING_ORES
		if (ore + netherOre > 0.5) {
			oreEmission *= pow(lightmap.x, 4.0 / max(EMISSIVE_CURVE, 1.0)) * vanillaDiffuse;
			emission += oreEmission;
		}
		#endif
		
		vec3 shadow = vec3(0.0);
		GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, color.a, NoL, 
					vanillaDiffuse, parallaxShadow, emission, subsurface, basicSubsurface);

		#ifdef ADVANCED_MATERIALS
		float puddles = 0.0;
		#ifdef REFLECTION_RAIN
		float puddlesNoU = dot(outNormal, upVec);

		puddles = GetPuddles(worldPos, newCoord, lightmap.y, puddlesNoU, wetness);
		puddles *= 1.0 - lava;

		ApplyPuddleToMaterial(puddles, albedo, smoothness, f0, porosity);

		if (puddles > 0.001 && rainStrength > 0.001) {
			mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
							  tangent.y, binormal.y, normal.y,
							  tangent.z, binormal.z, normal.z);

			vec3 puddleNormal = GetPuddleNormal(worldPos, viewPos, tbnMatrix);
			outNormal = normalize(
				mix(outNormal, puddleNormal, puddles * sqrt(1.0 - porosity) * rainStrength)
			);
		}
		#endif

		skyOcclusion = lightmap.y;
		
		baseReflectance = mix(vec3(f0), rawAlbedo, metalness);
		float fresnel = pow(clamp(1.0 + dot(outNormal, normalize(viewPos.xyz)), 0.0, 1.0), 5.0);

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
										   specularColor, shadow * vanillaDiffuse, color.a);
		#endif
		
		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR && defined REFLECTION_ROUGH
		newNormal = outNormal;
		if ((normalMap.x > -0.999 || normalMap.y > -0.999) && viewVector == viewVector) {
			normalMap = mix(vec3(0.0, 0.0, 1.0), normalMap, smoothness);
			newNormal = mix(normalMap * tbnMatrix, newNormal, 1.0 - pow(1.0 - puddles, 4.0));
			newNormal = clamp(normalize(newNormal), vec3(-1.0), vec3(1.0));
		}
		#endif
		
		#ifdef IRIS_FEATURE_FADE_VARIABLE
		ChunkFade(albedo.rgb, viewPos, chunkFade);
		#endif

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		#endif
	}

    /* DRAWBUFFERS:0 */
    gl_FragData[0] = albedo;

	#ifdef MCBL_SS
		/* DRAWBUFFERS:08 */
		gl_FragData[1] = vec4(lightAlbedo, 1.0);

		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR
		/* DRAWBUFFERS:08367 */
		gl_FragData[2] = vec4(smoothness, skyOcclusion, 0.0, 1.0);
		gl_FragData[3] = vec4(EncodeNormal(newNormal), float(gl_FragCoord.z < 1.0), 1.0);
		gl_FragData[4] = vec4(fresnel3, 1.0);
		#endif
	#else
		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR
		/* DRAWBUFFERS:0367 */
		gl_FragData[1] = vec4(smoothness, skyOcclusion, 0.0, 1.0);
		gl_FragData[2] = vec4(EncodeNormal(newNormal), float(gl_FragCoord.z < 1.0), 1.0);
		gl_FragData[3] = vec4(fresnel3, 1.0);
		#endif
	#endif
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying float mat, recolor;

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

#ifdef IRIS_FEATURE_FADE_VARIABLE
varying float chunkFade;
#endif

//Uniforms//
uniform int worldTime;

uniform float frameTimeCounter;
uniform float timeAngle;

uniform vec3 cameraPosition;
uniform vec3 relativeEyePosition;

uniform mat4 gbufferModelView, gbufferModelViewInverse;

#ifdef TAA
uniform int frameCounter;

uniform float viewWidth, viewHeight;
#endif

//Attributes//
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

#ifdef ADVANCED_MATERIALS
attribute vec4 at_tangent;
#endif

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

	int blockID = int(mc_Entity.x / 100);

	normal = normalize(gl_NormalMatrix * gl_Normal);

	#ifdef ADVANCED_MATERIALS
	binormal = normalize(gl_NormalMatrix * cross(at_tangent.xyz, gl_Normal.xyz) * at_tangent.w);
	tangent  = normalize(gl_NormalMatrix * at_tangent.xyz);
	
	mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
						  tangent.y, binormal.y, normal.y,
						  tangent.z, binormal.z, normal.z);
								  
	viewVector = tbnMatrix * (gl_ModelViewMatrix * gl_Vertex).xyz;

	if (mc_Entity.x == 0)
		viewVector /= 0.0;
	
	dist = length(gl_ModelViewMatrix * gl_Vertex);

	vec2 midCoord = (gl_TextureMatrix[0] *  mc_midTexCoord).st;
	vec2 texMinMidCoord = texCoord - midCoord;

	vTexCoordAM.pq  = abs(texMinMidCoord) * 2;
	vTexCoordAM.st  = min(texCoord, midCoord - texMinMidCoord);
	
	vTexCoord.xy    = sign(texMinMidCoord) * 0.5 + 0.5;
	#endif
    
	color = gl_Color;
	
	mat = 0.0; recolor = 0.0;

	if (blockID >= 100 && blockID < 150)
		mat = 1.0;
	if (blockID == 105 || blockID == 106){
		mat = 2.0;
		color.rgb *= 1.225;
	}
	if (blockID >= 150 && blockID < 200)
		mat = 3.0;
	if (blockID == 153){
		mat = 4.0;
		lmCoord.x += 0.0667;
	}
	if (blockID == 158)
		mat = 5.0;
	if (blockID == 159)
		mat = 6.0;
	if (blockID == 160)
		mat = 7.0;
	if (blockID == 251)
		mat = 8.0;

	if (blockID == 151 || blockID == 155 || blockID == 156)
		recolor = 1.0;

	if (blockID == 152)
		lmCoord.x -= 0.0667;

	if (color.a < 0.1)
		color.a = 1.0;

	#ifdef IRIS_FEATURE_FADE_VARIABLE
	chunkFade = mc_chunkFade;
	#endif

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
	
	float istopv = gl_MultiTexCoord0.t < mc_midTexCoord.t ? 1.0 : 0.0;
	position.xyz = WavingBlocks(position.xyz, blockID, istopv);

    #ifdef WORLD_CURVATURE
	position.y -= WorldCurvature(position.xz);
    #endif

	gl_Position = gl_ProjectionMatrix * gbufferModelView * position;
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif