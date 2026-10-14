package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
/** Sparse, collision-free pixel union followed by exact contiguous-row merging. */
public final class PolygonSpanRows {
 public final int u,v,w;
 private final Long2ObjectOpenHashMap<IntArrayList> rows=new Long2ObjectOpenHashMap<>();
 public PolygonSpanRows(int sx,int sy,int sz){
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
}

