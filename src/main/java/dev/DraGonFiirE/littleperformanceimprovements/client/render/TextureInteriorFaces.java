package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.BoxCorner;
import team.creative.creativecore.common.util.math.box.BoxFace;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleTransformableBox.VectorFanFaceCache;

public final class TextureInteriorFaces {
   private static final double EPS = 1.0E-7;
   private final IdentityHashMap<RenderBox, List<TextureInteriorFaces.Solid>> candidates = new IdentityHashMap<>();

   public TextureInteriorFaces(List<RenderBox> var1, List<RenderBox> var2) {
      Set var3 = Collections.newSetFromMap(new IdentityHashMap());
      var3.addAll(var2);
      ArrayList var4 = new ArrayList();

      for (RenderBox var6 : var1) {
         var4.add(new TextureInteriorFaces.Solid(var6));
      }

      if (!var4.isEmpty()) {
         TextureInteriorFaces.Node var11 = new TextureInteriorFaces.Node(var4);

         for (RenderBox var7 : var1) {
            if (!var3.contains(var7)) {
               OutlineWorkChunks.check();
               ArrayList<TextureInteriorFaces.Solid> var8 = new ArrayList<>();
               var11.query(new double[]{var7.minX, var7.minY, var7.minZ, var7.maxX, var7.maxY, var7.maxZ}, var8);
               var8.removeIf(var1x -> var1x.box == var7);

               for (TextureInteriorFaces.Solid var10 : (Iterable<TextureInteriorFaces.Solid>)(Iterable<?>)(var8)) {
                  var10.prepare();
               }

               var8.removeIf(var0 -> var0.planes.isEmpty());
               if (!var8.isEmpty()) {
                  this.candidates.put(var7, var8);
               }
            }
         }
      }
   }

   public void apply(LittleGrid var1) {
      for (Entry var3 : this.candidates.entrySet()) {
         OutlineWorkChunks.check();
         RenderBox var4 = (RenderBox)var3.getKey();

         for (Facing var8 : Facing.VALUES) {
            List var9 = polygons(var4, var8);
            List var10 = var9;

            for (TextureInteriorFaces.Solid var12 : (Iterable<TextureInteriorFaces.Solid>)(Iterable<?>)((List)var3.getValue())) {
               ArrayList var13 = new ArrayList();

               for (List var15 : (Iterable<List>)(Iterable<?>)(var10)) {
                  var13.addAll(subtract(var15, var12));
               }

               var10 = var13;
               if (var10.isEmpty()) {
                  break;
               }
            }

            if (var10.size() == var9.size()) {
               boolean var17 = true;

               for (int var19 = 0; var19 < var10.size(); var19++) {
                  if (var10.get(var19) != var9.get(var19)) {
                     var17 = false;
                     break;
                  }
               }

               if (var17) {
                  continue;
               }
            }

            ArrayList var18 = new ArrayList();

            for (List var22 : (Iterable<List>)(Iterable<?>)(var10)) {
               Vec3f[] var24 = new Vec3f[var22.size()];

               for (int var25 = 0; var25 < var24.length; var25++) {
                  TextureInteriorFaces.P var16 = (TextureInteriorFaces.P)var22.get(var25);
                  var24[var25] = new Vec3f(
                     (float)((var16.x - var4.getPreviewOffX()) / var4.getPreviewScaleX()),
                     (float)((var16.y - var4.getPreviewOffY()) / var4.getPreviewScaleY()),
                     (float)((var16.z - var4.getPreviewOffZ()) / var4.getPreviewScaleZ())
                  );
               }

               var18.add(new VectorFan(var24));
            }

            RenderBoxFace var21 = var4.getFace(var8);
            float var23 = var21.hasCachedFans() ? var21.getScale() : (var4 instanceof LittleRenderBoxTransformable ? (float)var1.pixelLength : 1.0F);
            var4.setFace(var8, new RenderBoxFaceSpecial(var18, var23));
         }
      }
   }

   private static boolean overlaps(double[] var0, double[] var1) {
      for (int var2 = 0; var2 < 3; var2++) {
         if (var0[var2 + 3] < var1[var2] - 1.0E-7 || var1[var2 + 3] < var0[var2] - 1.0E-7) {
            return false;
         }
      }

      return true;
   }

   private static TextureInteriorFaces.P normal(List<TextureInteriorFaces.P> var0) {
      TextureInteriorFaces.P var1 = new TextureInteriorFaces.P(0.0, 0.0, 0.0);

      for (int var2 = 1; var2 + 1 < var0.size(); var2++) {
         var1 = var1.add(
            ((TextureInteriorFaces.P)var0.get(var2))
               .sub((TextureInteriorFaces.P)var0.get(0))
               .cross(((TextureInteriorFaces.P)var0.get(var2 + 1)).sub((TextureInteriorFaces.P)var0.get(0)))
         );
      }

      return var1;
   }

   private static List<List<TextureInteriorFaces.P>> subtract(List<TextureInteriorFaces.P> var0, TextureInteriorFaces.Solid var1) {
      for (TextureInteriorFaces.Plane var3 : var1.planes) {
         double var4 = Double.POSITIVE_INFINITY;
         double var6 = Double.NEGATIVE_INFINITY;

         for (TextureInteriorFaces.P var9 : var0) {
            double var10 = var3.distance(var9);
            var4 = Math.min(var4, var10);
            var6 = Math.max(var6, var10);
         }

         if (var4 >= -1.0E-7 && (var6 > 1.0E-7 || normal(var0).dot(var3.n) >= 0.0)) {
            return List.of(var0);
         }
      }

      List var12 = var0;
      ArrayList var13 = new ArrayList();

      for (TextureInteriorFaces.Plane var5 : var1.planes) {
         if (var12.size() < 3) {
            break;
         }

         boolean var15 = true;
         Iterator var7 = var12.iterator();

         while (true) {
            if (var7.hasNext()) {
               TextureInteriorFaces.P var17 = (TextureInteriorFaces.P)var7.next();
               if (!(Math.abs(var5.distance(var17)) > 1.0E-7)) {
                  continue;
               }

               var15 = false;
            }

            if (!var15) {
               List var16 = clip(var12, var5, false);
               if (var16.size() >= 3 && normal(var16).dot(normal(var16)) > 1.0E-20) {
                  var13.add(var16);
               }

               var12 = clip(var12, var5, true);
            }
            break;
         }
      }

      return var13;
   }

   private static List<TextureInteriorFaces.P> clip(List<TextureInteriorFaces.P> var0, TextureInteriorFaces.Plane var1, boolean var2) {
      ArrayList var3 = new ArrayList();
      if (var0.isEmpty()) {
         return var3;
      }

      TextureInteriorFaces.P var4 = (TextureInteriorFaces.P)var0.getLast();
      double var5 = var1.distance(var4);
      boolean var7 = var2 ? var5 <= 0.0 : var5 >= 0.0;

      for (TextureInteriorFaces.P var9 : var0) {
         double var10 = var1.distance(var9);
         boolean var12 = var2 ? var10 <= 0.0 : var10 >= 0.0;
         if (var7 != var12) {
            TextureInteriorFaces.P var13 = var4.add(var9.sub(var4).mul(var5 / (var5 - var10)));
            append(var3, var13);
         }

         if (var12) {
            append(var3, var9);
         }

         var4 = var9;
         var5 = var10;
         var7 = var12;
      }

      if (var3.size() > 1 && distance((TextureInteriorFaces.P)var3.getFirst(), (TextureInteriorFaces.P)var3.getLast()) < 1.0E-20) {
         var3.removeLast();
      }

      return var3;
   }

   private static double distance(TextureInteriorFaces.P var0, TextureInteriorFaces.P var1) {
      TextureInteriorFaces.P var2 = var0.sub(var1);
      return var2.dot(var2);
   }

   private static void append(List<TextureInteriorFaces.P> var0, TextureInteriorFaces.P var1) {
      if (var0.isEmpty() || distance((TextureInteriorFaces.P)var0.getLast(), var1) > 1.0E-20) {
         var0.add(var1);
      }
   }

   private static List<List<TextureInteriorFaces.P>> polygons(RenderBox var0, Facing var1) {
      RenderBoxFace var2 = var0.getFace(var1);
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      if (var2.hasCachedFans()) {
         var3.addAll(var2.getCachedFans());
      } else if (var0 instanceof LittleRenderBoxTransformable var5) {
         VectorFanFaceCache var6 = var5.getFaceCache(var1);
         if (var6 != null) {
            if (var2.shouldRender() && var6.hasAxisStrip()) {
               var3.addAll(var6.axisStrips);
            }

            if (var6.hasTiltedStripsRendering()) {
               var6.collectAllTiltedStripsRendering(var3);
            }
         }
      } else if (var2.shouldRender()) {
         ArrayList var12 = new ArrayList();

         for (BoxCorner var10 : BoxFace.get(var1).getCorners()) {
            var12.add(
               new TextureInteriorFaces.P(
                  var10.x.positive ? var0.maxX : var0.minX, var10.y.positive ? var0.maxY : var0.minY, var10.z.positive ? var0.maxZ : var0.minZ
               )
            );
         }

         var4.add(var12);
      }

      for (VectorFan var13 : (Iterable<VectorFan>)(Iterable<?>)(var3)) {
         ArrayList var14 = new ArrayList();

         for (int var15 = 0; var15 < var13.count(); var15++) {
            Vec3f var16 = var13.get(var15);
            var14.add(
               new TextureInteriorFaces.P(
                  var0.getPreviewOffX() + var16.x * var0.getPreviewScaleX(),
                  var0.getPreviewOffY() + var16.y * var0.getPreviewScaleY(),
                  var0.getPreviewOffZ() + var16.z * var0.getPreviewScaleZ()
               )
            );
         }

         if (var14.size() >= 3) {
            var4.add(var14);
         }
      }

      return var4;
   }

   private static final class Node {
      double[] b = new double[]{
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      };
      TextureInteriorFaces.Node left;
      TextureInteriorFaces.Node right;
      List<TextureInteriorFaces.Solid> leaf;

      Node(List<TextureInteriorFaces.Solid> var1) {
         for (TextureInteriorFaces.Solid var3 : var1) {
            for (int var4 = 0; var4 < 3; var4++) {
               this.b[var4] = Math.min(this.b[var4], var3.b[var4]);
               this.b[var4 + 3] = Math.max(this.b[var4 + 3], var3.b[var4 + 3]);
            }
         }

         if (var1.size() <= 8) {
            this.leaf = var1;
         } else {
            int var5 = 0;

            for (int var6 = 1; var6 < 3; var6++) {
               if (this.b[var6 + 3] - this.b[var6] > this.b[var5 + 3] - this.b[var5]) {
                  var5 = var6;
               }
            }

            int var7 = var5;
            var1.sort(Comparator.comparingDouble(var1x -> var1x.b[var7] + var1x.b[var7 + 3]));
            int var8 = var1.size() / 2;
            this.left = new TextureInteriorFaces.Node(new ArrayList<>(var1.subList(0, var8)));
            this.right = new TextureInteriorFaces.Node(new ArrayList<>(var1.subList(var8, var1.size())));
         }
      }

      void query(double[] var1, List<TextureInteriorFaces.Solid> var2) {
         if (TextureInteriorFaces.overlaps(this.b, var1)) {
            if (this.leaf != null) {
               for (TextureInteriorFaces.Solid var4 : this.leaf) {
                  if (TextureInteriorFaces.overlaps(var4.b, var1)) {
                     var2.add(var4);
                  }
               }
            } else {
               this.left.query(var1, var2);
               this.right.query(var1, var2);
            }
         }
      }
   }

   private record P(double x, double y, double z) {
      TextureInteriorFaces.P add(TextureInteriorFaces.P var1) {
         return new TextureInteriorFaces.P(this.x + var1.x, this.y + var1.y, this.z + var1.z);
      }

      TextureInteriorFaces.P sub(TextureInteriorFaces.P var1) {
         return new TextureInteriorFaces.P(this.x - var1.x, this.y - var1.y, this.z - var1.z);
      }

      TextureInteriorFaces.P mul(double var1) {
         return new TextureInteriorFaces.P(this.x * var1, this.y * var1, this.z * var1);
      }

      double dot(TextureInteriorFaces.P var1) {
         return this.x * var1.x + this.y * var1.y + this.z * var1.z;
      }

      TextureInteriorFaces.P cross(TextureInteriorFaces.P var1) {
         return new TextureInteriorFaces.P(this.y * var1.z - this.z * var1.y, this.z * var1.x - this.x * var1.z, this.x * var1.y - this.y * var1.x);
      }

      double axis(int var1) {
         return var1 == 0 ? this.x : (var1 == 1 ? this.y : this.z);
      }
   }

   private record Plane(TextureInteriorFaces.P n, double d) {
      double distance(TextureInteriorFaces.P var1) {
         return this.n.dot(var1) - this.d;
      }
   }

   private static final class Solid {
      final RenderBox box;
      final double[] b;
      List<TextureInteriorFaces.Plane> planes;

      Solid(RenderBox var1) {
         this.box = var1;
         this.b = new double[]{var1.minX, var1.minY, var1.minZ, var1.maxX, var1.maxY, var1.maxZ};
      }

      void prepare() {
         if (this.planes == null) {
            this.planes = new ArrayList<>();
            if (!(this.box instanceof LittleRenderBoxTransformable)) {
               for (int var14 = 0; var14 < 3; var14++) {
                  TextureInteriorFaces.P var17 = new TextureInteriorFaces.P(var14 == 0 ? 1.0 : 0.0, var14 == 1 ? 1.0 : 0.0, var14 == 2 ? 1.0 : 0.0);
                  this.planes.add(new TextureInteriorFaces.Plane(var17, this.b[var14 + 3]));
                  this.planes.add(new TextureInteriorFaces.Plane(var17.mul(-1.0), -this.b[var14]));
               }
            } else {
               ArrayList var1 = new ArrayList();

               for (Facing var5 : Facing.VALUES) {
                  var1.addAll(TextureInteriorFaces.polygons(this.box, var5));
               }

               TextureInteriorFaces.P var15 = new TextureInteriorFaces.P(0.0, 0.0, 0.0);
               int var18 = 0;

               for (List var21 : (Iterable<List>)(Iterable<?>)(var1)) {
                  for (TextureInteriorFaces.P var7 : (Iterable<TextureInteriorFaces.P>)(Iterable<?>)(var21)) {
                     var15 = var15.add(var7);
                     var18++;
                  }
               }

               if (var18 != 0) {
                  var15 = var15.mul(1.0 / var18);

                  for (List var22 : (Iterable<List>)(Iterable<?>)(var1)) {
                     TextureInteriorFaces.P var23 = TextureInteriorFaces.normal(var22);
                     double var25 = Math.sqrt(var23.dot(var23));
                     if (!(var25 < 1.0E-12)) {
                        var23 = var23.mul(1.0 / var25);
                        if (var23.dot(var15.sub((TextureInteriorFaces.P)var22.get(0))) > 0.0) {
                           var23 = var23.mul(-1.0);
                        }

                        TextureInteriorFaces.Plane var9 = new TextureInteriorFaces.Plane(var23, var23.dot((TextureInteriorFaces.P)var22.get(0)));

                        for (List var11 : (Iterable<List>)(Iterable<?>)(var1)) {
                           for (TextureInteriorFaces.P var13 : (Iterable<TextureInteriorFaces.P>)(Iterable<?>)(var11)) {
                              if (var9.distance(var13) > 8.0E-7) {
                                 this.planes.clear();
                                 return;
                              }
                           }
                        }

                        boolean var26 = false;

                        for (TextureInteriorFaces.Plane var28 : this.planes) {
                           if (var28.n.dot(var9.n) > 0.9999999999 && Math.abs(var28.d - var9.d) < 1.0E-7) {
                              var26 = true;
                              break;
                           }
                        }

                        if (!var26) {
                           this.planes.add(var9);
                        }
                     }
                  }

                  if (this.planes.size() < 4) {
                     this.planes.clear();
                  }
               }
            }
         }
      }
   }
}
