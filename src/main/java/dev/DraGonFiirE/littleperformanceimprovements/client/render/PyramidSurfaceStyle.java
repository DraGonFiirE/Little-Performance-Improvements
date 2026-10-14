package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.MeshData;import com.mojang.blaze3d.systems.RenderSystem;import java.util.*;
public final class PyramidSurfaceStyle {
 private static final Set<MeshData> meshes=Collections.newSetFromMap(new WeakHashMap<>());
 public static synchronized void mark(MeshData mesh){if(mesh!=null){meshes.add(mesh);WallPreviewDepth.mark(mesh);}}
 public static Runnable wrap(MeshData mesh,Runnable setup){synchronized(PyramidSurfaceStyle.class){if(!meshes.contains(mesh))return setup;}return ()->{if(setup!=null)setup.run();float[] c=RenderSystem.getShaderColor();float alpha=(float)((dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig)(Object)team.creative.littletiles.LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewSurfaceAlpha();RenderSystem.setShaderColor(c[0],c[1],c[2],alpha);};}
 public static team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult build(team.creative.littletiles.client.render.overlay.PreviewRenderer renderer,team.creative.littletiles.common.math.box.collection.LittleBoxes boxes){var result=PyramidSlabExterior.build(renderer,boxes);mark(result.data());return SurfaceChiselShade.apply(result);}
}
