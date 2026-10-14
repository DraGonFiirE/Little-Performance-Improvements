package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import team.creative.littletiles.common.math.box.LittleBox;

public final class LittleBoxCombinePrecheck {
   private static final int MIN_PRECHECK_SIZE = 32;

   private LittleBoxCombinePrecheck() {
   }

   public static boolean canSkip(List<LittleBox> boxes) {
      if (boxes.size() < 32) {
         return false;
      }

      Set<LittleBoxCombinePrecheck.BoxKey> unique = new HashSet<>(capacity(boxes.size()));

      for (LittleBox box : boxes) {
         if (box.getClass() != LittleBox.class) {
            return false;
         }

         if (!unique.add(new LittleBoxCombinePrecheck.BoxKey(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))) {
            return false;
         }
      }

      return !hasAdjacentPair(boxes, LittleBoxCombinePrecheck.Axis.X)
         && !hasAdjacentPair(boxes, LittleBoxCombinePrecheck.Axis.Y)
         && !hasAdjacentPair(boxes, LittleBoxCombinePrecheck.Axis.Z);
   }

   private static boolean hasAdjacentPair(List<LittleBox> boxes, LittleBoxCombinePrecheck.Axis axis) {
      int capacity = capacity(boxes.size());
      Set<LittleBoxCombinePrecheck.FaceKey> starts = new HashSet<>(capacity);
      Set<LittleBoxCombinePrecheck.FaceKey> ends = new HashSet<>(capacity);

      for (LittleBox box : boxes) {
         LittleBoxCombinePrecheck.FaceKey start = face(box, axis, min(box, axis));
         LittleBoxCombinePrecheck.FaceKey end = face(box, axis, max(box, axis));
         if (ends.contains(start) || starts.contains(end)) {
            return true;
         }

         starts.add(start);
         ends.add(end);
      }

      return false;
   }

   private static LittleBoxCombinePrecheck.FaceKey face(LittleBox box, LittleBoxCombinePrecheck.Axis axis, int plane) {
      return switch (axis) {
         case X -> new LittleBoxCombinePrecheck.FaceKey(plane, box.minY, box.maxY, box.minZ, box.maxZ);
         case Y -> new LittleBoxCombinePrecheck.FaceKey(plane, box.minX, box.maxX, box.minZ, box.maxZ);
         case Z -> new LittleBoxCombinePrecheck.FaceKey(plane, box.minX, box.maxX, box.minY, box.maxY);
      };
   }

   private static int min(LittleBox box, LittleBoxCombinePrecheck.Axis axis) {
      return switch (axis) {
         case X -> box.minX;
         case Y -> box.minY;
         case Z -> box.minZ;
      };
   }

   private static int max(LittleBox box, LittleBoxCombinePrecheck.Axis axis) {
      return switch (axis) {
         case X -> box.maxX;
         case Y -> box.maxY;
         case Z -> box.maxZ;
      };
   }

   private static int capacity(int size) {
      return Math.max(16, (int)Math.ceil(size / 0.75));
   }

   private enum Axis {
      X,
      Y,
      Z;
   }

   private record BoxKey(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
   }

   private record FaceKey(int plane, int minOne, int maxOne, int minTwo, int maxTwo) {
   }
}
