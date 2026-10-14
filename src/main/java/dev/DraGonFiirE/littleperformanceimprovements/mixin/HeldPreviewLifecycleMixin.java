package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewCleanup;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.LittleToolPlacer;
import team.creative.littletiles.client.tool.LittleToolPlacer.LittleGroupResult;

@Mixin(LittleToolPlacer.class)
public abstract class HeldPreviewLifecycleMixin {
   @Shadow
   protected abstract void removeCache();

   @Redirect(
      method = "buildCache",
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
      ),
      require = 1
   )
   private CompletableFuture<LittleGroupResult> littleperformanceimprovements$submitHeldPreview(Supplier<LittleGroupResult> var1, Executor var2) {
      LocalPlayer var3 = Minecraft.getInstance().player;
      LittleToolPlacer var4 = (LittleToolPlacer)(Object)this;
      return var3 != null && var3.getMainHandItem() == var4.stack
         ? HeldPreviewBuild.submit(
            this,
            var3.getMainHandItem(),
            var3.getInventory().selected,
            var1,
            LittleGroupResult::close,
            () -> ((HeldPreviewCleanup)(Object)this).littleperformanceimprovements$cleanupHeldPreview(),
            var2
         )
         : CompletableFuture.failedFuture(new CompletionException(new CancellationException("Blueprint is not held")));
   }

   @Inject(method = "removeCache", at = @At("HEAD"), require = 1)
   private void littleperformanceimprovements$cancelHeldPreview(CallbackInfo var1) {
      HeldPreviewBuild.cancel(this);
   }

   @Inject(method = "tickInternal", at = @At("HEAD"), require = 1)
   private void littleperformanceimprovements$validateHeldPreview(PreviewRenderer var1, CallbackInfo var2) {
      LocalPlayer var3 = Minecraft.getInstance().player;
      if (HeldPreviewBuild.changed(this, var3 == null ? null : var3.getMainHandItem(), var3 == null ? -1 : var3.getInventory().selected)) {
         this.removeCache();
      }
   }

   @Redirect(
      method = "lambda$buildCache$1",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/render/overlay/PreviewRenderer;createBuffer()Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;"
      ),
      require = 1
   )
   private ByteBufferBuilder littleperformanceimprovements$trackHeldPreviewBuffer(PreviewRenderer var1) {
      HeldPreviewBuild.check();
      return HeldPreviewBuild.track(var1.createBuffer());
   }
}
