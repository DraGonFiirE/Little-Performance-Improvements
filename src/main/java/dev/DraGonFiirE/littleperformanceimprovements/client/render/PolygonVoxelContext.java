package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
public record PolygonVoxelContext(LongOpenHashSet cells,int[] min,long sy,long sz){static final ThreadLocal<PolygonVoxelContext> current=new ThreadLocal<>();static PolygonVoxelContext take(){var c=current.get();current.remove();return c;}}
