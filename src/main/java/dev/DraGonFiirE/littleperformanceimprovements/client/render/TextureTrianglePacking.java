package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import java.nio.*;import java.util.*;
import org.lwjgl.system.MemoryUtil;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TexturedQuadMeshMerger.MergedMesh;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.TextureDrawLayout.SourceSpan;
/** Combines only byte-identical shared triangle endpoints, including UV and tint. */
public final class TextureTrianglePacking {
 private static boolean vertex(ByteBuffer b,int a,int c){return b.getLong(a)==b.getLong(c)&&b.getLong(a+8)==b.getLong(c+8)&&b.getLong(a+16)==b.getLong(c+16);}
 private static boolean pair(ByteBuffer b,int p,int end){return p+192<=end&&vertex(b,p+48,p+72)&&vertex(b,p+144,p+168)&&vertex(b,p+48,p+96)&&vertex(b,p,p+144);}
 public static MergedMesh finish(MergedMesh previous,MeshData source,List<SourceSpan> sourceSpans){
  MeshData input=previous==null?source:previous.mesh();List<SourceSpan> spans=previous==null?sourceSpans:previous.spans();var state=input.drawState();
  if(state.mode()!=VertexFormat.Mode.QUADS||!state.format().equals(DefaultVertexFormat.POSITION_TEX_COLOR)||state.vertexCount()%4!=0||input.indexBuffer()!=null||spans==null||spans.isEmpty())return previous;
  long required=(long)state.vertexCount()*24;ByteBuffer in=input.vertexBuffer();if(required<=0||required>in.limit()||required>Integer.MAX_VALUE-8)return previous;
  int expected=0;for(SourceSpan s:spans){if(s.firstVertex()!=expected||s.endVertex()<=expected||(s.firstVertex()&3)!=0||(s.endVertex()&3)!=0||s.endVertex()>state.vertexCount())return previous;expected=s.endVertex();}if(expected!=state.vertexCount())return previous;
  int bytes=0;for(SourceSpan s:spans){OutlineWorkChunks.check();int end=s.endVertex()*24;for(int p=s.firstVertex()*24;p<end;){if((p&65535)==0)OutlineWorkChunks.check();boolean merge=pair(in,p,end);bytes+=96;p+=merge?192:96;}}
  if(bytes==required)return previous;
  ByteBufferBuilder owner=new ByteBufferBuilder(bytes);
  try{
   ByteBuffer out=MemoryUtil.memByteBuffer(owner.reserve(bytes),bytes);int target=0;List<SourceSpan> compactSpans=new ArrayList<>(spans.size());
   for(SourceSpan s:spans){OutlineWorkChunks.check();int first=target/24,end=s.endVertex()*24;for(int p=s.firstVertex()*24;p<end;){if((p&65535)==0)OutlineWorkChunks.check();boolean merge=pair(in,p,end);if(merge){out.put(target,in,p,72);out.put(target+72,in,p+120,24);}else out.put(target,in,p,96);target+=96;p+=merge?192:96;}
    compactSpans.add(new SourceSpan(first,target/24,s.minX(),s.minY(),s.minZ(),s.maxX(),s.maxY(),s.maxZ()));
   }
   int vertices=bytes/24;MeshData result=new MeshData(owner.build(),new MeshData.DrawState(state.format(),vertices,vertices/4*6,VertexFormat.Mode.QUADS,VertexFormat.IndexType.least(vertices)));
   MergedMesh packed=new MergedMesh(owner,result,compactSpans);
   if(previous!=null)previous.close();
   return packed;
  }catch(Throwable t){owner.close();throw t;}
 }
}
