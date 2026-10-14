package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.FaceInfo.VertexInfo;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox.VectorFanFaceCache;
import team.creative.littletiles.common.math.face.LittleFace;

public final class OutlineOrdinaryFaces {
   private static final int MAX_INDEXED_FACE_CELLS = 256;
   private static final RenderBoxFaceSpecial EMPTY_CACHED_FACE = new RenderBoxFaceSpecial(List.of(), 1.0F);

   private OutlineOrdinaryFaces() {
   }

   public static List<RenderBox> retainAirTouching(List<RenderBox> var0, LittleGrid var1) {
      if (var0.size() < 2) {
         return var0;
      }

      ArrayList var2 = new ArrayList(var0.size());

      for (RenderBox var4 : var0) {
         OutlineWorkChunks.check();
         if (var4 instanceof LittleRenderBoxTransformable) {
            return BlueprintSurfaceFaceFilter.retainAirTouching(var0, var1);
         }

         if (var4 instanceof LittleRenderBox var5 && var5.box != null) {
            var2.add(var5);
         }
      }

      if (var2.size() < 2) {
         return var0;
      }

      OutlineOrdinaryFaces.FaceIndex var6 = new OutlineOrdinaryFaces.FaceIndex(Math.max(1, var1.count / 8), var2.size());

      for (LittleRenderBox var9 : (Iterable<LittleRenderBox>)(Iterable<?>)(var2)) {
         OutlineWorkChunks.check();
         var6.add(var9);
      }

      var6.finishBuild();
      ArrayList<Integer> var8 = new ArrayList<>();

      for (int var10 = 0; var10 < var2.size(); var10 += 4096) {
         var8.add(Integer.valueOf(var10));
      }

      OutlineFaceTasks.process(
         var8,
         (var3, var4x) -> {
            try {
               int var5x = var3;

               for (int var6x = Math.min(var3 + 4096, var2.size()); var5x < var6x; var5x++) {
                  OutlineFaceTasks.check();
                  LittleRenderBox var7 = (LittleRenderBox)var2.get(var5x);
                  LittleBox var8x = var7.box;

                  for (Facing var12 : Facing.VALUES) {
                     if (var6.isExactOrdinaryCovered(var5x, var12)) {
                        var7.setFace(var12, EMPTY_CACHED_FACE);
                     } else {
                        List var13 = var6.findOpposite(var8x, var12);
                        if (!var13.isEmpty()) {
                           boolean var14 = false;
                           Axis var15 = var12.one();
                           Axis var16 = var12.two();

                           for (LittleRenderBox var18 : (Iterable<LittleRenderBox>)(Iterable<?>)(var13)) {
                              if (var18 != var7) {
                                 LittleBox var19 = var18.box;
                                 if (var19.getMin(var15) == var8x.getMin(var15)
                                    && var19.getMax(var15) == var8x.getMax(var15)
                                    && var19.getMin(var16) == var8x.getMin(var16)
                                    && var19.getMax(var16) == var8x.getMax(var16)) {
                                    var14 = true;
                                    break;
                                 }
                              }
                           }

                           if (var14) {
                              var7.setFace(var12, EMPTY_CACHED_FACE);
                           } else {
                              OutlineOrdinaryFaces.OrdinaryClip var25 = clipOrdinaryFace(var8x, var12, var13, var7);
                              if (var25.touched()) {
                                 var7.setFace(
                                    var12,
                                    var25.exposed().isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(normalizeForPreview(var25.exposed(), var8x), 1.0F)
                                 );
                              } else {
                                 LittleFace var26 = var8x.generateFace(var1, var12);
                                 if (var26 != null) {
                                    boolean var27 = false;

                                    for (LittleRenderBox var21 : (Iterable<LittleRenderBox>)(Iterable<?>)(var13)) {
                                       if (var21 != var7) {
                                          var21.box.fill(var26);
                                          var27 = true;
                                       }
                                    }

                                    if (var27 && var26.isPartiallyFilled()) {
                                       List var28 = var26.generateFans();
                                       var7.setFace(
                                          var12, var28.isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(normalizeForPreview(var28, var8x), 1.0F)
                                       );
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            } finally {
               var6.queries.remove();
            }
         },
         var0x -> {}
      );
      return var0;
   }

   private static OutlineOrdinaryFaces.OrdinaryClip clipOrdinaryFace(LittleBox var0, Facing var1, List<LittleRenderBox> var2, LittleRenderBox var3) {
      Axis var4 = var1.one();
      Axis var5 = var1.two();
      ArrayList var6 = new ArrayList();
      var6.add(new OutlineOrdinaryFaces.FaceRect(var0.getMin(var4), var0.getMin(var5), var0.getMax(var4), var0.getMax(var5)));
      boolean var7 = false;
      int var8 = 0;

      for (LittleRenderBox var10 : var2) {
         if (var10 != var3 && !(var10 instanceof LittleRenderBoxTransformable)) {
            LittleBox var11 = var10.box;
            int var12 = Math.max(var0.getMin(var4), var11.getMin(var4));
            int var13 = Math.max(var0.getMin(var5), var11.getMin(var5));
            int var14 = Math.min(var0.getMax(var4), var11.getMax(var4));
            int var15 = Math.min(var0.getMax(var5), var11.getMax(var5));
            if (var12 < var14 && var13 < var15) {
               var7 = true;
               var8++;
               ArrayList var16 = new ArrayList(var6.size() + 3);

               for (OutlineOrdinaryFaces.FaceRect var18 : (Iterable<OutlineOrdinaryFaces.FaceRect>)(Iterable<?>)(var6)) {
                  subtract(var18, var12, var13, var14, var15, var16);
               }

               var6 = var16;
               if (var6.isEmpty()) {
                  break;
               }
            }
         }
      }

      if (!var7) {
         return OutlineOrdinaryFaces.OrdinaryClip.UNTOUCHED;
      }

      mergeAdjacentRectangles(var6);
      ArrayList var19 = new ArrayList(var6.size());

      for (OutlineOrdinaryFaces.FaceRect var21 : (Iterable<OutlineOrdinaryFaces.FaceRect>)(Iterable<?>)(var6)) {
         var19.add(rectangleFan(var0.get(var1), var1, var21));
      }

      return new OutlineOrdinaryFaces.OrdinaryClip(true, var8, var19);
   }

   private static void subtract(OutlineOrdinaryFaces.FaceRect var0, int var1, int var2, int var3, int var4, List<OutlineOrdinaryFaces.FaceRect> var5) {
      int var6 = Math.max(var0.minOne(), var1);
      int var7 = Math.max(var0.minTwo(), var2);
      int var8 = Math.min(var0.maxOne(), var3);
      int var9 = Math.min(var0.maxTwo(), var4);
      if (var6 < var8 && var7 < var9) {
         if (var0.minOne() < var6) {
            var5.add(new OutlineOrdinaryFaces.FaceRect(var0.minOne(), var0.minTwo(), var6, var0.maxTwo()));
         }

         if (var8 < var0.maxOne()) {
            var5.add(new OutlineOrdinaryFaces.FaceRect(var8, var0.minTwo(), var0.maxOne(), var0.maxTwo()));
         }

         if (var0.minTwo() < var7) {
            var5.add(new OutlineOrdinaryFaces.FaceRect(var6, var0.minTwo(), var8, var7));
         }

         if (var9 < var0.maxTwo()) {
            var5.add(new OutlineOrdinaryFaces.FaceRect(var6, var9, var8, var0.maxTwo()));
         }
      } else {
         var5.add(var0);
      }
   }

   private static void mergeAdjacentRectangles(List<OutlineOrdinaryFaces.FaceRect> var0) {
      boolean var1;
      label55:
      do {
         var1 = false;

         for (int var2 = 0; var2 < var0.size(); var2++) {
            OutlineOrdinaryFaces.FaceRect var3 = (OutlineOrdinaryFaces.FaceRect)var0.get(var2);

            for (int var4 = var2 + 1; var4 < var0.size(); var4++) {
               OutlineOrdinaryFaces.FaceRect var5 = (OutlineOrdinaryFaces.FaceRect)var0.get(var4);
               OutlineOrdinaryFaces.FaceRect var6 = null;
               if (var3.minTwo() != var5.minTwo() || var3.maxTwo() != var5.maxTwo() || var3.maxOne() != var5.minOne() && var5.maxOne() != var3.minOne()) {
                  if (var3.minOne() == var5.minOne() && var3.maxOne() == var5.maxOne() && (var3.maxTwo() == var5.minTwo() || var5.maxTwo() == var3.minTwo())) {
                     var6 = new OutlineOrdinaryFaces.FaceRect(
                        var3.minOne(), Math.min(var3.minTwo(), var5.minTwo()), var3.maxOne(), Math.max(var3.maxTwo(), var5.maxTwo())
                     );
                  }
               } else {
                  var6 = new OutlineOrdinaryFaces.FaceRect(
                     Math.min(var3.minOne(), var5.minOne()), var3.minTwo(), Math.max(var3.maxOne(), var5.maxOne()), var3.maxTwo()
                  );
               }

               if (var6 != null) {
                  var0.set(var2, var6);
                  var0.remove(var4);
                  var1 = true;
                  continue label55;
               }
            }
         }
      } while (var1);
   }

   private static VectorFan rectangleFan(int var0, Facing var1, OutlineOrdinaryFaces.FaceRect var2) {
      FaceInfo var3 = FaceInfo.fromFacing(var1.toVanilla());
      Vec3f[] var4 = new Vec3f[4];

      for (int var5 = 0; var5 < 4; var5++) {
         VertexInfo var6 = var3.getVertexInfo(var5);
         var4[var5] = new Vec3f(
            rectangleCoordinate(var6.xFace, var0, var1, var2),
            rectangleCoordinate(var6.yFace, var0, var1, var2),
            rectangleCoordinate(var6.zFace, var0, var1, var2)
         );
      }

      return new VectorFan(var4);
   }

   private static float rectangleCoordinate(int var0, int var1, Facing var2, OutlineOrdinaryFaces.FaceRect var3) {
      Facing var4 = Facing.get(var0);
      if (var4.axis == var2.one()) {
         return var4.positive ? var3.maxOne() : var3.minOne();
      } else if (var4.axis == var2.two()) {
         return var4.positive ? var3.maxTwo() : var3.minTwo();
      } else {
         return var1;
      }
   }

   private static List<VectorFan> normalizeForPreview(List<VectorFan> var0, LittleBox var1) {
      float var2 = var1.maxX - var1.minX;
      float var3 = var1.maxY - var1.minY;
      float var4 = var1.maxZ - var1.minZ;
      ArrayList var5 = new ArrayList(var0.size());

      for (VectorFan var7 : var0) {
         VectorFan var8 = var7.copy();

         for (int var9 = 0; var9 < var8.count(); var9++) {
            var8.get(var9).x = (var8.get(var9).x - var1.minX) / var2;
            var8.get(var9).y = (var8.get(var9).y - var1.minY) / var3;
            var8.get(var9).z = (var8.get(var9).z - var1.minZ) / var4;
         }

         var5.add(var8);
      }

      return var5;
   }

   private static final class FaceIndex {
      private final int cellSize;
      private final IdentityHashMap<LittleRenderBox, Integer> boxIds;
      private final ArrayList<LittleRenderBox> boxesById;
      private final ThreadLocal<OutlineOrdinaryFaces.FaceIndex.QueryState> queries;
      private final boolean[] exactOrdinaryCovered;
      private final boolean[] originalTransformAxisVisible;
      private Long2LongOpenHashMap exactOrdinaryFaces;
      private boolean hasTransformables;
      private final Int2ObjectOpenHashMap<IntArrayList>[] planes;
      private final Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<IntArrayList>>[] cells;
      private final Int2ObjectOpenHashMap<IntArrayList>[] broad;
      private final Long2ObjectOpenHashMap<IntArrayList>[] transformProjectionCells;
      private final IntArrayList[] broadTransformProjections;
      private final Map<OutlineOrdinaryFaces.VolumeCellKey, IntArrayList> ordinaryVolumeCells = new HashMap<>();
      private final IntArrayList broadOrdinaryVolumes = new IntArrayList();
      private long candidateVisits;
      private long uniqueCandidates;
      private long queryCount;

      FaceIndex(int var1, int var2) {
         this.cellSize = var1;
         this.boxIds = new IdentityHashMap<>(var2);
         this.boxesById = new ArrayList<>(var2);
         this.queries = ThreadLocal.withInitial(() -> new OutlineOrdinaryFaces.FaceIndex.QueryState(var2));
         this.exactOrdinaryCovered = new boolean[Math.multiplyExact(var2, Facing.VALUES.length)];
         this.originalTransformAxisVisible = new boolean[Math.multiplyExact(var2, Facing.VALUES.length)];
         this.exactOrdinaryFaces = new Long2LongOpenHashMap(Math.max(16, Math.multiplyExact(var2, 3)));
         int var3 = Facing.VALUES.length;
         this.planes = new Int2ObjectOpenHashMap[var3];
         this.cells = new Int2ObjectOpenHashMap[var3];
         this.broad = new Int2ObjectOpenHashMap[var3];
         this.transformProjectionCells = new Long2ObjectOpenHashMap[var3];
         this.broadTransformProjections = new IntArrayList[var3];

         for (int var4 = 0; var4 < var3; var4++) {
            this.planes[var4] = new Int2ObjectOpenHashMap<>();
            this.cells[var4] = new Int2ObjectOpenHashMap<>();
            this.broad[var4] = new Int2ObjectOpenHashMap<>();
            this.transformProjectionCells[var4] = new Long2ObjectOpenHashMap<>();
         }
      }

      void add(LittleRenderBox var1) {
         int var2 = this.boxIds.size();
         this.boxIds.put(var1, var2);
         this.boxesById.add(var1);
         LittleBox var3 = var1.box;
         boolean var4 = !(var1 instanceof LittleRenderBoxTransformable);
         if (var4) {
            this.addOrdinaryVolume(var2, var3);
            this.addExactOrdinaryFaces(var1, var3, var2);
         } else {
            this.hasTransformables = true;
            LittleRenderBoxTransformable var5 = (LittleRenderBoxTransformable)var1;

            for (Facing var9 : Facing.VALUES) {
               VectorFanFaceCache var10 = var5.getFaceCache(var9);
               this.originalTransformAxisVisible[var2 * Facing.VALUES.length + var9.ordinal()] = var10 != null
                  && var10.hasAxisStrip()
                  && var5.getFace(var9).shouldRender();
            }
         }

         for (Facing var28 : Facing.VALUES) {
            Axis var29 = var28.one();
            Axis var30 = var28.two();
            int var11 = var3.get(var28);
            int var12 = var3.getMin(var29);
            int var13 = var3.getMax(var29);
            int var14 = var3.getMin(var30);
            int var15 = var3.getMax(var30);
            int var16 = var28.ordinal();
            this.planes[var16].computeIfAbsent(var11, var0 -> new IntArrayList()).add(var2);
            int var17 = Math.floorDiv(var12, this.cellSize);
            int var18 = Math.floorDiv(var13 - 1, this.cellSize);
            int var19 = Math.floorDiv(var14, this.cellSize);
            int var20 = Math.floorDiv(var15 - 1, this.cellSize);
            long var21 = (long)(var18 - var17 + 1) * (var20 - var19 + 1);
            if (var21 > 256L) {
               this.broad[var16].computeIfAbsent(var11, var0 -> new IntArrayList()).add(var2);
               if (var1 instanceof LittleRenderBoxTransformable) {
                  IntArrayList var32 = this.broadTransformProjections[var16];
                  if (var32 == null) {
                     this.broadTransformProjections[var16] = var32 = new IntArrayList();
                  }

                  var32.add(var2);
               }
            } else {
               for (int var23 = var17; var23 <= var18; var23++) {
                  for (int var24 = var19; var24 <= var20; var24++) {
                     this.cells[var16]
                        .computeIfAbsent(var11, var0 -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(packedCell(var23, var24), var0 -> new IntArrayList())
                        .add(var2);
                  }
               }

               if (var1 instanceof LittleRenderBoxTransformable) {
                  for (int var31 = var17; var31 <= var18; var31++) {
                     for (int var33 = var19; var33 <= var20; var33++) {
                        this.transformProjectionCells[var16].computeIfAbsent(packedCell(var31, var33), var0 -> new IntArrayList()).add(var2);
                     }
                  }
               }
            }
         }
      }

      void finishBuild() {
         this.exactOrdinaryFaces = null;
      }

      boolean isExactOrdinaryCovered(int var1, Facing var2) {
         return var1 >= 0 && this.exactOrdinaryCovered[var1 * Facing.VALUES.length + var2.ordinal()];
      }

      private void addExactOrdinaryFaces(LittleRenderBox var1, LittleBox var2, int var3) {
         for (Facing var7 : Facing.VALUES) {
            long var8 = exactFaceKey(var2, var7);
            long var10 = this.exactOrdinaryFaces.get(var8);
            Facing var12 = var7.opposite();
            long var13 = var12.positive ? var10 & 4294967295L : var10 >>> 32;
            if (var13 != 0L) {
               int var15 = (int)var13 - 1;
               LittleRenderBox var16 = this.boxesById.get(var15);
               if (isExactFaceMatch(var2, var7, var16.box)) {
                  this.exactOrdinaryCovered[var3 * Facing.VALUES.length + var7.ordinal()] = true;
                  this.exactOrdinaryCovered[var15 * Facing.VALUES.length + var12.ordinal()] = true;
               }
            }

            long var17 = var3 + 1L & 4294967295L;
            if (var7.positive) {
               if ((var10 & 4294967295L) == 0L) {
                  this.exactOrdinaryFaces.put(var8, var10 & -4294967296L | var17);
               }
            } else if (var10 >>> 32 == 0L) {
               this.exactOrdinaryFaces.put(var8, var10 & 4294967295L | var17 << 32);
            }
         }
      }

      private static boolean isExactFaceMatch(LittleBox var0, Facing var1, LittleBox var2) {
         Axis var3 = var1.one();
         Axis var4 = var1.two();
         return var2.get(var1.opposite()) == var0.get(var1)
            && var2.getMin(var3) == var0.getMin(var3)
            && var2.getMax(var3) == var0.getMax(var3)
            && var2.getMin(var4) == var0.getMin(var4)
            && var2.getMax(var4) == var0.getMax(var4);
      }

      private static long exactFaceKey(LittleBox var0, Facing var1) {
         Axis var2 = var1.one();
         Axis var3 = var1.two();
         long var4 = -7046029254386353131L ^ var1.axis.ordinal();
         var4 = mixExactFace(var4, var0.get(var1));
         var4 = mixExactFace(var4, var0.getMin(var2));
         var4 = mixExactFace(var4, var0.getMax(var2));
         var4 = mixExactFace(var4, var0.getMin(var3));
         return mixExactFace(var4, var0.getMax(var3));
      }

      private static long mixExactFace(long var0, int var2) {
         long var3 = var2 * -7046029254386353131L;
         var3 ^= var3 >>> 30;
         var3 *= -4658895280553007687L;
         var3 ^= var3 >>> 27;
         var3 *= -7723592293110705685L;
         var3 ^= var3 >>> 31;
         var0 ^= var3;
         var0 = Long.rotateLeft(var0, 27);
         return var0 * 5L + 1390208809L;
      }

      private void addOrdinaryVolume(int var1, LittleBox var2) {
         int var3 = Math.floorDiv(var2.minX, this.cellSize);
         int var4 = Math.floorDiv(var2.maxX - 1, this.cellSize);
         int var5 = Math.floorDiv(var2.minY, this.cellSize);
         int var6 = Math.floorDiv(var2.maxY - 1, this.cellSize);
         int var7 = Math.floorDiv(var2.minZ, this.cellSize);
         int var8 = Math.floorDiv(var2.maxZ - 1, this.cellSize);
         long var9 = (long)(var4 - var3 + 1) * (var6 - var5 + 1) * (var8 - var7 + 1);
         if (var9 > 256L) {
            this.broadOrdinaryVolumes.add(var1);
         } else {
            for (int var11 = var3; var11 <= var4; var11++) {
               for (int var12 = var5; var12 <= var6; var12++) {
                  for (int var13 = var7; var13 <= var8; var13++) {
                     this.ordinaryVolumeCells
                        .computeIfAbsent(new OutlineOrdinaryFaces.VolumeCellKey(var11, var12, var13), var0 -> new IntArrayList())
                        .add(var1);
                  }
               }
            }
         }
      }

      List<LittleRenderBox> findOrdinaryVolumes(VectorFan var1) {
         float var2 = Float.POSITIVE_INFINITY;
         float var3 = Float.POSITIVE_INFINITY;
         float var4 = Float.POSITIVE_INFINITY;
         float var5 = Float.NEGATIVE_INFINITY;
         float var6 = Float.NEGATIVE_INFINITY;
         float var7 = Float.NEGATIVE_INFINITY;

         for (int var8 = 0; var8 < var1.count(); var8++) {
            Vec3f var9 = var1.get(var8);
            var2 = Math.min(var2, var9.x);
            var3 = Math.min(var3, var9.y);
            var4 = Math.min(var4, var9.z);
            var5 = Math.max(var5, var9.x);
            var6 = Math.max(var6, var9.y);
            var7 = Math.max(var7, var9.z);
         }

         int var19 = (int)Math.floor((var2 - 5.0E-4F) / this.cellSize);
         int var20 = (int)Math.floor((var5 + 5.0E-4F) / this.cellSize);
         int var10 = (int)Math.floor((var3 - 5.0E-4F) / this.cellSize);
         int var11 = (int)Math.floor((var6 + 5.0E-4F) / this.cellSize);
         int var12 = (int)Math.floor((var4 - 5.0E-4F) / this.cellSize);
         int var13 = (int)Math.floor((var7 + 5.0E-4F) / this.cellSize);
         List var14 = this.beginQuery(this.queries.get().queryResult);
         this.addUnique(var14, this.broadOrdinaryVolumes);

         for (int var15 = var19; var15 <= var20; var15++) {
            for (int var16 = var10; var16 <= var11; var16++) {
               for (int var17 = var12; var17 <= var13; var17++) {
                  IntArrayList var18 = this.ordinaryVolumeCells.get(new OutlineOrdinaryFaces.VolumeCellKey(var15, var16, var17));
                  if (var18 != null) {
                     this.addUnique(var14, var18);
                  }
               }
            }
         }

         return var14;
      }

      List<LittleRenderBox> findOpposite(LittleBox var1, Facing var2) {
         return this.findOpposite(
            var2.opposite(), var1.get(var2), var1.getMin(var2.one()), var1.getMax(var2.one()), var1.getMin(var2.two()), var1.getMax(var2.two())
         );
      }

      List<LittleRenderBox> findOpposite(LittleFace var1) {
         return this.findOpposite(var1.facing.opposite(), var1.origin, var1.minOne, var1.maxOne, var1.minTwo, var1.maxTwo);
      }

      private List<LittleRenderBox> findOpposite(Facing var1, int var2, int var3, int var4, int var5, int var6) {
         int var7 = var1.ordinal();
         int var8 = Math.floorDiv(var3, this.cellSize);
         int var9 = Math.floorDiv(var4 - 1, this.cellSize);
         int var10 = Math.floorDiv(var5, this.cellSize);
         int var11 = Math.floorDiv(var6 - 1, this.cellSize);
         long var12 = (long)(var9 - var8 + 1) * (var11 - var10 + 1);
         if (var12 > 256L) {
            IntArrayList var20 = this.planes[var7].get(var2);
            return var20 != null ? this.materialize(var20) : List.of();
         }

         List var14 = this.beginQuery(this.queries.get().queryResult);
         IntArrayList var15 = this.broad[var7].get(var2);
         if (var15 != null) {
            this.addUnique(var14, var15);
         }

         Long2ObjectOpenHashMap var16 = this.cells[var7].get(var2);
         if (var16 == null) {
            return var14;
         }

         for (int var17 = var8; var17 <= var9; var17++) {
            for (int var18 = var10; var18 <= var11; var18++) {
               IntArrayList var19 = (IntArrayList)var16.get(packedCell(var17, var18));
               if (var19 != null) {
                  this.addUnique(var14, var19);
               }
            }
         }

         return var14;
      }

      List<LittleRenderBox> findExactOpposite(LittleBox var1, Facing var2) {
         IntArrayList var3 = this.planes[var2.opposite().ordinal()].get(var1.get(var2));
         Axis var4 = var2.one();
         Axis var5 = var2.two();
         this.queries.get().filteredResult.clear();
         if (var3 == null) {
            return this.queries.get().filteredResult;
         }

         for (int var6 = 0; var6 < var3.size(); var6++) {
            LittleRenderBox var7 = this.boxesById.get(var3.getInt(var6));
            LittleBox var8 = var7.box;
            if (var8.getMin(var4) == var1.getMin(var4)
               && var8.getMax(var4) == var1.getMax(var4)
               && var8.getMin(var5) == var1.getMin(var5)
               && var8.getMax(var5) == var1.getMax(var5)) {
               this.queries.get().filteredResult.add(var7);
            }
         }

         return this.queries.get().filteredResult;
      }

      int id(LittleRenderBox var1) {
         Integer var2 = this.boxIds.get(var1);
         return var2 == null ? -1 : var2;
      }

      boolean wasTransformAxisVisible(LittleRenderBoxTransformable var1, Facing var2) {
         int var3 = this.id(var1);
         return var3 >= 0 && this.originalTransformAxisVisible[var3 * Facing.VALUES.length + var2.ordinal()];
      }

      List<LittleRenderBox> findProjectedTransformables(LittleFace var1) {
         return this.findProjectedTransformables(var1.facing, var1.origin, var1.one, var1.two, var1.minOne, var1.maxOne, var1.minTwo, var1.maxTwo);
      }

      List<LittleRenderBox> findProjectedTransformables(LittleBox var1, Facing var2) {
         return this.findProjectedTransformables(
            var2, var1.get(var2), var2.one(), var2.two(), var1.getMin(var2.one()), var1.getMax(var2.one()), var1.getMin(var2.two()), var1.getMax(var2.two())
         );
      }

      private List<LittleRenderBox> findProjectedTransformables(Facing var1, int var2, Axis var3, Axis var4, int var5, int var6, int var7, int var8) {
         int var9 = var1.ordinal();
         int var10 = Math.floorDiv(var5, this.cellSize);
         int var11 = Math.floorDiv(var6 - 1, this.cellSize);
         int var12 = Math.floorDiv(var7, this.cellSize);
         int var13 = Math.floorDiv(var8 - 1, this.cellSize);
         List var14 = this.beginQuery(this.queries.get().queryResult);
         IntArrayList var15 = this.broadTransformProjections[var9];
         if (var15 != null) {
            this.addUnique(var14, var15);
         }

         for (int var16 = var10; var16 <= var11; var16++) {
            for (int var17 = var12; var17 <= var13; var17++) {
               IntArrayList var18 = this.transformProjectionCells[var9].get(packedCell(var16, var17));
               if (var18 != null) {
                  this.addUnique(var14, var18);
               }
            }
         }

         this.queries.get().filteredResult.clear();
         ArrayList var23 = this.queries.get().filteredResult;

         for (LittleRenderBox var25 : (Iterable<LittleRenderBox>)(Iterable<?>)(var14)) {
            LittleBox var19 = var25.box;
            if (var19.getMax(var3) > var5 && var19.getMin(var3) < var6 && var19.getMax(var4) > var7 && var19.getMin(var4) < var8) {
               int var20 = var19.getMin(var1.axis);
               int var21 = var19.getMax(var1.axis);
               boolean var22 = var20 < var2 && var21 > var2;
               if (var22) {
                  var23.add(var25);
               }
            }
         }

         return var23;
      }

      private List<LittleRenderBox> beginQuery(ArrayList<LittleRenderBox> var1) {
         var1.clear();
         if (++this.queries.get().visitMark == 0) {
            Arrays.fill(this.queries.get().visitMarks, 0);
            this.queries.get().visitMark = 1;
         }

         return var1;
      }

      private List<LittleRenderBox> materialize(IntArrayList var1) {
         this.queries.get().queryResult.clear();

         for (int var2 = 0; var2 < var1.size(); var2++) {
            this.queries.get().queryResult.add(this.boxesById.get(var1.getInt(var2)));
         }

         return this.queries.get().queryResult;
      }

      private void addUnique(List<LittleRenderBox> var1, IntArrayList var2) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            int var4 = var2.getInt(var3);
            if (this.queries.get().visitMarks[var4] != this.queries.get().visitMark) {
               this.queries.get().visitMarks[var4] = this.queries.get().visitMark;
               var1.add(this.boxesById.get(var4));
            }
         }
      }

      boolean hasTransformables() {
         return this.hasTransformables;
      }

      long candidateVisits() {
         return this.candidateVisits;
      }

      long uniqueCandidates() {
         return this.uniqueCandidates;
      }

      long queryCount() {
         return this.queryCount;
      }

      private static long packedCell(int var0, int var1) {
         return (long)var0 << 32 ^ var1 & 4294967295L;
      }

      private static final class QueryState {
         final int[] visitMarks;
         int visitMark;
         final ArrayList<LittleRenderBox> queryResult = new ArrayList<>();
         final ArrayList<LittleRenderBox> filteredResult = new ArrayList<>();

         QueryState(int var1) {
            this.visitMarks = new int[var1];
         }
      }
   }

   private record FaceRect(int minOne, int minTwo, int maxOne, int maxTwo) {
   }

   private record OrdinaryClip(boolean touched, int cutCount, List<VectorFan> exposed) {
      private static final OutlineOrdinaryFaces.OrdinaryClip UNTOUCHED = new OutlineOrdinaryFaces.OrdinaryClip(false, 0, List.of());
   }

   private record QuantizedPoint(long x, long y, long z) implements Comparable<OutlineOrdinaryFaces.QuantizedPoint> {
      public int compareTo(OutlineOrdinaryFaces.QuantizedPoint var1) {
         int var2 = Long.compare(this.x, var1.x);
         if (var2 != 0) {
            return var2;
         }

         var2 = Long.compare(this.y, var1.y);
         return var2 != 0 ? var2 : Long.compare(this.z, var1.z);
      }
   }

   private record TiltedFanKey(List<OutlineOrdinaryFaces.QuantizedPoint> points) {
      private static OutlineOrdinaryFaces.TiltedFanKey of(VectorFan var0) {
         ArrayList<OutlineOrdinaryFaces.QuantizedPoint> var1 = new ArrayList<>(var0.count());

         for (int var2 = 0; var2 < var0.count(); var2++) {
            Vec3f var3 = var0.get(var2);
            var1.add(new OutlineOrdinaryFaces.QuantizedPoint(quantize(var3.x), quantize(var3.y), quantize(var3.z)));
         }

         var1.sort(OutlineOrdinaryFaces.QuantizedPoint::compareTo);
         return new OutlineOrdinaryFaces.TiltedFanKey(List.copyOf(var1));
      }

      private static long quantize(float var0) {
         return Math.round(var0 / 5.0E-4F);
      }
   }

   private static final class TransformFaceSnapshot {
      private final LittleRenderBoxTransformable owner;
      private final Facing facing;
      private final List<VectorFan> fans;
      private final float scale;
      private final boolean[] removed;

      private TransformFaceSnapshot(LittleRenderBoxTransformable var1, Facing var2, List<VectorFan> var3, float var4) {
         this.owner = var1;
         this.facing = var2;
         this.fans = var3;
         this.scale = var4;
         this.removed = new boolean[var3.size()];
      }
   }

   private record VolumeCellKey(int x, int y, int z) {
   }
}
