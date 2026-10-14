package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreviewFanTriangulation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;

@Mixin(RenderBox.class)
public abstract class RenderBoxPreviewFacingMixin {
   @Shadow
   protected abstract Object getRenderQuads(Facing var1);

   @Redirect(
      method = "renderPreview",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/creativecore/client/render/box/RenderBox;getRenderQuads(Lteam/creative/creativecore/common/util/math/base/Facing;)Ljava/lang/Object;"
      )
   )
   private Object littleperformanceimprovements$capturePreviewFacing(RenderBox instance, Facing facing) {
      PreviewFanTriangulation.currentFacing(facing.ordinal());
      return this.getRenderQuads(facing);
   }
}
