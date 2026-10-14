package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.system.MemoryUtil;
import team.creative.littletiles.LittleTiles;

public final class PreparedPreviewCompaction {
   private static final Map<MeshData, BlueprintPreviewMeshCompactor.CompactedMesh> READY = new IdentityHashMap<>();

   private PreparedPreviewCompaction() {
   }

   public static MeshData prepare(MeshData var0, ItemStack var1) {
      if (var0 != null
         && var0.drawState().mode() == Mode.QUADS
         && ((PreviewRenderingConfig)LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewRenderer() == PreviewRendererMode.Default
         && FullDetailPreviewCompaction.eligible(var1)
         && !BlueprintPerformance.usesLowResolutionPreview(var1)) {
         prepareMesh(var0, BlueprintPerformance.boxCount(var1));
      }

      return var0;
   }

   static void prepareMesh(MeshData var0, int var1) {
      BlueprintPreviewMeshCompactor.CompactedMesh var2 = DefaultVertexSharing.compact(var0, var1);
      if (var2 != null) {
         try {
            var2 = trim(var2);
            synchronized (READY) {
               BlueprintPreviewMeshCompactor.CompactedMesh var4 = READY.put(var0, var2);
               if (var4 != null) {
                  var4.close();
               }
            }
         } catch (Throwable var7) {
            var2.close();
            throw var7;
         }
      }
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh take(MeshData var0, int var1) {
      synchronized (READY) {
         BlueprintPreviewMeshCompactor.CompactedMesh var3 = READY.remove(var0);
         if (var3 != null) {
            return var3;
         }
      }

      return DefaultVertexSharing.compact(var0, var1);
   }

   public static void discard(MeshData var0) {
      BlueprintPreviewMeshCompactor.CompactedMesh var1;
      synchronized (READY) {
         var1 = READY.remove(var0);
      }

      if (var1 != null) {
         var1.close();
      }
   }

   private static BlueprintPreviewMeshCompactor.CompactedMesh trim(BlueprintPreviewMeshCompactor.CompactedMesh var0) {
      MeshData var1 = var0.mesh();
      int var2 = Math.multiplyExact(var1.drawState().vertexCount(), var1.drawState().format().getVertexSize());
      ByteBuffer var3 = var1.vertexBuffer();
      if (var3.remaining() == var2) {
         return var0;
      }

      ByteBuffer var4 = var1.indexBuffer();
      if (var2 <= var3.remaining() && var4 != null) {
         ByteBufferBuilder var5 = new ByteBufferBuilder(Math.addExact(var2, var4.remaining()));

         try {
            MemoryUtil.memCopy(MemoryUtil.memAddress(var3), var5.reserve(var2), var2);
            Result var6 = var5.build();
            MemoryUtil.memCopy(MemoryUtil.memAddress(var4), var5.reserve(var4.remaining()), var4.remaining());
            Result var7 = var5.build();
            MeshData var8 = new MeshData(var6, var1.drawState());
            ((MeshDataAccessor)var8).littleperformanceimprovements$setIndexBuffer(var7);
            var0.close();
            return new BlueprintPreviewMeshCompactor.CompactedMesh(var5, var8);
         } catch (Throwable var9) {
            var5.close();
            throw var9;
         }
      } else {
         throw new IllegalStateException("Invalid compacted preview buffers");
      }
   }
}
