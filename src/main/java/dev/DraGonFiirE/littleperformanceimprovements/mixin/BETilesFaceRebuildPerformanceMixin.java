package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.IndexedFaceRebuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.common.block.entity.BETiles;

@Mixin(BETiles.class)
public abstract class BETilesFaceRebuildPerformanceMixin {
   @Inject(method = "rebuildFaces", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$rebuildOrdinaryFacesFromPlaneIndex(CallbackInfo callback) {
      if (IndexedFaceRebuilder.rebuild((BETiles)(Object)this)) {
         callback.cancel();
      }
   }
}
