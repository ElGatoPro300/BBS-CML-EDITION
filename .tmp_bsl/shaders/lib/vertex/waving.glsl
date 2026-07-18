const float pi = 3.1415927;
float pi2wt = 6.2831854 * (time * 24.0);

float GetNoise(vec2 pos) {
	return fract(sin(dot(pos, vec2(12.9898, 4.1414))) * 43758.5453);
}

float Noise2D(vec2 pos) {
    vec2 flr = floor(pos);
    vec2 frc = fract(pos);
    frc = frc * frc * (3.0 - 2.0 * frc);

    float n00 = GetNoise(flr);
    float n01 = GetNoise(flr + vec2(0.0, 1.0));
    float n10 = GetNoise(flr + vec2(1.0, 0.0));
    float n11 = GetNoise(flr + vec2(1.0, 1.0));

    float n0 = mix(n00, n01, frc.y);
    float n1 = mix(n10, n11, frc.y);

    return mix(n0, n1, frc.x) - 0.5;
}

vec3 CalcMove(vec3 pos, float density, float speed, vec2 mult) {
    pos = pos * density + time * speed;
    mult *= ANIMATION_STRENGTH;
    vec3 wave = vec3(Noise2D(pos.yz), Noise2D(pos.xz + 0.333), Noise2D(pos.xy + 0.667));
    return wave * vec3(mult, mult.x);
}

float CalcLilypadMove(vec3 worldPos) {
    float wave = sin(2 * pi * (time * 0.7 + worldPos.x * 0.14 + worldPos.z * 0.07)) +
                 sin(2 * pi * (time * 0.5 + worldPos.x * 0.10 + worldPos.z * 0.20));
    return wave * 0.0125 * ANIMATION_STRENGTH;
}

float CalcLavaMove(vec3 worldPos) {
    float fy = fract(worldPos.y + 0.005);
		
    if (fy > 0.01) {
    float wave = sin(pi * (time * 0.7 + worldPos.x * 0.14 + worldPos.z * 0.07)) +
                 sin(pi * (time * 0.5 + worldPos.x * 0.10 + worldPos.z * 0.20));
    return wave * 0.0125 * ANIMATION_STRENGTH;
    } else return 0.0;
}

vec3 CalcLanternMove(vec3 position) {
    vec3 frc = fract(position);
    frc = vec3(frc.x - 0.5, fract(frc.y - 0.001) - 1.0, frc.z - 0.5);
    vec3 flr = position - frc;
    float offset = flr.x * 2.4 + flr.y * 2.7 + flr.z * 2.2;

    float rmult = pi * 0.016;
    float rx = sin(time       + offset) * rmult;
    float ry = sin(time * 1.7 + offset) * rmult;
    float rz = sin(time * 1.4 + offset) * rmult;
    mat3 rotx = mat3(
               1,        0,        0,
               0,  cos(rx), -sin(rx),
               0,  sin(rx),  cos(rx)
    );
    mat3 roty = mat3(
         cos(ry),        0,  sin(ry),
               0,        1,        0,
        -sin(ry),        0,  cos(ry)
    );
    mat3 rotz = mat3(
         cos(rz), -sin(rz),        0,
         sin(rz),  cos(rz),        0,
               0,        0,        1
    );
    frc = rotx * roty * rotz * frc;
    
    return flr + frc - position;
}

vec3 CalcGrassBend(vec3 position) {
    position += relativeEyePosition + vec3(0.0, 0.62, 0.0);

    vec3 positionMod = position * vec3(4.0, 2.0, 4.0);
    float positionModLength = length(positionMod);
    
    vec3 bendAmount = vec3(1.0, 0.25, 1.0) * max(2.0 / max(positionModLength, 1.0) - 0.35, 0.0);
    
    return position * bendAmount;
}

vec3 WavingBlocks(vec3 position, int blockID, float istopv) {
    vec3 wave = vec3(0.0);
    vec3 worldPos = position + cameraPosition;

    #ifdef WAVING_GRASS
    if (blockID == 100 && istopv > 0.9) {
        wave += CalcMove(worldPos, 0.35, 1.0, vec2(0.25, 0.06));
        wave += CalcGrassBend(position);
    }
    #endif

    #ifdef WAVING_CROP
    if (blockID == 104 && (istopv > 0.9 || fract(worldPos.y + 0.0675) > 0.01)) {
        wave += CalcMove(worldPos, 0.35, 1.0, vec2(0.15, 0.06));
        wave += CalcGrassBend(position);
    }
    #endif

    #ifdef WAVING_PLANT
    if (blockID == 101 && (istopv > 0.9 || fract(worldPos.y + 0.005) > 0.01)) {
        wave += CalcMove(worldPos, 0.7, 1.35, vec2(0.12, 0.00));
        wave += CalcGrassBend(position);
    }
    if (blockID == 107 || blockID == 157)
        wave += CalcMove(worldPos, 0.5, 1.25, vec2(0.06, 0.00));
    if (blockID == 108)
        wave.y += CalcLilypadMove(worldPos);
    #endif

    #ifdef WAVING_TALL_PLANT
    if (blockID == 102 && (istopv > 0.9 || fract(worldPos.y + 0.005) > 0.01) ||
        blockID == 103)
        wave += CalcMove(worldPos, 0.35, 1.15, vec2(0.15, 0.06));
    #endif

    #ifdef WAVING_LEAF
    if (blockID == 105)
        wave += CalcMove(worldPos, 0.25, 1.0, vec2(0.08, 0.08));
    #endif

    #ifdef WAVING_VINE
    if (blockID == 106)
        wave += CalcMove(worldPos, 0.35, 1.25, vec2(0.06, 0.06));     
    #endif
    
    #ifdef WAVING_LAVA
    if (blockID == 153)
        wave.y += CalcLavaMove(worldPos);
    #endif

    #ifdef WAVING_FIRE
    if (blockID == 154 && istopv > 0.9)
        wave += CalcMove(worldPos, 2.0, 3.0, vec2(0.0, 1.0));
    #endif

    #ifdef WAVING_LANTERN
    if (blockID == 156)
		wave += CalcLanternMove(worldPos);
    #endif

    position += wave;

    return position;
}