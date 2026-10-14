package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.LittleTransformableBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
public final class SafeDragMesh {
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes){
  boolean transformed=false;for(var b:boxes.all())if(b instanceof LittleTransformableBox){transformed=true;break;}
  if(!transformed)return renderer.buildBoxes(PreviewRenderer.EMPTY,boxes,false);
  ByteBufferBuilder buffer=renderer.createBuffer();
  try{
   BufferBuilder builder=renderer.createBuilder(buffer,false);
   for(var b:boxes.all()){
    var render=b.getRenderingBox(boxes.grid);if(render==null)continue;
    if(render instanceof team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable transformedBox){
     for(Facing face:Facing.values()){var fans=transformedBox.getFaceCache(face);if(fans!=null)for(VectorFan fan:fans)emit(builder,render,fan);}
    }else renderer.buildBox(PreviewRenderer.EMPTY,render,builder,255,false,false);
   }
   MeshData mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);
   return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,buffer,mesh);
  }catch(Throwable failure){buffer.close();throw failure;}
 }
 static void emit(BufferBuilder out,RenderBox box,VectorFan fan){
  int n=fan.count();if(n<3)return;
  // Pad each fan triangle individually to the Default renderer's QUADS format.
  for(int i=1;i<n-1;i++){vertex(out,box,fan,0);vertex(out,box,fan,i);vertex(out,box,fan,i+1);vertex(out,box,fan,i+1);}
 }
 private static void vertex(BufferBuilder out,RenderBox box,VectorFan fan,int index){
  var p=fan.get(index);float x=p.x,y=p.y,z=p.z;
  {x=x*box.getPreviewScaleX()+box.getPreviewOffX();y=y*box.getPreviewScaleY()+box.getPreviewOffY();z=z*box.getPreviewScaleZ()+box.getPreviewOffZ();}
  out.addVertex(x,y,z).setColor((box.color>>16)&255,(box.color>>8)&255,box.color&255,255);
 }
}

