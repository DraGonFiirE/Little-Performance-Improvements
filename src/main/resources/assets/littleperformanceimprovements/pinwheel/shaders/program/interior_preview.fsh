in vec4 vertexColor;
in vec3 viewPosition;
in vec3 localPosition;

uniform vec4 ColorModulator;
uniform sampler2D SceneDepth;
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
    // OpenGL view space looks down -Z. Removing only the shallow view-depth
    // band clears a face crossing the camera without erasing nearby cabin
    // walls merely because they are close laterally.
    if (-viewPosition.z < NearClipDistance) {
        discard;
    }
    // Front/back ordering is handled by two fixed-function depth passes in
    // VeilInteriorPreviewRenderer. Keeping gl_FragDepth untouched preserves
    // early depth testing and works across NVIDIA's stricter GLSL compiler.
    float alpha = vertexColor.a * ColorModulator.a;
    vec3 color = vertexColor.rgb * ColorModulator.rgb;
    if (PerTilePulse != 0) {
        // Shade by the actual face orientation in blueprint-local space.
        // Keep it continuous across adjacent tiles: Surface mode previews the
        // hull, not the ownership of every individual LittleTiles element.
        vec3 areaNormal = cross(dFdx(localPosition), dFdy(localPosition));
        vec3 normal = areaNormal / max(length(areaNormal), 0.000001);
        vec3 axis = abs(normal);
        float orientation = clamp(dot(axis, vec3(0.20, 0.43, 0.72)), 0.0, 1.0);
        float shade = mix(0.58, 0.92, orientation);

        // Directional shading belongs in RGB. Scaling alpha here meant that a
        // configured previewSurfaceAlpha of 1.0 still produced translucent
        // faces (between 0.90 and 1.0 depending on their orientation).
        alpha = ColorModulator.a;
        color *= shade;

        // The exact mesh reserves the highest encoded alpha for shallow
        // ordinary tiles (128 rather than at most 127 after PreviewRenderer's
        // configured 0.5 alpha). Give those ledges a stable tonal contrast so
        // they remain readable head-on without outlines or animation.
        if (HighlightThinFeatures != 0 && vertexColor.a > 0.5) {
            color *= 0.62;
            alpha = min(1.0, alpha * 1.12);
        }
    }
    fragColor = vec4(color, alpha);
}
