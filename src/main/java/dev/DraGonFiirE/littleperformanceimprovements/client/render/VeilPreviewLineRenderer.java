package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33C;

public final class VeilPreviewLineRenderer {
   private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("littleperformanceimprovements", "preview_line");
   private static boolean reportedMissingShader;

   private VeilPreviewLineRenderer() {
   }

   public static boolean draw(VertexBuffer var0, float var1, float var2, float var3, float var4, float var5) {
      var2 = OutlinePreviewColor.channel(0, var2);
      var3 = OutlinePreviewColor.channel(1, var3);
      var4 = OutlinePreviewColor.channel(2, var4);
      ShaderProgram var6 = VeilRenderSystem.setShader(SHADER);
      if (var6 != null && var6.isValid() && var6.hasGeometry()) {
         var6.setDefaultUniforms(Mode.DEBUG_LINES);
         var6.getUniformSafe("LineWidth").setFloat(var1);
         var6.getUniformSafe("ColorModulator").setVector(var2, var3, var4, var5);
         var6.bind();

         try (OutlineSurfaceTarget.Binding var7 = OutlineLineDepthTexture.capture()) {
            GL33C.glUniform1i(GL33C.glGetUniformLocation(var6.getProgram(), "OutlineSurface"), 0);
            GL33C.glUniform1i(GL33C.glGetUniformLocation(var6.getProgram(), "OutlineVertices"), 1);
            GL33C.glUniform3f(GL33C.glGetUniformLocation(var6.getProgram(), "OutlineCamera"), var7.cameraX, var7.cameraY, var7.cameraZ);
            SphereOutlineBatches.draw(var0);
         } finally {
            ShaderProgram.unbind();
         }

         return true;
      } else {
         if (!reportedMissingShader) {
            reportedMissingShader = true;
            LittlePerformanceImprovements.LOGGER.error("Veil preview-line shader {} is unavailable; using native one-pixel lines", SHADER);
         }

         return false;
      }
   }
}
