package dev.DraGonFiirE.littleperformanceimprovements.access;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import team.creative.creativecore.common.util.type.Color;
public interface PreviewRenderingConfig {
 PreviewRendererMode littleperformanceimprovements$getBlueprintRenderer();
 PreviewRendererMode littleperformanceimprovements$getShapeRenderer();
 Color littleperformanceimprovements$getBlueprintColor();
 Color littleperformanceimprovements$getShapeColor();
 /** Settings for the active preview tool, shared by its render and build paths. */
 PreviewRendererMode littleperformanceimprovements$getPreviewRenderer();
 Color littleperformanceimprovements$getPreviewColor();
 double littleperformanceimprovements$getPreviewOutlineThickness();
 double littleperformanceimprovements$getPreviewOutlineAlpha();
 double littleperformanceimprovements$getPreviewSurfaceAlpha();
 double littleperformanceimprovements$getPreviewTextureAlpha();
}
