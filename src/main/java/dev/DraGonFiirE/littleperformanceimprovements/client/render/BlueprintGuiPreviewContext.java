package dev.DraGonFiirE.littleperformanceimprovements.client.render;

public final class BlueprintGuiPreviewContext {
   private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

   private BlueprintGuiPreviewContext() {
   }

   public static boolean active() {
      return DEPTH.get() > 0;
   }

   public static void enter() {
      DEPTH.set(DEPTH.get() + 1);
   }

   public static void exit() {
      int depth = DEPTH.get();
      if (depth <= 1) {
         DEPTH.remove();
      } else {
         DEPTH.set(depth - 1);
      }
   }
}
