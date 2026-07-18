//Dithering from Jodie
float Bayer2(vec2 a) {
    a = floor(a);
    return fract(a.x * 0.5 + a.y * a.y * 0.75);
}

#define Bayer4(a) (Bayer2(a * 0.5) * 0.25 + Bayer2(a))
#define Bayer8(a) (Bayer4(a * 0.5) * 0.25 + Bayer2(a))

float BayerCloud2(vec2 a) {
    a = floor(a);
    return fract(a.x * 0.5 + a.y * 0.75);
}

#define BayerCloud4(a) (BayerCloud2(a * 0.5) * 0.25 + BayerCloud2(a))
#define BayerCloud8(a) (BayerCloud4(a * 0.5) * 0.25 + BayerCloud2(a))