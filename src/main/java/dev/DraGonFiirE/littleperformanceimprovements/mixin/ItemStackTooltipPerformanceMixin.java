package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.tooltip.LittleToolTooltipCache;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipPerformanceMixin {
   private static final ThreadLocal<Boolean> LITTLEPERFORMANCEIMPROVEMENTS_CACHE_HIT = ThreadLocal.withInitial(() -> false);

   @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$reuseLittleToolTooltip(
      TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> callback
   ) {
      LITTLEPERFORMANCEIMPROVEMENTS_CACHE_HIT.set(false);
      ItemStack stack = (ItemStack)(Object)this;
      if (LittleToolTooltipCache.supports(stack)) {
         List<Component> cached = LittleToolTooltipCache.lookup(stack, player, flag);
         if (cached != null) {
            LITTLEPERFORMANCEIMPROVEMENTS_CACHE_HIT.set(true);
            callback.setReturnValue(cached);
         }
      }
   }

   @Inject(method = "getTooltipLines", at = @At("RETURN"))
   private void littleperformanceimprovements$rememberLittleToolTooltip(
      TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> callback
   ) {
      if (LITTLEPERFORMANCEIMPROVEMENTS_CACHE_HIT.get()) {
         LITTLEPERFORMANCEIMPROVEMENTS_CACHE_HIT.set(false);
      } else {
         ItemStack stack = (ItemStack)(Object)this;
         LittleToolTooltipCache.store(stack, player, flag, callback.getReturnValue());
      }
   }
}
