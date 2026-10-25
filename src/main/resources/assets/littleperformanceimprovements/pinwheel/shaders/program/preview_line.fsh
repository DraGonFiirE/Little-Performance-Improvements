in vec4 lineColor;
in vec3 lineLocalPosition;
noperspective in vec3 centerNdc;
noperspective in float lineDistance;
uniform int HammerStyle;
uniform vec4 ColorModulator;
uniform usampler2D OutlineSurface;
uniform samplerBuffer OutlineVertices;
uniform vec3 OutlineCamera;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 fragColor;

// The surface pass uses Minecraft's sequential QUADS index buffer:
// (0,1,2), (2,3,0). The texture buffer aliases the original POSITION_COLOR VBO.
bool occludes(uint encoded, bool footprint) {
    if(encoded==0u)return false;
    int triangle=int(encoded-1u);
    int base=(triangle/2)*4;
    ivec3 index=(triangle%2==0)?ivec3(0,1,2):ivec3(2,3,0);
    vec3 a=texelFetch(OutlineVertices,base+index.x).xyz;
    vec3 b=texelFetch(OutlineVertices,base+index.y).xyz;
    vec3 c=texelFetch(OutlineVertices,base+index.z).xyz;
    vec3 e1=b-a,e2=c-a,n=cross(e1,e2);
    float lengthN=length(n);
    if(lengthN<1e-12)return false;
    n/=lengthN;
    vec3 ray=lineLocalPosition-OutlineCamera;
    float denominator=dot(n,ray);
    if(abs(denominator)<1e-9)return false;
    // Measure the separation in model coordinates; it remains precise as
    // camera distance increases. True edges on this triangle have zero gap.
    float gap=dot(n,lineLocalPosition-a);
    if(denominator<0.0)gap=-gap;
    if(gap<=0.00002)return false;
    float t=dot(n,a-OutlineCamera)/denominator;
    if(t<=0.0||t>=1.0)return false;
    vec3 hit=OutlineCamera+ray*t;
    // Verify actual triangle coverage instead of extrapolating a neighboring
    // pixel's plane across silhouettes and sharp corners.
    vec3 relative=hit-a;
    float u=dot(cross(relative,e2),n)/lengthN;
    float v=dot(cross(e1,relative),n)/lengthN;
    if(u>=-0.00001&&v>=-0.00001&&u+v<=1.00001)return true;
    if(!footprint)return false;
    // A sampled triangle represents a pixel footprint, not an infinitesimal
    // point. Bound extrapolation to half a pixel so distant subpixel facets
    // do not punch holes in otherwise continuous occluding surfaces.
    vec4 ca=ProjMat*ModelViewMat*vec4(a,1.0);
    vec4 cb=ProjMat*ModelViewMat*vec4(b,1.0);
    vec4 cc=ProjMat*ModelViewMat*vec4(c,1.0);
    if(min(ca.w,min(cb.w,cc.w))<=0.0)return false;
    vec2 scale=vec2(textureSize(OutlineSurface,0))*0.5;
    vec2 pa=ca.xy/ca.w*scale,pb=cb.xy/cb.w*scale,pc=cc.xy/cc.w*scale;
    vec2 point=centerNdc.xy*scale;
    vec2 ab=pb-pa,bc=pc-pb,ac=pa-pc;
    float orient=sign(ab.x*(pc.y-pa.y)-ab.y*(pc.x-pa.x));
    if(orient==0.0)return false;
    vec2 da=point-pa,db=point-pb,dc=point-pc;
    return orient*(ab.x*da.y-ab.y*da.x)>=-0.5*(abs(ab.x)+abs(ab.y))
        &&orient*(bc.x*db.y-bc.y*db.x)>=-0.5*(abs(bc.x)+abs(bc.y))
        &&orient*(ac.x*dc.y-ac.y*dc.x)>=-0.5*(abs(ac.x)+abs(ac.y));
}
vec4 planeFor(uint encoded){
 int triangle=int(encoded-1u),base=(triangle/2)*4;
 ivec3 index=(triangle%2==0)?ivec3(0,1,2):ivec3(2,3,0);
 vec3 a=texelFetch(OutlineVertices,base+index.x).xyz;
 vec3 n=cross(texelFetch(OutlineVertices,base+index.y).xyz-a,texelFetch(OutlineVertices,base+index.z).xyz-a);
 float len=length(n);if(len<1e-12)return vec4(0);n/=len;
 return vec4(n,dot(n,a));
}
void main(){
 if(HammerStyle!=0){
  if(HammerStyle==2&&mod(lineDistance,12.0)>=6.0)discard;
  fragColor=lineColor*ColorModulator;
  return;
 }
 ivec2 size=textureSize(OutlineSurface,0);
 vec2 pixel=(centerNdc.xy*0.5+0.5)*vec2(size)-0.5;
 ivec2 base=ivec2(floor(pixel));uint ids[4];
 for(int y=0;y<2;y++)for(int x=0;x<2;x++){
  uint id=texelFetch(OutlineSurface,clamp(base+ivec2(x,y),ivec2(0),size-1),0).r;ids[y*2+x]=id;
  if(occludes(id,false))discard;
 }
 bool samePlane=ids[0]!=0u;bool distinct=false;vec4 plane=ids[0]!=0u?planeFor(ids[0]):vec4(0);
 for(int i=1;i<4&&samePlane;i++){
  if(ids[i]==0u){samePlane=false;break;}if(ids[i]==ids[0])continue;
  distinct=true;vec4 other=planeFor(ids[i]);if(dot(plane.xyz,other.xyz)<0.0)other=-other;
  samePlane=dot(plane.xyz,other.xyz)>0.999999&&abs(plane.w-other.w)<0.00002;
 }
 if(samePlane&&distinct){for(int i=0;i<4;i++)if(occludes(ids[i],true))discard;}
 fragColor=lineColor*ColorModulator;
}
