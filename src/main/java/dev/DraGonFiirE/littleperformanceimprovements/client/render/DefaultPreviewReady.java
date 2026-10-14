package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
public final class DefaultPreviewReady {
 public static boolean draw(ItemStack stack,PreviewRenderer.BoxRenderResult result,MeshData mesh){
  if(ChiselOnlyPreviewMode.current(stack)!=PreviewRendererMode.Default)return true;
  if(result!=null&&result.data()==mesh)return true;
  // This is the uncached, one-frame native bounding-box mesh, not the worker result.
  if(mesh!=null)mesh.close();return false;
 }
}
