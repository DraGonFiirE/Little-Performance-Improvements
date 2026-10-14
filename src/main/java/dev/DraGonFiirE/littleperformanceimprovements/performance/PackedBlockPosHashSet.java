package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.Arrays;
import java.util.HashSet;
import net.minecraft.core.BlockPos;

public final class PackedBlockPosHashSet extends HashSet<BlockPos> {
   private static final float LOAD_FACTOR = 0.6F;
   private long[] keys = new long[16];
   private int mask = this.keys.length - 1;
   private int maxFill = maxFill(this.keys.length);
   private int packedSize;
   private boolean containsZero;

   public boolean add(BlockPos position) {
      return this.addPacked(position.asLong());
   }

   @Override
   public boolean contains(Object value) {
      return value instanceof BlockPos position && this.containsPacked(position.asLong());
   }

   @Override
   public boolean remove(Object value) {
      return value instanceof BlockPos position && this.removePacked(position.asLong());
   }

   @Override
   public void clear() {
      if (this.packedSize != 0) {
         Arrays.fill(this.keys, 0L);
         this.packedSize = 0;
         this.containsZero = false;
      }
   }

   @Override
   public int size() {
      return this.packedSize;
   }

   @Override
   public boolean isEmpty() {
      return this.packedSize == 0;
   }

   public boolean addPacked(long value) {
      if (value == 0L) {
         if (this.containsZero) {
            return false;
         }

         this.containsZero = true;
         this.packedSize++;
         return true;
      } else {
         int index;
         long current;
         for (index = index(value, this.mask); (current = this.keys[index]) != 0L; index = index + 1 & this.mask) {
            if (current == value) {
               return false;
            }
         }

         this.keys[index] = value;
         if (++this.packedSize >= this.maxFill) {
            this.rehash(this.keys.length << 1);
         }

         return true;
      }
   }

   public boolean containsPacked(long value) {
      if (value == 0L) {
         return this.containsZero;
      }

      long current;
      for (int index = index(value, this.mask); (current = this.keys[index]) != 0L; index = index + 1 & this.mask) {
         if (current == value) {
            return true;
         }
      }

      return false;
   }

   private boolean removePacked(long value) {
      if (value == 0L) {
         if (!this.containsZero) {
            return false;
         }

         this.containsZero = false;
         this.packedSize--;
         return true;
      } else {
         long current;
         for (int index = index(value, this.mask); (current = this.keys[index]) != 0L; index = index + 1 & this.mask) {
            if (current == value) {
               this.shiftKeys(index);
               this.packedSize--;
               return true;
            }
         }

         return false;
      }
   }

   private void shiftKeys(int removed) {
      int index = removed;

      while (true) {
         int last = index;

         long current;
         for (index = index + 1 & this.mask; (current = this.keys[index]) != 0L; index = index + 1 & this.mask) {
            int slot = index(current, this.mask);
            if (last <= index ? last >= slot || slot > index : last >= slot && slot > index) {
               break;
            }
         }

         if (current == 0L) {
            this.keys[last] = 0L;
            return;
         }

         this.keys[last] = current;
      }
   }

   private void rehash(int capacity) {
      long[] oldKeys = this.keys;
      this.keys = new long[capacity];
      this.mask = capacity - 1;
      this.maxFill = maxFill(capacity);

      for (long value : oldKeys) {
         if (value != 0L) {
            int index = index(value, this.mask);

            while (this.keys[index] != 0L) {
               index = index + 1 & this.mask;
            }

            this.keys[index] = value;
         }
      }
   }

   private static int maxFill(int capacity) {
      return Math.min(capacity - 1, (int)Math.ceil(capacity * 0.6F));
   }

   private static int index(long value, int mask) {
      long mixed = value + -7046029254386353131L;
      mixed = (mixed ^ mixed >>> 30) * -4658895280553007687L;
      mixed = (mixed ^ mixed >>> 27) * -7723592293110705685L;
      mixed ^= mixed >>> 31;
      return (int)mixed & mask;
   }
}
