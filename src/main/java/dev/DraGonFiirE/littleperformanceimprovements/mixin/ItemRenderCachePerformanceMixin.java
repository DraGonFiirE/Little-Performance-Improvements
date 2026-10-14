package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiIconCache;
import dev.DraGonFiirE.littleperformanceimprovements.client.tooltip.LittleToolTooltipCache;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.render.item.ItemRenderCache;

@Mixin(ItemRenderCache.class)
public abstract class ItemRenderCachePerformanceMixin {
   @Inject(method = {"clearCache", "unload"}, at = @At("HEAD"))
   private void littleperformanceimprovements$clearBlueprintGuiIconCache(CallbackInfo callback) {
      BlueprintGuiIconCache.clear();
      LittleToolTooltipCache.clear();
   }

   @Inject(method = "hasTranslucent", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$skipLargePayloadTranslucencyLookup(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
      if (!ILittleTool.hasData(stack) || BlueprintPerformance.hasLargePayload(stack)) {
         callback.setReturnValue(false);
      }
   }

   @Inject(method = "requestCache", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$skipLargePayloadItemMeshCache(
      ItemStack stack, boolean translucent, CallbackInfoReturnable<List<BakedQuad>> callback
   ) {
      if (!ILittleTool.hasData(stack) || BlueprintPerformance.hasLargePayload(stack)) {
         callback.setReturnValue(null);
      }
   }
}
