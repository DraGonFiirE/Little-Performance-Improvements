package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.block.little.tile.group.LittleGroupAbsolute;
import team.creative.littletiles.common.grid.LittleGrid;

@Mixin(LittleGroupAbsolute.class)
public abstract class LittleGroupAbsolutePerformanceMixin {
   @Redirect(
      method = "add(Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;Lnet/minecraft/core/BlockPos;Lteam/creative/littletiles/common/block/little/tile/parent/IParentCollection;Lteam/creative/littletiles/common/block/little/tile/LittleTile;)V",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;addTile(Lteam/creative/littletiles/common/grid/LittleGrid;Lteam/creative/littletiles/common/block/little/tile/LittleTile;)V"
      )
   )
   private static void littleperformanceimprovements$deferPreviewNormalization(LittleGroup group, LittleGrid grid, LittleTile tile) {
      group.addTileFast(grid, tile);
   }
}
