package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiIconCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.client.render.model.layer.CreativeBakedItemLayer;
import team.creative.creativecore.client.render.model.layer.CreativeBakedLayerModel;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

@Mixin(CreativeBakedLayerModel.class)
public abstract class CreativeBakedLayerModelPerformanceMixin {
   @Shadow
   @Final
   public ImmutableList<CreativeBakedItemLayer> layers;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$renderCombinedCachedBlueprintGuiIcon(
      ItemRenderer itemRenderer,
      ItemStack stack,
      ItemDisplayContext context,
      boolean leftHand,
      PoseStack pose,
      MultiBufferSource buffers,
      int packedLight,
      int packedOverlay,
      CallbackInfo callback
   ) {
      if (context == ItemDisplayContext.GUI
         && stack.getItem() instanceof ItemLittleBlueprint
         && BlueprintGuiIconCache.renderLayeredIcon(pose, this.layers, itemRenderer, stack, context, leftHand, packedOverlay)) {
         callback.cancel();
      }
   }
}
