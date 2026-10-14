package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ParallelOutlineSolidUnion {
   private static final double RADIUS = 1.0E-4;
   private static final double EPS = 2.0E-5;
   private static final double NORMAL_DOT_TOLERANCE = 1.0E-8;
   private static final SimpleOutlineEdges.Point RAY = new SimpleOutlineEdges.Point(1.0, 0.371390676, 0.529173921);
   private final ArrayList<ParallelOutlineSolidUnion.Solid> solids = new ArrayList<>();
   private ParallelOutlineSolidUnion.Solid[] indexed;
   private ParallelOutlineSolidUnion.Node root;

   private static boolean near(double[] var0, SimpleOutlineEdges.Point var1) {
      return var1.x() >= var0[0] - 1.0E-4
         && var1.y() >= var0[1] - 1.0E-4
         && var1.z() >= var0[2] - 1.0E-4
         && var1.x() <= var0[3] + 1.0E-4
         && var1.y() <= var0[4] + 1.0E-4
         && var1.z() <= var0[5] + 1.0E-4;
   }

   public void box(double var1, double var3, double var5, double var7, double var9, double var11) {
      this.solids.add(new ParallelOutlineSolidUnion.Solid(new double[]{var1, var3, var5, var7, var9, var11}, null));
   }

   public void mesh(List<SimpleOutlineEdges.Point[]> var1) {
      if (!var1.isEmpty()) {
         double[] var2 = new double[]{
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
         };

         for (SimpleOutlineEdges.Point[] var4 : var1) {
            for (SimpleOutlineEdges.Point var8 : var4) {
               var2[0] = Math.min(var2[0], var8.x());
               var2[1] = Math.min(var2[1], var8.y());
               var2[2] = Math.min(var2[2], var8.z());
               var2[3] = Math.max(var2[3], var8.x());
               var2[4] = Math.max(var2[4], var8.y());
               var2[5] = Math.max(var2[5], var8.z());
            }
         }

         this.solids.add(new ParallelOutlineSolidUnion.Solid(var2, var1));
      }
   }

   public void build() {
      for (ParallelOutlineSolidUnion.Solid var2 : this.solids) {
         OutlineWorkChunks.check();
         var2.preparePlanes();
      }

      this.indexed = this.solids.toArray(ParallelOutlineSolidUnion.Solid[]::new);
      this.root = this.indexed.length == 0 ? null : new ParallelOutlineSolidUnion.Node(this.indexed, 0, this.indexed.length);
      this.solids.clear();
   }

   private static void angle(
      List<Double> var0, SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2, SimpleOutlineEdges.Point var3, SimpleOutlineEdges.Point var4
   ) {
      if (!(var1.length() < 0.5)) {
         SimpleOutlineEdges.Point var5 = var1.cross(var2);
         if (!(var5.length() < 1.0E-8)) {
            double var6 = Math.atan2(var5.dot(var4), var5.dot(var3));
            if (var6 < 0.0) {
               var6 += Math.PI * 2;
            }

            var0.add(var6);
            var0.add((var6 + Math.PI) % (Math.PI * 2));
         }
      }
   }

   public boolean corner(SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2) {
      return this.corner(var1, var2, false);
   }

   public boolean innerCorner(SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2) {
      return this.corner(var1, var2, true);
   }

   private boolean corner(SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2, boolean var3) {
      if (this.root == null) {
         return false;
      }

      List var4 = OutlineWorkChunks.scratch(true);

      try {
         this.root.query(this.indexed, var1, var4);
         if (var4.isEmpty()) {
            return false;
         }

         int var5 = var2.y() == 0.0 && var2.z() == 0.0 && var2.x() != 0.0
            ? 0
            : (var2.x() == 0.0 && var2.z() == 0.0 && var2.y() != 0.0 ? 1 : (var2.x() == 0.0 && var2.y() == 0.0 && var2.z() != 0.0 ? 2 : -1));
         if (var5 >= 0) {
            boolean var6 = true;

            for (ParallelOutlineSolidUnion.Solid var8 : (Iterable<ParallelOutlineSolidUnion.Solid>)(Iterable<?>)(var4)) {
               if (var8.faces != null) {
                  var6 = false;
                  break;
               }
            }

            if (var6) {
               int var31 = (var5 + 1) % 3;
               int var33 = (var5 + 2) % 3;
               int var34 = 0;
               double var36 = var31 == 0 ? var1.x() : (var31 == 1 ? var1.y() : var1.z());
               double var40 = var33 == 0 ? var1.x() : (var33 == 1 ? var1.y() : var1.z());

               for (ParallelOutlineSolidUnion.Solid var46 : (Iterable<ParallelOutlineSolidUnion.Solid>)(Iterable<?>)(var4)) {
                  double[] var16 = var46.bounds;
                  if (!(var1.x() < var16[0] - 2.0E-5)
                     && !(var1.x() > var16[3] + 2.0E-5)
                     && !(var1.y() < var16[1] - 2.0E-5)
                     && !(var1.y() > var16[4] + 2.0E-5)
                     && !(var1.z() < var16[2] - 2.0E-5)
                     && !(var1.z() > var16[5] + 2.0E-5)) {
                     boolean var48 = Math.abs(var36 - var16[var31]) > 2.0E-5;
                     boolean var18 = Math.abs(var36 - var16[var31 + 3]) > 2.0E-5;
                     boolean var49 = Math.abs(var40 - var16[var33]) > 2.0E-5;
                     boolean var20 = Math.abs(var40 - var16[var33 + 3]) > 2.0E-5;
                     if (var48 && var49) {
                        var34 |= 1;
                     }

                     if (var18 && var49) {
                        var34 |= 2;
                     }

                     if (var18 && var20) {
                        var34 |= 4;
                     }

                     if (var48 && var20) {
                        var34 |= 8;
                     }
                  }
               }

               int var44 = Integer.bitCount(var34);
               boolean var47 = var34 == 5 || var34 == 10;
               return var3 ? var44 == 3 || var47 : var44 == 1 || var44 == 3 || var47;
            }
         }

         SimpleOutlineEdges.Point var28 = Math.abs(var2.x()) < 0.8 ? new SimpleOutlineEdges.Point(1.0, 0.0, 0.0) : new SimpleOutlineEdges.Point(0.0, 1.0, 0.0);
         SimpleOutlineEdges.Point var29 = var2.cross(var28);
         var29 = var29.scale(1.0 / var29.length());
         SimpleOutlineEdges.Point var32 = var2.cross(var29);
         ArrayList var9 = new ArrayList();
         var9.add(0.0);
         var9.add(Math.PI * 2);

         for (ParallelOutlineSolidUnion.Solid var11 : (Iterable<ParallelOutlineSolidUnion.Solid>)(Iterable<?>)(var4)) {
            if (var11.faces == null) {
               double[] var38 = new double[]{var1.x(), var1.y(), var1.z()};
               SimpleOutlineEdges.Point[] var13 = new SimpleOutlineEdges.Point[]{
                  new SimpleOutlineEdges.Point(1.0, 0.0, 0.0), new SimpleOutlineEdges.Point(0.0, 1.0, 0.0), new SimpleOutlineEdges.Point(0.0, 0.0, 1.0)
               };

               for (int var14 = 0; var14 < 3; var14++) {
                  if (Math.abs(var38[var14] - var11.bounds[var14]) <= 2.0E-5 || Math.abs(var38[var14] - var11.bounds[var14 + 3]) <= 2.0E-5) {
                     angle(var9, var13[var14], var2, var29, var32);
                  }
               }
            } else {
               for (int var12 = 0; var12 < var11.faces.size(); var12++) {
                  if (Math.abs(var1.sub(var11.faces.get(var12)[0]).dot(var11.normals.get(var12))) <= 2.0E-5) {
                     angle(var9, var11.normals.get(var12), var2, var29, var32);
                  }
               }
            }
         }

         var9.sort((a, b) -> Double.compare((Double)a, (Double)b));
         ArrayList var35 = new ArrayList();
         ArrayList var37 = new ArrayList();
         double var39 = 0.0;

         for (int var41 = 1; var41 < var9.size(); var41++) {
            double var15 = (Double)var9.get(var41 - 1);
            double var17 = (Double)var9.get(var41);
            if (!(1.0 - Math.cos(var17 - var15) <= 1.0E-8)) {
               double var19 = (var15 + var17) / 2.0;
               SimpleOutlineEdges.Point var21 = var29.scale(Math.cos(var19)).add(var32.scale(Math.sin(var19)));
               boolean var22 = false;

               for (ParallelOutlineSolidUnion.Solid var24 : (Iterable<ParallelOutlineSolidUnion.Solid>)(Iterable<?>)(var4)) {
                  if (var24.localContains(var1, var21)) {
                     var22 = true;
                     break;
                  }
               }

               var35.add(var15);
               var37.add(var22);
               if (var22) {
                  var39 += var17 - var15;
               }
            }
         }

         ArrayList var42 = new ArrayList();

         for (int var45 = 0; var45 < var37.size(); var45++) {
            if (!((Boolean)var37.get(var45)).equals(var37.get((var45 + var37.size() - 1) % var37.size()))) {
               var42.add((Double)var35.get(var45));
            }
         }

         if (var3) {
            return var42.size() >= 4 || var42.size() == 2 && var39 > 3.142592653589793 && var39 < 6.282185307179586;
         } else {
            return var42.size() != 2 ? var42.size() > 2 : 1.0 - Math.cos((Double)var42.get(1) - (Double)var42.get(0) - Math.PI) > 1.0E-8;
         }
      } finally {
         var4.clear();
      }
   }

   private static final class Node {
      final double[] b;
      final int from;
      final int to;
      ParallelOutlineSolidUnion.Node left;
      ParallelOutlineSolidUnion.Node right;

      Node(ParallelOutlineSolidUnion.Solid[] var1, int var2, int var3) {
         OutlineWorkChunks.check();
         this.from = var2;
         this.to = var3;
         double[] var4 = new double[]{
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
         };
         double[] var5 = new double[]{
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
         };

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
            int var13 = OutlineSpatialPartition.splitAxis(var5);
            int var15 = var13;
            int var17 = (var2 + var3) / 2;
            OutlineSpatialPartition.partition(var1, var2, var3, var17, var1x -> var1x.bounds[var15] + var1x.bounds[var15 + 3]);
            this.left = new ParallelOutlineSolidUnion.Node(var1, var2, var17);
            this.right = new ParallelOutlineSolidUnion.Node(var1, var17, var3);
         }

         if (var2 == 0 && var3 == var1.length) {
            ArrayList var14 = new ArrayList();
            ArrayDeque var16 = new ArrayDeque();
            var16.push(this);

            while (!var16.isEmpty()) {
               ParallelOutlineSolidUnion.Node var18 = (ParallelOutlineSolidUnion.Node)var16.pop();
               var14.add(var18);
               if (var18.left != null) {
                  var16.push(var18.right);
                  var16.push(var18.left);
               }
            }

            int[] var19 = new int[var14.size()];

            for (int var9 = var14.size() - 1; var9 >= 0; var9--) {
               var19[var9] = ((ParallelOutlineSolidUnion.Node)var14.get(var9)).left == null ? var9 + 1 : var19[var19[var9 + 1]];
            }

            double[] var20 = new double[var14.size() * 9];

            for (int var10 = 0; var10 < var14.size(); var10++) {
               ParallelOutlineSolidUnion.Node var11 = (ParallelOutlineSolidUnion.Node)var14.get(var10);
               int var12 = var10 * 9;
               System.arraycopy(var11 == this ? var4 : var11.b, 0, var20, var12, 6);
               var20[var12 + 6] = var11.from;
               var20[var12 + 7] = var11.to;
               var20[var12 + 8] = var19[var10];
            }

            this.b = var20;
            this.left = this.right = null;
         } else {
            this.b = var4;
         }
      }

      void query(ParallelOutlineSolidUnion.Solid[] var1, SimpleOutlineEdges.Point var2, List<ParallelOutlineSolidUnion.Solid> var3) {
         if (this.b.length > 6) {
            int var10 = this.b.length / 9;
            int var5 = 0;

            while (var5 < var10) {
               int var6 = var5 * 9;
               int var7 = (int)this.b[var6 + 8];
               if (!OutlineFlatIndex.near(this.b, var6, var2.x(), var2.y(), var2.z())) {
                  var5 = var7;
               } else {
                  if (var7 == var5 + 1) {
                     int var8 = (int)this.b[var6 + 6];

                     for (int var9 = (int)this.b[var6 + 7]; var8 < var9; var8++) {
                        if (ParallelOutlineSolidUnion.near(var1[var8].bounds, var2)) {
                           var3.add(var1[var8]);
                        }
                     }
                  }

                  var5++;
               }
            }
         } else if (ParallelOutlineSolidUnion.near(this.b, var2)) {
            if (this.left != null) {
               this.left.query(var1, var2, var3);
               this.right.query(var1, var2, var3);
            } else {
               for (int var4 = this.from; var4 < this.to; var4++) {
                  if (ParallelOutlineSolidUnion.near(var1[var4].bounds, var2)) {
                     var3.add(var1[var4]);
                  }
               }
            }
         }
      }
   }

   private static final class Solid {
      final double[] bounds;
      final List<SimpleOutlineEdges.Point[]> faces;
      final List<SimpleOutlineEdges.Point> normals;
      List<SimpleOutlineEdges.Point> outward;

      Solid(double[] var1, List<SimpleOutlineEdges.Point[]> var2) {
         this.bounds = var1;
         this.faces = var2;
         this.normals = new ArrayList<>();
         if (var2 != null) {
            for (SimpleOutlineEdges.Point[] var4 : var2) {
               SimpleOutlineEdges.Point var5 = new SimpleOutlineEdges.Point(0.0, 0.0, 0.0);

               for (int var6 = 1; var6 + 1 < var4.length; var6++) {
                  var5 = var5.add(var4[var6].sub(var4[0]).cross(var4[var6 + 1].sub(var4[0])));
               }

               double var8 = var5.length();
               this.normals.add(var8 > 1.0E-12 ? var5.scale(1.0 / var8) : new SimpleOutlineEdges.Point(0.0, 0.0, 0.0));
            }
         }
      }

      void preparePlanes() {
         if (this.faces != null) {
            SimpleOutlineEdges.Point var1 = new SimpleOutlineEdges.Point(0.0, 0.0, 0.0);
            int var2 = 0;

            for (SimpleOutlineEdges.Point[] var4 : this.faces) {
               for (SimpleOutlineEdges.Point var8 : var4) {
                  var1 = var1.add(var8);
                  var2++;
               }
            }

            var1 = var1.scale(1.0 / var2);
            ArrayList var14 = new ArrayList();

            for (int var15 = 0; var15 < this.faces.size(); var15++) {
               SimpleOutlineEdges.Point var16 = this.normals.get(var15);
               SimpleOutlineEdges.Point var17 = this.faces.get(var15)[0];
               if (var16.length() < 0.5) {
                  return;
               }

               if (var1.sub(var17).dot(var16) > 0.0) {
                  var16 = var16.scale(-1.0);
               }

               for (SimpleOutlineEdges.Point[] var19 : this.faces) {
                  for (SimpleOutlineEdges.Point var12 : var19) {
                     if (var12.sub(var17).dot(var16) > 2.0E-5) {
                        return;
                     }
                  }
               }

               var14.add(var16);
            }

            this.outward = var14;
         }
      }

      boolean localContains(SimpleOutlineEdges.Point var1, SimpleOutlineEdges.Point var2) {
         if (this.faces != null) {
            if (this.outward == null) {
               return this.contains(var1.add(var2.scale(1.0E-4)));
            }

            for (int var7 = 0; var7 < this.faces.size(); var7++) {
               SimpleOutlineEdges.Point var8 = this.outward.get(var7);
               double var9 = var1.sub(this.faces.get(var7)[0]).dot(var8);
               if (var9 > 2.0E-5) {
                  return false;
               }

               if (var9 >= -2.0E-5 && var8.dot(var2) > 1.0E-8) {
                  return false;
               }
            }

            return true;
         } else {
            double[] var3 = new double[]{var1.x(), var1.y(), var1.z()};
            double[] var4 = new double[]{var2.x(), var2.y(), var2.z()};

            for (int var5 = 0; var5 < 3; var5++) {
               if (var3[var5] < this.bounds[var5] - 2.0E-5 || var3[var5] > this.bounds[var5 + 3] + 2.0E-5) {
                  return false;
               }

               if (Math.abs(var3[var5] - this.bounds[var5]) <= 2.0E-5 && var4[var5] < -1.0E-8) {
                  return false;
               }

               if (Math.abs(var3[var5] - this.bounds[var5 + 3]) <= 2.0E-5 && var4[var5] > 1.0E-8) {
                  return false;
               }
            }

            return true;
         }
      }

      boolean contains(SimpleOutlineEdges.Point var1) {
         if (!(var1.x() < this.bounds[0])
            && !(var1.y() < this.bounds[1])
            && !(var1.z() < this.bounds[2])
            && !(var1.x() > this.bounds[3])
            && !(var1.y() > this.bounds[4])
            && !(var1.z() > this.bounds[5])) {
            if (this.faces == null) {
               return true;
            }

            ArrayList var2 = new ArrayList();

            for (SimpleOutlineEdges.Point[] var4 : this.faces) {
               for (int var5 = 1; var5 + 1 < var4.length; var5++) {
                  SimpleOutlineEdges.Point var6 = var4[var5].sub(var4[0]);
                  SimpleOutlineEdges.Point var7 = var4[var5 + 1].sub(var4[0]);
                  SimpleOutlineEdges.Point var8 = ParallelOutlineSolidUnion.RAY.cross(var7);
                  double var9 = var6.dot(var8);
                  if (!(Math.abs(var9) < 1.0E-12)) {
                     SimpleOutlineEdges.Point var11 = var1.sub(var4[0]);
                     double var12 = var11.dot(var8) / var9;
                     if (!(var12 < -1.0E-9) && !(var12 > 1.000000001)) {
                        SimpleOutlineEdges.Point var14 = var11.cross(var6);
                        double var15 = ParallelOutlineSolidUnion.RAY.dot(var14) / var9;
                        if (!(var15 < -1.0E-9) && !(var12 + var15 > 1.000000001)) {
                           double var17 = var7.dot(var14) / var9;
                           if (var17 > 1.0E-10) {
                              var2.add(var17);
                           }
                        }
                     }
                  }
               }
            }

            var2.sort((a, b) -> Double.compare((Double)a, (Double)b));
            int var19 = 0;
            double var20 = Double.NEGATIVE_INFINITY;

            for (double var22 : (Iterable<Double>)(Iterable<?>)(var2)) {
               if (var22 - var20 > 1.0E-8) {
                  var19++;
                  var20 = var22;
               }
            }

            return (var19 & 1) != 0;
         } else {
            return false;
         }
      }
   }
}
