package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ChiselOnlyPreviewMode;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ChiselTexturePlaceholder;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.DefaultInitialPreview;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.DefaultWhileDragging;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.HammerPreview;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreviewShapeGeometry;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SpherePreviewBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SurfaceDragThreshold;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SurfaceWallPreview;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TextureDragShapes;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ToolOutlinePreviewMesh;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ToolPreviewMode;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ToolTexturePreviewMesh;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.WallPreviewDepth;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
import dev.DraGonFiirE.littleperformanceimprovements.performance.ShapePreviewTransform;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.api.common.tool.ILittleShaper;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.tool.shaper.LittleToolShaper;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.grid.LittleGrid;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.EditingOutlinePreview;
import team.creative.littletiles.common.level.context.ILittleLevelContext;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.placement.PreviewMode;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.ShapeRegistry;
import team.creative.littletiles.common.placement.shape.config.HollowThicknessConfig;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.placement.shape.type.LittleShapeBlob;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurve;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCylinder;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;

@Mixin(LittleToolShaper.class)
public abstract class LittleToolShaperPerformanceMixin extends LittleTool {
   @Shadow
   private ShapeSelection builtSelection;
   @Shadow
   private boolean builtLines;
   @Shadow
   private boolean built;
   @Shadow
   private CompletableFuture<BoxRenderResult> worker;
   @Shadow
   private BoxRenderResult builtResult;
   @Shadow
   private LittleShape builtShape;
   @Shadow
   private LittleShapeConfig builtShapeConfig;
   @Unique
   private String littleperformanceimprovements$previewCacheKey = "";
   @Unique
   private volatile long littleperformanceimprovements$previewGeneration;
   @Unique
   private ScheduledFuture<?> littleperformanceimprovements$scheduledPreview;
   @Unique
   private CompletableFuture<BoxRenderResult> littleperformanceimprovements$scheduledPromise;
   @Unique
   private static final ScheduledThreadPoolExecutor littleperformanceimprovements$previewExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
      Thread thread = new Thread(runnable, "Little Performance Improvements shape preview");
      thread.setDaemon(true);
      return thread;
   });

   @Shadow
   protected abstract void removeCache();

   public LittleToolShaperPerformanceMixin(ItemStack stack) {
      super(stack);
   }

   @Inject(method = "tickInternal", at = @At("HEAD"))
   private void littleperformanceimprovements$invalidatePreviewCache(PreviewRenderer renderer, CallbackInfo callback) {
      String key = EditingOutlinePreview.cacheKey(ShapePreviewTransform.cacheKey(this.stack) + "|preview=" + ChiselOnlyPreviewMode.current(this.stack).name(), this.stack);
      if (!key.equals(this.littleperformanceimprovements$previewCacheKey)) {
         this.littleperformanceimprovements$previewCacheKey = key;
         this.removeCache();
      }
   }

   @Redirect(
      method = "tickInternal",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/api/common/tool/ILittleShaper;previewMode(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lteam/creative/littletiles/common/placement/PreviewMode;"
      )
   )
   private PreviewMode littleperformanceimprovements$selectConfiguredPreviewMode(ILittleShaper shaper, Player player, ItemStack stack) {
      // Editing operations use see-through outlines; other chisel operations
      // use the selected renderer independently of LT's legacy previewLines.
      if (EditingOutlinePreview.uses(stack)) return PreviewMode.LINES;
      return stack.getItem() instanceof team.creative.littletiles.common.item.ItemLittleChisel
         ? ToolPreviewMode.littleTilesMode() : shaper.previewMode(player, stack);
   }

   @Inject(method = "removeCache", at = @At("HEAD"))
   private void littleperformanceimprovements$invalidateObsoletePreviewTasks(CallbackInfo callback) {
      this.littleperformanceimprovements$previewGeneration++;
      ScheduledFuture<?> scheduled = this.littleperformanceimprovements$scheduledPreview;
      if (scheduled != null) {
         scheduled.cancel(false);
      }

      CompletableFuture<BoxRenderResult> promise = this.littleperformanceimprovements$scheduledPromise;
      if (promise != null) {
         promise.cancel(false);
      }

      this.littleperformanceimprovements$scheduledPreview = null;
      this.littleperformanceimprovements$scheduledPromise = null;
   }

   @Redirect(
      method = "buildCache",
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
      )
   )
   private CompletableFuture<BoxRenderResult> littleperformanceimprovements$buildRotatedPreview(
      Supplier<BoxRenderResult> original,
      Executor executor,
      PreviewRenderer renderer,
      Level level,
      LittleShape shape,
      LittleGrid grid,
      LittleShapeConfig config,
      boolean lines,
      boolean inside
   ) {
      ItemStack stack = this.stack.copy();
      ShapeSelection selection = this.builtSelection;
      if (EditingOutlinePreview.uses(stack) && !HammerPreview.needsDetail(shape)) {
         try {
            return CompletableFuture.completedFuture(
               dev.DraGonFiirE.littleperformanceimprovements.client.render.HammerPreview.build(shape, selection, config, stack, renderer));
         } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
         }
      }
      long generation = this.littleperformanceimprovements$previewGeneration;
      if (!(TextureDragShapes.defer(shape, stack) & SurfaceDragThreshold.large(selection, stack))) {
         try {
            BoxRenderResult result = PreviewBuildCancellation.run(
               () -> false,
               lines,
               () -> {
                  LittleBoxes boxes = PreviewShapeGeometry.build(shape, selection, config, stack);
                  boxes = ShapePreviewTransform.apply(stack, selection, boxes, lines, "preview");
                  return !boxes.isEmpty()
                     ? littleperformanceimprovements$buildPreviewMesh(renderer, boxes, stack, lines, shape, selection, config)
                     : new BoxRenderResult(boxes, boxes.pos, null, null);
               }
            );
            return CompletableFuture.completedFuture(result);
         } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
         }
      } else {
         this.builtResult = DefaultWhileDragging.build(shape, selection, config, stack, renderer, lines);
         CompletableFuture<BoxRenderResult> promise = new CompletableFuture<>();
         this.littleperformanceimprovements$scheduledPromise = promise;
         this.littleperformanceimprovements$scheduledPreview = littleperformanceimprovements$previewExecutor.schedule(
            () -> {
               try {
                  BoxRenderResult result = PreviewBuildCancellation.run(
                     () -> generation != this.littleperformanceimprovements$previewGeneration,
                     lines,
                     () -> {
                        PreviewBuildCancellation.check();
                        LittleBoxes boxes = PreviewShapeGeometry.build(shape, selection, config, stack);
                        PreviewBuildCancellation.check();
                        boxes = ShapePreviewTransform.apply(stack, selection, boxes, lines, "preview");
                        PreviewBuildCancellation.check();
                        return !boxes.isEmpty()
                           ? littleperformanceimprovements$buildPreviewMesh(renderer, boxes, stack, lines, shape, selection, config)
                           : new BoxRenderResult(boxes, boxes.pos, null, null);
                     }
                  );
                  if (!promise.complete(result)) {
                     result.close();
                  }
               } catch (Throwable error) {
                  promise.completeExceptionally(error);
               }
            },
            150L,
            TimeUnit.MILLISECONDS
         );
         return promise.exceptionally(error -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof CancellationException) {
               return littleperformanceimprovements$emptyResult(selection, grid);
            } else {
               throw new CompletionException(cause);
            }
         });
      }
   }

   @Unique
   private static boolean littleperformanceimprovements$usesBoundaryOutline(LittleShape shape) {
      return shape instanceof LittleShapePolygon
         || shape instanceof LittleShapeBlob
         || shape instanceof LittleShapeCurve
         || shape instanceof LittleShapeCurveWall
         || shape instanceof LittleShapeCylinder
         || shape instanceof LittleShapeSphere;
   }

   @Unique
   private static boolean littleperformanceimprovements$usesExactBoundaryOutline(ItemStack stack, LittleShape shape) {
      return littleperformanceimprovements$usesBoundaryOutline(shape) || EditingOutlinePreview.uses(stack) && shape == ShapeRegistry.DRAG_BOX;
   }

   @Unique
   private static BoxRenderResult littleperformanceimprovements$emptyResult(ShapeSelection selection, LittleGrid grid) {
      LittleBoxes boxes = new LittleBoxesSimple(selection.pos, grid);
      return new BoxRenderResult(boxes, boxes.pos, null, null);
   }

   @Unique
   private static BoxRenderResult littleperformanceimprovements$buildPreviewMesh(
      PreviewRenderer renderer, LittleBoxes boxes, ItemStack stack, boolean lines, LittleShape shape, ShapeSelection selection, LittleShapeConfig config
   ) {
      if (EditingOutlinePreview.uses(stack)) {
         // Detailed geometry still uses the inexpensive lines-only renderer.
         return HammerPreview.mesh(renderer, PreviewRenderer.EMPTY, boxes);
      }
      if (ChiselOnlyPreviewMode.current(stack) != PreviewRendererMode.Default && lines && littleperformanceimprovements$usesExactBoundaryOutline(stack, shape)) {
         LittleBoxes display = boxes;
         if (shape instanceof LittleShapeSphere && config instanceof HollowThicknessConfig sphereConfig) {
            display = SpherePreviewBoxes.build(selection, sphereConfig);
         }

         return ToolOutlinePreviewMesh.build(renderer, PreviewRenderer.EMPTY, boxes, display);
      } else {
         return !lines && ChiselOnlyPreviewMode.current(stack) == PreviewRendererMode.Texture
            ? ToolTexturePreviewMesh.build(renderer, boxes, stack)
            : SurfaceWallPreview.build(renderer, PreviewRenderer.EMPTY, boxes, lines, stack, shape);
      }
   }

   @Redirect(
      method = "renderInternal",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/render/overlay/PreviewRenderer;buildBoxes(Lcom/mojang/blaze3d/vertex/PoseStack;Lteam/creative/littletiles/common/math/box/collection/LittleBoxes;Z)Lteam/creative/littletiles/client/render/overlay/PreviewRenderer$BoxRenderResult;"
      )
   )
   private BoxRenderResult littleperformanceimprovements$useBoundaryLinesForTemporaryResult(
      PreviewRenderer renderer, PoseStack pose, LittleBoxes boxes, boolean lines
   ) {
      if (EditingOutlinePreview.uses(this.stack)) {
         return dev.DraGonFiirE.littleperformanceimprovements.client.render.HammerPreview.mesh(renderer, pose, boxes);
      }
      if (ChiselOnlyPreviewMode.current(this.stack) != PreviewRendererMode.Default
         && lines
         && littleperformanceimprovements$usesExactBoundaryOutline(this.stack, this.builtShape)) {
         LittleBoxes display = boxes;
         if (this.builtShape instanceof LittleShapeSphere && this.builtSelection != null && this.builtShapeConfig instanceof HollowThicknessConfig sphereConfig
            )
          {
            display = SpherePreviewBoxes.build(this.builtSelection, sphereConfig);
         }

         return ToolOutlinePreviewMesh.build(renderer, pose, boxes, display);
      } else {
         return !lines && ChiselOnlyPreviewMode.current(this.stack) == PreviewRendererMode.Texture
            ? ToolTexturePreviewMesh.build(renderer, boxes, this.stack.copy())
            : SurfaceWallPreview.build(renderer, pose, boxes, lines, this.stack, this.builtShape);
      }
   }

   @Overwrite
   public BoxRenderResult getShapeResult() {
      CompletableFuture<BoxRenderResult> current = this.worker;
      if (current != null && current.isDone()) {
         this.worker = null;

         try {
            BoxRenderResult completed = current.join();
            if (this.builtResult != null) {
               this.builtResult.close();
            }

            this.builtResult = completed;
         } catch (CancellationException | CompletionException var3) {
         }
      }

      return this.builtResult;
   }

   @Redirect(
      method = "renderInternal",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/render/overlay/PreviewRenderer;renderBoxes(Lteam/creative/littletiles/common/level/context/ILittleLevelContext;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;ZLcom/mojang/blaze3d/vertex/MeshData;Ljava/lang/Runnable;)V"
      )
   )
   private void littleperformanceimprovements$renderConfiguredPreview(
      PreviewRenderer renderer, ILittleLevelContext context, Vec3 camera, BlockPos position, boolean lines, MeshData mesh, Runnable originalSetup
   ) {
      if (DefaultInitialPreview.draw(this.stack, this.builtResult, mesh, this.builtShape, this.builtSelection)) {
         mesh = ChiselTexturePlaceholder.prepare(mesh, renderer, this.builtSelection, this.stack, this.builtResult, lines);
         if (DefaultWhileDragging.mode(this.stack, mesh) == PreviewRendererMode.Default) {
            ChiselOnlyPreviewMode.draw(renderer, context, camera, position, lines, mesh, originalSetup, this.stack);
         } else {
            try {
               boolean texturedMesh = mesh != null && mesh.drawState().format().equals(DefaultVertexFormat.POSITION_TEX_COLOR);
               WallPreviewDepth.draw(renderer, context, camera, position, lines, mesh, () -> ToolPreviewMode.setup(renderer, lines, texturedMesh));
            } finally {
               ToolPreviewMode.reset();
            }
         }
      }
   }

   @Redirect(
      method = "interact",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/render/overlay/PreviewRenderer$BoxRenderResult;boxes()Lteam/creative/littletiles/common/math/box/collection/LittleBoxes;"
      )
   )
   private LittleBoxes littleperformanceimprovements$rebuildExactSphereForPlacement(BoxRenderResult preview) {
      if ((EditingOutlinePreview.uses(this.stack) || this.builtShape instanceof LittleShapeSphere)
         && this.builtSelection != null && this.builtShapeConfig != null) {
         LittleBoxes exact = this.builtShape.build(this.builtSelection, this.builtShapeConfig);
         return ShapePreviewTransform.apply(this.stack, this.builtSelection, exact, this.builtLines, "placement");
      } else {
         return preview.boxes();
      }
   }

   static {
      littleperformanceimprovements$previewExecutor.setRemoveOnCancelPolicy(true);
      littleperformanceimprovements$previewExecutor.allowCoreThreadTimeOut(true);
      littleperformanceimprovements$previewExecutor.setKeepAliveTime(30L, TimeUnit.SECONDS);
   }
}
