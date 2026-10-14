package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;import java.nio.ByteOrder;import com.mojang.blaze3d.vertex.*;
/** Shade the native initial cursor mesh once, before its first upload. */
public final class InitialSurfaceShade {
 private static final Set<MeshData> shaded=Collections.newSetFromMap(new WeakHashMap<>());
 public static synchronized void apply(MeshData mesh){
  if(mesh==null||mesh.drawState().format()!=DefaultVertexFormat.POSITION_COLOR||mesh.drawState().mode()!=VertexFormat.Mode.QUADS||mesh.indexBuffer()!=null||!shaded.add(mesh))return;
  SurfaceChiselShade.shade(mesh.vertexBuffer().duplicate().order(ByteOrder.nativeOrder()),mesh.drawState().vertexCount());
 }
}
