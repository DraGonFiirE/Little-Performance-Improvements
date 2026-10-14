out uint fragSurface;
uniform uint OutlineTriangleStep;
void main(){fragSurface=uint(gl_PrimitiveID)*max(OutlineTriangleStep,1u)+1u;}
