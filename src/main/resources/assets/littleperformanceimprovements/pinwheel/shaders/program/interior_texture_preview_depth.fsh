in vec2 texCoord0;
in float viewDepth;
in vec4 vertexColor;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float NearClipDistance;
uniform int DeduplicatePass;
uniform int PerTilePulse;
uniform int HighlightThinFeatures;

out vec4 fragColor;

void main() {
    if (viewDepth < NearClipDistance) {
        discard;
    }

    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.01) {
        discard;
    }

    fragColor = vec4(
        texel.rgb * vertexColor.rgb,
        texel.a * vertexColor.a * ColorModulator.a);
}
