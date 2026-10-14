package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.face.LittleFaceState;
import team.creative.littletiles.common.math.face.LittleServerFace;
import team.creative.littletiles.common.structure.attribute.LittleStructureAttribute;

public final class IndexedFaceRebuilder {
   private static final int MIN_INDEXED_BOXES = 32;

   private IndexedFaceRebuilder() {
   }

   public static boolean rebuild(BETiles be) {
      LittleGrid grid = be.getGrid();
      List<IndexedFaceRebuilder.Entry> entries = new ArrayList<>();

      for (Pair<IParentCollection, LittleTile> pair : be.allTiles()) {
         if (pair.key.getGrid() != grid) {
            return false;
         }

         for (LittleBox box : pair.value) {
            if (box.getClass() != LittleBox.class) {
               return false;
            }

            entries.add(new IndexedFaceRebuilder.Entry(pair.key, pair.value, box));
         }
      }

      if (entries.size() < 32) {
         return false;
      }

      IndexedFaceRebuilder.FaceIndex index = new IndexedFaceRebuilder.FaceIndex();

      for (IndexedFaceRebuilder.Entry entry : entries) {
         index.add(entry);
      }

      LittleServerFace face = new LittleServerFace(be);

      for (IndexedFaceRebuilder.Entry entry : entries) {
         for (Facing facing : Facing.VALUES) {
            face.set(entry.parent, entry.tile, entry.box, facing);
            LittleFaceState state;
            if (face.isFaceInsideBlock()) {
               state = calculateInside(index.candidates(facing, face.origin()), entry, face);
            } else {
               state = face.calculate();
            }

            entry.box.setFaceState(facing, state);
         }
      }

      return true;
   }

   private static LittleFaceState calculateInside(List<IndexedFaceRebuilder.Entry> candidates, IndexedFaceRebuilder.Entry rendered, LittleServerFace face) {
      if (candidates != null) {
         for (IndexedFaceRebuilder.Entry candidate : candidates) {
            if (candidate.box != rendered.box
               && (!candidate.parent.isStructure() || !LittleStructureAttribute.noCollision(candidate.parent.getAttribute()))
               && (candidate.tile.doesProvideSolidFace() || candidate.tile.canBeRenderCombined(rendered.tile))) {
               candidate.box.fill(face);
            }
         }
      }

      if (face.isFilled()) {
         return LittleFaceState.INSIDE_COVERED;
      } else {
         return face.isPartiallyFilled() ? LittleFaceState.INSIDE_PARTIALLY_COVERED : LittleFaceState.INSIDE_UNCOVERED;
      }
   }

   private record Entry(IParentCollection parent, LittleTile tile, LittleBox box) {
   }

   private static final class FaceIndex {
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> xMin = new HashMap<>();
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> xMax = new HashMap<>();
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> yMin = new HashMap<>();
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> yMax = new HashMap<>();
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> zMin = new HashMap<>();
      private final Map<Integer, List<IndexedFaceRebuilder.Entry>> zMax = new HashMap<>();

      private void add(IndexedFaceRebuilder.Entry entry) {
         LittleBox box = entry.box;
         add(this.xMin, box.minX, entry);
         add(this.xMax, box.maxX, entry);
         add(this.yMin, box.minY, entry);
         add(this.yMax, box.maxY, entry);
         add(this.zMin, box.minZ, entry);
         add(this.zMax, box.maxZ, entry);
      }

      private List<IndexedFaceRebuilder.Entry> candidates(Facing facing, int plane) {
         Map<Integer, List<IndexedFaceRebuilder.Entry>> planes = switch (facing.axis) {
            case X -> facing.positive ? this.xMin : this.xMax;
            case Y -> facing.positive ? this.yMin : this.yMax;
            case Z -> facing.positive ? this.zMin : this.zMax;
         };
         return planes.get(plane);
      }

      private static void add(Map<Integer, List<IndexedFaceRebuilder.Entry>> planes, int plane, IndexedFaceRebuilder.Entry entry) {
         planes.computeIfAbsent(plane, ignored -> new ArrayList<>()).add(entry);
      }
   }
}
