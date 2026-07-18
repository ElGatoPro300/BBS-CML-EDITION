//Jitter offset from Chocapic13
#ifndef VOXY_PATCH
uniform float framemod8;
uniform float framemod2;
#endif

vec2 jitterOffsets8[8] = vec2[8](
							vec2( 0.125,-0.375),
							vec2(-0.125, 0.375),
							vec2( 0.625, 0.125),
							vec2( 0.375,-0.625),
							vec2(-0.625, 0.625),
							vec2(-0.875,-0.125),
							vec2( 0.375,-0.875),
							vec2( 0.875, 0.875)
						);
vec2 jitterOffsets2[2] = vec2[2](
							vec2( 1.0,  0.0),
							vec2( 0.0,  1.0)
						);
							   
vec2 TAAJitter(vec2 coord, float w) {
	#if TAA_MODE == 0
	vec2 offset = jitterOffsets8[int(framemod8)] * (w / vec2(viewWidth, viewHeight));
	#else
	vec2 offset = jitterOffsets2[int(framemod2)] * (w / vec2(viewWidth, viewHeight));
	#endif
	return coord + offset;
}