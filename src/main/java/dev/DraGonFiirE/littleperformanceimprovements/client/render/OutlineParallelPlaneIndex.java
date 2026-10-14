package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.ArrayList;
import java.util.Arrays;

public final class OutlineParallelPlaneIndex {
   private static final double EPS = 2.0E-5;
   private static final int HEADER = 10;
   private static final int GROUP = 3;

   public static OutlinePlaneIndex.Built build(double[][] var0, int[] var1, double[] var2) {
      int[] var3 = OutlinePlaneOrder.sort(var1, var2);
      int var4 = var0.length;
      ArrayList var5 = new ArrayList();
      int var6 = 0;

      while (var6 < var4) {
         int var7 = var6 + 1;
         int var8 = var3[var6];

         while (var7 < var4 && var1[var3[var7]] == var1[var8] && (var1[var8] == 3 || var2[var3[var7]] == var2[var8])) {
            var7++;
         }

         OutlineWorkChunks.check();
         var5.add(new int[]{var6, var7, var1[var8]});
         var6 = var7;
      }

      OutlineParallelPlaneIndex.Buffer var9 = new OutlineParallelPlaneIndex.Buffer();
      var9.reserve(10 + var5.size() * 3);
      var9.values[0] = Double.NaN;
      var9.values[1] = var5.size();
      int var10 = 0;

      for (int var11 = 0; var11 < 4; var11++) {
         var9.values[2 + var11 * 2] = var10;

         while (var10 < var5.size() && ((int[])var5.get(var10))[2] == var11) {
            var10++;
         }

         var9.values[3 + var11 * 2] = var10;
      }

      for (int var12 = 0; var12 < var5.size(); var12++) {
         var9.values[10 + var12 * 3] = var2[var3[((int[])var5.get(var12))[0]]];
      }

      int[] var13 = new int[]{0};
      OutlineFaceTasks.<int[], OutlineParallelPlaneIndex.Buffer>process(var5, (var2x, var3x) -> {
         OutlineParallelPlaneIndex.Buffer var4x = new OutlineParallelPlaneIndex.Buffer();
         node(var4x, var0, var3, var2x[0], var2x[1]);
         var3x.accept(var4x);
      }, var5x -> {
         int var6x = var13[0]++;
         int var7x = 10 + var6x * 3;
         int var8x = var9.reserve(var5x.size);
         double var9x = var2[var3[((int[])var5.get((int)var6x))[0]]];
         var9.values[var7x + 1] = var8x;
         var9.values[var7x + 2] = var8x + var5x.size;
         System.arraycopy(var5x.values, 0, var9.values, var8x, var5x.size);

         for (int var11x = var8x; var11x < var8x + var5x.size; var11x += 9) {
            var9.values[var11x + 8] = var9.values[var11x + 8] + var8x;
         }
      });
      return new OutlinePlaneIndex.Built(Arrays.copyOf(var9.values, var9.size), var3);
   }

   private static void node(OutlineParallelPlaneIndex.Buffer var0, double[][] var1, int[] var2, int var3, int var4) {
      OutlineFaceTasks.check();
      int var5 = var0.reserve(9);
      double[] var6 = new double[]{
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      };
      double[] var7 = (double[])var6.clone();

      for (int var8 = var3; var8 < var4; var8++) {
         double[] var9 = var1[var2[var8]];

         for (int var10 = 0; var10 < 3; var10++) {
            var6[var10] = Math.min(var6[var10], var9[var10]);
            var6[var10 + 3] = Math.max(var6[var10 + 3], var9[var10 + 3]);
            double var11 = var9[var10] + var9[var10 + 3];
            var7[var10] = Math.min(var7[var10], var11);
            var7[var10 + 3] = Math.max(var7[var10 + 3], var11);
         }
      }

      System.arraycopy(var6, 0, var0.values, var5, 6);
      var0.values[var5 + 6] = var3;
      var0.values[var5 + 7] = var4;
      if (var4 - var3 > 12) {
         int var13 = OutlineSpatialPartition.splitAxis(var7);
         int var14 = (var3 + var4) / 2;
         partition(var1, var2, var3, var4, var14, var13);
         node(var0, var1, var2, var3, var14);
         node(var0, var1, var2, var14, var4);
      }

      var0.values[var5 + 8] = var0.size;
   }

   private static double center(double[][] var0, int var1, int var2) {
      double[] var3 = var0[var1];
      return var3[var2] + var3[var2 + 3];
   }

   private static void partition(double[][] var0, int[] var1, int var2, int var3, int var4, int var5) {
      int var6 = var2;
      int var7 = var3;
      int var8 = 64;

      while (var7 - var6 > 1) {
         if (--var8 == 0) {
            int var23 = var7 - var6;

            for (int var10 = var23 / 2 - 1; var10 >= 0; var10--) {
               int var25 = var10;

               while (var25 * 2 + 1 < var23) {
                  int var12 = var25 * 2 + 1;
                  if (var12 + 1 < var23 && center(var0, var1[var6 + var12], var5) < center(var0, var1[var6 + var12 + 1], var5)) {
                     var12++;
                  }

                  if (center(var0, var1[var6 + var25], var5) >= center(var0, var1[var6 + var12], var5)) {
                     break;
                  }

                  int var29 = var1[var6 + var25];
                  var1[var6 + var25] = var1[var6 + var12];
                  var1[var6 + var12] = var29;
                  var25 = var12;
               }
            }

            for (int var24 = var23 - 1; var24 > 0; var24--) {
               int var26 = var1[var6];
               var1[var6] = var1[var6 + var24];
               var1[var6 + var24] = var26;
               int var28 = 0;

               while (var28 * 2 + 1 < var24) {
                  int var30 = var28 * 2 + 1;
                  if (var30 + 1 < var24 && center(var0, var1[var6 + var30], var5) < center(var0, var1[var6 + var30 + 1], var5)) {
                     var30++;
                  }

                  if (center(var0, var1[var6 + var28], var5) >= center(var0, var1[var6 + var30], var5)) {
                     break;
                  }

                  var26 = var1[var6 + var28];
                  var1[var6 + var28] = var1[var6 + var30];
                  var1[var6 + var30] = var26;
                  var28 = var30;
               }
            }

            return;
         }

         double var9 = center(var0, var1[var6], var5);
         double var11 = center(var0, var1[var6 + var7 >>> 1], var5);
         double var13 = center(var0, var1[var7 - 1], var5);
         double var15 = Math.max(Math.min(var9, var11), Math.min(Math.max(var9, var11), var13));
         int var17 = var6;
         int var18 = var6;
         int var19 = var7;

         while (var18 < var19) {
            double var20 = center(var0, var1[var18], var5);
            if (var20 < var15) {
               int var22 = var1[var17];
               var1[var17++] = var1[var18];
               var1[var18++] = var22;
            } else if (var20 > var15) {
               int var31 = var1[--var19];
               var1[var19] = var1[var18];
               var1[var18] = var31;
            } else {
               var18++;
            }
         }

         if (var4 < var17) {
            var7 = var17;
         } else {
            if (var4 < var19) {
               return;
            }

            var6 = var19;
         }
      }
   }

   public static int[] query(double[] var0, double[] var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      int[] var14 = new int[16];
      int var15 = 0;

      for (int var16 = 0; var16 < 4; var16++) {
         int var17 = (int)var0[2 + var16 * 2];
         int var18 = (int)var0[3 + var16 * 2];
         if (var16 < 3) {
            double var19 = var16 == 0 ? var2 : (var16 == 1 ? var4 : var6);
            double var21 = var16 == 0 ? var8 : (var16 == 1 ? var10 : var12);
            double var23 = Math.max(var19, var21) - 2.0E-5 - 1.0E-12;
            double var25 = Math.min(var19, var21) + 2.0E-5 + 1.0E-12;
            if (var23 > var25) {
               continue;
            }

            int var27 = var17;
            int var28 = var18;

            while (var27 < var28) {
               int var29 = var27 + var28 >>> 1;
               if (var0[10 + var29 * 3] < var23) {
                  var27 = var29 + 1;
               } else {
                  var28 = var29;
               }
            }

            var17 = var27;
            var27 = var17;
            var28 = var18;

            while (var27 < var28) {
               int var36 = var27 + var28 >>> 1;
               if (var0[10 + var36 * 3] <= var25) {
                  var27 = var36 + 1;
               } else {
                  var28 = var36;
               }
            }

            var18 = var27;
         }

         for (int var31 = var17; var31 < var18; var31++) {
            int var20 = 10 + var31 * 3;
            int var32 = (int)var0[var20 + 2];
            int var22 = (int)var0[var20 + 1];

            while (var22 < var32) {
               int var33 = (int)var0[var22 + 8];
               if (OutlineFlatIndex.overlaps(var0, var22, var1) && OutlineFlatIndex.intersects(var0, var22, var2, var4, var6, var8, var10, var12)) {
                  if (var33 == var22 + 9) {
                     if (var15 + 2 > var14.length) {
                        var14 = Arrays.copyOf(var14, var14.length * 2);
                     }

                     var14[var15++] = (int)var0[var22 + 6];
                     var14[var15++] = (int)var0[var22 + 7];
                  }

                  var22 += 9;
               } else {
                  var22 = var33;
               }
            }
         }
      }

      return Arrays.copyOf(var14, var15);
   }

   private static final class Buffer {
      double[] values = new double[1024];
      int size;

      int reserve(int var1) {
         int var2 = this.size;
         this.size += var1;
         if (this.size > this.values.length) {
            this.values = Arrays.copyOf(this.values, Math.max(this.size, this.values.length * 2));
         }

         return var2;
      }
   }
}
