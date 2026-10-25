package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.tool.shaper.LittleToolShaper;
/** Selects the settings used by the active preview, including shared mesh builders. */
public final class PreviewConfigSelection {
 private PreviewConfigSelection() {}
 public static boolean isHammer() {
  return LittleTilesClient.PREVIEW_RENDERER != null
   && LittleTilesClient.PREVIEW_RENDERER.tool() instanceof LittleToolShaper shaper
   && shaper.stack.getItem() instanceof team.creative.littletiles.common.item.ItemLittleHammer;
 }
 public static boolean isShape() {
  return LittleTilesClient.PREVIEW_RENDERER != null && LittleTilesClient.PREVIEW_RENDERER.tool() instanceof LittleToolShaper;
 }
}

