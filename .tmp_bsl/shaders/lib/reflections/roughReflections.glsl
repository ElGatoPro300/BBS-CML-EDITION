#ifdef REFLECTION_PREVIOUS
#define colortexR colortex5
#else
#define colortexR colortex0
#endif

vec4 RoughReflection(vec3 viewPos, vec3 normal, float dither, float smoothness) {
    vec4 color = vec4(0.0);
	float border = 0.0;

	#if WATER_NORMALS == 0
	float inc = 1.4;
	#else
	float inc = 1.8;
	#endif

    vec4 pos = Raytrace(depthtex0, viewPos, normal, dither, border, 6, 0.5, 0.1, inc);
	border = clamp(13.333 * (1.0 - border) * (0.9 * smoothness + 0.1), 0.0, 1.0);

	float fovScale = gbufferProjection[1][1] / 1.37;
	
	if (pos.z < 1.0 - 1e-5) {
		#ifdef REFLECTION_ROUGH
		float dist = 0.03125 * pow(1.0 - smoothness, 2.0) * pos.a * fovScale;
		float lod = log2(viewHeight * dist);
		#else
		float lod = 0.0;
		#endif

		if (lod < 1.0) {
			color.a = texture2DLod(colortex6, pos.st, 1.0).b;
			if (color.a > 0.001) color.rgb = texture2DLod(colortexR, pos.st, 1.0).rgb;
			#ifdef REFLECTION_PREVIOUS
			color.rgb = pow(color.rgb * 2.0, vec3(8.0));
			#else
			#if ALPHA_BLEND == 0
			color.rgb = pow(color.rgb, vec3(2.2));
			#endif
			#endif
		}else{
			for(int i = -2; i <= 2; i++) {
				for(int j = -2; j <= 2; j++) {
					vec2 refOffset = vec2(i, j) * exp2(lod - 1.0) / vec2(viewWidth, viewHeight);
					vec2 refCoord = pos.st + refOffset;
					float alpha = texture2DLod(colortex6, refCoord, lod).b;
					if (alpha > 0.001) {
						vec3 ssrSample = texture2DLod(colortexR, refCoord, max(lod - 1.0, 0.0)).rgb;

						#ifdef REFLECTION_PREVIOUS
						ssrSample = pow(ssrSample * 2.0, vec3(8.0));
						#else
						#if ALPHA_BLEND == 0
						ssrSample = pow(ssrSample, vec3(2.2));
						#endif
						#endif

						color.rgb += ssrSample;
						color.a += alpha;
					}
				}
			}
			color /= 25.0;
		}

		//Fog(color.rgb, (gbufferProjectionInverse * pos).xyz);
		
		color *= color.a;
		color.a = clamp(color.a * 2.0 - 1.0, 0.0, 1.0) * border;
		// color.a = pos.a * float(border > 0.0);
	}
	
    return color;
}