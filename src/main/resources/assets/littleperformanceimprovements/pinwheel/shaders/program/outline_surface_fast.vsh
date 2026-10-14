in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ReverseDepthScale;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    // GL_ZERO_TO_ONE lets hardware rasterize reverse depth directly, preserving
    // its precision and early depth rejection without a geometry shader.
    gl_Position.z=ReverseDepthScale;
}
