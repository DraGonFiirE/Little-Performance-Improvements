package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;
import team.creative.littletiles.client.LittleTilesClient;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.LittleTool;
import java.lang.reflect.Method;
import java.util.ArrayList;
/** Shader-pack-independent previews, composited after world effects instead of into pack-specific G-buffers. */
public final class ShaderPreviewPass {
 private static final ArrayList<Draw> pending=new ArrayList<>(2);
 private static boolean checked,flushing;
 private static Object iris;
 private static Method enabled,shadow;
 private record Draw(LittleTool tool,PreviewRenderer renderer,PoseStack pose,Vec3 camera,boolean highlight,Matrix4f view,Matrix4f projection,VertexSorting sorting){}
 private ShaderPreviewPass(){}
 private static boolean shaders(){
  if(!checked){checked=true;try{Class<?> api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");iris=api.getMethod("getInstance").invoke(null);enabled=api.getMethod("isShaderPackInUse");shadow=api.getMethod("isRenderingShadowPass");}catch(ReflectiveOperationException|LinkageError e){iris=null;}}
  if(iris==null)return false;try{return (boolean)enabled.invoke(iris);}catch(ReflectiveOperationException e){return false;}
 }
 public static void beginFrame(){pending.clear();}
 public static void render(LittleTool tool,PreviewRenderer renderer,PoseStack pose,Vec3 camera,boolean highlight){
  if(flushing||!shaders()){tool.render(renderer,pose,camera,highlight);return;}
  try{if((boolean)shadow.invoke(iris))return;}catch(ReflectiveOperationException ignored){}
  PoseStack copy=new PoseStack();copy.last().pose().set(pose.last().pose());copy.last().normal().set(pose.last().normal());
  // One draw per tool/pass; shadow or repeated stage events cannot accumulate work.
  pending.removeIf(draw->draw.tool==tool&&draw.highlight==highlight);
  pending.add(new Draw(tool,renderer,copy,camera,highlight,new Matrix4f(RenderSystem.getModelViewMatrix()),new Matrix4f(RenderSystem.getProjectionMatrix()),RenderSystem.getVertexSorting()));
 }
 public static void flush(){
  if(pending.isEmpty())return;
  var draws=new ArrayList<>(pending);pending.clear();Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.options.hideGui)return;
  try(var state=new PreviewPassState()){
   flushing=true;com.mojang.blaze3d.vertex.BufferUploader.invalidate();mc.getMainRenderTarget().bindWrite(true);
   RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL33C.GL_LEQUAL);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
   for(var draw:draws){if(draw.tool!=LittleTilesClient.PREVIEW_RENDERER.tool())continue;
    RenderSystem.setProjectionMatrix(draw.projection,draw.sorting);RenderSystem.getModelViewStack().set(draw.view);RenderSystem.applyModelViewMatrix();
    draw.tool.render(draw.renderer,draw.pose,draw.camera,draw.highlight);
   }
  }finally{com.mojang.blaze3d.vertex.BufferUploader.invalidate();flushing=false;}
 }
}

