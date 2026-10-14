package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import team.creative.littletiles.common.math.box.SurroundingBox;
import team.creative.littletiles.common.structure.LittleStructure;

public final class StructureBoundsCache {
   private static final Map<LittleStructure, WeakReference<SurroundingBox>> CACHE = new WeakHashMap<>();

   private StructureBoundsCache() {
   }

   public static synchronized SurroundingBox get(LittleStructure structure) {
      WeakReference<SurroundingBox> reference = CACHE.get(structure);
      if (reference == null) {
         return null;
      }

      SurroundingBox box = reference.get();
      if (box == null) {
         CACHE.remove(structure);
      }

      return box;
   }

   public static synchronized void put(LittleStructure structure, SurroundingBox box) {
      CACHE.put(structure, new WeakReference<>(box));
   }

   public static synchronized void invalidate(LittleStructure structure) {
      CACHE.remove(structure);
   }
}
