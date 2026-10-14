package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;import team.creative.littletiles.common.math.box.*;import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Compute an extruded union in its two-dimensional footprint, independent of wall height. */
public final class ExtrudedPreviewFaces {
 public static List<int[]> faces(LittleBoxes boxes){
  if(boxes==null||boxes.all().size()<2)return null;
  LittleBox first=null;boolean[] common={true,true,true};long[] work=new long[3];
  for(var b:boxes.all()){if(b.getClass()!=LittleBox.class)return null;if(first==null)first=b;for(int a=0;a<3;a++){common[a]&=min(b,a)==min(first,a)&&max(b,a)==max(first,a);work[a]+=(long)max(b,a)-min(b,a);}}
  int axis=-1;for(int a=0;a<3;a++)if(common[a]&&(axis<0||max(first,a)-min(first,a)>max(first,axis)-min(first,axis)))axis=a;
  if(axis<0)return null;int u=(axis+1)%3,v=(axis+2)%3;if(work[v]>8000000)return null;
  return FlatWallFaces.build(boxes,axis,min(first,axis),max(first,axis));
 }
 private static int min(LittleBox b,int a){return a==0?b.minX:a==1?b.minY:b.minZ;}
 private static int max(LittleBox b,int a){return a==0?b.maxX:a==1?b.maxY:b.maxZ;}
 public static team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult defaultMesh(team.creative.littletiles.client.render.overlay.PreviewRenderer renderer,LittleBoxes boxes){var result=DragExteriorMesh.build(renderer,boxes);DefaultCylinderGrid2.mark(result.data());return result;}
}
