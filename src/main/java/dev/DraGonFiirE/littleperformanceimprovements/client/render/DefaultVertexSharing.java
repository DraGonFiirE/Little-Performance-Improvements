package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;import java.nio.*;import org.lwjgl.system.MemoryUtil;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.MeshDataAccessor;
/** Exact bounded vertex sharing for Default; preserves the complete index sequence. */
public final class DefaultVertexSharing {
 private static final int[] CORNERS={0,1,2,2,3,0};
 private static boolean equal(ByteBuffer b,int a,int c){return b.getLong(a)==b.getLong(c)&&b.getLong(a+8)==b.getLong(c+8);}
 public static BlueprintPreviewMeshCompactor.CompactedMesh compact(MeshData mesh,int boxes){
  if(((dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig)(Object)team.creative.littletiles.LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewRenderer()!=dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode.Default)return BlueprintPreviewMeshCompactor.compact(mesh,boxes);
  var s=mesh.drawState();int count=s.vertexCount();if(s.mode()!=VertexFormat.Mode.QUADS||!s.format().equals(DefaultVertexFormat.POSITION_COLOR)||count<=0||count%4!=0||mesh.indexBuffer()!=null)return null;
  ByteBuffer src=mesh.vertexBuffer();if((long)count*16>src.limit())return null;
  int batch=64;if(boxes>0&&count%boxes==0){int n=count/boxes;if(n>=4&&n<=384&&n%4==0)batch=n;}
  int[] offsets=new int[batch],mapping=new int[batch];int uniqueTotal=0;
  for(int start=0;start<count;start+=batch){OutlineWorkChunks.check();int unique=0,n=Math.min(batch,count-start);for(int j=0;j<n;j++){int p=(start+j)*16,k=0;while(k<unique&&!equal(src,p,offsets[k]))k++;if(k==unique)offsets[unique++]=p;}uniqueTotal+=unique;}
  if(uniqueTotal==count)return null;
  long vb=(long)uniqueTotal*16,ib=(long)count/4*6*4;if(vb+ib>Integer.MAX_VALUE-8)return null;
  ByteBufferBuilder owner=new ByteBufferBuilder((int)(vb+ib));
  try{
   ByteBuffer vertices=MemoryUtil.memByteBuffer(owner.reserve((int)vb),(int)vb);
   int output=0;
   for(int start=0;start<count;start+=batch){OutlineWorkChunks.check();int unique=0,n=Math.min(batch,count-start);for(int j=0;j<n;j++){int p=(start+j)*16,k=0;while(k<unique&&!equal(src,p,offsets[k]))k++;if(k==unique){offsets[unique++]=p;vertices.put((output+k)*16,src,p,16);}mapping[j]=output+k;}
    output+=unique;
   }
   var vr=owner.build();
   ByteBuffer indices=MemoryUtil.memByteBuffer(owner.reserve((int)ib),(int)ib).order(ByteOrder.nativeOrder());int idx=0;output=0;
   for(int start=0;start<count;start+=batch){OutlineWorkChunks.check();int unique=0,n=Math.min(batch,count-start);for(int j=0;j<n;j++){int p=(start+j)*16,k=0;while(k<unique&&!equal(src,p,offsets[k]))k++;if(k==unique)offsets[unique++]=p;mapping[j]=output+k;}for(int q=0;q<n;q+=4)for(int corner:CORNERS){indices.putInt(idx,mapping[q+corner]);idx+=4;}output+=unique;}
   if(output!=uniqueTotal||idx!=ib)throw new IllegalStateException("Default mesh packing count mismatch");
   var ir=owner.build();MeshData result=new MeshData(vr,new MeshData.DrawState(s.format(),output,count/4*6,s.mode(),VertexFormat.IndexType.INT));((MeshDataAccessor)(Object)result).littleperformanceimprovements$setIndexBuffer(ir);
   return new BlueprintPreviewMeshCompactor.CompactedMesh(owner,result);
  }catch(Throwable t){owner.close();throw t;}
 }
}

