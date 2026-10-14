package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.common.grid.LittleGrid;

public final class SimpleBlueprintOutline {
   private static final RenderBoxFaceSpecial EMPTY = new RenderBoxFaceSpecial(List.of(), 1.0F);

   public static List<RenderBox> retainSurfaceOutline(List<RenderBox> var0, LittleGrid var1) {
      return FastBlueprintOutline.retainSurfaceOutline(var0, var1);
   }

   public static List<RenderBox> retainAirTouching(List<RenderBox> var0, int var1) {
      return var0;
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
         RenderBoxFaceSpecial var3 = new RenderBoxFaceSpecial(List.of(new SimpleBlueprintOutline.EdgeFan(var1)), 1.0F);

         for (Facing var7 : Facing.VALUES) {
            this.setFace(var7, var7 == Facing.EAST ? var3 : SimpleBlueprintOutline.EMPTY);
         }

         super.color = -1;
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
