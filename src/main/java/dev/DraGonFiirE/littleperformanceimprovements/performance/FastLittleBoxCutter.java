package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox;
import team.creative.littletiles.common.math.box.volume.LittleBoxReturnedVolume;
import team.creative.littletiles.common.math.vec.SplitRangeBoxes.SplitRangeBox;

public final class FastLittleBoxCutter {
   private FastLittleBoxCutter() {
   }

   public static List<LittleBox> cutOutNoCombine(
      LittleBox source, LittleGrid grid, List<LittleBox> cutters, List<LittleBox> cutout, @Nullable LittleBoxReturnedVolume volume
   ) {
      List<LittleBox> remaining = new ArrayList<>();
      if (cutters.isEmpty()) {
         remaining.add(source.copy());
         return remaining;
      }

      label120:
      for (SplitRangeBox range : source.split(cutters)) {
         LittleBox piece = source.extractBox(grid, range.x.min(), range.y.min(), range.z.min(), range.x.max(), range.y.max(), range.z.max(), volume);
         if (piece != null) {
            LittleBox partialCutter = null;

            for (LittleBox cutter : cutters) {
               if (LittleBox.intersectsWith(cutter, piece)) {
                  if (cutter.isSolid() || piece.isMinimumSize()) {
                     cutout.add(piece);
                     continue label120;
                  }

                  LittleBox clippedCutter = cutter.extractBox(grid, piece.minX, piece.minY, piece.minZ, piece.maxX, piece.maxY, piece.maxZ, null);
                  if (littleperformanceimprovements$sameGeometry(piece, clippedCutter)) {
                     cutout.add(piece);
                     continue label120;
                  }

                  if (partialCutter == null) {
                     partialCutter = cutter;
                  }
               }
            }

            if (partialCutter == null) {
               remaining.add(piece);
            } else {
               boolean[][][] filled = new boolean[piece.maxX - piece.minX][piece.maxY - piece.minY][piece.maxZ - piece.minZ];
               partialCutter.fillInSpace(piece, filled);
               boolean expected = filled[0][0][0];
               boolean continuous = true;

               label105:
               for (int x = 0; x < filled.length; x++) {
                  for (int y = 0; y < filled[x].length; y++) {
                     for (int z = 0; z < filled[x][y].length; z++) {
                        if (filled[x][y][z] != expected) {
                           continuous = false;
                           break label105;
                        }
                     }
                  }
               }

               if (!continuous) {
                  for (int x = 0; x < filled.length; x++) {
                     for (int y = 0; y < filled[x].length; y++) {
                        for (int z = 0; z < filled[x][y].length; z++) {
                           LittleBox voxel = piece.extractBox(x + piece.minX, y + piece.minY, z + piece.minZ, volume);
                           if (voxel != null) {
                              if (filled[x][y][z]) {
                                 cutout.add(voxel);
                              } else {
                                 remaining.add(voxel);
                              }
                           }
                        }
                     }
                  }
               } else if (expected) {
                  cutout.add(piece);
               } else {
                  remaining.add(piece);
               }
            }
         }
      }

      return remaining;
   }

   private static boolean littleperformanceimprovements$sameGeometry(LittleBox first, @Nullable LittleBox second) {
      if (second == null
         || first.minX != second.minX
         || first.minY != second.minY
         || first.minZ != second.minZ
         || first.maxX != second.maxX
         || first.maxY != second.maxY
         || first.maxZ != second.maxZ) {
         return false;
      } else if (!(first instanceof LittleTransformableBox firstTransformable)) {
         return !(second instanceof LittleTransformableBox);
      } else if (!(second instanceof LittleTransformableBox secondTransformable)) {
         return false;
      } else {
         Vec3f[] firstCorners = firstTransformable.getTiltedCorners();
         Vec3f[] secondCorners = secondTransformable.getTiltedCorners();
         if (firstCorners.length != secondCorners.length) {
            return false;
         }

         for (int index = 0; index < firstCorners.length; index++) {
            if (Math.abs(firstCorners[index].x - secondCorners[index].x) > 1.0E-5F
               || Math.abs(firstCorners[index].y - secondCorners[index].y) > 1.0E-5F
               || Math.abs(firstCorners[index].z - secondCorners[index].z) > 1.0E-5F) {
               return false;
            }
         }

         for (int facing = 0; facing < 6; facing++) {
            if (firstTransformable.getFlipped(facing) != secondTransformable.getFlipped(facing)) {
               return false;
            }
         }

         return true;
      }
   }
}
