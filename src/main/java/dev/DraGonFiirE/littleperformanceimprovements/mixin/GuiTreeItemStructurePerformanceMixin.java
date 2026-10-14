package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiPreviewContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.client.level.little.FakeClientLevel;
import team.creative.littletiles.common.gui.AnimationPreview;
import team.creative.littletiles.common.gui.tool.blueprint.GuiTreeItemStructure;

@Mixin(GuiTreeItemStructure.class)
public abstract class GuiTreeItemStructurePerformanceMixin {
   @Inject(method = "lambda$refreshAnimation$2", at = @At("HEAD"))
   private void littleperformanceimprovements$beginExactBlueprintGuiPreview(FakeClientLevel level, CallbackInfoReturnable<AnimationPreview> callback) {
      BlueprintGuiPreviewContext.enter();
   }

   @Inject(method = "lambda$refreshAnimation$2", at = @At("RETURN"))
   private void littleperformanceimprovements$finishExactBlueprintGuiPreview(FakeClientLevel level, CallbackInfoReturnable<AnimationPreview> callback) {
      BlueprintGuiPreviewContext.exit();
   }

   @Inject(method = "lambda$refreshAnimation$2", at = @At(value = "INVOKE", target = "Ljava/lang/RuntimeException;<init>(Ljava/lang/Throwable;)V"))
   private void littleperformanceimprovements$abortExactBlueprintGuiPreview(FakeClientLevel level, CallbackInfoReturnable<AnimationPreview> callback) {
      BlueprintGuiPreviewContext.exit();
   }
}
