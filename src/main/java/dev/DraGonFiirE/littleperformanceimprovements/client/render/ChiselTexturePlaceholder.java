package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;

public final class ChiselTexturePlaceholder {
   private static String key;
   private static BoxRenderResult cached;

   public static MeshData prepare(MeshData var0, PreviewRenderer var1, ShapeSelection var2, ItemStack var3, BoxRenderResult var4, boolean var5) {
      return FastChiselTexturePlaceholder.prepare(var0, var1, var2, var3, var4, var5);
   }
}
