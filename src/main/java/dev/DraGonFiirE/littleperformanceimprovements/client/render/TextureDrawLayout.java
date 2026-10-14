package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

public final class TextureDrawLayout implements AutoCloseable {
   private static final int FALLBACK_QUADS_PER_RANGE = 8192;
   private static final float CELL_SIZE = 4.0F;
   private static final float BOUNDS_MARGIN = 0.001953125F;
   private static final int OCCLUSION_QUERIES_PER_FRAME = 4;
   private static final long OCCLUSION_RECHECK_NANOS = 2000000000L;
   private static final float CLIP_MATRIX_EPSILON = 1.0E-5F;
   private static final int GL_ANY_SAMPLES_PASSED = 35887;
   private final Mode mode;
   private final int totalQuads;
   private final TextureDrawLayout.Range[] ranges;
   private final TextureDrawLayout.Cell[] cells;
   private final boolean[] visibleRanges;
   private final boolean[] frustumCells;
   private final int[] occlusionQueries;
   private final boolean[] occlusionPending;
   private final boolean[] occlusionResultValid;
   private final boolean[] occludedCells;
   private final long[] occlusionResultNanos;
   private final float[][] pendingClipMatrices;
   private final float[][] resultClipMatrices;
   private final IntBuffer drawCounts;
   private final PointerBuffer drawOffsets;
   private int occlusionCursor;
   private float[] previousClipMatrix;
   private boolean closed;

   private TextureDrawLayout(Mode mode, int totalQuads, TextureDrawLayout.Range[] ranges, TextureDrawLayout.Cell[] cells) {
      this.mode = mode;
      this.totalQuads = totalQuads;
      this.ranges = ranges;
      this.cells = cells;
      this.visibleRanges = new boolean[ranges.length];
      this.frustumCells = new boolean[cells.length];
      this.occlusionQueries = new int[cells.length];
      this.occlusionPending = new boolean[cells.length];
      this.occlusionResultValid = new boolean[cells.length];
      this.occludedCells = new boolean[cells.length];
      this.occlusionResultNanos = new long[cells.length];
      this.pendingClipMatrices = new float[cells.length][];
      this.resultClipMatrices = new float[cells.length][];
      int capacity = Math.max(1, ranges.length);
      this.drawCounts = ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
      this.drawOffsets = PointerBuffer.create(ByteBuffer.allocateDirect(capacity * PointerBuffer.POINTER_SIZE).order(ByteOrder.nativeOrder()));
   }

   public static TextureDrawLayout build(MeshData mesh, List<TextureDrawLayout.SourceSpan> sourceSpans) {
      DrawState state = mesh.drawState();
      if (state.mode() != Mode.QUADS || state.vertexCount() <= 0 || (state.vertexCount() & 3) != 0) {
         return TexturePassSelection.register(null, mesh);
      }

      if (sourceSpans != null && !sourceSpans.isEmpty()) {
         List<TextureDrawLayout.Range> ranges = new ArrayList<>(sourceSpans.size());
         int expectedVertex = 0;

         for (TextureDrawLayout.SourceSpan span : sourceSpans) {
            if (span == null
               || span.firstVertex != expectedVertex
               || span.endVertex <= span.firstVertex
               || (span.firstVertex & 3) != 0
               || (span.endVertex & 3) != 0
               || span.endVertex > state.vertexCount()
               || !finiteBounds(span)) {
               return TexturePassSelection.register(buildFallback(mesh), mesh);
            }

            ranges.add(
               new TextureDrawLayout.Range(
                  span.firstVertex / 4,
                  (span.endVertex - span.firstVertex) / 4,
                  span.minX - 0.001953125F,
                  span.minY - 0.001953125F,
                  span.minZ - 0.001953125F,
                  span.maxX + 0.001953125F,
                  span.maxY + 0.001953125F,
                  span.maxZ + 0.001953125F,
                  true
               )
            );
            expectedVertex = span.endVertex;
         }

         if (expectedVertex != state.vertexCount()) {
            return TexturePassSelection.register(buildFallback(mesh), mesh);
         }

         TextureDrawLayout.Range[] rangeArray = ranges.toArray(TextureDrawLayout.Range[]::new);
         return TexturePassSelection.register(new TextureDrawLayout(state.mode(), state.vertexCount() / 4, rangeArray, buildCells(rangeArray)), mesh);
      } else {
         return TexturePassSelection.register(buildFallback(mesh), mesh);
      }
   }

   public static TextureDrawLayout build(MeshData mesh) {
      return TexturePassSelection.register(buildFallback(mesh), mesh);
   }

   private static TextureDrawLayout buildFallback(MeshData mesh) {
      DrawState state = mesh.drawState();
      if (state.mode() == Mode.QUADS && state.vertexCount() > 0 && (state.vertexCount() & 3) == 0) {
         int stride = state.format().getVertexSize();
         ByteBuffer vertices = mesh.vertexBuffer();
         if (stride >= 12 && (long)state.vertexCount() * stride <= vertices.limit()) {
            int totalQuads = state.vertexCount() / 4;
            List<TextureDrawLayout.Range> ranges = new ArrayList<>((totalQuads + 8192 - 1) / 8192);

            for (int firstQuad = 0; firstQuad < totalQuads; firstQuad += 8192) {
               int quadCount = Math.min(8192, totalQuads - firstQuad);
               int firstVertex = firstQuad * 4;
               int endVertex = (firstQuad + quadCount) * 4;
               float minX = Float.POSITIVE_INFINITY;
               float minY = Float.POSITIVE_INFINITY;
               float minZ = Float.POSITIVE_INFINITY;
               float maxX = Float.NEGATIVE_INFINITY;
               float maxY = Float.NEGATIVE_INFINITY;
               float maxZ = Float.NEGATIVE_INFINITY;
               boolean valid = true;

               for (int vertex = firstVertex; vertex < endVertex; vertex++) {
                  int offset = vertex * stride;
                  float x = vertices.getFloat(offset);
                  float y = vertices.getFloat(offset + 4);
                  float z = vertices.getFloat(offset + 8);
                  if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                     valid = false;
                     break;
                  }

                  minX = Math.min(minX, x);
                  minY = Math.min(minY, y);
                  minZ = Math.min(minZ, z);
                  maxX = Math.max(maxX, x);
                  maxY = Math.max(maxY, y);
                  maxZ = Math.max(maxZ, z);
               }

               ranges.add(
                  valid
                     ? new TextureDrawLayout.Range(
                        firstQuad,
                        quadCount,
                        minX - 0.001953125F,
                        minY - 0.001953125F,
                        minZ - 0.001953125F,
                        maxX + 0.001953125F,
                        maxY + 0.001953125F,
                        maxZ + 0.001953125F,
                        true
                     )
                     : new TextureDrawLayout.Range(firstQuad, quadCount, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false)
               );
            }

            TextureDrawLayout.Range[] rangeArray = ranges.toArray(TextureDrawLayout.Range[]::new);
            return new TextureDrawLayout(state.mode(), totalQuads, rangeArray, buildCells(rangeArray));
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean drawVisible(VertexBuffer vertexBuffer) {
      return this.drawVisible(vertexBuffer, true);
   }

   public boolean drawVisible(VertexBuffer vertexBuffer, boolean useOcclusionCulling) {

      if (vertexBuffer != null && this.ranges.length != 0 && this.mode == Mode.QUADS && this.totalQuads > 0) {
         Matrix4f clip = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
         float[] clipElements = new float[16];
         clip.get(clipElements);
         boolean cameraMatrixStable = this.previousClipMatrix != null && matricesMatch(this.previousClipMatrix, clipElements);
         this.previousClipMatrix = (float[])clipElements.clone();
         long occlusionNow = System.nanoTime();
         if (useOcclusionCulling) {
            this.pollOcclusionQueries(occlusionNow);
         }

         IndexType indexType = RenderSystem.getSequentialBuffer(this.mode).type();
         if (indexType == null) {
            return false;
         }

         Arrays.fill(this.visibleRanges, false);
         Arrays.fill(this.frustumCells, false);
         Vector4f point = new Vector4f();

         for (int cellIndex = 0; cellIndex < this.cells.length; cellIndex++) {
            TextureDrawLayout.Cell cell = this.cells[cellIndex];
            if (!cell.hasBounds || !outsideClipVolume(cell, clip, point)) {
               this.frustumCells[cellIndex] = true;

               if (!useOcclusionCulling
                  || !this.occludedCells[cellIndex]
                  || !this.occlusionResultValid[cellIndex]
                  || !matricesMatch(this.resultClipMatrices[cellIndex], clipElements)) {
                  for (int rangeIndex : cell.rangeIndices) {
                     TextureDrawLayout.Range range = this.ranges[rangeIndex];
                     if (!range.hasBounds || !outsideClipVolume(range, clip, point)) {
                        this.visibleRanges[rangeIndex] = TexturePassSelection.accept(this, rangeIndex);
                     }
                  }
               }
            }
         }

         int visibleQuads = 0;

         for (int rangeIndex = 0; rangeIndex < this.ranges.length; rangeIndex++) {
            if (this.visibleRanges[rangeIndex]) {
               visibleQuads += this.ranges[rangeIndex].quadCount;

            }
         }

         if (visibleQuads <= 0) {
            if (useOcclusionCulling) {
               this.issueOcclusionQueries(indexType, clipElements, cameraMatrixStable, occlusionNow);
            }
            return true;
         } else if (visibleQuads * 100L >= this.totalQuads * TexturePassSelection.wholeThreshold()) {
            vertexBuffer.draw();
            if (useOcclusionCulling) {
               this.issueOcclusionQueries(indexType, clipElements, cameraMatrixStable, occlusionNow);
            }
            return true;
         } else {
            this.drawCounts.clear();
            this.drawOffsets.clear();
            int firstQuad = -1;
            int endQuad = -1;

            for (int rangeIndex = 0; rangeIndex < this.ranges.length; rangeIndex++) {
               TextureDrawLayout.Range range = this.ranges[rangeIndex];
               if (!this.visibleRanges[rangeIndex]) {
                  if (firstQuad >= 0) {
                     this.appendDraw(firstQuad, endQuad, indexType);
                     firstQuad = -1;
                  }
               } else {
                  if (firstQuad < 0) {
                     firstQuad = range.firstQuad;
                  }

                  endQuad = range.firstQuad + range.quadCount;
               }
            }

            if (firstQuad >= 0) {
               this.appendDraw(firstQuad, endQuad, indexType);
            }

            this.drawCounts.flip();
            this.drawOffsets.flip();

            if (this.drawCounts.remaining() == 1) {
               GL11.glDrawElements(this.mode.asGLMode, this.drawCounts.get(0), indexType.asGLType, this.drawOffsets.get(0));
            } else {
               GL14.glMultiDrawElements(this.mode.asGLMode, this.drawCounts, indexType.asGLType, this.drawOffsets);
            }

            if (useOcclusionCulling) {
               this.issueOcclusionQueries(indexType, clipElements, cameraMatrixStable, occlusionNow);
            }
            return true;
         }
      } else {
         return false;
      }
   }

   private void pollOcclusionQueries(long now) {
      for (int cellIndex = 0; cellIndex < this.cells.length; cellIndex++) {
         int query = this.occlusionQueries[cellIndex];
         if (this.occlusionPending[cellIndex] && query != 0 && GL15.glGetQueryObjecti(query, 34919) != 0) {
            this.occludedCells[cellIndex] = GL15.glGetQueryObjecti(query, 34918) == 0;
            this.occlusionResultValid[cellIndex] = true;
            this.occlusionResultNanos[cellIndex] = now;
            this.resultClipMatrices[cellIndex] = this.pendingClipMatrices[cellIndex];
            this.pendingClipMatrices[cellIndex] = null;
            this.occlusionPending[cellIndex] = false;
         }
      }
   }

   private void issueOcclusionQueries(IndexType indexType, float[] clipElements, boolean cameraMatrixStable, long now) {
      if (!this.closed && this.cells.length != 0 && cameraMatrixStable) {
         int previousDepthFunc = GL11.glGetInteger(2932);
         GL11.glColorMask(false, false, false, false);
         GL11.glDepthMask(false);
         GL11.glDepthFunc(515);

         try {
            int issued = 0;
            int inspected = 0;

            while (issued < 4 && inspected < this.cells.length) {
               int cellIndex = this.occlusionCursor;
               this.occlusionCursor = (this.occlusionCursor + 1) % this.cells.length;
               inspected++;
               TextureDrawLayout.Cell cell = this.cells[cellIndex];
               if (this.frustumCells[cellIndex]
                  && cell.hasBounds
                  && !this.occlusionPending[cellIndex]
                  && cell.rangeIndices.length != 0
                  && (
                     !this.occlusionResultValid[cellIndex]
                        || !matricesMatch(this.resultClipMatrices[cellIndex], clipElements)
                        || now - this.occlusionResultNanos[cellIndex] >= 2000000000L
                  )) {
                  int query = this.occlusionQueries[cellIndex];
                  if (query == 0) {
                     query = GL15.glGenQueries();
                     this.occlusionQueries[cellIndex] = query;
                  }

                  this.prepareCellDraws(cell, indexType);
                  if (this.drawCounts.hasRemaining()) {
                     this.pendingClipMatrices[cellIndex] = (float[])clipElements.clone();
                     this.occlusionPending[cellIndex] = true;
                     GL15.glBeginQuery(35887, query);
                     if (this.drawCounts.remaining() == 1) {
                        GL11.glDrawElements(this.mode.asGLMode, this.drawCounts.get(0), indexType.asGLType, this.drawOffsets.get(0));
                     } else {
                        GL14.glMultiDrawElements(this.mode.asGLMode, this.drawCounts, indexType.asGLType, this.drawOffsets);
                     }

                     GL15.glEndQuery(35887);
                     issued++;
                  }
               }
            }
         } finally {
            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
         }
      }
   }

   private void prepareCellDraws(TextureDrawLayout.Cell cell, IndexType indexType) {
      this.drawCounts.clear();
      this.drawOffsets.clear();
      int firstQuad = -1;
      int endQuad = -1;

      for (int rangeIndex : cell.rangeIndices) {
         TextureDrawLayout.Range range = this.ranges[rangeIndex];
         if (firstQuad < 0) {
            firstQuad = range.firstQuad;
            endQuad = range.firstQuad + range.quadCount;
         } else if (range.firstQuad == endQuad) {
            endQuad += range.quadCount;
         } else {
            this.appendDraw(firstQuad, endQuad, indexType);
            firstQuad = range.firstQuad;
            endQuad = range.firstQuad + range.quadCount;
         }
      }

      if (firstQuad >= 0) {
         this.appendDraw(firstQuad, endQuad, indexType);
      }

      this.drawCounts.flip();
      this.drawOffsets.flip();
   }

   private static boolean matricesMatch(float[] previous, float[] current) {
      if (previous != null && current != null && previous.length == 16 && current.length == 16) {
         for (int index = 0; index < 16; index++) {
            if (Math.abs(previous[index] - current[index]) > 1.0E-5F) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         int[] queries = Arrays.stream(this.occlusionQueries).filter(query -> query != 0).toArray();
         Arrays.fill(this.occlusionQueries, 0);
         if (queries.length != 0) {
            Runnable delete = () -> {
               for (int query : queries) {
                  GL15.glDeleteQueries(query);
               }
            };
            if (RenderSystem.isOnRenderThreadOrInit()) {
               delete.run();
            } else {
               RenderSystem.recordRenderCall(delete::run);
            }
         }
      }
   }

   private void appendDraw(int firstQuad, int endQuad, IndexType indexType) {
      this.drawCounts.put((endQuad - firstQuad) * 6);
      this.drawOffsets.put(firstQuad * 6L * indexType.bytes);
   }

   private static TextureDrawLayout.Cell[] buildCells(TextureDrawLayout.Range[] ranges) {
      Map<TextureDrawLayout.CellKey, TextureDrawLayout.MutableCell> cells = new HashMap<>();
      TextureDrawLayout.MutableCell unbounded = null;

      for (int rangeIndex = 0; rangeIndex < ranges.length; rangeIndex++) {
         TextureDrawLayout.Range range = ranges[rangeIndex];
         if (!range.hasBounds) {
            if (unbounded == null) {
               unbounded = new TextureDrawLayout.MutableCell(false);
            }

            unbounded.add(rangeIndex, range);
         } else {
            int cellX = fastFloor((range.minX + range.maxX) * 0.5F / 4.0F);
            int cellY = fastFloor((range.minY + range.maxY) * 0.5F / 4.0F);
            int cellZ = fastFloor((range.minZ + range.maxZ) * 0.5F / 4.0F);
            cells.computeIfAbsent(new TextureDrawLayout.CellKey(cellX, cellY, cellZ), ignored -> new TextureDrawLayout.MutableCell(true))
               .add(rangeIndex, range);
         }
      }

      List<TextureDrawLayout.Cell> result = new ArrayList<>(cells.size() + (unbounded == null ? 0 : 1));

      for (TextureDrawLayout.MutableCell cell : cells.values()) {
         result.add(cell.freeze());
      }

      if (unbounded != null) {
         result.add(unbounded.freeze());
      }

      return result.toArray(TextureDrawLayout.Cell[]::new);
   }

   private static int fastFloor(float value) {
      int integer = (int)value;
      return value < integer ? integer - 1 : integer;
   }

   private static boolean finiteBounds(TextureDrawLayout.SourceSpan span) {
      return Float.isFinite(span.minX)
         && Float.isFinite(span.minY)
         && Float.isFinite(span.minZ)
         && Float.isFinite(span.maxX)
         && Float.isFinite(span.maxY)
         && Float.isFinite(span.maxZ)
         && span.minX <= span.maxX
         && span.minY <= span.maxY
         && span.minZ <= span.maxZ;
   }

   private static boolean outsideClipVolume(TextureDrawLayout.Bounds bounds, Matrix4f clip, Vector4f point) {
      boolean left = true;
      boolean right = true;
      boolean bottom = true;
      boolean top = true;
      boolean near = true;
      boolean far = true;

      for (int corner = 0; corner < 8; corner++) {
         point.set(
            (corner & 1) == 0 ? bounds.minX() : bounds.maxX(),
            (corner & 2) == 0 ? bounds.minY() : bounds.maxY(),
            (corner & 4) == 0 ? bounds.minZ() : bounds.maxZ(),
            1.0F
         );
         clip.transform(point);
         float x = point.x;
         float y = point.y;
         float z = point.z;
         float w = point.w;
         if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
            return false;
         }

         left &= x < -w;
         right &= x > w;
         bottom &= y < -w;
         top &= y > w;
         near &= z < -w;
         far &= z > w;
      }

      return left || right || bottom || top || near || far;
   }

   private interface Bounds {
      float minX();

      float minY();

      float minZ();

      float maxX();

      float maxY();

      float maxZ();
   }

   private record Cell(int[] rangeIndices, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, boolean hasBounds)
      implements TextureDrawLayout.Bounds {
   }

   private record CellKey(int x, int y, int z) {
   }

   private static final class MutableCell {
      private int[] rangeIndices = new int[8];
      private int size;
      private float minX = Float.POSITIVE_INFINITY;
      private float minY = Float.POSITIVE_INFINITY;
      private float minZ = Float.POSITIVE_INFINITY;
      private float maxX = Float.NEGATIVE_INFINITY;
      private float maxY = Float.NEGATIVE_INFINITY;
      private float maxZ = Float.NEGATIVE_INFINITY;
      private final boolean hasBounds;

      private MutableCell(boolean hasBounds) {
         this.hasBounds = hasBounds;
      }

      private void add(int rangeIndex, TextureDrawLayout.Range range) {
         if (this.size == this.rangeIndices.length) {
            this.rangeIndices = Arrays.copyOf(this.rangeIndices, this.size * 2);
         }

         this.rangeIndices[this.size++] = rangeIndex;
         if (this.hasBounds) {
            this.minX = Math.min(this.minX, range.minX);
            this.minY = Math.min(this.minY, range.minY);
            this.minZ = Math.min(this.minZ, range.minZ);
            this.maxX = Math.max(this.maxX, range.maxX);
            this.maxY = Math.max(this.maxY, range.maxY);
            this.maxZ = Math.max(this.maxZ, range.maxZ);
         }
      }

      private TextureDrawLayout.Cell freeze() {
         return new TextureDrawLayout.Cell(
            Arrays.copyOf(this.rangeIndices, this.size),
            this.hasBounds ? this.minX : 0.0F,
            this.hasBounds ? this.minY : 0.0F,
            this.hasBounds ? this.minZ : 0.0F,
            this.hasBounds ? this.maxX : 0.0F,
            this.hasBounds ? this.maxY : 0.0F,
            this.hasBounds ? this.maxZ : 0.0F,
            this.hasBounds
         );
      }
   }

   private record Range(int firstQuad, int quadCount, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, boolean hasBounds)
      implements TextureDrawLayout.Bounds {
   }

   public record SourceSpan(int firstVertex, int endVertex, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
   }
}
