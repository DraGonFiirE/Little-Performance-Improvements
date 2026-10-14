package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TexturedQuadMeshMerger {
   private static final int QUADS_PER_BATCH = 32768;
   private static final float SPATIAL_CELL_SIZE = 1.0F;
   private static final int POSITION_GRID = 4096;
   private static final double POSITION_EPSILON = 3.0517578E-5F;
   private static final double UV_EPSILON = 2.0E-6;
   private static final double UV_KEY_SCALE = 1000000.0;

   private TexturedQuadMeshMerger() {
   }

   public static TexturedQuadMeshMerger.MergedMesh merge(MeshData sourceMesh, List<TextureDrawLayout.SourceSpan> sourceSpans) {
      DrawState state = sourceMesh.drawState();
      if (state.mode() != Mode.QUADS
         || !state.format().equals(DefaultVertexFormat.POSITION_TEX_COLOR)
         || state.vertexCount() < 8
         || (state.vertexCount() & 3) != 0) {
         return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
      }

      if (!validSourceSpans(sourceSpans, state.vertexCount())) {
         return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
      }

      int stride = state.format().getVertexSize();
      ByteBuffer source = sourceMesh.vertexBuffer();
      if (stride >= 24 && (long)state.vertexCount() * stride <= source.limit()) {
         ByteBufferBuilder backing = new ByteBufferBuilder(Math.min(16777216, Math.max(1048576, state.vertexCount() * stride / 16)));

         TexturedQuadMeshMerger.MergedMesh var10000;
         try {
            BufferBuilder output = new BufferBuilder(backing, Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            List<TextureDrawLayout.SourceSpan> spans = new ArrayList<>();
            int outputVertices = 0;
            Map<TexturedQuadMeshMerger.SpatialCell, List<TextureDrawLayout.SourceSpan>> spatialSpans = partitionSpatially(sourceSpans);
            TexturedQuadMeshMerger.QuadScratch scratch = new TexturedQuadMeshMerger.QuadScratch();

            for (List<TextureDrawLayout.SourceSpan> cellSpans : spatialSpans.values()) {
               TexturedQuadMeshMerger.Batch batch = new TexturedQuadMeshMerger.Batch();

               for (TextureDrawLayout.SourceSpan span : cellSpans) {
                  int firstQuad = span.firstVertex() / 4;
                  int endQuad = span.endVertex() / 4;

                  while (firstQuad < endQuad) {
                     int available = 32768 - batch.quadCount;
                     int partEnd = Math.min(endQuad, firstQuad + available);
                     classifyRange(source, stride, firstQuad, partEnd, batch, scratch);
                     firstQuad = partEnd;
                     if (batch.quadCount == 32768) {
                        outputVertices = appendBatch(output, source, stride, batch, outputVertices, spans);
                        batch = new TexturedQuadMeshMerger.Batch();
                     }
                  }
               }

               if (batch.quadCount > 0) {
                  outputVertices = appendBatch(output, source, stride, batch, outputVertices, spans);
               }
            }

            MeshData mergedMesh = output.build();
            if (mergedMesh == null) {
               backing.close();
               return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
            }

            if (mergedMesh.drawState().vertexCount() >= state.vertexCount()) {
               mergedMesh.close();
               backing.close();
               return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
            }


            var10000 = new TexturedQuadMeshMerger.MergedMesh(backing, mergedMesh, List.copyOf(spans));
         } catch (Throwable throwable) {
            backing.close();
            LittlePerformanceImprovements.LOGGER.warn("Unable to merge ordinary texture preview; retaining exact source mesh", throwable);
            return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
         }

         return TextureTrianglePacking.finish(var10000, sourceMesh, sourceSpans);
      } else {
         return TextureTrianglePacking.finish(null, sourceMesh, sourceSpans);
      }
   }

   private static boolean validSourceSpans(List<TextureDrawLayout.SourceSpan> spans, int vertexCount) {
      if (spans != null && !spans.isEmpty()) {
         int expectedVertex = 0;

         for (TextureDrawLayout.SourceSpan span : spans) {
            if (span == null
               || span.firstVertex() != expectedVertex
               || span.endVertex() <= span.firstVertex()
               || (span.firstVertex() & 3) != 0
               || (span.endVertex() & 3) != 0
               || span.endVertex() > vertexCount
               || !Float.isFinite(span.minX())
               || !Float.isFinite(span.minY())
               || !Float.isFinite(span.minZ())
               || !Float.isFinite(span.maxX())
               || !Float.isFinite(span.maxY())
               || !Float.isFinite(span.maxZ())) {
               return false;
            }

            expectedVertex = span.endVertex();
         }

         return expectedVertex == vertexCount;
      } else {
         return false;
      }
   }

   private static Map<TexturedQuadMeshMerger.SpatialCell, List<TextureDrawLayout.SourceSpan>> partitionSpatially(List<TextureDrawLayout.SourceSpan> sourceSpans) {
      Map<TexturedQuadMeshMerger.SpatialCell, List<TextureDrawLayout.SourceSpan>> cells = new LinkedHashMap<>();

      for (TextureDrawLayout.SourceSpan span : sourceSpans) {
         TexturedQuadMeshMerger.SpatialCell cell = new TexturedQuadMeshMerger.SpatialCell(
            spatialCell((span.minX() + span.maxX()) * 0.5F), spatialCell((span.minY() + span.maxY()) * 0.5F), spatialCell((span.minZ() + span.maxZ()) * 0.5F)
         );
         cells.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(span);
      }

      return cells;
   }

   private static int spatialCell(float coordinate) {
      return (int)Math.floor(coordinate / 1.0F);
   }

   private static void classifyRange(
      ByteBuffer source, int stride, int firstQuad, int endQuad, TexturedQuadMeshMerger.Batch batch, TexturedQuadMeshMerger.QuadScratch scratch
   ) {
      for (int quad = firstQuad; quad < endQuad; quad++) {
         int sourceOffset = quad * 4 * stride;
         updateBounds(batch, source, sourceOffset, stride);
         batch.quadCount++;
         TexturedQuadMeshMerger.ClassifiedQuad classified = classify(source, sourceOffset, stride, scratch);
         if (classified == null) {
            batch.rawQuadOffsets.add(sourceOffset);
         } else {
            TexturedQuadMeshMerger.MergeGroup group = batch.groups
               .computeIfAbsent(
                  classified.key,
                  ignored -> new TexturedQuadMeshMerger.MergeGroup(
                     classified.axis,
                     classified.firstAxis,
                     classified.secondAxis,
                     classified.plane,
                     classified.cornerOrder,
                     classified.color,
                     classified.uFirst,
                     classified.uSecond,
                     classified.uConstant,
                     classified.vFirst,
                     classified.vSecond,
                     classified.vConstant
                  )
               );
            group.rectangles
               .add(new TexturedQuadMeshMerger.SourceRect(classified.minFirst, classified.minSecond, classified.maxFirst, classified.maxSecond, sourceOffset));
         }
      }
   }

   private static int appendBatch(
      BufferBuilder output, ByteBuffer source, int stride, TexturedQuadMeshMerger.Batch batch, int outputVertices, List<TextureDrawLayout.SourceSpan> spans
   ) {
      int firstOutputVertex = outputVertices;

      for (int sourceOffset : batch.rawQuadOffsets) {
         appendRawQuad(output, source, sourceOffset, stride);
         outputVertices += 4;
      }

      for (TexturedQuadMeshMerger.MergeGroup group : batch.groups.values()) {
         List<TexturedQuadMeshMerger.Rect> merged = mergeRectangles(group.rectangles);
         if (merged.size() >= group.rectangles.size()) {
            for (TexturedQuadMeshMerger.SourceRect rectangle : group.rectangles) {
               appendRawQuad(output, source, rectangle.sourceOffset, stride);
               outputVertices += 4;
            }
         } else {
            for (TexturedQuadMeshMerger.Rect rectangle : merged) {
               appendMergedQuad(output, group, rectangle);
               outputVertices += 4;
            }
         }
      }

      if (outputVertices > firstOutputVertex) {
         spans.add(new TextureDrawLayout.SourceSpan(firstOutputVertex, outputVertices, batch.minX, batch.minY, batch.minZ, batch.maxX, batch.maxY, batch.maxZ));
      }

      return outputVertices;
   }

   private static TexturedQuadMeshMerger.ClassifiedQuad classify(ByteBuffer source, int sourceOffset, int stride, TexturedQuadMeshMerger.QuadScratch scratch) {
      float[] position = scratch.position;
      float[] u = scratch.u;
      float[] v = scratch.v;
      int color = source.getInt(sourceOffset + 20);

      for (int vertex = 0; vertex < 4; vertex++) {
         int offset = sourceOffset + vertex * stride;
         int positionOffset = vertex * 3;
         position[positionOffset] = source.getFloat(offset);
         position[positionOffset + 1] = source.getFloat(offset + 4);
         position[positionOffset + 2] = source.getFloat(offset + 8);
         u[vertex] = source.getFloat(offset + 12);
         v[vertex] = source.getFloat(offset + 16);
         if (source.getInt(offset + 20) != color) {
            return null;
         }
      }

      int axis = constantAxis(position);
      if (axis < 0) {
         return null;
      }

      int firstAxis = axis == 0 ? 1 : 0;
      int secondAxis = axis == 2 ? 1 : 2;
      if (axis == 1) {
         firstAxis = 0;
         secondAxis = 2;
      }

      int plane = grid(position[axis]);
      int minFirst = Integer.MAX_VALUE;
      int minSecond = Integer.MAX_VALUE;
      int maxFirst = Integer.MIN_VALUE;
      int maxSecond = Integer.MIN_VALUE;
      int[] first = scratch.first;
      int[] second = scratch.second;

      for (int vertex = 0; vertex < 4; vertex++) {
         int positionOffset = vertex * 3;
         if (!nearGrid(position[positionOffset + axis], plane)) {
            return null;
         }

         first[vertex] = grid(position[positionOffset + firstAxis]);
         second[vertex] = grid(position[positionOffset + secondAxis]);
         if (!nearGrid(position[positionOffset + firstAxis], first[vertex]) || !nearGrid(position[positionOffset + secondAxis], second[vertex])) {
            return null;
         }

         minFirst = Math.min(minFirst, first[vertex]);
         minSecond = Math.min(minSecond, second[vertex]);
         maxFirst = Math.max(maxFirst, first[vertex]);
         maxSecond = Math.max(maxSecond, second[vertex]);
      }

      if (minFirst < maxFirst && minSecond < maxSecond) {
         int[] cornerOrder = scratch.cornerOrder;
         int[] cornerVertex = scratch.cornerVertex;
         cornerVertex[0] = -1;
         cornerVertex[1] = -1;
         cornerVertex[2] = -1;
         cornerVertex[3] = -1;

         for (int vertex = 0; vertex < 4; vertex++) {
            int firstBit = first[vertex] == maxFirst ? 1 : (first[vertex] == minFirst ? 0 : -1);
            int secondBit = second[vertex] == maxSecond ? 1 : (second[vertex] == minSecond ? 0 : -1);
            if (firstBit < 0 || secondBit < 0) {
               return null;
            }

            int corner = firstBit | secondBit << 1;
            if (cornerVertex[corner] >= 0) {
               return null;
            }

            cornerVertex[corner] = vertex;
            cornerOrder[vertex] = corner;
         }

         for (int vertex : cornerVertex) {
            if (vertex < 0) {
               return null;
            }
         }

         double minFirstPosition = minFirst / 4096.0;
         double minSecondPosition = minSecond / 4096.0;
         double firstLength = (maxFirst - minFirst) / 4096.0;
         double secondLength = (maxSecond - minSecond) / 4096.0;
         int c00 = cornerVertex[0];
         int c10 = cornerVertex[1];
         int c01 = cornerVertex[2];
         int c11 = cornerVertex[3];
         double uFirst = (u[c10] - u[c00]) / firstLength;
         double uSecond = (u[c01] - u[c00]) / secondLength;
         double uConstant = u[c00] - uFirst * minFirstPosition - uSecond * minSecondPosition;
         double vFirst = (v[c10] - v[c00]) / firstLength;
         double vSecond = (v[c01] - v[c00]) / secondLength;
         double vConstant = v[c00] - vFirst * minFirstPosition - vSecond * minSecondPosition;
         if (!(Math.abs(u[c10] + u[c01] - u[c00] - u[c11]) > 2.0E-6) && !(Math.abs(v[c10] + v[c01] - v[c00] - v[c11]) > 2.0E-6)) {
            int encodedOrder = encodeOrder(cornerOrder);
            TexturedQuadMeshMerger.MergeKey key = new TexturedQuadMeshMerger.MergeKey(
               axis, plane, color, encodedOrder, uvKey(uFirst), uvKey(uSecond), uvKey(uConstant), uvKey(vFirst), uvKey(vSecond), uvKey(vConstant)
            );
            return new TexturedQuadMeshMerger.ClassifiedQuad(
               key,
               axis,
               firstAxis,
               secondAxis,
               plane,
               encodedOrder,
               color,
               minFirst,
               minSecond,
               maxFirst,
               maxSecond,
               uFirst,
               uSecond,
               uConstant,
               vFirst,
               vSecond,
               vConstant
            );
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static int constantAxis(float[] positions) {
      for (int axis = 0; axis < 3; axis++) {
         boolean constant = true;
         int value = grid(positions[axis]);

         for (int vertex = 1; vertex < 4; vertex++) {
            if (!nearGrid(positions[vertex * 3 + axis], value)) {
               constant = false;
               break;
            }
         }

         if (constant) {
            return axis;
         }
      }

      return -1;
   }

   private static List<TexturedQuadMeshMerger.Rect> mergeRectangles(List<TexturedQuadMeshMerger.SourceRect> source) {
      List<TexturedQuadMeshMerger.Rect> rectangles = new ArrayList<>(source.size());

      for (TexturedQuadMeshMerger.SourceRect rectangle : source) {
         rectangles.add(new TexturedQuadMeshMerger.Rect(rectangle.minFirst, rectangle.minSecond, rectangle.maxFirst, rectangle.maxSecond));
      }

      int priorSize;
      do {
         priorSize = rectangles.size();
         rectangles = mergeAlongFirst(rectangles);
         rectangles = mergeAlongSecond(rectangles);
      } while (rectangles.size() < priorSize);

      return rectangles;
   }

   private static List<TexturedQuadMeshMerger.Rect> mergeAlongFirst(List<TexturedQuadMeshMerger.Rect> source) {
      source.sort(
         Comparator.comparingInt(TexturedQuadMeshMerger.Rect::minSecond)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::maxSecond)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::minFirst)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::maxFirst)
      );
      List<TexturedQuadMeshMerger.Rect> output = new ArrayList<>(source.size());

      for (TexturedQuadMeshMerger.Rect rectangle : source) {
         if (!output.isEmpty()) {
            TexturedQuadMeshMerger.Rect previous = output.get(output.size() - 1);
            if (previous.minSecond == rectangle.minSecond && previous.maxSecond == rectangle.maxSecond && previous.maxFirst == rectangle.minFirst) {
               output.set(output.size() - 1, new TexturedQuadMeshMerger.Rect(previous.minFirst, previous.minSecond, rectangle.maxFirst, previous.maxSecond));
               continue;
            }
         }

         output.add(rectangle);
      }

      return output;
   }

   private static List<TexturedQuadMeshMerger.Rect> mergeAlongSecond(List<TexturedQuadMeshMerger.Rect> source) {
      source.sort(
         Comparator.comparingInt(TexturedQuadMeshMerger.Rect::minFirst)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::maxFirst)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::minSecond)
            .thenComparingInt(TexturedQuadMeshMerger.Rect::maxSecond)
      );
      List<TexturedQuadMeshMerger.Rect> output = new ArrayList<>(source.size());

      for (TexturedQuadMeshMerger.Rect rectangle : source) {
         if (!output.isEmpty()) {
            TexturedQuadMeshMerger.Rect previous = output.get(output.size() - 1);
            if (previous.minFirst == rectangle.minFirst && previous.maxFirst == rectangle.maxFirst && previous.maxSecond == rectangle.minSecond) {
               output.set(output.size() - 1, new TexturedQuadMeshMerger.Rect(previous.minFirst, previous.minSecond, previous.maxFirst, rectangle.maxSecond));
               continue;
            }
         }

         output.add(rectangle);
      }

      return output;
   }

   private static void appendMergedQuad(BufferBuilder output, TexturedQuadMeshMerger.MergeGroup group, TexturedQuadMeshMerger.Rect rectangle) {
      for (int vertex = 0; vertex < 4; vertex++) {
         int corner = group.cornerOrder[vertex];
         double first = ((corner & 1) != 0 ? rectangle.maxFirst : rectangle.minFirst) / 4096.0;
         double second = ((corner & 2) != 0 ? rectangle.maxSecond : rectangle.minSecond) / 4096.0;
         float firstPosition = (float)first;
         float secondPosition = (float)second;
         float planePosition = group.plane / 4096.0F;
         float x = group.axis == 0 ? planePosition : (group.firstAxis == 0 ? firstPosition : secondPosition);
         float y = group.axis == 1 ? planePosition : (group.firstAxis == 1 ? firstPosition : secondPosition);
         float z = group.axis == 2 ? planePosition : (group.firstAxis == 2 ? firstPosition : secondPosition);
         float u = (float)(group.uFirst * first + group.uSecond * second + group.uConstant);
         float v = (float)(group.vFirst * first + group.vSecond * second + group.vConstant);
         output.addVertex(x, y, z).setUv(u, v).setColor(group.color & 0xFF, group.color >>> 8 & 0xFF, group.color >>> 16 & 0xFF, group.color >>> 24 & 0xFF);
      }
   }

   private static void appendRawQuad(BufferBuilder output, ByteBuffer source, int sourceOffset, int stride) {
      for (int vertex = 0; vertex < 4; vertex++) {
         int offset = sourceOffset + vertex * stride;
         output.addVertex(source.getFloat(offset), source.getFloat(offset + 4), source.getFloat(offset + 8))
            .setUv(source.getFloat(offset + 12), source.getFloat(offset + 16))
            .setColor(
               Byte.toUnsignedInt(source.get(offset + 20)),
               Byte.toUnsignedInt(source.get(offset + 21)),
               Byte.toUnsignedInt(source.get(offset + 22)),
               Byte.toUnsignedInt(source.get(offset + 23))
            );
      }
   }

   private static void updateBounds(TexturedQuadMeshMerger.Batch batch, ByteBuffer source, int sourceOffset, int stride) {
      for (int vertex = 0; vertex < 4; vertex++) {
         int offset = sourceOffset + vertex * stride;
         float x = source.getFloat(offset);
         float y = source.getFloat(offset + 4);
         float z = source.getFloat(offset + 8);
         batch.minX = Math.min(batch.minX, x);
         batch.minY = Math.min(batch.minY, y);
         batch.minZ = Math.min(batch.minZ, z);
         batch.maxX = Math.max(batch.maxX, x);
         batch.maxY = Math.max(batch.maxY, y);
         batch.maxZ = Math.max(batch.maxZ, z);
      }
   }

   private static int grid(float value) {
      return Math.round(value * 4096.0F);
   }

   private static boolean nearGrid(float value, int gridValue) {
      return Math.abs(value - gridValue / 4096.0) <= 3.0517578E-5F;
   }

   private static long uvKey(double value) {
      return Math.round(value * 1000000.0);
   }

   private static int encodeOrder(int[] cornerOrder) {
      return cornerOrder[0] | cornerOrder[1] << 2 | cornerOrder[2] << 4 | cornerOrder[3] << 6;
   }

   private static int[] decodeOrder(int encoded) {
      return new int[]{encoded & 3, encoded >>> 2 & 3, encoded >>> 4 & 3, encoded >>> 6 & 3};
   }

   private static final class Batch {
      private final List<Integer> rawQuadOffsets = new ArrayList<>();
      private final Map<TexturedQuadMeshMerger.MergeKey, TexturedQuadMeshMerger.MergeGroup> groups = new LinkedHashMap<>();
      private int quadCount;
      private float minX = Float.POSITIVE_INFINITY;
      private float minY = Float.POSITIVE_INFINITY;
      private float minZ = Float.POSITIVE_INFINITY;
      private float maxX = Float.NEGATIVE_INFINITY;
      private float maxY = Float.NEGATIVE_INFINITY;
      private float maxZ = Float.NEGATIVE_INFINITY;
   }

   private record ClassifiedQuad(
      TexturedQuadMeshMerger.MergeKey key,
      int axis,
      int firstAxis,
      int secondAxis,
      int plane,
      int cornerOrder,
      int color,
      int minFirst,
      int minSecond,
      int maxFirst,
      int maxSecond,
      double uFirst,
      double uSecond,
      double uConstant,
      double vFirst,
      double vSecond,
      double vConstant
   ) {
   }

   private static final class MergeGroup {
      private final int axis;
      private final int firstAxis;
      private final int secondAxis;
      private final int plane;
      private final int[] cornerOrder;
      private final int color;
      private final double uFirst;
      private final double uSecond;
      private final double uConstant;
      private final double vFirst;
      private final double vSecond;
      private final double vConstant;
      private final List<TexturedQuadMeshMerger.SourceRect> rectangles = new ArrayList<>();

      private MergeGroup(
         int axis,
         int firstAxis,
         int secondAxis,
         int plane,
         int cornerOrder,
         int color,
         double uFirst,
         double uSecond,
         double uConstant,
         double vFirst,
         double vSecond,
         double vConstant
      ) {
         this.axis = axis;
         this.firstAxis = firstAxis;
         this.secondAxis = secondAxis;
         this.plane = plane;
         this.cornerOrder = TexturedQuadMeshMerger.decodeOrder(cornerOrder);
         this.color = color;
         this.uFirst = uFirst;
         this.uSecond = uSecond;
         this.uConstant = uConstant;
         this.vFirst = vFirst;
         this.vSecond = vSecond;
         this.vConstant = vConstant;
      }
   }

   private record MergeKey(
      int axis, int plane, int color, int cornerOrder, long uFirst, long uSecond, long uConstant, long vFirst, long vSecond, long vConstant
   ) {
   }

   public record MergedMesh(ByteBufferBuilder backing, MeshData mesh, List<TextureDrawLayout.SourceSpan> spans) implements AutoCloseable {
      @Override
      public void close() {
         try {
            this.mesh.close();
         } finally {
            this.backing.close();
         }
      }
   }

   private static final class QuadScratch {
      private final float[] position = new float[12];
      private final float[] u = new float[4];
      private final float[] v = new float[4];
      private final int[] first = new int[4];
      private final int[] second = new int[4];
      private final int[] cornerOrder = new int[4];
      private final int[] cornerVertex = new int[4];
   }

   private record Rect(int minFirst, int minSecond, int maxFirst, int maxSecond) {
   }

   private record SourceRect(int minFirst, int minSecond, int maxFirst, int maxSecond, int sourceOffset) {
   }

   private record SpatialCell(int x, int y, int z) {
   }
}
