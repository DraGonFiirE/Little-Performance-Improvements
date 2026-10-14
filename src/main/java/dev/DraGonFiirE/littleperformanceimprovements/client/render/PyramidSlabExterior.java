package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;import com.mojang.blaze3d.vertex.*;import team.creative.littletiles.client.render.overlay.PreviewRenderer;import team.creative.littletiles.client.render.mc.MeshDataExtender;import team.creative.littletiles.common.math.box.LittleBox;import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Clip touching pyramid layers directly, without expanding them into voxel rows. */
public final class PyramidSlabExterior {
 public static List<int[]> faces(LittleBoxes boxes){var slabs=new ArrayList<int[]>();for(var b:boxes.all()){if(b.getClass()!=LittleBox.class)return null;slabs.add(new int[]{b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ});}if(slabs.isEmpty())return List.of();int axis=-1;
  for(int a=0;a<3;a++){final int coordinate=a;slabs.sort(Comparator.comparingInt(r->r[coordinate]));boolean disjoint=true;for(int i=1;i<slabs.size();i++)if(slabs.get(i-1)[a+3]>slabs.get(i)[a]){disjoint=false;break;}if(disjoint){axis=a;break;}}if(axis<0)return null;
  int u=(axis+1)%3,v=(axis+2)%3;var result=new ArrayList<int[]>();
  for(int i=0;i<slabs.size();i++){var r=slabs.get(i);for(int a:new int[]{u,v})for(int sign=0;sign<2;sign++){int b=3-axis-a;add(result,a,sign,r[a+sign*3],b,axis,r[b],r[b+3],r[axis],r[axis+3]);}
   for(int sign=0;sign<2;sign++){int j=i+(sign==0?-1:1),plane=r[axis+sign*3];int[] neighbor=j>=0&&j<slabs.size()?slabs.get(j):null;
    if(neighbor==null||neighbor[axis+(sign==0?3:0)]!=plane){add(result,axis,sign,plane,u,v,r[u],r[u+3],r[v],r[v+3]);continue;}
    int x0=Math.max(r[u],neighbor[u]),x1=Math.min(r[u+3],neighbor[u+3]),y0=Math.max(r[v],neighbor[v]),y1=Math.min(r[v+3],neighbor[v+3]);
    if(x0>=x1||y0>=y1){add(result,axis,sign,plane,u,v,r[u],r[u+3],r[v],r[v+3]);continue;}
    add(result,axis,sign,plane,u,v,r[u],x0,r[v],r[v+3]);add(result,axis,sign,plane,u,v,x1,r[u+3],r[v],r[v+3]);add(result,axis,sign,plane,u,v,x0,x1,r[v],y0);add(result,axis,sign,plane,u,v,x0,x1,y1,r[v+3]);
   }
  }return result;
 }
 private static void add(List<int[]> result,int axis,int sign,int plane,int u,int v,int loU,int hiU,int loV,int hiV){if(loU<hiU&&loV<hiV)result.add(new int[]{axis,sign,plane,u,v,loU,hiU,loV,hiV});}
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes){var faces=faces(boxes);if(faces==null)return DragExteriorMesh.build(renderer,boxes);var owner=renderer.createBuffer();try{var builder=renderer.createBuilder(owner,false);float scale=(float)boxes.grid.pixelLength;for(var f:faces){int axis=f[0],u=f[3],v=f[4];boolean forward=((u+1)%3==v)==(f[1]==1);for(int j=0;j<4;j++){int c=forward?j:(4-j)%4;float a=f[c==1||c==2?6:5]*scale,b=f[c>=2?8:7]*scale,h=f[2]*scale;builder.addVertex(axis==0?h:u==0?a:b,axis==1?h:u==1?a:b,axis==2?h:u==2?a:b).setColor(-1);}}var mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,owner,mesh);}catch(Throwable e){owner.close();throw e;}}
}
