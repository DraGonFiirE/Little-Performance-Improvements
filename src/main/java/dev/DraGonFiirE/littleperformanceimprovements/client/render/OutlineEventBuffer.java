package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.Arrays;

public final class OutlineEventBuffer {
   private long[] values = new long[6];
   private int size;

   public int size() {
      return this.size;
   }

   public long at(int var1) {
      return this.values[var1 * 3];
   }

   public int positive(int var1) {
      return (int)this.values[var1 * 3 + 1];
   }

   public int negative(int var1) {
      return (int)this.values[var1 * 3 + 2];
   }

   public void add(long var1, int var3, int var4) {
      int var5 = this.size * 3;
      if (var5 + 3 > this.values.length) {
         this.values = Arrays.copyOf(this.values, this.values.length * 2);
      }

      this.values[var5] = var1;
      this.values[var5 + 1] = var3;
      this.values[var5 + 2] = var4;
      this.size++;
   }

   public void addAll(OutlineEventBuffer var1) {
      int var2 = (this.size + var1.size) * 3;
      if (var2 > this.values.length) {
         this.values = Arrays.copyOf(this.values, var2);
      }

      System.arraycopy(var1.values, 0, this.values, this.size * 3, var1.size * 3);
      this.size = this.size + var1.size;
   }

   private void swap(int var1, int var2) {
      for (int var3 = 0; var3 < 3; var3++) {
         long var4 = this.values[var1 * 3 + var3];
         this.values[var1 * 3 + var3] = this.values[var2 * 3 + var3];
         this.values[var2 * 3 + var3] = var4;
      }
   }

   private void sift(int var1, int var2) {
      while (var1 * 2 + 1 < var2) {
         int var3 = var1 * 2 + 1;
         if (var3 + 1 < var2 && this.at(var3) < this.at(var3 + 1)) {
            var3++;
         }

         if (this.at(var1) >= this.at(var3)) {
            return;
         }

         this.swap(var1, var3);
         var1 = var3;
      }
   }

   public void sort() {
      if (this.size > 16) {
         for (int var3 = this.size / 2 - 1; var3 >= 0; var3--) {
            if ((var3 & 1023) == 0) {
               OutlineWorkChunks.check();
            }

            this.sift(var3, this.size);
         }

         for (int var4 = this.size - 1; var4 > 0; var4--) {
            if ((var4 & 1023) == 0) {
               OutlineWorkChunks.check();
            }

            this.swap(0, var4);
            this.sift(0, var4);
         }
      } else {
         for (int var1 = 1; var1 < this.size; var1++) {
            for (int var2 = var1; var2 > 0 && this.at(var2 - 1) > this.at(var2); var2--) {
               this.swap(var2 - 1, var2);
            }
         }
      }
   }
}
