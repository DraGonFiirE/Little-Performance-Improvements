package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.Arrays;

public final class OutlineDoubleCuts {
   private static final ThreadLocal<OutlineDoubleCuts> LOCAL = ThreadLocal.withInitial(OutlineDoubleCuts::new);
   private double[] values = new double[32];
   private int size;

   public static OutlineDoubleCuts acquire() {
      OutlineDoubleCuts var0 = LOCAL.get();
      var0.size = 0;
      return var0;
   }

   public static void release() {
      LOCAL.remove();
   }

   public void add(double var1) {
      if (this.size == this.values.length) {
         this.values = Arrays.copyOf(this.values, this.size * 2);
      }

      this.values[this.size++] = var1;
   }

   public void sort() {
      Arrays.sort(this.values, 0, this.size);
   }

   public int size() {
      return this.size;
   }

   public double get(int var1) {
      return this.values[var1];
   }
}
