package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import java.nio.*;
import org.lwjgl.system.MemoryUtil;
/** Packs adjacent padded triangles into their identical QUADS index pattern. */
public final class SurfaceTrianglePacking {
 private static boolean vertex(ByteBuffer b,int a,int c){return b.getLong(a)==b.getLong(c)&&b.getLong(a+8)==b.getLong(c+8);}
 private static boolean pair(ByteBuffer b,int p,int end){return p+128<=end&&vertex(b,p+32,p+48)&&vertex(b,p+96,p+112)&&vertex(b,p+32,p+64)&&vertex(b,p,p+96);}
 public static BlueprintPreviewMeshCompactor.CompactedMesh compact(MeshData source,int ignored){
  var state=source.drawState();if(state.mode()!=VertexFormat.Mode.QUADS||!state.format().equals(DefaultVertexFormat.POSITION_COLOR)||state.vertexCount()%4!=0||source.indexBuffer()!=null)return null;
  long required=(long)state.vertexCount()*16;ByteBuffer in=source.vertexBuffer();if(required<=0||required>in.limit()||required>Integer.MAX_VALUE-8)return null;int end=(int)required,bytes=0;
  for(int p=0;p<end;){if((p&65535)==0)OutlineWorkChunks.check();boolean merge=pair(in,p,end);bytes+=64;p+=merge?128:64;}
  if(bytes==end)return null;
  ByteBufferBuilder owner=new ByteBufferBuilder(bytes);
  try{
   ByteBuffer out=MemoryUtil.memByteBuffer(owner.reserve(bytes),bytes);int target=0;
   for(int p=0;p<end;){if((p&65535)==0)OutlineWorkChunks.check();boolean merge=pair(in,p,end);if(merge){out.put(target,in,p,48);out.put(target+48,in,p+80,16);}else out.put(target,in,p,64);target+=64;p+=merge?128:64;}
   int vertices=bytes/16;MeshData result=new MeshData(owner.build(),new MeshData.DrawState(state.format(),vertices,vertices/4*6,VertexFormat.Mode.QUADS,VertexFormat.IndexType.least(vertices)));

   return new BlueprintPreviewMeshCompactor.CompactedMesh(owner,result);
  }catch(Throwable t){owner.close();throw t;}
 }
}
