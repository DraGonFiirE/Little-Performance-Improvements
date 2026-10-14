package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.client.render.item.LittleModelItemTiles;

@Mixin(LittleModelItemTiles.class)
public abstract class LittleModelItemTilesPerformanceMixin {
   private static final AtomicBoolean LITTLEEXTRATILES_LOGGED_LOW_RESOLUTION_ITEM = new AtomicBoolean();

   @Inject(method = "getBoxes", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$buildLargeBlueprintInventoryBoundsWithoutLoadingTiles(
      ItemStack stack, boolean translucent, CallbackInfoReturnable<List<? extends RenderBox>> callback
   ) {
      if (BlueprintPerformance.hasLargePayload(stack)) {

         if (translucent) {
            callback.setReturnValue(Collections.emptyList());
         } else {
            RenderBox bounds = new RenderBox(new AlignedBox(0.175F, 0.175F, 0.175F, 0.825F, 0.825F, 0.825F), LittleTilesRegistry.CLEAN.value());
            callback.setReturnValue(Collections.singletonList(bounds));
         }
      }
   }
}
