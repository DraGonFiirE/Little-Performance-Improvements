package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.level.context.ILittleLevelContext;

public final class ChiselPreviewColor {
   public static void draw(PreviewRenderer var0, ILittleLevelContext var1, Vec3 var2, BlockPos var3, boolean var4, MeshData var5, Runnable var6) {
      ShaderInstance var7 = RenderSystem.getShader();
      Runnable var8 = DefaultCylinderGrid2.wrap(var5, var6);
      float[] var9 = RenderSystem.getShaderColor();
      float var10 = var9[0];
      float var11 = var9[1];
      float var12 = var9[2];
      float var13 = var9[3];

      try {
         WallPreviewDepth.draw(var0, var1, var2, var3, var4, var5, () -> {
            if (var8 != null) {
               var8.run();
            }

            Color var1x = ((PreviewRenderingConfig)LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewColor();
            if (var1x == null) {
               var1x = Color.WHITE;
            }

            RenderSystem.setShaderColor(var1x.getRedDecimal(), var1x.getGreenDecimal(), var1x.getBlueDecimal(), RenderSystem.getShaderColor()[3]);
            OutlinePreviewColor.apply();
         });
      } finally {
         DefaultCylinderGrid2.restore(var7);
         RenderSystem.setShaderColor(var10, var11, var12, var13);
      }
   }
}
