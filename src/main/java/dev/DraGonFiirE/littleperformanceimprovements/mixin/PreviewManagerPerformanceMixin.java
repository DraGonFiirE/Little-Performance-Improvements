package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.PersistentPreviewBufferOwner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.client.render.overlay.PreviewManager;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;

@Mixin(PreviewManager.class)
public class PreviewManagerPerformanceMixin {
   @Shadow
   @Final
   public PreviewRenderer renderer;

   @Inject(method = "clearToolPreviews", at = @At("HEAD"))
   private void littleperformanceimprovements$releasePersistentPreviewBuffer(CallbackInfo callback) {
      ((PersistentPreviewBufferOwner)(Object)this.renderer).littleperformanceimprovements$releasePersistentPreviewBuffer();
   }
}
