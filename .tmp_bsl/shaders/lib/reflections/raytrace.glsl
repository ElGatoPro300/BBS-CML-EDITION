vec3 nvec3(vec4 pos) {
    return pos.xyz/pos.w;
}

vec4 nvec4(vec3 pos) {
    return vec4(pos.xyz, 1.0);
}

float cdist(vec2 coord) {
	return max(abs(coord.x - 0.5), abs(coord.y - 0.5)) * 1.85;
}

#if WATER_NORMALS == 0
#if REFLECTION_MODE == 0
float errMult = 1.0;
#elif REFLECTION_MODE == 1
float errMult = 1.8;
#else
float errMult = 2.2;
#endif
#else
#if REFLECTION_MODE == 0
float errMult = 1.0;
#elif REFLECTION_MODE == 1
float errMult = 1.3;
#else
float errMult = 1.6;
#endif
#endif

vec4 Raytrace(sampler2D depthtex, vec3 viewPos, vec3 normal, float dither, out float border, 
			  int maxf, float stp, float ref, float inc) {
	vec3 pos = vec3(0.0);
	float dist = 0.0;
	
	#ifdef TAA
	#if TAA_MODE == 0
	dither = fract(dither + frameCounter * 0.618);
	#else
	dither = fract(dither + frameCounter * 0.5);
	#endif
	#endif

	vec3 start = viewPos + normal * 0.075;

    vec3 vector = stp * reflect(normalize(viewPos), normalize(normal));
    viewPos += vector;
	vec3 tvector = vector;

    int sr = 0;

    for(int i = 0; i < 30; i++) {
        pos = nvec3(gbufferProjection * nvec4(viewPos)) * 0.5 + 0.5;
		if (pos.x < -0.05 || pos.x > 1.05 || pos.y < -0.05 || pos.y > 1.05) break;

		float sampleDepth = texture2D(depthtex, pos.xy).r;
        vec3 rfragpos = nvec3(gbufferProjectionInverse * nvec4(vec3(pos.xy, sampleDepth) * 2.0 - 1.0));

		#if REFLECTION_LOD == 1
		#ifdef VOXY
		if (sampleDepth >= 1.0) {
			sampleDepth = texture2D(vxDepthTexOpaque, pos.xy).r;
			rfragpos = nvec3(vxProjInv * nvec4(vec3(pos.xy, sampleDepth) * 2.0 - 1.0));
		}
		#endif
		#ifdef DISTANT_HORIZONS
		if (sampleDepth >= 1.0) {
			sampleDepth = texture2D(dhDepthTex1, pos.xy).r;
			rfragpos = nvec3(dhProjectionInverse * nvec4(vec3(pos.xy, sampleDepth) * 2.0 - 1.0));
		}
		#endif
		#endif

		dist = abs(dot(normalize(start - rfragpos), normal));

        float err = length(viewPos - rfragpos);
		float lVector = length(vector) * pow(length(tvector), 0.1) * errMult;
		if (err < lVector) {
			sr++;
			if (sr >= maxf) break;
			tvector -= vector;
			vector *= ref;
		}
        vector *= inc;
        tvector += vector;
		viewPos = start + tvector;
    }

	border = cdist(pos.st);

	#if defined REFLECTION_PREVIOUS || defined VOXY_PATCH
	// Previous frame reprojection from Chocapic13
	// Force reprojection on voxy chunks
	vec4 viewPosPrev = gbufferProjectionInverse * vec4(pos * 2.0 - 1.0, 1.0);
	viewPosPrev /= viewPosPrev.w;
	
	viewPosPrev = gbufferModelViewInverse * viewPosPrev;

	vec4 previousPosition = viewPosPrev + vec4(cameraPosition - previousCameraPosition, 0.0);
	previousPosition = gbufferPreviousModelView * previousPosition;
	previousPosition = gbufferPreviousProjection * previousPosition;
	pos.xy = previousPosition.xy / previousPosition.w * 0.5 + 0.5;
	#endif

	return vec4(pos, dist);
}

vec4 BasicReflect(vec3 viewPos, vec3 normal, out float border) {
	vec3 reflectedViewPos = reflect(viewPos, normal) + normal * dot(viewPos, normal) * 0.5;

	vec3 pos = nvec3(gbufferProjection * nvec4(reflectedViewPos)) * 0.5 + 0.5;

	border = cdist(pos.st);

	return vec4(pos, 0.0);
}