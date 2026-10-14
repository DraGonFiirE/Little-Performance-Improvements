package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.config.CatenaryConfig;
import team.creative.littletiles.common.placement.shape.config.InterpolationAxisThicknessConfig;
import team.creative.littletiles.common.placement.shape.config.InterpolationThicknessConfig;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.placement.shape.type.LittleShapeBlob;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCube;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurve;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCylinder;
import team.creative.littletiles.common.placement.shape.type.LittleShapeDragBox;
import team.creative.littletiles.common.placement.shape.type.LittleShapeInnerCorner;
import team.creative.littletiles.common.placement.shape.type.LittleShapeOuterCorner;
import team.creative.littletiles.common.placement.shape.type.LittleShapePillar;
import team.creative.littletiles.common.placement.shape.type.LittleShapePixel;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.type.LittleShapePyramid;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSelectable;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSlope;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;
import team.creative.littletiles.common.placement.shape.type.LittleShapeWall;

public final class TextureDragShapes {
   public static boolean defer(LittleShape var0, ItemStack var1) {
      if (SimpleShapeDirectPreview.direct(var0, var1)) {
         return false;
      } else if (OutlineWhileDragging.enabled(var0, var1)) {
         return true;
      } else {
         return ChiselOnlyPreviewMode.current(var1) != PreviewRendererMode.Texture
               || !(var0 instanceof LittleShapePixel)
                  && !(var0 instanceof LittleShapeBlob)
                  && !(var0 instanceof LittleShapeCube)
                  && !(var0 instanceof LittleShapeSelectable)
            ? var0 instanceof LittleShapePolygon
               || var0 instanceof LittleShapeCylinder
               || var0 instanceof LittleShapeSphere
               || var1.getItem() instanceof ItemLittleChisel
                  && (
                     ChiselOnlyPreviewMode.current(var1) == PreviewRendererMode.Texture
                        || ChiselOnlyPreviewMode.current(var1) == PreviewRendererMode.Surface
                        || ChiselOnlyPreviewMode.current(var1) == PreviewRendererMode.Default
                  )
            : false;
      }
   }

   public static LittleBoxes geometry(LittleShape var0, ShapeSelection var1, LittleShapeConfig var2) {
      if (var0 instanceof LittleShapePolygon) {
         return PolygonPerimeterPreview.geometry(var0, var1, var2);
      } else if (var0 instanceof LittleShapeCylinder) {
         return FastCylinderDrag.build(var0, var1, var2);
      } else if (var0 instanceof LittleShapeSphere) {
         return sphere(var1);
      } else if (var0.getClass() == LittleShapeCatenary.class) {
         return RawCatenaryDrag.sample(var1, (CatenaryConfig)var2);
      } else if (var0.getClass() == LittleShapeCurve.class) {
         return RawCurveDrag.sample(var1, (InterpolationThicknessConfig)var2);
      } else if (var0.getClass() == LittleShapeCurveWall.class) {
         return RawCurveWallDrag.sample(var1, (InterpolationAxisThicknessConfig)var2);
      } else {
         return !(var0 instanceof LittleShapeDragBox)
               && !(var0 instanceof LittleShapeCube)
               && !(var0 instanceof LittleShapePixel)
               && !(var0 instanceof LittleShapeSlope)
               && !(var0 instanceof LittleShapeInnerCorner)
               && !(var0 instanceof LittleShapeOuterCorner)
               && !(var0 instanceof LittleShapePyramid)
               && !(var0 instanceof LittleShapePillar)
               && !(var0 instanceof LittleShapeWall)
               && !(var0 instanceof LittleShapeCurve)
               && !(var0 instanceof LittleShapeCurveWall)
               && !(var0 instanceof LittleShapeCatenary)
            ? frame(var1)
            : var0.build(var1, var2);
      }
   }

   public static LittleBoxes frame(ShapeSelection var0) {
      LittleBox var1 = var0.getOverallBox();
      LittleBoxesSimple var2 = new LittleBoxesSimple(var0.pos, var0.grid);

      for (int var3 = 0; var3 < 3; var3++) {
         for (int var4 = 0; var4 < 2; var4++) {
            for (int var5 = 0; var5 < 2; var5++) {
               int[] var6 = new int[]{var1.minX, var1.minY, var1.minZ};
               int[] var7 = new int[]{var1.maxX, var1.maxY, var1.maxZ};
               int var8 = (var3 + 1) % 3;
               int var9 = (var3 + 2) % 3;
               var6[var8] = var4 == 0 ? var6[var8] : var7[var8] - 1;
               var7[var8] = var6[var8] + 1;
               var6[var9] = var5 == 0 ? var6[var9] : var7[var9] - 1;
               var7[var9] = var6[var9] + 1;
               var2.add(new LittleBox(var6[0], var6[1], var6[2], var7[0], var7[1], var7[2]));
            }
         }
      }

      return var2;
   }

   public static LittleBoxes sphere(ShapeSelection var0) {
      return ExactSphereDrag.build(var0);
   }
}
