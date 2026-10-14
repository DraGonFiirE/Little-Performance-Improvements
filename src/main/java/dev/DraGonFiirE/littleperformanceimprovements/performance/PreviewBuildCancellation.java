package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class PreviewBuildCancellation {
   private static final ThreadLocal<BooleanSupplier> CANCELLED = new ThreadLocal<>();
   private static final ThreadLocal<Boolean> LINE_PREVIEW = new ThreadLocal<>();

   private PreviewBuildCancellation() {
   }

   public static <T> T run(BooleanSupplier cancelled, Supplier<T> task) {
      return run(cancelled, false, task);
   }

   public static <T> T run(BooleanSupplier cancelled, boolean lines, Supplier<T> task) {
      CANCELLED.set(cancelled);
      LINE_PREVIEW.set(lines);

      try {
         return task.get();
      } finally {
         LINE_PREVIEW.remove();
         CANCELLED.remove();
      }
   }

   public static void check() {
      BooleanSupplier cancelled = CANCELLED.get();
      if (cancelled != null && cancelled.getAsBoolean()) {
         throw new CancellationException("Obsolete LittleTiles shape preview");
      }
   }

   public static boolean isPreviewBuild() {
      return CANCELLED.get() != null;
   }

   public static boolean isLinePreview() {
      return Boolean.TRUE.equals(LINE_PREVIEW.get());
   }

   public static void awaitStable(long milliseconds) {
      long deadline = System.nanoTime() + milliseconds * 1000000L;

      while (true) {
         check();
         long remaining = deadline - System.nanoTime();
         if (remaining <= 0L) {
            return;
         }

         LockSupport.parkNanos(Math.min(remaining, 10000000L));
      }
   }
}
