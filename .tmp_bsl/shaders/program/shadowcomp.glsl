/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Compute Shader////////////////////////////////////////////////////////////////////////////////////
#ifdef CSH

layout (local_size_x = 8, local_size_y = 8, local_size_z = 8) in;

#ifdef MCBL_HALF_HEIGHT
#if MCBL_DISTANCE == 128
const ivec3 workGroups = ivec3(16, 8, 16);
#elif MCBL_DISTANCE == 192
const ivec3 workGroups = ivec3(24, 12, 24);
#elif MCBL_DISTANCE == 256
const ivec3 workGroups = ivec3(32, 16, 32);
#elif MCBL_DISTANCE == 384
const ivec3 workGroups = ivec3(48, 24, 48);
#elif MCBL_DISTANCE == 512
const ivec3 workGroups = ivec3(64, 32, 64);
#endif
#else
#if MCBL_DISTANCE == 128
const ivec3 workGroups = ivec3(16, 16, 16);
#elif MCBL_DISTANCE == 192
const ivec3 workGroups = ivec3(24, 24, 24);
#elif MCBL_DISTANCE == 256
const ivec3 workGroups = ivec3(32, 32, 32);
#elif MCBL_DISTANCE == 384
const ivec3 workGroups = ivec3(48, 32, 48);
#elif MCBL_DISTANCE == 512
const ivec3 workGroups = ivec3(64, 32, 64);
#endif
#endif

//Uniforms//
uniform float framemod2;

uniform vec3 cameraPosition, previousCameraPosition;

uniform usampler3D voxeltex;
uniform sampler3D lighttex0;
uniform sampler3D lighttex1;

writeonly uniform image3D lightimg0;
writeonly uniform image3D lightimg1;

//Common Variables//

//Common Functions//
vec3 HSV2RGB(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 GetLightFromNeighbors(sampler3D lighttex, ivec3 pos) {
	vec3 light = vec3(0.0);
	
	light += texelFetch(lighttex, pos + ivec3( 1,0,0), 0).rgb;
	light += texelFetch(lighttex, pos + ivec3(-1,0,0), 0).rgb;
	light += texelFetch(lighttex, pos + ivec3(0, 1,0), 0).rgb;
	light += texelFetch(lighttex, pos + ivec3(0,-1,0), 0).rgb;
	light += texelFetch(lighttex, pos + ivec3(0,0, 1), 0).rgb;
	light += texelFetch(lighttex, pos + ivec3(0,0,-1), 0).rgb;
	light /= 6.0 / 0.995;

	return light;
}

//Includes//
#include "/lib/color/blocklightColor.glsl"
#include "/lib/util/voxelMapHelper.glsl"
#include "/lib/lighting/voxelColorFetch.glsl"

//Program//
void main() {
	int iFrameMod2 = int(framemod2);

	ivec3 pos = ivec3(gl_GlobalInvocationID);
	uint voxelData = texelFetch(voxeltex, pos, 0).r;

	if (voxelData == 99) {
		if (iFrameMod2 == 0) {
			imageStore(lightimg0, pos, vec4(0));
		} else {
			imageStore(lightimg1, pos, vec4(0));
		}
		return;
	}

	vec3 emission = GetEmission(voxelData);
	vec3 tint = GetTint(voxelData);
	vec3 light = vec3(0.0);

	#ifdef MCBL_RANDOM
	if (emission != vec3(0.0)) {
		vec3 randomPos = pos + floor(cameraPosition) - voxelMapSize / 2;
		emission = GetRandomEmissionHSV(randomPos, voxelData);
		emission = HSV2RGB(emission);
	}
	#endif

	ivec3 prevPos = pos + ivec3(floor(cameraPosition) - floor(previousCameraPosition));

	if (emission == vec3(0.0)) {
		if (iFrameMod2 == 0) {
			light = GetLightFromNeighbors(lighttex1, prevPos);
		} else {
			light = GetLightFromNeighbors(lighttex0, prevPos);
		}
	}

	vec4 color = vec4(emission + light * tint, 0.0);

	if (iFrameMod2 == 0) {
		imageStore(lightimg0, pos, color);
	} else {
		imageStore(lightimg1, pos, color);
	}
}

#endif