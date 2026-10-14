package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.Arrays;

public final class SphereVoxelColumns {
   private SphereVoxelColumns() {
   }

   public static SphereVoxelColumns.Columns ellipsoid(int sizeX, int sizeY, int sizeZ) {
      double radiusX = Math.pow(Math.max(1, sizeX / 2), 2.0);
      double radiusY = Math.pow(Math.max(1, sizeY / 2), 2.0);
      double radiusZ = Math.pow(Math.max(1, sizeZ / 2), 2.0);
      double[] valuesX = axisValues(sizeX, radiusX);
      double[] valuesY = axisValues(sizeY, radiusY);
      double[] valuesZ = axisValues(sizeZ, radiusZ);
      int[] minimum = new int[sizeX * sizeY];
      int[] maximum = new int[sizeX * sizeY];
      Arrays.fill(minimum, -1);
      Arrays.fill(maximum, -1);

      for (int x = 0; x < sizeX; x++) {
         PreviewBuildCancellation.check();

         for (int y = 0; y < sizeY; y++) {
            int[] range = axisRange(valuesX[x] + valuesY[y], valuesZ, radiusZ, sizeZ);
            if (range != null) {
               int index = x * sizeY + y;
               minimum[index] = range[0];
               maximum[index] = range[1];
            }
         }
      }

      return new SphereVoxelColumns.Columns(sizeX, sizeY, minimum, maximum);
   }

   public static SphereVoxelColumns.Columns erode(SphereVoxelColumns.Columns source, int layers) {
      SphereVoxelColumns.Columns current = source;

      for (int layer = 0; layer < layers; layer++) {
         int[] minimum = new int[current.minimum.length];
         int[] maximum = new int[current.maximum.length];
         Arrays.fill(minimum, -1);
         Arrays.fill(maximum, -1);

         for (int x = 0; x < current.sizeX; x++) {
            PreviewBuildCancellation.check();

            for (int y = 0; y < current.sizeY; y++) {
               int center = current.index(x, y);
               if (current.present(center) && x != 0 && x + 1 != current.sizeX && y != 0 && y + 1 != current.sizeY) {
                  int left = current.index(x - 1, y);
                  int right = current.index(x + 1, y);
                  int down = current.index(x, y - 1);
                  int up = current.index(x, y + 1);
                  if (current.present(left) && current.present(right) && current.present(down) && current.present(up)) {
                     int low = Math.max(
                        current.minimum[center] + 1,
                        Math.max(Math.max(current.minimum[left], current.minimum[right]), Math.max(current.minimum[down], current.minimum[up]))
                     );
                     int high = Math.min(
                        current.maximum[center] - 1,
                        Math.min(Math.min(current.maximum[left], current.maximum[right]), Math.min(current.maximum[down], current.maximum[up]))
                     );
                     if (low <= high) {
                        minimum[center] = low;
                        maximum[center] = high;
                     }
                  }
               }
            }
         }

         current = new SphereVoxelColumns.Columns(current.sizeX, current.sizeY, minimum, maximum);
      }

      return current;
   }

   private static double[] axisValues(int size, double radiusSquared) {
      double[] values = new double[size];
      double center = size / 2;
      boolean even = size % 2 == 0;

      for (int index = 0; index < size; index++) {
         double position = index - center + (even ? 0.5 : 0.0);
         values[index] = position * position / radiusSquared;
      }

      return values;
   }

   private static int[] axisRange(double base, double[] values, double radiusSquared, int size) {
      if (!(base > 1.0) && size > 0) {
         double center = size / 2.0 - (size % 2 == 0 ? 0.5 : 0.0);
         double extent = Math.sqrt(Math.max(0.0, radiusSquared * (1.0 - base)));
         int first = Math.max(0, (int)Math.ceil(center - extent));
         int last = Math.min(size - 1, (int)Math.floor(center + extent));

         while (first <= last && base + values[first] > 1.0) {
            first++;
         }

         while (last >= first && base + values[last] > 1.0) {
            last--;
         }

         while (first > 0 && base + values[first - 1] <= 1.0) {
            first--;
         }

         while (last + 1 < size && base + values[last + 1] <= 1.0) {
            last++;
         }

         return first <= last ? new int[]{first, last} : null;
      } else {
         return null;
      }
   }

   public static final class Columns {
      public final int sizeX;
      public final int sizeY;
      private final int[] minimum;
      private final int[] maximum;

      private Columns(int sizeX, int sizeY, int[] minimum, int[] maximum) {
         this.sizeX = sizeX;
         this.sizeY = sizeY;
         this.minimum = minimum;
         this.maximum = maximum;
      }

      public int index(int x, int y) {
         return x * this.sizeY + y;
      }

      public boolean present(int index) {
         return index >= 0 && index < this.minimum.length && this.minimum[index] >= 0;
      }

      public int minimum(int index) {
         return this.minimum[index];
      }

      public int maximum(int index) {
         return this.maximum[index];
      }
   }
}
