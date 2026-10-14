package dev.DraGonFiirE.littleperformanceimprovements.blueprint;

import dev.DraGonFiirE.littleperformanceimprovements.mixin.IntArrayTagAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public final class BlueprintPerformance {
   private static final String BOX_COUNT_KEY = "boxes";

   private BlueprintPerformance() {
   }

   public static boolean usesLowResolutionPreview(ItemStack stack) {
      if (!(stack.getItem() instanceof ItemLittleBlueprint)) {
         return false;
      }

      int boxes = boxCount(stack);
      return boxes > 0 && boxes >= LittleTiles.CONFIG.building.lowResolutionBoxCount;
   }

   public static boolean hasLargePayload(ItemStack stack) {
      int boxes = boxCount(stack);
      return boxes > 0 && boxes >= LittleTiles.CONFIG.rendering.itemLowResolutionBoxCount;
   }

   public static int cheapContentHash(ItemStack stack, CustomData data) {
      return hasLargePayload(stack) ? stableMetadataHash(content(stack)) : data.hashCode();
   }

   private static int stableMetadataHash(CompoundTag content) {
      int hash = 31 * content.getInt("boxes") + content.getInt("tiles");
      hash = hashIntArrayTag(hash, content.get("size"));
      return hashIntArrayTag(hash, content.get("min"));
   }

   private static int hashIntArrayTag(int hash, Tag tag) {
      if (!(tag instanceof IntArrayTag arrayTag)) {
         return hash;
      } else {
         int[] values = ((IntArrayTagAccessor)arrayTag).littleperformanceimprovements$getData();

         for (int value : values) {
            hash = 31 * hash + value;
         }

         return hash;
      }
   }

   public static CompoundTag content(ItemStack stack) {
      CustomData data = stack.get(LittleTilesRegistry.DATA);
      if (data == null) {
         return new CompoundTag();
      }

      CompoundTag root = data.getUnsafe();
      CompoundTag content = root;
      if (root.contains("c") && !root.contains("boxes")) {
         content = root.getCompound("c");
      }

      return content;
   }

   public static int boxCount(ItemStack stack) {
      return content(stack).getInt("boxes");
   }
}
