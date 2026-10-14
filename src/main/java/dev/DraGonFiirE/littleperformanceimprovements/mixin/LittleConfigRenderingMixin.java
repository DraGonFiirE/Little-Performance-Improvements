package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.access.HeldItemRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreviewConfigSelection;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import org.spongepowered.asm.mixin.Mixin;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.api.CreativeConfig.DecimalRange;
import team.creative.creativecore.common.config.api.CreativeConfig.IntRange;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.common.config.LittleConfigRendering;

@Mixin(LittleConfigRendering.class)
public abstract class LittleConfigRenderingMixin implements PreviewRenderingConfig, HeldItemRenderingConfig {
   @CreativeConfig
   @IntRange(slider = false, min = 0, max = Integer.MAX_VALUE)
   public int heldItemSkipBoxCount = 1000;
   @CreativeConfig
   public PreviewRendererMode blueprintRenderer = PreviewRendererMode.Surface;
   @CreativeConfig
   public PreviewRendererMode shapeRenderer = PreviewRendererMode.Surface;
   @CreativeConfig
   public Color blueprintColor = new Color(255, 255, 255, 255);
   @CreativeConfig
   public Color shapeColor = new Color(255, 255, 255, 255);
   @CreativeConfig
   @DecimalRange(slider = false, min = 0.25, max = 16.0)
   public double previewOutlineThickness = 2.0;
   @CreativeConfig
   @DecimalRange(slider = false, min = 0.0, max = 1.0)
   public double previewOutlineAlpha = 1.0;
   @CreativeConfig
   @DecimalRange(slider = false, min = 0.0, max = 1.0)
   public double previewSurfaceAlpha = 0.5;
   @CreativeConfig
   @DecimalRange(slider = false, min = 0.0, max = 1.0)
   public double previewTextureAlpha = 1.0;

   @Override
   public int littleperformanceimprovements$getHeldItemSkipBoxCount() {
      return this.heldItemSkipBoxCount;
   }

   @Override
   public PreviewRendererMode littleperformanceimprovements$getPreviewRenderer() {
      return PreviewConfigSelection.isShape() ? this.shapeRenderer : this.blueprintRenderer;
   }

   @Override
   public Color littleperformanceimprovements$getPreviewColor() {
      return PreviewConfigSelection.isShape() ? this.shapeColor : this.blueprintColor;
   }

   @Override
   public PreviewRendererMode littleperformanceimprovements$getBlueprintRenderer() {
      return this.blueprintRenderer;
   }

   @Override
   public PreviewRendererMode littleperformanceimprovements$getShapeRenderer() {
      return this.shapeRenderer;
   }

   @Override
   public Color littleperformanceimprovements$getBlueprintColor() {
      return this.blueprintColor;
   }

   @Override
   public Color littleperformanceimprovements$getShapeColor() {
      return this.shapeColor;
   }

   @Override
   public double littleperformanceimprovements$getPreviewOutlineThickness() {
      return this.previewOutlineThickness;
   }

   @Override
   public double littleperformanceimprovements$getPreviewOutlineAlpha() {
      return this.previewOutlineAlpha;
   }

   @Override
   public double littleperformanceimprovements$getPreviewSurfaceAlpha() {
      return this.previewSurfaceAlpha;
   }

   @Override
   public double littleperformanceimprovements$getPreviewTextureAlpha() {
      return this.previewTextureAlpha;
   }
}
