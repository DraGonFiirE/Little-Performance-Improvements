package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import dev.DraGonFiirE.littleperformanceimprovements.performance.*;
public final class SphereDirectFaces {
 private record Data(LittleBox bounds,SphereVoxelColumns.Columns columns){}
 private static final Map<LittleBoxes,Data> built=Collections.synchronizedMap(new WeakHashMap<>());
 public static void mark(LittleBoxes boxes,LittleBox bounds,SphereVoxelColumns.Columns columns){built.put(boxes,new Data(bounds.copy(),columns));}
 public static List<RenderBox> retain(List<RenderBox> boxes,LittleGrid grid,LittleBoxes source){
  Data data=built.get(source);if(data==null)return TextureContactVisibility.retainAirTouching(boxes,grid);
  for(var r:boxes)if(!(r instanceof LittleRenderBox l)||l.box.getClass()!=LittleBox.class||r.state==null||!r.state.canOcclude())return TextureContactVisibility.retainAirTouching(boxes,grid);
  for(var render:boxes){PreviewBuildCancellation.check();var owner=((LittleRenderBox)render).box;
   for(var facing:Facing.VALUES){int axis=facing.axis.ordinal();int[] lo={owner.minX,owner.minY,owner.minZ},hi={owner.maxX,owner.maxY,owner.maxZ};
    int plane=facing.positive?hi[axis]:lo[axis],neighbor=facing.positive?plane:plane-1;
    List<int[]> rectangles=new ArrayList<>();int u=axis==0?1:0,v=axis==2?1:2;
    if(axis<2){
     for(int t=lo[u];t<hi[u];t++){int x=axis==0?neighbor:t,y=axis==1?neighbor:t;int index=index(data,x,y);
      if(index<0){rect(rectangles,t,t+1,lo[2],hi[2]);continue;}
      int low=data.bounds.minZ+data.columns.minimum(index),high=data.bounds.minZ+data.columns.maximum(index)+1;
      rect(rectangles,t,t+1,lo[2],Math.min(hi[2],low));rect(rectangles,t,t+1,Math.max(lo[2],high),hi[2]);
     }
    }else{
     for(int x=lo[0];x<hi[0];x++){int start=-1;for(int y=lo[1];y<=hi[1];y++){boolean exposed=y<hi[1]&&!occupied(data,x,y,neighbor);if(exposed&&start<0)start=y;if(!exposed&&start>=0){rect(rectangles,x,x+1,start,y);start=-1;}}}
    }
    long area=0;for(var r:rectangles)area+=(long)(r[1]-r[0])*(r[3]-r[2]);
    if(area==(long)(hi[u]-lo[u])*(hi[v]-lo[v]))continue;
    var fans=new ArrayList<VectorFan>(rectangles.size());
    for(var r:rectangles){Vec3f[] points=new Vec3f[4];boolean forward=((u+1)%3==v)==facing.positive;for(int j=0;j<4;j++){int k=forward?j:(4-j)%4;float[] p=new float[3];p[axis]=facing.positive?1:0;p[u]=((k==1||k==2?r[1]:r[0])-lo[u])/(float)(hi[u]-lo[u]);p[v]=((k>=2?r[3]:r[2])-lo[v])/(float)(hi[v]-lo[v]);points[j]=new Vec3f(p[0],p[1],p[2]);}fans.add(new VectorFan(points));}
    render.setFace(facing,new RenderBoxFaceSpecial(fans,1));
   }
  }return boxes;
 }
 private static int index(Data d,int x,int y){x-=d.bounds.minX;y-=d.bounds.minY;if(x<0||y<0||x>=d.columns.sizeX||y>=d.columns.sizeY)return -1;int i=d.columns.index(x,y);return d.columns.present(i)?i:-1;}
 private static boolean occupied(Data d,int x,int y,int z){int i=index(d,x,y);return i>=0&&z>=d.bounds.minZ+d.columns.minimum(i)&&z<=d.bounds.minZ+d.columns.maximum(i);}
 private static void rect(List<int[]> out,int a,int b,int c,int d){if(a>=b||c>=d)return;for(int i=out.size()-1;i>=Math.max(0,out.size()-2);i--){int[] old=out.get(i);if(old[1]==a&&old[2]==c&&old[3]==d){old[1]=b;return;}}out.add(new int[]{a,b,c,d});}
}
