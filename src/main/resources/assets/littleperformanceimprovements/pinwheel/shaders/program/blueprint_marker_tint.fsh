in vec3 localPosition;
in vec4 vertexColor;
uniform vec4 ColorModulator;
uniform samplerBuffer MarkerData;
uniform int MarkerCount;
out vec4 fragColor;
void main() {
    vec3 tint = ColorModulator.rgb;
    for (int i = 0; i < MarkerCount; ++i) {
        vec3 low = texelFetch(MarkerData, i * 3).xyz;
        vec3 high = texelFetch(MarkerData, i * 3 + 1).xyz;
        if (all(greaterThanEqual(localPosition, low - vec3(0.000001))) &&
            all(lessThanEqual(localPosition, high + vec3(0.000001)))) {
            tint = texelFetch(MarkerData, i * 3 + 2).rgb;
            break;
        }
    }
    fragColor = vec4(vertexColor.rgb * tint, vertexColor.a * ColorModulator.a);
}
