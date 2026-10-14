package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;import org.joml.Vector3f;
import java.lang.ref.WeakReference;
import java.util.*;
/** Exact face-facing rejection for small, closed convex outline meshes. */
public final class ConvexOutlineVisibility {
 private static WeakReference<MeshData> source=new WeakReference<>(null);
 private static double[][] planes;private static float[][] vertices;private static int[][] adjacent;private static long lastMask=Long.MIN_VALUE;private static VertexBuffer filtered;private static boolean supported;
 public static VertexBuffer select(VertexBuffer original,MeshData lines,MeshData surface,Matrix4f view){
  if(source.get()!=lines){source=new WeakReference<>(lines);supported=false;lastMask=Long.MIN_VALUE;prepare(lines,surface);}
  if(!supported)return original;
  var camera=new Matrix4f(view).invert().transformPosition(new Vector3f());long mask=0;
  for(int i=0;i<adjacent.length;i++)for(int face:adjacent[i]){var p=planes[face];if(p[0]*camera.x+p[1]*camera.y+p[2]*camera.z+p[3]>=-0.00002){mask|=1L<<i;break;}}
  if(mask!=lastMask){if(filtered==null)filtered=new VertexBuffer(VertexBuffer.Usage.STATIC);var owner=new ByteBufferBuilder(Math.max(64,Long.bitCount(mask)*32));try{var builder=new BufferBuilder(owner,VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);for(int i=0;i<adjacent.length;i++)if((mask&(1L<<i))!=0)for(int j=0;j<2;j++){float[] v=vertices[i*2+j];builder.addVertex(v[0],v[1],v[2]).setColor(255,255,255,255);}var mesh=builder.build();if(mesh==null)return original;filtered.bind();filtered.upload(mesh);lastMask=mask;}finally{owner.close();}}
  return filtered;
 }
 private static void prepare(MeshData lines,MeshData surface){
  if(surface==null||surface.drawState().vertexCount()>128||surface.drawState().mode()!=VertexFormat.Mode.QUADS)return;
  int stride=lines.drawState().format().getVertexSize(),step=lines.drawState().mode()==VertexFormat.Mode.LINES?2:1,count=lines.drawState().vertexCount()/step;if(count>126||count%2!=0)return;
  var bytes=lines.vertexBuffer().duplicate().order(java.nio.ByteOrder.nativeOrder());vertices=new float[count][3];for(int i=0;i<count;i++)for(int c=0;c<3;c++)vertices[i][c]=bytes.getFloat(i*step*stride+c*4);
  var s=surface.vertexBuffer().duplicate().order(java.nio.ByteOrder.nativeOrder());int size=surface.drawState().vertexCount(),ss=surface.drawState().format().getVertexSize();double[][] points=new double[size][3];double[] center=new double[3];for(int i=0;i<size;i++)for(int c=0;c<3;c++){points[i][c]=s.getFloat(i*ss+c*4);center[c]+=points[i][c]/size;}
  var list=new ArrayList<double[]>();for(int i=0;i<size;i+=4)for(int tri=0;tri<2;tri++){var a=points[i+(tri==0?0:2)];var b=points[i+(tri==0?1:3)];var c=points[i+(tri==0?2:0)];double x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2],u=c[0]-a[0],v=c[1]-a[1],w=c[2]-a[2];double nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u,len=Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-10)continue;nx/=len;ny/=len;nz/=len;double d=-(nx*a[0]+ny*a[1]+nz*a[2]);if(nx*center[0]+ny*center[1]+nz*center[2]+d>0){nx=-nx;ny=-ny;nz=-nz;d=-d;}for(var pt:points)if(nx*pt[0]+ny*pt[1]+nz*pt[2]+d>0.00002)return;double[] plane={nx,ny,nz,d};boolean duplicate=false;for(var q:list)if(Math.abs(q[0]-nx)+Math.abs(q[1]-ny)+Math.abs(q[2]-nz)<1e-6&&Math.abs(q[3]-d)<0.00002){duplicate=true;break;}if(!duplicate)list.add(plane);}
  if(list.size()<4)return;planes=list.toArray(double[][]::new);adjacent=new int[count/2][];for(int i=0;i<adjacent.length;i++){var matches=new ArrayList<Integer>();for(int f=0;f<planes.length;f++){var q=planes[f];boolean on=true;for(int j=0;j<2;j++){var pt=vertices[i*2+j];if(Math.abs(q[0]*pt[0]+q[1]*pt[1]+q[2]*pt[2]+q[3])>0.00002){on=false;break;}}if(on)matches.add(f);}if(matches.size()<2)return;adjacent[i]=matches.stream().mapToInt(Integer::intValue).toArray();}supported=true;
 }
}

