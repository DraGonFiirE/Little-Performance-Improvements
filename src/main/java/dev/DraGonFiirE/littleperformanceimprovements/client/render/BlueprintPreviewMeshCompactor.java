package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public final class BlueprintPreviewMeshCompactor {
   private BlueprintPreviewMeshCompactor() {
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh compact(MeshData sourceMesh, int boxCount) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      if (sourceState.vertexCount() <= 0 || sourceState.vertexCount() % 4 != 0) {
         return null;
      } else if (sourceState.mode() == Mode.QUADS && sourceState.format().equals(DefaultVertexFormat.POSITION_COLOR)) {
         return compactQuads(sourceMesh, boxCount);
      } else {
         return sourceState.mode() == Mode.LINES && sourceState.format().equals(DefaultVertexFormat.POSITION_COLOR_NORMAL)
            ? compactLines(sourceMesh, boxCount)
            : null;
      }
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh compactSequentialLines(MeshData sourceMesh, int boxCount) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      return sourceState.vertexCount() > 0
            && sourceState.vertexCount() % 4 == 0
            && sourceState.mode() == Mode.LINES
            && sourceState.format().equals(DefaultVertexFormat.POSITION_COLOR_NORMAL)
         ? compactLinesSequential(sourceMesh, boxCount)
         : null;
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh selectQuadsByAlpha(MeshData sourceMesh, int alpha) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      if (sourceState.vertexCount() > 0
         && sourceState.vertexCount() % 4 == 0
         && sourceState.mode() == Mode.QUADS
         && sourceState.format().equals(DefaultVertexFormat.POSITION_COLOR)) {
         int stride = sourceState.format().getVertexSize();
         int sourceVertexCount = sourceState.vertexCount();
         ByteBuffer source = sourceMesh.vertexBuffer();
         long requiredBytes = (long)sourceVertexCount * stride;
         if (requiredBytes > source.limit()) {
            return null;
         }

         int selectedVertices = 0;
         int vertex = 0;

         while (vertex < sourceVertexCount) {
            boolean selected = true;

            for (int corner = 0; corner < 4; corner++) {
               if (Byte.toUnsignedInt(source.get((vertex + corner) * stride + 15)) != alpha) {
                  selected = false;
                  break;
               }
            }

            if (selected) {
               selectedVertices += 4;
            }

            vertex += 4;
            HeldPreviewBuild.check();
         }

         if (selectedVertices == 0) {
            return null;
         }

         long outputBytes = (long)selectedVertices * stride;
         if (outputBytes > 2147483639L) {
            return null;
         }

         ByteBufferBuilder var10000 = new ByteBufferBuilder((int)outputBytes);
         HeldPreviewBuild.track(var10000);
         ByteBufferBuilder buffers = var10000;

         try {
            long outputPointer = buffers.reserve((int)outputBytes);
            long sourcePointer = MemoryUtil.memAddress(source);
            int outputVertex = 0;
            int vertexx = 0;

            while (vertexx < sourceVertexCount) {
               boolean selected = true;

               for (int corner = 0; corner < 4; corner++) {
                  if (Byte.toUnsignedInt(source.get((vertexx + corner) * stride + 15)) != alpha) {
                     selected = false;
                     break;
                  }
               }

               if (selected) {
                  MemoryUtil.memCopy(sourcePointer + (long)vertexx * stride, outputPointer + (long)outputVertex * stride, stride * 4L);
                  outputVertex += 4;
               }

               vertexx += 4;
               HeldPreviewBuild.check();
            }

            Result vertexResult = buffers.build();
            MeshData selected = new MeshData(
               vertexResult, new DrawState(sourceState.format(), selectedVertices, selectedVertices / 4 * 6, Mode.QUADS, IndexType.least(selectedVertices))
            );
            return new BlueprintPreviewMeshCompactor.CompactedMesh(buffers, selected);
         } catch (Throwable throwable) {
            buffers.close();
            throw throwable;
         }
      } else {
         return null;
      }
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh rebuildWidthAwareLines(MeshData sourceMesh) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      if (sourceState.vertexCount() > 0
         && sourceState.vertexCount() % 4 == 0
         && sourceState.mode() == Mode.LINES
         && sourceState.format().equals(DefaultVertexFormat.POSITION_COLOR_NORMAL)) {
         int sourceStride = sourceState.format().getVertexSize();
         int sourceVertexCount = sourceState.vertexCount();
         long requiredSourceBytes = (long)sourceVertexCount * sourceStride;
         long outputBytes = (long)sourceVertexCount * DefaultVertexFormat.POSITION_COLOR_NORMAL.getVertexSize();
         ByteBuffer source = sourceMesh.vertexBuffer();
         if (requiredSourceBytes <= source.limit() && outputBytes <= 2147483639L) {
            ByteBufferBuilder var10000 = new ByteBufferBuilder((int)outputBytes);
            HeldPreviewBuild.track(var10000);
            ByteBufferBuilder buffers = var10000;

            try {
               BufferBuilder builder = new BufferBuilder(buffers, Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               int vertex = 0;

               while (vertex < sourceVertexCount) {
                  int first = vertex * sourceStride;
                  int second = (vertex + 2) * sourceStride;
                  float ax = source.getFloat(first);
                  float ay = source.getFloat(first + 4);
                  float az = source.getFloat(first + 8);
                  float bx = source.getFloat(second);
                  float by = source.getFloat(second + 4);
                  float bz = source.getFloat(second + 8);
                  float dx = bx - ax;
                  float dy = by - ay;
                  float dz = bz - az;
                  float lengthSquared = dx * dx + dy * dy + dz * dz;
                  if (!(lengthSquared <= 1.0E-12F)) {
                     float inverseLength = (float)(1.0 / Math.sqrt(lengthSquared));
                     dx *= inverseLength;
                     dy *= inverseLength;
                     dz *= inverseLength;
                     builder.addVertex(ax, ay, az)
                        .setColor(
                           Byte.toUnsignedInt(source.get(first + 12)),
                           Byte.toUnsignedInt(source.get(first + 13)),
                           Byte.toUnsignedInt(source.get(first + 14)),
                           Byte.toUnsignedInt(source.get(first + 15))
                        )
                        .setNormal(dx, dy, dz);
                     builder.addVertex(bx, by, bz)
                        .setColor(
                           Byte.toUnsignedInt(source.get(second + 12)),
                           Byte.toUnsignedInt(source.get(second + 13)),
                           Byte.toUnsignedInt(source.get(second + 14)),
                           Byte.toUnsignedInt(source.get(second + 15))
                        )
                        .setNormal(-dx, -dy, -dz);
                  }

                  vertex += 4;
                  HeldPreviewBuild.check();
               }

               MeshData rebuilt = builder.build();
               if (rebuilt == null) {
                  buffers.close();
                  return null;
               } else {

                  return new BlueprintPreviewMeshCompactor.CompactedMesh(buffers, rebuilt);
               }
            } catch (Throwable throwable) {
               buffers.close();
               throw throwable;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static BlueprintPreviewMeshCompactor.CompactedMesh compactLinesSequential(MeshData sourceMesh, int boxCount) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      int sourceStride = sourceState.format().getVertexSize();
      int sourceVertexCount = sourceState.vertexCount();
      int verticesPerBox = verticesPerBox(sourceVertexCount, boxCount, 96);
      int outputStride = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
      int maximumOutputVertices = sourceVertexCount / 2;
      long maximumVertexBytes = (long)maximumOutputVertices * outputStride;
      if (maximumVertexBytes > 2147483639L) {
         return null;
      }

      ByteBuffer source = sourceMesh.vertexBuffer();
      ByteBufferBuilder var10000 = new ByteBufferBuilder((int)maximumVertexBytes);
      HeldPreviewBuild.track(var10000);
      ByteBufferBuilder buffers = var10000;

      try {
         long outputPointer = buffers.reserve((int)maximumVertexBytes);
         long sourcePointer = MemoryUtil.memAddress(source);
         int[] uniqueSegmentOffsets = new int[Math.max(1, verticesPerBox / 4)];
         int outputVertexCount = 0;
         int batchStart = 0;

         while (batchStart < sourceVertexCount) {
            int batchSize = Math.min(verticesPerBox, sourceVertexCount - batchStart);
            int uniqueSegments = 0;

            for (int localVertex = 0; localVertex < batchSize; localVertex += 4) {
               int segmentOffset = (batchStart + localVertex) * sourceStride;
               boolean duplicate = false;

               for (int candidate = 0; candidate < uniqueSegments; candidate++) {
                  if (sameLineSegment(source, segmentOffset, uniqueSegmentOffsets[candidate], sourceStride)) {
                     duplicate = true;
                     break;
                  }
               }

               if (!duplicate) {
                  uniqueSegmentOffsets[uniqueSegments++] = segmentOffset;
                  MemoryUtil.memCopy(sourcePointer + segmentOffset, outputPointer + (long)(outputVertexCount++) * outputStride, outputStride);
                  MemoryUtil.memCopy(
                     sourcePointer + segmentOffset + sourceStride * 2L, outputPointer + (long)(outputVertexCount++) * outputStride, outputStride
                  );
               }
            }

            batchStart += verticesPerBox;
            HeldPreviewBuild.check();
         }

         Result vertexResult = buffers.build();
         MeshData compacted = new MeshData(
            vertexResult,
            new DrawState(DefaultVertexFormat.POSITION_COLOR, outputVertexCount, outputVertexCount, Mode.DEBUG_LINES, IndexType.least(outputVertexCount))
         );
         return new BlueprintPreviewMeshCompactor.CompactedMesh(buffers, compacted);
      } catch (Throwable throwable) {
         buffers.close();
         throw throwable;
      }
   }

   private static BlueprintPreviewMeshCompactor.CompactedMesh compactQuads(MeshData var0, int var1) {
      return SurfaceTrianglePacking.compact(var0, var1);
   }

   private static BlueprintPreviewMeshCompactor.CompactedMesh compactLines(MeshData sourceMesh, int boxCount) {
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      int stride = sourceState.format().getVertexSize();
      int sourceVertexCount = sourceState.vertexCount();
      int verticesPerBox = exactVerticesPerBox(sourceVertexCount, boxCount);
      if (verticesPerBox == 0) {

         return compactGeneratedLines(sourceMesh);
      }

      int outputStride = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
      long maximumOutputVertices = sourceVertexCount / 2L;
      long maximumVertexBytes = maximumOutputVertices * outputStride;
      long maximumIndexBytes = maximumOutputVertices * 4L;
      long totalBytes = maximumVertexBytes + maximumIndexBytes;
      if (totalBytes > 2147483639L) {
         return null;
      }

      ByteBuffer source = sourceMesh.vertexBuffer();
      long requiredSourceBytes = (long)sourceVertexCount * stride;
      if (requiredSourceBytes > source.limit()) {
         return null;
      }

      ByteBufferBuilder var10000 = new ByteBufferBuilder((int)totalBytes);
      HeldPreviewBuild.track(var10000);
      ByteBufferBuilder buffers = var10000;

      try {
         long vertexPointer = buffers.reserve((int)maximumVertexBytes);
         Result vertexResult = buffers.build();
         long indexPointer = buffers.reserve((int)maximumIndexBytes);
         long sourcePointer = MemoryUtil.memAddress(source);
         int[] uniqueSegmentOffsets = new int[Math.max(1, verticesPerBox / 4)];
         int[] uniqueEndpointOffsets = new int[Math.max(2, verticesPerBox / 2)];
         int outputVertexCount = 0;
         int outputIndexCount = 0;
         int outputIndexOffset = 0;
         int batchStart = 0;

         while (batchStart < sourceVertexCount) {
            int batchSize = Math.min(verticesPerBox, sourceVertexCount - batchStart);
            if (batchSize % 4 != 0) {
               buffers.close();
               return null;
            }

            int uniqueSegments = 0;
            int uniqueEndpoints = 0;

            for (int localVertex = 0; localVertex < batchSize; localVertex += 4) {
               int segmentOffset = (batchStart + localVertex) * stride;
               boolean duplicate = false;

               for (int candidate = 0; candidate < uniqueSegments; candidate++) {
                  if (sameLineSegment(source, segmentOffset, uniqueSegmentOffsets[candidate], stride)) {
                     duplicate = true;
                     break;
                  }
               }

               if (!duplicate) {
                  if (uniqueSegments >= uniqueSegmentOffsets.length) {
                     buffers.close();
                     return null;
                  }

                  uniqueSegmentOffsets[uniqueSegments++] = segmentOffset;

                  for (int endpoint = 0; endpoint < 2; endpoint++) {
                     int endpointOffset = segmentOffset + endpoint * stride * 2;
                     if (endpointOffset < 0 || (long)endpointOffset + outputStride > requiredSourceBytes) {
                        buffers.close();
                        return null;
                     }

                     int uniqueEndpoint = -1;

                     for (int candidate = 0; candidate < uniqueEndpoints; candidate++) {
                        if (sameBytes(source, endpointOffset, uniqueEndpointOffsets[candidate], outputStride)) {
                           uniqueEndpoint = candidate;
                           break;
                        }
                     }

                     if (uniqueEndpoint < 0) {
                        if (uniqueEndpoints >= uniqueEndpointOffsets.length || (long)outputVertexCount + uniqueEndpoints >= maximumOutputVertices) {
                           buffers.close();
                           return null;
                        }

                        uniqueEndpoint = uniqueEndpoints;
                        uniqueEndpointOffsets[uniqueEndpoints++] = endpointOffset;
                        MemoryUtil.memCopy(
                           sourcePointer + endpointOffset, vertexPointer + (long)(outputVertexCount + uniqueEndpoint) * outputStride, outputStride
                        );
                     }

                     if (outputIndexCount >= maximumOutputVertices) {
                        buffers.close();
                        return null;
                     }

                     outputIndexOffset = putIndex(indexPointer, outputIndexOffset, outputVertexCount + uniqueEndpoint);
                     outputIndexCount++;
                  }
               }
            }

            outputVertexCount += uniqueEndpoints;
            batchStart += verticesPerBox;
            HeldPreviewBuild.check();
         }

         Result indexResult = buffers.build();
         MeshData compacted = new MeshData(
            vertexResult, new DrawState(DefaultVertexFormat.POSITION_COLOR, outputVertexCount, outputIndexCount, Mode.DEBUG_LINES, IndexType.INT)
         );
         ((MeshDataAccessor)compacted).littleperformanceimprovements$setIndexBuffer(indexResult);

         return new BlueprintPreviewMeshCompactor.CompactedMesh(buffers, compacted);
      } catch (Throwable throwable) {
         buffers.close();
         throw throwable;
      }
   }

   public static BlueprintPreviewMeshCompactor.CompactedMesh compactGeneratedLines(MeshData sourceMesh) {
      OutlineMeshColor.neutralize(sourceMesh);
      HeldPreviewBuild.check();
      DrawState sourceState = sourceMesh.drawState();
      int sourceStride = sourceState.format().getVertexSize();
      int sourceVertexCount = sourceState.vertexCount();
      int outputStride = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
      int outputVertexCount = sourceVertexCount / 2;
      long outputBytes = (long)outputVertexCount * outputStride;
      long requiredSourceBytes = (long)sourceVertexCount * sourceStride;
      ByteBuffer source = sourceMesh.vertexBuffer();
      if (sourceVertexCount % 4 == 0 && outputBytes <= 2147483639L && requiredSourceBytes <= source.limit()) {
         ByteBufferBuilder var10000 = new ByteBufferBuilder((int)outputBytes);
         HeldPreviewBuild.track(var10000);
         ByteBufferBuilder buffers = var10000;

         try {
            long outputPointer = buffers.reserve((int)outputBytes);
            long sourcePointer = MemoryUtil.memAddress(source);
            int outputVertex = 0;
            int sourceVertex = 0;

            while (sourceVertex < sourceVertexCount) {
               long sourceOffset = (long)sourceVertex * sourceStride;
               MemoryUtil.memCopy(sourcePointer + sourceOffset, outputPointer + (long)(outputVertex++) * outputStride, outputStride);
               MemoryUtil.memCopy(sourcePointer + sourceOffset + sourceStride * 2L, outputPointer + (long)(outputVertex++) * outputStride, outputStride);
               sourceVertex += 4;
               HeldPreviewBuild.check();
            }

            Result vertexResult = buffers.build();
            MeshData compacted = new MeshData(
               vertexResult,
               new DrawState(DefaultVertexFormat.POSITION_COLOR, outputVertexCount, outputVertexCount, Mode.DEBUG_LINES, IndexType.least(outputVertexCount))
            );

            return new BlueprintPreviewMeshCompactor.CompactedMesh(buffers, compacted);
         } catch (Throwable throwable) {
            buffers.close();
            throw throwable;
         }
      } else {
         return null;
      }
   }

   private static int verticesPerBox(int vertexCount, int boxCount, int fallback) {
      if (boxCount > 0 && vertexCount % boxCount == 0) {
         int value = vertexCount / boxCount;
         if (value >= 4 && value <= 4096 && value % 4 == 0) {
            return value;
         }
      }

      return fallback;
   }

   private static int exactVerticesPerBox(int vertexCount, int boxCount) {
      if (boxCount > 0 && vertexCount % boxCount == 0) {
         int value = vertexCount / boxCount;
         return value == 96 ? value : 0;
      } else {
         return 0;
      }
   }

   private static boolean sameLineSegment(ByteBuffer source, int first, int second, int stride) {
      int firstEnd = first + stride * 2;
      int secondEnd = second + stride * 2;
      return sameBytes(source, first, second, 16) && sameBytes(source, firstEnd, secondEnd, 16)
         || sameBytes(source, first, secondEnd, 16) && sameBytes(source, firstEnd, second, 16);
   }

   private static boolean sameBytes(ByteBuffer source, int first, int second, int length) {
      int offset;
      for (offset = 0; offset + 8 <= length; offset += 8) {
         if (source.getLong(first + offset) != source.getLong(second + offset)) {
            return false;
         }
      }

      while (offset < length) {
         if (source.get(first + offset) != source.get(second + offset)) {
            return false;
         }

         offset++;
      }

      return true;
   }

   private static int putIndex(long pointer, int offset, int value) {
      MemoryUtil.memPutInt(pointer + offset, value);
      return offset + 4;
   }

   public record CompactedMesh(ByteBufferBuilder buffers, MeshData mesh) implements AutoCloseable {
      @Override
      public void close() {
         this.buffers.close();
      }
   }
}
