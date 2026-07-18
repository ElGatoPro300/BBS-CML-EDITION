/* 
BSL Shaders v10 Series by Capt Tatsu 
https://capttatsu.com 
*/ 

vec2 neighbourOffsets[8] = vec2[8](
	vec2( 0.0, -1.0),
	vec2(-1.0,  0.0),
	vec2( 1.0,  0.0),
	vec2( 0.0,  1.0),
	vec2(-1.0, -1.0),
	vec2( 1.0, -1.0),
	vec2(-1.0,  1.0),
	vec2( 1.0,  1.0)
);

vec3 GetBlurredColor(vec2 view) {
	float blurFactor = 0.1667;
	vec3 color = texture2DLod(colortex1, texCoord + neighbourOffsets[4] * blurFactor / view, 0).rgb;
		 color+= texture2DLod(colortex1, texCoord + neighbourOffsets[5] * blurFactor / view, 0).rgb;
		 color+= texture2DLod(colortex1, texCoord + neighbourOffsets[6] * blurFactor / view, 0).rgb;
		 color+= texture2DLod(colortex1, texCoord + neighbourOffsets[7] * blurFactor / view, 0).rgb;
		 
	color /= 4.0;

	return color;
}

#ifdef TAA_SELECTIVE
float GetSkipFlag(float depth, vec2 view) {
	float skip = texture2D(colortex3, texCoord.xy).b;
	float skipDepth = depth;

	for (int i = 0; i < 4; i++) {
		float sampleDepth = texture2D(depthtex1, texCoord + neighbourOffsets[i + 4] / view).r;
		float sampleSkip = texture2D(colortex3, texCoord + neighbourOffsets[i + 4] / view).b;

		skip = (sampleDepth < skipDepth && sampleSkip == 0) ? 0 : skip;
		skipDepth = min(skipDepth, sampleDepth);
	}

	return skip;
}
#endif

//Previous frame reprojection from Chocapic13
vec2 Reprojection(vec3 pos, mat4 projectionInverse, mat4 previousProjection, bool useHand) {
	pos = pos * 2.0 - 1.0;

	vec4 viewPosPrev = projectionInverse * vec4(pos, 1.0);
	viewPosPrev /= viewPosPrev.w;
	viewPosPrev = gbufferModelViewInverse * viewPosPrev;

	vec3 cameraOffset = cameraPosition - previousCameraPosition;
	cameraOffset *= float(pos.z > 0.56 || !useHand);

	vec4 previousPosition = viewPosPrev + vec4(cameraOffset, 0.0);
	previousPosition = gbufferPreviousModelView * previousPosition;
	previousPosition = previousProjection * previousPosition;
	return previousPosition.xy / previousPosition.w * 0.5 + 0.5;
}

//Catmull-Rom sampling from Filmic SMAA presentation
vec3 textureCatmullRom(sampler2D colortex, vec2 texcoord, vec2 view) {
    vec2 position = texcoord * view;
    vec2 centerPosition = floor(position - 0.5) + 0.5;
    vec2 f = position - centerPosition;
    vec2 f2 = f * f;
    vec2 f3 = f * f2;

    float c = 0.7;
    vec2 w0 =        -c  * f3 +  2.0 * c         * f2 - c * f;
    vec2 w1 =  (2.0 - c) * f3 - (3.0 - c)        * f2         + 1.0;
    vec2 w2 = -(2.0 - c) * f3 + (3.0 -  2.0 * c) * f2 + c * f;
    vec2 w3 =         c  * f3 -                c * f2;

    vec2 w12 = w1 + w2;
    vec2 tc12 = (centerPosition + w2 / w12) / view;

    vec2 tc0 = (centerPosition - 1.0) / view;
    vec2 tc3 = (centerPosition + 2.0) / view;
    vec4 color = vec4(texture2DLod(colortex, vec2(tc12.x, tc0.y ), 0).gba, 1.0) * (w12.x * w0.y ) +
                 vec4(texture2DLod(colortex, vec2(tc0.x,  tc12.y), 0).gba, 1.0) * (w0.x  * w12.y) +
                 vec4(texture2DLod(colortex, vec2(tc12.x, tc12.y), 0).gba, 1.0) * (w12.x * w12.y) +
                 vec4(texture2DLod(colortex, vec2(tc3.x,  tc12.y), 0).gba, 1.0) * (w3.x  * w12.y) +
                 vec4(texture2DLod(colortex, vec2(tc12.x, tc3.y ), 0).gba, 1.0) * (w12.x * w3.y );
    return color.rgb / color.a;
}

vec3 RGBToYCoCg(vec3 col) {
	return vec3(
		col.r * 0.25 + col.g * 0.5 + col.b * 0.25,
		col.r * 0.5 - col.b * 0.5,
		col.r * -0.25 + col.g * 0.5 + col.b * -0.25
	);
}

vec3 YCoCgToRGB(vec3 col) {
	float n = col.r - col.b;
	return vec3(n + col.g, col.r + col.b, n - col.g);
}

vec3 ClipAABB(vec3 q,vec3 aabb_min, vec3 aabb_max){
	vec3 p_clip = 0.5 * (aabb_max + aabb_min);
	vec3 e_clip = 0.5 * (aabb_max - aabb_min) + 0.00000001;

	vec3 v_clip = q - vec3(p_clip);
	vec3 v_unit = v_clip.xyz / e_clip;
	vec3 a_unit = abs(v_unit);
	float ma_unit = max(a_unit.x, max(a_unit.y, a_unit.z));

	if (ma_unit > 1.0)
		return vec3(p_clip) + v_clip / ma_unit;
	else
		return q;
}

vec3 NeighbourhoodClipping(vec3 color, vec3 tempColor, vec2 view) {
	vec3 minclr = RGBToYCoCg(color);
	vec3 maxclr = minclr;

	for(int i = 0; i < 8; i++) {
		vec2 offset = neighbourOffsets[i] * view;
		vec3 clr = texture2DLod(colortex1, texCoord + offset, 0.0).rgb;

		clr = RGBToYCoCg(clr);
		minclr = min(minclr, clr); maxclr = max(maxclr, clr);
	}

	tempColor = RGBToYCoCg(tempColor);
	tempColor = ClipAABB(tempColor, minclr, maxclr);

	return YCoCgToRGB(tempColor);
}

vec4 TemporalAA(inout vec3 color, float tempData) {
	vec2 view = vec2(viewWidth, viewHeight);

	vec3 blur = GetBlurredColor(view);
	float depth = texture2D(depthtex1, texCoord).r;

	#ifdef VOXY
	float vxDepth = texture2D(vxDepthTexOpaque, texCoord).r;
	#endif

	#ifdef DISTANT_HORIZONS
	float dhDepth = texture2D(dhDepthTex1, texCoord).r;
	#endif

	#ifdef TAA_SELECTIVE
	float skip = GetSkipFlag(depth, view);

	if (skip > 0.0) {
		color = blur;
		return vec4(tempData, vec3(0.0));
	}
	#endif

	vec3 coord = vec3(texCoord, depth);
	vec2 prvCoord = Reprojection(coord, gbufferProjectionInverse, gbufferPreviousProjection, true);

	#if defined DISTANT_HORIZONS || defined VOXY
	if (depth < 1.0) {
	#endif
	#ifdef VOXY
	} else if (vxDepth < 1.0) {
		vec3 vxCoord = vec3(texCoord, vxDepth);
		prvCoord = Reprojection(vxCoord, vxProjInv, vxProjPrev, false);
	#endif
	#ifdef DISTANT_HORIZONS
	} else if (dhDepth < 1.0) {
		vec3 dhCoord = vec3(texCoord, dhDepth);
		prvCoord = Reprojection(dhCoord, dhProjectionInverse, dhPreviousProjection, false);
	#endif
	#if defined DISTANT_HORIZONS || defined VOXY
	}
	#endif
	
	vec3 tempColor = textureCatmullRom(colortex2, prvCoord, view);

	if(tempColor == vec3(0.0)) {
		color = blur;
		return vec4(tempData, color);
	}

	vec3 tempColorRaw = tempColor;
	tempColor = NeighbourhoodClipping(color, tempColor, 1.0 / view);
	
	#if TAA_MODE == 0
	vec2 velocity = (texCoord - prvCoord.xy) * view;
	float blendFactor = float(
		prvCoord.x > 0.0 && prvCoord.x < 1.0 &&
		prvCoord.y > 0.0 && prvCoord.y < 1.0
	);
	blendFactor *= exp(-length(velocity)) * 0.2 + 0.7;
	
	color = mix(color, tempColor, blendFactor);
	#endif

	vec3 outColor = color;
	
	#if TAA_MODE == 1
	color = mix(color, tempColor, 0.5);
	#endif

	return vec4(tempData, outColor);
}