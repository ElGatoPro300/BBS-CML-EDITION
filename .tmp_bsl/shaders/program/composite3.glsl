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
uniform float centerDepthSmooth;
uniform float frameTime;
uniform float screenBrightness;
uniform float viewWidth, viewHeight, aspectRatio;

uniform mat4 gbufferProjection, gbufferProjectionInverse;

uniform sampler2D colortex0;
uniform sampler2D colortex2;
uniform sampler2D depthtex1;
uniform sampler2D depthtex2;

//Optifine Constants//
const bool colortex0MipmapEnabled = true;

//Common Variables//
float pw = 1.0 / viewWidth;
float ph = 1.0 / viewHeight;

vec2 dofOffsets[60] = vec2[60](
	vec2( 0.0    ,  0.25  ),
	vec2(-0.2165 ,  0.125 ),
	vec2(-0.2165 , -0.125 ),
	vec2( 0      , -0.25  ),
	vec2( 0.2165 , -0.125 ),
	vec2( 0.2165 ,  0.125 ),
	vec2( 0      ,  0.5   ),
	vec2(-0.25   ,  0.433 ),
	vec2(-0.433  ,  0.25  ),
	vec2(-0.5    ,  0     ),
	vec2(-0.433  , -0.25  ),
	vec2(-0.25   , -0.433 ),
	vec2( 0      , -0.5   ),
	vec2( 0.25   , -0.433 ),
	vec2( 0.433  , -0.2   ),
	vec2( 0.5    ,  0     ),
	vec2( 0.433  ,  0.25  ),
	vec2( 0.25   ,  0.433 ),
	vec2( 0      ,  0.75  ),
	vec2(-0.2565 ,  0.7048),
	vec2(-0.4821 ,  0.5745),
	vec2(-0.51295,  0.375 ),
	vec2(-0.7386 ,  0.1302),
	vec2(-0.7386 , -0.1302),
	vec2(-0.51295, -0.375 ),
	vec2(-0.4821 , -0.5745),
	vec2(-0.2565 , -0.7048),
	vec2(-0      , -0.75  ),
	vec2( 0.2565 , -0.7048),
	vec2( 0.4821 , -0.5745),
	vec2( 0.51295, -0.375 ),
	vec2( 0.7386 , -0.1302),
	vec2( 0.7386 ,  0.1302),
	vec2( 0.51295,  0.375 ),
	vec2( 0.4821 ,  0.5745),
	vec2( 0.2565 ,  0.7048),
	vec2( 0      ,  1     ),
	vec2(-0.2588 ,  0.9659),
	vec2(-0.5    ,  0.866 ),
	vec2(-0.7071 ,  0.7071),
	vec2(-0.866  ,  0.5   ),
	vec2(-0.9659 ,  0.2588),
	vec2(-1      ,  0     ),
	vec2(-0.9659 , -0.2588),
	vec2(-0.866  , -0.5   ),
	vec2(-0.7071 , -0.7071),
	vec2(-0.5    , -0.866 ),
	vec2(-0.2588 , -0.9659),
	vec2(-0      , -1     ),
	vec2( 0.2588 , -0.9659),
	vec2( 0.5    , -0.866 ),
	vec2( 0.7071 , -0.7071),
	vec2( 0.866  , -0.5   ),
	vec2( 0.9659 , -0.2588),
	vec2( 1      ,  0     ),
	vec2( 0.9659 ,  0.2588),
	vec2( 0.866  ,  0.5   ),
	vec2( 0.7071 ,  0.7071),
	vec2( 0.5    ,  0.8660),
	vec2( 0.2588 ,  0.9659)
);

//Common Functions//
float GetLinearDepth(float depth, mat4 invProjMatrix) {
    depth = depth * 2.0 - 1.0;
    vec2 zw = depth * invProjMatrix[2].zw + invProjMatrix[3].zw;
    return -zw.x / zw.y;
}

float GetNonLinearDepth(float linDepth, mat4 projMatrix) {
    vec2 zw = -linDepth * projMatrix[2].zw + projMatrix[3].zw;
    return (zw.x / zw.y) * 0.5 + 0.5;
}

vec3 DepthOfField(vec3 color, float z, float handZ, float tempFocusDistance, out float focusDistance) {
	vec3 dof = vec3(0.0);
	focusDistance = 0.0;
	float hand = float(abs(z - handZ) > 0.0001);
	
	float linZ = GetLinearDepth(z, gbufferProjectionInverse);

	#if DOF_FOCUS_MODE == 0
	float linFocus = GetLinearDepth(centerDepthSmooth, gbufferProjectionInverse);
	#elif DOF_FOCUS_MODE == 1
	vec2 focusPoint = vec2(DOF_FOCUS_X, DOF_FOCUS_Y);

	float centerDepth = texture2D(depthtex2, focusPoint).r;
	focusDistance = centerDepth;

	float linFocus = GetLinearDepth(tempFocusDistance, gbufferProjectionInverse);
	#elif DOF_FOCUS_MODE == 1
	float linFocus = max(DOF_FOCUS_DISTANCE, 0.05);
	#else
	float linFocus = max(DOF_FOCUS_DISTANCE * screenBrightness, 0.05);
	#endif
	
	float coc = abs(linZ - linFocus) / (linZ * linFocus);

	#ifdef DOF_FOCUS_POINT
	if (length((texCoord - focusPoint) * vec2(aspectRatio, 1.0)) < 0.01) {
		return vec3(1);
	}
	#endif

	coc = max(coc * DOF_STRENGTH * 1.0 - 0.025, 0.0);
	coc = coc / sqrt(coc * coc + 0.625);
	
	float fovScale = gbufferProjection[1][1] / 1.37;

	if (coc > 0.0 && hand < 0.5) {
		for(int i = 0; i < 60; i++) {
			vec2 offset = dofOffsets[i] * coc * 0.015 * fovScale * vec2(1.0 / aspectRatio, 1.0);
			float lod = log2(viewHeight * aspectRatio * coc * fovScale / 320.0);
			dof += texture2DLod(colortex0, texCoord + offset, lod).rgb;
		}
		dof /= 60.0;
	}
	else dof = color;

	return dof;
}

//Includes//
#ifdef OUTLINE_OUTER
#include "/lib/util/outlineOffset.glsl"
#include "/lib/util/outlineDepth.glsl"
#endif

//Program//
void main() {
	vec3 color = texture2DLod(colortex0, texCoord, 0.0).rgb;

	float tempFocusDistance = 1.0 - pow(texture2D(colortex2, vec2(5.0 * pw, ph)).r, 2.0);
	float focusDistance = 0.0;

	vec3 temporalColor = vec3(0.0);
	#ifdef TAA
	temporalColor = texture2D(colortex2, texCoord).gba;
	#endif
	
	#ifdef DOF
	float z = texture2D(depthtex1, texCoord.xy).x;
	float handZ = texture2D(depthtex2, texCoord.xy).x;

	#ifdef OUTLINE_OUTER
	DepthOutline(z, depthtex1);
	DepthOutline(handZ, depthtex2);
	#endif

	color = DepthOfField(color, z, handZ, tempFocusDistance, focusDistance);
	#endif
	
	float temporalData = 0.0;
	
	if (texCoord.x < 4.0 * pw && texCoord.y < 2.0 * ph) {
		temporalData = texture2D(colortex2, texCoord.xy).r;
	}
	
	#ifdef DOF
	if (texCoord.x >= 4.0 * pw && texCoord.x < 6.0 * pw && texCoord.y < 2.0 * ph) {
		float focusBlendFactor = exp2(-frameTime * 10.0 * DOF_FOCUS_SPEED);
		temporalData = sqrt(1.0 - mix(focusDistance, tempFocusDistance, focusBlendFactor));
	}
	#endif
	
    /*DRAWBUFFERS:02*/
	gl_FragData[0] = vec4(color, 1.0);
	gl_FragData[1] = vec4(temporalData, temporalColor);
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