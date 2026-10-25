package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.level.context.ILittleLevelContext;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import team.creative.littletiles.LittleTiles;
import org.lwjgl.opengl.GL11;
import java.lang.ref.*;
import java.util.*;
public final class ChiselOutlineDepth {
 private static final ReferenceQueue<MeshData> queue=new ReferenceQueue<>();
 private static final Map<Key,PreviewRenderer.BoxRenderResult> surfaces=new HashMap<>();
 private static final class Key extends WeakReference<MeshData>{final int hash;Key(MeshData m){super(m,queue);hash=System.identityHashCode(m);}public int hashCode(){return hash;}public boolean equals(Object o){return this==o||o instanceof Key k&&get()!=null&&get()==k.get();}}
 private static VertexBuffer surfaceBuffer,lineBuffer;
 private static WeakReference<MeshData> uploaded=new WeakReference<>(null);
 public static PreviewRenderer.BoxRenderResult register(PreviewRenderer.BoxRenderResult result,PreviewRenderer renderer,LittleBoxes geometry){
  if(EditingOutlinePreview.active())return LinesOnlyOutlineDrag.mark(result);
  if(result==null||result.data()==null||LinesOnlyOutlineDrag.contains(result.data())||ToolPreviewMode.current()!=PreviewRendererMode.Outline)return result;
  synchronized(surfaces){if(surfaces.containsKey(new Key(result.data())))return result;}
  var mode=result.data().drawState().mode();if(mode!=VertexFormat.Mode.LINES&&mode!=VertexFormat.Mode.DEBUG_LINES)return result;
  var depth=PolygonVoxelOutline.take();if(depth==null)depth=FastChiselOutline.take();if(depth==null)depth=SafeDragMesh.build(renderer,geometry);if(depth.data()==null){depth.close();return result;}
  synchronized(surfaces){var old=surfaces.put(new Key(result.data()),depth);if(old!=null)old.close();}
  return result;
 }
 private static PreviewRenderer.BoxRenderResult lookup(MeshData mesh){synchronized(surfaces){Key dead;while((dead=(Key)queue.poll())!=null){var stale=surfaces.remove(dead);if(stale!=null)stale.close();}return surfaces.get(new Key(mesh));}}
 public static boolean draw(PreviewRenderer renderer,ILittleLevelContext context,Vec3 camera,BlockPos pos,boolean lines,MeshData mesh,Runnable setup){
  if(!lines||mesh==null||ToolPreviewMode.current()!=PreviewRendererMode.Outline)return false;
  boolean drag=LinesOnlyOutlineDrag.contains(mesh);var depth=drag?null:lookup(mesh);if(!drag&&depth==null)return false;
  ChiselOutlineTint.prepare(true,mesh);
  if(uploaded.get()!=mesh){SphereOutlineBatches.clear();
   if(surfaceBuffer==null){surfaceBuffer=new VertexBuffer(VertexBuffer.Usage.STATIC);lineBuffer=new VertexBuffer(VertexBuffer.Usage.STATIC);}
   if(!drag){surfaceBuffer.bind();surfaceBuffer.upload(depth.data());OutlineDepthVertexSharing.prepare(surfaceBuffer,depth.data());}
   lineBuffer.bind();
   if(mesh.drawState().mode()==VertexFormat.Mode.LINES){var compact=BlueprintPreviewMeshCompactor.compactGeneratedLines(mesh);if(compact==null){VertexBuffer.unbind();return false;}try{lineBuffer.upload(compact.mesh());}finally{compact.mesh().close();compact.buffers().close();}}
   else if(!drag)SphereOutlineBatches.upload(lineBuffer,mesh);else lineBuffer.upload(mesh);
   VertexBuffer.unbind();uploaded=new WeakReference<>(mesh);OutlineLineDepthTexture.invalidate();
  }
  var matrix=RenderSystem.getModelViewStack();matrix.pushMatrix();
  boolean oldCull=GL11.glIsEnabled(GL11.GL_CULL_FACE),oldDepth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),oldWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),oldBlend=GL11.glIsEnabled(GL11.GL_BLEND),oldOffset=GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
  int oldFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);float factor=GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),units=GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS),width=GL11.glGetFloat(GL11.GL_LINE_WIDTH);float[] color=RenderSystem.getShaderColor().clone();
  try(var stack=org.lwjgl.system.MemoryStack.stackPush()){
   var mask=stack.malloc(4);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,mask);
   try{
    context.transformMatrix(matrix,pos.getX(),pos.getY(),pos.getZ(),camera,0);RenderSystem.applyModelViewMatrix();renderer.setupPreviewRenderer(true);if(setup!=null)setup.run();
    var visibleLines=drag?lineBuffer:ConvexOutlineVisibility.select(lineBuffer,mesh,depth.data(),RenderSystem.getModelViewMatrix());
    RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(true);RenderSystem.colorMask(false,false,false,false);RenderSystem.disableCull();GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);GL11.glPolygonOffset(1,1);
    if(drag||visibleLines!=lineBuffer){OutlineLineDepthTexture.invalidate();}
    else if(!OutlineLineDepthTexture.captureSurface(surfaceBuffer)){surfaceBuffer.bind();surfaceBuffer.drawWithShader(RenderSystem.getModelViewMatrix(),RenderSystem.getProjectionMatrix(),GameRenderer.getPositionColorShader());}
    GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);RenderSystem.colorMask(mask.get(0)!=0,mask.get(1)!=0,mask.get(2)!=0,mask.get(3)!=0);RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
    var config=(PreviewRenderingConfig)(Object)LittleTiles.CONFIG.rendering;int rgb=OutlinePreviewColor.rgb();float r=((rgb>>16)&255)/255f,g=((rgb>>8)&255)/255f,b=(rgb&255)/255f,a=(float)config.littleperformanceimprovements$getPreviewOutlineAlpha();
    float lineWidth=Math.max(1,(float)config.littleperformanceimprovements$getPreviewOutlineThickness());
    visibleLines.bind();
    if(EditingOutlinePreview.active()){
     // Complementary tests against world depth: neither pass writes depth,
     // so the preview never hides its own rear edges.
     RenderSystem.depthFunc(GL11.GL_GREATER);
     VeilPreviewLineRenderer.drawHammer(visibleLines,lineWidth,r,g,b,a*0.25F,true);
     RenderSystem.depthFunc(GL11.GL_LEQUAL);
     if(!VeilPreviewLineRenderer.drawHammer(visibleLines,lineWidth,r,g,b,a,false)){
      RenderSystem.setShaderColor(r,g,b,a);
      visibleLines.drawWithShader(RenderSystem.getModelViewMatrix(),RenderSystem.getProjectionMatrix(),GameRenderer.getPositionColorShader());
     }
    }else if(!VeilPreviewLineRenderer.draw(visibleLines,lineWidth,r,g,b,a)){
     RenderSystem.setShaderColor(r,g,b,a);
     visibleLines.drawWithShader(RenderSystem.getModelViewMatrix(),RenderSystem.getProjectionMatrix(),GameRenderer.getPositionColorShader());
    }
   }finally{VertexBuffer.unbind();RenderSystem.colorMask(mask.get(0)!=0,mask.get(1)!=0,mask.get(2)!=0,mask.get(3)!=0);RenderSystem.depthMask(oldWrite);RenderSystem.depthFunc(oldFunc);if(oldCull)RenderSystem.enableCull();else RenderSystem.disableCull();if(oldDepth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(oldBlend)RenderSystem.enableBlend();else RenderSystem.disableBlend();GL11.glPolygonOffset(factor,units);if(oldOffset)GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);RenderSystem.lineWidth(width);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);}
  }finally{matrix.popMatrix();RenderSystem.applyModelViewMatrix();}
  return true;
 }
}
