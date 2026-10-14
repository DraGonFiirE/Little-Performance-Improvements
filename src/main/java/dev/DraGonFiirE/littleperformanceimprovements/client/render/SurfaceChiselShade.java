package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.nio.*;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
/** Bake the blueprint Surface shader's orientation shade into chisel meshes once. */
public final class SurfaceChiselShade {
 public static PreviewRenderer.BoxRenderResult apply(PreviewRenderer.BoxRenderResult result){
  if(result==null||result.data()==null)return result;
  var mesh=result.data();var state=mesh.drawState();
  if(state.format()!=DefaultVertexFormat.POSITION_COLOR||state.mode()!=VertexFormat.Mode.QUADS)return result;
  shade(mesh.vertexBuffer().duplicate().order(ByteOrder.nativeOrder()),state.vertexCount());return result;
 }
 static void shade(ByteBuffer b,int count){
  for(int at=0;at+4<=count;at+=4){
   int base=at*16;double nx=0,ny=0,nz=0,best=0;
   // Padded fan triangles may duplicate a vertex. Pick the strongest triangle.
   for(int i=1;i<3;i++){
    int a=base+i*16,c=base+(i+1)*16;
    double ux=(double)b.getFloat(a)-b.getFloat(base),uy=(double)b.getFloat(a+4)-b.getFloat(base+4),uz=(double)b.getFloat(a+8)-b.getFloat(base+8);
    double vx=(double)b.getFloat(c)-b.getFloat(base),vy=(double)b.getFloat(c+4)-b.getFloat(base+4),vz=(double)b.getFloat(c+8)-b.getFloat(base+8);
    double x=uy*vz-uz*vy,y=uz*vx-ux*vz,z=ux*vy-uy*vx,len=x*x+y*y+z*z;
    if(len>best){best=len;nx=x;ny=y;nz=z;}
   }
   if(!(best>0)||!Double.isFinite(best))continue;
   double orientation=Math.min(1,(Math.abs(nx)*.20+Math.abs(ny)*.43+Math.abs(nz)*.72)/Math.sqrt(best));
   double shade=.58+.34*orientation;
   for(int v=0;v<4;v++)for(int c=12;c<15;c++){int offset=base+v*16+c;b.put(offset,(byte)Math.round((b.get(offset)&255)*shade));}
  }
 }
}
