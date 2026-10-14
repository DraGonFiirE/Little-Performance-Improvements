package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
/** Exact voxel boundaries of the sphere's three central sections, in linear time. */
public final class ExactSphereDrag {
 public static LittleBoxes build(ShapeSelection selection){
  var box=selection.getOverallBox();int[] min={box.minX,box.minY,box.minZ},size={box.maxX-box.minX,box.maxY-box.minY,box.maxZ-box.minZ};
  var result=new LittleBoxesSimple(selection.pos,selection.grid);
  if(size[0]<=0||size[1]<=0||size[2]<=0)return result;
  double[] center=new double[3],radius2=new double[3];
  for(int a=0;a<3;a++){center[a]=(size[a]-1)/2.0;radius2[a]=Math.pow(Math.max(1,size[a]/2),2);}
  var pixels=new SphereRingPixels(min,size);
  for(int normal=0;normal<3;normal++)for(int swap=0;swap<2;swap++){
   int u=(normal+1+swap)%3,v=(normal+2-swap)%3;
   int[] p={(size[0]-1)/2,(size[1]-1)/2,(size[2]-1)/2};
   for(int coordinate=0;coordinate<size[u];coordinate++){
    if((coordinate&255)==0)PreviewBuildCancellation.check();p[u]=coordinate;
    double remaining=1-Math.pow(p[normal]-center[normal],2)/radius2[normal]-Math.pow(p[u]-center[u],2)/radius2[u];
    if(remaining< -1e-12)continue;
    double extent=Math.sqrt(Math.max(0,remaining)*radius2[v]);
    int low=Math.max(0,(int)Math.ceil(center[v]-extent)),high=Math.min(size[v]-1,(int)Math.floor(center[v]+extent));
    while(low>0&&inside(p,v,low-1,center,radius2))low--;
    while(high+1<size[v]&&inside(p,v,high+1,center,radius2))high++;
    while(low<=high&&!inside(p,v,low,center,radius2))low++;
    while(high>=low&&!inside(p,v,high,center,radius2))high--;
    if(low>high)continue;
    p[v]=low;pixels.add(min[0]+p[0],min[1]+p[1],min[2]+p[2]);
    p[v]=high;pixels.add(min[0]+p[0],min[1]+p[1],min[2]+p[2]);
   }
  }
  for(int[] p:pixels.finish())result.add(new LittleBox(p[0],p[1],p[2],p[3],p[4],p[5]));return result;
 }
 private static boolean inside(int[] p,int axis,int coordinate,double[] center,double[] radius2){
  double x=(axis==0?coordinate:p[0])-center[0],y=(axis==1?coordinate:p[1])-center[1],z=(axis==2?coordinate:p[2])-center[2];
  return Math.pow(x,2)/radius2[0]+Math.pow(y,2)/radius2[1]+Math.pow(z,2)/radius2[2]<=1;
 }
}
