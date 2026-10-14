package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.item.ItemLittleBlueprint;
import team.creative.littletiles.common.math.vec.LittleVecGrid;

@Mixin(ItemLittleBlueprint.class)
public abstract class ItemLittleBlueprintPerformanceMixin {
   // These metadata consumers only read the content. In pre232 they call
   // getContent(), which defensively copies every box. The HUD repeatedly calls
   // getName while a selected item is displayed; preview sizing also uses these
   // accessors. This must not depend on the low-resolution rendering threshold.
   // Leave the public getContent contract and all mutation callers unchanged.
   @Redirect(
         method = { "getName", "shouldRenderInHand", "getCachedSize", "getCachedMin" },
         at = @At(value = "INVOKE",
               target = "Lteam/creative/littletiles/common/item/ItemLittleBlueprint;getContent(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/nbt/CompoundTag;"),
         require = 4)
   private CompoundTag littleperformanceimprovements$readMetadataWithoutCopy(ItemStack stack) {
      return BlueprintPerformance.content(stack);
   }

   @Inject(method = "getContent", at = @At("HEAD"), cancellable = true)
   private static void littleperformanceimprovements$readLargeBlueprintContentWithoutCopy(
         ItemStack stack, CallbackInfoReturnable<CompoundTag> callback) {
      if (BlueprintPerformance.hasLargePayload(stack))
         callback.setReturnValue(BlueprintPerformance.content(stack));
   }

   @Inject(method = "getLow", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$loadLargeLowPreviewWithoutCopy(
         ItemStack stack, CallbackInfoReturnable<LittleGroup> callback) {
      if (BlueprintPerformance.usesLowResolutionPreview(stack))
         callback.setReturnValue(LittleGroup.loadLow(BlueprintPerformance.content(stack)));
   }

   @Inject(method = "getCachedSize", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$readLargeCachedSizeWithoutCopy(
         ItemStack stack, CallbackInfoReturnable<LittleVecGrid> callback) {
      if (BlueprintPerformance.usesLowResolutionPreview(stack))
         callback.setReturnValue(LittleGroup.getSize(BlueprintPerformance.content(stack)));
   }

   @Inject(method = "getCachedMin", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$readLargeCachedMinWithoutCopy(
         ItemStack stack, CallbackInfoReturnable<LittleVecGrid> callback) {
      if (BlueprintPerformance.usesLowResolutionPreview(stack))
         callback.setReturnValue(LittleGroup.getMin(BlueprintPerformance.content(stack)));
   }
}
