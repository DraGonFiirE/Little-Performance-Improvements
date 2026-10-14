layout(lines) in;
layout(triangle_strip, max_vertices = 4) out;

in vec4 vertexColor[];
in vec3 vertexLocalPosition[];
out vec3 lineLocalPosition;
out vec4 lineColor;
noperspective out vec3 centerNdc;

uniform vec2 ScreenSize;
uniform float LineWidth;

void emitLineVertex(vec4 clipPosition, vec2 offset, vec4 color, vec3 viewPosition) {
    vec2 ndc = clipPosition.xy / clipPosition.w;
    gl_Position = vec4((ndc + offset) * clipPosition.w,
                       clipPosition.z,
                       clipPosition.w);
    centerNdc = clipPosition.xyz / clipPosition.w;
    lineLocalPosition = viewPosition;
    lineColor = color;
    EmitVertex();
}

void main() {
    vec4 start = gl_in[0].gl_Position;
    vec4 end = gl_in[1].gl_Position;
    vec3 startView=vertexLocalPosition[0],endView=vertexLocalPosition[1];

    // Clip against the OpenGL near plane before dividing by W. This prevents
    // a line crossing the camera from expanding into a screen-filling quad.
    float startDistance = start.z + start.w;
    float endDistance = end.z + end.w;
    if (startDistance < 0.0 && endDistance < 0.0) {
        return;
    }
    if (startDistance < 0.0) {
        float amount = startDistance / (startDistance - endDistance);
        start = mix(start, end, amount);
        startView=mix(startView,endView,amount);
    } else if (endDistance < 0.0) {
        float amount = endDistance / (endDistance - startDistance);
        end = mix(end, start, amount);
        endView=mix(endView,startView,amount);
    }

    vec2 startNdc = start.xy / start.w;
    vec2 endNdc = end.xy / end.w;
    vec2 screenDirection = (endNdc - startNdc) * ScreenSize;
    float directionLength = length(screenDirection);
    if (directionLength < 0.0001) {
        return;
    }

    vec2 perpendicular = vec2(-screenDirection.y, screenDirection.x)
                       / directionLength;
    // NDC spans two units across the framebuffer. Offset each side by the
    // requested half-width in pixels, including that factor of two, so the
    // finished strip matches native OpenGL's configured line width.
    vec2 offset = perpendicular * LineWidth / ScreenSize;

    emitLineVertex(start, offset, vertexColor[0],startView);
    emitLineVertex(start, -offset, vertexColor[0],startView);
    emitLineVertex(end, offset, vertexColor[1],endView);
    emitLineVertex(end, -offset, vertexColor[1],endView);
    EndPrimitive();
}
