package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
/** Applies the current preview tint without rebuilding the cached Default mesh. */
public final class DefaultPreviewColor {
 public static void draw(VertexBuffer buffer, Matrix4f view, Matrix4f projection, ShaderInstance shader) {
  float[] previous=RenderSystem.getShaderColor();
  float r=previous[0],g=previous[1],b=previous[2],a=previous[3];
  Color color=((PreviewRenderingConfig)(Object)LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewColor();
  if(color==null)color=Color.WHITE;
  try {
   RenderSystem.setShaderColor(color.getRedDecimal(),color.getGreenDecimal(),color.getBlueDecimal(),a);
   buffer.drawWithShader(view,projection,shader);
  } finally { RenderSystem.setShaderColor(r,g,b,a); }
 }
}
