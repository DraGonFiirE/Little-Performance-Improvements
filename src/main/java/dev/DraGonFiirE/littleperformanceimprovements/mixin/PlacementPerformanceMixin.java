package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.placement.Placement;
import team.creative.littletiles.common.placement.PlacementResult;

@Mixin(Placement.class)
public abstract class PlacementPerformanceMixin {
   @Inject(method = "placeTiles", at = @At("RETURN"))
   private void littleperformanceimprovements$normalizePlacedPreviewsOnce(CallbackInfoReturnable<PlacementResult> callback) {
      PlacementResult result = callback.getReturnValue();
      if (result != null) {
         result.placedPreviews.convertToSmallest();
      }
   }
}
