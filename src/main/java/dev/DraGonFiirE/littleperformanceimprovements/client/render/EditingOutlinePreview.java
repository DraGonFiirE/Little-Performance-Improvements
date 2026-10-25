package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.tool.shaper.LittleToolShaper;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.item.ItemLittleHammer;
import team.creative.littletiles.common.placement.mode.PlacementMode;

/** Editing tools share see-through outlines, but retain their own configured colors. */
public final class EditingOutlinePreview {
   private EditingOutlinePreview() {}

   public static boolean uses(ItemStack stack) {
      if (stack.getItem() instanceof ItemLittleHammer) return true;
      if (!(stack.getItem() instanceof ItemLittleChisel) || LittleTilesClient.ACTION_HANDLER == null
            || LittleTilesClient.ACTION_HANDLER.setting == null) return false;
      var mode = LittleTilesClient.ACTION_HANDLER.setting.placementMode();
      return mode == PlacementMode.REPLACE || mode == PlacementMode.STENCIL || mode == PlacementMode.COLORIZE;
   }

   public static String cacheKey(String key, ItemStack stack) {
      return key + "|editing=" + uses(stack);
   }

   public static boolean active() {
      return LittleTilesClient.PREVIEW_RENDERER != null
         && LittleTilesClient.PREVIEW_RENDERER.tool() instanceof LittleToolShaper shaper
         && uses(shaper.stack);
   }
}
