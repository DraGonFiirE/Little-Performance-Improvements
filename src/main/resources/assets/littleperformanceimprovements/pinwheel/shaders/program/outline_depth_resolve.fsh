uniform usampler2D OutlineSurface;
uniform samplerBuffer OutlineVertices;
uniform sampler2D OutlineReverseDepth;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ReverseDepthScale;
uniform float OutlineWidth;
out vec4 fragColor;
void main(){
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    uint encoded=texelFetch(OutlineSurface,pixel,0).r;
    if(encoded==0u)discard;
    float reversed=texelFetch(OutlineReverseDepth,pixel,0).r;
    float viewZ=ReverseDepthScale>0.0?-ReverseDepthScale/reversed:1.0-1.0/reversed;
    vec4 clip=ProjMat*vec4(0.0,0.0,viewZ,1.0);
    float depth=clip.z/clip.w*0.5+0.5;

    // Reproduce the original polygon offset using this triangle's own plane,
    // avoiding derivatives across a silhouette or two different surfaces.
    int triangle=int(encoded-1u),base=(triangle/2)*4;
    ivec3 index=(triangle%2==0)?ivec3(0,1,2):ivec3(2,3,0);
    vec3 a=texelFetch(OutlineVertices,base+index.x).xyz;
    vec3 b=texelFetch(OutlineVertices,base+index.y).xyz;
    vec3 c=texelFetch(OutlineVertices,base+index.z).xyz;
    vec3 n=transpose(inverse(mat3(ModelViewMat)))*cross(b-a,c-a);
    vec3 viewA=(ModelViewMat*vec4(a,1.0)).xyz;
    float plane=dot(n,viewA);
    vec2 size=vec2(textureSize(OutlineSurface,0));
    vec2 slope=abs(ProjMat[3][2]*n.xy/(size*vec2(ProjMat[0][0],ProjMat[1][1])*plane));
    // One additional representable step covers the rounding in the depth
    // transfer; the exact triangle check still rejects occluded centerlines.
    gl_FragDepth=depth+max(slope.x,slope.y)*max(1.0,OutlineWidth*0.5+0.5)+0.00000011920929;
    fragColor=vec4(0.0);
}
