package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL33C;
import org.joml.Matrix4f;
/** Isolates preview draws from the world/composite and subsequent hand/GUI passes. */
final class PreviewPassState implements AutoCloseable {
 private final int read=GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING),draw=GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING),program=GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM),vao=GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING),active=GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
 private final int depthFunc=GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC),srcRgb=GL33C.glGetInteger(GL33C.GL_BLEND_SRC_RGB),dstRgb=GL33C.glGetInteger(GL33C.GL_BLEND_DST_RGB),srcAlpha=GL33C.glGetInteger(GL33C.GL_BLEND_SRC_ALPHA),dstAlpha=GL33C.glGetInteger(GL33C.GL_BLEND_DST_ALPHA);
 private final boolean depth=GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST),write=GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK),blend=GL33C.glIsEnabled(GL33C.GL_BLEND),cull=GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
 private final boolean offset=GL33C.glIsEnabled(GL33C.GL_POLYGON_OFFSET_FILL),scissor=GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
 private final float offsetFactor=GL33C.glGetFloat(GL33C.GL_POLYGON_OFFSET_FACTOR),offsetUnits=GL33C.glGetFloat(GL33C.GL_POLYGON_OFFSET_UNITS),lineWidth=GL33C.glGetFloat(GL33C.GL_LINE_WIDTH);
 private final int equationRgb=GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_RGB),equationAlpha=GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_ALPHA);
 private final boolean[] mask=new boolean[4];
 private final int[] viewport=new int[4],textures=new int[3],buffers=new int[3],samplers=new int[3],shaderTextures=new int[12];
 private final Matrix4f textureMatrix=new Matrix4f(RenderSystem.getTextureMatrix());private final float[] color=RenderSystem.getShaderColor().clone();
 private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());private final com.mojang.blaze3d.vertex.VertexSorting sorting=RenderSystem.getVertexSorting();
 private final net.minecraft.client.renderer.ShaderInstance shader=RenderSystem.getShader();
 PreviewPassState(){GL33C.glGetIntegerv(GL33C.GL_VIEWPORT,viewport);try(var stack=org.lwjgl.system.MemoryStack.stackPush()){var values=stack.malloc(4);GL33C.glGetBooleanv(GL33C.GL_COLOR_WRITEMASK,values);for(int i=0;i<4;i++)mask[i]=values.get(i)!=0;}
  for(int i=0;i<shaderTextures.length;i++)shaderTextures[i]=RenderSystem.getShaderTexture(i);
  for(int i=0;i<3;i++){RenderSystem.activeTexture(GL33C.GL_TEXTURE0+i);textures[i]=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);buffers[i]=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_BUFFER);samplers[i]=GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING,i);}RenderSystem.activeTexture(active);RenderSystem.getModelViewStack().pushMatrix();}
 public void close(){
  RenderSystem.getModelViewStack().popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
  RenderSystem.depthMask(write);RenderSystem.depthFunc(depthFunc);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
  RenderSystem.colorMask(mask[0],mask[1],mask[2],mask[3]);RenderSystem.polygonOffset(offsetFactor,offsetUnits);if(offset)RenderSystem.enablePolygonOffset();else RenderSystem.disablePolygonOffset();if(scissor)GL33C.glEnable(GL33C.GL_SCISSOR_TEST);else GL33C.glDisable(GL33C.GL_SCISSOR_TEST);RenderSystem.lineWidth(lineWidth);GL33C.glBlendEquationSeparate(equationRgb,equationAlpha);RenderSystem.setTextureMatrix(textureMatrix);
  for(int i=0;i<shaderTextures.length;i++)RenderSystem.setShaderTexture(i,shaderTextures[i]);
  for(int i=0;i<3;i++){RenderSystem.activeTexture(GL33C.GL_TEXTURE0+i);RenderSystem.bindTexture(textures[i]);GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,textures[i]);GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER,buffers[i]);GL33C.glBindSampler(i,samplers[i]);}
  GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER,read);GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER,draw);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);GL33C.glBindVertexArray(vao);GL33C.glUseProgram(program);RenderSystem.activeTexture(active);
 }
}

