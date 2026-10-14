in vec2 texCoord;

uniform sampler2D PreviewTexture;

out vec4 fragColor;

void main() {
    vec4 preview = texture(PreviewTexture, texCoord);
    if (preview.a <= 0.0) {
        discard;
    }
    fragColor = preview;
}
