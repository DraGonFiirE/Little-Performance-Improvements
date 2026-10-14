package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.PackedBlockPosHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import team.creative.creativecore.common.level.NeighborUpdateCollector;
import team.creative.creativecore.common.util.type.map.HashMapList;

@Mixin(NeighborUpdateCollector.class)
public abstract class NeighborUpdateCollectorPerformanceMixin {
   @Shadow
   @Final
   private HashMapList<Level, BlockPos> blocksToUpdate;
   @Unique
   private PackedBlockPosHashSet littleperformanceimprovements$scheduledPositions;

   @Overwrite
   protected void processPosition(Level level, BlockPos pos, HashSet<BlockPos> notifiedBlocks) {
      BlockState source = level.getBlockState(pos);

      for (Direction direction : Direction.values()) {
         BlockPos neighbor = pos.relative(direction);
         if ((
               this.littleperformanceimprovements$scheduledPositions != null
                  ? !this.littleperformanceimprovements$scheduledPositions.contains(neighbor)
                  : !this.blocksToUpdate.contains(neighbor)
            )
            && !notifiedBlocks.contains(neighbor)) {
            level.getBlockState(neighbor).handleNeighborChanged(level, neighbor, source.getBlock(), pos, false);
            notifiedBlocks.add(neighbor);
         }
      }
   }

   @Overwrite
   public void process(Level level) {
      ArrayList<BlockPos> positions = this.blocksToUpdate.removeKey(level);
      if (positions != null && !positions.isEmpty()) {
         PackedBlockPosHashSet processed = new PackedBlockPosHashSet();
         PackedBlockPosHashSet notified = new PackedBlockPosHashSet();
         this.littleperformanceimprovements$scheduledPositions = new PackedBlockPosHashSet();

         for (BlockPos position : positions) {
            this.littleperformanceimprovements$scheduledPositions.add(position);
         }

         try {
            for (BlockPos position : positions) {
               if (processed.add(position)) {
                  this.processPosition(level, position, notified);
               }
            }
         } finally {
            this.littleperformanceimprovements$scheduledPositions = null;
         }
      }
   }

   @Overwrite
   public void process() {
      PackedBlockPosHashSet processed = new PackedBlockPosHashSet();
      PackedBlockPosHashSet notified = new PackedBlockPosHashSet();

      for (Entry<Level, ArrayList<BlockPos>> entry : this.blocksToUpdate.entrySet()) {
         processed.clear();
         notified.clear();
         this.littleperformanceimprovements$scheduledPositions = new PackedBlockPosHashSet();

         for (BlockPos position : entry.getValue()) {
            this.littleperformanceimprovements$scheduledPositions.add(position);
         }

         try {
            for (BlockPos position : entry.getValue()) {
               if (processed.add(position)) {
                  this.processPosition(entry.getKey(), position, notified);
               }
            }
         } finally {
            this.littleperformanceimprovements$scheduledPositions = null;
         }
      }

      this.blocksToUpdate.clear();
   }
}
