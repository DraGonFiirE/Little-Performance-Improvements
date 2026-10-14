package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.BoxCorner;
import team.creative.creativecore.common.util.math.box.BoxFace;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleTransformableBox.VectorFanFaceCache;

public final class FastBlueprintOutline {
   private static final RenderBoxFaceSpecial EMPTY = new RenderBoxFaceSpecial(List.of(), 1.0F);

   public static List<RenderBox> retainSurfaceOutline(List<RenderBox> var0, LittleGrid var1) {
      if (var0.isEmpty()) {
         return var0;
      }

      Thread var2 = Thread.currentThread();
      int var3 = var2.getPriority();
      var2.setPriority(Math.min(var3, 2));

      try {
         boolean var4 = true;

         for (RenderBox var6 : var0) {
            if (var6 instanceof LittleRenderBoxTransformable) {
               var4 = false;
               break;
            }
         }

         ParallelOutlineSolidUnion var15 = new ParallelOutlineSolidUnion();

         for (RenderBox var7 : var0) {
            OutlineWorkChunks.check();
            if (var7 instanceof LittleRenderBoxTransformable) {
               ArrayList var8 = new ArrayList();
               collect(var7, var8::add);
               var15.mesh(var8);
            } else {
               var15.box(var7.minX, var7.minY, var7.minZ, var7.maxX, var7.maxY, var7.maxZ);
            }
         }

         var15.build();
         OutlineOrdinaryFaces.retainAirTouching(var0, var1);
         if (var0.isEmpty()) {
            return var0;
         }

         SmoothOutlineEdges var17 = new SmoothOutlineEdges();
         var17.solids(var15);

         for (RenderBox var20 : var0) {
            collect(var20, var17::polygon);
         }

         ArrayList var19 = new ArrayList();
         BlockState var21 = ((RenderBox)var0.getFirst()).state;
         OutlinePackedLines var9 = new OutlinePackedLines(var21, var19);
         var17.finish(Function.identity(), var9::add);
         var9.finish();
         OutlineWorkChunks.releaseScratch();
         return var19;
      } finally {
         OutlineWorkChunks.releaseScratch();
         var2.setPriority(var3);
      }
   }

   private static void collect(RenderBox var0, Consumer<SimpleOutlineEdges.Point[]> var1) {
      for (Facing var5 : Facing.VALUES) {
         RenderBoxFace var6 = var0.getFace(var5);
         if (var6.hasCachedFans()) {
            fans(var1, var0, var6.getCachedFans());
         } else if (var0 instanceof LittleRenderBoxTransformable var7) {
            VectorFanFaceCache var12 = var7.getFaceCache(var5);
            if (var12 != null) {
               if (var6.shouldRender() && var12.hasAxisStrip()) {
                  fans(var1, var0, var12.axisStrips);
               }

               if (var12.hasTiltedStripsRendering()) {
                  ArrayList var13 = new ArrayList();
                  var12.collectAllTiltedStripsRendering(var13);
                  fans(var1, var0, var13);
               }
            }
         } else if (var6.shouldRender()) {
            BoxCorner[] var8 = BoxFace.get(var5).getCorners();
            SimpleOutlineEdges.Point[] var9 = new SimpleOutlineEdges.Point[var8.length];

            for (int var10 = 0; var10 < var8.length; var10++) {
               BoxCorner var11 = var8[var10];
               var9[var10] = new SimpleOutlineEdges.Point(
                  var11.x.positive ? var0.maxX : var0.minX, var11.y.positive ? var0.maxY : var0.minY, var11.z.positive ? var0.maxZ : var0.minZ
               );
            }

            var1.accept(var9);
         }
      }
   }

   public static List<RenderBox> retainAirTouching(List<RenderBox> var0, int var1) {
      return var0;
   }

   private static void fans(Consumer<SimpleOutlineEdges.Point[]> var0, RenderBox var1, List<VectorFan> var2) {
      for (VectorFan var4 : var2) {
         SimpleOutlineEdges.Point[] var5 = new SimpleOutlineEdges.Point[var4.count()];

         for (int var6 = 0; var6 < var5.length; var6++) {
            Vec3f var7 = var4.get(var6);
            var5[var6] = new SimpleOutlineEdges.Point(
               var1.getPreviewOffX() + var7.x * var1.getPreviewScaleX(),
               var1.getPreviewOffY() + var7.y * var1.getPreviewScaleY(),
               var1.getPreviewOffZ() + var7.z * var1.getPreviewScaleZ()
            );
         }

         var0.accept(var5);
      }
   }

   private static final class EdgeBox extends RenderBox {
      EdgeBox(SimpleOutlineEdges.Segment var1, BlockState var2) {
         super(
            (float)Math.min(var1.a().x(), var1.b().x()) - 0.01F,
            (float)Math.min(var1.a().y(), var1.b().y()) - 0.01F,
            (float)Math.min(var1.a().z(), var1.b().z()) - 0.01F,
            (float)Math.max(var1.a().x(), var1.b().x()) + 0.01F,
            (float)Math.max(var1.a().y(), var1.b().y()) + 0.01F,
            (float)Math.max(var1.a().z(), var1.b().z()) + 0.01F,
            var2
         );
         RenderBoxFaceSpecial var3 = new RenderBoxFaceSpecial(List.of(new FastBlueprintOutline.EdgeFan(var1)), 1.0F);

         for (Facing var7 : Facing.VALUES) {
            this.setFace(var7, var7 == Facing.EAST ? var3 : FastBlueprintOutline.EMPTY);
         }
      }

      @Override
      protected boolean previewScalingAndOffset() {
         return false;
      }
   }

   private static final class EdgeFan extends VectorFan {
      EdgeFan(SimpleOutlineEdges.Segment var1) {
         super(new Vec3f[]{point(var1.a()), point(var1.b()), point(var1.b()), point(var1.a())});
      }

      private static Vec3f point(SimpleOutlineEdges.Point var0) {
         return new Vec3f((float)var0.x(), (float)var0.y(), (float)var0.z());
      }

      @Override
      public void forAllEdges(BiConsumer<Vec3f, Vec3f> var1) {
         var1.accept(this.get(0), this.get(1));
      }

      @Override
      public void renderLines(
         Pose var1, VertexConsumer var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9, int var10, int var11, int var12
      ) {
         this.renderLines(var1, var2, var9, var10, var11, var12);
      }

      @Override
      public void renderLines(Pose var1, VertexConsumer var2, int var3, int var4, int var5, int var6, Vec3d var7, double var8) {
         this.renderLines(var1, var2, var3, var4, var5, var6);
      }

      @Override
      public void renderLines(
         Pose var1,
         VertexConsumer var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         int var9,
         int var10,
         int var11,
         int var12,
         Vec3d var13,
         double var14
      ) {
         this.renderLines(var1, var2, var9, var10, var11, var12);
      }
   }
}
