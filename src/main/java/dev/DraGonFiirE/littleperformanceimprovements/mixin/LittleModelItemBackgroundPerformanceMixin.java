package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiIconCache;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

@Mixin(targets = "team.creative.littletiles.client.render.item.LittleModelItemBackground")
public abstract class LittleModelItemBackgroundPerformanceMixin {
   @Shadow
   protected abstract ItemStack getFakeStack(ItemStack var1);

   @Shadow
   public abstract void prepareRenderer(ItemDisplayContext var1, PoseStack var2);

   @Inject(method = "applyCustomOpenGLHackery", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$renderCachedBlueprintGuiContentOverlay(
      PoseStack pose, ItemStack stack, ItemDisplayContext context, CallbackInfo callback
   ) {
      if (context == ItemDisplayContext.GUI
         && stack.getItem() instanceof ItemLittleBlueprint
         && BlueprintGuiIconCache.render(pose, stack, this::getFakeStack, this::prepareRenderer)) {
         callback.cancel();
      }
   }

   @Inject(method = "getFakeStack", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$skipLargeBlueprintTileOverlay(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
      if (BlueprintPerformance.hasLargePayload(stack)) {
         callback.setReturnValue(ItemStack.EMPTY);
      }
   }
}
