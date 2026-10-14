package dev.DraGonFiirE.littleperformanceimprovements.performance;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HeldPreviewBuild {
   private static final Map<Object, HeldPreviewBuild.Job> JOBS = new IdentityHashMap<>();
   private static final Set<Object> STALE = Collections.newSetFromMap(new WeakHashMap<>());
   private static final ThreadLocal<HeldPreviewBuild.Job> CURRENT = new ThreadLocal<>();

   private HeldPreviewBuild() {
   }

   public static <T> CompletableFuture<T> submit(Object var0, Object var1, int var2, Supplier<T> var3, Consumer<T> var4, Runnable var5, Executor var6) {
      HeldPreviewBuild.Job var7 = new HeldPreviewBuild.Job(var0, var1, var2, var5);
      HeldPreviewBuild.Job var8;
      synchronized (JOBS) {
         STALE.remove(var0);
         var8 = JOBS.put(var0, var7);
         if (var8 != null) {
            var8.cancelled = true;
         }
      }

      CompletableFuture var12 = var8 == null ? CompletableFuture.completedFuture(null) : var8.finished;
      CompletableFuture var10 = var12.thenApplyAsync(var4x -> {
         CURRENT.set(var7);
         boolean var5x = false;

         try {
            check();
            Object var6x = var3.get();
            if (var7.cancelled) {
               if (var6x != null) {
                  var4.accept((T)var6x);
               }

               throw new CancellationException("Blueprint left the main hand or preview changed");
            } else {
               var5x = true;
               return var6x;
            }
         } finally {
            try {
               if (!var5x) {
                  try {
                     var7.cleanup.run();
                  } finally {
                     for (AutoCloseable var25 : var7.buffers) {
                        try {
                           var25.close();
                        } catch (Exception var87) {
                        }
                     }
                  }
               }
            } finally {
               CURRENT.remove();
               synchronized (JOBS) {
                  JOBS.remove(var0, var7);
               }

               var7.finished.complete(null);
            }
         }
      }, var6);
      var10.whenComplete((var2x, var3x) -> {
         if (!var7.finished.isDone()) {
            synchronized (JOBS) {
               JOBS.remove(var0, var7);
            }

            var7.finished.complete(null);
         }
      });
      return var10;
   }

   public static void cancel(Object var0) {
      synchronized (JOBS) {
         HeldPreviewBuild.Job var2 = JOBS.get(var0);
         if (var2 != null) {
            var2.cancelled = true;
         }
      }
   }

   public static boolean changed(Object var0, Object var1, int var2) {
      synchronized (JOBS) {
         HeldPreviewBuild.Job var4 = JOBS.get(var0);
         if (STALE.remove(var0)) {
            return true;
         }

         if (var4 == null) {
            return false;
         }

         if (var4.held != var1 || var4.slot != var2) {
            var4.cancelled = true;
         }

         return var4.cancelled;
      }
   }

   public static void observe(Object var0, int var1) {
      synchronized (JOBS) {
         for (HeldPreviewBuild.Job var4 : JOBS.values()) {
            if (var4.held != var0 || var4.slot != var1) {
               var4.cancelled = true;
               STALE.add(var4.owner);
            }
         }
      }
   }

   public static boolean deferCleanup(Object var0) {
      synchronized (JOBS) {
         HeldPreviewBuild.Job var2 = JOBS.get(var0);
         HeldPreviewBuild.Job var3 = CURRENT.get();
         return var2 != null && (var3 == null || var3.owner != var0);
      }
   }

   public static void check() {
      HeldPreviewBuild.Job var0 = CURRENT.get();
      if (var0 != null && var0.cancelled) {
         throw new CancellationException("Obsolete blueprint preview");
      }
   }

   public static <T extends AutoCloseable> T track(T var0) {
      HeldPreviewBuild.Job var1 = CURRENT.get();
      if (var1 != null && var0 != null) {
         var1.buffers.add(var0);
      }

      return (T)var0;
   }

   private static final class Job {
      final Object owner;
      final Object held;
      final int slot;
      final Runnable cleanup;
      final CompletableFuture<Void> finished = new CompletableFuture<>();
      final Set<AutoCloseable> buffers = Collections.newSetFromMap(new IdentityHashMap<>());
      volatile boolean cancelled;

      Job(Object var1, Object var2, int var3, Runnable var4) {
         this.owner = var1;
         this.held = var2;
         this.slot = var3;
         this.cleanup = var4;
      }
   }
}
