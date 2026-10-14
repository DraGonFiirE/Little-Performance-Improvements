package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintPreviewMesh;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintPreviewMeshCompactor;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlueprintSurfaceFaceFilter;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ChiselTransformTextures;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.DefaultPreviewColor;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.FullDetailPreviewCompaction;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.OutlineDepthVertexSharing;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.OutlineLineDepthTexture;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.OutlinePreviewColor;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreparedPreviewCompaction;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreviewFanTriangulation;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleBlueprintOutline;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TextureDrawLayout;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TextureTransparentFaces;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TexturedQuadMeshMerger;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.VeilInteriorPreviewRenderer;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.VeilPreviewLineRenderer;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewCleanup;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.creativecore.client.render.box.QuadGeneratorContext;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.matrix.IntMatrix3c;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.api.common.tool.ILittlePlacer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.tool.LittleToolPlacer;
import team.creative.littletiles.client.tool.LittleToolPlacer.LittleGroupResult;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.placement.PlacementPosition;
import team.creative.littletiles.common.placement.PlacementPreview;
import team.creative.littletiles.common.placement.PreviewMode;
import team.creative.littletiles.common.placement.mode.PlacementMode;

@Mixin(LittleToolPlacer.class)
public abstract class LittleToolPlacerPerformanceMixin extends LittleTool implements HeldPreviewCleanup {
   @Unique
   private static final int littleperformanceimprovements$FILLED_DETAIL_ALPHA_MARKER = 254;
   @Unique
   private static final int littleperformanceimprovements$CACHED_POSITION_BOX_THRESHOLD = 1024;
   @Unique
   private VertexBuffer littleperformanceimprovements$largePreviewVertexBuffer;
   @Unique
   private MeshData littleperformanceimprovements$uploadedLargePreviewMesh;
   @Unique
   private boolean littleperformanceimprovements$largePreviewUsesNativeLines;

   @Unique
   private volatile LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$generatedOutlineDepthMesh;
   @Unique
   private volatile MeshData littleperformanceimprovements$generatedOutlineDepthForMesh;
   @Unique
   private VertexBuffer littleperformanceimprovements$outlineDepthVertexBuffer;
   @Unique
   private volatile LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$generatedInteriorMesh;
   @Unique
   private volatile LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$generatedInteriorBackfaceMesh;
   @Unique
   private volatile MeshData littleperformanceimprovements$generatedInteriorForMesh;
   @Unique
   private volatile LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$generatedTextureMesh;
   @Unique
   private volatile LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$generatedTextureBackfaceMesh;
   @Unique
   private volatile MeshData littleperformanceimprovements$generatedTextureForMesh;
   @Unique
   private VertexBuffer littleperformanceimprovements$interiorVertexBuffer;
   @Unique
   private VertexBuffer littleperformanceimprovements$interiorBackfaceVertexBuffer;
   @Unique
   private VertexBuffer littleperformanceimprovements$textureVertexBuffer;
   @Unique
   private VertexBuffer littleperformanceimprovements$textureBackfaceVertexBuffer;
   @Unique
   private TextureDrawLayout littleperformanceimprovements$textureDrawLayout;

   @Unique
   private boolean littleperformanceimprovements$renderingInteriorFilledPreview;
   @Unique
   private CustomData littleperformanceimprovements$cachedHashData;
   @Unique
   private int littleperformanceimprovements$cachedHash;
   @Shadow
   @Final
   private ILittlePlacer placer;
   @Shadow
   private Level placedLevel;
   @Shadow
   private PlacementPosition placedPosition;
   @Shadow
   private IntMatrix3c builtMatrix;
   @Shadow
   private PlacementMode builtMode;
   @Shadow
   private boolean builtLines;

   public LittleToolPlacerPerformanceMixin(ItemStack stack) {
      super(stack);
   }

   @Redirect(
      method = "lambda$buildCache$1",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/render/overlay/PreviewRenderer;buildBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lteam/creative/creativecore/client/render/box/RenderBox;Lcom/mojang/blaze3d/vertex/BufferBuilder;IZ)V"
      )
   )
   private void littleperformanceimprovements$markFilledDetailQuads(
      PreviewRenderer renderer, PoseStack pose, RenderBox box, BufferBuilder builder, int alpha, boolean lines
   ) {
      HeldPreviewBuild.check();
      int renderedAlpha = !lines
            && littleperformanceimprovements$previewRenderer() != PreviewRendererMode.Default
            && !PreviewFanTriangulation.workerMeshActive()
            && littleperformanceimprovements$isThinSurfaceFeature(box)
         ? 254
         : alpha;
      renderer.buildBox(pose, box, builder, renderedAlpha, lines);
   }

   @Redirect(
      method = "lambda$buildCache$1",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;build()Lcom/mojang/blaze3d/vertex/MeshData;")
   )
   private MeshData littleperformanceimprovements$finishPlacementMesh(BufferBuilder builder) {
      try {
         return PreparedPreviewCompaction.prepare(builder.build(), super.stack);
      } finally {
         PreviewFanTriangulation.workerMeshActive(false);
      }
   }

   @Redirect(method = "buildCache", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/CustomData;hashCode()I"))
   private int littleperformanceimprovements$useCheapLargeBlueprintHash(CustomData data) {
      return littleperformanceimprovements$rendererAwareHash(this.littleperformanceimprovements$cachedContentHash(data));
   }

   @Redirect(method = "tickInternal", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
   private Optional littleperformanceimprovements$useSameCheapHashWhileTicking(Optional optional, Function mapper) {
      return optional.map(value -> littleperformanceimprovements$rendererAwareHash(this.littleperformanceimprovements$cachedContentHash((CustomData)value)));
   }

   @Unique
   private int littleperformanceimprovements$cachedContentHash(CustomData data) {
      if (data != this.littleperformanceimprovements$cachedHashData) {
         this.littleperformanceimprovements$cachedHashData = data;
         this.littleperformanceimprovements$cachedHash = BlueprintPerformance.cheapContentHash(this.stack, data);
      }

      return this.littleperformanceimprovements$cachedHash;
   }

   @Unique
   private static int littleperformanceimprovements$rendererAwareHash(int contentHash) {
      return 31 * contentHash + littleperformanceimprovements$previewRenderer().ordinal();
   }

   @Redirect(
      method = "checkForWorker",
      at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;get(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;")
   )
   private Object littleperformanceimprovements$pollLargePreviewWithoutBlocking(CompletableFuture<?> var1, long var2, TimeUnit var4) throws InterruptedException, ExecutionException, TimeoutException {
      return var1.get(0L, TimeUnit.MILLISECONDS);
   }

   @Redirect(
      method = {"buildCache", "tickInternal"},
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/placement/mode/PlacementMode;getPreviewMode()Lteam/creative/littletiles/common/placement/PreviewMode;"
      )
   )
   private PreviewMode littleperformanceimprovements$useConfiguredPreviewRenderer(PlacementMode mode) {
      if (littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Default) {
         return mode.getPreviewMode();
      } else {
         return littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Outline ? PreviewMode.LINES : PreviewMode.PREVIEWS;
      }
   }

   @Inject(method = "renderInternal", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$renderOnlyConfiguredPreview(
      PreviewRenderer renderer, PoseStack pose, Vec3 camera, boolean lines, CallbackInfo callback
   ) {
      if (littleperformanceimprovements$previewRenderer() != PreviewRendererMode.Default) {
         boolean outline = littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Outline;
         if (lines != outline) {
            callback.cancel();
         }
      }
   }

   @Redirect(
      method = "renderInternal",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 0)
   )
   private void littleperformanceimprovements$drawLargePreviewFromPersistentBuffer(MeshData mesh) {
      if (littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Default) {
         if (this.littleperformanceimprovements$largePreviewVertexBuffer != null && this.littleperformanceimprovements$uploadedLargePreviewMesh == mesh) {
            this.littleperformanceimprovements$largePreviewVertexBuffer.bind();
         } else {
            this.littleperformanceimprovements$closeLargePreviewVertexBuffer();
            this.littleperformanceimprovements$largePreviewVertexBuffer = new VertexBuffer(Usage.STATIC);
            this.littleperformanceimprovements$largePreviewVertexBuffer.bind();
            BlueprintPreviewMeshCompactor.CompactedMesh compacted = mesh.drawState().mode() == Mode.QUADS
                  && FullDetailPreviewCompaction.eligible(this.stack)
                  && !BlueprintPerformance.usesLowResolutionPreview(this.stack)
               ? PreparedPreviewCompaction.take(mesh, BlueprintPerformance.boxCount(this.stack))
               : null;
            if (compacted != null) {
               try (var compactedResource = compacted) {
                  this.littleperformanceimprovements$largePreviewVertexBuffer.upload(compacted.mesh());
                  OutlineLineDepthTexture.invalidate();
               }
            } else {
               this.littleperformanceimprovements$largePreviewVertexBuffer.upload(mesh);
               OutlineLineDepthTexture.invalidate();
            }

            this.littleperformanceimprovements$uploadedLargePreviewMesh = mesh;
         }

         try {
            DefaultPreviewColor.draw(
               this.littleperformanceimprovements$largePreviewVertexBuffer,
               RenderSystem.getModelViewMatrix(),
               RenderSystem.getProjectionMatrix(),
               RenderSystem.getShader()
            );
         } finally {
            VertexBuffer.unbind();
         }
      } else {
         boolean lineMesh = mesh.drawState().mode() == Mode.LINES;
         float lineThickness = littleperformanceimprovements$previewOutlineThickness();
         boolean veilWideLines = lineMesh;
         if (lineMesh && !this.littleperformanceimprovements$largePreviewUsesNativeLines) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
         }

         if (this.littleperformanceimprovements$largePreviewVertexBuffer != null && this.littleperformanceimprovements$uploadedLargePreviewMesh == mesh) {
            this.littleperformanceimprovements$largePreviewVertexBuffer.bind();
         } else {
            boolean ownsGeneratedOutlineDepth = lineMesh && this.littleperformanceimprovements$generatedOutlineDepthForMesh == mesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedOutlineDepth = ownsGeneratedOutlineDepth
               ? this.littleperformanceimprovements$generatedOutlineDepthMesh
               : null;
            boolean ownsGeneratedInterior = !lineMesh && this.littleperformanceimprovements$generatedInteriorForMesh == mesh;
            boolean ownsGeneratedTexture = !lineMesh && this.littleperformanceimprovements$generatedTextureForMesh == mesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedInterior = ownsGeneratedInterior
               ? this.littleperformanceimprovements$generatedInteriorMesh
               : null;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedInteriorBackface = ownsGeneratedInterior
               ? this.littleperformanceimprovements$generatedInteriorBackfaceMesh
               : null;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedTexture = ownsGeneratedTexture
               ? this.littleperformanceimprovements$generatedTextureMesh
               : null;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedTextureBackface = ownsGeneratedTexture
               ? this.littleperformanceimprovements$generatedTextureBackfaceMesh
               : null;
            if (ownsGeneratedInterior) {
               this.littleperformanceimprovements$generatedInteriorMesh = null;
               this.littleperformanceimprovements$generatedInteriorBackfaceMesh = null;
               this.littleperformanceimprovements$generatedInteriorForMesh = null;
            }

            if (ownsGeneratedOutlineDepth) {
               this.littleperformanceimprovements$generatedOutlineDepthMesh = null;
               this.littleperformanceimprovements$generatedOutlineDepthForMesh = null;
            }

            if (ownsGeneratedTexture) {
               this.littleperformanceimprovements$generatedTextureMesh = null;
               this.littleperformanceimprovements$generatedTextureBackfaceMesh = null;
               this.littleperformanceimprovements$generatedTextureForMesh = null;
            }

            this.littleperformanceimprovements$closeLargePreviewVertexBuffer();
            this.littleperformanceimprovements$largePreviewVertexBuffer = new VertexBuffer(Usage.STATIC);
            this.littleperformanceimprovements$uploadGeneratedOutlineDepthMesh(generatedOutlineDepth);
            if (!lineMesh) {
               this.littleperformanceimprovements$uploadGeneratedInteriorMesh(generatedInterior, generatedInteriorBackface);
               this.littleperformanceimprovements$uploadGeneratedTextureMesh(generatedTexture, generatedTextureBackface);
            }

            this.littleperformanceimprovements$largePreviewVertexBuffer.bind();
            this.littleperformanceimprovements$largePreviewUsesNativeLines = false;

            BlueprintPreviewMeshCompactor.CompactedMesh compacted = null;
            if (lineMesh || FullDetailPreviewCompaction.eligible(this.stack) && !BlueprintPerformance.usesLowResolutionPreview(this.stack)) {
               compacted = PreparedPreviewCompaction.take(mesh, BlueprintPerformance.boxCount(this.stack));
            }

            if (compacted != null) {
               try (var compactedResource = compacted) {
                  if (lineMesh) {

                  }

                  this.littleperformanceimprovements$largePreviewVertexBuffer.upload(compacted.mesh());
                  OutlineLineDepthTexture.invalidate();
                  this.littleperformanceimprovements$largePreviewUsesNativeLines = compacted.mesh().drawState().mode() == Mode.DEBUG_LINES;
                  if (lineMesh) {
                  }
               }
            } else {
               if (lineMesh) {

               }

               this.littleperformanceimprovements$largePreviewVertexBuffer.upload(mesh);
               OutlineLineDepthTexture.invalidate();
               if (lineMesh) {
               }
            }

            this.littleperformanceimprovements$uploadedLargePreviewMesh = mesh;
         }

         if (lineMesh && this.littleperformanceimprovements$outlineDepthVertexBuffer != null) {

            littleperformanceimprovements$drawOutlineDepthPrepass(this.littleperformanceimprovements$outlineDepthVertexBuffer);

            this.littleperformanceimprovements$largePreviewVertexBuffer.bind();
         }

         if (this.littleperformanceimprovements$largePreviewUsesNativeLines) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            if (veilWideLines) {
               RenderSystem.disableCull();
            }

            RenderSystem.setShaderColor(
               littleperformanceimprovements$previewRed(),
               littleperformanceimprovements$previewGreen(),
               littleperformanceimprovements$previewBlue(),
               littleperformanceimprovements$previewOutlineAlpha()
            );
            if (!veilWideLines) {
               RenderSystem.lineWidth(lineThickness);
            }
         }

         try {
            if (lineMesh) {
               boolean drawnByVeil = false;
               if (this.littleperformanceimprovements$largePreviewUsesNativeLines && veilWideLines) {
                  try {
                     RenderSystem.disableCull();
                     drawnByVeil = VeilPreviewLineRenderer.draw(
                        this.littleperformanceimprovements$largePreviewVertexBuffer,
                        lineThickness,
                        littleperformanceimprovements$previewRed(),
                        littleperformanceimprovements$previewGreen(),
                        littleperformanceimprovements$previewBlue(),
                        littleperformanceimprovements$previewOutlineAlpha()
                     );
                  } finally {
                     RenderSystem.enableCull();
                  }
               }

               if (!drawnByVeil) {
                  this.littleperformanceimprovements$largePreviewVertexBuffer
                     .drawWithShader(
                        RenderSystem.getModelViewMatrix(),
                        RenderSystem.getProjectionMatrix(),
                        this.littleperformanceimprovements$largePreviewUsesNativeLines ? GameRenderer.getPositionColorShader() : RenderSystem.getShader()
                     );
               }

               return;
            }

            this.littleperformanceimprovements$renderingInteriorFilledPreview = true;
            if (littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Texture
               && this.littleperformanceimprovements$textureVertexBuffer != null) {
               try {
                  RenderSystem.disableCull();
                  boolean resolved = littleperformanceimprovements$drawNearestTexturedSurface(
                     this.littleperformanceimprovements$textureVertexBuffer,
                     this.littleperformanceimprovements$textureBackfaceVertexBuffer != null
                        ? this.littleperformanceimprovements$textureBackfaceVertexBuffer
                        : this.littleperformanceimprovements$textureVertexBuffer,
                     this.littleperformanceimprovements$textureDrawLayout
                  );

                  if (resolved) {
                     return;
                  }
               } finally {
                  RenderSystem.enableCull();
               }
            }

            if (this.littleperformanceimprovements$interiorVertexBuffer == null) {
               try {
                  RenderSystem.disableCull();
                  littleperformanceimprovements$drawNearestFilledSurface(
                     this.littleperformanceimprovements$largePreviewVertexBuffer, this.littleperformanceimprovements$largePreviewVertexBuffer, false
                  );
                  return;
               } finally {
                  RenderSystem.enableCull();
               }
            }

            try {
               RenderSystem.disableCull();
               littleperformanceimprovements$drawNearestFilledSurface(
                  this.littleperformanceimprovements$interiorVertexBuffer,
                  this.littleperformanceimprovements$interiorBackfaceVertexBuffer != null
                     ? this.littleperformanceimprovements$interiorBackfaceVertexBuffer
                     : this.littleperformanceimprovements$interiorVertexBuffer,
                  false
               );
            } finally {
               RenderSystem.enableCull();
            }
         } finally {
            VertexBuffer.unbind();
            if (this.littleperformanceimprovements$largePreviewUsesNativeLines) {
               RenderSystem.lineWidth(1.0F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
            }
         }
      }
   }

   @Redirect(
      method = "renderInternal",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 1)
   )
   private void littleperformanceimprovements$hideInteriorStructurePositioningOverlay(MeshData mesh) {
      if (littleperformanceimprovements$previewRenderer() != PreviewRendererMode.Outline
         && littleperformanceimprovements$previewRenderer() != PreviewRendererMode.Default) {
         mesh.close();
      } else {
         BufferUploader.drawWithShader(mesh);
      }
   }

   @Redirect(
      method = "getPositingCubes",
      at = @At(value = "INVOKE", target = "Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;hasStructure()Z")
   )
   private boolean littleperformanceimprovements$skipHiddenFilledPositioningGeometry(LittleGroup group) {
      return (
            littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Outline
               || littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Default
         )
         && group.hasStructure();
   }

   @Inject(method = "renderInternal", at = @At("HEAD"))
   private void littleperformanceimprovements$resetInteriorRenderState(
      PreviewRenderer renderer, PoseStack pose, Vec3 camera, boolean lines, CallbackInfo callback
   ) {
      this.littleperformanceimprovements$renderingInteriorFilledPreview = false;
   }

   @Unique
   private static void littleperformanceimprovements$drawNearestFilledSurface(VertexBuffer buffer, VertexBuffer interiorBuffer, boolean highlightThinFeatures) {
      float alpha = littleperformanceimprovements$previewSurfaceAlpha();
      if (!VeilInteriorPreviewRenderer.drawResolved(
         buffer,
         interiorBuffer,
         littleperformanceimprovements$previewRed(),
         littleperformanceimprovements$previewGreen(),
         littleperformanceimprovements$previewBlue(),
         alpha,
         0.02F,
         true,
         highlightThinFeatures
      )) {
         RenderSystem.enableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(true);
         RenderSystem.colorMask(false, false, false, false);

         try {
            littleperformanceimprovements$drawFilledSurfacePass(buffer, 1.0F, false);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
            RenderSystem.depthFunc(514);
            RenderSystem.setShaderColor(
               littleperformanceimprovements$previewRed(), littleperformanceimprovements$previewGreen(), littleperformanceimprovements$previewBlue(), alpha
            );
            littleperformanceimprovements$drawFilledSurfacePass(buffer, alpha, false);
         } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
         }
      }
   }

   @Unique
   private static void littleperformanceimprovements$drawOutlineDepthPrepass(VertexBuffer buffer) {
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(false, false, false, false);
      RenderSystem.disableCull();
      GL11.glEnable(32823);
      GL11.glPolygonOffset(1.0F, 1.0F);

      try {
         buffer.bind();
         if (!OutlineLineDepthTexture.captureSurface(buffer)) {
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
         }
      } finally {
         GL11.glPolygonOffset(0.0F, 0.0F);
         GL11.glDisable(32823);
         RenderSystem.colorMask(true, true, true, true);
         RenderSystem.depthMask(false);
         RenderSystem.enableCull();
      }
   }

   @Unique
   private static boolean littleperformanceimprovements$drawNearestTexturedSurface(
      VertexBuffer buffer, VertexBuffer interiorBuffer, TextureDrawLayout drawLayout
   ) {
      return VeilInteriorPreviewRenderer.drawResolvedTextured(buffer, interiorBuffer, littleperformanceimprovements$previewTextureAlpha(), 0.02F, drawLayout);
   }

   @Unique
   private static void littleperformanceimprovements$drawFilledSurfacePass(VertexBuffer buffer, float alpha, boolean deduplicatePass) {
      if (!VeilInteriorPreviewRenderer.draw(
         buffer,
         littleperformanceimprovements$previewRed(),
         littleperformanceimprovements$previewGreen(),
         littleperformanceimprovements$previewBlue(),
         alpha,
         0.02F,
         deduplicatePass
      )) {
         RenderSystem.setShaderColor(
            littleperformanceimprovements$previewRed(), littleperformanceimprovements$previewGreen(), littleperformanceimprovements$previewBlue(), alpha
         );
         buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
      }
   }

   @Unique
   private static PreviewRenderingConfig littleperformanceimprovements$previewConfig() {
      return (PreviewRenderingConfig)LittleTiles.CONFIG.rendering;
   }

   @Unique
   private static PreviewRendererMode littleperformanceimprovements$previewRenderer() {
      return littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewRenderer();
   }

   @Unique
   private static Color littleperformanceimprovements$previewColor() {
      Color color = littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewColor();
      return color != null ? color : Color.WHITE;
   }

   @Unique
   private static float littleperformanceimprovements$previewRed() {
      return OutlinePreviewColor.channel(0, littleperformanceimprovements$previewColor().getRedDecimal());
   }

   @Unique
   private static float littleperformanceimprovements$previewGreen() {
      return OutlinePreviewColor.channel(1, littleperformanceimprovements$previewColor().getGreenDecimal());
   }

   @Unique
   private static float littleperformanceimprovements$previewBlue() {
      return OutlinePreviewColor.channel(2, littleperformanceimprovements$previewColor().getBlueDecimal());
   }

   @Unique
   private static float littleperformanceimprovements$previewOutlineThickness() {
      return (float)Math.clamp(littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewOutlineThickness(), 0.25, 16.0);
   }

   @Unique
   private static float littleperformanceimprovements$previewOutlineAlpha() {
      return (float)Math.clamp(littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewOutlineAlpha(), 0.0, 1.0);
   }

   @Unique
   private static float littleperformanceimprovements$previewSurfaceAlpha() {
      return (float)Math.clamp(littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewSurfaceAlpha(), 0.0, 1.0);
   }

   @Unique
   private static float littleperformanceimprovements$previewTextureAlpha() {
      return (float)Math.clamp(littleperformanceimprovements$previewConfig().littleperformanceimprovements$getPreviewTextureAlpha(), 0.0, 1.0);
   }

   @Unique
   private static boolean littleperformanceimprovements$isThinSurfaceFeature(RenderBox box) {
      if (box instanceof LittleRenderBox && !(box instanceof LittleRenderBoxTransformable)) {
         double epsilon = 1.0E-6;
         return box.maxX - box.minX < 1.0 - epsilon || box.maxY - box.minY < 1.0 - epsilon || box.maxZ - box.minZ < 1.0 - epsilon;
      } else {
         return false;
      }
   }

   @Unique
   @Inject(method = "removeCache", at = @At("HEAD"))
   private void littleperformanceimprovements$releaseLargePreviewVertexBuffer(CallbackInfo callback) {
      this.littleperformanceimprovements$closeLargePreviewVertexBuffer();
      this.littleperformanceimprovements$discardGeneratedInteriorMesh();
   }

   @Unique
   private void littleperformanceimprovements$closeLargePreviewVertexBuffer() {
      if (this.littleperformanceimprovements$largePreviewVertexBuffer != null) {
         this.littleperformanceimprovements$largePreviewVertexBuffer.close();
         this.littleperformanceimprovements$largePreviewVertexBuffer = null;
      }

      this.littleperformanceimprovements$uploadedLargePreviewMesh = null;
      this.littleperformanceimprovements$largePreviewUsesNativeLines = false;

      if (this.littleperformanceimprovements$outlineDepthVertexBuffer != null) {
         this.littleperformanceimprovements$outlineDepthVertexBuffer.close();
         this.littleperformanceimprovements$outlineDepthVertexBuffer = null;
      }

      if (this.littleperformanceimprovements$interiorVertexBuffer != null) {
         this.littleperformanceimprovements$interiorVertexBuffer.close();
         this.littleperformanceimprovements$interiorVertexBuffer = null;
      }

      if (this.littleperformanceimprovements$interiorBackfaceVertexBuffer != null) {
         this.littleperformanceimprovements$interiorBackfaceVertexBuffer.close();
         this.littleperformanceimprovements$interiorBackfaceVertexBuffer = null;
      }

      if (this.littleperformanceimprovements$textureVertexBuffer != null) {
         this.littleperformanceimprovements$textureVertexBuffer.close();
         this.littleperformanceimprovements$textureVertexBuffer = null;
      }

      if (this.littleperformanceimprovements$textureBackfaceVertexBuffer != null) {
         this.littleperformanceimprovements$textureBackfaceVertexBuffer.close();
         this.littleperformanceimprovements$textureBackfaceVertexBuffer = null;
      }

      if (this.littleperformanceimprovements$textureDrawLayout != null) {
         this.littleperformanceimprovements$textureDrawLayout.close();
         this.littleperformanceimprovements$textureDrawLayout = null;
      }

   }

   @Unique
   private void littleperformanceimprovements$discardGeneratedInteriorMesh() {
      if (!HeldPreviewBuild.deferCleanup(this)) {
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedInterior = this.littleperformanceimprovements$generatedInteriorMesh;
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedInteriorBackface = this.littleperformanceimprovements$generatedInteriorBackfaceMesh;
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedTexture = this.littleperformanceimprovements$generatedTextureMesh;
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedTextureBackface = this.littleperformanceimprovements$generatedTextureBackfaceMesh;
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedOutlineDepth = this.littleperformanceimprovements$generatedOutlineDepthMesh;
         this.littleperformanceimprovements$generatedInteriorMesh = null;
         this.littleperformanceimprovements$generatedInteriorBackfaceMesh = null;
         this.littleperformanceimprovements$generatedInteriorForMesh = null;
         this.littleperformanceimprovements$generatedTextureMesh = null;
         this.littleperformanceimprovements$generatedTextureBackfaceMesh = null;
         this.littleperformanceimprovements$generatedTextureForMesh = null;
         this.littleperformanceimprovements$generatedOutlineDepthMesh = null;
         this.littleperformanceimprovements$generatedOutlineDepthForMesh = null;
         if (generatedInterior != null) {
            generatedInterior.close();
         }

         if (generatedInteriorBackface != null) {
            generatedInteriorBackface.close();
         }

         if (generatedTexture != null) {
            generatedTexture.close();
         }

         if (generatedTextureBackface != null) {
            generatedTextureBackface.close();
         }

         if (generatedOutlineDepth != null) {
            generatedOutlineDepth.close();
         }
      }
   }

   @Redirect(
      method = "lambda$buildCache$1",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/api/common/tool/ILittlePlacer;get(Lnet/minecraft/world/item/ItemStack;Z)Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;"
      )
   )
   private LittleGroup littleperformanceimprovements$loadOnlyBoundedPreviewWhileHolding(ILittlePlacer placer, ItemStack stack, boolean lowResolution) {
      HeldPreviewBuild.check();
      return BlueprintPerformance.usesLowResolutionPreview(stack) ? BlueprintPreviewMesh.create(stack) : placer.get(stack, lowResolution);
   }

   @Inject(method = "lambda$buildCache$1", at = @At("RETURN"))
   private void littleperformanceimprovements$associateGeneratedInteriorMesh(CallbackInfoReturnable<LittleGroupResult> callback) {
      LittleGroupResult result = callback.getReturnValue();
      if (result != null) {
         MeshData mesh = result.data();
         if (this.builtLines) {
            this.littleperformanceimprovements$generatedOutlineDepthForMesh = mesh;
         } else {
            this.littleperformanceimprovements$generatedInteriorForMesh = mesh;
            this.littleperformanceimprovements$generatedTextureForMesh = mesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh interior = this.littleperformanceimprovements$generatedInteriorMesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh interiorBackface = this.littleperformanceimprovements$generatedInteriorBackfaceMesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh texture = this.littleperformanceimprovements$generatedTextureMesh;
            LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh textureBackface = this.littleperformanceimprovements$generatedTextureBackfaceMesh;

         }
      }
   }

   @Redirect(
      method = "lambda$buildCache$1",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;getPlaceBoxes(Lteam/creative/littletiles/common/math/vec/LittleVec;)Ljava/util/List;"
      )
   )
   private List<RenderBox> littleperformanceimprovements$buildLowResolutionPlacementMesh(
      LittleGroup group, LittleVec offset, PreviewRenderer renderer, IntMatrix3c matrix
   ) {
      HeldPreviewBuild.check();
      PreviewFanTriangulation.workerMeshActive(false);
      PreviewRendererMode mode = littleperformanceimprovements$previewRenderer();
      if (mode == PreviewRendererMode.Default) {
         return group.getPlaceBoxes(offset);
      }

      List<RenderBox> boxes = mode == PreviewRendererMode.Texture
         ? littleperformanceimprovements$materialPlaceBoxes(group, offset)
         : group.getPlaceBoxes(offset);

      if (this.builtLines) {
         LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh previousOutlineDepth = this.littleperformanceimprovements$generatedOutlineDepthMesh;
         this.littleperformanceimprovements$generatedOutlineDepthMesh = null;
         this.littleperformanceimprovements$generatedOutlineDepthForMesh = null;
         if (previousOutlineDepth != null) {
            previousOutlineDepth.close();
         }

         List<RenderBox> filtered;
         try {
            filtered = SimpleBlueprintOutline.retainSurfaceOutline(boxes, group.getGrid());
            this.littleperformanceimprovements$generatedOutlineDepthMesh = littleperformanceimprovements$buildSurfaceBackfaceMesh(renderer, boxes);
         } catch (Throwable throwable) {
            Logger var22 = LittlePerformanceImprovements.LOGGER;
            HeldPreviewBuild.check();
            var22.error("Failed to derive Outline from exact Surface geometry; using native box edges", throwable);
            boxes = group.getPlaceBoxes(offset);
            filtered = SimpleBlueprintOutline.retainAirTouching(boxes, group.getGrid().count);
         }

         return filtered;
      } else {
         if (mode != PreviewRendererMode.Texture) {
            try {
               BlueprintSurfaceFaceFilter.retainAirTouching(boxes, group.getGrid());
            } catch (Throwable throwable) {
               Logger var10000 = LittlePerformanceImprovements.LOGGER;
               HeldPreviewBuild.check();
               var10000.error("Failed to filter filled blueprint faces; using native placement geometry", throwable);
               boxes = group.getPlaceBoxes(offset);
            }

         }

         try {
            this.littleperformanceimprovements$buildExactInteriorMesh(renderer, boxes, group, offset);
         } catch (Throwable throwable) {
            Logger var21 = LittlePerformanceImprovements.LOGGER;
            HeldPreviewBuild.check();
            var21.error("Failed to build exact filled blueprint companion; using native worker mesh", throwable);
            this.littleperformanceimprovements$discardGeneratedInteriorMesh();
         }

         PreviewFanTriangulation.workerMeshActive(mode == PreviewRendererMode.Surface);
         return boxes;
      }
   }

   @Unique
   private void littleperformanceimprovements$buildExactInteriorMesh(PreviewRenderer renderer, List<RenderBox> boxes, LittleGroup group, LittleVec offset) {
      HeldPreviewBuild.check();
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh previous = this.littleperformanceimprovements$generatedInteriorMesh;
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh previousInteriorBackface = this.littleperformanceimprovements$generatedInteriorBackfaceMesh;
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh previousTexture = this.littleperformanceimprovements$generatedTextureMesh;
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh previousTextureBackface = this.littleperformanceimprovements$generatedTextureBackfaceMesh;
      this.littleperformanceimprovements$generatedInteriorMesh = null;
      this.littleperformanceimprovements$generatedInteriorBackfaceMesh = null;
      this.littleperformanceimprovements$generatedTextureMesh = null;
      this.littleperformanceimprovements$generatedTextureBackfaceMesh = null;
      if (previous != null) {
         previous.close();
      }

      if (previousInteriorBackface != null) {
         previousInteriorBackface.close();
      }

      if (previousTexture != null) {
         previousTexture.close();
      }

      if (previousTextureBackface != null) {
         previousTextureBackface.close();
      }

      if (littleperformanceimprovements$previewRenderer() == PreviewRendererMode.Texture) {

         List<RenderBox> materialBoxes = boxes;

         try {
            TextureTransparentFaces.retainAirTouching(materialBoxes, group.getGrid());
         } catch (Throwable throwable) {
            Logger var28 = LittlePerformanceImprovements.LOGGER;
            HeldPreviewBuild.check();
            var28.error("Failed to filter Texture material faces; using unfiltered material geometry", throwable);
            materialBoxes = littleperformanceimprovements$materialPlaceBoxes(group, offset);
         }

         try (PreviewFanTriangulation ignored = PreviewFanTriangulation.activate()) {
            this.littleperformanceimprovements$buildExactTextureMesh(renderer, materialBoxes);
         }

      }
   }

   @Unique
   private static LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh littleperformanceimprovements$buildSurfaceBackfaceMesh(
      PreviewRenderer renderer, List<RenderBox> boxes
   ) {
      HeldPreviewBuild.check();
      ByteBufferBuilder backing = renderer.createBuffer();

      try {
         BufferBuilder builder = renderer.createBuilder(backing, false);

         try (PreviewFanTriangulation ignored = PreviewFanTriangulation.activate()) {
            for (RenderBox box : boxes) {
               renderer.buildBox(PreviewRenderer.EMPTY, box, builder, 255, false);
               HeldPreviewBuild.check();
            }
         }

         MeshData mesh = builder.build();
         if (mesh == null) {
            backing.close();
            return null;
         }

         if (mesh instanceof MeshDataExtender persistent) {
            persistent.keepAlive(true);
         }

         return new LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh(backing, mesh);
      } catch (Throwable throwable) {
         backing.close();
         Logger var10000 = LittlePerformanceImprovements.LOGGER;
         HeldPreviewBuild.check();
         var10000.error("Failed to build transformable-aware Surface back-face mesh", throwable);
         return null;
      }
   }

   @Unique
   private static List<RenderBox> littleperformanceimprovements$materialPlaceBoxes(LittleGroup group, LittleVec offset) {
      HeldPreviewBuild.check();
      ArrayList<RenderBox> boxes = new ArrayList<>();
      littleperformanceimprovements$collectMaterialPlaceBoxes(group, offset, boxes);
      return boxes;
   }

   @Unique
   private static void littleperformanceimprovements$collectMaterialPlaceBoxes(LittleGroup group, LittleVec offset, List<RenderBox> boxes) {
      float offsetX = group.getGrid().toVanillaGridF(offset.x);
      float offsetY = group.getGrid().toVanillaGridF(offset.y);
      float offsetZ = group.getGrid().toVanillaGridF(offset.z);

      for (LittleTile tile : group) {
         int start = boxes.size();
         tile.addRenderingBoxes(group.getGrid(), boxes);

         for (int i = start; i < boxes.size(); i++) {
            boxes.get(i).add(offsetX, offsetY, offsetZ);
         }
      }

      for (LittleGroup child : group.children.all()) {
         littleperformanceimprovements$collectMaterialPlaceBoxes(child, offset, boxes);
      }
   }

   @Unique
   private void littleperformanceimprovements$buildExactTextureMesh(PreviewRenderer renderer, List<RenderBox> boxes) {
      HeldPreviewBuild.check();
      ByteBufferBuilder backing = renderer.createBuffer();

      try {
         BufferBuilder builder = new BufferBuilder(backing, Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         QuadGeneratorContext context = new QuadGeneratorContext();
         RandomSource random = RandomSource.create();
         Minecraft minecraft = Minecraft.getInstance();
         BlockPos origin = BlockPos.ZERO;
         ModelData baseModelData = renderer.level().getModelData(origin);
         Map<BlockState, BakedModel> modelCache = new HashMap<>();
         Map<BlockState, ModelData> modelDataCache = new HashMap<>();
         Map<BlockState, List<RenderType>> renderTypesCache = new HashMap<>();
         Map<BlockState, Map<Integer, Integer>> tintCache = new HashMap<>();
         Map<BlockState, Map<RenderType, Map<Facing, List<BakedQuad>>>> surfaceTemplateCache = new HashMap<>();
         int transformableBoxes = 0;

         int emittedVertices = 0;

         List<TextureDrawLayout.SourceSpan> drawSpans = new ArrayList<>();
         int boxIndex = 0;

         while (boxIndex < boxes.size()) {
            RenderBox box = boxes.get(boxIndex);
            if (box.state != null) {
               boolean transformable = box instanceof LittleRenderBoxTransformable;
               if (transformable) {
                  transformableBoxes++;
               }

               BakedModel model = modelCache.computeIfAbsent(box.state, state -> {
                  HeldPreviewBuild.check();
                  return minecraft.getBlockRenderer().getBlockModel(state);
               });
               ModelData modelData = modelDataCache.computeIfAbsent(box.state, state -> {
                  HeldPreviewBuild.check();
                  return model.getModelData(renderer.level(), origin, state, baseModelData);
               });
               List<RenderType> renderTypes = renderTypesCache.get(box.state);
               if (renderTypes == null) {
                  random.setSeed(box.state.getSeed(origin));
                  ArrayList<RenderType> collectedRenderTypes = new ArrayList<>();

                  for (RenderType renderType : model.getRenderTypes(box.state, random, modelData)) {
                     collectedRenderTypes.add(renderType);
                     HeldPreviewBuild.check();
                  }

                  renderTypes = List.copyOf(collectedRenderTypes);
                  renderTypesCache.put(box.state, renderTypes);
               }

               boolean partialSurfaceFace = !transformable && littleperformanceimprovements$hasPartialSurfaceFace(box);

               if (!transformable && !partialSurfaceFace) {
                  int minCellX = (int)Math.floor(box.minX);
                  int minCellY = (int)Math.floor(box.minY);
                  int minCellZ = (int)Math.floor(box.minZ);
                  int maxCellX = (int)Math.ceil(box.maxX) - 1;
                  int maxCellY = (int)Math.ceil(box.maxY) - 1;
                  int maxCellZ = (int)Math.ceil(box.maxZ) - 1;
                  int nativeEmittedVertices = 0;
                  int cellX = minCellX;

                  while (cellX <= maxCellX) {
                     int cellY = minCellY;

                     while (cellY <= maxCellY) {
                        int cellZ = minCellZ;

                        while (cellZ <= maxCellZ) {
                           BlockPos boxOffset = new BlockPos(cellX, cellY, cellZ);
                           random.setSeed(box.state.getSeed(origin));

                           for (RenderType renderType : renderTypes) {
                              Facing[] var45 = Facing.VALUES;
                              int var46 = var45.length;
                              int var47 = 0;

                              while (var47 < var46) {
                                 Facing facing = var45[var47];
                                 if (box.shouldRenderFace(facing)) {
                                    for (BakedQuad quad : ChiselTransformTextures.quads(
                                       box, context, renderer.level(), origin, boxOffset, box.state, model, modelData, facing, renderType, random, true, -1
                                    )) {
                                       int[] vertices = quad.getVertices();
                                       int sourceStride = vertices.length / 4;
                                       if (sourceStride < 6) {
                                          HeldPreviewBuild.check();
                                       } else {
                                          float shade = littleperformanceimprovements$textureDirectionalShade(quad, vertices, sourceStride);
                                          int tintRed = 255;
                                          int tintGreen = 255;
                                          int tintBlue = 255;
                                          int tintAlpha = 255;
                                          if (box.color != -1) {
                                             tintRed = ColorUtils.red(box.color);
                                             tintGreen = ColorUtils.green(box.color);
                                             tintBlue = ColorUtils.blue(box.color);
                                             tintAlpha = ColorUtils.alpha(box.color);
                                          } else if (quad.isTinted()) {
                                             int tintIndex = quad.getTintIndex();
                                             int modelTint = tintCache.computeIfAbsent(box.state, ignored -> {
                                                HeldPreviewBuild.check();
                                                return new HashMap<>();
                                             }).computeIfAbsent(tintIndex, ignored -> {
                                                HeldPreviewBuild.check();
                                                return minecraft.getBlockColors().getColor(box.state, renderer.level(), origin, tintIndex);
                                             });
                                             if (modelTint != -1) {
                                                tintRed = modelTint >>> 16 & 0xFF;
                                                tintGreen = modelTint >>> 8 & 0xFF;
                                                tintBlue = modelTint & 0xFF;
                                             }
                                          }

                                          int vertex = 0;

                                          while (vertex < 4) {
                                             int offset = vertex * sourceStride;
                                             float x = Float.intBitsToFloat(vertices[offset]);
                                             float y = Float.intBitsToFloat(vertices[offset + 1]);
                                             float z = Float.intBitsToFloat(vertices[offset + 2]);
                                             int bakedColor = vertices[offset + 3];
                                             int red = Math.round((bakedColor & 0xFF) * tintRed / 255.0F * shade);
                                             int green = Math.round((bakedColor >>> 8 & 0xFF) * tintGreen / 255.0F * shade);
                                             int blue = Math.round((bakedColor >>> 16 & 0xFF) * tintBlue / 255.0F * shade);
                                             int alpha = (bakedColor >>> 24 & 0xFF) * tintAlpha / 255;
                                             float u = Float.intBitsToFloat(vertices[offset + 4]);
                                             float v = Float.intBitsToFloat(vertices[offset + 5]);
                                             builder.addVertex(x, y, z).setUv(u, v).setColor(red, green, blue, alpha);
                                             vertex++;
                                             HeldPreviewBuild.check();
                                          }

                                          nativeEmittedVertices += 4;
                                          HeldPreviewBuild.check();
                                       }
                                    }
                                 }

                                 var47++;
                                 HeldPreviewBuild.check();
                              }

                              HeldPreviewBuild.check();
                           }

                           cellZ++;
                           HeldPreviewBuild.check();
                        }

                        cellY++;
                        HeldPreviewBuild.check();
                     }

                     cellX++;
                     HeldPreviewBuild.check();
                  }

                  cellX = nativeEmittedVertices;

                  if (cellX > 0) {
                     drawSpans.add(littleperformanceimprovements$textureDrawSpan(emittedVertices, emittedVertices + cellX, box));
                     emittedVertices += cellX;
                  }
               } else {
                  int addedVertices = 0;
                  if (!renderTypes.isEmpty()) {
                     addedVertices = this.littleperformanceimprovements$appendSurfaceTextureMesh(
                        renderer, builder, box, model, modelData, renderTypes.get(0), random, minecraft, origin, true, tintCache, surfaceTemplateCache
                     );
                  }

                  if (addedVertices > 0) {
                     drawSpans.add(littleperformanceimprovements$textureDrawSpan(emittedVertices, emittedVertices + addedVertices, box));
                     emittedVertices += addedVertices;
                  }
               }
            }

            boxIndex++;
            HeldPreviewBuild.check();
         }

         MeshData mesh = builder.build();
         if (mesh == null) {
            backing.close();
            return;
         }

         if (transformableBoxes == 0) {
            TexturedQuadMeshMerger.MergedMesh merged = TexturedQuadMeshMerger.merge(mesh, drawSpans);
            if (merged != null) {
               mesh.close();
               backing.close();
               backing = merged.backing();
               mesh = merged.mesh();
               drawSpans.clear();
               drawSpans.addAll(merged.spans());
            }
         }

         if (mesh instanceof MeshDataExtender persistent) {
            persistent.keepAlive(true);
         }

         this.littleperformanceimprovements$generatedTextureMesh = new LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh(
            backing, mesh, TextureDrawLayout.build(mesh, drawSpans)
         );
      } catch (Throwable throwable) {
         backing.close();
         Logger var10000 = LittlePerformanceImprovements.LOGGER;
         HeldPreviewBuild.check();
         var10000.error("Failed to build material-textured blueprint preview; Surface mode remains available", throwable);
      }
   }

   @Unique
   private static TextureDrawLayout.SourceSpan littleperformanceimprovements$textureDrawSpan(int firstVertex, int endVertex, RenderBox box) {
      return new TextureDrawLayout.SourceSpan(firstVertex, endVertex, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
   }

   @Unique
   private int littleperformanceimprovements$appendSurfaceTextureMesh(
      PreviewRenderer renderer,
      BufferBuilder target,
      RenderBox box,
      BakedModel model,
      ModelData modelData,
      RenderType renderType,
      RandomSource random,
      Minecraft minecraft,
      BlockPos origin,
      boolean preservePartialFaceUv,
      Map<BlockState, Map<Integer, Integer>> tintCache,
      Map<BlockState, Map<RenderType, Map<Facing, List<BakedQuad>>>> surfaceTemplateCache
   ) {
      HeldPreviewBuild.check();
      if (!littleperformanceimprovements$hasSurfacePreviewGeometry(box)) {
         return 0;
      }

      ByteBufferBuilder surfaceBacking = renderer.createBuffer();
      int emittedVertices = 0;

      try {
         BufferBuilder surfaceBuilder = renderer.createBuilder(surfaceBacking, false);

         try (PreviewFanTriangulation ignored = PreviewFanTriangulation.activateFacingMarkers()) {
            renderer.buildBox(PreviewRenderer.EMPTY, box, surfaceBuilder, 255, false);
         }

         try (MeshData surfaceMesh = surfaceBuilder.build()) {
            if (surfaceMesh == null) {
               return emittedVertices;
            }

            int vertexCount = surfaceMesh.drawState().vertexCount();
            int stride = surfaceMesh.drawState().format().getVertexSize();
            if (vertexCount >= 4 && stride >= 12) {
               ByteBuffer data = surfaceMesh.vertexBuffer();
               float[] x = new float[4];
               float[] y = new float[4];
               float[] z = new float[4];
               float[] projectedU = new float[4];
               float[] projectedV = new float[4];
               int first = 0;

               while (first + 3 < vertexCount) {
                  int vertex = 0;

                  while (vertex < 4) {
                     int offset = (first + vertex) * stride;
                     x[vertex] = data.getFloat(offset);
                     y[vertex] = data.getFloat(offset + 4);
                     z[vertex] = data.getFloat(offset + 8);
                     vertex++;
                     HeldPreviewBuild.check();
                  }

                  float ax = x[1] - x[0];
                  float ay = y[1] - y[0];
                  float az = z[1] - z[0];
                  float bx = x[2] - x[0];
                  float by = y[2] - y[0];
                  float bz = z[2] - z[0];
                  float nx = ay * bz - az * by;
                  float ny = az * bx - ax * bz;
                  float nz = ax * by - ay * bx;
                  int facingMarker = Byte.toUnsignedInt(data.get(first * stride + 12));
                  Facing facing = facingMarker >= 1 && facingMarker <= Facing.VALUES.length
                     ? Facing.VALUES[facingMarker - 1]
                     : littleperformanceimprovements$outwardFacing(box, x, y, z, nx, ny, nz);
                  Map<Facing, List<BakedQuad>> facingTemplates = surfaceTemplateCache.computeIfAbsent(box.state, ignored -> {
                     HeldPreviewBuild.check();
                     return new HashMap<>();
                  }).computeIfAbsent(renderType, ignored -> {
                     HeldPreviewBuild.check();
                     return new HashMap<>();
                  });
                  List<BakedQuad> templates = facingTemplates.get(facing);
                  if (templates == null) {
                     random.setSeed(box.state.getSeed(origin));
                     templates = model.getQuads(box.state, facing.toVanilla(), random, modelData, renderType);
                     if (templates.isEmpty()) {
                        random.setSeed(box.state.getSeed(origin));
                        templates = model.getQuads(box.state, null, random, modelData, renderType);
                     }

                     templates = List.copyOf(templates);
                     facingTemplates.put(facing, templates);
                  }

                  if (!templates.isEmpty()) {
                     BakedQuad template = templates.getFirst();
                     TextureAtlasSprite sprite = template.getSprite();
                     float[] textureProjection = preservePartialFaceUv ? littleperformanceimprovements$textureProjection(template, sprite, facing) : null;
                     int tintRed = 255;
                     int tintGreen = 255;
                     int tintBlue = 255;
                     int tintAlpha = 255;
                     if (box.color != -1) {
                        tintRed = ColorUtils.red(box.color);
                        tintGreen = ColorUtils.green(box.color);
                        tintBlue = ColorUtils.blue(box.color);
                        tintAlpha = ColorUtils.alpha(box.color);
                     } else if (template.isTinted()) {
                        int tintIndex = template.getTintIndex();
                        int modelTint = tintCache.computeIfAbsent(box.state, ignored -> {
                           HeldPreviewBuild.check();
                           return new HashMap<>();
                        }).computeIfAbsent(tintIndex, ignored -> {
                           HeldPreviewBuild.check();
                           return minecraft.getBlockColors().getColor(box.state, renderer.level(), origin, tintIndex);
                        });
                        if (modelTint != -1) {
                           tintRed = modelTint >>> 16 & 0xFF;
                           tintGreen = modelTint >>> 8 & 0xFF;
                           tintBlue = modelTint & 0xFF;
                        }
                     }

                     float minU = Float.POSITIVE_INFINITY;
                     float minV = Float.POSITIVE_INFINITY;
                     float maxU = Float.NEGATIVE_INFINITY;
                     float maxV = Float.NEGATIVE_INFINITY;
                     int vertexx = 0;

                     while (vertexx < 4) {
                        projectedU[vertexx] = facing.getU(x[vertexx], y[vertexx], z[vertexx]);
                        projectedV[vertexx] = facing.getV(x[vertexx], y[vertexx], z[vertexx]);
                        minU = Math.min(minU, projectedU[vertexx]);
                        minV = Math.min(minV, projectedV[vertexx]);
                        maxU = Math.max(maxU, projectedU[vertexx]);
                        maxV = Math.max(maxV, projectedV[vertexx]);
                        vertexx++;
                        HeldPreviewBuild.check();
                     }

                     float sizeU = Math.max(maxU - minU, 1.0E-6F);
                     float sizeV = Math.max(maxV - minV, 1.0E-6F);
                     float shade = littleperformanceimprovements$textureDirectionalShade(nx, ny, nz, facing);
                     int red = Math.round(tintRed * shade);
                     int green = Math.round(tintGreen * shade);
                     int blue = Math.round(tintBlue * shade);
                     if (textureProjection != null) {
                        emittedVertices += littleperformanceimprovements$appendTiledSurfaceQuad(
                           target, facing, x, y, z, projectedU, projectedV, minU, minV, maxU, maxV, sprite, textureProjection, red, green, blue, tintAlpha
                        );
                     } else {
                        int vertexxx = 0;

                        while (vertexxx < 4) {
                           float normalizedU = (projectedU[vertexxx] - minU) / sizeU;
                           float normalizedV = (projectedV[vertexxx] - minV) / sizeV;
                           float u = sprite.getU(normalizedU);
                           float v = sprite.getV(normalizedV);
                           target.addVertex(x[vertexxx], y[vertexxx], z[vertexxx]).setUv(u, v).setColor(red, green, blue, tintAlpha);
                           vertexxx++;
                           HeldPreviewBuild.check();
                        }

                        emittedVertices += 4;
                     }
                  }

                  first += 4;
                  HeldPreviewBuild.check();
               }

               return emittedVertices;
            } else {
               return emittedVertices;
            }
         }
      } finally {
         surfaceBacking.close();
      }
   }

   @Unique
   private static int littleperformanceimprovements$appendTiledSurfaceQuad(
      BufferBuilder target,
      Facing facing,
      float[] x,
      float[] y,
      float[] z,
      float[] projectedU,
      float[] projectedV,
      float minU,
      float minV,
      float maxU,
      float maxV,
      TextureAtlasSprite sprite,
      float[] textureProjection,
      int red,
      int green,
      int blue,
      int alpha
   ) {
      HeldPreviewBuild.check();
      int emittedVertices = 0;
      int firstCellU = (int)Math.floor(minU + 1.0E-5F);
      int firstCellV = (int)Math.floor(minV + 1.0E-5F);
      int lastCellU = (int)Math.ceil(maxU - 1.0E-5F) - 1;
      int lastCellV = (int)Math.ceil(maxV - 1.0E-5F) - 1;
      List<LittleToolPlacerPerformanceMixin.SurfacePoint> source = new ArrayList<>(4);
      int vertex = 0;

      while (vertex < 4) {
         LittleToolPlacerPerformanceMixin.SurfacePoint point = new LittleToolPlacerPerformanceMixin.SurfacePoint(
            projectedU[vertex], projectedV[vertex], x[vertex], y[vertex], z[vertex]
         );
         if (source.isEmpty() || !littleperformanceimprovements$sameSurfacePoint(source.getLast(), point)) {
            source.add(point);
         }

         vertex++;
         HeldPreviewBuild.check();
      }

      if (source.size() > 1 && littleperformanceimprovements$sameSurfacePoint(source.getFirst(), source.getLast())) {
         source.removeLast();
      }

      if (source.size() < 3) {
         return emittedVertices;
      }

      vertex = firstCellU;

      while (vertex <= lastCellU) {
         float partMinU = Math.max(minU, vertex);
         float partMaxU = Math.min(maxU, vertex + 1.0F);
         if (!(partMaxU - partMinU < 1.0E-6F)) {
            int cellV = firstCellV;

            while (cellV <= lastCellV) {
               float partMinV = Math.max(minV, cellV);
               float partMaxV = Math.min(maxV, cellV + 1.0F);
               if (!(partMaxV - partMinV < 1.0E-6F)) {
                  List<LittleToolPlacerPerformanceMixin.SurfacePoint> clipped = source;
                  clipped = littleperformanceimprovements$clipSurfacePolygon(clipped, true, partMinU, true);
                  clipped = littleperformanceimprovements$clipSurfacePolygon(clipped, true, partMaxU, false);
                  clipped = littleperformanceimprovements$clipSurfacePolygon(clipped, false, partMinV, true);
                  clipped = littleperformanceimprovements$clipSurfacePolygon(clipped, false, partMaxV, false);
                  if (clipped.size() >= 3) {
                     if (clipped.size() != 4) {
                        LittleToolPlacerPerformanceMixin.SurfacePoint first = clipped.getFirst();
                        int vertexx = 1;

                        while (vertexx + 1 < clipped.size()) {
                           LittleToolPlacerPerformanceMixin.SurfacePoint second = clipped.get(vertexx);
                           LittleToolPlacerPerformanceMixin.SurfacePoint third = clipped.get(vertexx + 1);
                           littleperformanceimprovements$appendSurfacePoint(target, first, vertex, cellV, sprite, textureProjection, red, green, blue, alpha);
                           littleperformanceimprovements$appendSurfacePoint(target, second, vertex, cellV, sprite, textureProjection, red, green, blue, alpha);
                           littleperformanceimprovements$appendSurfacePoint(target, third, vertex, cellV, sprite, textureProjection, red, green, blue, alpha);
                           littleperformanceimprovements$appendSurfacePoint(target, third, vertex, cellV, sprite, textureProjection, red, green, blue, alpha);
                           emittedVertices += 4;
                           vertexx++;
                           HeldPreviewBuild.check();
                        }
                     } else {
                        for (LittleToolPlacerPerformanceMixin.SurfacePoint point : clipped) {
                           littleperformanceimprovements$appendSurfacePoint(target, point, vertex, cellV, sprite, textureProjection, red, green, blue, alpha);
                           HeldPreviewBuild.check();
                        }

                        emittedVertices += 4;
                     }
                  }
               }

               cellV++;
               HeldPreviewBuild.check();
            }
         }

         vertex++;
         HeldPreviewBuild.check();
      }

      return emittedVertices;
   }

   @Unique
   private static List<LittleToolPlacerPerformanceMixin.SurfacePoint> littleperformanceimprovements$clipSurfacePolygon(
      List<LittleToolPlacerPerformanceMixin.SurfacePoint> input, boolean uAxis, float boundary, boolean keepGreater
   ) {
      if (input.isEmpty()) {
         return List.of();
      }

      List<LittleToolPlacerPerformanceMixin.SurfacePoint> output = new ArrayList<>(input.size() + 1);
      LittleToolPlacerPerformanceMixin.SurfacePoint previous = input.getLast();
      boolean previousInside = littleperformanceimprovements$surfacePointInside(previous, uAxis, boundary, keepGreater);

      for (LittleToolPlacerPerformanceMixin.SurfacePoint current : input) {
         boolean currentInside = littleperformanceimprovements$surfacePointInside(current, uAxis, boundary, keepGreater);
         if (currentInside != previousInside) {
            output.add(littleperformanceimprovements$surfaceIntersection(previous, current, uAxis, boundary));
         }

         if (currentInside) {
            output.add(current);
         }

         previous = current;
         previousInside = currentInside;
      }

      return output;
   }

   @Unique
   private static boolean littleperformanceimprovements$surfacePointInside(
      LittleToolPlacerPerformanceMixin.SurfacePoint point, boolean uAxis, float boundary, boolean keepGreater
   ) {
      float coordinate = uAxis ? point.u() : point.v();
      return keepGreater ? coordinate >= boundary - 1.0E-6F : coordinate <= boundary + 1.0E-6F;
   }

   @Unique
   private static LittleToolPlacerPerformanceMixin.SurfacePoint littleperformanceimprovements$surfaceIntersection(
      LittleToolPlacerPerformanceMixin.SurfacePoint start, LittleToolPlacerPerformanceMixin.SurfacePoint end, boolean uAxis, float boundary
   ) {
      float startCoordinate = uAxis ? start.u() : start.v();
      float endCoordinate = uAxis ? end.u() : end.v();
      float difference = endCoordinate - startCoordinate;
      float progress = Math.abs(difference) < 1.0E-7F ? 0.0F : (boundary - startCoordinate) / difference;
      progress = Math.max(0.0F, Math.min(1.0F, progress));
      return new LittleToolPlacerPerformanceMixin.SurfacePoint(
         start.u() + (end.u() - start.u()) * progress,
         start.v() + (end.v() - start.v()) * progress,
         start.x() + (end.x() - start.x()) * progress,
         start.y() + (end.y() - start.y()) * progress,
         start.z() + (end.z() - start.z()) * progress
      );
   }

   @Unique
   private static boolean littleperformanceimprovements$sameSurfacePoint(
      LittleToolPlacerPerformanceMixin.SurfacePoint first, LittleToolPlacerPerformanceMixin.SurfacePoint second
   ) {
      return Math.abs(first.u() - second.u()) < 1.0E-6F && Math.abs(first.v() - second.v()) < 1.0E-6F;
   }

   @Unique
   private static void littleperformanceimprovements$appendSurfacePoint(
      BufferBuilder target,
      LittleToolPlacerPerformanceMixin.SurfacePoint point,
      int cellU,
      int cellV,
      TextureAtlasSprite sprite,
      float[] textureProjection,
      int red,
      int green,
      int blue,
      int alpha
   ) {
      HeldPreviewBuild.check();
      float localU = point.u() - cellU;
      float localV = point.v() - cellV;
      float normalizedU = textureProjection[0] * localU + textureProjection[1] * localV + textureProjection[2];
      float normalizedV = textureProjection[3] * localU + textureProjection[4] * localV + textureProjection[5];
      normalizedU = Math.max(0.0F, Math.min(1.0F, normalizedU));
      normalizedV = Math.max(0.0F, Math.min(1.0F, normalizedV));
      target.addVertex(point.x(), point.y(), point.z()).setUv(sprite.getU(normalizedU), sprite.getV(normalizedV)).setColor(red, green, blue, alpha);
   }

   @Unique
   private static float[] littleperformanceimprovements$textureProjection(BakedQuad template, TextureAtlasSprite sprite, Facing facing) {
      int[] vertices = template.getVertices();
      int stride = vertices.length / 4;
      if (stride < 6) {
         return null;
      }

      float[] projectedU = new float[4];
      float[] projectedV = new float[4];
      float[] textureU = new float[4];
      float[] textureV = new float[4];

      for (int vertex = 0; vertex < 4; vertex++) {
         int offset = vertex * stride;
         float x = Float.intBitsToFloat(vertices[offset]);
         float y = Float.intBitsToFloat(vertices[offset + 1]);
         float z = Float.intBitsToFloat(vertices[offset + 2]);
         projectedU[vertex] = facing.getU(x, y, z);
         projectedV[vertex] = facing.getV(x, y, z);
         textureU[vertex] = sprite.getUOffset(Float.intBitsToFloat(vertices[offset + 4]));
         textureV[vertex] = sprite.getVOffset(Float.intBitsToFloat(vertices[offset + 5]));
      }

      for (int first = 0; first < 2; first++) {
         for (int second = first + 1; second < 3; second++) {
            for (int third = second + 1; third < 4; third++) {
               float oneU = projectedU[second] - projectedU[first];
               float oneV = projectedV[second] - projectedV[first];
               float twoU = projectedU[third] - projectedU[first];
               float twoV = projectedV[third] - projectedV[first];
               float determinant = oneU * twoV - twoU * oneV;
               if (!(Math.abs(determinant) < 1.0E-6F)) {
                  float deltaUOne = textureU[second] - textureU[first];
                  float deltaUTwo = textureU[third] - textureU[first];
                  float uFromU = (deltaUOne * twoV - deltaUTwo * oneV) / determinant;
                  float uFromV = (oneU * deltaUTwo - twoU * deltaUOne) / determinant;
                  float uOffset = textureU[first] - uFromU * projectedU[first] - uFromV * projectedV[first];
                  float deltaVOne = textureV[second] - textureV[first];
                  float deltaVTwo = textureV[third] - textureV[first];
                  float vFromU = (deltaVOne * twoV - deltaVTwo * oneV) / determinant;
                  float vFromV = (oneU * deltaVTwo - twoU * deltaVOne) / determinant;
                  float vOffset = textureV[first] - vFromU * projectedU[first] - vFromV * projectedV[first];
                  return new float[]{uFromU, uFromV, uOffset, vFromU, vFromV, vOffset};
               }
            }
         }
      }

      return null;
   }

   @Unique
   private static boolean littleperformanceimprovements$hasPartialSurfaceFace(RenderBox box) {
      for (Facing facing : Facing.VALUES) {
         if (box.getFace(facing).hasCachedFans() && !box.getFace(facing).getCachedFans().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   @Unique
   private static boolean littleperformanceimprovements$hasSurfacePreviewGeometry(RenderBox box) {
      for (Facing facing : Facing.VALUES) {
         if (box.shouldRenderFace(facing)) {
            return true;
         }

         if (box.getFace(facing).hasCachedFans() && !box.getFace(facing).getCachedFans().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   @Unique
   private static Facing littleperformanceimprovements$outwardFacing(RenderBox box, float[] x, float[] y, float[] z, float nx, float ny, float nz) {
      float centerX = (x[0] + x[1] + x[2] + x[3]) * 0.25F;
      float centerY = (y[0] + y[1] + y[2] + y[3]) * 0.25F;
      float centerZ = (z[0] + z[1] + z[2] + z[3]) * 0.25F;
      float boxCenterX = (float)((box.minX + box.maxX) * 0.5);
      float boxCenterY = (float)((box.minY + box.maxY) * 0.5);
      float boxCenterZ = (float)((box.minZ + box.maxZ) * 0.5);
      float xWeight = Math.abs(nx);
      float yWeight = Math.abs(ny);
      float zWeight = Math.abs(nz);
      if (yWeight >= xWeight && yWeight >= zWeight) {
         float side = centerY - boxCenterY;
         return Math.abs(side) > 1.0E-5F ? (side > 0.0F ? Facing.UP : Facing.DOWN) : (ny >= 0.0F ? Facing.UP : Facing.DOWN);
      } else if (zWeight >= xWeight) {
         float side = centerZ - boxCenterZ;
         return Math.abs(side) > 1.0E-5F ? (side > 0.0F ? Facing.SOUTH : Facing.NORTH) : (nz >= 0.0F ? Facing.SOUTH : Facing.NORTH);
      } else {
         float side = centerX - boxCenterX;
         return Math.abs(side) > 1.0E-5F ? (side > 0.0F ? Facing.EAST : Facing.WEST) : (nx >= 0.0F ? Facing.EAST : Facing.WEST);
      }
   }

   @Unique
   private static float littleperformanceimprovements$textureDirectionalShade(float nx, float ny, float nz, Facing facing) {
      float weight = Math.abs(nx) + Math.abs(ny) + Math.abs(nz);
      if (weight < 1.0E-6F) {
         return switch (facing) {
            case DOWN, UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
         };
      } else {
         return (Math.abs(nx) * 0.6F + Math.abs(ny) + Math.abs(nz) * 0.8F) / weight;
      }
   }

   @Unique
   private static float littleperformanceimprovements$textureDirectionalShade(BakedQuad quad, int[] vertices, int stride) {
      if (quad.isShade() && stride >= 3) {
         float x0 = Float.intBitsToFloat(vertices[0]);
         float y0 = Float.intBitsToFloat(vertices[1]);
         float z0 = Float.intBitsToFloat(vertices[2]);
         float x1 = Float.intBitsToFloat(vertices[stride]);
         float y1 = Float.intBitsToFloat(vertices[stride + 1]);
         float z1 = Float.intBitsToFloat(vertices[stride + 2]);
         float x2 = Float.intBitsToFloat(vertices[stride * 2]);
         float y2 = Float.intBitsToFloat(vertices[stride * 2 + 1]);
         float z2 = Float.intBitsToFloat(vertices[stride * 2 + 2]);
         float ax = x1 - x0;
         float ay = y1 - y0;
         float az = z1 - z0;
         float bx = x2 - x0;
         float by = y2 - y0;
         float bz = z2 - z0;
         float nx = ay * bz - az * by;
         float ny = az * bx - ax * bz;
         float nz = ax * by - ay * bx;
         float weight = Math.abs(nx) + Math.abs(ny) + Math.abs(nz);
         if (weight < 1.0E-6F) {
            return switch (quad.getDirection()) {
               case DOWN, UP -> 1.0F;
               case NORTH, SOUTH -> 0.8F;
               case WEST, EAST -> 0.6F;
            };
         } else {
            return (Math.abs(nx) * 0.6F + Math.abs(ny) + Math.abs(nz) * 0.8F) / weight;
         }
      } else {
         return 1.0F;
      }
   }

   @Unique
   private void littleperformanceimprovements$uploadGeneratedInteriorMesh(
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generated, LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedBackface
   ) {
      if (generated != null) {
         try (generated) {

            this.littleperformanceimprovements$interiorVertexBuffer = new VertexBuffer(Usage.STATIC);
            this.littleperformanceimprovements$interiorVertexBuffer.bind();
            this.littleperformanceimprovements$interiorVertexBuffer.upload(generated.mesh());
            OutlineLineDepthTexture.invalidate();
         }

         if (generatedBackface != null) {
            try (generatedBackface) {
               this.littleperformanceimprovements$interiorBackfaceVertexBuffer = new VertexBuffer(Usage.STATIC);
               this.littleperformanceimprovements$interiorBackfaceVertexBuffer.bind();
               this.littleperformanceimprovements$interiorBackfaceVertexBuffer.upload(generatedBackface.mesh());
               OutlineLineDepthTexture.invalidate();
            }
         }
      }
   }

   @Unique
   private void littleperformanceimprovements$uploadGeneratedOutlineDepthMesh(LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generated) {
      if (generated != null) {
         try (generated) {
            this.littleperformanceimprovements$outlineDepthVertexBuffer = new VertexBuffer(Usage.STATIC);
            this.littleperformanceimprovements$outlineDepthVertexBuffer.bind();
            OutlineDepthVertexSharing.upload(this.littleperformanceimprovements$outlineDepthVertexBuffer, generated.mesh());
            OutlineLineDepthTexture.invalidate();
         }
      }
   }

   @Unique
   private void littleperformanceimprovements$uploadGeneratedTextureMesh(
      LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generated, LittleToolPlacerPerformanceMixin.GeneratedInteriorMesh generatedBackface
   ) {
      if (generated != null) {
         try (generated) {
            this.littleperformanceimprovements$textureDrawLayout = generated.drawLayout();

            this.littleperformanceimprovements$textureVertexBuffer = new VertexBuffer(Usage.STATIC);

            this.littleperformanceimprovements$textureVertexBuffer.bind();
            this.littleperformanceimprovements$textureVertexBuffer.upload(generated.mesh());
            OutlineLineDepthTexture.invalidate();
         }

         if (generatedBackface != null) {
            try (generatedBackface) {
               this.littleperformanceimprovements$textureBackfaceVertexBuffer = new VertexBuffer(Usage.STATIC);
               this.littleperformanceimprovements$textureBackfaceVertexBuffer.bind();
               this.littleperformanceimprovements$textureBackfaceVertexBuffer.upload(generatedBackface.mesh());
               OutlineLineDepthTexture.invalidate();
            }
         }
      }
   }

   @Redirect(
      method = "tickInternal",
      at = @At(
         value = "INVOKE",
         target = "Lteam/creative/littletiles/client/tool/LittleToolPlacer;calculatePlacementPosition(Lnet/minecraft/world/level/Level;Lteam/creative/littletiles/common/block/little/tile/group/LittleGroup;)Lteam/creative/littletiles/common/placement/PlacementPosition;"
      )
   )
   private PlacementPosition littleperformanceimprovements$positionLargeBlueprintFromCachedBounds(LittleToolPlacer placer, Level level, LittleGroup group) {
      if (BlueprintPerformance.boxCount(this.stack) >= 1024) {
         group = null;
      }

      return placer.calculatePlacementPosition(level, group);
   }

   @Inject(method = "getPlacement", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$loadExactBlueprintOnlyWhenPlaced(CallbackInfoReturnable<PlacementPreview> callback) {
      if (BlueprintPerformance.usesLowResolutionPreview(this.stack)
         && this.placedLevel != null
         && this.placedPosition != null
         && this.builtMode != null
         && this.builtMatrix != null) {
         LittleGroup exact = LittleGroup.load(BlueprintPerformance.content(this.stack));
         exact.transform(this.builtMatrix, exact.getGrid().rotationCenter);
         exact.convertToSmallest();
         callback.setReturnValue(PlacementPreview.relative(this.placedLevel, exact, this.builtMode, this.placedPosition));
      }
   }

   @Override
   public void littleperformanceimprovements$cleanupHeldPreview() {
      PreviewFanTriangulation.workerMeshActive(false);
      this.littleperformanceimprovements$discardGeneratedInteriorMesh();
   }

   @Unique
   private record GeneratedInteriorMesh(
      ByteBufferBuilder backing, MeshData mesh, TextureDrawLayout drawLayout
   ) implements AutoCloseable {
      private GeneratedInteriorMesh(ByteBufferBuilder backing, MeshData mesh) {
         this(backing, mesh, null);
      }

      @Override
      public void close() {
         try {
            this.mesh.close();
         } finally {
            this.backing.close();
         }
      }
   }

   @Unique
   private record SurfacePoint(float u, float v, float x, float y, float z) {
   }
}
