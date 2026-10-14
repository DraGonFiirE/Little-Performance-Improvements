package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.common.block.little.tile.LittleTile;

@Mixin(LittleTile.class)
public abstract class HeldPreviewTileMixin {
   @Inject(method = {"addPlaceBoxes", "transform"}, at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"), require = 2)
   private void littleperformanceimprovements$checkHeldPreviewTile(CallbackInfo var1) {
      HeldPreviewBuild.check();
   }
}
