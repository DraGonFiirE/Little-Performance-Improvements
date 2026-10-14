package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
/** Native chisel edges without the legacy per-coordinate preview expansion. */
public final class ExactChiselOutline {
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,PoseStack pose,LittleBoxes boxes,boolean lines){
  if(!lines||ToolPreviewMode.current()!=PreviewRendererMode.Outline)return renderer.buildBoxes(pose,boxes,lines);
  var owner=renderer.createBuffer();try{var builder=renderer.createBuilder(owner,true);int i=0;for(var box:boxes.all()){if((i++&255)==0)PreviewBuildCancellation.check();var render=box.getRenderingBox(boxes.getGrid());if(render!=null)render.renderLines(pose,builder,255);}
   var mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);OutlineMeshColor.neutralize(mesh);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,owner,mesh);
  }catch(Throwable e){owner.close();throw e;}
 }
}
