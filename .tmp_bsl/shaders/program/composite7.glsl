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

//Uniforms//
uniform int frameCounter;
uniform float viewWidth, viewHeight, aspectRatio;

uniform vec3 cameraPosition, previousCameraPosition;

uniform mat4 gbufferPreviousProjection, gbufferProjectionInverse;
uniform mat4 gbufferPreviousModelView, gbufferModelViewInverse;

uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex1;

#ifdef TAA_SELECTIVE
uniform sampler2D colortex3;
#endif

#ifdef VOXY
uniform mat4 vxProjInv, vxProjPrev;

uniform sampler2D vxDepthTexOpaque;
#endif

#ifdef DISTANT_HORIZONS
uniform mat4 dhProjectionInverse, dhPreviousProjection;

uniform sampler2D dhDepthTex1;
#endif

//Optifine Constants//
#ifdef LIGHT_SHAFT
const bool colortex1MipmapEnabled = true;
#endif

//Common Functions//
float GetLuminance(vec3 color) {
	return dot(color, vec3(0.299, 0.587, 0.114));
}

//Includes//
#include "/lib/antialiasing/taa.glsl"

//Program//
void main() {
	vec2 newTexCoord = texCoord;
	#if defined TAA && TAA_MODE == 1
	vec2 offset = frameCounter % 2 == 0 ? vec2(0.5, 0.0) : vec2(0.0, 0.5);
	newTexCoord += offset / vec2(viewWidth, viewHeight);
	#endif

	vec3 color = texture2DLod(colortex1, newTexCoord, 0.0).rgb;
    vec4 prev = vec4(texture2DLod(colortex2, texCoord, 0).r, 0.0, 0.0, 0.0);
	
	#ifdef TAA
	prev = TemporalAA(color, prev.r);
	#endif

    /*DRAWBUFFERS:12*/
	gl_FragData[0] = vec4(color, 1.0);
	gl_FragData[1] = vec4(prev);
}

#endif

//Vertex Shader/////////////////////////////////////////////////////////////////////////////////////
#ifdef VSH

//Varyings//
varying vec2 texCoord;

//Program//
void main() {
	texCoord = gl_MultiTexCoord0.xy;
	
	gl_Position = ftransform();
}

#endif