package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiIconCache;
import dev.DraGonFiirE.littleperformanceimprovements.access.HeldItemRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import team.creative.littletiles.LittleTiles;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import team.creative.creativecore.client.render.model.box.CreativeBakedBoxItemLayer;
import team.creative.creativecore.client.render.model.box.ItemModelBox;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.client.render.item.LittleModelItemTiles;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

@Mixin(CreativeBakedBoxItemLayer.class)
public abstract class CreativeBakedBoxItemLayerPerformanceMixin {
   @Shadow
   public abstract ItemModelBox model();

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$renderCachedPre225BlueprintGuiLayer(
         ItemRenderer itemRenderer,
         ItemStack stack,
         ItemDisplayContext context,
         boolean leftHand,
         PoseStack pose,
         MultiBufferSource buffers,
         int packedLight,
         int packedOverlay,
         CallbackInfo callback) {
      // Cancel only the miniature's box layer; the paper layer still renders.
      boolean held = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
      if (held && stack.getItem() instanceof ItemLittleBlueprint
            && this.model() instanceof LittleModelItemTiles) {
         int threshold = ((HeldItemRenderingConfig) (Object) LittleTiles.CONFIG.rendering)
               .littleperformanceimprovements$getHeldItemSkipBoxCount();
         // Zero disables skipping. Read the setting each draw so GUI changes
         // take effect immediately, independently of existing mesh caches.
         if (threshold > 0 && BlueprintPerformance.boxCount(stack) >= threshold) {
            callback.cancel();
            return;
         }
      }
      boolean cachedContext = context == ItemDisplayContext.GUI
            || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
      if (cachedContext
            && stack.getItem() instanceof ItemLittleBlueprint
            && !stack.hasFoil()
            && this.model() instanceof LittleModelItemTiles
            && BlueprintGuiIconCache.renderBoxLayer(
                  pose,
                  (CreativeBakedBoxItemLayer) (Object) this,
                  itemRenderer,
                  stack,
                  context,
                  leftHand,
                  packedLight,
                  packedOverlay))
         callback.cancel();
   }

   @ModifyArg(
         method = "renderModelLists",
         at = @At(
               value = "INVOKE",
               target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V"),
         index = 3)
   private ItemStack littleperformanceimprovements$useTileColorsForBlueprintGeometry(ItemStack tintStack) {
      // CreativeBakedQuad stores the tile's ARGB color in the tint-index field.
      // Passing the blueprint stack makes its parchment ItemColor reinterpret
      // every non-zero ARGB value as the blueprint's secondary brown layer.
      // ITEM_TILES uses CreativeCore's identity ItemColor and preserves ARGB.
      if (this.model() instanceof LittleModelItemTiles)
         return new ItemStack(LittleTilesRegistry.ITEM_TILES.value());
      return tintStack;
   }

   @ModifyArgs(
         method = "render",
         at = @At(
               value = "INVOKE",
               target = "Lteam/creative/creativecore/client/render/model/box/CreativeBakedBoxItemLayer;renderLayer(Lnet/minecraft/client/renderer/entity/ItemRenderer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V"))
   private void littleperformanceimprovements$restoreLegacyGuiLighting(
         Args args,
         ItemRenderer itemRenderer,
         ItemStack stack,
         ItemDisplayContext context,
         boolean leftHand,
         PoseStack pose,
         MultiBufferSource buffers,
         int packedLight,
         int packedOverlay) {
      if (context == ItemDisplayContext.GUI && this.model() instanceof LittleModelItemTiles)
         args.set(6, LightTexture.FULL_BRIGHT);
   }
}
