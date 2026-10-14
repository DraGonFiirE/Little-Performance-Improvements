package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiPreviewContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.ParentCollection;
import team.creative.littletiles.common.placement.PlacementResult;
import team.creative.littletiles.common.structure.LittleStructureType;

@Mixin(PlacementResult.class)
public abstract class BlueprintGuiPlacementResultPerformanceMixin {
   @Inject(method = "addPlacedTile", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$skipUnusedBlueprintGuiPlacementResults(
      LittleStructureType type, ParentCollection parent, LittleTile tile, CallbackInfo callback
   ) {
      if (BlueprintGuiPreviewContext.active()) {
         callback.cancel();
      }
   }
}
