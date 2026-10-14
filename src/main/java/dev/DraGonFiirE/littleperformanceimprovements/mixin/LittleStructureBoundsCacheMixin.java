package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.StructureBoundsCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.math.box.SurroundingBox;
import team.creative.littletiles.common.structure.LittleStructure;

@Mixin(LittleStructure.class)
public abstract class LittleStructureBoundsCacheMixin {
   @Inject(method = "getSurroundingBox", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$reuseStructureSelectionBounds(CallbackInfoReturnable<SurroundingBox> callback) {
      SurroundingBox cached = StructureBoundsCache.get((LittleStructure)(Object)this);
      if (cached != null) {
         callback.setReturnValue(cached);
      }
   }

   @Inject(method = "getSurroundingBox", at = @At("RETURN"))
   private void littleperformanceimprovements$cacheStructureSelectionBounds(CallbackInfoReturnable<SurroundingBox> callback) {
      StructureBoundsCache.put((LittleStructure)(Object)this, callback.getReturnValue());
   }

   @Inject(method = "markDirty", at = @At("HEAD"))
   private void littleperformanceimprovements$invalidateStructureSelectionBounds(CallbackInfo callback) {
      StructureBoundsCache.invalidate((LittleStructure)(Object)this);
   }
}
