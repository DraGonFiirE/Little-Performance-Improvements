package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary;
public final class DefaultInitialPreview {
 public static boolean initial(LittleShape shape,ShapeSelection selection){return shape instanceof LittleShapeCatenary && selection!=null && selection.size()<2;}
 public static boolean draw(ItemStack stack,PreviewRenderer.BoxRenderResult result,MeshData mesh,LittleShape shape,ShapeSelection selection){
  if(initial(shape,selection)&&ChiselOnlyPreviewMode.current(stack)==dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode.Surface)InitialSurfaceShade.apply(mesh);
  // Native catenary has no geometry yet: retain LT's initial cursor marker only.
  return initial(shape,selection)||DefaultPreviewReady.draw(stack,result,mesh);
 }
}
