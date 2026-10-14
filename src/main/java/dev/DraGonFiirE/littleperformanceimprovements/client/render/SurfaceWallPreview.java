package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurve;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCylinder;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.type.LittleShapePyramid;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;
import team.creative.littletiles.common.placement.shape.type.LittleShapeWall;

public final class SurfaceWallPreview {
   public static BoxRenderResult build(PreviewRenderer var0, PoseStack var1, LittleBoxes var2, boolean var3, ItemStack var4, LittleShape var5) {
      if (var3 && var5 instanceof LittleShapeCatenary && ChiselOnlyPreviewMode.current(var4) == PreviewRendererMode.Outline) {
         BoxRenderResult var10000 = FastChiselOutline.build(var0, var2, var2);
         if (var10000 != null) {
            return var10000;
         }
      }

      if (!var3 && var5 instanceof LittleShapePolygon && var4.getItem() instanceof ItemLittleChisel) {
         PreviewRendererMode var6 = ChiselOnlyPreviewMode.current(var4);
         if (var6 == PreviewRendererMode.Surface) {
            return ChiselOutlineDepth.register(SurfaceChiselShade.apply(DragExteriorMesh.build(var0, var2)), var0, var2);
         }
      }

      if (ChiselOnlyPreviewMode.current(var4) != PreviewRendererMode.Default
         || !(var4.getItem() instanceof ItemLittleChisel)
         || !(var5 instanceof LittleShapeCurve) && !(var5 instanceof LittleShapeCurveWall) && !(var5 instanceof LittleShapeCatenary)) {
         if (!var3 && ChiselOnlyPreviewMode.current(var4) == PreviewRendererMode.Default && var5 instanceof LittleShapeWall) {
            return ChiselOutlineDepth.register(SafeDragMesh.build(var0, var2), var0, var2);
         }

         if (!var3 && ChiselOnlyPreviewMode.current(var4) == PreviewRendererMode.Default && var5 instanceof LittleShapeSphere) {
            return ChiselOutlineDepth.register(SphereModeSharing.compact(DefaultSphereExterior.build(var0, var2), true, false), var0, var2);
         }

         if (!var3 && ChiselOnlyPreviewMode.current(var4) == PreviewRendererMode.Surface) {
            if (var5 instanceof LittleShapePyramid) {
               return ChiselOutlineDepth.register(PyramidSurfaceStyle.build(var0, var2), var0, var2);
            }

            if (var5 instanceof LittleShapeSphere) {
               return ChiselOutlineDepth.register(
                  SphereModeSharing.compact(SurfaceChiselShade.apply(DragExteriorMesh.build(var0, var2)), false, false), var0, var2
               );
            }

            if (var5 instanceof LittleShapeCylinder) {
               return ChiselOutlineDepth.register(SurfaceChiselShade.apply(DragExteriorMesh.build(var0, var2)), var0, var2);
            }

            if (var5 instanceof LittleShapeWall) {
               return ChiselOutlineDepth.register(SurfaceChiselShade.apply(buildWall(var0, var2)), var0, var2);
            }

            if (var5 instanceof LittleShapeCurve || var5 instanceof LittleShapeCurveWall || var5 instanceof LittleShapeCatenary) {
               BoxRenderResult var8 = DragExteriorMesh.build(var0, var2);
               if (var5 instanceof LittleShapeCurveWall) {
                  WallPreviewDepth.mark(var8.data());
               }

               return ChiselOutlineDepth.register(SurfaceChiselShade.apply(var8), var0, var2);
            }
         }

         BoxRenderResult var7 = ExactChiselOutline.build(var0, var1, var2, var3);
         return ChiselOutlineDepth.register(
            !var3 && ChiselOnlyPreviewMode.current(var4) == PreviewRendererMode.Surface ? SurfaceChiselShade.apply(var7) : var7, var0, var2
         );
      } else {
         return ChiselOutlineDepth.register(
            var5 instanceof LittleShapeCurveWall ? AsyncDefaultWallMesh.build(var0, var2, var3) : IndexedDefaultBoxMesh.build(var0, var2, var3), var0, var2
         );
      }
   }

   public static BoxRenderResult buildWall(PreviewRenderer var0, LittleBoxes var1) {
      BoxRenderResult var2 = SafeDragMesh.build(var0, var1);
      WallPreviewDepth.mark(var2.data());
      return var2;
   }
}
