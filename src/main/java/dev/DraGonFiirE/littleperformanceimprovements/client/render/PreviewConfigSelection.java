package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.tool.shaper.LittleToolShaper;
/** Selects the settings used by the active preview, including shared mesh builders. */
public final class PreviewConfigSelection {
 private PreviewConfigSelection() {}
 public static boolean isShape() {
  return LittleTilesClient.PREVIEW_RENDERER != null && LittleTilesClient.PREVIEW_RENDERER.tool() instanceof LittleToolShaper;
 }
}

