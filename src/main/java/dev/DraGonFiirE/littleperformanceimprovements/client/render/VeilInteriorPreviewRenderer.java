package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class VeilInteriorPreviewRenderer {
   private static final long TEXTURE_STILL_REFRESH_NANOS = 100000000L;
   private static final long TEXTURE_MOVING_REFRESH_NANOS = 8000000L;

   private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("littleperformanceimprovements", "interior_preview");
   private static final ResourceLocation TEXTURE_SHADER = ResourceLocation.fromNamespaceAndPath("littleperformanceimprovements", "interior_texture_preview");
   private static final ResourceLocation TEXTURE_DEPTH_SHADER = ResourceLocation.fromNamespaceAndPath(
      "littleperformanceimprovements", "interior_texture_preview_depth"
   );
   private static final ResourceLocation COMPOSITE_SHADER = ResourceLocation.fromNamespaceAndPath("littleperformanceimprovements", "interior_composite");
   private static boolean reportedMissingShader;
   private static int framebuffer;
   private static int colorTexture;
   private static int depthBuffer;
   private static int sceneDepthFramebuffer;
   private static int sceneDepthTexture;
   private static int fullscreenVao;
   private static int width;
   private static int height;

   private static boolean resolvedTextureFrameValid;
   private static long lastTextureResolveNanos;
   private static VertexBuffer resolvedTextureVertexBuffer;
   private static VertexBuffer resolvedTextureInteriorVertexBuffer;
   private static TextureDrawLayout resolvedTextureDrawLayout;
   private static final float[] resolvedTextureProjection = new float[16];
   private static final float[] resolvedTextureModelView = new float[16];
   private static float resolvedTextureAlpha;
   private static float resolvedTextureNearClip;
   private static int textureSamplerProgram = -1;
   private static int textureSamplerLocation = -1;
   private static int sceneDepthProgram = -1;
   private static int sceneDepthLocation = -1;

   private VeilInteriorPreviewRenderer() {
   }

   public static boolean draw(VertexBuffer vertexBuffer, float red, float green, float blue, float alpha, float nearClipDistance, boolean deduplicatePass) {
      ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
      if (shader != null && shader.isValid()) {
         shader.setDefaultUniforms(Mode.QUADS);
         shader.getUniformSafe("NearClipDistance").setFloat(nearClipDistance);
         shader.getUniformSafe("DeduplicatePass").setInt(deduplicatePass ? 1 : 0);
         shader.getUniformSafe("ColorModulator").setVector(red, green, blue, alpha);
         shader.bind();

         try {
            vertexBuffer.bind();
            vertexBuffer.draw();
         } finally {
            VertexBuffer.unbind();
            ShaderProgram.unbind();
         }

         return true;
      } else {
         if (!reportedMissingShader) {
            reportedMissingShader = true;
            LittlePerformanceImprovements.LOGGER.error("Veil interior-preview shader {} is unavailable; using the un-clipped fallback", SHADER);
         }

         return false;
      }
   }

   public static boolean drawResolved(
      VertexBuffer vertexBuffer,
      VertexBuffer interiorVertexBuffer,
      float red,
      float green,
      float blue,
      float alpha,
      float nearClipDistance,
      boolean shadeGeometry,
      boolean highlightThinFeatures
   ) {
      return drawResolved(vertexBuffer, interiorVertexBuffer, red, green, blue, alpha, nearClipDistance, shadeGeometry, highlightThinFeatures, false, null);
   }

   public static boolean drawResolvedTextured(VertexBuffer vertexBuffer, VertexBuffer interiorVertexBuffer, float alpha, float nearClipDistance) {
      return drawResolvedTextured(vertexBuffer, interiorVertexBuffer, alpha, nearClipDistance, null);
   }

   public static boolean drawResolvedTextured(VertexBuffer var0, VertexBuffer var1, float var2, float var3, TextureDrawLayout var4) {
      return TextureTransparencyRenderer.draw(var0, var1, var2, var3, var4);
   }

   private static boolean drawResolved(
      VertexBuffer vertexBuffer,
      VertexBuffer interiorVertexBuffer,
      float red,
      float green,
      float blue,
      float alpha,
      float nearClipDistance,
      boolean shadeGeometry,
      boolean highlightThinFeatures,
      boolean textured,
      TextureDrawLayout drawLayout
   ) {
      resolvedTextureFrameValid = resolvedTextureFrameValid & SurfaceResolveKey.same(red, green, blue, shadeGeometry, highlightThinFeatures, textured);

      Minecraft minecraft = Minecraft.getInstance();
      int targetWidth = minecraft.getWindow().getWidth();
      int targetHeight = minecraft.getWindow().getHeight();
      int previousDrawFramebuffer = GL11.glGetInteger(36006);
      int previousReadFramebuffer = GL11.glGetInteger(36010);
      if (targetWidth > 0 && targetHeight > 0 && ensureTarget(targetWidth, targetHeight)) {
         float[] currentProjection = new float[16];
         float[] currentModelView = new float[16];
         RenderSystem.getProjectionMatrix().get(currentProjection);
         RenderSystem.getModelViewMatrix().get(currentModelView);
         long resolveNow = System.nanoTime();
         boolean reusableResolvedTexture = true
            && resolvedTextureFrameValid
            && vertexBuffer == resolvedTextureVertexBuffer
            && interiorVertexBuffer == resolvedTextureInteriorVertexBuffer
            && drawLayout == resolvedTextureDrawLayout
            && Float.floatToIntBits(alpha) == Float.floatToIntBits(resolvedTextureAlpha)
            && Float.floatToIntBits(nearClipDistance) == Float.floatToIntBits(resolvedTextureNearClip);
         boolean sameResolvedView = reusableResolvedTexture
            && matrixMatches(currentProjection, resolvedTextureProjection)
            && matrixMatches(currentModelView, resolvedTextureModelView);
         long resolvedTextureAge = resolveNow - lastTextureResolveNanos;
         boolean reuseResolvedTexture = reusableResolvedTexture && resolvedTextureAge < (sameResolvedView ? TEXTURE_STILL_REFRESH_NANOS : TEXTURE_MOVING_REFRESH_NANOS);

         RenderTarget mainTarget = minecraft.getMainRenderTarget();
         boolean useMainDepthTexture = previousReadFramebuffer == mainTarget.frameBufferId
            && mainTarget.useDepth
            && mainTarget.width == targetWidth
            && mainTarget.height == targetHeight
            && mainTarget.getDepthTextureId() > 0;
         boolean hardwareTextureDepth = textured && useMainDepthTexture;
         ResourceLocation requestedShader = textured ? (hardwareTextureDepth ? TEXTURE_DEPTH_SHADER : TEXTURE_SHADER) : SHADER;
         ShaderProgram surfaceShader = VeilRenderSystem.setShader(requestedShader);
         ShaderProgram compositeShader = VeilRenderSystem.setShader(COMPOSITE_SHADER);
         if (surfaceShader != null && surfaceShader.isValid() && compositeShader != null && compositeShader.isValid()) {
            int previousActiveTexture = GL11.glGetInteger(34016);
            GL13.glActiveTexture(33984);
            int previousTexture0 = GL11.glGetInteger(32873);
            GL13.glActiveTexture(33985);
            int previousTexture1 = GL11.glGetInteger(32873);
            GL13.glActiveTexture(previousActiveTexture);
            boolean blend = GL11.glIsEnabled(3042);
            boolean depthTest = GL11.glIsEnabled(2929);
            boolean cull = GL11.glIsEnabled(2884);
            boolean scissor = GL11.glIsEnabled(3089);
            boolean polygonOffsetFill = GL11.glIsEnabled(32823);
            int cullFaceMode = GL11.glGetInteger(2885);
            float polygonOffsetFactor = GL11.glGetFloat(32824);
            float polygonOffsetUnits = GL11.glGetFloat(10752);
            int resolvedSceneDepthTexture = sceneDepthTexture;
            if (useMainDepthTexture) {
               resolvedSceneDepthTexture = mainTarget.getDepthTextureId();
            }

            try {
               if (!reuseResolvedTexture) {
                  if (hardwareTextureDepth) {
                     GL30.glBindFramebuffer(36008, previousReadFramebuffer);
                     GL30.glBindFramebuffer(36009, framebuffer);
                     GL30.glBlitFramebuffer(0, 0, targetWidth, targetHeight, 0, 0, targetWidth, targetHeight, 256, 9728);
                  } else if (!useMainDepthTexture) {
                     GL30.glBindFramebuffer(36008, previousReadFramebuffer);
                     GL30.glBindFramebuffer(36009, sceneDepthFramebuffer);
                     GL30.glBlitFramebuffer(0, 0, targetWidth, targetHeight, 0, 0, targetWidth, targetHeight, 256, 9728);
                  }

                  GL30.glBindFramebuffer(36160, framebuffer);
                  GL11.glViewport(0, 0, targetWidth, targetHeight);
                  GL11.glDisable(3089);
                  GL11.glDisable(3042);
                  GL11.glEnable(2929);
                  GL11.glDepthMask(true);
                  GL11.glDepthFunc(515);
                  GL11.glColorMask(true, true, true, true);
                  GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  if (hardwareTextureDepth) {
                     GL11.glClear(16384);
                  } else {
                     GL11.glClearDepth(1.0);
                     GL11.glClear(16640);
                  }

                  surfaceShader.setDefaultUniforms(Mode.QUADS);
                  surfaceShader.getUniformSafe("NearClipDistance").setFloat(nearClipDistance);
                  surfaceShader.getUniformSafe("DeduplicatePass").setInt(0);
                  surfaceShader.getUniformSafe("PerTilePulse").setInt(shadeGeometry ? 1 : 0);
                  surfaceShader.getUniformSafe("HighlightThinFeatures").setInt(highlightThinFeatures ? 1 : 0);
                  surfaceShader.getUniformSafe("ColorModulator").setVector(red, green, blue, alpha);
                  if (!hardwareTextureDepth) {
                     surfaceShader.getUniformSafe("SceneDepth").setInt(1);
                  }

                  if (textured) {
                     surfaceShader.setTexture("Sampler0", InventoryMenu.BLOCK_ATLAS);
                  }

                  surfaceShader.bind();
                  GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                  GL11.glPolygonOffset(-1.0F, -2.0F);

                  try {
                     if (!hardwareTextureDepth) {
                        GL13.glActiveTexture(33985);
                        GL11.glBindTexture(3553, resolvedSceneDepthTexture);
                        int sceneDepthLocation = sceneDepthUniformLocation(surfaceShader.getProgram());
                        if (sceneDepthLocation >= 0) {
                           GL20.glUniform1i(sceneDepthLocation, 1);
                        }
                     }

                     GL13.glActiveTexture(33984);
                     vertexBuffer.bind();

                     if (!textured) {
                        if (interiorVertexBuffer != null && interiorVertexBuffer != vertexBuffer) {
                           GL11.glEnable(2884);
                           GL11.glCullFace(1029);
                           vertexBuffer.draw();

                           GL11.glCullFace(1028);
                           GL11.glEnable(32823);
                           GL11.glPolygonOffset(-0.5F, -1.0F);
                           interiorVertexBuffer.bind();
                           interiorVertexBuffer.draw();
                        } else {
                           GL11.glDisable(2884);
                           GL11.glEnable(32823);
                           GL11.glDepthFunc(513);
                           vertexBuffer.draw();

                        }
                     } else {
                        int atlasTexture = minecraft.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).getId();
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, atlasTexture);
                        int samplerLocation = textureSamplerUniformLocation(surfaceShader.getProgram());
                        if (samplerLocation >= 0) {
                           GL20.glUniform1i(samplerLocation, 0);
                        }

                        if (interiorVertexBuffer != null && interiorVertexBuffer != vertexBuffer) {
                           GL11.glEnable(2884);
                           GL11.glCullFace(1029);
                           drawTexturedBuffer(vertexBuffer, drawLayout, false);

                           GL11.glCullFace(1028);
                           GL11.glEnable(32823);
                           GL11.glPolygonOffset(-0.5F, -1.0F);
                           interiorVertexBuffer.bind();
                           drawTexturedBuffer(interiorVertexBuffer, drawLayout, false);
                        } else {
                           GL11.glDisable(2884);
                           GL11.glEnable(32823);
                           GL11.glDepthFunc(513);
                           drawTexturedBuffer(vertexBuffer, drawLayout, true);

                        }

                     }

                  } finally {
                     VertexBuffer.unbind();
                     if (polygonOffsetFill) {
                        GL11.glEnable(32823);
                     } else {
                        GL11.glDisable(32823);
                     }

                     GL11.glPolygonOffset(polygonOffsetFactor, polygonOffsetUnits);
                     GL11.glCullFace(cullFaceMode);
                     ShaderProgram.unbind();
                  }

                  if (true) {
                     resolvedTextureFrameValid = true;
                     lastTextureResolveNanos = resolveNow;
                     resolvedTextureVertexBuffer = vertexBuffer;
                     resolvedTextureInteriorVertexBuffer = interiorVertexBuffer;
                     resolvedTextureDrawLayout = drawLayout;
                     resolvedTextureAlpha = alpha;
                     resolvedTextureNearClip = nearClipDistance;
                     System.arraycopy(currentProjection, 0, resolvedTextureProjection, 0, 16);
                     System.arraycopy(currentModelView, 0, resolvedTextureModelView, 0, 16);
                  }
               }

               GL30.glBindFramebuffer(36009, previousDrawFramebuffer);
               GL30.glBindFramebuffer(36008, previousReadFramebuffer);
               GL11.glViewport(0, 0, targetWidth, targetHeight);
               GL11.glEnable(3042);
               GL14.glBlendFuncSeparate(770, 771, 1, 771);
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
               GL11.glDisable(2884);
               GL13.glActiveTexture(33984);
               GL11.glBindTexture(3553, colorTexture);
               compositeShader.getUniformSafe("PreviewTexture").setInt(0);
               compositeShader.bind();

               try {
                  GL30.glBindVertexArray(fullscreenVao);
                  GL11.glDrawArrays(4, 0, 3);
                  GL30.glBindVertexArray(0);
               } finally {
                  ShaderProgram.unbind();
               }
            } finally {
               GL30.glBindFramebuffer(36009, previousDrawFramebuffer);
               GL30.glBindFramebuffer(36008, previousReadFramebuffer);
               GL11.glViewport(0, 0, targetWidth, targetHeight);
               GL13.glActiveTexture(33984);
               GL11.glBindTexture(3553, previousTexture0);
               GL13.glActiveTexture(33985);
               GL11.glBindTexture(3553, previousTexture1);
               GL13.glActiveTexture(previousActiveTexture);
               GL11.glDepthFunc(515);
               GL11.glDepthMask(true);
               if (blend) {
                  GL11.glEnable(3042);
               } else {
                  GL11.glDisable(3042);
               }

               if (depthTest) {
                  GL11.glEnable(2929);
               } else {
                  GL11.glDisable(2929);
               }

               if (cull) {
                  GL11.glEnable(2884);
               } else {
                  GL11.glDisable(2884);
               }

               if (scissor) {
                  GL11.glEnable(3089);
               } else {
                  GL11.glDisable(3089);
               }

               GL14.glBlendFuncSeparate(770, 771, 1, 771);

            }

            return true;
         } else {
            if (!reportedMissingShader) {
               reportedMissingShader = true;
               LittlePerformanceImprovements.LOGGER.error("Veil resolved preview shader {} is unavailable; using the direct fallback", requestedShader);
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean matrixMatches(float[] left, float[] right) {
      for (int index = 0; index < 16; index++) {
         if (Float.floatToIntBits(left[index]) != Float.floatToIntBits(right[index])) {
            return false;
         }
      }

      return true;
   }

   private static int textureSamplerUniformLocation(int program) {
      if (textureSamplerProgram != program) {
         textureSamplerProgram = program;
         textureSamplerLocation = GL20.glGetUniformLocation(program, "Sampler0");
      }

      return textureSamplerLocation;
   }

   private static int sceneDepthUniformLocation(int program) {
      if (sceneDepthProgram != program) {
         sceneDepthProgram = program;
         sceneDepthLocation = GL20.glGetUniformLocation(program, "SceneDepth");
      }

      return sceneDepthLocation;
   }

   private static void drawTexturedBuffer(VertexBuffer vertexBuffer, TextureDrawLayout drawLayout) {
      drawTexturedBuffer(vertexBuffer, drawLayout, true);
   }

   private static void drawTexturedBuffer(VertexBuffer vertexBuffer, TextureDrawLayout drawLayout, boolean useOcclusionCulling) {
      if (drawLayout == null || !drawLayout.drawVisible(vertexBuffer, useOcclusionCulling)) {
         vertexBuffer.draw();
      }
   }

   private static boolean ensureTarget(int targetWidth, int targetHeight) {
      if (framebuffer != 0 && width == targetWidth && height == targetHeight) {
         return true;
      }

      destroyTarget();
      width = targetWidth;
      height = targetHeight;
      framebuffer = GL30.glGenFramebuffers();
      colorTexture = GL11.glGenTextures();
      depthBuffer = GL30.glGenRenderbuffers();
      sceneDepthFramebuffer = GL30.glGenFramebuffers();
      sceneDepthTexture = GL11.glGenTextures();
      fullscreenVao = GL30.glGenVertexArrays();
      GL30.glBindFramebuffer(36160, framebuffer);
      GL11.glBindTexture(3553, colorTexture);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, 32856, targetWidth, targetHeight, 0, 6408, 5121, 0L);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, colorTexture, 0);
      GL30.glBindRenderbuffer(36161, depthBuffer);
      GL30.glRenderbufferStorage(36161, 33190, targetWidth, targetHeight);
      GL30.glFramebufferRenderbuffer(36160, 36096, 36161, depthBuffer);
      int status = GL30.glCheckFramebufferStatus(36160);
      GL30.glBindFramebuffer(36160, sceneDepthFramebuffer);
      GL11.glBindTexture(3553, sceneDepthTexture);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, 33190, targetWidth, targetHeight, 0, 6402, 5125, 0L);
      GL30.glFramebufferTexture2D(36160, 36096, 3553, sceneDepthTexture, 0);
      GL11.glDrawBuffer(0);
      GL11.glReadBuffer(0);
      int sceneDepthStatus = GL30.glCheckFramebufferStatus(36160);
      GL30.glBindFramebuffer(36160, 0);
      GL11.glBindTexture(3553, 0);
      GL30.glBindRenderbuffer(36161, 0);
      if (status == 36053 && sceneDepthStatus == 36053) {
         return true;
      }

      LittlePerformanceImprovements.LOGGER
         .error("Interior preview framebuffer is incomplete: color=0x{}, sceneDepth=0x{}", Integer.toHexString(status), Integer.toHexString(sceneDepthStatus));
      destroyTarget();
      return false;
   }

   private static void destroyTarget() {
      resolvedTextureFrameValid = false;
      if (fullscreenVao != 0) {
         GL30.glDeleteVertexArrays(fullscreenVao);
      }

      if (depthBuffer != 0) {
         GL30.glDeleteRenderbuffers(depthBuffer);
      }

      if (sceneDepthTexture != 0) {
         GL11.glDeleteTextures(sceneDepthTexture);
      }

      if (sceneDepthFramebuffer != 0) {
         GL30.glDeleteFramebuffers(sceneDepthFramebuffer);
      }

      if (colorTexture != 0) {
         GL11.glDeleteTextures(colorTexture);
      }

      if (framebuffer != 0) {
         GL30.glDeleteFramebuffers(framebuffer);
      }

      fullscreenVao = 0;
      depthBuffer = 0;
      sceneDepthTexture = 0;
      sceneDepthFramebuffer = 0;
      colorTexture = 0;
      framebuffer = 0;
      width = 0;
      height = 0;
   }

}
