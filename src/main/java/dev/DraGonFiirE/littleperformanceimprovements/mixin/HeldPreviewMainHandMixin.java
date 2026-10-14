package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class HeldPreviewMainHandMixin {
   @Inject(method = "tick", at = @At("HEAD"), require = 1)
   private void littleperformanceimprovements$observePreviewMainHand(CallbackInfo var1) {
      LocalPlayer var2 = ((Minecraft)(Object)this).player;
      HeldPreviewBuild.observe(var2 == null ? null : var2.getMainHandItem(), var2 == null ? -1 : var2.getInventory().selected);
   }
}
