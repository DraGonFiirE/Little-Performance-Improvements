package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.nio.ByteBuffer;import org.lwjgl.system.MemoryUtil;
public final class DefaultBoxIndices {
 private static ByteBuffer cache;private static int filled;
 private static final int[] Q={4,0,1,5,2,6,7,3,3,1,0,2,6,4,5,7,2,0,4,6,7,5,1,3},T={0,1,2,2,3,0};
 private static void fill(long address,int from,int to){for(int box=from;box<to;box++){long p=address+(long)box*144;for(int face=0;face<6;face++)for(int c:T){MemoryUtil.memPutInt(p,box*8+Q[face*4+c]);p+=4;}}}
 public static synchronized void copy(long destination,int count){
  if(count>262144){fill(destination,0,count);return;}
  if(cache==null||cache.capacity()<count*144){int capacity=Math.min(262144,Math.max(1024,Integer.highestOneBit(count-1)<<1))*144;cache=cache==null?MemoryUtil.memAlloc(capacity):MemoryUtil.memRealloc(cache,capacity);}
  if(filled<count){fill(MemoryUtil.memAddress(cache),filled,count);filled=count;}
  MemoryUtil.memCopy(MemoryUtil.memAddress(cache),destination,(long)count*144);
 }
}
