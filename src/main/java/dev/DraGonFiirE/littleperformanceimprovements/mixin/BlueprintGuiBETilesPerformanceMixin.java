package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintGuiPreviewContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.face.LittleFaceState;

@Mixin(BETiles.class)
public abstract class BlueprintGuiBETilesPerformanceMixin {
   @Inject(method = "rebuildFaces", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$initializeExactBlueprintGuiFaces(CallbackInfo callback) {
      if (BlueprintGuiPreviewContext.active()) {
         BETiles self = (BETiles)(Object)this;

         for (Pair<IParentCollection, LittleTile> pair : self.allTiles()) {
            for (LittleBox box : pair.value) {
               for (Facing facing : Facing.VALUES) {
                  box.setFaceState(facing, LittleFaceState.INSIDE_UNCOVERED);
               }
            }
         }

         callback.cancel();
      }
   }
}
