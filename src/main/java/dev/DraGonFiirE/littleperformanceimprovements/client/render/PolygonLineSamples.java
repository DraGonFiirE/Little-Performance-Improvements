package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.common.grid.LittleGrid;
public final class PolygonLineSamples {
 public static boolean sample(PolygonPixelRows pixels,LittleGrid grid,Vec3d a,Vec3d b,Vec3d c,int n,int m){
  if(n!=m||b.x!=c.x||b.y!=c.y||b.z!=c.z)return false;
  if(n<=1)return true; // Native's zero/NaN sampling produces no pixels.
  double step=1.0/(n-1);
  double ex=32*Math.ulp(Math.abs(a.x)+Math.abs(b.x)+1);
  double ey=32*Math.ulp(Math.abs(a.y)+Math.abs(b.y)+1);
  double ez=32*Math.ulp(Math.abs(a.z)+Math.abs(b.z)+1);
  for(int k=0;k<n;k++){
   double t=k*step,x=a.x+b.x*t,y=a.y+b.y*t,z=a.z+b.z*t;
   // All (i,j) with i+j=k map to this sample unless floating-point
   // rounding straddles a grid boundary. Preserve those cases explicitly.
   if(k==n-1||grid.toGrid(x-ex)!=grid.toGrid(x+ex)||grid.toGrid(y-ey)!=grid.toGrid(y+ey)||grid.toGrid(z-ez)!=grid.toGrid(z+ez)){
    for(int i=0;i<=k;i++){double s=i*step,r=(k-i)*step;
     if(s<=1&&r<=1&&s+r<=1)pixels.add(grid.toGrid((a.x+b.x*s)+b.x*r),grid.toGrid((a.y+b.y*s)+b.y*r),grid.toGrid((a.z+b.z*s)+b.z*r));
    }
   }else if(t<=1)pixels.add(grid.toGrid(x),grid.toGrid(y),grid.toGrid(z));
  }
  return true;
 }
}
