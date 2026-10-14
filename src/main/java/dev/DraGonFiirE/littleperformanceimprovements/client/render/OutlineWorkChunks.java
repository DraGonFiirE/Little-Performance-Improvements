package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class OutlineWorkChunks {
   private static final int WORKERS = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
   private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKERS, var0 -> {
      Thread var1 = new Thread(var0, "LPI Outline Geometry");
      var1.setDaemon(true);
      var1.setPriority(2);
      return var1;
   });
   private static final ThreadLocal<OutlineWorkChunks.Context> LOCAL = new ThreadLocal<>();

   public static void check() {
      HeldPreviewBuild.check();
      OutlineWorkChunks.Context var0 = LOCAL.get();
      if (var0 != null && var0.run.cancelled) {
         throw new CancellationException("Outline batch cancelled");
      }
   }

   public static <T> List<T> scratch(boolean var0) {
      OutlineWorkChunks.Context var1 = LOCAL.get();
      if (var1 == null) {
         var1 = new OutlineWorkChunks.Context(new OutlineWorkChunks.Run());
         LOCAL.set(var1);
      }

      ArrayList var2 = var0 ? var1.solids : var1.faces;
      var2.clear();
      return var2;
   }

   public static void releaseScratch() {
      LOCAL.remove();
      OutlineDoubleCuts.release();
   }

   public static <T, R> void process(Collection<T> var0, BiConsumer<T, Consumer<R>> var1, Consumer<R> var2) {
      int var3 = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
      OutlineWorkChunks.Run var4 = new OutlineWorkChunks.Run();
      ArrayList<T> var5 = new ArrayList<>(var0);
      ArrayDeque<CompletableFuture<List<R>>> var6 = new ArrayDeque<>();
      if (var0 instanceof OutlineWorkItems) {
         var0.clear();
      }

      try {
         if (var5.size() < 4096 || var3 == 1) {
            LOCAL.set(new OutlineWorkChunks.Context(var4));

            for (T var30 : var5) {
               check();
               var1.accept(var30, var2);
            }

            return;
         }

         int var7 = 0;

         while (var7 < var5.size() || !var6.isEmpty()) {
            check();

            while (var7 < var5.size() && var6.size() < var3) {
               int var8 = var7;
               int var9 = Math.min(var5.size(), var7 + 64);
               var7 = var9;
               var6.add(CompletableFuture.supplyAsync(() -> {
                  LOCAL.set(new OutlineWorkChunks.Context(var4));

                  try {
                     List<R> var5x = new ArrayList<>();

                     for (int var6x = var8; var6x < var9; var6x++) {
                        check();
                        var1.accept(var5.get(var6x), var5x::add);
                        var5.set(var6x, null);
                     }

                     return var5x;
                  } finally {
                     LOCAL.remove();
                     OutlineDoubleCuts.release();
                  }
               }, POOL));
            }

            CompletableFuture<List<R>> var29 = var6.getFirst();

            List<R> var31;
            while (true) {
               check();

               try {
                  var31 = var29.get(10L, TimeUnit.MILLISECONDS);
                  break;
               } catch (TimeoutException var24) {
               } catch (InterruptedException var25) {
                  Thread.currentThread().interrupt();
                  throw new CancellationException("Outline wait interrupted");
               } catch (ExecutionException var26) {
                  throw new CompletionException(var26.getCause());
               }
            }

            var6.removeFirst();
            check();

            for (R var11 : var31) {
               var2.accept(var11);
            }
         }
      } finally {
         var4.cancelled = true;

         for (CompletableFuture<List<R>> var14 : var6) {
            try {
               var14.handle((var0x, var1x) -> null).join();
            } catch (RuntimeException var23) {
            }
         }

         LOCAL.remove();
         OutlineDoubleCuts.release();
      }
   }

   private static final class Context {
      final OutlineWorkChunks.Run run;
      final ArrayList<Object> solids = new ArrayList<>(32);
      final ArrayList<Object> faces = new ArrayList<>(64);

      Context(OutlineWorkChunks.Run var1) {
         this.run = var1;
      }
   }

   private static final class Run {
      volatile boolean cancelled;
   }
}
