package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiPreviewContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "team.creative.littletiles.common.placement.Placement$PlacementBlock")
public abstract class BlueprintGuiPlacementBlockPerformanceMixin {
   @Inject(method = "combineTilesSecretly", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$skipRedundantBlueprintGuiCombining(CallbackInfoReturnable<Boolean> callback) {
      if (BlueprintGuiPreviewContext.active()) {
         callback.setReturnValue(false);
      }
   }
}
