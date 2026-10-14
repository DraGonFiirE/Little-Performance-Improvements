package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.world.item.ItemStack;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
/** The preview selection includes the hovered point: two entries mean the first point is fixed. */
public final class SurfaceDragThreshold {
 public static boolean large(ShapeSelection selection,ItemStack stack){return selection!=null&&selection.size()>=2;}
}
