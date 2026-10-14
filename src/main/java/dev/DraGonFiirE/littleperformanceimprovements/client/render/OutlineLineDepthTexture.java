package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import org.lwjgl.opengl.GL33C;

public final class OutlineLineDepthTexture {
   private static boolean failed;

   private OutlineLineDepthTexture() {
   }

   public static boolean captureSurface(VertexBuffer var0) {
      if (failed) {
         OutlineSurfaceTarget.invalidate();
         return false;
      }

      try {
         var0.bind();
         int var1 = GL33C.glGetVertexAttribi(0, 34975);
         OutlineSurfaceTarget.capture(() -> {
            var0.bind();
            var0.draw();
         }, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), var1);
         return OutlineSurfaceTarget.resolve(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
      } catch (RuntimeException var2) {
         failed = true;
         OutlineSurfaceTarget.invalidate();
         LittlePerformanceImprovements.LOGGER.error("Precise outline depth unavailable; keeping normal outline rendering", var2);
         return false;
      }
   }

   public static OutlineSurfaceTarget.Binding capture() {
      return OutlineSurfaceTarget.bind();
   }

   public static void invalidate() {
      OutlineSurfaceTarget.invalidate();
   }

   public static void release() {
      OutlineSurfaceTarget.release();
      failed = false;
   }
}
