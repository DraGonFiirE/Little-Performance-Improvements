package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
/** The exact two-point polygon sampler is fast even when a flat bound bypasses drag deferral. */
public final class PreviewShapeGeometry {
 public static LittleBoxes build(LittleShape shape,ShapeSelection selection,LittleShapeConfig config,ItemStack stack){
  SphereModeSharing.mark(null);
  if(shape.getClass()==team.creative.littletiles.common.placement.shape.type.LittleShapeSphere.class&&config instanceof team.creative.littletiles.common.placement.shape.config.HollowThicknessConfig sphere){
   var preview=new team.creative.littletiles.common.placement.shape.config.HollowThicknessConfig(true,sphere.thickness);
   return SphereModeSharing.mark(shape.build(selection,preview));
  }
  var mode=ChiselOnlyPreviewMode.current(stack);
  if(mode==PreviewRendererMode.Default&&stack.getItem() instanceof team.creative.littletiles.common.item.ItemLittleChisel&&(shape instanceof team.creative.littletiles.common.placement.shape.type.LittleShapeCurve||shape instanceof team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall||shape instanceof team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary))return DefaultDragUnion.build(TextureDragShapes.geometry(shape,selection,config));
  if(shape.getClass()==LittleShapePolygon.class&&selection.size()==2&&(mode==PreviewRendererMode.Surface||mode==PreviewRendererMode.Texture))return FastPolygonDrag.build(shape,selection,config);
  return shape.build(selection,config);
 }
}
