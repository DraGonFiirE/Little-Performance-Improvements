package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
import dev.DraGonFiirE.littleperformanceimprovements.performance.SphereVoxelColumns;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.placement.shape.config.HollowThicknessConfig;

public final class SpherePreviewBoxes {
   private static final int COORDINATE_BITS = 21;
   private static final long COORDINATE_MASK = 2097151L;

   private SpherePreviewBoxes() {
   }

   public static LittleBoxes build(ShapeSelection selection, HollowThicknessConfig config) {
      LittleBox bounds = selection.getOverallBox();
      LittleVec size = bounds.getSize();
      LittleVec minimum = bounds.getMinVec();
      int sizeX = size.x;
      int sizeY = size.y;
      int sizeZ = size.z;
      boolean hollow = true && sizeX > config.thickness * 2 && sizeY > config.thickness * 2 && sizeZ > config.thickness * 2;
      SphereVoxelColumns.Columns outerColumns = SphereVoxelColumns.ellipsoid(sizeX, sizeY, sizeZ);
      SphereVoxelColumns.Columns cavityColumns = hollow ? SphereVoxelColumns.erode(outerColumns, Math.max(1, config.thickness)) : null;
      SpherePreviewBoxes.Column[] columns = new SpherePreviewBoxes.Column[sizeX * sizeY];

      for (int x = 0; x < sizeX; x++) {
         PreviewBuildCancellation.check();

         for (int y = 0; y < sizeY; y++) {
            int index = outerColumns.index(x, y);
            if (outerColumns.present(index)) {
               int[] outer = new int[]{outerColumns.minimum(index), outerColumns.maximum(index)};
               if (!hollow) {
                  columns[index] = SpherePreviewBoxes.Column.single(outer[0], outer[1]);
               } else {
                  int[] cavity = cavityColumns.present(index) ? new int[]{cavityColumns.minimum(index), cavityColumns.maximum(index)} : null;
                  columns[index] = SpherePreviewBoxes.Column.subtract(outer, cavity);
               }
            }
         }
      }

      LongOpenHashSet surface = new LongOpenHashSet();

      for (int x = 0; x < sizeX; x++) {
         PreviewBuildCancellation.check();

         for (int y = 0; y < sizeY; y++) {
            SpherePreviewBoxes.Column current = columns[x * sizeY + y];
            if (current != null) {
               current.addVerticalEndpoints(surface, x, y);
               addDifference(surface, x, y, current, column(columns, x - 1, y, sizeX, sizeY));
               addDifference(surface, x, y, current, column(columns, x + 1, y, sizeX, sizeY));
               addDifference(surface, x, y, current, column(columns, x, y - 1, sizeX, sizeY));
               addDifference(surface, x, y, current, column(columns, x, y + 1, sizeX, sizeY));
            }
         }
      }

      LittleBoxes display = new LittleBoxesSimple(selection.pos, selection.grid);
      LongIterator iterator = surface.iterator();

      while (iterator.hasNext()) {
         long key = iterator.nextLong();
         int x = (int)(key >>> 42);
         int y = (int)(key >>> 21 & 2097151L);
         int z = (int)(key & 2097151L);
         display.add(new LittleBox(minimum.x + x, minimum.y + y, minimum.z + z, minimum.x + x + 1, minimum.y + y + 1, minimum.z + z + 1));
      }

      return display;
   }

   private static SpherePreviewBoxes.Column column(SpherePreviewBoxes.Column[] columns, int x, int y, int sizeX, int sizeY) {
      return x >= 0 && x < sizeX && y >= 0 && y < sizeY ? columns[x * sizeY + y] : null;
   }

   private static void addDifference(LongOpenHashSet surface, int x, int y, SpherePreviewBoxes.Column current, SpherePreviewBoxes.Column neighbor) {
      for (int interval = 0; interval < current.count; interval++) {
         int start = current.minimum(interval);
         int end = current.maximum(interval);
         if (neighbor == null) {
            addRange(surface, x, y, start, end);
         } else {
            for (int other = 0; other < neighbor.count && start <= end; other++) {
               int otherMin = neighbor.minimum(other);
               int otherMax = neighbor.maximum(other);
               if (otherMax >= start) {
                  if (otherMin > end) {
                     break;
                  }

                  addRange(surface, x, y, start, Math.min(end, otherMin - 1));
                  start = Math.max(start, otherMax + 1);
               }
            }

            addRange(surface, x, y, start, end);
         }
      }
   }

   private static void addRange(LongOpenHashSet surface, int x, int y, int minimum, int maximum) {
      for (int z = minimum; z <= maximum; z++) {
         surface.add(cellKey(x, y, z));
      }
   }

   private static long cellKey(int x, int y, int z) {
      if ((x | y | z) >= 0 && x <= 2097151L && y <= 2097151L && z <= 2097151L) {
         return (long)x << 42 | (long)y << 21 | z;
      } else {
         throw new IllegalArgumentException("Sphere preview exceeds supported grid coordinate range");
      }
   }

   private static final class Column {
      private final int firstMin;
      private final int firstMax;
      private final int secondMin;
      private final int secondMax;
      private final int count;

      private Column(int firstMin, int firstMax, int secondMin, int secondMax, int count) {
         this.firstMin = firstMin;
         this.firstMax = firstMax;
         this.secondMin = secondMin;
         this.secondMax = secondMax;
         this.count = count;
      }

      static SpherePreviewBoxes.Column single(int minimum, int maximum) {
         return new SpherePreviewBoxes.Column(minimum, maximum, 0, -1, 1);
      }

      static SpherePreviewBoxes.Column subtract(int[] outer, int[] inner) {
         if (inner != null && inner[1] >= outer[0] && inner[0] <= outer[1]) {
            int innerMin = Math.max(outer[0], inner[0]);
            int innerMax = Math.min(outer[1], inner[1]);
            boolean lower = outer[0] < innerMin;
            boolean upper = innerMax < outer[1];
            if (lower && upper) {
               return new SpherePreviewBoxes.Column(outer[0], innerMin - 1, innerMax + 1, outer[1], 2);
            } else if (lower) {
               return single(outer[0], innerMin - 1);
            } else {
               return upper ? single(innerMax + 1, outer[1]) : null;
            }
         } else {
            return single(outer[0], outer[1]);
         }
      }

      int minimum(int interval) {
         return interval == 0 ? this.firstMin : this.secondMin;
      }

      int maximum(int interval) {
         return interval == 0 ? this.firstMax : this.secondMax;
      }

      void addVerticalEndpoints(LongOpenHashSet surface, int x, int y) {
         for (int interval = 0; interval < this.count; interval++) {
            surface.add(SpherePreviewBoxes.cellKey(x, y, this.minimum(interval)));
            surface.add(SpherePreviewBoxes.cellKey(x, y, this.maximum(interval)));
         }
      }
   }
}
