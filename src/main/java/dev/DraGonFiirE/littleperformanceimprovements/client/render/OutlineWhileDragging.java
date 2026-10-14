package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.*;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.performance.ShapePreviewTransform;
public final class OutlineWhileDragging {
 public static boolean enabled(LittleShape shape,ItemStack stack){return ChiselOnlyPreviewMode.current(stack)==PreviewRendererMode.Outline&&(shape instanceof LittleShapeCurve||shape instanceof LittleShapeCurveWall||shape instanceof LittleShapeCatenary||shape instanceof LittleShapePolygon||shape instanceof LittleShapeCylinder||shape instanceof LittleShapeSphere);}
 public static PreviewRenderer.BoxRenderResult build(LittleShape shape,ShapeSelection selection,LittleShapeConfig config,ItemStack stack,PreviewRenderer renderer,boolean lines){
  if(!lines||!enabled(shape,stack))return null;
  var boxes=TextureDragShapes.geometry(shape,selection,config);
  boxes=UniformOutlineDragResolution.apply(shape,boxes);
  boxes=ShapePreviewTransform.apply(stack,selection,boxes,true,"preview");
  var result=shape instanceof LittleShapePolygon?LinesOnlyOutlineDrag.build(renderer,boxes):LinesOnlyOutlineDrag.generic(renderer,boxes);
  if(result==null)result=ToolOutlinePreviewMesh.build(renderer,PreviewRenderer.EMPTY,boxes,boxes);
  return PolygonPerimeterPreview.mark(shape,selection,result);
 }
}
