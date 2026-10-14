package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.tool.shaper.*;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
import team.creative.creativecore.common.util.math.geo.Ray3d;
public final class PolygonPerimeterPreview {
 private static final Map<PreviewRenderer.BoxRenderResult,Boolean> temporary=Collections.synchronizedMap(new WeakHashMap<>());
 public static LittleBoxes geometry(LittleShape shape,ShapeSelection selection,LittleShapeConfig config){
  if(shape.getClass()!=LittleShapePolygon.class||selection.size()<3)return FastPolygonSurface.build(shape,selection,config);
  LittleBox bounds=selection.getOverallBox();PolygonPixelRows pixels=new PolygonPixelRows(bounds.maxX-bounds.minX,bounds.maxY-bounds.minY,bounds.maxZ-bounds.minZ);
  Set<Long> edges=new LinkedHashSet<>();
  for(int i=2;i<selection.size();i++){toggle(edges,i-2,i-1);toggle(edges,i-1,i);toggle(edges,i-2,i);}
  for(long key:edges){
   var first=selection.get((int)(key>>>32));var second=selection.get((int)key);
   var a=first.getRelative(selection.pos).getVec(first.getGrid());var b=second.getRelative(selection.pos).getVec(second.getGrid());
   double half=first.getGrid().halfPixelLength;a.x+=half;a.y+=half;a.z+=half;b.x+=half;b.y+=half;b.z+=half;
   var d=new Ray3d(a,b,false).direction;int steps=Math.max(1,(int)Math.ceil(d.length()/selection.grid.pixelLength*1.4));
   for(int i=0;i<=steps;i++){double t=(double)i/steps;pixels.add(selection.grid.toGrid(a.x+d.x*t),selection.grid.toGrid(a.y+d.y*t),selection.grid.toGrid(a.z+d.z*t));}
  }
  LittleBoxesSimple result=new LittleBoxesSimple(selection.pos,selection.grid);
  for(int[] b:pixels.finish())result.add(new LittleBox(b[0],b[1],b[2],b[3],b[4],b[5]));return result;
 }
 private static void toggle(Set<Long> edges,int a,int b){long key=((long)Math.min(a,b)<<32)|(Math.max(a,b)&0xffffffffL);if(!edges.add(key))edges.remove(key);}
 public static PreviewRenderer.BoxRenderResult mark(LittleShape shape,ShapeSelection selection,PreviewRenderer.BoxRenderResult result){
  if(result!=null)temporary.put(result,Boolean.TRUE);return result;
 }
 /** Returning null selects the existing full-shape placement build path. */
 public static PreviewRenderer.BoxRenderResult placement(PreviewRenderer.BoxRenderResult result){return result!=null&&temporary.containsKey(result)?null:result;}
}

