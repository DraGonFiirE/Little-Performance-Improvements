package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.placement.PreviewMode;

public final class ToolPreviewMode {
   private ToolPreviewMode() {
   }

   private static PreviewRenderingConfig config() {
      return (PreviewRenderingConfig)LittleTiles.CONFIG.rendering;
   }

   public static PreviewRendererMode current() {
      return config().littleperformanceimprovements$getPreviewRenderer();
   }

   public static PreviewMode littleTilesMode() {
      return current() == PreviewRendererMode.Outline ? PreviewMode.LINES : PreviewMode.PREVIEWS;
   }

   public static boolean shouldRender(boolean lines) {
      return current() == PreviewRendererMode.Default || lines == (current() == PreviewRendererMode.Outline);
   }

   public static void setup(PreviewRenderer renderer, boolean lines) {
      setup(renderer, lines, false);
      OutlinePreviewColor.apply();
   }

   public static void setup(PreviewRenderer renderer, boolean lines, boolean texturedMesh) {
      if (current() == PreviewRendererMode.Default) {
         renderer.setupPreviewRenderer(lines);
         OutlinePreviewColor.apply();
      } else {
         PreviewRenderingConfig config = config();
         Color color = config.littleperformanceimprovements$getPreviewColor();
         float red = color.getRedDecimal();
         float green = color.getGreenDecimal();
         float blue = color.getBlueDecimal();
         if (lines) {
            renderer.setupPreviewRendererLines(
               red,
               green,
               blue,
               (float)config.littleperformanceimprovements$getPreviewOutlineAlpha(),
               (float)config.littleperformanceimprovements$getPreviewOutlineThickness()
            );
            OutlinePreviewColor.apply();
         } else {
            renderer.setupPreviewRenderer(false);
            if (current() == PreviewRendererMode.Texture && texturedMesh) {
               RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
               RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            float alpha = (float)(
               current() == PreviewRendererMode.Texture
                  ? config.littleperformanceimprovements$getPreviewTextureAlpha()
                  : config.littleperformanceimprovements$getPreviewSurfaceAlpha()
            );
            RenderSystem.depthMask(alpha >= 0.999F);
            ToolTextureColor.apply(red, green, blue, alpha);
            OutlinePreviewColor.apply();
         }
      }
   }

   public static void reset() {
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.depthFunc(515);
      GL11.glLineWidth(1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
   }

   public static boolean drawExactOutline(VertexBuffer buffer) {
      if (current() != PreviewRendererMode.Outline) {
         return false;
      }

      PreviewRenderingConfig config = config();
      Color color = config.littleperformanceimprovements$getPreviewColor();
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      float alpha = (float)config.littleperformanceimprovements$getPreviewOutlineAlpha();
      float thickness = Math.max(1.0F, (float)config.littleperformanceimprovements$getPreviewOutlineThickness());
      RenderSystem.disableCull();

      boolean drawnByVeil;
      try {
         drawnByVeil = VeilPreviewLineRenderer.draw(buffer, thickness, red, green, blue, alpha);
      } finally {
         RenderSystem.enableCull();
      }

      if (drawnByVeil) {
         return true;
      }

      Matrix4f var10001 = RenderSystem.getModelViewMatrix();
      Matrix4f var10002 = RenderSystem.getProjectionMatrix();
      ShaderInstance var10003 = GameRenderer.getPositionColorShader();
      OutlinePreviewColor.apply();
      buffer.drawWithShader(var10001, var10002, var10003);
      return true;
   }
}
