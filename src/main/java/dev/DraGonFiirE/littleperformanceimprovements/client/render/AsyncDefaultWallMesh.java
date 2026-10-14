package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;import org.lwjgl.system.MemoryUtil;import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;import it.unimi.dsi.fastutil.ints.IntArrayList;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;import team.creative.littletiles.client.render.mc.MeshDataExtender;import team.creative.littletiles.common.math.box.*;import team.creative.littletiles.common.math.box.collection.*;import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;
/** Shares corners without removing, reordering, or changing any Default triangles. */
public final class AsyncDefaultWallMesh {
 private static volatile Layout ready;
 private static final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean();
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Default wall vertex layout");t.setDaemon(true);return t;});
 private static final class Layout { int[] bounds,coordinates,corners,first; int vertices; byte[] indices; }
 private static boolean match(Layout s,LittleBoxes boxes,int[] bounds){if(s==null||s.coordinates==null||s.coordinates.length!=boxes.all().size()*6)return false;int i=0;for(var b:boxes.all()){int[] c={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};for(int j=0;j<6;j++){int old=s.coordinates[i++],a=j%3;if(old==s.bounds[a]){if(c[j]!=bounds[a])return false;}else if(old==s.bounds[a+3]){if(c[j]!=bounds[a+3])return false;}else if(old!=c[j])return false;}}return true;}
 private static final int[] Q={4,0,1,5,2,6,7,3,3,1,0,2,6,4,5,7,2,0,4,6,7,5,1,3},T={0,1,2,2,3,0};
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes,boolean lines){
 int count=boxes.all().size();if(lines||count<4096||count>250000)return IndexedDefaultBoxMesh.build(renderer,boxes,lines);
 int mx=Integer.MAX_VALUE,my=mx,mz=mx;long xx=Long.MIN_VALUE,yy=xx,zz=xx;
 for(var b:boxes.all()){if(b.getClass()!=LittleBox.class)return IndexedDefaultBoxMesh.build(renderer,boxes,lines);mx=Math.min(mx,b.minX);my=Math.min(my,b.minY);mz=Math.min(mz,b.minZ);xx=Math.max(xx,b.maxX);yy=Math.max(yy,b.maxY);zz=Math.max(zz,b.maxZ);}
 if(xx-mx>=2097152||yy-my>=2097152||zz-mz>=2097152)return IndexedDefaultBoxMesh.build(renderer,boxes,lines);
 int[] bounds={mx,my,mz,(int)xx,(int)yy,(int)zz};var state=ready;boolean hit=match(state,boxes,bounds);if(!hit)for(Object saved:DefaultWallLayoutHistory.get()){if(saved!=state&&saved instanceof Layout candidate&&match(candidate,boxes,bounds)){state=candidate;hit=true;break;}}int[] corners;int vertices;
 if(hit){corners=state.corners;vertices=state.vertices;}else{
 if(busy.compareAndSet(false,true)){
  int[] snapshot=new int[count*6];int at=0;for(var box:boxes.all()){snapshot[at++]=box.minX;snapshot[at++]=box.minY;snapshot[at++]=box.minZ;snapshot[at++]=box.maxX;snapshot[at++]=box.maxY;snapshot[at++]=box.maxZ;}
  worker.execute(()->{try{ready=layout(snapshot,bounds);DefaultWallLayoutHistory.remember(ready,snapshot.length/6);}finally{busy.set(false);}});
 }
 return IndexedDefaultBoxMesh.build(renderer,boxes,lines);
 }

 boolean small=vertices<=65536;int stride=small?2:4;var owner=new ByteBufferBuilder(vertices*16+count*36*stride);
 try{long ptr=owner.reserve(vertices*16);float scale=(float)boxes.grid.pixelLength;int next=0,boxIndex=0;for(var box:boxes.all()){while(next<vertices&&state.first[next]/8==boxIndex){int c=state.first[next]%8;long at=ptr+(long)next*16;MemoryUtil.memPutFloat(at,((c&1)==0?box.minX:box.maxX)*scale);MemoryUtil.memPutFloat(at+4,((c&2)==0?box.minY:box.maxY)*scale);MemoryUtil.memPutFloat(at+8,((c&4)==0?box.minZ:box.maxZ)*scale);MemoryUtil.memPutInt(at+12,-1);next++;}boxIndex++;}var vb=owner.build();ptr=owner.reserve(count*36*stride);
 MemoryUtil.memByteBuffer(ptr,state.indices.length).put(state.indices);
 var ib=owner.build();var mesh=new MeshData(vb,new MeshData.DrawState(DefaultVertexFormat.POSITION_COLOR,vertices,count*36,VertexFormat.Mode.QUADS,small?VertexFormat.IndexType.SHORT:VertexFormat.IndexType.INT));((MeshDataAccessor)(Object)mesh).littleperformanceimprovements$setIndexBuffer(ib);((MeshDataExtender)(Object)mesh).keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,owner,mesh);
 }catch(Throwable e){owner.close();throw e;}
 }

 private static Layout layout(int[] xyz,int[] bounds){var state=new Layout();int count=xyz.length/6;var map=new Long2IntOpenHashMap(count*2);map.defaultReturnValue(-1);int[] corners=new int[count*8];int cursor=0,vertices=0;var first=new IntArrayList();
 for(int b=0;b<xyz.length;b+=6)for(int c=0;c<8;c++){int x=xyz[b+((c&1)==0?0:3)],y=xyz[b+((c&2)==0?1:4)],z=xyz[b+((c&4)==0?2:5)];long key=((long)(x-bounds[0])<<42)|((long)(y-bounds[1])<<21)|(z-bounds[2]);int index=map.get(key);if(index<0){index=vertices++;map.put(key,index);first.add(cursor);}corners[cursor++]=index;}
 state.bounds=bounds;state.coordinates=xyz;state.corners=corners;state.vertices=vertices;state.first=first.toIntArray();boolean small=vertices<=65536;var bytes=java.nio.ByteBuffer.allocate(count*36*(small?2:4)).order(java.nio.ByteOrder.nativeOrder());for(int box=0;box<count;box++)for(int face=0;face<6;face++)for(int c:T){int index=corners[box*8+Q[face*4+c]];if(small)bytes.putShort((short)index);else bytes.putInt(index);}state.indices=bytes.array();return state;
 }
}
