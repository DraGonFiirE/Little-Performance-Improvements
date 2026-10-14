package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import java.util.Arrays;

public final class OutlinePlaneOrder {
   public static int[] sort(int[] var0, double[] var1) {
      int var2 = var0.length;
      int[] var3 = new int[var2];
      int[] var4 = new int[var2];
      long[] var5 = new long[var2];

      for (int var6 = 0; var6 < var2; var6++) {
         if ((var6 & 8191) == 0) {
            HeldPreviewBuild.check();
         }

         var3[var6] = var6;
         long var7 = Double.doubleToLongBits(var1[var6]);
         var5[var6] = var7 < 0L ? ~var7 : var7 ^ Long.MIN_VALUE;
      }

      int[] var13 = new int[256];

      for (int var14 = 0; var14 < 9; var14++) {
         HeldPreviewBuild.check();
         Arrays.fill(var13, 0);
         int var8 = var14 * 8;

         for (int var9 = 0; var9 < var2; var9++) {
            if ((var9 & 8191) == 0) {
               HeldPreviewBuild.check();
            }

            int var10 = var3[var9];
            int var11 = var14 == 8 ? var0[var10] : (int)(var5[var10] >>> var8) & 0xFF;
            var13[var11]++;
         }

         int var15 = 0;

         for (int var16 = 0; var16 < 256; var16++) {
            int var19 = var13[var16];
            var13[var16] = var15;
            var15 += var19;
         }

         for (int var17 = 0; var17 < var2; var17++) {
            if ((var17 & 8191) == 0) {
               HeldPreviewBuild.check();
            }

            int var20 = var3[var17];
            int var12 = var14 == 8 ? var0[var20] : (int)(var5[var20] >>> var8) & 0xFF;
            var4[var13[var12]++] = var20;
         }

         int[] var18 = var3;
         var3 = var4;
         var4 = var18;
      }

      return var3;
   }
}
