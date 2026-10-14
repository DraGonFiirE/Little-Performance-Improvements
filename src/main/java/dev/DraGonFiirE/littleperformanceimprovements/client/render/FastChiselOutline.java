package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
public final class FastChiselOutline {
 private static final ThreadLocal<PreviewRenderer.BoxRenderResult> prepared=new ThreadLocal<>();
 static PreviewRenderer.BoxRenderResult take(){var d=prepared.get();prepared.remove();return d;}
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes original,LittleBoxes geometry){
  var faces=DragExteriorMesh.faces(geometry);if(faces==null)return null;
  var surfaceOwner=renderer.createBuffer();ByteBufferBuilder lineOwner=null;PreviewRenderer.BoxRenderResult depth=null;
  try{var surface=renderer.createBuilder(surfaceOwner,false);var edges=new PrimitiveGridOutlineEdges();double scale=geometry.grid.pixelLength;int count=0;
   for(int[] f:faces){if((count++&255)==0)PreviewBuildCancellation.check();int axis=f[0],a=f[3],b=f[4];boolean forward=((a+1)%3==b)==(f[1]==1);for(int j=0;j<4;j++){int corner=forward?j:(4-j)%4;double[] p=new double[3];p[axis]=f[2]*scale;p[a]=f[corner==1||corner==2?6:5]*scale;p[b]=f[corner>=2?8:7]*scale;surface.addVertex((float)p[0],(float)p[1],(float)p[2]).setColor(255,255,255,255);}edges.face(f,scale);}
   var surfaceMesh=surface.build();if(surfaceMesh instanceof MeshDataExtender e)e.keepAlive(true);depth=new PreviewRenderer.BoxRenderResult(geometry,geometry.pos,surfaceOwner,surfaceMesh);
   lineOwner=renderer.createBuffer();var line=new BufferBuilder(lineOwner,VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);edges.finish(s->{line.addVertex((float)s.a().x(),(float)s.a().y(),(float)s.a().z()).setColor(255,255,255,255);line.addVertex((float)s.b().x(),(float)s.b().y(),(float)s.b().z()).setColor(255,255,255,255);});PreviewBuildCancellation.check();var mesh=line.build();if(mesh instanceof MeshDataExtender e)e.keepAlive(true);var result=new PreviewRenderer.BoxRenderResult(original,original.pos,lineOwner,mesh);if(mesh!=null&&surfaceMesh!=null){prepared.set(depth);ChiselOutlineDepth.register(result,renderer,geometry);if(prepared.get()==null)depth=null;}return result;
  }catch(Throwable e){if(lineOwner!=null)lineOwner.close();throw e;}finally{prepared.remove();if(depth!=null)depth.close();else if(lineOwner==null)surfaceOwner.close();}
 }
}
