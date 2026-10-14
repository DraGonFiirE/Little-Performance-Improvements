package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.api.common.tool.ILittleTool;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenPerformanceMixin {
   @Redirect(
      method = "getTooltipFromContainerItem",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;contains(Lnet/minecraft/world/item/ItemStack;)Z")
   )
   private boolean littleperformanceimprovements$skipCreativeTabMembershipHashForConfiguredLittleTool(CreativeModeTab tab, ItemStack stack) {
      return stack.getItem() instanceof ILittleTool && ILittleTool.hasData(stack) ? false : tab.contains(stack);
   }
}
