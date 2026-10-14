package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
public final class CompactSphereDragMesh {
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes){
  for(var box:boxes.all())if(box.getClass()!=LittleBox.class)return SafeDragMesh.build(renderer,boxes);
  var buffer=renderer.createBuffer();try{var builder=renderer.createBuilder(buffer,false);float scale=(float)boxes.grid.pixelLength;
   for(var box:boxes.all()){float x0=box.minX*scale,y0=box.minY*scale,z0=box.minZ*scale,x1=box.maxX*scale,y1=box.maxY*scale,z1=box.maxZ*scale;
    builder.addVertex(x0,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x0,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z1).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z1).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z1).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x0,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z0).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z0).setColor(255,255,255,255);
    builder.addVertex(x0,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x1,y0,z1).setColor(255,255,255,255);
    builder.addVertex(x1,y1,z1).setColor(255,255,255,255);
    builder.addVertex(x0,y1,z1).setColor(255,255,255,255);
   }
   var mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,buffer,mesh);
  }catch(Throwable e){buffer.close();throw e;}
 }
 private static int coord(LittleBox b,int axis,boolean max){return axis==0?(max?b.maxX:b.minX):axis==1?(max?b.maxY:b.minY):(max?b.maxZ:b.minZ);}
}
