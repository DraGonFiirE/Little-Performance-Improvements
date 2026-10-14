package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;import team.creative.littletiles.LittleTiles;import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
public final class OutlinePreviewColor {
 public static int rgb(){var c=((PreviewRenderingConfig)(Object)LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewColor();if(c==null)return 0;int r=Math.round(c.getRedDecimal()*255),g=Math.round(c.getGreenDecimal()*255),b=Math.round(c.getBlueDecimal()*255);return r==255&&g==255&&b==255?0:(r<<16)|(g<<8)|b;}
 public static float channel(int channel,float original){if(ToolPreviewMode.current()!=PreviewRendererMode.Outline)return original;return ((rgb()>>(16-channel*8))&255)/255f;}
 public static void apply(){if(ToolPreviewMode.current()!=PreviewRendererMode.Outline)return;int c=rgb();RenderSystem.setShaderColor(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,RenderSystem.getShaderColor()[3]);}
}
