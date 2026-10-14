package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;
/** Native Default box triangles with shared corners; identical triangle order and alpha. */
public final class IndexedDefaultBoxMesh {
 private static final int[] QUADS={4,0,1,5,2,6,7,3,3,1,0,2,6,4,5,7,2,0,4,6,7,5,1,3};
 private static final int[] TRI={0,1,2,2,3,0};
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes,boolean lines){
  int count=boxes.all().size();if(lines||count==0||count>Integer.MAX_VALUE/272)return FastDefaultBoxMesh.build(renderer,boxes,lines);
  for(var b:boxes.all())if(b.getClass()!=LittleBox.class)return FastDefaultBoxMesh.build(renderer,boxes,lines);
  var owner=new ByteBufferBuilder(count*272);try{
   long vertices=owner.reserve(count*128);float scale=(float)boxes.grid.pixelLength;int base=0;
   for(var b:boxes.all()){float x0=b.minX*scale,x1=b.maxX*scale,y0=b.minY*scale,y1=b.maxY*scale,z0=b.minZ*scale,z1=b.maxZ*scale;
    for(int corner=0;corner<8;corner++){long at=vertices+(long)base*16;MemoryUtil.memPutFloat(at,(corner&1)==0?x0:x1);MemoryUtil.memPutFloat(at+4,(corner&2)==0?y0:y1);MemoryUtil.memPutFloat(at+8,(corner&4)==0?z0:z1);MemoryUtil.memPutInt(at+12,-1);base++;}
   }
   var vb=owner.build();long indices=owner.reserve(count*144);int offset=0;
   DefaultBoxIndices.copy(indices,count);
   var ib=owner.build();var mesh=new MeshData(vb,new MeshData.DrawState(DefaultVertexFormat.POSITION_COLOR,count*8,count*36,VertexFormat.Mode.QUADS,VertexFormat.IndexType.INT));
   ((MeshDataAccessor)(Object)mesh).littleperformanceimprovements$setIndexBuffer(ib);((MeshDataExtender)(Object)mesh).keepAlive(true);
   return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,owner,mesh);
  }catch(Throwable e){owner.close();throw e;}
 }
}
