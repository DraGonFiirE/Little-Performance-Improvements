package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.List;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;

public final class DragExteriorMesh {
   public static List<int[]> faces(LittleBoxes var0) {
      List var10000 = PolygonDragFaces.faces(var0);
      if (var10000 != null) {
         return var10000;
      }

      List var1 = ExtrudedPreviewFaces.faces(var0);
      if (var1 != null) {
         return var1;
      }

      List var2 = MonotoneBoxExterior.faces(var0);
      if (var2 != null) {
         return var2;
      }

      List var3 = PrimitiveBendingExterior.faces(var0);
      if (var3 != null) {
         return var3;
      }

      long[] var4 = new long[3];
      LittleBox var5 = null;

      for (LittleBox var7 : var0.all()) {
         if (var7.getClass() != LittleBox.class) {
            return null;
         }

         if (var5 == null || !var5.equals(var7)) {
            var5 = var7;
            long var8 = (long)var7.maxX - var7.minX;
            long var10 = (long)var7.maxY - var7.minY;
            long var12 = (long)var7.maxZ - var7.minZ;
            var4[0] += var10 * var12;
            var4[1] += var8 * var12;
            var4[2] += var8 * var10;
         }
      }

      int var15 = var4[0] <= var4[1] && var4[0] <= var4[2] ? 0 : (var4[1] <= var4[2] ? 1 : 2);
      if (var4[var15] > 8000000L) {
         return null;
      }

      DragSurfaceRows var16 = new DragSurfaceRows(var15);
      var5 = null;

      for (LittleBox var9 : var0.all()) {
         if (var5 == null || !var5.equals(var9)) {
            DragSurfaceDelta.add(var16, var9, var5);
            var5 = var9;
         }
      }

      return var16.faces();
   }

   public static BoxRenderResult build(PreviewRenderer var0, LittleBoxes var1) {
      List var2 = faces(var1);
      if (var2 == null) {
         return SafeDragMesh.build(var0, var1);
      }

      ByteBufferBuilder var3 = var0.createBuffer();

      try {
         BufferBuilder var4 = var0.createBuilder(var3, false);
         float var5 = (float)var1.grid.pixelLength;

         for (int[] var7 : (Iterable<int[]>)(Iterable<?>)(var2)) {
            int var8 = var7[0];
            int var9 = var7[3];
            int var10 = var7[4];
            boolean var11 = (var9 + 1) % 3 == var10 == (var7[1] == 1);

            for (int var12 = 0; var12 < 4; var12++) {
               int var13 = var11 ? var12 : (4 - var12) % 4;
               float[] var14 = new float[3];
               var14[var8] = var7[2] * var5;
               var14[var9] = var7[var13 != 1 && var13 != 2 ? 5 : 6] * var5;
               var14[var10] = var7[var13 >= 2 ? 8 : 7] * var5;
               var4.addVertex(var14[0], var14[1], var14[2]).setColor(255, 255, 255, 255);
            }
         }

         MeshData var16 = var4.build();
         if (var16 instanceof MeshDataExtender var17) {
            var17.keepAlive(true);
         }

         return new BoxRenderResult(var1, var1.pos, var3, var16);
      } catch (Throwable var15) {
         var3.close();
         throw var15;
      }
   }
}
