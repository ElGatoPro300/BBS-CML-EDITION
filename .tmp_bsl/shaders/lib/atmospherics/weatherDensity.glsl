#if defined OVERWORLD
#if defined WEATHER_PERBIOME
float fogDensity = FOG_DENSITY * mix(
	1.0,
	(
		FOG_DENSITY_COLD * isCold +
		FOG_DENSITY_DRY * (isDesert + isMesa + isSavanna) +
		FOG_DENSITY_DAMP * (isSwamp + isMushroom + isJungle)
	) / max(weatherWeight, 0.0001),
	weatherWeight
);
#else
float fogDensity = FOG_DENSITY;
#endif
#elif defined NETHER
float fogDensity = FOG_DENSITY_NETHER;
#elif defined END
float fogDensity = FOG_DENSITY_END;
#else
float fogDensity = FOG_DENSITY;
#endif