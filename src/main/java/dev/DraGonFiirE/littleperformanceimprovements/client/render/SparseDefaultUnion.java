package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
/** Exact sparse union: sample deltas become intervals, then contiguous solid boxes. */
public final class SparseDefaultUnion {
 public static LittleBoxes build(LittleBoxes raw){
  long[] cost=new long[3];for(var b:raw.all()){long x=(long)b.maxX-b.minX,y=(long)b.maxY-b.minY,z=(long)b.maxZ-b.minZ;cost[0]+=y*z;cost[1]+=x*z;cost[2]+=x*y;}
  int axis=cost[0]<=cost[1]&&cost[0]<=cost[2]?0:cost[1]<=cost[2]?1:2;
  var rows=new DragSurfaceRows(axis);LittleBox previous=null;var seen=new HashSet<LittleBox>();
  for(var b:raw.all())if(seen.add(b)){DragSurfaceDelta.add(rows,b,previous);previous=b;}
  var result=new LittleBoxesSimple(raw.pos,raw.grid);var last=new HashMap<String,int[]>();var merged=new ArrayList<int[]>();
  for(var b:rows.finish()){
   String key=b[rows.u]+","+b[rows.u+3]+","+b[rows.v]+","+b[rows.v+3];var prior=last.get(key);
   if(prior!=null&&prior[rows.w+3]==b[rows.w])prior[rows.w+3]=b[rows.w+3];
   else{last.put(key,b);merged.add(b);}
  }
  for(var b:merged)result.add(new LittleBox(b[0],b[1],b[2],b[3],b[4],b[5]));
  return DefaultCylinderBlocks.split(result);
 }
}
