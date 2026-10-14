package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
/** Sparse, collision-free pixel union followed by exact contiguous-row merging. */
public final class DragSurfaceRows {
 public final int u,v,w;
 public DragSurfaceRows(int axis){u=axis;v=(axis+1)%3;w=(axis+2)%3;}
 private final Long2ObjectOpenHashMap<IntArrayList> rows=new Long2ObjectOpenHashMap<>();
 public DragSurfaceRows(int sx,int sy,int sz){
  u=sx>=sy&&sx>=sz?0:sy>=sz?1:2;
  v=u==0?(sy>=sz?1:2):u==1?(sx>=sz?0:2):(sx>=sy?0:1);w=3-u-v;
 }
 private static int coord(int axis,int x,int y,int z){return axis==0?x:axis==1?y:z;}
 public void addRange(int lo,int hi,int row,int slice){
  long key=((long)slice<<32)|((row^Integer.MIN_VALUE)&0xffffffffL);
  IntArrayList spans=rows.get(key);if(spans==null){spans=new IntArrayList();rows.put(key,spans);}
  if(spans.size()==2&&lo>=spans.getInt(0)&&hi<=spans.getInt(1))return;
  int i=0;while(i<spans.size()&&spans.getInt(i+1)<lo)i+=2;
  int end=i;while(end<spans.size()&&spans.getInt(end)<=hi){lo=Math.min(lo,spans.getInt(end));hi=Math.max(hi,spans.getInt(end+1));end+=2;}
  if(i==end){spans.add(i,lo);spans.add(i+1,hi);}else{spans.set(i,lo);spans.set(i+1,hi);spans.removeElements(i+2,end);}
 } public List<int[]> finish(){
  List<int[]> output=new ArrayList<>();long[] keys=rows.keySet().toLongArray();Arrays.sort(keys);
  Long2ObjectOpenHashMap<int[]> previous=new Long2ObjectOpenHashMap<>(),current=new Long2ObjectOpenHashMap<>();
  for(long key:keys){int slice=(int)(key>>32),row=((int)key)^Integer.MIN_VALUE;
   int[] values=rows.get(key).toIntArray();current.clear();
   for(int i=0;i<values.length;){int lo=values[i++],hi=values[i++];
    long span=((long)lo<<32)|(hi&0xffffffffL);int[] box=previous.get(span);
    if(box!=null&&box[v+3]==row&&box[w]==slice)box[v+3]=row+1;
    else{box=new int[6];box[u]=lo;box[u+3]=hi;box[v]=row;box[v+3]=row+1;box[w]=slice;box[w+3]=slice+1;output.add(box);}
    current.put(span,box);
   }
   var swap=previous;previous=current;current=swap;
  }
  return output;
 }
 public List<int[]> faces(){
  Long2ObjectOpenHashMap<PolygonSpanRows> planes=new Long2ObjectOpenHashMap<>();
  for(var e:rows.long2ObjectEntrySet()){
   long key=e.getLongKey();int z=(int)(key>>32),y=((int)key)^Integer.MIN_VALUE;var spans=e.getValue();
   for(int i=0;i<spans.size();i+=2){int lo=spans.getInt(i),hi=spans.getInt(i+1);
    face(planes,u,false,lo,y,y+1,z);face(planes,u,true,hi,y,y+1,z);
   }
   difference(planes,spans,rows.get(key(y-1,z)),v,false,y,z);
   difference(planes,spans,rows.get(key(y+1,z)),v,true,y+1,z);
   difference(planes,spans,rows.get(key(y,z-1)),w,false,z,y);
   difference(planes,spans,rows.get(key(y,z+1)),w,true,z+1,y);
  }
  List<int[]> result=new ArrayList<>();
  for(var e:planes.long2ObjectEntrySet()){
   long k=e.getLongKey();int axis=(int)(k&3),sign=(int)((k>>2)&1),plane=(int)(k>>3);
   int a=axis==u?v:u,b=axis==w?v:w;
   for(int[] q:e.getValue().finish())result.add(new int[]{axis,sign,plane,a,b,q[0],q[3],q[1],q[4]});
  }return result;
 }
 private static long key(int row,int slice){return ((long)slice<<32)|((row^Integer.MIN_VALUE)&0xffffffffL);}
 private static void face(Long2ObjectOpenHashMap<PolygonSpanRows> planes,int axis,boolean positive,int plane,int lo,int hi,int row){
  long key=((long)plane<<3)|axis|(positive?4:0);var p=planes.get(key);
  if(p==null){p=new PolygonSpanRows(3,2,1);planes.put(key,p);}p.addRange(lo,hi,row,0);
 }
 private static void difference(Long2ObjectOpenHashMap<PolygonSpanRows> planes,IntArrayList own,IntArrayList neighbor,int axis,boolean positive,int plane,int row){
  int j=0;
  for(int i=0;i<own.size();i+=2){int start=own.getInt(i),end=own.getInt(i+1);
   if(neighbor!=null){while(j<neighbor.size()&&neighbor.getInt(j+1)<=start)j+=2;
    for(int k=j;k<neighbor.size()&&neighbor.getInt(k)<end;k+=2){int lo=neighbor.getInt(k),hi=neighbor.getInt(k+1);if(lo>start)face(planes,axis,positive,plane,start,Math.min(lo,end),row);start=Math.max(start,hi);if(start>=end)break;}
   }
   if(start<end)face(planes,axis,positive,plane,start,end,row);
  }
 }

}

