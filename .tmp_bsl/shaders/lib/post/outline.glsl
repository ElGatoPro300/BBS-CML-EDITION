float SampleFullLinearDepth(vec2 coord) {
    float z = texture2D(depthtex0, coord).r;
    float linZ = GetLinearDepth(z, gbufferProjectionInverse);

	#ifdef VOXY
	if (z >= 1.0) {
    	z = texture2D(vxDepthTexTrans, coord).r;
        linZ = GetLinearDepth(z, vxProjInv);
    }
	#endif
	#ifdef DISTANT_HORIZONS
	if (z >= 1.0) {
    	z = texture2D(dhDepthTex0, coord).r;
        linZ = GetLinearDepth(z, dhProjectionInverse);
    }
	#endif

    return linZ;
}

vec3 GetOutlinedViewPos(float linZ) {
    float z = GetNonLinearDepth(linZ, gbufferProjection);

    vec4 viewPos = gbufferProjectionInverse * (vec4(texCoord.x, texCoord.y, z, 1.0) * 2.0 - 1.0);
	viewPos /= viewPos.w;

    return viewPos.xyz;
}

void Outline(vec3 color, bool secondPass, out vec4 outerOutline, out vec4 innerOutline, out float minLinZ) {
	float ph = ceil(viewHeight / 1600.0) / viewHeight;
	float pw = ph / aspectRatio;

	float oOutlineMask = 1.0, iOutlineMask = 1.0;
	vec3 oOutlineColor = vec3(0.0), iOutlineColor = color;

	float linZ = SampleFullLinearDepth(texCoord);
	minLinZ = linZ;
    float outerMult = 8.0 / linZ;

    int sampleCount = viewHeight >= 720.0 ? 12 : 4;

    #ifdef RETRO_FILTER
    ph = RETRO_FILTER_SIZE / viewHeight;
    pw = ph / aspectRatio;
    sampleCount = 4;
    #endif

	for (int i = 0; i < sampleCount; i++) {
		vec2 offset = vec2(pw, ph) * outlineOffsets[i];
        float linSampleZSum = 0.0, linSampleZDiff = 0.0;

        for (int j = 0; j < 2; j++) {
            float linSampleZ = SampleFullLinearDepth(texCoord + offset);

            #ifdef OUTLINE_OUTER_COLOR
            if(linSampleZ < (minLinZ + 0.01)) {
                oOutlineColor = texture2D(colortex0, texCoord + offset).rgb;
            }
            #endif

            linSampleZSum += linSampleZ;
            if(j == 0) linSampleZDiff = linSampleZ;
            else linSampleZDiff -= linSampleZ;
            
            minLinZ = min(minLinZ, linSampleZ);
            offset = -offset;
        }

        #ifdef OUTLINE_OUTER
        oOutlineMask *= clamp(1.0 - (linZ * 2.0 - linSampleZSum) * outerMult, 0.0, 1.0);
        #endif
        
        #ifdef OUTLINE_INNER
        linSampleZSum -= abs(linSampleZDiff) * 0.5;
        iOutlineMask *= clamp(1.125 + (linZ * 2.0 - linSampleZSum) * 64.0, 0.0, 1.0);
        #endif
	}
    
	#if ALPHA_BLEND == 0
	oOutlineColor *= oOutlineColor;
	#endif

    oOutlineColor *= 0.125;
    oOutlineMask = 1.0 - oOutlineMask;

    iOutlineColor *= 1.4;
    iOutlineMask = 1.0 - iOutlineMask;

    vec3 viewPos = GetOutlinedViewPos(minLinZ);

	if (oOutlineMask > 0.001) {
		Fog(oOutlineColor, viewPos);
		if (isEyeInWater == 1.0 && secondPass) {
            vec4 waterFog = GetWaterFog(viewPos, vec3(0.0));
            oOutlineColor = mix(oOutlineColor, waterFog.rgb, waterFog.a);
        }
	}

	outerOutline = vec4(oOutlineColor, oOutlineMask);
    innerOutline = vec4(iOutlineColor, iOutlineMask);
}