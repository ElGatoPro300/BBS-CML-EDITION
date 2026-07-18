#ifdef OVERWORLD
float sunSkyVisibility = clamp(dot(sunVec, upVec) * 2.0 + 0.5, 0.0, 1.0);

vec3 lightSkyColRaw = mix(lightNight, lightSun, sunSkyVisibility);
vec3 lightSkyColSqrt = mix(lightSkyColRaw, dot(lightSkyColRaw, vec3(0.299, 0.587, 0.114)) * weatherCol.rgb, rainStrength);
vec3 lightSkyCol = lightSkyColSqrt * lightSkyColSqrt;
#endif