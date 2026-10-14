package dev.DraGonFiirE.littleperformanceimprovements.client.render;

final class OutlineFlatIndex {
   static final int STRIDE = 9;

   private OutlineFlatIndex() {
   }

   static boolean overlaps(double[] var0, int var1, double[] var2) {
      for (int var3 = 0; var3 < 3; var3++) {
         if (var0[var1 + var3] > var2[var3 + 3] + 2.0E-5 || var2[var3] > var0[var1 + var3 + 3] + 2.0E-5) {
            return false;
         }
      }

      return true;
   }

   static boolean near(double[] var0, int var1, double var2, double var4, double var6) {
      return var2 >= var0[var1] - 1.0E-4
         && var2 <= var0[var1 + 3] + 1.0E-4
         && var4 >= var0[var1 + 1] - 1.0E-4
         && var4 <= var0[var1 + 4] + 1.0E-4
         && var6 >= var0[var1 + 2] - 1.0E-4
         && var6 <= var0[var1 + 5] + 1.0E-4;
   }

   static boolean intersects(double[] var0, int var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      if ((var2 == var8 ? 0 : 1) + (var4 == var10 ? 0 : 1) + (var6 == var12 ? 0 : 1) <= 1) {
         return true;
      }

      double var14 = 0.0;
      double var16 = 1.0;

      for (int var18 = 0; var18 < 3; var18++) {
         double var19 = var18 == 0 ? var2 : (var18 == 1 ? var4 : var6);
         double var21 = var18 == 0 ? var8 : (var18 == 1 ? var10 : var12);
         double var23 = var0[var1 + var18] - 8.0E-5;
         double var25 = var0[var1 + var18 + 3] + 8.0E-5;
         double var27 = var21 - var19;
         if (var27 == 0.0) {
            if (var19 < var23 || var19 > var25) {
               return false;
            }
         } else {
            double var29 = (var23 - var19) / var27;
            double var31 = (var25 - var19) / var27;
            if (var29 > var31) {
               double var33 = var29;
               var29 = var31;
               var31 = var33;
            }

            var14 = Math.max(var14, var29);
            var16 = Math.min(var16, var31);
            if (var14 > var16) {
               return false;
            }
         }
      }

      return true;
   }
}
