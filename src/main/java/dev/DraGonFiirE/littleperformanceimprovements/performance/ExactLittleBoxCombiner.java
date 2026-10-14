package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import team.creative.littletiles.common.math.box.LittleBox;

public final class ExactLittleBoxCombiner {
   private static final int MIN_INDEXED_SIZE = 512;

   private ExactLittleBoxCombiner() {
   }

   public static Boolean combine(List<LittleBox> boxes) {
      PreviewBuildCancellation.check();
      if (boxes.size() < 512) {
         return null;
      }

      for (LittleBox box : boxes) {
         if (box.getClass() != LittleBox.class) {
            return null;
         }
      }

      ExactLittleBoxCombiner.Node head = null;
      ExactLittleBoxCombiner.Node previous = null;
      ExactLittleBoxCombiner.Index index = new ExactLittleBoxCombiner.Index();

      for (int order = 0; order < boxes.size(); order++) {
         if ((order & 0xFF) == 0) {
            PreviewBuildCancellation.check();
         }

         ExactLittleBoxCombiner.Node node = new ExactLittleBoxCombiner.Node(order, boxes.get(order));
         if (head == null) {
            head = node;
         }

         node.previous = previous;
         if (previous != null) {
            previous.next = node;
         }

         previous = node;
         index.add(node);
      }

      boolean changed = false;
      boolean passChanged = true;

      while (passChanged) {
         PreviewBuildCancellation.check();
         passChanged = false;

         for (ExactLittleBoxCombiner.Node current = head; current != null; current = current.next) {
            if ((current.order & 0xFF) == 0) {
               PreviewBuildCancellation.check();
            }

            for (ExactLittleBoxCombiner.Node cursor = head; cursor != null; passChanged = true) {
               ExactLittleBoxCombiner.Node other = index.firstMergeCandidate(current, cursor);
               if (other == null) {
                  break;
               }

               LittleBox merged = current.box.combineBoxes(other.box);
               if (merged == null) {
                  throw new IllegalStateException("Indexed LittleBox merge candidate did not merge");
               }

               ExactLittleBoxCombiner.Node nextCursor = other.next;
               index.remove(current);
               index.remove(other);
               if (other.previous != null) {
                  other.previous.next = other.next;
               } else {
                  head = other.next;
               }

               if (other.next != null) {
                  other.next.previous = other.previous;
               }

               current.box = merged;
               index.add(current);
               cursor = nextCursor;
               changed = true;
            }
         }
      }

      if (changed) {
         boxes.clear();

         for (ExactLittleBoxCombiner.Node node = head; node != null; node = node.next) {
            boxes.add(node.box);
         }
      }

      return changed;
   }

   private static ExactLittleBoxCombiner.BoxKey boxKey(LittleBox box) {
      return new ExactLittleBoxCombiner.BoxKey(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
   }

   private static ExactLittleBoxCombiner.FaceKey xFace(LittleBox box, int plane) {
      return new ExactLittleBoxCombiner.FaceKey(plane, box.minY, box.maxY, box.minZ, box.maxZ);
   }

   private static ExactLittleBoxCombiner.FaceKey yFace(LittleBox box, int plane) {
      return new ExactLittleBoxCombiner.FaceKey(plane, box.minX, box.maxX, box.minZ, box.maxZ);
   }

   private static ExactLittleBoxCombiner.FaceKey zFace(LittleBox box, int plane) {
      return new ExactLittleBoxCombiner.FaceKey(plane, box.minX, box.maxX, box.minY, box.maxY);
   }

   private record BoxKey(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
   }

   private record FaceKey(int plane, int minOne, int maxOne, int minTwo, int maxTwo) {
   }

   private static final class Index {
      private static final Comparator<ExactLittleBoxCombiner.Node> ORDER = Comparator.comparingInt(node -> node.order);
      private final Map<ExactLittleBoxCombiner.BoxKey, NavigableSet<ExactLittleBoxCombiner.Node>> exact = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> xMin = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> xMax = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> yMin = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> yMax = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> zMin = new HashMap<>();
      private final Map<ExactLittleBoxCombiner.FaceKey, NavigableSet<ExactLittleBoxCombiner.Node>> zMax = new HashMap<>();

      private void add(ExactLittleBoxCombiner.Node node) {
         LittleBox box = node.box;
         add(this.exact, ExactLittleBoxCombiner.boxKey(box), node);
         add(this.xMin, ExactLittleBoxCombiner.xFace(box, box.minX), node);
         add(this.xMax, ExactLittleBoxCombiner.xFace(box, box.maxX), node);
         add(this.yMin, ExactLittleBoxCombiner.yFace(box, box.minY), node);
         add(this.yMax, ExactLittleBoxCombiner.yFace(box, box.maxY), node);
         add(this.zMin, ExactLittleBoxCombiner.zFace(box, box.minZ), node);
         add(this.zMax, ExactLittleBoxCombiner.zFace(box, box.maxZ), node);
      }

      private void remove(ExactLittleBoxCombiner.Node node) {
         LittleBox box = node.box;
         remove(this.exact, ExactLittleBoxCombiner.boxKey(box), node);
         remove(this.xMin, ExactLittleBoxCombiner.xFace(box, box.minX), node);
         remove(this.xMax, ExactLittleBoxCombiner.xFace(box, box.maxX), node);
         remove(this.yMin, ExactLittleBoxCombiner.yFace(box, box.minY), node);
         remove(this.yMax, ExactLittleBoxCombiner.yFace(box, box.maxY), node);
         remove(this.zMin, ExactLittleBoxCombiner.zFace(box, box.minZ), node);
         remove(this.zMax, ExactLittleBoxCombiner.zFace(box, box.maxZ), node);
      }

      private ExactLittleBoxCombiner.Node firstMergeCandidate(ExactLittleBoxCombiner.Node node, ExactLittleBoxCombiner.Node cursor) {
         LittleBox box = node.box;
         ExactLittleBoxCombiner.Node best = null;
         best = earlier(best, candidate(this.exact.get(ExactLittleBoxCombiner.boxKey(box)), cursor, node));
         best = earlier(best, candidate(this.zMax.get(ExactLittleBoxCombiner.zFace(box, box.minZ)), cursor, node));
         best = earlier(best, candidate(this.zMin.get(ExactLittleBoxCombiner.zFace(box, box.maxZ)), cursor, node));
         best = earlier(best, candidate(this.yMax.get(ExactLittleBoxCombiner.yFace(box, box.minY)), cursor, node));
         best = earlier(best, candidate(this.yMin.get(ExactLittleBoxCombiner.yFace(box, box.maxY)), cursor, node));
         best = earlier(best, candidate(this.xMax.get(ExactLittleBoxCombiner.xFace(box, box.minX)), cursor, node));
         return earlier(best, candidate(this.xMin.get(ExactLittleBoxCombiner.xFace(box, box.maxX)), cursor, node));
      }

      private static ExactLittleBoxCombiner.Node candidate(
         NavigableSet<ExactLittleBoxCombiner.Node> nodes, ExactLittleBoxCombiner.Node cursor, ExactLittleBoxCombiner.Node excluded
      ) {
         if (nodes == null) {
            return null;
         }

         ExactLittleBoxCombiner.Node candidate = nodes.ceiling(cursor);
         if (candidate == excluded) {
            candidate = nodes.higher(candidate);
         }

         return candidate;
      }

      private static ExactLittleBoxCombiner.Node earlier(ExactLittleBoxCombiner.Node first, ExactLittleBoxCombiner.Node second) {
         if (first == null) {
            return second;
         } else if (second == null) {
            return first;
         } else {
            return first.order < second.order ? first : second;
         }
      }

      private static <K> void add(Map<K, NavigableSet<ExactLittleBoxCombiner.Node>> index, K key, ExactLittleBoxCombiner.Node node) {
         index.computeIfAbsent(key, ignored -> new TreeSet<>(ORDER)).add(node);
      }

      private static <K> void remove(Map<K, NavigableSet<ExactLittleBoxCombiner.Node>> index, K key, ExactLittleBoxCombiner.Node node) {
         NavigableSet<ExactLittleBoxCombiner.Node> nodes = index.get(key);
         nodes.remove(node);
         if (nodes.isEmpty()) {
            index.remove(key);
         }
      }
   }

   private static final class Node {
      private final int order;
      private LittleBox box;
      private ExactLittleBoxCombiner.Node previous;
      private ExactLittleBoxCombiner.Node next;

      private Node(int order, LittleBox box) {
         this.order = order;
         this.box = box;
      }
   }
}
