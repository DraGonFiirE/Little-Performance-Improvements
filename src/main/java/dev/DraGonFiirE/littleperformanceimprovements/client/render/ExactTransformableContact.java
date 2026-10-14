package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.util.List;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.common.math.box.LittleTransformableBox.VectorFanFaceCache;

public final class ExactTransformableContact {
   public static boolean matches(LittleRenderBoxTransformable var0, LittleRenderBoxTransformable var1, Facing var2) {
      VectorFanFaceCache var3 = var0.getFaceCache(var2);
      VectorFanFaceCache var4 = var1.getFaceCache(var2.opposite());
      return var3 != null && var4 != null && var3.hasAxisStrip() && var4.hasAxisStrip() && samePolygons(var3.axisStrips, var4.axisStrips);
   }

   public static boolean samePolygons(List<VectorFan> var0, List<VectorFan> var1) {
      if (!var0.isEmpty() && var0.size() == var1.size()) {
         boolean[] var2 = new boolean[var1.size()];

         for (VectorFan var4 : var0) {
            boolean var5 = false;

            for (int var6 = 0; var6 < var1.size(); var6++) {
               if (!var2[var6] && samePolygon(var4, (VectorFan)var1.get(var6))) {
                  var2[var6] = true;
                  var5 = true;
                  break;
               }
            }

            if (!var5) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean samePolygon(VectorFan var0, VectorFan var1) {
      int var2 = var0.count();
      if (var2 >= 3 && var1.count() == var2) {
         for (int var3 = 0; var3 < var2; var3++) {
            for (int var7 : new int[]{1, -1}) {
               boolean var8 = true;

               for (int var9 = 0; var9 < var2; var9++) {
                  Vec3f var10 = var0.get(var9);
                  Vec3f var11 = var1.get((var3 + var7 * var9 + var2) % var2);
                  if (var10.x != var11.x || var10.y != var11.y || var10.z != var11.z) {
                     var8 = false;
                     break;
                  }
               }

               if (var8) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
