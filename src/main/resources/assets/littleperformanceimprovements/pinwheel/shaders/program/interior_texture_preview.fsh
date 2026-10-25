in vec2 texCoord0;
in float viewDepth;
in vec4 vertexColor;

uniform sampler2D Sampler0;
uniform sampler2D SceneDepth;
uniform vec4 ColorModulator;
uniform float NearClipDistance;
uniform int DeduplicatePass;
uniform int PerTilePulse;
uniform int HighlightThinFeatures;

out vec4 fragColor;

void main() {
    float sceneDepth = texelFetch(SceneDepth, ivec2(gl_FragCoord.xy), 0).r;
    // Match the slope-scaled raster depth bias when checking world depth
    // manually. A fixed epsilon alone fails on grazing-angle surfaces.
    float slope = max(abs(dFdx(gl_FragCoord.z)), abs(dFdy(gl_FragCoord.z)));
    if (gl_FragCoord.z - slope > sceneDepth + 0.0000001) {
        discard;
    }
    if (viewDepth < NearClipDistance) {
        discard;
    }

    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.01) {
        discard;
    }

    // vertexColor is LittleTiles' baked per-element tint/shading. It is not the
    // configurable previewColor: Texture mode deliberately applies only the
    // material color carried by the transformed baked quad.
    fragColor = vec4(
        texel.rgb * vertexColor.rgb,
        texel.a * vertexColor.a * ColorModulator.a);
}
