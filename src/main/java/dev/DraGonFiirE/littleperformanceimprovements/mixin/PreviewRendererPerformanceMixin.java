package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PersistentPreviewBufferOwner;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ToolPreviewMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;

@Mixin(PreviewRenderer.class)
public class PreviewRendererPerformanceMixin implements PersistentPreviewBufferOwner {
   @Unique
   private static final int littleperformanceimprovements$PERSISTENT_BUFFER_MIN_VERTICES = 256;
   @Unique
   private VertexBuffer littleperformanceimprovements$persistentPreviewBuffer;
   @Unique
   private MeshData littleperformanceimprovements$uploadedPreviewMesh;

   @Redirect(
      method = "renderBoxes(Lteam/creative/littletiles/common/level/context/ILittleLevelContext;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;ZLcom/mojang/blaze3d/vertex/MeshData;Ljava/lang/Runnable;)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V")
   )
   private void littleperformanceimprovements$drawLargePreviewFromPersistentBuffer(MeshData mesh) {
      boolean exactLines = mesh.drawState().mode() == Mode.DEBUG_LINES;
      if (!exactLines && mesh.drawState().vertexCount() < 256) {
         BufferUploader.drawWithShader(mesh);
      } else {
         if (this.littleperformanceimprovements$persistentPreviewBuffer != null && this.littleperformanceimprovements$uploadedPreviewMesh == mesh) {
            this.littleperformanceimprovements$persistentPreviewBuffer.bind();
         } else {
            this.littleperformanceimprovements$releasePersistentPreviewBuffer();
            this.littleperformanceimprovements$persistentPreviewBuffer = new VertexBuffer(Usage.STATIC);
            this.littleperformanceimprovements$persistentPreviewBuffer.bind();
            this.littleperformanceimprovements$persistentPreviewBuffer.upload(mesh);
            this.littleperformanceimprovements$uploadedPreviewMesh = mesh;
         }

         try {
            boolean drawn = exactLines && ToolPreviewMode.drawExactOutline(this.littleperformanceimprovements$persistentPreviewBuffer);
            if (!drawn) {
               this.littleperformanceimprovements$persistentPreviewBuffer
                  .drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            }
         } finally {
            VertexBuffer.unbind();
         }
      }
   }

   @Override
   public void littleperformanceimprovements$releasePersistentPreviewBuffer() {
      if (this.littleperformanceimprovements$persistentPreviewBuffer != null) {
         this.littleperformanceimprovements$persistentPreviewBuffer.close();
      }

      this.littleperformanceimprovements$persistentPreviewBuffer = null;
      this.littleperformanceimprovements$uploadedPreviewMesh = null;
   }
}
