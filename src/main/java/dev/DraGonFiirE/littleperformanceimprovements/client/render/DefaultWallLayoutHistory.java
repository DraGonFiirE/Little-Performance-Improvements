package dev.DraGonFiirE.littleperformanceimprovements.client.render;
/** Small immutable history, bounded by both entries and estimated retained bytes. */
public final class DefaultWallLayoutHistory {
 private static volatile Object[] entries=new Object[0];
 private static long[] sizes=new long[0];
 public static Object[] get(){return entries;}
 public static synchronized void remember(Object value,int boxes){long weight=(long)boxes*240;if(weight>24*1024*1024)return;var old=entries;int keep=0;long total=weight;while(keep<old.length&&keep<5&&total+sizes[keep]<=24*1024*1024){total+=sizes[keep];keep++;}Object[] next=new Object[keep+1];long[] weights=new long[keep+1];next[0]=value;weights[0]=weight;System.arraycopy(old,0,next,1,keep);System.arraycopy(sizes,0,weights,1,keep);sizes=weights;entries=next;}
}
