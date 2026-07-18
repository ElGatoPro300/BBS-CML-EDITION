float WorldCurvature(vec2 pos) {
    #ifndef VOXY
    return dot(pos, pos) / WORLD_CURVATURE_SIZE;
    #else
    return 0.0;
    #endif
}