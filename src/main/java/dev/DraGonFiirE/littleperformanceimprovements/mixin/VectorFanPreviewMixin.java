package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PreviewFanTriangulation;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;

@Mixin(VectorFan.class)
public abstract class VectorFanPreviewMixin {
   @Inject(method = "renderPreview(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIII)V", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$triangulatePreviewFan(
      Matrix4f matrix, VertexConsumer consumer, int red, int green, int blue, int alpha, CallbackInfo callback
   ) {
      if (PreviewFanTriangulation.active()) {
         Vec3f[] points = ((VectorFan)(Object)this).getCoords();
         boolean facingMarkers = PreviewFanTriangulation.facingMarkersActive();
         if (facingMarkers) {
            red = PreviewFanTriangulation.currentFacing() + 1;
         }

         littleperformanceimprovements$emitFan(matrix, consumer, points, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, red, green, blue, alpha);
         callback.cancel();
      }
   }

   @Inject(method = "renderPreview(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFIIII)V", at = @At("HEAD"), cancellable = true)
   private void littleperformanceimprovements$triangulateScaledPreviewFan(
      Matrix4f matrix,
      VertexConsumer consumer,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      int red,
      int green,
      int blue,
      int alpha,
      CallbackInfo callback
   ) {
      if (PreviewFanTriangulation.active()) {
         Vec3f[] points = ((VectorFan)(Object)this).getCoords();
         boolean facingMarkers = PreviewFanTriangulation.facingMarkersActive();
         if (facingMarkers) {
            red = PreviewFanTriangulation.currentFacing() + 1;
         }

         littleperformanceimprovements$emitFan(matrix, consumer, points, offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha);
         callback.cancel();
      }
   }

   private static void littleperformanceimprovements$emitFan(
      Matrix4f matrix,
      VertexConsumer consumer,
      Vec3f[] points,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      int red,
      int green,
      int blue,
      int alpha
   ) {
      List<Integer> polygon = littleperformanceimprovements$cleanPolygon(points);
      if (polygon.size() >= 3) {
         int projectionAxis = littleperformanceimprovements$projectionAxis(points, polygon);
         double signedArea = littleperformanceimprovements$signedArea(points, polygon, projectionAxis);
         boolean counterClockwise = signedArea >= 0.0;
         int guard = polygon.size() * polygon.size();

         label70:
         while (polygon.size() > 3 && guard-- > 0) {
            boolean clipped = false;

            for (int index = 0; index < polygon.size(); index++) {
               int previous = polygon.get((index + polygon.size() - 1) % polygon.size());
               int current = polygon.get(index);
               int next = polygon.get((index + 1) % polygon.size());
               double cross = littleperformanceimprovements$cross2d(points[previous], points[current], points[next], projectionAxis);
               if ((counterClockwise ? !(cross <= 1.0E-8) : !(cross >= -1.0E-8))
                  && !littleperformanceimprovements$containsProjectedPoint(points, polygon, previous, current, next, projectionAxis, counterClockwise)) {
                  littleperformanceimprovements$emitTriangle(
                     matrix, consumer, points, previous, current, next, offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha
                  );
                  polygon.remove(index);
                  clipped = true;
                  break;
               }
            }

            if (!clipped) {
               int index = 0;

               while (true) {
                  if (index < polygon.size()) {
                     int previous = polygon.get((index + polygon.size() - 1) % polygon.size());
                     int current = polygon.get(index);
                     int next = polygon.get((index + 1) % polygon.size());
                     if (!(Math.abs(littleperformanceimprovements$cross2d(points[previous], points[current], points[next], projectionAxis)) <= 1.0E-8)) {
                        index++;
                        continue;
                     }

                     polygon.remove(index);
                     clipped = true;
                  }

                  if (!clipped) {
                     break label70;
                  }
                  break;
               }
            }
         }

         if (polygon.size() == 3) {
            littleperformanceimprovements$emitTriangle(
               matrix,
               consumer,
               points,
               polygon.get(0),
               polygon.get(1),
               polygon.get(2),
               offsetX,
               offsetY,
               offsetZ,
               scaleX,
               scaleY,
               scaleZ,
               red,
               green,
               blue,
               alpha
            );
         }
      }
   }

   private static void littleperformanceimprovements$emitTriangle(
      Matrix4f matrix,
      VertexConsumer consumer,
      Vec3f[] points,
      int first,
      int second,
      int third,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      int red,
      int green,
      int blue,
      int alpha
   ) {
      int facingOrdinal = PreviewFanTriangulation.currentFacing();
      if (facingOrdinal >= 0 && facingOrdinal < Facing.VALUES.length) {
         Vec3f a = points[first];
         Vec3f b = points[second];
         Vec3f c = points[third];
         float ax = (b.x - a.x) * scaleX;
         float ay = (b.y - a.y) * scaleY;
         float az = (b.z - a.z) * scaleZ;
         float bx = (c.x - a.x) * scaleX;
         float by = (c.y - a.y) * scaleY;
         float bz = (c.z - a.z) * scaleZ;
         float nx = ay * bz - az * by;
         float ny = az * bx - ax * bz;
         float nz = ax * by - ay * bx;
         Facing facing = Facing.VALUES[facingOrdinal];

         float outward = switch (facing.axis) {
            case X -> nx * facing.offset();
            case Y -> ny * facing.offset();
            case Z -> nz * facing.offset();
         };
         if (outward < -1.0E-8F) {
            int swap = second;
            second = third;
            third = swap;
         }
      }

      littleperformanceimprovements$emitVertex(matrix, consumer, points[first], offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha);
      littleperformanceimprovements$emitVertex(matrix, consumer, points[second], offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha);
      littleperformanceimprovements$emitVertex(matrix, consumer, points[third], offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha);
      littleperformanceimprovements$emitVertex(matrix, consumer, points[third], offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ, red, green, blue, alpha);
   }

   private static List<Integer> littleperformanceimprovements$cleanPolygon(Vec3f[] points) {
      List<Integer> polygon = new ArrayList<>(points.length);

      for (int index = 0; index < points.length; index++) {
         if (polygon.isEmpty() || !littleperformanceimprovements$samePoint(points[polygon.getLast()], points[index])) {
            polygon.add(index);
         }
      }

      if (polygon.size() > 1 && littleperformanceimprovements$samePoint(points[polygon.getFirst()], points[polygon.getLast()])) {
         polygon.removeLast();
      }

      return polygon;
   }

   private static boolean littleperformanceimprovements$samePoint(Vec3f first, Vec3f second) {
      return Math.abs(first.x - second.x) <= 1.0E-6F && Math.abs(first.y - second.y) <= 1.0E-6F && Math.abs(first.z - second.z) <= 1.0E-6F;
   }

   private static int littleperformanceimprovements$projectionAxis(Vec3f[] points, List<Integer> polygon) {
      double normalX = 0.0;
      double normalY = 0.0;
      double normalZ = 0.0;

      for (int index = 0; index < polygon.size(); index++) {
         Vec3f current = points[polygon.get(index)];
         Vec3f next = points[polygon.get((index + 1) % polygon.size())];
         normalX += (current.y - next.y) * (current.z + next.z);
         normalY += (current.z - next.z) * (current.x + next.x);
         normalZ += (current.x - next.x) * (current.y + next.y);
      }

      double x = Math.abs(normalX);
      double y = Math.abs(normalY);
      double z = Math.abs(normalZ);
      if (x >= y && x >= z) {
         return 0;
      } else {
         return y >= z ? 1 : 2;
      }
   }

   private static double littleperformanceimprovements$signedArea(Vec3f[] points, List<Integer> polygon, int projectionAxis) {
      double area = 0.0;

      for (int index = 0; index < polygon.size(); index++) {
         Vec3f current = points[polygon.get(index)];
         Vec3f next = points[polygon.get((index + 1) % polygon.size())];
         area += littleperformanceimprovements$u(current, projectionAxis) * littleperformanceimprovements$v(next, projectionAxis)
            - littleperformanceimprovements$u(next, projectionAxis) * littleperformanceimprovements$v(current, projectionAxis);
      }

      return area * 0.5;
   }

   private static double littleperformanceimprovements$cross2d(Vec3f first, Vec3f second, Vec3f third, int projectionAxis) {
      double abU = littleperformanceimprovements$u(second, projectionAxis) - littleperformanceimprovements$u(first, projectionAxis);
      double abV = littleperformanceimprovements$v(second, projectionAxis) - littleperformanceimprovements$v(first, projectionAxis);
      double acU = littleperformanceimprovements$u(third, projectionAxis) - littleperformanceimprovements$u(first, projectionAxis);
      double acV = littleperformanceimprovements$v(third, projectionAxis) - littleperformanceimprovements$v(first, projectionAxis);
      return abU * acV - abV * acU;
   }

   private static boolean littleperformanceimprovements$containsProjectedPoint(
      Vec3f[] points, List<Integer> polygon, int first, int second, int third, int projectionAxis, boolean counterClockwise
   ) {
      for (int candidate : polygon) {
         if (candidate != first && candidate != second && candidate != third) {
            double firstCross = littleperformanceimprovements$cross2d(points[first], points[second], points[candidate], projectionAxis);
            double secondCross = littleperformanceimprovements$cross2d(points[second], points[third], points[candidate], projectionAxis);
            double thirdCross = littleperformanceimprovements$cross2d(points[third], points[first], points[candidate], projectionAxis);
            if (counterClockwise
               ? firstCross > 1.0E-8 && secondCross > 1.0E-8 && thirdCross > 1.0E-8
               : firstCross < -1.0E-8 && secondCross < -1.0E-8 && thirdCross < -1.0E-8) {
               return true;
            }
         }
      }

      return false;
   }

   private static double littleperformanceimprovements$u(Vec3f point, int projectionAxis) {
      return switch (projectionAxis) {
         case 0 -> point.y;
         case 1 -> point.x;
         default -> point.x;
      };
   }

   private static double littleperformanceimprovements$v(Vec3f point, int projectionAxis) {
      return switch (projectionAxis) {
         case 0 -> point.z;
         case 1 -> point.z;
         default -> point.y;
      };
   }

   private static void littleperformanceimprovements$emitVertex(
      Matrix4f matrix,
      VertexConsumer consumer,
      Vec3f point,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      int red,
      int green,
      int blue,
      int alpha
   ) {
      consumer.addVertex(matrix, point.x * scaleX + offsetX, point.y * scaleY + offsetY, point.z * scaleZ + offsetZ).setColor(red, green, blue, alpha);
   }
}
