package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.MeshData;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import java.util.*;
/** Neutralize native chisel outline vertices once, before their first upload. */
public final class ChiselOutlineTint {
 private static final Set<MeshData> prepared=Collections.newSetFromMap(new WeakHashMap<>());
 public static void prepare(boolean lines,MeshData mesh){
  if(!lines||mesh==null||ToolPreviewMode.current()!=PreviewRendererMode.Outline||!prepared.add(mesh))return;
  OutlineMeshColor.neutralize(mesh);
 }
}
