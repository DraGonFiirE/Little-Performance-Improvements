package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.IntArrayTagAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.registry.LittleBlockRegistry;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.math.vec.LittleVecGrid;

public final class BlueprintPreviewMesh {
   private static final int MAX_PREVIEW_CELLS = 4096;
   private static final int MAX_AXIS_CELLS = 64;

   private BlueprintPreviewMesh() {
   }

   public static LittleGroup create(ItemStack stack) {
      CompoundTag content = BlueprintPerformance.content(stack);
      LittleVecGrid minimum = LittleGroup.getMin(content);
      LittleVecGrid size = LittleGroup.getSize(content);
      if (minimum != null && size != null) {
         size.convertTo(minimum.getGrid());
         LittleGrid grid = minimum.getGrid();
         LittleVec min = minimum.getVec();
         LittleVec dimensions = size.getVec();
         BlueprintPreviewMesh.CellGrid cells = new BlueprintPreviewMesh.CellGrid(
            min.x, min.y, min.z, Math.max(1, dimensions.x), Math.max(1, dimensions.y), Math.max(1, dimensions.z)
         );
         collect(content, cells);
         return cells.build(grid);
      } else {
         return LittleGroup.loadLow(content);
      }
   }

   private static void collect(CompoundTag group, BlueprintPreviewMesh.CellGrid cells) {
      CompoundTag tiles = group.getCompound("t");

      for (String blockName : tiles.getAllKeys()) {
         BlockState state = LittleBlockRegistry.loadState(blockName);
         ListTag entries = tiles.getList(blockName, 11);
         int color = -1;

         for (int index = 0; index < entries.size(); index++) {
            Tag entry = entries.get(index);
            if (entry instanceof IntArrayTag) {
               int[] data = ((IntArrayTagAccessor)entry).littleperformanceimprovements$getData();
               if (data.length == 1) {
                  color = data[0];
               } else if (data.length >= 6) {
                  cells.include(data[0], data[1], data[2], data[3], data[4], data[5], state, color);
               }
            }
         }
      }

      ListTag children = group.getList("c", 10);

      for (int index = 0; index < children.size(); index++) {
         collect(children.getCompound(index), cells);
      }

      CompoundTag extensions = group.getCompound("e");

      for (String key : extensions.getAllKeys()) {
         collect(extensions.getCompound(key), cells);
      }
   }

   private static final class CellGrid {
      private final int minX;
      private final int minY;
      private final int minZ;
      private final int sizeX;
      private final int sizeY;
      private final int sizeZ;
      private final int cellsX;
      private final int cellsY;
      private final int cellsZ;
      private final int coverageSizeY;
      private final int coverageSizeZ;
      private final int[] coverage;
      private final BlockState[] states;
      private final int[] colors;
      private BlockState fallbackState;
      private int fallbackColor = -1;

      private CellGrid(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.sizeX = sizeX;
         this.sizeY = sizeY;
         this.sizeZ = sizeZ;
         double volumeCellSize = Math.cbrt((double)sizeX * sizeY * sizeZ / 4096.0);
         double axisCellSize = Math.max(sizeX, Math.max(sizeY, sizeZ)) / 64.0;
         double cellSize = Math.max(1.0, Math.max(volumeCellSize, axisCellSize));

         while (true) {
            int countX = cellCount(sizeX, cellSize);
            int countY = cellCount(sizeY, cellSize);
            int countZ = cellCount(sizeZ, cellSize);
            if ((long)countX * countY * countZ <= 4096L) {
               this.cellsX = countX;
               this.cellsY = countY;
               this.cellsZ = countZ;
               int count = countX * countY * countZ;
               this.coverageSizeY = countY + 1;
               this.coverageSizeZ = countZ + 1;
               this.coverage = new int[(countX + 1) * this.coverageSizeY * this.coverageSizeZ];
               this.states = new BlockState[count];
               this.colors = new int[count];
               return;
            }

            cellSize *= 1.05;
         }
      }

      private void include(int boxMinX, int boxMinY, int boxMinZ, int boxMaxX, int boxMaxY, int boxMaxZ, BlockState state, int color) {
         int fromX = cellIndex(boxMinX, this.minX, this.sizeX, this.cellsX);
         int fromY = cellIndex(boxMinY, this.minY, this.sizeY, this.cellsY);
         int fromZ = cellIndex(boxMinZ, this.minZ, this.sizeZ, this.cellsZ);
         int toX = cellIndex(boxMaxX - 1, this.minX, this.sizeX, this.cellsX);
         int toY = cellIndex(boxMaxY - 1, this.minY, this.sizeY, this.cellsY);
         int toZ = cellIndex(boxMaxZ - 1, this.minZ, this.sizeZ, this.cellsZ);
         int afterX = toX + 1;
         int afterY = toY + 1;
         int afterZ = toZ + 1;
         this.updateCoverage(fromX, fromY, fromZ, 1);
         this.updateCoverage(afterX, fromY, fromZ, -1);
         this.updateCoverage(fromX, afterY, fromZ, -1);
         this.updateCoverage(fromX, fromY, afterZ, -1);
         this.updateCoverage(afterX, afterY, fromZ, 1);
         this.updateCoverage(afterX, fromY, afterZ, 1);
         this.updateCoverage(fromX, afterY, afterZ, 1);
         this.updateCoverage(afterX, afterY, afterZ, -1);
         if (this.fallbackState == null) {
            this.fallbackState = state;
            this.fallbackColor = color;
         }

         int materialX = fromX + toX >>> 1;
         int materialY = fromY + toY >>> 1;
         int materialZ = fromZ + toZ >>> 1;
         int materialIndex = (materialX * this.cellsY + materialY) * this.cellsZ + materialZ;
         if (this.states[materialIndex] == null) {
            this.states[materialIndex] = state;
            this.colors[materialIndex] = color;
         }
      }

      private LittleGroup build(LittleGrid grid) {
         LittleGroup result = new LittleGroup();
         LittleGroup.setGridSecretly(result, grid);

         for (int x = 0; x < this.cellsX; x++) {
            for (int y = 0; y < this.cellsY; y++) {
               for (int z = 0; z < this.cellsZ; z++) {
                  int coverageIndex = this.coverageIndex(x, y, z);
                  int value = this.coverage[coverageIndex]
                     + this.coverageAt(x - 1, y, z)
                     + this.coverageAt(x, y - 1, z)
                     + this.coverageAt(x, y, z - 1)
                     - this.coverageAt(x - 1, y - 1, z)
                     - this.coverageAt(x - 1, y, z - 1)
                     - this.coverageAt(x, y - 1, z - 1)
                     + this.coverageAt(x - 1, y - 1, z - 1);
                  this.coverage[coverageIndex] = value;
                  if (value > 0) {
                     int index = (x * this.cellsY + y) * this.cellsZ + z;
                     BlockState state = this.states[index];
                     int color = this.colors[index];
                     if (state == null) {
                        state = this.fallbackState;
                        color = this.fallbackColor;
                     }

                     if (state != null) {
                        int boxMinX = this.minX + x * this.sizeX / this.cellsX;
                        int boxMinY = this.minY + y * this.sizeY / this.cellsY;
                        int boxMinZ = this.minZ + z * this.sizeZ / this.cellsZ;
                        int boxMaxX = this.minX + (x + 1) * this.sizeX / this.cellsX;
                        int boxMaxY = this.minY + (y + 1) * this.sizeY / this.cellsY;
                        int boxMaxZ = this.minZ + (z + 1) * this.sizeZ / this.cellsZ;
                        result.addFast(
                           grid,
                           new LittleElement(state, color),
                           new LittleBox(
                              boxMinX, boxMinY, boxMinZ, Math.max(boxMinX + 1, boxMaxX), Math.max(boxMinY + 1, boxMaxY), Math.max(boxMinZ + 1, boxMaxZ)
                           )
                        );
                     }
                  }
               }
            }
         }

         return result;
      }

      private void updateCoverage(int x, int y, int z, int amount) {
         this.coverage[this.coverageIndex(x, y, z)] += amount;
      }

      private int coverageAt(int x, int y, int z) {
         return x >= 0 && y >= 0 && z >= 0 ? this.coverage[this.coverageIndex(x, y, z)] : 0;
      }

      private int coverageIndex(int x, int y, int z) {
         return (x * this.coverageSizeY + y) * this.coverageSizeZ + z;
      }

      private static int cellCount(int size, double cellSize) {
         return Math.max(1, Math.min(64, (int)Math.ceil(size / cellSize)));
      }

      private static int cellIndex(int value, int min, int size, int cells) {
         int index = (int)Math.floor((double)((long)(value - min) * cells) / size);
         return Math.max(0, Math.min(cells - 1, index));
      }
   }
}
