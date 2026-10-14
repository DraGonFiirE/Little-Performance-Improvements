package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.placement.PlacementResult;

@Mixin(PlacementResult.class)
public abstract class PlacementResultPerformanceMixin {
   @Redirect(
      method = "addPlacedTile",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;add(Lteam/creative/littletiles/common/grid/LittleGrid;Lteam/creative/littletiles/common/block/little/element/LittleElement;Ljava/lang/Iterable;)V"
      )
   )
   private void littleperformanceimprovements$deferPlacedPreviewGridNormalization(
      LittleGroup group, LittleGrid grid, LittleElement element, Iterable<LittleBox> boxes
   ) {
      LittleTile copy = boxes instanceof LittleTile tile ? tile : new LittleTile(element, boxes);
      group.addTileFast(grid, copy);
   }
}
