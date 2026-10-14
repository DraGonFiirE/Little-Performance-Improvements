package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Cancels rectangle edges in the fast cylinder's non-overlapping extrusion. */
public final class CylinderDragExterior {
 public static List<int[]> faces(LittleBoxes boxes){
  List<int[]> rects=new ArrayList<>();int axis=-1;int[] first=null;boolean[] common={true,true,true};
  for(var b:boxes.all()){
   if(b.getClass()!=LittleBox.class)return DragExteriorMesh.faces(boxes);
   int[] r={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};rects.add(r);
   if(first==null)first=r;else for(int a=0;a<3;a++)common[a]&=first[a]==r[a]&&first[a+3]==r[a+3];
  }
  if(first==null)return List.of();for(int a=0;a<3;a++)if(common[a]){axis=a;break;}
  if(axis<0)return DragExteriorMesh.faces(boxes);
  int u=(axis+1)%3,v=(axis+2)%3;var result=new ArrayList<int[]>();var edges=new HashMap<Long,TreeMap<Integer,Integer>>();
  for(var r:rects){
   for(int sign=0;sign<2;sign++)result.add(new int[]{axis,sign,r[axis+sign*3],u,v,r[u],r[u+3],r[v],r[v+3]});
   edge(edges,u,r[u],r[v],r[v+3],-1);edge(edges,u,r[u+3],r[v],r[v+3],1);
   edge(edges,v,r[v],r[u],r[u+3],-1);edge(edges,v,r[v+3],r[u],r[u+3],1);
  }
  for(var e:edges.entrySet()){
   int a=(int)(e.getKey()&3),plane=(int)(e.getKey()>>2),b=a==u?v:u,coverage=0,start=0;
   for(var event:e.getValue().entrySet()){
    int end=event.getKey();if(coverage!=0&&end>start)result.add(new int[]{a,coverage>0?1:0,plane,b,axis,start,end,first[axis],first[axis+3]});
    coverage+=event.getValue();start=end;
   }
  }return result;
 }
 private static void edge(Map<Long,TreeMap<Integer,Integer>> edges,int axis,int plane,int lo,int hi,int sign){
  var events=edges.computeIfAbsent(((long)plane<<2)|axis,k->new TreeMap<>());events.merge(lo,sign,Integer::sum);events.merge(hi,-sign,Integer::sum);
 }
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes){
  var faces=faces(boxes);if(faces==null)return SafeDragMesh.build(renderer,boxes);
  var buffer=renderer.createBuffer();try{
   var builder=renderer.createBuilder(buffer,false);float scale=(float)boxes.grid.pixelLength;
   for(var f:faces){int axis=f[0],a=f[3],b=f[4];boolean forward=((a+1)%3==b)==(f[1]==1);
    for(int j=0;j<4;j++){int corner=forward?j:(4-j)%4;float av=f[corner==1||corner==2?6:5]*scale,bv=f[corner>=2?8:7]*scale,p=f[2]*scale;
     float x=axis==0?p:a==0?av:bv,y=axis==1?p:a==1?av:bv,z=axis==2?p:a==2?av:bv;
     builder.addVertex(x,y,z).setColor(255,255,255,255);
    }
   }
   MeshData mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,buffer,mesh);
  }catch(Throwable e){buffer.close();throw e;}
 }
}
