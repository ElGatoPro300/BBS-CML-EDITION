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

uniform float far, near;
uniform float frameTimeCounter;
uniform float viewWidth, viewHeight, aspectRatio;

uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;

uniform sampler2D depthtex0;
uniform sampler2D noisetex;

#ifdef VOXY
uniform mat4 vxProjInv;

uniform sampler2D vxDepthTexOpaque;
#endif

#ifdef DISTANT_HORIZONS
uniform float dhFarPlane, dhNearPlane;

uniform mat4 dhProjectionInverse;
uniform sampler2D dhDepthTex0;
#endif

//Common Variables//
float pw = 1.0 / viewWidth;
float ph = 1.0 / viewHeight;

#ifdef VOXY
float vxNear = 16.0;
float vxFar = 48000.0;
#endif

//Common Functions//
float GetLinearDepth(float depth, mat4 invProjMatrix) {
    depth = depth * 2.0 - 1.0;
    vec2 zw = depth * invProjMatrix[2].zw + invProjMatrix[3].zw;
    return -zw.x / zw.y;
}

//Includes//
#include "/lib/lighting/ambientOcclusion.glsl"

//Program//
void main() {
    float ao = 1.0;
	float blueNoise = texture2D(noisetex, gl_FragCoord.xy / 512.0).b;

    float z = texture2D(depthtex0, texCoord.xy).r;
    #ifdef VOXY
    float vxZ = texture2D(vxDepthTexOpaque, texCoord.xy).r;
    #endif
    #ifdef DISTANT_HORIZONS
    float dhZ = texture2D(dhDepthTex0, texCoord.xy).r;
    #endif

    if (z < 1.0) {
        ao = AmbientOcclusion(z, depthtex0, gbufferProjectionInverse, near, far, 0.25, blueNoise, false);
    #ifdef VOXY
    } else if (vxZ < 1.0) {
        ao = AmbientOcclusion(vxZ, vxDepthTexOpaque, vxProjInv, vxNear, vxFar, 1.5, blueNoise, true);
        ao = pow(ao, 2.0);
    #endif
    #ifdef DISTANT_HORIZONS
    } else if (dhZ < 1.0) {
        ao = AmbientOcclusion(dhZ, dhDepthTex0, dhProjectionInverse, dhNearPlane, dhFarPlane, 1.5, blueNoise, true);
        ao = pow(ao, 2.0);
    #endif
    }
    
    /* DRAWBUFFERS:4 */
    gl_FragData[0] = vec4(ao, 0.0, 0.0, 0.0);
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
