package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
public final class ToolTextureColor {
 public static void apply(float r,float g,float b,float alpha) {
  if(ToolPreviewMode.current()==PreviewRendererMode.Texture)RenderSystem.setShaderColor(1,1,1,alpha);
  else RenderSystem.setShaderColor(r,g,b,alpha);
 }
}
