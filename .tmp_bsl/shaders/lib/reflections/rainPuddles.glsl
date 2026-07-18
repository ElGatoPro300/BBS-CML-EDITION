float GetPuddles(vec3 worldPos, vec2 coord, float skylight, float NoU, float wetness) {
    if (wetness < 0.001) return 0.0;

	worldPos = (worldPos + cameraPosition) * 0.005;

    float height = texture2DGradARB(normals, coord, dcdx, dcdy).a;
    height = mix(1.0, height, PARALLAX_DEPTH);
    height = smoothstep(1.0, 0.95, height) * 0.1 - 0.05;
    float noise = texture2D(noisetex, worldPos.xz * 0.5  ).r * 0.375;
		  noise+= texture2D(noisetex, worldPos.xz * 0.125).r * 0.625;
		  noise = noise + height + (wetness * REFLECTION_RAIN_AMOUNT - 0.5);

    float puddles = smoothstep(0.4, 0.6, noise);
    
    #ifdef WEATHER_PERBIOME
    float weatherWeight = isCold + isDesert + isMesa + isSavanna;
    puddles *= 1.0 - weatherWeight;
    #endif
    
    puddles *= clamp(skylight * 32.0 - 31.0, 0.0, 1.0) * clamp(NoU, 0.0, 1.0);

	return puddles;
}

float rand(vec2 worldPos){
	return fract(sin(dot(worldPos, vec2(12.9898, 4.1414))) * 43758.5453);
}

vec2 getpos(vec2 i){
    return vec2(rand(i), rand(i + 1.0)) * 0.5 + 0.25;
}

float GetRipple(vec3 worldPos, vec2 offset) {
	vec2 ppos = worldPos.xz + offset * 0.1 + time * 0.01;
    ppos = vec2(ppos.x * 0.7 + ppos.y * 0.7, ppos.x * -0.7 +  ppos.y * 0.7) * 0.8;
    vec2 ppossh = ppos + vec2(fract(0.618 * floor(ppos.y)) * sin(time * 0.05), 0.0);
    vec2 pposfr = fract(ppossh);
    vec2 pposfl = floor(ppossh);
	
	float val = texture2D(noisetex, ppos / 64.0 + time * 0.007).r * 0.125;
	val += texture2D(noisetex, ppos / 64.0 - time * 0.005).r * 0.125;

    float seed = rand(pposfl);
    float rippleTime = time * 1.7 + fract(seed * 1.618);
    float rippleSeed = seed + floor(rippleTime) * 1.618;
    vec2 ripplePos = getpos(pposfl + rippleSeed);
    float ripple = clamp(1.0 - 4.0 * length(pposfr - ripplePos), 0.0, 1.0);
    ripple = clamp(ripple + fract(rippleTime) - 1.0, 0.0, 1.0);
    ripple = sin(min(ripple * 6.0 * 3.1415, 3.0 * 3.1415)) * pow(1.0 - fract(rippleTime), 2.0);
    val += ripple * 0.3;

    //if(pposfr.x < 0.01 || pposfr.y < 0.01) val += 0.85;

	return val;
}

vec3 GetPuddleNormal(vec3 worldPos, vec3 viewPos, mat3 tbn) {
    vec3 puddlePos = worldPos + cameraPosition;
    float normalOffset = 0.1;

	float fresnel = pow(clamp(1.0 + dot(normalize(normal), normalize(viewPos)), 0.0, 1.0), 7.5);
	float normalStrength = 0.35 * (1.0 - fresnel);
    
    float h1 = GetRipple(puddlePos, vec2( normalOffset, 0.0));
    float h2 = GetRipple(puddlePos, vec2(-normalOffset, 0.0));
    float h3 = GetRipple(puddlePos, vec2(0.0,  normalOffset));
    float h4 = GetRipple(puddlePos, vec2(0.0, -normalOffset));
    
    float xDelta = (h2 - h1) / normalOffset;
    float yDelta = (h4 - h3) / normalOffset;

	vec3 normalMap = vec3(xDelta, yDelta, 1.0 - (xDelta * xDelta + yDelta * yDelta));
	normalMap = normalMap * normalStrength + vec3(0.0, 0.0, 1.0 - normalStrength);

    return clamp(normalize(normalMap * tbn), vec3(-1.0), vec3(1.0));
}

void ApplyPuddleToMaterial(float puddles, inout vec4 albedo, inout float smoothness, inout float f0, float porosity) {
    float puddleSmoothness = sqrt(1.0 - 0.75 * porosity);
    float puddleDarkening = (0.5 * porosity + 0.15);	
    
    smoothness = mix(smoothness, 1.0, puddles * puddleSmoothness);
    f0 = max(f0, puddles * 0.02);

    albedo.rgb *= 1.0 - (puddles * puddleDarkening);
}