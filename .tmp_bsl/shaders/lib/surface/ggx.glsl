//GGX area light approximation from Decima Engine: Advances in Lighting and AA presentation
float GetNoHSquared(float radiusTan, float NoL, float NoV, float VoL) {
    float radiusCos = 1.0 / sqrt(1.0 + radiusTan * radiusTan);
    
    float RoL = 2.0 * NoL * NoV - VoL;
    if (RoL >= radiusCos)
        return 1.0;

    float rOverLengthT = radiusCos * radiusTan / sqrt(1.0 - RoL * RoL);
    float NoTr = rOverLengthT * (NoV - RoL * NoL);
    float VoTr = rOverLengthT * (2.0 * NoV * NoV - 1.0 - RoL * VoL);

    float triple = sqrt(clamp(1.0 - NoL * NoL - NoV * NoV - VoL * VoL + 2.0 * NoL * NoV * VoL, 0.0, 1.0));
    
    float NoBr = rOverLengthT * triple, VoBr = rOverLengthT * (2.0 * triple * NoV);
    float NoLVTr = NoL * radiusCos + NoV + NoTr, VoLVTr = VoL * radiusCos + 1.0 + VoTr;
    float p = NoBr * VoLVTr, q = NoLVTr * VoLVTr, s = VoBr * NoLVTr;    
    float xNum = q * (-0.5 * p + 0.25 * VoBr * NoLVTr);
    float xDenom = p * p + s * ((s - 2.0 * p)) + NoLVTr * ((NoL * radiusCos + NoV) * VoLVTr * VoLVTr + 
                   q * (-0.5 * (VoLVTr + VoL * radiusCos) - 0.5));
    float twoX1 = 2.0 * xNum / (xDenom * xDenom + xNum * xNum);
    float sinTheta = twoX1 * xDenom;
    float cosTheta = 1.0 - twoX1 * xNum;
    NoTr = cosTheta * NoTr + sinTheta * NoBr;
    VoTr = cosTheta * VoTr + sinTheta * VoBr;
    
    float newNoL = NoL * radiusCos + NoTr;
    float newVoL = VoL * radiusCos + VoTr;
    float NoH = NoV + newNoL;
    float HoH = 2.0 * newVoL + 2.0;

    float NoHsqr = clamp(NoH * NoH / HoH, 0.0, 1.0);

    return NoHsqr;
}

float GGXTrowbridgeReitz(float NoHsqr, float roughness){
    float roughnessSqr = roughness * roughness;
    float distr = NoHsqr * (roughnessSqr - 1.0) + 1.0;
    return roughnessSqr / (3.14159 * distr * distr);
}

float BSLSquarePhong(float sunSize, vec3 normal, vec3 lightVec, vec3 viewPos, float roughness) {
    viewPos = reflect(viewPos, normal);

    const vec2 sunRotationData = vec2(cos(sunPathRotation * 0.01745329251994), -sin(sunPathRotation * 0.01745329251994));
    float ang = fract(timeAngle + 0.0001 - 0.25);
    ang = (ang + (cos(ang * 3.14159265358979) * -0.5 + 0.5 - ang) / 3.0) * 6.28318530717959;

    vec3 nextSunVec = normalize((gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);
    vec3 sunTangent = normalize(nextSunVec - sunVec);
    vec3 sunBinormal = -cross(sunVec, sunTangent);

    float VoL = dot(-viewPos, lightVec);
    float VoLt = dot(viewPos, sunTangent);
    float VoLb = dot(viewPos, sunBinormal);

    vec2 sdfCoord = abs(vec2(VoLt, VoLb) / sunSize * 1.667) - 1.0;
    float squareSDF = length(max(sdfCoord, 0.0));

    float NoHsqr = max(1.0 - pow(squareSDF * sunSize, 2.0), 0.0) * step(0.0, VoL);
    
    float roughnessSqr = roughness * roughness;
    float distr = NoHsqr * (roughnessSqr - 1.0) + 1.0;
    return roughnessSqr / (3.14159 * distr * distr);
}

float SchlickGGX(float NoL, float NoV, float roughness){
    float k = roughness * 0.5;
    
    float smithL = 0.5 / (NoL * (1.0 - k) + k);
    float smithV = 0.5 / (NoV * (1.0 - k) + k);

	return smithL * smithV;
}

vec3 SphericalGaussianFresnel(float HoL, vec3 baseReflectance){
    float fresnel = exp2(((-5.55473 * HoL) - 6.98316) * HoL);
    return fresnel * (1.0 - baseReflectance) + baseReflectance;
}

vec3 GGX(vec3 normal, vec3 viewPos, float smoothness, vec3 baseReflectance, float sunSize) {
    float roughness = max(1.0 - smoothness, 0.025); roughness *= roughness;
    viewPos = -viewPos;
    
    vec3 halfVec = normalize(lightVec + viewPos);

    float HoL = clamp(dot(halfVec, lightVec), 0.0, 1.0);
    float NoL = clamp(dot(normal,  lightVec), 0.0, 1.0);
    float NoV = clamp(dot(normal,  viewPos), -1.0, 1.0);
    float VoL = dot(lightVec, viewPos);

    float NoHsqr = GetNoHSquared(sunSize, NoL, NoV, VoL);
    if (NoV < 0.0){
        NoHsqr = dot(normal, halfVec);
        NoHsqr *= NoHsqr;
    }
    NoV = max(NoV, 0.0);
    
    #if SHADER_SUN_MOON_SHAPE == 0
    float D = GGXTrowbridgeReitz(NoHsqr, roughness);
    #else
    float D = BSLSquarePhong(sunSize, normal, lightVec, viewPos, roughness);
    #endif
    vec3  F = SphericalGaussianFresnel(HoL, baseReflectance);
    float G = SchlickGGX(NoL, NoV, roughness);
    
    float Fl = max(length(F), 0.001);
    vec3  Fn = F / Fl;

    float specular = D * Fl * G;
    vec3 specular3 = specular / (1.0 + 0.03125 / 4.0 * specular) * Fn * NoL;

    #ifndef SPECULAR_HIGHLIGHT_ROUGH
    specular3 *= 1.0 - roughness * roughness;
    #endif

    return specular3;
}

vec3 GetSpecularHighlight(vec3 normal, vec3 viewPos, float smoothness, vec3 baseReflectance,
                          vec3 specularColor, vec3 shadow, float smoothLighting) {
    if (dot(shadow, shadow) < 0.00001) return vec3(0.0);
    #ifndef SPECULAR_HIGHLIGHT_ROUGH
    if (smoothness < 0.00002) return vec3(0.0);
    #endif

    smoothLighting *= smoothLighting;

    #ifdef END
    smoothness *= 0.75;
    #endif
    
    vec3 specular = GGX(normal, normalize(viewPos), smoothness, baseReflectance,
                        (0.025 * sunVisibility + 0.05) * SHADER_SUN_MOON_SIZE);
    specular *= shadow * shadowFade * smoothLighting;
    specular *= (1.0 - rainStrength) * (1.0 - rainStrength);
    
    return specular * specularColor;
}