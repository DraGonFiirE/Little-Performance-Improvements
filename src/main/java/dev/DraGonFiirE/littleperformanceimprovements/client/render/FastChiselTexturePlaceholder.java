package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.nio.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
/** Constant-size textured drag placeholder; detailed texture tiling belongs to the finished mesh. */
public final class FastChiselTexturePlaceholder {
 private static String material,key;
 private static PreviewRenderer.BoxRenderResult unit,cached;
 public static MeshData prepare(MeshData mesh,PreviewRenderer renderer,ShapeSelection selection,ItemStack stack,PreviewRenderer.BoxRenderResult actual,boolean lines){
  if(lines||ChiselOnlyPreviewMode.current(stack)!=PreviewRendererMode.Texture||selection==null||(actual!=null&&actual.data()!=null))return mesh;
  var element=LittleElement.getOrDefault(stack);String nextMaterial=element.getState().toString()+":"+element.color;
  if(unit==null||!nextMaterial.equals(material)){
   var boxes=new LittleBoxesSimple(BlockPos.ZERO,LittleGrid.get(1));boxes.add(new LittleBox(0,0,0,1,1,1));
   var replacement=ToolTexturePreviewMesh.build(renderer,boxes,stack);
   if(replacement.data()==null){replacement.close();return mesh;}
   if(unit!=null)unit.close();unit=replacement;material=nextMaterial;key=null;
  }
  var box=selection.getOverallBox();String next=selection.pos+":"+selection.grid.count+":"+box;
  if(cached==null||!next.equals(key)){
   MeshData source=unit.data();int count=source.drawState().vertexCount();
   var owner=new ByteBufferBuilder(Math.max(256,count*24));
   try{
    var builder=new BufferBuilder(owner,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
    ByteBuffer data=source.vertexBuffer().duplicate().order(ByteOrder.nativeOrder());float scale=(float)selection.grid.pixelLength;
    PlaceholderSurfaceTiles.emit(builder,data,count,box,scale);
    MeshData result=builder.build();if(result==null){owner.close();return mesh;}((MeshDataExtender)(Object)result).keepAlive(true);
    var boxes=new LittleBoxesSimple(selection.pos,selection.grid);boxes.add(box.copy());
    var replacement=new PreviewRenderer.BoxRenderResult(boxes,selection.pos,owner,result);
    if(cached!=null)cached.close();cached=replacement;key=next;
   }catch(Throwable t){owner.close();throw t;}
  }
  if(mesh!=null)mesh.close();return cached.data();
 }
}
