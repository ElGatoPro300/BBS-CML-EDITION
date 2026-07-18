vec3 ApplyHandlightColor(vec3 mcblCol, vec3 worldPos) {
    vec3 heldLightPos = worldPos + relativeEyePosition + vec3(0.0, 0.5, 0.0);

    float falloff = max(1.0 - 2.0 / 15.0 * length(heldLightPos), 0.0);
	falloff = pow(0.3, length(heldLightPos)) * 0.5;

	int heldData = heldItemId % 100;
	int heldData2 = heldItemId2 % 100;

	vec3 handlightColor = vec3(0.0);
	if (heldData >= 1 && heldData <= 50) {
		handlightColor += lightColorsRGB[heldData - 1];
	}
	if (heldData2 >= 1 && heldData2 <= 50) {
		handlightColor += lightColorsRGB[heldData2 - 1];
	}

	mcblCol = mix(mcblCol, handlightColor, falloff);

    return mcblCol;
}

#ifdef MCBL_SS
vec2 Reprojection(vec3 pos) {
	pos = pos * 2.0 - 1.0;

	vec4 viewPosPrev = gbufferProjectionInverse * vec4(pos, 1.0);
	viewPosPrev /= viewPosPrev.w;
	viewPosPrev = gbufferModelViewInverse * viewPosPrev;

	vec3 cameraOffset = cameraPosition - previousCameraPosition;
	cameraOffset *= float(pos.z > 0.56);

	vec4 previousPosition = viewPosPrev + vec4(cameraOffset, 0.0);
	previousPosition = gbufferPreviousModelView * previousPosition;
	previousPosition = gbufferPreviousProjection * previousPosition;
	return previousPosition.xy / previousPosition.w * 0.5 + 0.5;
}
#endif

float GetMCBLLegacyMask(vec3 worldPos) {
	#if MCBL_SS_MODE == 0 && defined MULTICOLORED_BLOCKLIGHT
	vec3 maskPos = abs(worldPos / (voxelMapSize / 2));
	return float(maskPos.x > 0.9 || maskPos.y > 0.9 || maskPos.z > 0.9);
	#else
	return 1.0;
	#endif
}

vec3 ApplyMultiColoredBlocklight(vec3 blocklightCol, vec3 screenPos, vec3 worldPos, vec3 normal, float hand) {
	vec3 mcblCol = vec3(0.0);
	float voxelBounds = 0.0;

	#if defined MULTICOLORED_BLOCKLIGHT
	vec3 worldNormal = mat3(gbufferModelViewInverse) * normal;
	worldPos += worldNormal * 0.5;

	#ifdef WORLD_CURVATURE
	worldPos.y += dot(worldPos.xz, worldPos.xz) / WORLD_CURVATURE_SIZE;
	#endif

	vec3 voxelMapPos = WorldToVoxel(worldPos);
	
	if (IsInVoxelMapVolume(voxelMapPos)) {
		voxelBounds = GetVoxelMapSoftBounds(voxelMapPos);

		voxelMapPos /= voxelMapSize;
		
		int iFrameMod2 = int(frameCounter % 2);
		
		if (iFrameMod2 == 0) {
			mcblCol = texture3D(lighttex0, voxelMapPos).rgb;
		} else {
			mcblCol = texture3D(lighttex1, voxelMapPos).rgb;
		}

		mcblCol = ApplyHandlightColor(mcblCol, worldPos);

		mcblCol = normalize(mcblCol + vec3(1e-5));
		mcblCol *= mcblCol;
		mcblCol *= BLOCKLIGHT_I * BLOCKLIGHT_I * 2.0;
		
		blocklightCol = mix(blocklightCol, mcblCol, voxelBounds);
	}
	#endif

	#ifdef MCBL_SS
	if (hand < 0.5) {
		screenPos.xy = Reprojection(screenPos);
	}

	vec3 ssmcblCol = texture2DLod(colortex9, screenPos.xy, 2).rgb;
	float ssmcblFactor = min((ssmcblCol.r + ssmcblCol.g + ssmcblCol.b) * 2048.0, 1.0);

	#if MCBL_SS_MODE == 0 && defined MULTICOLORED_BLOCKLIGHT
	ssmcblFactor *= 1.0 - voxelBounds;
	#endif
	
	ssmcblCol = ssmcblCol + 0.000001;
	ssmcblCol = normalize(ssmcblCol * ssmcblCol) * 0.875 + 0.125;
	ssmcblCol *= BLOCKLIGHT_I * BLOCKLIGHT_I * 1.25;

	blocklightCol = mix(blocklightCol, ssmcblCol, ssmcblFactor);
	#endif

	return blocklightCol;
}