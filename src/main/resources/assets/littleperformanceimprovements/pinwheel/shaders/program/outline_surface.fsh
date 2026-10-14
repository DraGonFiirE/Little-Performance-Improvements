in vec3 vertexViewPosition;
out uint fragSurface;
uniform uint OutlineTriangleStep;
void main(){
    fragSurface=uint(gl_PrimitiveID)*max(OutlineTriangleStep,1u)+1u;
    gl_FragDepth=1.0/(1.0+max(-vertexViewPosition.z,0.0));
}
