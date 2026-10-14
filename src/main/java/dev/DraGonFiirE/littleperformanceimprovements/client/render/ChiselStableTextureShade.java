package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.client.renderer.block.model.BakedQuad;
public final class ChiselStableTextureShade {
 public static float shade(BakedQuad quad,int[] vertices,int stride){
  if(!quad.isShade()||stride<3)return 1;
  // Triangles are padded to four vertices; the first three can be collinear or repeated.
  // Use the largest valid triangle, in double precision, with no size-dependent cutoff.
  double best=0,nx=0,ny=0,nz=0;
  for(int a=0;a<2;a++)for(int b=a+1;b<3;b++)for(int c=b+1;c<4;c++){
   double ax=Float.intBitsToFloat(vertices[a*stride]),ay=Float.intBitsToFloat(vertices[a*stride+1]),az=Float.intBitsToFloat(vertices[a*stride+2]);
   double ux=Float.intBitsToFloat(vertices[b*stride])-ax,uy=Float.intBitsToFloat(vertices[b*stride+1])-ay,uz=Float.intBitsToFloat(vertices[b*stride+2])-az;
   double vx=Float.intBitsToFloat(vertices[c*stride])-ax,vy=Float.intBitsToFloat(vertices[c*stride+1])-ay,vz=Float.intBitsToFloat(vertices[c*stride+2])-az;
   double x=uy*vz-uz*vy,y=uz*vx-ux*vz,z=ux*vy-uy*vx,size=x*x+y*y+z*z;
   if(size>best){best=size;nx=x;ny=y;nz=z;}
  }
  if(best>0){double x=Math.abs(nx),y=Math.abs(ny),z=Math.abs(nz);return (float)((x*.6+y+z*.8)/(x+y+z));}
  var axis=quad.getDirection().getAxis();return axis==net.minecraft.core.Direction.Axis.X?.6f:axis==net.minecraft.core.Direction.Axis.Z?.8f:1f;
 }
}
