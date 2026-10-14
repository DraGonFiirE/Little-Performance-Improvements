package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.Vec3f;

public final class OutlinePackedLines {
   private final BlockState state;
   private final List<RenderBox> output;
   private float[] values = new float[24576];
   private int size;

   public OutlinePackedLines(BlockState var1, List<RenderBox> var2) {
      this.state = var1;
      this.output = var2;
   }

   public void add(SimpleOutlineEdges.Segment var1) {
      this.values[this.size++] = (float)var1.a().x();
      this.values[this.size++] = (float)var1.a().y();
      this.values[this.size++] = (float)var1.a().z();
      this.values[this.size++] = (float)var1.b().x();
      this.values[this.size++] = (float)var1.b().y();
      this.values[this.size++] = (float)var1.b().z();
      if (this.size == this.values.length) {
         this.flush();
      }
   }

   private void flush() {
      if (this.size != 0) {
         this.output.add(new OutlinePackedLines.PackedBox(Arrays.copyOf(this.values, this.size), this.state));
         this.size = 0;
      }
   }

   public void finish() {
      this.flush();
   }

   private static final class PackedBox extends RenderBox {
      static final RenderBoxFaceSpecial EMPTY = new RenderBoxFaceSpecial(List.of(), 1.0F);

      PackedBox(float[] var1, BlockState var2) {
         super(min(var1, 0), min(var1, 1), min(var1, 2), max(var1, 0), max(var1, 1), max(var1, 2), var2);
         RenderBoxFaceSpecial var3 = new RenderBoxFaceSpecial(List.of(new OutlinePackedLines.PackedFan(var1)), 1.0F);

         for (Facing var7 : Facing.VALUES) {
            this.setFace(var7, var7 == Facing.EAST ? var3 : EMPTY);
         }
      }

      static float min(float[] var0, int var1) {
         float var2 = Float.POSITIVE_INFINITY;

         for (int var3 = var1; var3 < var0.length; var3 += 3) {
            var2 = Math.min(var2, var0[var3]);
         }

         return var2 - 0.01F;
      }

      static float max(float[] var0, int var1) {
         float var2 = Float.NEGATIVE_INFINITY;

         for (int var3 = var1; var3 < var0.length; var3 += 3) {
            var2 = Math.max(var2, var0[var3]);
         }

         return var2 + 0.01F;
      }

      @Override
      protected boolean previewScalingAndOffset() {
         return false;
      }
   }

   private static final class PackedFan extends VectorFan {
      private final float[] values;

      PackedFan(float[] var1) {
         super(new Vec3f[]{new Vec3f(0.0F, 0.0F, 0.0F), new Vec3f(0.0F, 0.0F, 0.0F), new Vec3f(0.0F, 0.0F, 0.0F), new Vec3f(0.0F, 0.0F, 0.0F)});
         this.values = var1;
      }

      @Override
      public void forAllEdges(BiConsumer<Vec3f, Vec3f> var1) {
         Vec3f var2 = new Vec3f(0.0F, 0.0F, 0.0F);
         Vec3f var3 = new Vec3f(0.0F, 0.0F, 0.0F);

         for (int var4 = 0; var4 < this.values.length; var4 += 6) {
            var2.x = this.values[var4];
            var2.y = this.values[var4 + 1];
            var2.z = this.values[var4 + 2];
            var3.x = this.values[var4 + 3];
            var3.y = this.values[var4 + 4];
            var3.z = this.values[var4 + 5];
            var1.accept(var2, var3);
         }
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
