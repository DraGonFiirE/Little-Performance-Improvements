package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.performance.ShapePreviewTransform;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurve;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCylinder;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.type.LittleShapePyramid;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;
import team.creative.littletiles.common.placement.shape.type.LittleShapeWall;

public final class DefaultWhileDragging {
   public static BoxRenderResult build(LittleShape var0, ShapeSelection var1, LittleShapeConfig var2, ItemStack var3, PreviewRenderer var4, boolean var5) {
      if (SimpleShapeDirectPreview.direct(var0, var3)) {
         return null;
      }

      BoxRenderResult var10000 = OutlineWhileDragging.build(var0, var1, var2, var3, var4, var5);
      if (var10000 != null) {
         return var10000;
      }

      PreviewRendererMode var6 = ChiselOnlyPreviewMode.current(var3);
      if (var6 == PreviewRendererMode.Texture && var0 instanceof LittleShapePyramid) {
         var6 = PreviewRendererMode.Surface;
      }

      if (var6 == PreviewRendererMode.Default && var3.getItem() instanceof ItemLittleChisel) {
         LittleBoxes var10 = !(var0 instanceof LittleShapeCurve) && !(var0 instanceof LittleShapeCurveWall) && !(var0 instanceof LittleShapeCatenary)
            ? TextureDragShapes.geometry(var0, var1, var2)
            : PreviewShapeGeometry.build(var0, var1, var2, var3);
         var10 = ShapePreviewTransform.apply(var3, var1, var10, var5, "preview");
         return PolygonPerimeterPreview.mark(
            var0,
            var1,
            !var5 && var0 instanceof LittleShapeWall
               ? SafeDragMesh.build(var4, var10)
               : (
                  !var5 && var0 instanceof LittleShapeCylinder
                     ? DefaultCylinderExterior.build(var4, var10)
                     : (var0 instanceof LittleShapeCurveWall ? AsyncDefaultWallMesh.build(var4, var10, var5) : IndexedDefaultBoxMesh.build(var4, var10, var5))
               )
         );
      }

      if (!var5 && (var6 == PreviewRendererMode.Texture || var6 == PreviewRendererMode.Surface)) {
         LittleBoxes var7 = TextureDragShapes.geometry(var0, var1, var2);
         var7 = ShapePreviewTransform.apply(var3, var1, var7, false, "preview");
         if (var6 == PreviewRendererMode.Surface && var0 instanceof LittleShapePyramid) {
            return PyramidTextureSurfaceStyle.mark(PolygonPerimeterPreview.mark(var0, var1, PyramidSurfaceStyle.build(var4, var7)), var3);
         }

         BoxRenderResult var8 = PolygonPerimeterPreview.mark(
            var0,
            var1,
            var6 == PreviewRendererMode.Surface && var0 instanceof LittleShapeCylinder
               ? CylinderDragExterior.build(var4, var7)
               : (
                  var0 instanceof LittleShapeSphere
                     ? CompactSphereDragMesh.build(var4, var7)
                     : (
                        !(var0 instanceof LittleShapePolygon)
                              && !(var0 instanceof LittleShapeCatenary)
                              && !(var0 instanceof LittleShapeCurve)
                              && !(var0 instanceof LittleShapeCurveWall)
                           ? SafeDragMesh.build(var4, var7)
                           : PolygonDragFaces.surface(var4, var7, var0)
                     )
               )
         );
         if (var0 instanceof LittleShapeCurveWall
            || var0 instanceof LittleShapeWall
            || var6 == PreviewRendererMode.Surface && var0 instanceof LittleShapePyramid) {
            WallPreviewDepth.mark(var8.data());
         }

         SurfaceChiselShade.apply(var8);
         return var8;
      } else {
         return null;
      }
   }

   public static PreviewRendererMode mode(ItemStack var0, MeshData var1) {
      PreviewRendererMode var2 = ChiselOnlyPreviewMode.current(var0);
      return var2 == PreviewRendererMode.Texture && var1 != null && var1.drawState().format() != DefaultVertexFormat.POSITION_TEX_COLOR
         ? (PyramidTextureSurfaceStyle.contains(var1) ? PreviewRendererMode.Surface : PreviewRendererMode.Default)
         : var2;
   }
}
