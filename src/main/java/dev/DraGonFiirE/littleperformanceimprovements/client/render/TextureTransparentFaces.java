package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.List;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.littletiles.common.grid.LittleGrid;

public final class TextureTransparentFaces {
   public static List<RenderBox> retainAirTouching(List<RenderBox> var0, LittleGrid var1) {
      return TextureContactVisibility.retainAirTouching(var0, var1);
   }
}
