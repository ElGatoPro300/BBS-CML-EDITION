#ifdef SKY_VANILLA
#ifndef VOXY_PATCH
uniform vec3 skyColor;

#ifndef NETHER
uniform vec3 fogColor;
#endif
#endif

vec3 skyCol = pow(skyColor, vec3(2.2)) * SKY_I * SKY_I;
vec3 fogCol = pow(fogColor, vec3(2.2)) * SKY_I * SKY_I;
#else
vec3 skyColSqrt = vec3(SKY_R, SKY_G, SKY_B) * SKY_I / 255.0;
vec3 fogColSqrt = vec3(FOG_R, FOG_G, FOG_B) * FOG_I / 255.0;
vec3 skyCol = skyColSqrt * skyColSqrt;
vec3 fogCol = fogColSqrt * fogColSqrt;
#endif