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
varying float dist;

varying vec2 texCoord, lmCoord;

varying vec3 normal, binormal, tangent;
varying vec3 sunVec, upVec, eastVec;
varying vec3 viewVector;

varying vec4 color;

//Uniforms//
uniform int bedrockLevel;
uniform int dhRenderDistance;
uniform int frameCounter;
uniform int isEyeInWater;
uniform int moonPhase;
uniform int worldTime;

uniform float blindFactor, darknessFactor, nightVision;
uniform float cloudHeight;
uniform float dhFarPlane;
uniform float endFlashIntensity;
uniform float far, near;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform float screenBrightness; 
uniform float shadowFade;
uniform float timeAngle, timeBrightness;
uniform float viewWidth, viewHeight;

uniform ivec2 eyeBrightnessSmooth;

uniform vec3 cameraPosition, previousCameraPosition;

uniform mat4 dhProjection, dhPreviousProjection, dhProjectionInverse;
uniform mat4 gbufferModelView, gbufferPreviousModelView, gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

uniform sampler2D texture;
uniform sampler2D gaux2;
uniform sampler2D depthtex1;
uniform sampler2D noisetex;
uniform sampler2D dhDepthTex1;

#if CLOUDS == 2
uniform sampler2D gaux1;
#endif

#ifdef VOXY
uniform int vxRenderDistance;

uniform mat4 vxProjInv;

uniform sampler2D vxDepthTexOpaque;
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

mat4 gbufferProjection = dhProjection;
mat4 gbufferPreviousProjection = dhPreviousProjection;
mat4 gbufferProjectionInverse = dhProjectionInverse;

//Common Functions//
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

vec3 GetParallaxWaves(vec3 worldPos, vec3 viewVector) {
	vec3 parallaxPos = worldPos;
	
	for(int i = 0; i < 4; i++) {
		float height = -1.25 * GetWaterHeightMap(parallaxPos, vec2(0.0)) + 0.25;
		parallaxPos.xz += height * viewVector.xy / dist;
	}
	return parallaxPos;
}

vec3 GetWaterNormal(vec3 worldPos, vec3 viewPos, vec3 viewVector) {
	vec3 waterPos = worldPos + cameraPosition;

	#if WATER_PIXEL > 0
	waterPos = floor(waterPos * WATER_PIXEL) / WATER_PIXEL;
	#endif

	#ifdef WATER_PARALLAX
	waterPos = GetParallaxWaves(waterPos, viewVector);
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

//Program//
void main() {
	vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);

	float opaqueDepth = texture2D(depthtex1, screenPos.xy).r;
	if (opaqueDepth < 1.0) discard;

    vec4 albedo = color;
	vec3 newNormal = normal;
	float smoothness = 0.0;
	vec3 lightAlbedo = vec3(0.0);

	vec3 vlAlbedo = vec3(1.0);
	vec3 refraction = vec3(0.0);

	float cloudBlendOpacity = 1.0;

	{
		vec2 lightmap = clamp(lmCoord, vec2(0.0), vec2(1.0));
		
		float water       = float(mat > 0.98 && mat < 1.02);
		float glass 	  = float(mat > 1.98 && mat < 2.02);
		float translucent = float(mat > 2.98 && mat < 3.02);
		float portal      = float(mat > 3.98 && mat < 4.02);
		
		float metalness       = 0.0;
		float emission        = portal;
		float subsurface      = 0.0;
		float basicSubsurface = water;
		vec3 baseReflectance  = vec3(0.04);
		
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);
		
		#ifndef REFLECTION_TRANSLUCENT
		glass = 0.0;
		translucent = 0.0;
		#endif

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

		#if CLOUDS == 2
		float cloudMaxDistance = 2.0 * far;
		#ifdef VOXY
		cloudMaxDistance = max(cloudMaxDistance, vxRenderDistance * 16.0);
		#endif
		#ifdef DISTANT_HORIZONS
		cloudMaxDistance = max(cloudMaxDistance, dhFarPlane);
		#endif

		float cloudViewLength = texture2D(gaux1, screenPos.xy).r * cloudMaxDistance;

		cloudBlendOpacity = step(viewLength, cloudViewLength);

		if (cloudBlendOpacity == 0) {
			discard;
		}
		// albedo.rgb *= fract(viewLength);
		#endif

		vec3 normalMap = vec3(0.0, 0.0, 1.0);
		
		mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
							  tangent.y, binormal.y, normal.y,
							  tangent.z, binormal.z, normal.z);

		#if WATER_NORMALS == 1 || WATER_NORMALS == 2
		if (water > 0.5) {
			normalMap = GetWaterNormal(worldPos, viewPos, viewVector);
			newNormal = clamp(normalize(normalMap * tbnMatrix), vec3(-1.0), vec3(1.0));
		}
		#endif

		#if REFRACTION == 1
		refraction = vec3((newNormal.xy - normal.xy) * 0.5 + 0.5, float(albedo.a < 0.95) * water);
		#elif REFRACTION == 2
		refraction = vec3((newNormal.xy - normal.xy) * 0.5 + 0.5, float(albedo.a < 0.95));
		#endif

		#ifdef TOON_LIGHTMAP
		lightmap = floor(lmCoord * 14.999) / 14.0;
		lightmap = clamp(lightmap, vec2(0.0), vec2(1.0));
		#endif

		albedo.rgb /= albedo.a;
    	albedo.rgb = pow(albedo.rgb, vec3(2.2));
		
		vlAlbedo = albedo.rgb;

		#ifdef WHITE_WORLD
		albedo.rgb = vec3(0.35);
		#endif
		
		if (water > 0.5) {
			#if WATER_MODE == 0
			albedo.rgb = waterColor.rgb * waterColor.a / albedo.a;
			#elif WATER_MODE == 1
			albedo.rgb *= WATER_VI * WATER_VI;
			#elif WATER_MODE == 2
			float waterLuma = length(albedo.rgb / pow(color.rgb, vec3(2.2)));
			albedo.rgb = waterLuma * waterColor.rgb * waterColor.a;
			#elif WATER_MODE == 3
			albedo.rgb *= WATER_VI * WATER_VI * 2.0;
			#endif
			#if WATER_ALPHA_MODE == 0
			albedo.a = waterAlpha;
			#else
			albedo.a = pow(albedo.a, WATER_VA);
			#endif
			vlAlbedo = sqrt(albedo.rgb);
			baseReflectance = vec3(0.02);
		}
		
		#if WATER_FOG == 1
		vec3 fogAlbedo = albedo.rgb;
		#endif
		
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
		GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, color.a, NoL, 
					vanillaDiffuse, 1.0, emission, subsurface, basicSubsurface);
		
		float fresnel = pow(clamp(1.0 + dot(newNormal, normalize(viewPos)), 0.0, 1.0), 5.0);

		if (water > 0.5 || ((translucent + glass) > 0.5 && albedo.a < 0.95)) {
			#if REFLECTION > 0
			vec4 reflection = vec4(0.0);
			vec3 skyReflection = vec3(0.0);
			float reflectionMask = 0.0;
	
			fresnel = fresnel * 0.98 + 0.02;
			fresnel*= max(1.0 - isEyeInWater * 0.5 * water, 0.5);
			// fresnel = 1.0;
			
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

		#if WATER_FOG == 1
		if((isEyeInWater == 0 && water > 0.5) || (isEyeInWater == 1 && water < 0.5)) {
			float opaqueDepth = texture2D(depthtex1, screenPos.xy).r;
			vec3 opaqueScreenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), opaqueDepth);
			#ifdef TAA
			vec3 opaqueViewPos = ToNDC(vec3(TAAJitter(opaqueScreenPos.xy, -0.5), opaqueScreenPos.z));
			#else
			vec3 opaqueViewPos = ToNDC(opaqueScreenPos);
			#endif

			vec4 waterFog = GetWaterFog(opaqueViewPos - viewPos.xyz, fogAlbedo);
			albedo = mix(waterFog, vec4(albedo.rgb, 1.0), albedo.a);
		}
		#endif

		Fog(albedo.rgb, viewPos);

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		#endif
	}
	albedo.a *= cloudBlendOpacity;

    /* DRAWBUFFERS:01 */
    gl_FragData[0] = albedo;
	gl_FragData[1] = vec4(vlAlbedo, 1.0);
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying float mat;
varying float dist;

varying vec2 texCoord, lmCoord;

varying vec3 normal, binormal, tangent;
varying vec3 sunVec, upVec, eastVec;
varying vec3 viewVector;

varying vec4 color;

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
attribute vec4 at_tangent;

//Common Variables//
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

//Common Functions//
float WavingWater(vec3 worldPos) {
	worldPos += cameraPosition;
	float fractY = fract(worldPos.y + 0.005);
		
	float wave = sin(6.2831854 * (time * 0.7 + worldPos.x * 0.14 + worldPos.z * 0.07)) +
				 sin(6.2831854 * (time * 0.5 + worldPos.x * 0.10 + worldPos.z * 0.20));
	if (fractY > 0.01) return wave * 0.0125 * ANIMATION_STRENGTH;
	
	return 0.0;
}

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
	
	int blockID = dhMaterialId;

	normal   = normalize(gl_NormalMatrix * gl_Normal);
	binormal = normalize(gbufferModelView[2].xyz);
	tangent  = normalize(gbufferModelView[0].xyz);
	
	mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
						  tangent.y, binormal.y, normal.y,
						  tangent.z, binormal.z, normal.z);
								  
	viewVector = tbnMatrix * (gl_ModelViewMatrix * gl_Vertex).xyz;
	
	dist = length(gl_ModelViewMatrix * gl_Vertex);
    
	color = gl_Color;
	
	mat = 0.0;

	if (blockID == DH_BLOCK_WATER) mat = 1.0;

	const vec2 sunRotationData = vec2(
		 cos(sunPathRotation * 0.01745329251994),
		-sin(sunPathRotation * 0.01745329251994)
	);
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
	eastVec = tangent;

	vec4 position = gbufferModelViewInverse * gl_ModelViewMatrix * gl_Vertex;
	
	// #ifdef WAVING_WATER
	// float istopv = gl_MultiTexCoord0.t < mc_midTexCoord.t ? 1.0 : 0.0;
	// if (blockID == 200 || blockID == 202 || blockID == 204) position.y += WavingWater(position.xyz);
	// #endif

    #ifdef WORLD_CURVATURE
	position.y -= WorldCurvature(position.xz);
    #endif

	gl_Position = gl_ProjectionMatrix * gbufferModelView * position;
	if (mat == 0.0) gl_Position.z -= 0.00001;
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif