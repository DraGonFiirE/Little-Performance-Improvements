package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.LittleTool;

/** Give coincident filled previews priority without disabling world occlusion. */
public final class PreviewDepthPriority {
   private PreviewDepthPriority() {}

   public static void render(LittleTool tool, PreviewRenderer renderer, PoseStack pose, Vec3 camera, boolean highlight) {
      if (ToolPreviewMode.current() == PreviewRendererMode.Outline) {
         tool.render(renderer, pose, camera, highlight);
         return;
      }
      try (MemoryStack stack = MemoryStack.stackPush()) {
         var range = stack.mallocDouble(2);
         GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, range);
         double near = range.get(0), far = range.get(1);
         boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
         float factor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
         float units = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
         GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
         GL11.glPolygonOffset(-1.0F, -2.0F);
         // About three units of a 24-bit depth buffer at the far end. Unlike
         // polygon offset, this also reaches custom off-screen preview passes
         // and their gl_FragCoord-based scene-depth comparisons.
         GL11.glDepthRange(near, far - (far - near) * 0.0000002);
         try {
            tool.render(renderer, pose, camera, highlight);
         } finally {
            GL11.glDepthRange(near, far);
            GL11.glPolygonOffset(factor, units);
            if (offset) GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
         }
      }
   }
}
