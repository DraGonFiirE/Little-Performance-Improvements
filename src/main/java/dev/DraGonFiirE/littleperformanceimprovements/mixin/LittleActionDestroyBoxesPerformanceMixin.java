package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.performance.FastLittleBoxCutter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.action.LittleActionDestroyBoxes;
import team.creative.littletiles.common.action.source.LittleActionSource;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.collection.LittleCollection;
import team.creative.littletiles.common.block.little.tile.group.LittleGroupAbsolute;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.block.little.tile.parent.ParentCollection;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.ingredient.LittleIngredients;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox;
import team.creative.littletiles.common.math.box.volume.LittleBoxReturnedVolume;

@Mixin(LittleActionDestroyBoxes.class)
public abstract class LittleActionDestroyBoxesPerformanceMixin {
   private static final int MIN_INDEXED_BOXES = 1;
   private static final int MAX_TRANSFORM_RECONSTRUCTION_PARTS = 2048;
   private static final int MAX_TRANSFORM_EXPANSION_PARTS = 63;
   private static final int MAX_TRANSFORM_EXPANSION_STATES = 100000;
   @Shadow
   public transient boolean doneSomething;
   @Shadow
   public transient LittleGroupAbsolute destroyed;

   @Shadow
   public abstract boolean shouldSkipTile(IParentCollection var1, LittleTile var2);

   @Redirect(
      method = "action(Lnet/minecraft/world/level/Level;Lteam/creative/littletiles/common/action/source/LittleActionSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/List;Lteam/creative/littletiles/common/grid/LittleGrid;)V",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/block/entity/BETiles;convertTo(Lteam/creative/littletiles/common/grid/LittleGrid;)V"
      )
   )
   private void littleperformanceimprovements$keepExistingGridForExactRemoval(
      BETiles be,
      LittleGrid requestedGrid,
      Level level,
      LittleActionSource source,
      BlockPos pos,
      BlockState state,
      List<LittleBox> boxes,
      LittleGrid sourceGrid
   ) {
      LittleGrid targetGrid = be.getGrid();
      if (!this.littleperformanceimprovements$canDownConvertExactly(be, boxes, sourceGrid, targetGrid)) {
         be.convertTo(requestedGrid);
      } else {
         for (LittleBox box : boxes) {
            box.convertTo(sourceGrid, targetGrid);
         }
      }
   }

   private boolean littleperformanceimprovements$canDownConvertExactly(BETiles be, List<LittleBox> boxes, LittleGrid sourceGrid, LittleGrid targetGrid) {
      if (sourceGrid.count > targetGrid.count && sourceGrid.count % targetGrid.count == 0 && !boxes.isEmpty()) {
         int ratio = sourceGrid.count / targetGrid.count;

         for (LittleBox box : boxes) {
            if (!littleperformanceimprovements$isExactlyRepresentable(box, ratio)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean littleperformanceimprovements$isExactlyRepresentable(LittleBox box, int ratio) {
      if (box.minX % ratio == 0 && box.minY % ratio == 0 && box.minZ % ratio == 0 && box.maxX % ratio == 0 && box.maxY % ratio == 0 && box.maxZ % ratio == 0) {
         if (box instanceof LittleTransformableBox transformable) {
            for (Vec3f corner : transformable.getTiltedCorners()) {
               if (!littleperformanceimprovements$isGridMultiple(corner.x, ratio)
                  || !littleperformanceimprovements$isGridMultiple(corner.y, ratio)
                  || !littleperformanceimprovements$isGridMultiple(corner.z, ratio)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean littleperformanceimprovements$isGridMultiple(float value, int ratio) {
      double scaled = value / ratio;
      return Math.abs(scaled - Math.rint(scaled)) < 1.0E-6;
   }

   @Redirect(
      method = "action(Lteam/creative/littletiles/common/action/source/LittleActionSource;Lteam/creative/littletiles/common/block/entity/BETiles;Ljava/util/List;ZLteam/creative/littletiles/common/grid/LittleGrid;)Lteam/creative/littletiles/common/ingredient/LittleIngredients;",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/block/little/tile/group/LittleGroupAbsolute;add(Lteam/creative/littletiles/common/block/little/tile/parent/IParentCollection;Lteam/creative/littletiles/common/block/little/element/LittleElement;Lteam/creative/littletiles/common/math/box/LittleBox;)V"
      )
   )
   private void littleperformanceimprovements$deferDestroyedSnapshotNormalization(
      LittleGroupAbsolute group, IParentCollection parent, LittleElement element, LittleBox box
   ) {
      group.addFast(parent, element, box);
   }

   @Inject(
      method = "action(Lteam/creative/littletiles/common/action/source/LittleActionSource;Lteam/creative/littletiles/common/block/entity/BETiles;Ljava/util/List;ZLteam/creative/littletiles/common/grid/LittleGrid;)Lteam/creative/littletiles/common/ingredient/LittleIngredients;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void littleperformanceimprovements$removeExactBoxesByIndex(
      LittleActionSource source, BETiles be, List<LittleBox> boxes, boolean simulate, LittleGrid grid, CallbackInfoReturnable<LittleIngredients> callback
   ) {
      boolean hasTransformable = false;

      for (LittleBox box : boxes) {
         if (box instanceof LittleTransformableBox) {
            hasTransformable = true;
            break;
         }
      }

      if (boxes.size() >= 1 || hasTransformable) {
         Map<LittleActionDestroyBoxesPerformanceMixin.ShapeKey, LittleBox> removals = new HashMap<>(littleperformanceimprovements$mapCapacity(boxes.size()));

         for (LittleBox box : boxes) {
            removals.putIfAbsent(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(box), box);
         }

         LittleActionDestroyBoxesPerformanceMixin.RemovalIndex removalIndex = new LittleActionDestroyBoxesPerformanceMixin.RemovalIndex(boxes, grid);

         for (IParentCollection parent : be.groups()) {
            if (parent.isStructure()) {
               return;
            }

            for (LittleTile tile : parent) {
               if (!this.shouldSkipTile(parent, tile)) {
                  for (LittleBox existing : tile) {
                     if (!removals.containsKey(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(existing))) {
                        for (LittleBox removal : removalIndex.query(existing)) {
                           if (LittleBox.intersectsWith(existing, removal)) {
                              this.littleperformanceimprovements$removeBySpatialIndex(source, be, boxes, simulate, grid, callback);
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }

         this.doneSomething = false;
         if (this.destroyed == null) {
            this.destroyed = new LittleGroupAbsolute(be.getBlockPos());
         }

         LittleIngredients ingredients = new LittleIngredients();
         LittleCollection toRemove = new LittleCollection();

         for (IParentCollection parent : be.groups()) {
            for (LittleTile tile : parent) {
               if (!this.shouldSkipTile(parent, tile)) {
                  List<LittleBox> matched = simulate ? null : new ArrayList<>();

                  for (LittleBox existing : tile) {
                     LittleBox removal = removals.get(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(existing));
                     if (removal != null) {
                        this.doneSomething = true;
                        ingredients.add(LittleAction.getIngredients(parent, tile, removal));
                        if (!simulate) {
                           matched.add(removal);
                        }
                     }
                  }

                  if (!simulate && !matched.isEmpty()) {
                     toRemove.add(tile, matched);
                     this.destroyed.addFast(parent, tile.copy(matched));
                  }
               }
            }
         }

         if (!simulate) {
            be.updateTiles(interactor -> {
               ParentCollection parentx = interactor.noneStructureTiles();
               parentx.removeAll(toRemove);
            });
         }

         callback.setReturnValue(ingredients);
      }
   }

   private void littleperformanceimprovements$removeBySpatialIndex(
      LittleActionSource source, BETiles be, List<LittleBox> boxes, boolean simulate, LittleGrid grid, CallbackInfoReturnable<LittleIngredients> callback
   ) {
      LittleActionDestroyBoxesPerformanceMixin.RemovalIndex removalIndex = new LittleActionDestroyBoxesPerformanceMixin.RemovalIndex(boxes, grid);
      Map<LittleActionDestroyBoxesPerformanceMixin.ShapeKey, LittleBox> exactRemovals = new HashMap<>(littleperformanceimprovements$mapCapacity(boxes.size()));

      for (LittleBox box : boxes) {
         exactRemovals.putIfAbsent(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(box), box);
      }

      this.doneSomething = false;
      if (this.destroyed == null) {
         this.destroyed = new LittleGroupAbsolute(be.getBlockPos());
      }

      LittleIngredients ingredients = new LittleIngredients();
      LittleCollection toPlace = new LittleCollection();
      LittleCollection toRemove = new LittleCollection();

      for (IParentCollection parent : be.groups()) {
         if (parent.isStructure()) {
            return;
         }

         for (LittleTile tile : parent) {
            if (!this.shouldSkipTile(parent, tile)) {
               for (LittleBox existing : tile) {
                  LittleBox exactRemoval = exactRemovals.get(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(existing));
                  if (exactRemoval != null) {
                     this.doneSomething = true;
                     ingredients.add(LittleAction.getIngredients(parent, tile, existing));
                     if (!simulate) {
                        toRemove.add(tile, existing);
                        this.destroyed.addFast(parent, tile.copy(List.of(existing)));
                     }
                  } else {
                     List<LittleBox> candidates = removalIndex.query(existing);
                     if (existing instanceof LittleTransformableBox && littleperformanceimprovements$isCoveredByCombinableRemovalPieces(existing, candidates)) {
                        this.doneSomething = true;
                        ingredients.add(LittleAction.getIngredients(parent, tile, existing));
                        if (!simulate) {
                           toRemove.add(tile, existing);
                           this.destroyed.addFast(parent, tile.copy(List.of(existing)));
                        }
                     } else {
                        LittleBox firstIntersecting = null;

                        for (LittleBox candidate : candidates) {
                           if (LittleBox.intersectsWith(existing, candidate)) {
                              firstIntersecting = candidate;
                              break;
                           }
                        }

                        if (firstIntersecting != null) {
                           this.doneSomething = true;
                           if (existing.equals(firstIntersecting)) {
                              ingredients.add(LittleAction.getIngredients(parent, tile, existing));
                              if (!simulate) {
                                 toRemove.add(tile, existing);
                                 this.destroyed.addFast(parent, tile.copy(List.of(existing)));
                              }
                           } else {
                              List<LittleBox> cutout = new ArrayList<>();
                              LittleBoxReturnedVolume returnedVolume = new LittleBoxReturnedVolume();
                              List<LittleBox> remaining = FastLittleBoxCutter.cutOutNoCombine(existing, grid, candidates, cutout, returnedVolume);
                              if (remaining != null) {
                                 if (!simulate) {
                                    toPlace.add(tile, remaining);
                                    toRemove.add(tile, existing);
                                 }

                                 double volume = 0.0;

                                 for (LittleBox removed : cutout) {
                                    volume += removed.getPercentVolume(grid);
                                 }

                                 if (!simulate && !cutout.isEmpty()) {
                                    this.destroyed.addFast(parent, tile.copy(cutout));
                                 }

                                 if (volume > 0.0) {
                                    ingredients.add(LittleAction.getIngredients(tile, volume));
                                 }
                              }

                              if (returnedVolume.has()) {
                                 ingredients.add(LittleAction.getIngredients(tile, returnedVolume.getPercentVolume(grid)));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (!simulate) {
         be.updateTiles(interactor -> {
            ParentCollection parentx = interactor.noneStructureTiles();
            parentx.removeAll(toRemove);
            parentx.addAll(toPlace);
         });
      }

      callback.setReturnValue(ingredients);
   }

   private static boolean littleperformanceimprovements$isCoveredByCombinableRemovalPieces(LittleBox target, List<LittleBox> candidates) {
      LittleActionDestroyBoxesPerformanceMixin.ShapeKey targetKey = new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(target);
      Map<LittleActionDestroyBoxesPerformanceMixin.ShapeKey, LittleBox> unique = new LinkedHashMap<>();

      for (LittleBox candidate : candidates) {
         if (candidate.isSolid()
            && candidate.minX <= target.minX
            && candidate.minY <= target.minY
            && candidate.minZ <= target.minZ
            && candidate.maxX >= target.maxX
            && candidate.maxY >= target.maxY
            && candidate.maxZ >= target.maxZ) {
            return true;
         }

         if (candidate.minX >= target.minX
            && candidate.minY >= target.minY
            && candidate.minZ >= target.minZ
            && candidate.maxX <= target.maxX
            && candidate.maxY <= target.maxY
            && candidate.maxZ <= target.maxZ
            && LittleBox.intersectsWith(target, candidate)) {
            LittleActionDestroyBoxesPerformanceMixin.ShapeKey key = new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(candidate);
            if (targetKey.equals(key)) {
               return true;
            }

            unique.putIfAbsent(key, candidate);
            if (unique.size() > 2048) {
               return false;
            }
         }
      }

      if (unique.size() < 2) {
         return false;
      }

      List<LittleBox> pieces = new ArrayList<>(unique.size());

      for (LittleBox piece : unique.values()) {
         pieces.add(piece.copy());
      }

      if (littleperformanceimprovements$canReconstructByExpansion(targetKey, target, pieces)) {
         return true;
      }

      boolean changed = true;

      label106:
      while (changed) {
         changed = false;

         for (int first = 0; first < pieces.size(); first++) {
            LittleBox left = pieces.get(first);

            for (int second = 0; second < pieces.size(); second++) {
               if (first != second) {
                  LittleBox merged = left.combineBoxes(pieces.get(second));
                  if (merged != null
                     && merged.minX >= target.minX
                     && merged.minY >= target.minY
                     && merged.minZ >= target.minZ
                     && merged.maxX <= target.maxX
                     && merged.maxY <= target.maxY
                     && merged.maxZ <= target.maxZ) {
                     if (targetKey.equals(new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(merged))) {
                        return true;
                     }

                     pieces.set(first, merged);
                     pieces.remove(second);
                     changed = true;
                     continue label106;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean littleperformanceimprovements$canReconstructByExpansion(
      LittleActionDestroyBoxesPerformanceMixin.ShapeKey targetKey, LittleBox target, List<LittleBox> pieces
   ) {
      if (pieces.size() > 63) {
         return false;
      }

      ArrayDeque<LittleBox> pendingBoxes = new ArrayDeque<>();
      ArrayDeque<Long> pendingUsed = new ArrayDeque<>();
      Map<LittleActionDestroyBoxesPerformanceMixin.ShapeKey, Set<Long>> visited = new HashMap<>();

      for (int index = 0; index < pieces.size(); index++) {
         LittleBox piece = pieces.get(index);
         if (piece instanceof LittleTransformableBox) {
            long used = 1L << index;
            pendingBoxes.addLast(piece);
            pendingUsed.addLast(used);
            littleperformanceimprovements$markExpansionVisited(visited, new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(piece), used);
         }
      }

      int states = pendingBoxes.size();

      while (!pendingBoxes.isEmpty() && states <= 100000) {
         LittleBox current = pendingBoxes.removeFirst();
         long currentUsed = pendingUsed.removeFirst();

         for (int index = 0; index < pieces.size(); index++) {
            long bit = 1L << index;
            if ((currentUsed & bit) == 0L) {
               LittleBox candidate = pieces.get(index);
               LittleBox merged = current.combineBoxes(candidate);
               if (merged == null) {
                  merged = candidate.combineBoxes(current);
               }

               if (merged != null && littleperformanceimprovements$isWithin(merged, target)) {
                  LittleActionDestroyBoxesPerformanceMixin.ShapeKey mergedKey = new LittleActionDestroyBoxesPerformanceMixin.ShapeKey(merged);
                  if (targetKey.equals(mergedKey)) {
                     return true;
                  }

                  long used = currentUsed | bit;
                  if (littleperformanceimprovements$markExpansionVisited(visited, mergedKey, used)) {
                     pendingBoxes.addLast(merged);
                     pendingUsed.addLast(used);
                     states++;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean littleperformanceimprovements$markExpansionVisited(
      Map<LittleActionDestroyBoxesPerformanceMixin.ShapeKey, Set<Long>> visited, LittleActionDestroyBoxesPerformanceMixin.ShapeKey shape, long used
   ) {
      return visited.computeIfAbsent(shape, ignored -> new HashSet<>()).add(used);
   }

   private static boolean littleperformanceimprovements$isWithin(LittleBox box, LittleBox target) {
      return box.minX >= target.minX
         && box.minY >= target.minY
         && box.minZ >= target.minZ
         && box.maxX <= target.maxX
         && box.maxY <= target.maxY
         && box.maxZ <= target.maxZ;
   }

   private static int littleperformanceimprovements$mapCapacity(int size) {
      return size < 1000000 ? (int)(size / 0.75F) + 1 : Integer.MAX_VALUE;
   }

   private static final class RemovalIndex {
      private static final int MAX_BUCKET_COPIES = 64;
      private final List<LittleBox> boxes;
      private final int divisions;
      private final int bucketSize;
      private final Map<Integer, List<Integer>> buckets = new HashMap<>();
      private final List<Integer> broad = new ArrayList<>();

      private RemovalIndex(List<LittleBox> boxes, LittleGrid grid) {
         this.boxes = boxes;
         this.divisions = Math.max(1, Math.min(32, (int)Math.ceil(Math.cbrt(Math.max(1, boxes.size())))));
         this.bucketSize = Math.max(1, (grid.count + this.divisions - 1) / this.divisions);

         for (int index = 0; index < boxes.size(); index++) {
            LittleBox box = boxes.get(index);
            int minX = this.bucket(box.minX);
            int minY = this.bucket(box.minY);
            int minZ = this.bucket(box.minZ);
            int maxX = this.bucket(box.maxX - 1);
            int maxY = this.bucket(box.maxY - 1);
            int maxZ = this.bucket(box.maxZ - 1);
            int copies = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            if (copies > 64) {
               this.broad.add(index);
            } else {
               for (int x = minX; x <= maxX; x++) {
                  for (int y = minY; y <= maxY; y++) {
                     for (int z = minZ; z <= maxZ; z++) {
                        this.buckets.computeIfAbsent(this.key(x, y, z), ignored -> new ArrayList<>()).add(index);
                     }
                  }
               }
            }
         }
      }

      private List<LittleBox> query(LittleBox box) {
         IdentityHashMap<LittleBox, Boolean> seen = new IdentityHashMap<>();
         List<Integer> matches = new ArrayList<>();

         for (int index : this.broad) {
            this.add(index, seen, matches);
         }

         int minX = this.bucket(box.minX);
         int minY = this.bucket(box.minY);
         int minZ = this.bucket(box.minZ);
         int maxX = this.bucket(box.maxX - 1);
         int maxY = this.bucket(box.maxY - 1);
         int maxZ = this.bucket(box.maxZ - 1);

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  List<Integer> bucket = this.buckets.get(this.key(x, y, z));
                  if (bucket != null) {
                     for (int index : bucket) {
                        this.add(index, seen, matches);
                     }
                  }
               }
            }
         }

         matches.sort(Integer::compareTo);
         List<LittleBox> result = new ArrayList<>(matches.size());

         for (int match : matches) {
            LittleBox candidate = this.boxes.get(match);
            if (candidate.maxX > box.minX
               && candidate.minX < box.maxX
               && candidate.maxY > box.minY
               && candidate.minY < box.maxY
               && candidate.maxZ > box.minZ
               && candidate.minZ < box.maxZ) {
               result.add(candidate);
            }
         }

         return result;
      }

      private void add(int index, IdentityHashMap<LittleBox, Boolean> seen, List<Integer> matches) {
         LittleBox box = this.boxes.get(index);
         if (seen.put(box, Boolean.TRUE) == null) {
            matches.add(index);
         }
      }

      private int bucket(int coordinate) {
         return Math.max(0, Math.min(this.divisions - 1, Math.floorDiv(coordinate, this.bucketSize)));
      }

      private int key(int x, int y, int z) {
         return (x * this.divisions + y) * this.divisions + z;
      }
   }

   private static final class ShapeKey {
      private final Class<?> type;
      private final int[] data;
      private final int hash;

      private ShapeKey(LittleBox box) {
         this.type = box.getClass();
         if (box instanceof LittleTransformableBox transformable) {
            Vec3f[] corners = transformable.getTiltedCorners();
            this.data = new int[6 + corners.length * 3 + 6];
            this.data[0] = box.minX;
            this.data[1] = box.minY;
            this.data[2] = box.minZ;
            this.data[3] = box.maxX;
            this.data[4] = box.maxY;
            this.data[5] = box.maxZ;
            int index = 6;

            for (Vec3f corner : corners) {
               this.data[index++] = Math.round(corner.x);
               this.data[index++] = Math.round(corner.y);
               this.data[index++] = Math.round(corner.z);
            }

            for (int facing = 0; facing < 6; facing++) {
               this.data[index++] = transformable.getFlipped(facing) ? 1 : 0;
            }
         } else {
            this.data = box.getArray();
         }

         this.hash = 31 * this.type.hashCode() + Arrays.hashCode(this.data);
      }

      @Override
      public int hashCode() {
         return this.hash;
      }

      @Override
      public boolean equals(Object object) {
         return object instanceof LittleActionDestroyBoxesPerformanceMixin.ShapeKey other && this.type == other.type && Arrays.equals(this.data, other.data);
      }
   }
}
