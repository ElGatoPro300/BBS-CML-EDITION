/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

//Settings//
#include "/lib/settings.glsl"

//Fragment Shader///////////////////////////////////////////////////////////////////////////////////
#ifdef FSH

//Varyings//
varying vec2 texCoord;

varying vec4 color;

//Uniforms//
uniform ivec2 eyeBrightnessSmooth;

uniform sampler2D texture;

//Common Variables//
float eBS = eyeBrightnessSmooth.y / 240.0;

//Program//
void main() {
	vec4 albedo = texture2D(texture, texCoord) * color;
	albedo.rgb *= albedo.a;
	albedo.rgb = pow(albedo.rgb,vec3(2.2)) / (4.0 - 3.0 * eBS);

	#if ALPHA_BLEND == 0
	albedo.rgb = sqrt(max(albedo.rgb, vec3(0.0)));
	#endif
	
    /* DRAWBUFFERS:0 */
	gl_FragData[0] = albedo;
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord;

varying vec4 color;

//Uniforms//
#ifdef TAA
uniform int frameCounter;

uniform float viewWidth;
uniform float viewHeight;
#include "/lib/util/jitter.glsl"
#endif

#ifdef WORLD_CURVATURE
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
#endif

//Includes//
#ifdef WORLD_CURVATURE
#include "/lib/vertex/worldCurvature.glsl"
#endif

//Program//
void main() {
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;

	color = gl_Color;

	#ifdef WORLD_CURVATURE
	vec4 position = gbufferModelViewInverse * gl_ModelViewMatrix * gl_Vertex;
	if (gl_ProjectionMatrix[2][2] < -0.5) position.y -= WorldCurvature(position.xz);
	gl_Position = gl_ProjectionMatrix * gbufferModelView * position;
	#else
	gl_Position = ftransform();
	#endif
	
	#if defined TAA && !defined TAA_SELECTIVE
	gl_Position.xy = TAAJitter(gl_Position.xy, gl_Position.w);
	#endif
}

#endif