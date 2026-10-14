package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.common.item.ItemLittleChisel;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.level.context.ILittleLevelContext;
public final class ChiselOnlyPreviewMode {
 public static PreviewRendererMode current(ItemStack stack) {
  return stack.getItem() instanceof ItemLittleChisel ? ToolPreviewMode.current() : PreviewRendererMode.Default;
 }
 public static void draw(PreviewRenderer renderer, ILittleLevelContext context, Vec3 camera, BlockPos pos, boolean lines, MeshData mesh, Runnable setup, ItemStack stack) {
  if(stack.getItem() instanceof ItemLittleChisel)ChiselPreviewColor.draw(renderer,context,camera,pos,lines,mesh,setup);
  else renderer.renderBoxes(context,camera,pos,lines,mesh,setup);
 }
}
