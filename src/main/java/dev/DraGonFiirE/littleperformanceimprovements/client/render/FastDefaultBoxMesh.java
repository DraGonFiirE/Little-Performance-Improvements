package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import java.nio.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
import team.creative.littletiles.common.grid.LittleGrid;
import net.minecraft.core.BlockPos;
public final class FastDefaultBoxMesh {
 private static volatile int[] template;
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes,boolean lines){
  if(lines)return renderer.buildBoxes(PreviewRenderer.EMPTY,boxes,true);
  for(var b:boxes.all())if(b.getClass()!=LittleBox.class)return renderer.buildBoxes(PreviewRenderer.EMPTY,boxes,false);
  int[] vertices=template;if(vertices==null){synchronized(FastDefaultBoxMesh.class){if(template==null)template=template(renderer);vertices=template;}}
  var backing=renderer.createBuffer();try{
   var builder=renderer.createBuilder(backing,false);float scale=(float)boxes.grid.pixelLength;
   for(var box:boxes.all()){
    float x0=box.minX*scale,y0=box.minY*scale,z0=box.minZ*scale,x1=box.maxX*scale,y1=box.maxY*scale,z1=box.maxZ*scale;
    for(int v=0;v<vertices.length;v+=2){int corner=vertices[v];builder.addVertex((corner&1)==0?x0:x1,(corner&2)==0?y0:y1,(corner&4)==0?z0:z1).setColor(vertices[v+1]);}
   }
   var mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,backing,mesh);
  }catch(Throwable e){backing.close();throw e;}
 }
 private static int[] template(PreviewRenderer renderer){
  var boxes=new LittleBoxesSimple(BlockPos.ZERO,LittleGrid.get(16));boxes.add(new LittleBox(0,0,0,16,16,16));var result=renderer.buildBoxes(PreviewRenderer.EMPTY,boxes,false);
  try{var mesh=result.data();var data=mesh.vertexBuffer().order(ByteOrder.nativeOrder());int count=mesh.drawState().vertexCount();var out=new int[count*2];
   for(int v=0;v<count;v++){int at=v*16,mask=0;for(int a=0;a<3;a++){float f=data.getFloat(at+a*4);if(f==1)mask|=1<<a;else if(f!=0)throw new IllegalStateException("Non-box template");}out[v*2]=mask;out[v*2+1]=((data.get(at+15)&255)<<24)|((data.get(at+12)&255)<<16)|((data.get(at+13)&255)<<8)|(data.get(at+14)&255);}
   // Native triangulated quads can be submitted as the original four corners.
   if(count==48){var compact=new int[48];boolean valid=true;for(int face=0;face<6;face++){int at=face*16;valid&=out[at+8]==out[at+10]&&out[at+8]==out[at+12]&&out[at+8]==out[at+14];System.arraycopy(out,at,compact,face*8,8);}if(valid)return compact;}
   return out;
  }finally{result.close();}
 }
}
