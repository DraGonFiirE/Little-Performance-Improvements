package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import net.minecraft.client.Minecraft;
import static org.lwjgl.opengl.GL33C.*;

/** Retains pre-translucency depth without changing the world's depth buffer. */
public final class EditingPreviewDepth {
   private static int framebuffer, depth, width, height, format;
   private static boolean ready;
   private EditingPreviewDepth() {}

   public static void beginFrame() {
      ready = false;
      if (!EditingOutlinePreview.active()) {
         if (framebuffer != 0) glDeleteFramebuffers(framebuffer);
         if (depth != 0) glDeleteRenderbuffers(depth);
         framebuffer = depth = 0;
      }
   }

   public static void capture() {
      if (ready) return;
      var target = Minecraft.getInstance().getMainRenderTarget();
      int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
      int buffer = glGetInteger(GL_RENDERBUFFER_BINDING), texture = glGetInteger(GL_TEXTURE_BINDING_2D);
      boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
      try {
         glBindFramebuffer(GL_READ_FRAMEBUFFER, draw);
         int type = glGetFramebufferAttachmentParameteri(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
         int object = glGetFramebufferAttachmentParameteri(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
         int sourceFormat;
         if (type == GL_TEXTURE) {
            glBindTexture(GL_TEXTURE_2D, object);
            sourceFormat = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
         } else if (type == GL_RENDERBUFFER) {
            glBindRenderbuffer(GL_RENDERBUFFER, object);
            sourceFormat = glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_INTERNAL_FORMAT);
         } else return;
         if (framebuffer == 0) framebuffer = glGenFramebuffers();
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
         glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
         if (depth == 0 || width != target.width || height != target.height || format != sourceFormat) {
            if (depth != 0) glDeleteRenderbuffers(depth);
            depth = glGenRenderbuffers();
            width = target.width; height = target.height; format = sourceFormat;
            glBindRenderbuffer(GL_RENDERBUFFER, depth);
            glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
         }
         glDrawBuffer(GL_NONE);
         glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
         glReadBuffer(GL_NONE);
         glBindFramebuffer(GL_READ_FRAMEBUFFER, draw);
         if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) return;
         glDisable(GL_SCISSOR_TEST);
         glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
         ready = true;
      } finally {
         glBindTexture(GL_TEXTURE_2D, texture);
         glBindRenderbuffer(GL_RENDERBUFFER, buffer);
         glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
         if (scissor) glEnable(GL_SCISSOR_TEST);
      }
   }

   public static void bindForOverlay() {
      var target = Minecraft.getInstance().getMainRenderTarget();
      if (!ready || width != target.width || height != target.height) return;
      // Share final world color, but use the saved opaque depth for both line passes.
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
      glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getColorTextureId(), 0);
      glDrawBuffer(GL_COLOR_ATTACHMENT0);
      if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) target.bindWrite(true);
   }
}
