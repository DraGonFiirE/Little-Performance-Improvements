package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;import java.nio.*;import com.mojang.blaze3d.vertex.*;import org.lwjgl.system.MemoryUtil;import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;import it.unimi.dsi.fastutil.ints.IntArrayList;import team.creative.littletiles.client.render.overlay.PreviewRenderer;import team.creative.littletiles.client.render.mc.MeshDataExtender;import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;import team.creative.littletiles.common.math.box.collection.LittleBoxes;
public final class SphereModeSharing {
 private static final ThreadLocal<Boolean> building=ThreadLocal.withInitial(()->false);
 private static final Set<LittleBoxes> spheres=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
 public static LittleBoxes mark(LittleBoxes boxes){building.set(boxes!=null);if(boxes!=null)spheres.add(boxes);return boxes;}
 public static PreviewRenderer.BoxRenderResult compact(PreviewRenderer.BoxRenderResult source,boolean grid,boolean onlyMarked){
  boolean sphere=building.get();building.remove();
  if(source==null||source.data()==null||(onlyMarked&&!sphere&&!spheres.contains(source.boxes())))return source;
  var mesh=source.data();var state=mesh.drawState();int count=state.vertexCount(),stride=state.format().getVertexSize();
  if(count<4096||count%4!=0||state.mode()!=VertexFormat.Mode.QUADS||mesh.indexBuffer()!=null||(stride!=16&&stride!=24))return source;
  var input=mesh.vertexBuffer();var paired=new java.util.BitSet(count/4);int saved=0;int[] pairs={2,3,6,7,2,4,0,6};
  for(int i=0;i+8<=count;){boolean pair=true;for(int q=0;q<8&&pair;q+=2)for(int k=0;k<stride;k+=4)if(input.getInt((i+pairs[q])*stride+k)!=input.getInt((i+pairs[q+1])*stride+k)){pair=false;break;}if(pair){paired.set(i/4);saved+=4;i+=8;}else i+=4;}
  if(saved>0){int vertices=count-saved;var buffer=new ByteBufferBuilder(vertices*stride);try{long dest=buffer.reserve(vertices*stride),address=MemoryUtil.memAddress(input);int at=0;for(int i=0;i<count;){if(paired.get(i/4)){MemoryUtil.memCopy(address+(long)i*stride,dest+(long)at*stride,3L*stride);MemoryUtil.memCopy(address+(long)(i+5)*stride,dest+(long)(at+3)*stride,stride);i+=8;}else{MemoryUtil.memCopy(address+(long)i*stride,dest+(long)at*stride,4L*stride);i+=4;}at+=4;}var packed=new MeshData(buffer.build(),new MeshData.DrawState(state.format(),vertices,vertices/4*6,VertexFormat.Mode.QUADS,VertexFormat.IndexType.least(vertices)));((MeshDataExtender)(Object)packed).keepAlive(true);if(grid)DefaultCylinderGrid2.mark(packed);var result=new PreviewRenderer.BoxRenderResult(source.boxes(),source.pos(),buffer,packed);source.close();return result;}catch(Throwable t){buffer.close();throw t;}}

  var bytes=mesh.vertexBuffer();long src=MemoryUtil.memAddress(bytes);
  int capacity=1;while(capacity<(long)count*4/3&&capacity<(1<<29))capacity<<=1;
  int[] table=new int[capacity],originals=new int[count],indices=new int[count];int mask=capacity-1,unique=0;
  for(int i=0;i<count;i++){
   if((i&65535)==0)OutlineWorkChunks.check();
   long at=src+(long)i*stride,a=MemoryUtil.memGetLong(at),b=MemoryUtil.memGetLong(at+8),c=stride==24?MemoryUtil.memGetLong(at+16):0;
   long hash=a*0x9e3779b97f4a7c15L+Long.rotateLeft(b,23)*0xc2b2ae3d27d4eb4fL+c*0x165667b19e3779f9L;hash^=hash>>>33;hash*=0xff51afd7ed558ccdL;hash^=hash>>>33;
   int slot=(int)hash&mask;
   while(true){int prior=table[slot]-1;if(prior<0){table[slot]=i+1;originals[unique]=i;indices[i]=unique++;break;}long other=src+(long)prior*stride;
    if(a==MemoryUtil.memGetLong(other)&&b==MemoryUtil.memGetLong(other+8)&&(stride==16||c==MemoryUtil.memGetLong(other+16))){indices[i]=indices[prior];break;}slot=(slot+1)&mask;
   }
  }
  if(unique>=count*0.9)return source;
  int indexCount=count/4*6;long total=(long)unique*stride+(long)indexCount*4;if(total>Integer.MAX_VALUE){return source;}
  var owner=new ByteBufferBuilder((int)total);try{long target=owner.reserve(unique*stride);for(int i=0;i<unique;i++){long from=src+(long)originals[i]*stride,to=target+(long)i*stride;MemoryUtil.memPutLong(to,MemoryUtil.memGetLong(from));MemoryUtil.memPutLong(to+8,MemoryUtil.memGetLong(from+8));if(stride==24)MemoryUtil.memPutLong(to+16,MemoryUtil.memGetLong(from+16));}var vb=owner.build();long at=owner.reserve(indexCount*4);int cursor=0;for(int i=0;i<count;i+=4){long to=at+(long)cursor*4;MemoryUtil.memPutInt(to,indices[i]);MemoryUtil.memPutInt(to+4,indices[i+1]);MemoryUtil.memPutInt(to+8,indices[i+2]);MemoryUtil.memPutInt(to+12,indices[i+2]);MemoryUtil.memPutInt(to+16,indices[i+3]);MemoryUtil.memPutInt(to+20,indices[i]);cursor+=6;}var ib=owner.build();var packed=new MeshData(vb,new MeshData.DrawState(state.format(),unique,indexCount,VertexFormat.Mode.QUADS,VertexFormat.IndexType.INT));((MeshDataAccessor)(Object)packed).littleperformanceimprovements$setIndexBuffer(ib);((MeshDataExtender)(Object)packed).keepAlive(true);if(grid)DefaultCylinderGrid2.mark(packed);var result=new PreviewRenderer.BoxRenderResult(source.boxes(),source.pos(),owner,packed);source.close();return result;}catch(Throwable t){owner.close();throw t;}
 }
}
