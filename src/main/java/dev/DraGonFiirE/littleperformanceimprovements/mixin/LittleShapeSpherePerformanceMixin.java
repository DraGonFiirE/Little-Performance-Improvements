package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.FastFilledSphere;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
import dev.DraGonFiirE.littleperformanceimprovements.performance.SphereVoxelColumns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.config.HollowThicknessConfig;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;

@Mixin(LittleShapeSphere.class)
public abstract class LittleShapeSpherePerformanceMixin extends LittleShape<HollowThicknessConfig> {
   protected LittleShapeSpherePerformanceMixin() {
      super(2);
   }

   @Overwrite
   protected void build(LittleBoxes boxes, ShapeSelection selection, HollowThicknessConfig config) {
      LittleBox box = selection.getOverallBox();
      if (littleperformanceimprovements$useScanlineBuilder()) {
         littleperformanceimprovements$buildPreviewScanlines(boxes, box, config);
      } else {
         boolean hollow = config.hollow;
         LittleVec size = box.getSize();
         LittleVec invCenter = size.calculateInvertedCenter();
         invCenter.invert();
         double a = Math.pow(Math.max(1, size.x / 2), 2.0);
         double b = Math.pow(Math.max(1, size.y / 2), 2.0);
         double c = Math.pow(Math.max(1, size.z / 2), 2.0);
         double a2 = 1.0;
         double b2 = 1.0;
         double c2 = 1.0;
         int thickness = config.thickness;
         if (hollow && size.x > thickness * 2 && size.y > thickness * 2 && size.z > thickness * 2) {
            int all = size.x + size.y + size.z;
            double sizeXValue = (double)size.x / all;
            double sizeYValue = (double)size.y / all;
            double sizeZValue = (double)size.z / all;
            if (sizeXValue > 0.5) {
               sizeXValue = 0.5;
            }

            if (sizeYValue > 0.5) {
               sizeYValue = 0.5;
            }

            if (sizeZValue > 0.5) {
               sizeZValue = 0.5;
            }

            a2 = Math.pow(Math.max(1.0, (sizeXValue * all - thickness * 2) / 2.0), 2.0);
            b2 = Math.pow(Math.max(1.0, (sizeYValue * all - thickness * 2) / 2.0), 2.0);
            c2 = Math.pow(Math.max(1.0, (sizeZValue * all - thickness * 2) / 2.0), 2.0);
         } else {
            hollow = false;
         }

         boolean stretchedX = size.x % 2 == 0;
         boolean stretchedY = size.y % 2 == 0;
         boolean stretchedZ = size.z % 2 == 0;
         double centerX = size.x / 2;
         double centerY = size.y / 2;
         double centerZ = size.z / 2;
         LittleVec min = box.getMinVec();

         for (int x = 0; x < size.x; x++) {
            PreviewBuildCancellation.check();

            for (int y = 0; y < size.y; y++) {
               for (int z = 0; z < size.z; z++) {
                  if ((z & 0xFF) == 0) {
                     PreviewBuildCancellation.check();
                  }

                  double posX = x - centerX + (stretchedX ? 0.5 : 0.0);
                  double posY = y - centerY + (stretchedY ? 0.5 : 0.0);
                  double posZ = z - centerZ + (stretchedZ ? 0.5 : 0.0);
                  double valueA = Math.pow(posX, 2.0) / a;
                  double valueB = Math.pow(posY, 2.0) / b;
                  double valueC = Math.pow(posZ, 2.0) / c;
                  if (valueA + valueB + valueC <= 1.0) {
                     double valueA2 = Math.pow(posX, 2.0) / a2;
                     double valueB2 = Math.pow(posY, 2.0) / b2;
                     double valueC2 = Math.pow(posZ, 2.0) / c2;
                     if (!hollow || valueA2 + valueB2 + valueC2 > 1.0) {
                        boxes.add(new LittleBox(new LittleVec(min.x + x, min.y + y, min.z + z)));
                     }
                  }
               }
            }
         }

         boxes.combineBoxesBlocks();
      }
   }

   private static boolean littleperformanceimprovements$useScanlineBuilder() {
      return true;
   }

   private static void littleperformanceimprovements$buildPreviewScanlines(LittleBoxes boxes, LittleBox box, HollowThicknessConfig config) {
      if (PreviewBuildCancellation.isPreviewBuild() && !config.hollow) {
         FastFilledSphere.build(boxes, box);
      } else {
         LittleVec size = box.getSize();
         LittleVec min = box.getMinVec();
         if (config.hollow && size.x > config.thickness * 2 && size.y > config.thickness * 2 && size.z > config.thickness * 2) {
            littleperformanceimprovements$buildErodedHollow(boxes, min, size, Math.max(1, config.thickness));
         } else {
            double a = Math.pow(Math.max(1, size.x / 2), 2.0);
            double b = Math.pow(Math.max(1, size.y / 2), 2.0);
            double c = Math.pow(Math.max(1, size.z / 2), 2.0);
            boolean hollow = config.hollow;
            double a2 = 1.0;
            double b2 = 1.0;
            double c2 = 1.0;
            int thickness = config.thickness;
            if (hollow && size.x > thickness * 2 && size.y > thickness * 2 && size.z > thickness * 2) {
               int all = size.x + size.y + size.z;
               double sizeXValue = Math.min(0.5, (double)size.x / all);
               double sizeYValue = Math.min(0.5, (double)size.y / all);
               double sizeZValue = Math.min(0.5, (double)size.z / all);
               a2 = Math.pow(Math.max(1.0, (sizeXValue * all - thickness * 2) / 2.0), 2.0);
               b2 = Math.pow(Math.max(1.0, (sizeYValue * all - thickness * 2) / 2.0), 2.0);
               c2 = Math.pow(Math.max(1.0, (sizeZValue * all - thickness * 2) / 2.0), 2.0);
            } else {
               hollow = false;
            }

            double[] outerX = littleperformanceimprovements$axisValues(size.x, a);
            double[] outerY = littleperformanceimprovements$axisValues(size.y, b);
            double[] outerZ = littleperformanceimprovements$axisValues(size.z, c);
            double[] innerX = hollow ? littleperformanceimprovements$axisValues(size.x, a2) : null;
            double[] innerY = hollow ? littleperformanceimprovements$axisValues(size.y, b2) : null;
            double[] innerZ = hollow ? littleperformanceimprovements$axisValues(size.z, c2) : null;
            boolean compactSolidPreview = !hollow && false;
            int[] compactX = compactSolidPreview ? littleperformanceimprovements$centralThird(outerX) : null;
            int[] compactY = compactSolidPreview ? littleperformanceimprovements$centralThird(outerY) : null;
            int[] compactZ = compactSolidPreview ? littleperformanceimprovements$centralThird(outerZ) : null;
            double shellA = Math.pow(Math.max(1.0, (size.x - 2) / 2.0), 2.0);
            double shellB = Math.pow(Math.max(1.0, (size.y - 2) / 2.0), 2.0);
            double shellC = Math.pow(Math.max(1.0, (size.z - 2) / 2.0), 2.0);
            boolean hasPreviewCavity = compactX != null && compactY != null && compactZ != null && size.x > 2 && size.y > 2 && size.z > 2;
            double[] shellX = hasPreviewCavity ? littleperformanceimprovements$axisValues(size.x, shellA) : null;
            double[] shellY = hasPreviewCavity ? littleperformanceimprovements$axisValues(size.y, shellB) : null;
            double[] shellZ = hasPreviewCavity ? littleperformanceimprovements$axisValues(size.z, shellC) : null;

            for (int x = 0; x < size.x; x++) {
               PreviewBuildCancellation.check();

               for (int y = 0; y < size.y; y++) {
                  if ((y & 63) == 0) {
                     PreviewBuildCancellation.check();
                  }

                  int[] outer = littleperformanceimprovements$axisRange(outerX[x] + outerY[y], outerZ, c, size.z);
                  if (outer != null) {
                     if (!hollow) {
                        if (!hasPreviewCavity) {
                           littleperformanceimprovements$addRun(boxes, min, x, y, outer[0], outer[1] + 1);
                        } else {
                           int[] cavity = littleperformanceimprovements$axisRange(shellX[x] + shellY[y], shellZ, shellC, size.z);
                           if (cavity != null && cavity[1] >= outer[0] && cavity[0] <= outer[1]) {
                              int cavityMin = Math.max(outer[0], cavity[0]);
                              int cavityMax = Math.min(outer[1], cavity[1]);
                              littleperformanceimprovements$addRun(boxes, min, x, y, outer[0], cavityMin);
                              littleperformanceimprovements$addRun(boxes, min, x, y, cavityMax + 1, outer[1] + 1);
                           } else {
                              littleperformanceimprovements$addRun(boxes, min, x, y, outer[0], outer[1] + 1);
                           }
                        }
                     } else {
                        int[] inner = littleperformanceimprovements$axisRange(innerX[x] + innerY[y], innerZ, c2, size.z);
                        if (inner != null && inner[1] >= outer[0] && inner[0] <= outer[1]) {
                           int innerMin = Math.max(outer[0], inner[0]);
                           int innerMax = Math.min(outer[1], inner[1]);
                           if (outer[0] < innerMin) {
                              littleperformanceimprovements$addRun(boxes, min, x, y, outer[0], innerMin);
                           }

                           if (innerMax < outer[1]) {
                              littleperformanceimprovements$addRun(boxes, min, x, y, innerMax + 1, outer[1] + 1);
                           }
                        } else {
                           littleperformanceimprovements$addRun(boxes, min, x, y, outer[0], outer[1] + 1);
                        }
                     }
                  }
               }
            }

            if (hasPreviewCavity && PreviewBuildCancellation.isLinePreview()) {
               boxes.add(
                  new LittleBox(min.x + compactX[0], min.y + compactY[0], min.z + compactZ[0], min.x + compactX[1], min.y + compactY[1], min.z + compactZ[1])
               );
            }

            PreviewBuildCancellation.check();
            boxes.combineBoxesBlocks();
         }
      }
   }

   private static void littleperformanceimprovements$buildErodedHollow(LittleBoxes boxes, LittleVec min, LittleVec size, int thickness) {
      SphereVoxelColumns.Columns outer = SphereVoxelColumns.ellipsoid(size.x, size.y, size.z);
      SphereVoxelColumns.Columns cavity = SphereVoxelColumns.erode(outer, thickness);

      for (int x = 0; x < size.x; x++) {
         PreviewBuildCancellation.check();

         for (int y = 0; y < size.y; y++) {
            int index = outer.index(x, y);
            if (outer.present(index)) {
               if (!cavity.present(index)) {
                  littleperformanceimprovements$addRun(boxes, min, x, y, outer.minimum(index), outer.maximum(index) + 1);
               } else {
                  littleperformanceimprovements$addRun(boxes, min, x, y, outer.minimum(index), cavity.minimum(index));
                  littleperformanceimprovements$addRun(boxes, min, x, y, cavity.maximum(index) + 1, outer.maximum(index) + 1);
               }
            }
         }
      }

      PreviewBuildCancellation.check();
      boxes.combineBoxesBlocks();
   }

   private static double[] littleperformanceimprovements$axisValues(int size, double radiusSquared) {
      double[] values = new double[size];
      double center = size / 2;
      boolean stretched = size % 2 == 0;

      for (int index = 0; index < size; index++) {
         double position = index - center + (stretched ? 0.5 : 0.0);
         values[index] = Math.pow(position, 2.0) / radiusSquared;
      }

      return values;
   }

   private static int[] littleperformanceimprovements$axisRange(double base, double[] axisValues, double radiusSquared, int size) {
      if (!(base > 1.0) && size > 0) {
         double center = size / 2 - (size % 2 == 0 ? 0.5 : 0.0);
         double extent = Math.sqrt(Math.max(0.0, radiusSquared * (1.0 - base)));
         int first = Math.max(0, (int)Math.ceil(center - extent));
         int last = Math.min(size - 1, (int)Math.floor(center + extent));

         while (first <= last && base + axisValues[first] > 1.0) {
            first++;
         }

         while (last >= first && base + axisValues[last] > 1.0) {
            last--;
         }

         while (first > 0 && base + axisValues[first - 1] <= 1.0) {
            first--;
         }

         while (last + 1 < size && base + axisValues[last + 1] <= 1.0) {
            last++;
         }

         return first <= last ? new int[]{first, last} : null;
      } else {
         return null;
      }
   }

   private static int[] littleperformanceimprovements$centralThird(double[] axisValues) {
      int first = 0;

      while (first < axisValues.length && axisValues[first] > 0.3333333333333333) {
         first++;
      }

      int last = axisValues.length - 1;

      while (last >= first && axisValues[last] > 0.3333333333333333) {
         last--;
      }

      return first <= last ? new int[]{first, last + 1} : null;
   }

   private static void littleperformanceimprovements$addRun(LittleBoxes boxes, LittleVec min, int x, int y, int minZ, int maxZ) {
      if (minZ < maxZ) {
         boxes.add(new LittleBox(min.x + x, min.y + y, min.z + minZ, min.x + x + 1, min.y + y + 1, min.z + maxZ));
      }
   }
}
