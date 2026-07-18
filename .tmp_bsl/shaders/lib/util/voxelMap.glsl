vec3 ObjectToView(vec3 objectPos) {
	return mat3(gl_ModelViewMatrix) * objectPos + gl_ModelViewMatrix[3].xyz;
}

vec3 ViewToWorld(vec3 viewPos) {
	return mat3(shadowModelViewInverse) * viewPos + shadowModelViewInverse[3].xyz;
}

void UpdateVoxelMap(int vertexID, int blockData) {
	if (vertexID % 4 != 0) {
		return;
	}
	
	vec3 objectPos = gl_Vertex.xyz + at_midBlock / 64.0;
	vec3 viewPos = ObjectToView(objectPos);
	vec3 worldPos = ViewToWorld(viewPos);
	vec3 voxelPos = WorldToVoxel(worldPos);

	bool isEligible = renderStage == MC_RENDER_STAGE_TERRAIN_SOLID ||
					  renderStage == MC_RENDER_STAGE_TERRAIN_TRANSLUCENT ||
					  renderStage == MC_RENDER_STAGE_TERRAIN_CUTOUT ||
					  renderStage == MC_RENDER_STAGE_TERRAIN_CUTOUT_MIPPED;
	bool isInVolume = IsInVoxelMapVolume(voxelPos);

	if (isEligible && isInVolume && blockData > 0) {
		imageStore(voxelimg, ivec3(voxelPos), uvec4(blockData, 0u, 0u, 0u));
	}
}