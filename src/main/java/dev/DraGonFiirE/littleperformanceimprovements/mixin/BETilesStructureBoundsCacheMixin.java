package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.client.render.StructureBoundsCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.exception.NotYetConnectedException;

@Mixin(BETiles.class)
public abstract class BETilesStructureBoundsCacheMixin {
   @Inject(method = "updateTiles(ZZ)V", at = @At("HEAD"))
   private void littleperformanceimprovements$invalidateUpdatedStructureBounds(boolean updateNeighbour, boolean rebuildFaces, CallbackInfo callback) {
      BETiles self = (BETiles)(Object)this;

      for (IParentCollection parent : self.groups()) {
         if (parent.isStructure()) {
            try {
               StructureBoundsCache.invalidate(parent.getStructure());
            } catch (CorruptedConnectionException | NotYetConnectedException var8) {
            }
         }
      }
   }
}
