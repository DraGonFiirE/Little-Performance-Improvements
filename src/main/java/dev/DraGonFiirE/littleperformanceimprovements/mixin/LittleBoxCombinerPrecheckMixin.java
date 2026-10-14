package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.ExactLittleBoxCombiner;
import dev.DraGonFiirE.littleperformanceimprovements.performance.LittleBoxCombinePrecheck;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleBoxCombiner;

@Mixin(LittleBoxCombiner.class)
public abstract class LittleBoxCombinerPrecheckMixin {
   @Inject(method = "combine", at = @At("HEAD"), cancellable = true)
   private static void littleperformanceimprovements$skipAlreadyIrreducibleBoxLists(List<LittleBox> boxes, CallbackInfoReturnable<Boolean> callback) {
      Boolean indexedResult = ExactLittleBoxCombiner.combine(boxes);
      if (indexedResult != null) {
         callback.setReturnValue(indexedResult);
      } else {
         if (LittleBoxCombinePrecheck.canSkip(boxes)) {
            callback.setReturnValue(false);
         }
      }
   }
}
