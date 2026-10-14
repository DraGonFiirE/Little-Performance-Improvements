package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;

final class OutlineSpatialPartition {
   private OutlineSpatialPartition() {
   }

   static int splitAxis(double[] var0) {
      double var1 = var0[3] - var0[0];
      double var3 = var0[4] - var0[1];
      double var5 = var0[5] - var0[2];
      return var3 > var1 ? (var5 > var3 ? 2 : 1) : (var5 > var1 ? 2 : 0);
   }

   static boolean intersects(double[] var0, double var1, double var3, double var5, double var7, double var9, double var11) {
      if ((var1 == var7 ? 0 : 1) + (var3 == var9 ? 0 : 1) + (var5 == var11 ? 0 : 1) <= 1) {
         return true;
      }

      double var13 = 0.0;
      double var15 = 1.0;

      for (int var17 = 0; var17 < 3; var17++) {
         double var18 = var17 == 0 ? var1 : (var17 == 1 ? var3 : var5);
         double var20 = var17 == 0 ? var7 : (var17 == 1 ? var9 : var11);
         double var22 = var0[var17] - 8.0E-5;
         double var24 = var0[var17 + 3] + 8.0E-5;
         double var26 = var20 - var18;
         if (var26 == 0.0) {
            if (var18 < var22 || var18 > var24) {
               return false;
            }
         } else {
            double var28 = (var22 - var18) / var26;
            double var30 = (var24 - var18) / var26;
            if (var28 > var30) {
               double var32 = var28;
               var28 = var30;
               var30 = var32;
            }

            var13 = Math.max(var13, var28);
            var15 = Math.min(var15, var30);
            if (var13 > var15) {
               return false;
            }
         }
      }

      return true;
   }

   static <T> void partition(T[] var0, int var1, int var2, int var3, ToDoubleFunction<T> var4) {
      HeldPreviewBuild.check();
      int var5 = 2 * (32 - Integer.numberOfLeadingZeros(var2 - var1));

      while (var2 - var1 > 1) {
         if (--var5 == 0) {
            Arrays.sort(var0, var1, var2, Comparator.comparingDouble(var4));
            return;
         }

         double var6 = var4.applyAsDouble(var0[var1]);
         double var8 = var4.applyAsDouble(var0[var1 + var2 >>> 1]);
         double var10 = var4.applyAsDouble(var0[var2 - 1]);
         double var12 = Double.compare(var6, var8) < 0
            ? (Double.compare(var8, var10) < 0 ? var8 : (Double.compare(var6, var10) < 0 ? var10 : var6))
            : (Double.compare(var6, var10) < 0 ? var6 : (Double.compare(var8, var10) < 0 ? var10 : var8));
         int var14 = var1;
         int var15 = var1;
         int var16 = var2;

         while (var15 < var16) {
            int var17 = Double.compare(var4.applyAsDouble(var0[var15]), var12);
            if (var17 < 0) {
               swap(var0, var14++, var15++);
            } else if (var17 > 0) {
               swap(var0, var15, --var16);
            } else {
               var15++;
            }
         }

         if (var3 < var14) {
            var2 = var14;
         } else {
            if (var3 < var16) {
               return;
            }

            var1 = var16;
         }

         HeldPreviewBuild.check();
      }
   }

   private static <T> void swap(T[] var0, int var1, int var2) {
      T var3 = var0[var1];
      var0[var1] = var0[var2];
      var0[var2] = var3;
   }
}
