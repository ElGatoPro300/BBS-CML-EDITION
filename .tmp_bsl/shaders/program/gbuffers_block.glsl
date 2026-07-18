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
uniform int blockEntityId;
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
uniform float viewWidth, viewHeight, aspectRatio;

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

int endPortalLayerCount = 6;

vec3 endPortalColors[6] = vec3[6](
	vec3(0.4,1.0,0.9),
	vec3(0.2,0.9,1.0) * 0.80,
	vec3(0.7,0.5,1.0) * 0.25,
	vec3(0.4,0.5,1.0) * 0.30,
	vec3(0.3,0.8,1.0) * 0.30,
	vec3(0.3,0.8,1.0) * 0.35
);

vec3 endPortalParams[6] = vec3[6](
	vec3( 0.5,  0.00, 0.000),
	vec3( 1.5,  1.40, 0.125),
	vec3( 4.0,  2.50, 0.375),
	vec3( 8.0, -0.40, 0.250),
	vec3(12.0,  3.14, 0.500),
	vec3(12.0, -2.20, 0.625)
);

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color,vec3(0.299, 0.587, 0.114));
}

vec2 GetTriplanar(vec3 value, vec3 absWorldNormal, vec3 signWorldNormal) {
	vec2 xValue = value.zy * vec2( signWorldNormal.x,  1.0) * absWorldNormal.x;
	vec2 yValue = value.xz * vec2(-1.0, -signWorldNormal.y) * absWorldNormal.y;
	vec2 zValue = value.xy * vec2(-signWorldNormal.z,  1.0) * absWorldNormal.z;
	return xValue + yValue + zValue;
}

vec3 DrawEndPortal(vec3 worldPos, vec3 worldNormal, float dither) {
	vec3 absWorldNormal = abs(worldNormal);
	vec3 signWorldNormal = sign(worldNormal);
	vec3 bobbing = gbufferModelViewInverse[3].xyz;

	absWorldNormal.x = float(absWorldNormal.x > 0.5);
	absWorldNormal.y = float(absWorldNormal.y > 0.5);
	absWorldNormal.z = float(absWorldNormal.z > 0.5);
	signWorldNormal *= absWorldNormal;

	worldPos -= bobbing;
	vec3 cameraPos = cameraPosition + bobbing;

	vec2 portalCoord = GetTriplanar(worldPos, absWorldNormal, signWorldNormal);
	vec2 portalNormal = GetTriplanar(worldNormal, absWorldNormal, signWorldNormal);
	vec2 portalOffset = GetTriplanar(cameraPos, absWorldNormal, signWorldNormal);
	float portalDepth = dot(worldPos, -worldNormal);

	vec2 wind = vec2(0, time * 0.0125);

	portalCoord /= 16.0;
	portalOffset /= 16.0;
	
	#ifdef TAA
	dither = fract(dither + frameCounter * 0.618);
	#endif

	vec3 portalCol = vec3(0.0);

	int parallaxSampleCount = 6;
	float parallaxDepth = 0.0625;

	for (int i = 0; i < endPortalLayerCount; i++) {
		float layerScale = (portalDepth + endPortalParams[i].x) / portalDepth;
		vec2 scaledPortalCoord = portalCoord * layerScale;
		vec2 layerCoord = scaledPortalCoord + (portalOffset + endPortalParams[i].z);

		vec2 rot = vec2(cos(endPortalParams[i].y), sin(endPortalParams[i].y));
		layerCoord = vec2(layerCoord.x * rot.x - layerCoord.y * rot.y, layerCoord.x * rot.y + layerCoord.y * rot.x);
		layerCoord += wind;

		vec3 layerCol = texture2D(texture, layerCoord).r * endPortalColors[i];

		#ifdef PARALLAX_PORTAL
		for (int j = 0; j < parallaxSampleCount; j++) {
			float parallaxProgress = (j + dither) / parallaxSampleCount;
			float layerDepth = endPortalParams[i].x + parallaxProgress * parallaxDepth;
			
			layerScale = (portalDepth + layerDepth) / portalDepth;
			layerCoord = portalCoord * layerScale + (portalOffset + endPortalParams[i].z);
			
			layerCoord = vec2(layerCoord.x * rot.x - layerCoord.y * rot.y, layerCoord.x * rot.y + layerCoord.y * rot.x);
			layerCoord += wind;
			
			vec3 parallaxCol = texture2D(texture, layerCoord).r * endPortalColors[i];
			layerCol = max(layerCol, parallaxCol * (1.0 - parallaxProgress));
		}
		#endif

		float scaledLayerDistance = max(abs(scaledPortalCoord.x), abs(scaledPortalCoord.y)) * 0.25;
		float falloff = clamp(1.0 - scaledLayerDistance, 0.0, 1.0);
		layerCol *= falloff;

		portalCol += layerCol;
	}

	vec3 noiseColSqrt = vec3(END_R, END_G, END_B) / 255.0 * END_I;
	vec3 noiseCol = noiseColSqrt * 0.1;

	float noiseScale = (portalDepth + 32.0) / portalDepth;
	vec2 noiseCoord = portalCoord * noiseScale + portalOffset;
	noiseCoord /= 64.0;

	float cloudNoise = texture2D(noisetex, noiseCoord + wind * 2.0).r;

	portalCol += cloudNoise * noiseCol;

	portalCol *= portalCol;
	return portalCol;
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

//Program//
void main() {
    vec4 albedo = texture2D(texture, texCoord) * color;
	vec3 newNormal = normal;
	float smoothness = 0.0;
	vec3 lightAlbedo = vec3(0.0);
	
	#if MC_VERSION >= 11300
	int blockID = blockEntityId / 100;
	#else
	int blockID = blockEntityId;
	#endif

	#ifdef ADVANCED_MATERIALS
	vec2 newCoord = vTexCoord.st * vTexCoordAM.pq + vTexCoordAM.st;
	float surfaceDepth = 1.0;
	float parallaxFade = clamp((dist - PARALLAX_DISTANCE) / 32.0, 0.0, 1.0);

	#if MC_VERSION >= 11300
	float skipParallax = float(blockID == 251);
	#else
	float skipParallax = float(blockID == 63 || blockID == 68);
	#endif
	
	#ifdef PARALLAX
	if (skipParallax < 0.5) {
		newCoord = GetParallaxCoord(texCoord, parallaxFade, surfaceDepth);
		albedo = texture2DGradARB(texture, newCoord, dcdx, dcdy) * color;
	}
	#endif

	float skyOcclusion = 0.0;
	vec3 fresnel3 = vec3(0.0);
	#endif

	float endPortal = float(blockID == 252);

	if (endPortal < 0.5) {
		vec2 lightmap = clamp(lmCoord, vec2(0.0), vec2(1.0));
		
		float metalness       = 0.0;
		float emission        = float(blockID == 155);
		float subsurface      = 0.0;
		float basicSubsurface = float(blockID == 109) * 0.5;
		vec3 baseReflectance  = vec3(0.04);
		
		vec3 hsv = RGB2HSV(albedo.rgb);
		emission *= GetHardcodedEmission(albedo.rgb, hsv);

		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
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
		if (blockID == 155 && dot(color.rgb, vec3(1.0)) > 2.66) {
			float ec = length(albedo.rgb);
			albedo.rgb = blocklightCol * (ec * 0.63 / BLOCKLIGHT_I) + ec * 0.07;
		}
		#endif

		#ifdef MCBL_SS
		lightAlbedo = albedo.rgb + 0.00001;
		lightAlbedo = sqrt(normalize(lightAlbedo) * emission);

		#ifdef MULTICOLORED_BLOCKLIGHT
		lightAlbedo *= GetMCBLLegacyMask(worldPos);
		#endif
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
		GetLighting(albedo.rgb, shadow, viewPos, worldPos, normal, lightmap, color.a, NoL, 
					vanillaDiffuse, parallaxShadow, emission, subsurface, basicSubsurface);

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
										   specularColor, shadow * vanillaDiffuse, color.a);
		#endif

		#if defined ADVANCED_MATERIALS && defined REFLECTION_SPECULAR && defined REFLECTION_ROUGH
		normalMap = mix(vec3(0.0, 0.0, 1.0), normalMap, smoothness);
		newNormal = clamp(normalize(normalMap * tbnMatrix), vec3(-1.0), vec3(1.0));
		#endif

		#if ALPHA_BLEND == 0
		albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
		if(blockID == 155) albedo.a = sqrt(albedo.a);
		#endif
	}
	else
	{
		vec3 screenPos = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z);
		#ifdef TAA
		vec3 viewPos = ToNDC(vec3(TAAJitter(screenPos.xy, -0.5), screenPos.z));
		#else
		vec3 viewPos = ToNDC(screenPos);
		#endif
		vec3 worldPos = ToWorld(viewPos);
		vec3 worldNormal = normalize(mat3(gbufferModelViewInverse) * normal);
		
		float dither = Bayer8(gl_FragCoord.xy);

		albedo.rgb = DrawEndPortal(worldPos, worldNormal, dither);
		albedo.a = 1.0;

		lightAlbedo = normalize(albedo.rgb * 20.0 + 0.00001);
		
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
	binormal = normalize(gl_NormalMatrix * cross(at_tangent.xyz, gl_Normal.xyz) * at_tangent.w);
	tangent  = normalize(gl_NormalMatrix * at_tangent.xyz);
	
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

	if(color.a < 0.1) color.a = 1.0;

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
	
	#ifdef TAA
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif