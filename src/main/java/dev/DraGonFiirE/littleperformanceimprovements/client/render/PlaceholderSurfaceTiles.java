package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.nio.ByteBuffer;import java.util.*;
import com.mojang.blaze3d.vertex.BufferBuilder;
import team.creative.littletiles.common.math.box.LittleBox;
/** Tiles only the six outer surfaces, never iterates the enclosed block volume. */
public final class PlaceholderSurfaceTiles {
 public static void emit(BufferBuilder builder,ByteBuffer source,int count,LittleBox box,float scale){
  float[] min={box.minX*scale,box.minY*scale,box.minZ*scale},max={box.maxX*scale,box.maxY*scale,box.maxZ*scale};
  for(int start=0;start<count;start+=4){float[][] vertices=new float[4][9];for(int i=0;i<4;i++){int at=(start+i)*24;for(int d=0;d<5;d++)vertices[i][d]=source.getFloat(at+d*4);for(int d=0;d<4;d++)vertices[i][5+d]=source.get(at+20+d)&255;}
   int normal=-1;for(int d=0;d<3;d++){boolean flat=true;for(int i=1;i<4;i++)flat&=Math.abs(vertices[i][d]-vertices[0][d])<1e-6;if(flat&&(Math.abs(vertices[0][d])<1e-6||Math.abs(vertices[0][d]-1)<1e-6)){normal=d;break;}}
   if(normal<0){ // Unusual block-model detail: keep its shape inside the box.
    for(float[] p:vertices){float[] v=p.clone();for(int d=0;d<3;d++)v[d]=min[d]+v[d]*(max[d]-min[d]);vertex(builder,v);}continue;
   }
   int a=(normal+1)%3,b=(normal+2)%3;float face=vertices[0][normal]<.5f?min[normal]:max[normal];
   for(int x=(int)Math.floor(min[a]);x<(int)Math.ceil(max[a]);x++)for(int y=(int)Math.floor(min[b]);y<(int)Math.ceil(max[b]);y++){
    List<float[]> poly=new ArrayList<>(Arrays.asList(vertices));poly=clip(poly,a,Math.max(0,min[a]-x),true);poly=clip(poly,a,Math.min(1,max[a]-x),false);poly=clip(poly,b,Math.max(0,min[b]-y),true);poly=clip(poly,b,Math.min(1,max[b]-y),false);
    if(poly.size()<3)continue;
    for(int i=0;i<poly.size();i++){float[] v=poly.get(i).clone();v[a]+=x;v[b]+=y;v[normal]=face;poly.set(i,v);}
    if(poly.size()==4){for(float[] v:poly)vertex(builder,v);}else for(int i=1;i+1<poly.size();i++){vertex(builder,poly.get(0));vertex(builder,poly.get(i));vertex(builder,poly.get(i+1));vertex(builder,poly.get(i+1));}
   }
  }
 }
 static List<float[]> clip(List<float[]> points,int axis,float plane,boolean low){List<float[]> out=new ArrayList<>();if(points.isEmpty())return out;float[] prev=points.getLast();float pd=prev[axis]-plane;boolean pin=low?pd>=0:pd<=0;for(float[] v:points){float d=v[axis]-plane;boolean in=low?d>=0:d<=0;if(in!=pin){float t=pd/(pd-d);float[] p=new float[9];for(int k=0;k<9;k++)p[k]=prev[k]+t*(v[k]-prev[k]);out.add(p);}if(in)out.add(v);prev=v;pd=d;pin=in;}return out;}
 static void vertex(BufferBuilder b,float[] v){b.addVertex(v[0],v[1],v[2]).setUv(v[3],v[4]).setColor(Math.round(v[5]),Math.round(v[6]),Math.round(v[7]),Math.round(v[8]));}
}
