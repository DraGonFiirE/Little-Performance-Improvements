package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.performance.ShapePreviewTransform;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;

/** See-through hammer outlines, with detailed geometry for polygons and spheres. */
public final class HammerPreview {
   private HammerPreview() {}

   public static boolean needsDetail(LittleShape shape) {
      return shape instanceof LittleShapePolygon || shape instanceof LittleShapeSphere;
   }

   public static PreviewRenderer.BoxRenderResult build(LittleShape shape, ShapeSelection selection,
         LittleShapeConfig config, ItemStack stack, PreviewRenderer renderer) {
      if (selection.size() > 1) {
         var fast = OutlineWhileDragging.build(shape, selection, config, stack, renderer, true);
         if (fast != null) return LinesOnlyOutlineDrag.mark(fast);
      }
      // Native geometry matters for tile-selection shapes: their bounds are not
      // a substitute for the selected tiles. Initial cursors use it as well.
      var boxes = shape.build(selection, config);
      boxes = ShapePreviewTransform.apply(stack, selection, boxes, true, "preview");
      return mesh(renderer, PreviewRenderer.EMPTY, boxes);
   }

   public static PreviewRenderer.BoxRenderResult mesh(PreviewRenderer renderer, PoseStack pose, LittleBoxes boxes) {
      if (boxes.isEmpty()) return new PreviewRenderer.BoxRenderResult(boxes, boxes.pos, null, null);
      var result = LinesOnlyOutlineDrag.generic(renderer, boxes);
      if (result == null) result = ToolOutlinePreviewMesh.build(renderer, pose, boxes, boxes);
      return LinesOnlyOutlineDrag.mark(result);
   }
}
