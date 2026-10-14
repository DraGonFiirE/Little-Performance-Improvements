package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.List;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
/** Exact block clipping for axis-aligned rectangular cached faces. */
public final class TextureRectangleClip {
 public static boolean append(List<VectorFan> output,VectorFan fan,float sx,float sy,float sz,float ox,float oy,float oz,int ix,int iy,int iz){
  if(fan.count()!=4)return false;
  var a=fan.get(0);var b=fan.get(1);var c=fan.get(2);var d=fan.get(3);
  // Opposite corners of a rectangle; each edge must run along one axis.
  if(!edge(a,b)||!edge(b,c)||!edge(c,d)||!edge(d,a)||a.x+c.x!=b.x+d.x||a.y+c.y!=b.y+d.y||a.z+c.z!=b.z+d.z)return false;
  float ax=a.x*sx+ox-ix,ay=a.y*sy+oy-iy,az=a.z*sz+oz-iz,bx=b.x*sx+ox-ix,by=b.y*sy+oy-iy,bz=b.z*sz+oz-iz,cx=c.x*sx+ox-ix,cy=c.y*sy+oy-iy,cz=c.z*sz+oz-iz,dx=d.x*sx+ox-ix,dy=d.y*sy+oy-iy,dz=d.z*sz+oz-iz;
  float lx=Math.min(ax,cx),ly=Math.min(ay,cy),lz=Math.min(az,cz),hx=Math.max(ax,cx),hy=Math.max(ay,cy),hz=Math.max(az,cz);
  if(hx<0||hy<0||hz<0||lx>1||ly>1||lz>1)return true;
  double x=Math.max(0,Math.min(1,hx)-Math.max(0,lx)),y=Math.max(0,Math.min(1,hy)-Math.max(0,ly)),z=Math.max(0,Math.min(1,hz)-Math.max(0,lz));
  double area=x*y+x*z+y*z;if(4*area*area<=1e-16)return true;
  output.add(new VectorFan(new Vec3f[]{new Vec3f(clamp(ax),clamp(ay),clamp(az)),new Vec3f(clamp(bx),clamp(by),clamp(bz)),new Vec3f(clamp(cx),clamp(cy),clamp(cz)),new Vec3f(clamp(dx),clamp(dy),clamp(dz))}));return true;
 }
 private static float clamp(float f){return Math.max(0,Math.min(1,f));}
 private static boolean edge(Vec3f a,Vec3f b){return (a.x!=b.x?1:0)+(a.y!=b.y?1:0)+(a.z!=b.z?1:0)==1;}
}
