#ifdef MCBL_HALF_HEIGHT
vec3 voxelMapSize = vec3(MCBL_DISTANCE, min(MCBL_DISTANCE / 2, 256), MCBL_DISTANCE);
#else
vec3 voxelMapSize = vec3(MCBL_DISTANCE, min(MCBL_DISTANCE, 256), MCBL_DISTANCE);
#endif

vec3 WorldToVoxel(vec3 worldPos) {
	return worldPos + fract(cameraPosition) + voxelMapSize / 2;
}

bool IsInVoxelMapVolume(vec3 voxelPos) {
	vec3 clampedVoxelPos = clamp(voxelPos, vec3(0), voxelMapSize);
	return voxelPos == clampedVoxelPos;
}

float GetVoxelMapSoftBounds(vec3 voxelPos) {
	float borderDistance = 8.0;
	vec3 halfVoxelMapSize = voxelMapSize / 2;

	vec3 centeredAbsVoxelPos = abs(voxelPos - halfVoxelMapSize);
	vec3 centerToBorderStart = halfVoxelMapSize - vec3(borderDistance);

	vec3 border3D = centeredAbsVoxelPos - centerToBorderStart;
	float border = max(max(border3D.x, border3D.y), border3D.z) / borderDistance;
	border = 1.0 - clamp(border, 0.0, 1.0);

	return border;
}