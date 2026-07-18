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

varying vec2 texCoord;

#ifdef WATER_CAUSTICS
varying vec3 worldPos;
#endif

varying vec4 color;

//Uniforms//
uniform int blockEntityId;

uniform sampler2D tex;

#ifdef WATER_CAUSTICS
uniform int worldTime;

uniform float frameTimeCounter;

uniform sampler2D noisetex;
#endif

//Common Variables//
#ifdef WATER_CAUSTICS
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

//Common Functions//
float GetWaterHeightMap(vec3 worldPos, vec2 offset) {
    float noise = 0.0, noiseA = 0.0, noiseB = 0.0;
    
    vec2 wind = vec2(time) * 0.5 * WATER_SPEED;

	worldPos.xz -= worldPos.y * 0.2;

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

    return noise * WATER_CAUSTICS_STRENGTH;
}
#endif

//Includes//
#ifdef WATER_SHADOW_COLOR
#include "/lib/color/waterColor.glsl"
#endif

//Program//
void main() {
    #if MC_VERSION >= 11300
	if (blockEntityId / 100 == 155) discard;
	#endif

    vec4 albedo = texture2D(tex, texCoord.xy);
	albedo.rgb *= color.rgb;

    float premult = float(mat > 0.98 && mat < 1.02);
	float water = float(mat > 1.98 && mat < 2.02);
	float disable = float(mat > 2.98 && mat < 3.02);
	if (disable > 0.5) discard;

	if (water > 0.5) {
		#if !defined WATER_SHADOW_COLOR && !defined WATER_CAUSTICS
			discard;
		#else
			#ifdef WATER_SHADOW_COLOR
				#if WATER_MODE == 0
					albedo.rgb = pow(waterColor.rgb / waterColor.a, vec3(0.25));
				#elif WATER_MODE == 1
					albedo.rgb = sqrt(albedo.rgb);
				#elif WATER_MODE == 2
					float waterLuma = length(albedo.rgb * albedo.rgb / pow(color.rgb, vec3(2.2))) * 2.0;
					albedo.rgb = sqrt(waterLuma * sqrt(waterColor.rgb / waterColor.a));
				#elif WATER_MODE == 3
					albedo.rgb = sqrt(color.rgb * 0.59);
				#endif

				#if WATER_ALPHA_MODE == 0
				albedo.a = waterAlpha;
				#else
				albedo.a = pow(albedo.a, WATER_VA);
				#endif
			#else
				albedo.rgb = vec3(1.0);
			#endif
		
			#ifdef WATER_CAUSTICS
				float normalOffset = WATER_SHARPNESS + 0.2;
				
				float normalStrength = 0.35;

				float h0 = GetWaterHeightMap(worldPos, vec2(0.0));
				float h1 = GetWaterHeightMap(worldPos, vec2( normalOffset, 0.0));
				float h2 = GetWaterHeightMap(worldPos, vec2(-normalOffset, 0.0));
				float h3 = GetWaterHeightMap(worldPos, vec2(0.0,  normalOffset));
				float h4 = GetWaterHeightMap(worldPos, vec2(0.0, -normalOffset));

				float xDeltaA = (h1 - h0) / normalOffset;
				float xDeltaB = (h2 - h0) / normalOffset;
				float yDeltaA = (h3 - h0) / normalOffset;
				float yDeltaB = (h4 - h0) / normalOffset;

				float height = max((xDeltaA * -xDeltaB + yDeltaA * -yDeltaB), 0.0);

				#if WATER_NORMALS == 1
				height *= 48.0;
				#elif WATER_NORMALS == 2
				height *= 24.0;
				#endif

				#ifdef WATER_SHADOW_COLOR
					height /= length(albedo.rgb);
				#endif

				height /= sqrt(height * height / 9.0 + 1.0);

				albedo.rgb *= 1.0 + height;
			#endif
		#endif
	}

    #ifdef SHADOW_COLOR
	albedo.rgb = mix(vec3(1.0), albedo.rgb, 1.0 - pow(1.0 - albedo.a, 1.5));
	albedo.rgb *= 1.0 - pow(albedo.a, 96.0);
	#else
	if ((premult > 0.5 && albedo.a < 0.98)) albedo.a = 0.0;
	#endif

	#ifdef WATER_CAUSTICS
	albedo.rgb *= 0.25;
	#endif

	gl_FragData[0] = albedo;
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

#extension GL_ARB_shader_image_load_store : enable

//Varyings//
varying float mat;

varying vec2 texCoord;

#ifdef WATER_CAUSTICS
varying vec3 worldPos;
#endif

varying vec4 color;

//Uniforms//
uniform int renderStage;
uniform int worldTime;

uniform float frameTimeCounter;

uniform vec3 cameraPosition;
uniform vec3 cameraPositionFract;
uniform vec3 relativeEyePosition;

uniform mat4 gbufferModelView, gbufferModelViewInverse;
uniform mat4 shadowProjection, shadowProjectionInverse;
uniform mat4 shadowModelView, shadowModelViewInverse;

#ifdef MULTICOLORED_BLOCKLIGHT
writeonly uniform uimage3D voxelimg;
#endif

//Attributes//
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

attribute vec3 at_midBlock;

//Common Variables//
#ifdef WORLD_TIME_ANIMATION
float time = float(worldTime) * 0.05 * ANIMATION_SPEED;
#else
float time = frameTimeCounter * ANIMATION_SPEED;
#endif

//Includes//
#include "/lib/vertex/waving.glsl"

#ifdef WORLD_CURVATURE
#include "/lib/vertex/worldCurvature.glsl"
#endif

#ifdef MULTICOLORED_BLOCKLIGHT
#include "/lib/util/voxelMapHelper.glsl"
#include "/lib/util/voxelMap.glsl"
#endif

//Program//
void main() {
	texCoord = gl_MultiTexCoord0.xy;
	
	int blockID = int(mc_Entity.x / 100);
	int blockData = int(mc_Entity.x) % 100;

	color = gl_Color;
	
	mat = 0;
	if (blockID == 201 || blockID == 202) mat = 1;
	if (blockID == 200 || blockID == 205) mat = 2;
	
	#ifndef SHADOW_VEGETATION
	if (blockID >= 100 && blockID <= 104) mat = 3;
	#endif
	
	vec4 position = shadowModelViewInverse * shadowProjectionInverse * ftransform();

	#ifdef WATER_CAUSTICS
	worldPos = position.xyz + cameraPosition.xyz;
	#endif
	
	float istopv = gl_MultiTexCoord0.t < mc_midTexCoord.t ? 1.0 : 0.0;
	position.xyz = WavingBlocks(position.xyz, blockID, istopv);

	#ifdef WORLD_CURVATURE
	position.y -= WorldCurvature(position.xz);
	#endif

	#ifdef MULTICOLORED_BLOCKLIGHT
	UpdateVoxelMap(gl_VertexID, blockData);
	#endif
	
	gl_Position = shadowProjection * shadowModelView * position;

	float dist = sqrt(gl_Position.x * gl_Position.x + gl_Position.y * gl_Position.y);
	float distortFactor = dist * shadowMapBias + (1.0 - shadowMapBias);
	
	gl_Position.xy *= 1.0 / distortFactor;
	gl_Position.z = gl_Position.z * 0.2;
}

#endif