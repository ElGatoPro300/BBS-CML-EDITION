vec3 GetRandomEmissionHSV(vec3 pos, uint data) {
	float rand = fract(pos.x * 0.618 + pos.y * 0.414 + pos.z * 0.707);
	float s = (data >= 11 && data <= 24) || (data >= 36 && data <= 49) ? 0.33 : 0.75;
	float v = data > 25 ? 1.0 : 4.0;
	return vec3(rand, s, v);
}

vec3 GetEmission(uint data) {
	vec3 emission = vec3(0.0);
	if (data >= 1 && data <= 50) {
		uint colorIndex = data - 1;
		emission = lightColorsRGB[colorIndex];
	}
	return emission;
}

vec3 GetTint(uint data) {
	vec3 tint = vec3(1.0);
	if (data >= 51 && data <= 75) {
		uint colorIndex = data - 51;
		tint = tintColorsRGB[colorIndex];
	}
	return tint;
}