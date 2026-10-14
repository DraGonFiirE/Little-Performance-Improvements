package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.level.context.ILittleLevelContext;

public final class WallPreviewDepth {
   private static final Set<MeshData> meshes = Collections.newSetFromMap(new WeakHashMap<>());

   public static synchronized void mark(MeshData var0) {
      if (var0 != null) {
         meshes.add(var0);
      }
   }

   private static synchronized boolean contains(MeshData var0) {
      return meshes.contains(var0);
   }

   public static void draw(PreviewRenderer var0, ILittleLevelContext var1, Vec3 var2, BlockPos var3, boolean var4, MeshData var5, Runnable var6) {
      if (!ChiselOutlineDepth.draw(var0, var1, var2, var3, var4, var5, var6)) {
         ChiselOutlineTint.prepare(var4, var5);
         Runnable var7 = PyramidTextureSurfaceStyle.wrap(var5, PyramidSurfaceStyle.wrap(var5, var6));
         if (!var4 && contains(var5)) {
            boolean var8 = GL11.glIsEnabled(2884);
            boolean var9 = GL11.glIsEnabled(2929);
            boolean var10 = GL11.glGetBoolean(2930);
            int var11 = GL11.glGetInteger(2932);

            try (MemoryStack var12 = MemoryStack.stackPush()) {
               ByteBuffer var13 = var12.malloc(4);
               GL11.glGetBooleanv(3107, var13);

               try {
                  var0.renderBoxes(var1, var2, var3, false, var5, () -> {
                     if (var7 != null) {
                        var7.run();
                     }

                     RenderSystem.disableCull();
                     RenderSystem.enableDepthTest();
                     RenderSystem.depthFunc(515);
                     RenderSystem.depthMask(true);
                     RenderSystem.colorMask(false, false, false, false);
                  });
                  var0.renderBoxes(var1, var2, var3, false, var5, () -> {
                     if (var7 != null) {
                        var7.run();
                     }

                     RenderSystem.disableCull();
                     RenderSystem.enableDepthTest();
                     RenderSystem.depthFunc(514);
                     RenderSystem.depthMask(false);
                     RenderSystem.colorMask(var13.get(0) != 0, var13.get(1) != 0, var13.get(2) != 0, var13.get(3) != 0);
                  });
               } finally {
                  RenderSystem.colorMask(var13.get(0) != 0, var13.get(1) != 0, var13.get(2) != 0, var13.get(3) != 0);
                  RenderSystem.depthMask(var10);
                  RenderSystem.depthFunc(var11);
                  if (var8) {
                     RenderSystem.enableCull();
                  } else {
                     RenderSystem.disableCull();
                  }

                  if (var9) {
                     RenderSystem.enableDepthTest();
                  } else {
                     RenderSystem.disableDepthTest();
                  }
               }
            }
         } else {
            var0.renderBoxes(var1, var2, var3, var4, var5, var7);
         }
      }
   }
}
