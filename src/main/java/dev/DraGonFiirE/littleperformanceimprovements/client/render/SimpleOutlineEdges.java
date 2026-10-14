package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SimpleOutlineEdges {
   private static final double POSITION_SCALE = 100000.0;
   private static final double DIRECTION_SCALE = 1000000.0;
   private static final double NORMAL_DOT_TOLERANCE = 1.0E-8;
   private OutlineSolidUnion solids;
   private final Map<SimpleOutlineEdges.LineKey, SimpleOutlineEdges.Line> lines = new HashMap<>();
   private final ArrayList<SimpleOutlineEdges.Face> faces = new ArrayList<>();
   private static final double CONTACT_EPSILON = 2.0E-5;

   public void solids(OutlineSolidUnion var1) {
      this.solids = var1;
   }

   private static boolean overlaps(double[] var0, double[] var1) {
      for (int var2 = 0; var2 < 3; var2++) {
         if (var0[var2] > var1[var2 + 3] + 2.0E-5 || var1[var2] > var0[var2 + 3] + 2.0E-5) {
            return false;
         }
      }

      return true;
   }

   public void polygon(SimpleOutlineEdges.Point[] var1) {
      HeldPreviewBuild.check();
      if (var1.length >= 3) {
         SimpleOutlineEdges.Point var2 = new SimpleOutlineEdges.Point(0.0, 0.0, 0.0);
         SimpleOutlineEdges.Point var3 = var1[0];
         int var4 = 1;

         while (var4 + 1 < var1.length) {
            var2 = var2.add(var1[var4].sub(var3).cross(var1[var4 + 1].sub(var3)));
            var4++;
            HeldPreviewBuild.check();
         }

         double var26 = var2.length();
         if (Double.isFinite(var26) && !(var26 < 1.0E-12)) {
            int var6 = var2.negative() ? -1 : 1;
            var2 = var2.scale(var6 / var26);
            this.faces.add(new SimpleOutlineEdges.Face(var1, var2));
            int var7 = 0;

            while (var7 < var1.length) {
               SimpleOutlineEdges.Point var8 = var1[var7];
               SimpleOutlineEdges.Point var9 = var1[(var7 + 1) % var1.length];
               SimpleOutlineEdges.Point var10 = var9.sub(var8);
               double var11 = var10.length();
               if (Double.isFinite(var11) && !(var11 < 1.0E-8)) {
                  int var13 = var10.negative() ? -1 : 1;
                  var10 = var10.scale(var13 / var11);
                  SimpleOutlineEdges.Point var14 = var8.sub(var10.scale(var8.dot(var10)));
                  SimpleOutlineEdges.LineKey var15 = new SimpleOutlineEdges.LineKey(
                     Math.round(var10.x * 1000000.0),
                     Math.round(var10.y * 1000000.0),
                     Math.round(var10.z * 1000000.0),
                     Math.round(var14.x * 100000.0),
                     Math.round(var14.y * 100000.0),
                     Math.round(var14.z * 100000.0)
                  );
                  SimpleOutlineEdges.Line var16 = this.lines.get(var15);
                  if (var16 == null) {
                     var16 = new SimpleOutlineEdges.Line(var10, var14);
                     this.lines.put(var15, var16);
                  }

                  long var17 = Math.round(var8.dot(var16.direction) * 100000.0);
                  long var19 = Math.round(var9.dot(var16.direction) * 100000.0);
                  if (var17 != var19) {
                     SimpleOutlineEdges.Plane var21 = var16.plane(var2);
                     int var22 = var13 * var6 * (var21.normal.dot(var2) < 0.0 ? -1 : 1);
                     int var23 = var22 > 0 ? 1 : 0;
                     int var24 = var22 < 0 ? 1 : 0;
                     var21.events.add(new SimpleOutlineEdges.SideEvent(Math.min(var17, var19), var23, var24));
                     var21.events.add(new SimpleOutlineEdges.SideEvent(Math.max(var17, var19), -var23, -var24));
                  }
               }

               var7++;
               HeldPreviewBuild.check();
            }
         }
      }
   }

   public void finish(Consumer<SimpleOutlineEdges.Segment> var1) {
      HeldPreviewBuild.check();
      SimpleOutlineEdges.Face[] var2 = this.faces.toArray(SimpleOutlineEdges.Face[]::new);
      this.faces.clear();
      SimpleOutlineEdges.Node var3 = var2.length == 0 ? null : new SimpleOutlineEdges.Node(var2, 0, var2.length);

      for (SimpleOutlineEdges.Line var5 : this.lines.values()) {
         ArrayList var6 = new ArrayList();

         for (SimpleOutlineEdges.Plane var8 : var5.planes) {
            surfaceBoundary(var8.events, (var1x, var3x) -> {
               var6.add(new SimpleOutlineEdges.Event(var1x, 1));
               var6.add(new SimpleOutlineEdges.Event(var3x, -1));
            });
         }

         var6.sort(Comparator.comparingLong(SimpleOutlineEdges.Event::at));
         ArrayList var12 = new ArrayList(var6);
         if (this.solids != null && var5.planes.size() > 1) {
            for (SimpleOutlineEdges.Plane var9 : var5.planes) {
               for (SimpleOutlineEdges.SideEvent var11 : var9.events) {
                  var12.add(new SimpleOutlineEdges.Event(var11.at, var11.positive + var11.negative));
               }
            }
         }

         sweep(var12, (var6x, var8x) -> this.exposedSpans(var5, var6x, var8x, var3, var2, var6, var1));
         var5.planes.clear();
         HeldPreviewBuild.check();
      }

      this.lines.clear();
      this.faces.clear();
   }

   private void exposedSpans(
      SimpleOutlineEdges.Line var1,
      long var2,
      long var4,
      SimpleOutlineEdges.Node var6,
      SimpleOutlineEdges.Face[] var7,
      List<SimpleOutlineEdges.Event> var8,
      Consumer<SimpleOutlineEdges.Segment> var9
   ) {
      HeldPreviewBuild.check();
      SimpleOutlineEdges.Point var10 = var1.origin.add(var1.direction.scale(var2 / 100000.0));
      SimpleOutlineEdges.Point var11 = var1.origin.add(var1.direction.scale(var4 / 100000.0));
      double[] var12 = new double[]{
         Math.min(var10.x, var11.x),
         Math.min(var10.y, var11.y),
         Math.min(var10.z, var11.z),
         Math.max(var10.x, var11.x),
         Math.max(var10.y, var11.y),
         Math.max(var10.z, var11.z)
      };
      ArrayList var13 = this.faces;
      var13.clear();
      var6.query(var7, var12, var10, var11, var13);
      ArrayList var14 = new ArrayList();
      double var15 = (var4 - var2) / 100000.0;
      ArrayList var17 = new ArrayList();
      var17.add(0.0);
      var17.add(var15);

      for (SimpleOutlineEdges.Event var19 : var8) {
         if (var19.at > var2 && var19.at < var4) {
            var17.add((var19.at - var2) / 100000.0);
         }

         HeldPreviewBuild.check();
      }

      for (SimpleOutlineEdges.Face var42 : (Iterable<SimpleOutlineEdges.Face>)(Iterable<?>)(var13)) {
         SimpleOutlineEdges.SurfacePlane var20 = null;

         for (SimpleOutlineEdges.SurfacePlane var22 : (Iterable<SimpleOutlineEdges.SurfacePlane>)(Iterable<?>)(var14)) {
            if (1.0 - Math.abs(var22.normal.dot(var42.normal)) <= 1.0E-8) {
               var20 = var22;
               break;
            }
         }

         if (var20 == null) {
            var20 = new SimpleOutlineEdges.SurfacePlane(var42.normal);
            var14.add(var20);
         }

         var20.faces.add(var42);
         SimpleOutlineEdges.Point var44 = var42.normal.cross(var1.direction);

         for (int var45 = 0; var45 < var42.points.length; var45++) {
            SimpleOutlineEdges.Point var23 = var42.points[var45].sub(var10);
            SimpleOutlineEdges.Point var24 = var42.points[(var45 + 1) % var42.points.length].sub(var10);
            double var25 = var23.dot(var1.direction);
            double var27 = var24.dot(var1.direction);
            double var29 = var23.dot(var44);
            double var31 = var24.dot(var44);
            if (Math.abs(var29) <= 2.0E-5 && Math.abs(var31) <= 2.0E-5) {
               if (var25 > 0.0 && var25 < var15) {
                  var17.add(var25);
               }

               if (var27 > 0.0 && var27 < var15) {
                  var17.add(var27);
               }
            } else if (var29 <= 0.0 && var31 >= 0.0 || var29 >= 0.0 && var31 <= 0.0) {
               double var33 = var25 + (var27 - var25) * var29 / (var29 - var31);
               if (var33 > 0.0 && var33 < var15) {
                  var17.add(var33);
               }
            }
         }

         HeldPreviewBuild.check();
      }

      var17.sort((a, b) -> Double.compare((Double)a, (Double)b));
      double var41 = -1.0;
      double var43 = 0.0;
      int var46 = 0;
      int var47 = 0;
      int var48 = 1;

      while (var48 < var17.size()) {
         double var49 = (Double)var17.get(var48 - 1);
         double var50 = (Double)var17.get(var48);
         if (!(var50 - var49 < 1.0E-5)) {
            SimpleOutlineEdges.Point var51 = var10.add(var1.direction.scale((var49 + var50) * 0.5));
            boolean var30 = false;

            label130:
            for (SimpleOutlineEdges.SurfacePlane var32 : (Iterable<SimpleOutlineEdges.SurfacePlane>)(Iterable<?>)(var14)) {
               SimpleOutlineEdges.Point var54 = var32.normal.cross(var1.direction).scale(2.0E-5);
               SimpleOutlineEdges.Point var34 = var51.add(var54);
               SimpleOutlineEdges.Point var35 = var51.sub(var54);
               boolean var36 = false;
               boolean var37 = false;
               Iterator var38 = var32.faces.iterator();

               while (true) {
                  if (var38.hasNext()) {
                     SimpleOutlineEdges.Face var39 = (SimpleOutlineEdges.Face)var38.next();
                     var36 |= contains(var39, var34);
                     var37 |= contains(var39, var35);
                     if (!var36 || !var37) {
                        continue;
                     }

                     var30 = true;
                  }

                  if (var30) {
                     break label130;
                  }
                  break;
               }
            }

            long var53 = var2 + Math.round((var49 + var50) * 0.5 * 100000.0);

            while (var47 < var8.size() && ((SimpleOutlineEdges.Event)var8.get(var47)).at <= var53) {
               var46 += ((SimpleOutlineEdges.Event)var8.get(var47++)).delta;
            }

            if (var46 == 0) {
               var30 = true;
            }

            if (this.solids != null) {
               var30 = var30 ? !this.solids.innerCorner(var51, var1.direction) : !this.solids.corner(var51, var1.direction);
            }

            if (!var30) {
               if (var41 < 0.0) {
                  var41 = var49;
               }

               var43 = var50;
            } else if (var41 >= 0.0) {
               var9.accept(new SimpleOutlineEdges.Segment(var10.add(var1.direction.scale(var41)), var10.add(var1.direction.scale(var43))));
               var41 = -1.0;
            }
         }

         var48++;
         HeldPreviewBuild.check();
      }

      if (var41 >= 0.0) {
         var9.accept(new SimpleOutlineEdges.Segment(var10.add(var1.direction.scale(var41)), var10.add(var1.direction.scale(var43))));
      }
   }

   private static boolean contains(SimpleOutlineEdges.Face var0, SimpleOutlineEdges.Point var1) {
      int var2 = Math.abs(var0.normal.x) >= Math.abs(var0.normal.y) && Math.abs(var0.normal.x) >= Math.abs(var0.normal.z)
         ? 0
         : (Math.abs(var0.normal.y) >= Math.abs(var0.normal.z) ? 1 : 2);
      double var3 = var2 == 0 ? var1.y : var1.x;
      double var5 = var2 == 2 ? var1.y : var1.z;
      boolean var7 = false;
      int var8 = 0;

      for (int var9 = var0.points.length - 1; var8 < var0.points.length; var9 = var8++) {
         SimpleOutlineEdges.Point var10 = var0.points[var8];
         SimpleOutlineEdges.Point var11 = var0.points[var9];
         double var12 = var2 == 0 ? var10.y : var10.x;
         double var14 = var2 == 2 ? var10.y : var10.z;
         double var16 = var2 == 0 ? var11.y : var11.x;
         double var18 = var2 == 2 ? var11.y : var11.z;
         if (var14 > var5 != var18 > var5 && var3 < (var16 - var12) * (var5 - var14) / (var18 - var14) + var12) {
            var7 = !var7;
         }
      }

      return var7;
   }

   private static void surfaceBoundary(ArrayList<SimpleOutlineEdges.SideEvent> var0, SimpleOutlineEdges.SpanConsumer var1) {
      HeldPreviewBuild.check();
      var0.sort(Comparator.comparingLong(SimpleOutlineEdges.SideEvent::at));
      int var2 = 0;
      int var3 = 0;
      long var4 = 0L;

      for (int var6 = 0; var6 < var0.size(); HeldPreviewBuild.check()) {
         long var7 = ((SimpleOutlineEdges.SideEvent)var0.get(var6)).at;
         int var9 = 0;
         int var10 = 0;

         do {
            SimpleOutlineEdges.SideEvent var11 = (SimpleOutlineEdges.SideEvent)var0.get(var6++);
            var9 += var11.positive;
            var10 += var11.negative;
         } while (var6 < var0.size() && ((SimpleOutlineEdges.SideEvent)var0.get(var6)).at == var7);

         boolean var13 = var2 > 0 != var3 > 0;
         var2 += var9;
         var3 += var10;
         boolean var12 = var2 > 0 != var3 > 0;
         if (!var13 && var12) {
            var4 = var7;
         } else if (var13 && !var12 && var7 > var4) {
            var1.accept(var4, var7);
         }
      }
   }

   private static void sweep(ArrayList<SimpleOutlineEdges.Event> var0, SimpleOutlineEdges.SpanConsumer var1) {
      HeldPreviewBuild.check();
      var0.sort(Comparator.comparingLong(SimpleOutlineEdges.Event::at));
      int var2 = 0;
      long var3 = 0L;
      int var5 = 0;

      while (var5 < var0.size()) {
         long var6 = ((SimpleOutlineEdges.Event)var0.get(var5)).at;
         int var8 = 0;

         do {
            var8 += ((SimpleOutlineEdges.Event)var0.get(var5++)).delta;
         } while (var5 < var0.size() && ((SimpleOutlineEdges.Event)var0.get(var5)).at == var6);

         int var9 = var2 + var8;
         if (var2 == 0 && var9 != 0) {
            var3 = var6;
         } else if (var2 != 0 && var9 == 0 && var6 > var3) {
            var1.accept(var3, var6);
         }

         var2 = var9;
         HeldPreviewBuild.check();
      }
   }

   private record Event(long at, int delta) {
   }

   private static final class Face {
      final SimpleOutlineEdges.Point[] points;
      final SimpleOutlineEdges.Point normal;
      final double[] bounds = new double[]{
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      };

      Face(SimpleOutlineEdges.Point[] var1, SimpleOutlineEdges.Point var2) {
         this.points = var1;
         this.normal = var2;

         for (SimpleOutlineEdges.Point var6 : var1) {
            this.bounds[0] = Math.min(this.bounds[0], var6.x);
            this.bounds[1] = Math.min(this.bounds[1], var6.y);
            this.bounds[2] = Math.min(this.bounds[2], var6.z);
            this.bounds[3] = Math.max(this.bounds[3], var6.x);
            this.bounds[4] = Math.max(this.bounds[4], var6.y);
            this.bounds[5] = Math.max(this.bounds[5], var6.z);
         }
      }
   }

   private static final class Line {
      final SimpleOutlineEdges.Point direction;
      final SimpleOutlineEdges.Point origin;
      final ArrayList<SimpleOutlineEdges.Plane> planes = new ArrayList<>();

      Line(SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2) {
         this.direction = var1;
         this.origin = var2;
      }

      SimpleOutlineEdges.Plane plane(SimpleOutlineEdges.Point var1) {
         for (SimpleOutlineEdges.Plane var3 : this.planes) {
            if (1.0 - Math.abs(var3.normal.dot(var1)) <= 1.0E-8) {
               return var3;
            }
         }

         SimpleOutlineEdges.Plane var4 = new SimpleOutlineEdges.Plane(var1);
         this.planes.add(var4);
         return var4;
      }
   }

   private record LineKey(long dx, long dy, long dz, long x, long y, long z) {
   }

   private static final class Node {
      final double[] bounds;
      final int from;
      final int to;
      SimpleOutlineEdges.Node left;
      SimpleOutlineEdges.Node right;

      Node(SimpleOutlineEdges.Face[] var1, int var2, int var3) {
         if (var2 == 0 && var3 == var1.length) {
            this.from = var2;
            this.to = var3;
            double[][] var13 = new double[var3][];
            int[] var14 = new int[var3];
            double[] var17 = new double[var3];

            for (int var20 = 0; var20 < var3; var20++) {
               SimpleOutlineEdges.Face var25 = var1[var20];
               var13[var20] = var25.bounds;
               SimpleOutlineEdges.Point var28 = var25.normal;
               int var30 = var28.y == 0.0 && var28.z == 0.0 ? 0 : (var28.x == 0.0 && var28.z == 0.0 ? 1 : (var28.x == 0.0 && var28.y == 0.0 ? 2 : 3));
               if (var30 < 3 && var25.bounds[var30] != var25.bounds[var30 + 3]) {
                  var30 = 3;
               }

               var14[var20] = var30;
               var17[var20] = var30 < 3 ? var25.bounds[var30] : 0.0;
            }

            OutlinePlaneIndex.Built var21 = OutlinePlaneIndex.build(var13, var14, var17);
            this.bounds = var21.tree();
            SimpleOutlineEdges.Face[] var26 = (SimpleOutlineEdges.Face[])var1.clone();

            for (int var29 = 0; var29 < var3; var29++) {
               var1[var29] = var26[var21.order()[var29]];
            }
         } else {
            this.from = var2;
            this.to = var3;
            double[] var4 = new double[6];
            double[] var5 = new double[6];
            Arrays.fill(var4, 0, 3, Double.POSITIVE_INFINITY);
            Arrays.fill(var4, 3, 6, Double.NEGATIVE_INFINITY);
            Arrays.fill(var5, 0, 3, Double.POSITIVE_INFINITY);
            Arrays.fill(var5, 3, 6, Double.NEGATIVE_INFINITY);

            for (int var6 = var2; var6 < var3; var6++) {
               for (int var7 = 0; var7 < 3; var7++) {
                  var4[var7] = Math.min(var4[var7], var1[var6].bounds[var7]);
                  var4[var7 + 3] = Math.max(var4[var7 + 3], var1[var6].bounds[var7 + 3]);
                  double var8 = var1[var6].bounds[var7] + var1[var6].bounds[var7 + 3];
                  var5[var7] = Math.min(var5[var7], var8);
                  var5[var7 + 3] = Math.max(var5[var7 + 3], var8);
               }
            }

            if (var3 - var2 > 12) {
               int var15 = OutlineSpatialPartition.splitAxis(var5);
               int var18 = var15;
               int var22 = (var2 + var3) / 2;
               OutlineSpatialPartition.partition(var1, var2, var3, var22, var1x -> var1x.bounds[var18] + var1x.bounds[var18 + 3]);
               this.left = new SimpleOutlineEdges.Node(var1, var2, var22);
               this.right = new SimpleOutlineEdges.Node(var1, var22, var3);
            }

            if (var2 == 0 && var3 == var1.length) {
               ArrayList var16 = new ArrayList();
               ArrayDeque var19 = new ArrayDeque();
               var19.push(this);

               while (!var19.isEmpty()) {
                  SimpleOutlineEdges.Node var23 = (SimpleOutlineEdges.Node)var19.pop();
                  var16.add(var23);
                  if (var23.left != null) {
                     var19.push(var23.right);
                     var19.push(var23.left);
                  }
               }

               int[] var24 = new int[var16.size()];

               for (int var9 = var16.size() - 1; var9 >= 0; var9--) {
                  var24[var9] = ((SimpleOutlineEdges.Node)var16.get(var9)).left == null ? var9 + 1 : var24[var24[var9 + 1]];
               }

               double[] var27 = new double[var16.size() * 9];

               for (int var10 = 0; var10 < var16.size(); var10++) {
                  SimpleOutlineEdges.Node var11 = (SimpleOutlineEdges.Node)var16.get(var10);
                  int var12 = var10 * 9;
                  System.arraycopy(var11 == this ? var4 : var11.bounds, 0, var27, var12, 6);
                  var27[var12 + 6] = var11.from;
                  var27[var12 + 7] = var11.to;
                  var27[var12 + 8] = var24[var10];
               }

               this.bounds = var27;
               this.left = this.right = null;
            } else {
               this.bounds = var4;
            }
         }
      }

      void query(
         SimpleOutlineEdges.Face[] var1, double[] var2, SimpleOutlineEdges.Point var3, SimpleOutlineEdges.Point var4, List<SimpleOutlineEdges.Face> var5
      ) {
         if (Double.isNaN(this.bounds[0])) {
            int[] var14 = OutlinePlaneIndex.query(this.bounds, var2, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z);

            for (int var16 = 0; var16 < var14.length; var16 += 2) {
               for (int var17 = var14[var16]; var17 < var14[var16 + 1]; var17++) {
                  SimpleOutlineEdges.Face var18 = var1[var17];
                  if (SimpleOutlineEdges.overlaps(var18.bounds, var2)
                     && OutlineSpatialPartition.intersects(var18.bounds, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z)
                     && Math.abs(
                           (var3.x - var18.points[0].x) * var18.normal.x
                              + (var3.y - var18.points[0].y) * var18.normal.y
                              + (var3.z - var18.points[0].z) * var18.normal.z
                        )
                        <= 2.0E-5
                     && Math.abs(
                           (var4.x - var18.points[0].x) * var18.normal.x
                              + (var4.y - var18.points[0].y) * var18.normal.y
                              + (var4.z - var18.points[0].z) * var18.normal.z
                        )
                        <= 2.0E-5) {
                     var5.add(var18);
                  }
               }
            }
         } else if (this.bounds.length <= 6) {
            if (SimpleOutlineEdges.overlaps(this.bounds, var2)
               && OutlineSpatialPartition.intersects(this.bounds, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z)) {
               if (this.left != null) {
                  this.left.query(var1, var2, var3, var4, var5);
                  this.right.query(var1, var2, var3, var4, var5);
               } else {
                  for (int var13 = this.from; var13 < this.to; var13++) {
                     SimpleOutlineEdges.Face var15 = var1[var13];
                     if (SimpleOutlineEdges.overlaps(var15.bounds, var2)
                        && OutlineSpatialPartition.intersects(var15.bounds, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z)
                        && Math.abs(
                              (var3.x - var15.points[0].x) * var15.normal.x
                                 + (var3.y - var15.points[0].y) * var15.normal.y
                                 + (var3.z - var15.points[0].z) * var15.normal.z
                           )
                           <= 2.0E-5
                        && Math.abs(
                              (var4.x - var15.points[0].x) * var15.normal.x
                                 + (var4.y - var15.points[0].y) * var15.normal.y
                                 + (var4.z - var15.points[0].z) * var15.normal.z
                           )
                           <= 2.0E-5) {
                        var5.add(var15);
                     }
                  }
               }
            }
         } else {
            int var6 = this.bounds.length / 9;
            int var7 = 0;

            while (var7 < var6) {
               int var8 = var7 * 9;
               int var9 = (int)this.bounds[var8 + 8];
               if (OutlineFlatIndex.overlaps(this.bounds, var8, var2)
                  && OutlineFlatIndex.intersects(this.bounds, var8, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z)) {
                  if (var9 == var7 + 1) {
                     int var10 = (int)this.bounds[var8 + 6];

                     for (int var11 = (int)this.bounds[var8 + 7]; var10 < var11; var10++) {
                        SimpleOutlineEdges.Face var12 = var1[var10];
                        if (SimpleOutlineEdges.overlaps(var12.bounds, var2)
                           && OutlineSpatialPartition.intersects(var12.bounds, var3.x, var3.y, var3.z, var4.x, var4.y, var4.z)
                           && Math.abs(
                                 (var3.x - var12.points[0].x) * var12.normal.x
                                    + (var3.y - var12.points[0].y) * var12.normal.y
                                    + (var3.z - var12.points[0].z) * var12.normal.z
                              )
                              <= 2.0E-5
                           && Math.abs(
                                 (var4.x - var12.points[0].x) * var12.normal.x
                                    + (var4.y - var12.points[0].y) * var12.normal.y
                                    + (var4.z - var12.points[0].z) * var12.normal.z
                              )
                              <= 2.0E-5) {
                           var5.add(var12);
                        }
                     }
                  }

                  var7++;
               } else {
                  var7 = var9;
               }
            }
         }
      }
   }

   private static final class Plane {
      final SimpleOutlineEdges.Point normal;
      final ArrayList<SimpleOutlineEdges.SideEvent> events = new ArrayList<>();

      Plane(SimpleOutlineEdges.Point var1) {
         this.normal = var1;
      }
   }

   public record Point(double x, double y, double z) {
      SimpleOutlineEdges.Point sub(SimpleOutlineEdges.Point var1) {
         return new SimpleOutlineEdges.Point(this.x - var1.x, this.y - var1.y, this.z - var1.z);
      }

      SimpleOutlineEdges.Point scale(double var1) {
         return new SimpleOutlineEdges.Point(this.x * var1, this.y * var1, this.z * var1);
      }

      SimpleOutlineEdges.Point add(SimpleOutlineEdges.Point var1) {
         return new SimpleOutlineEdges.Point(this.x + var1.x, this.y + var1.y, this.z + var1.z);
      }

      double dot(SimpleOutlineEdges.Point var1) {
         return this.x * var1.x + this.y * var1.y + this.z * var1.z;
      }

      SimpleOutlineEdges.Point cross(SimpleOutlineEdges.Point var1) {
         return new SimpleOutlineEdges.Point(this.y * var1.z - this.z * var1.y, this.z * var1.x - this.x * var1.z, this.x * var1.y - this.y * var1.x);
      }

      double length() {
         return Math.sqrt(this.dot(this));
      }

      boolean negative() {
         return Math.abs(this.x) >= Math.abs(this.y) && Math.abs(this.x) >= Math.abs(this.z)
            ? this.x < 0.0
            : (Math.abs(this.y) >= Math.abs(this.z) ? this.y < 0.0 : this.z < 0.0);
      }
   }

   public record Segment(SimpleOutlineEdges.Point a, SimpleOutlineEdges.Point b) {
   }

   private record SideEvent(long at, int positive, int negative) {
   }

   private interface SpanConsumer {
      void accept(long var1, long var3);
   }

   private static final class SurfacePlane {
      final SimpleOutlineEdges.Point normal;
      final ArrayList<SimpleOutlineEdges.Face> faces = new ArrayList<>();

      SurfacePlane(SimpleOutlineEdges.Point var1) {
         this.normal = var1;
      }
   }
}
