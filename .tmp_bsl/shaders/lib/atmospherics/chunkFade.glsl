#ifdef IRIS_FEATURE_FADE_VARIABLE
void ChunkFade(inout vec3 color, vec3 viewPos, float fade) {
    if (fade < 0.0 || fade >= 1.0) {
        return;
    }
    
	#ifdef OVERWORLD
    vec3 fadeColor = GetSkyColor(viewPos, false);
	#endif
	#ifdef NETHER
	vec3 fadeColor = netherCol.rgb * 0.0425;
	#endif
	#ifdef END
	vec3 fadeColor = endCol.rgb * 0.003;
	#endif

	fade = clamp(fade, 0.0, 1.0);
	color = mix(fadeColor, color, fade);
}
#endif