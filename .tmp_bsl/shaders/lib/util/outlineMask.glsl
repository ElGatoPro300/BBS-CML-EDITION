float GetOutlineMask() {
	float ph = ceil(viewHeight / 1600.0) / viewHeight;
	float pw = ph / aspectRatio;

    int sampleCount = viewHeight >= 720.0 ? 12 : 4;
	
    #ifdef RETRO_FILTER
    ph = RETRO_FILTER_SIZE / viewHeight;
    pw = ph / aspectRatio;
    sampleCount = 4;
    #endif

	float mask = 0.0;
	for (int i = 0; i < sampleCount; i++) {
		vec2 offset = vec2(pw, ph) * outlineOffsets[i];
        for (int j = 0; j < 2; j++){
            float z0 = texture2D(depthtex0, texCoord + offset).r;
            float z1 = texture2D(depthtex1, texCoord + offset).r;

            float linZ0 = GetLinearDepth(z0, gbufferProjectionInverse);
            float linZ1 = GetLinearDepth(z1, gbufferProjectionInverse);

            #ifdef VOXY
            if (z0 >= 1.0) {
                z0 = texture2D(vxDepthTexTrans, texCoord + offset).r;
                linZ0 = GetLinearDepth(z0, vxProjInv);
            }
            if (z1 >= 1.0) {
                z1 = texture2D(vxDepthTexOpaque, texCoord + offset).r;
                linZ1 = GetLinearDepth(z1, vxProjInv);
            }
            #endif

            #ifdef DISTANT_HORIZONS
            if (z0 >= 1.0) {
                z0 = texture2D(dhDepthTex0, texCoord + offset).r;
                linZ0 = GetLinearDepth(z0, dhProjectionInverse);
            }
            if (z1 >= 1.0) {
                z1 = texture2D(dhDepthTex1, texCoord + offset).r;
                linZ1 = GetLinearDepth(z1, dhProjectionInverse);
            }
            #endif

		    mask += float(linZ0 < linZ1);
            offset = -offset;
        }
	}

	return float(mask > 0.5);
}