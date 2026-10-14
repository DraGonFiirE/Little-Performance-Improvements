package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreparedPreviewCompaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.client.tool.LittleToolPlacer;

@Mixin(LittleToolPlacer.LittleGroupResult.class)
public abstract class PreparedPreviewCleanupMixin {
   @Inject(method = "close", at = @At("HEAD"), require = 1)
   private void littleperformanceimprovements$discardPreparedMesh(CallbackInfo callback) {
      PreparedPreviewCompaction.discard(((LittleToolPlacer.LittleGroupResult) (Object) this).data());
   }
}
