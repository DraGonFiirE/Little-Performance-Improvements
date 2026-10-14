package dev.DraGonFiirE.littleperformanceimprovements.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ShaderPreviewPass;
@Mixin(GameRenderer.class)
public abstract class PreviewShaderCompositeMixin {
 @Inject(method="renderLevel",at=@At("HEAD"))
 private void littleperformanceimprovements$beginPreviewFrame(DeltaTracker delta,CallbackInfo ci){ShaderPreviewPass.beginFrame();}
 @Inject(method="renderLevel",at=@At("RETURN"))
 private void littleperformanceimprovements$compositePreview(DeltaTracker delta,CallbackInfo ci){ShaderPreviewPass.flush();}
}
