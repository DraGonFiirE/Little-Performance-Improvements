package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
public final class SphereRingPixels {
 private final int[] min;private final int u,v,w;private final PolygonPixelRows fallback;
 private final LongOpenHashSet pixels=new LongOpenHashSet();
 public SphereRingPixels(int[] min,int[] size){this.min=min;u=size[0]>=size[1]&&size[0]>=size[2]?0:size[1]>=size[2]?1:2;v=u==0?(size[1]>=size[2]?1:2):u==1?(size[0]>=size[2]?0:2):(size[0]>=size[1]?0:1);w=3-u-v;fallback=size[0]>=2097152||size[1]>=2097152||size[2]>=2097152?new PolygonPixelRows(size[0],size[1],size[2]):null;}
 public void add(int x,int y,int z){if(fallback!=null){fallback.add(x,y,z);return;}x-=min[0];y-=min[1];z-=min[2];int a=u==0?x:u==1?y:z,b=v==0?x:v==1?y:z,c=w==0?x:w==1?y:z;pixels.add(((long)c<<42)|((long)b<<21)|a);}
 public List<int[]> finish(){if(fallback!=null)return fallback.finish();long[] sorted=pixels.toLongArray();Arrays.sort(sorted);var out=new ArrayList<int[]>();var previous=new Long2ObjectOpenHashMap<int[]>();var current=new Long2ObjectOpenHashMap<int[]>();
  for(int i=0;i<sorted.length;){long rowKey=sorted[i]>>>21;int row=(int)(rowKey&2097151),slice=(int)(rowKey>>>21);current.clear();
   while(i<sorted.length&&sorted[i]>>>21==rowKey){int lo=(int)(sorted[i]&2097151),hi=lo+1;i++;while(i<sorted.length&&sorted[i]>>>21==rowKey&&(sorted[i]&2097151)==hi){hi++;i++;}
    long span=((long)lo<<32)|(hi&0xffffffffL);int[] box=previous.get(span);
    if(box!=null&&box[v+3]==min[v]+row&&box[w]==min[w]+slice)box[v+3]++;
    else{box=new int[6];box[u]=min[u]+lo;box[u+3]=min[u]+hi;box[v]=min[v]+row;box[v+3]=min[v]+row+1;box[w]=min[w]+slice;box[w+3]=min[w]+slice+1;out.add(box);}
    current.put(span,box);
   }var swap=previous;previous=current;current=swap;
  }return out;
 }
}
