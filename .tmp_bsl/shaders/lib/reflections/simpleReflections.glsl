vec4 SimpleReflection(vec3 viewPos, vec3 normal, float dither, out float reflectionMask) {
    vec4 color = vec4(0.0);
	float border = 0.0;
	reflectionMask = 0.0;

	#if WATER_NORMALS == 0
	float inc = 1.4;
	int maxf = 6;
	#else
	float inc = 2.0;
	int maxf = 4;
	#endif

    vec4 pos = Raytrace(depthtex1, viewPos, normal, dither, border, maxf, 1.0, 0.1, inc);
	border = clamp(13.333 * (1.0 - border), 0.0, 1.0);

	#ifdef REFLECTION_SKYBOX
	float zThreshold = 1.0 + 1e-5;
	#else
	float zThreshold = 1.0;
	#endif

	if (pos.z < zThreshold) {
		#if MC_VERSION > 10800
		color = texture2D(gaux2, pos.st);
		#else
		color = texture2DLod(gaux2, pos.st, 0);
		#endif
		reflectionMask = color.a;

		#ifdef REFLECTION_SKYBOX
		color.a = 1.0;
		#endif

		color.a *= border;
		reflectionMask *= border;
	}
	
    return color;
}

vec4 DHReflection(vec3 viewPos, vec3 normal, float dither, out float reflectionMask) {
    vec4 color = vec4(0.0);
	float border = 0.0;
	reflectionMask = 0.0;

    vec4 pos = BasicReflect(viewPos, normal, border);
	border = clamp(13.333 * (1.0 - border), 0.0, 1.0);

	#ifdef REFLECTION_SKYBOX
	float zThreshold = 1.0 + 1e-5;
	#else
	float zThreshold = 1.0;
	#endif
	
	if (pos.z < zThreshold) {
		color = texture2D(gaux2, pos.st);
		reflectionMask = color.a;

		#ifdef REFLECTION_SKYBOX
		color.a = 1.0;
		#endif

		color.a *= border;
		reflectionMask *= border;
	}
	
    return color;
}