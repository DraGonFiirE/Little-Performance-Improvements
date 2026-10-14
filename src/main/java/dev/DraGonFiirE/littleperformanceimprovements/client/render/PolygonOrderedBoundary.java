package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.grid.LittleGrid;
public final class PolygonOrderedBoundary {
 private static int at(LittleGrid g,double b,double d,double step,double offset,int j){return PolygonFastGrid.convert(g,(b+d*(j*step))+offset);}
 public static int end(LittleGrid g,double base,double direction,double step,double offset,int j,int last,int value){
  if(direction==0)return last;
  double boundary=(direction>0?(double)value+1:value)-0.000005;
  double crossing=(boundary-(base+offset)*g.count)/(direction*step*g.count);
  int candidate=Math.max(j,Math.min(last,(int)Math.floor(crossing)));
  int checks=0;
  while(candidate>j&&at(g,base,direction,step,offset,candidate)!=value){candidate--;if(++checks==4)return search(g,base,direction,step,offset,j,last,value);}
  while(candidate<last&&at(g,base,direction,step,offset,candidate+1)==value){candidate++;if(++checks==4)return search(g,base,direction,step,offset,j,last,value);}
  return candidate;
 }
 private static int search(LittleGrid g,double b,double d,double step,double offset,int j,int last,int value){
  int lo=j+1,hi=last+1;while(lo<hi){int m=(lo+hi)>>>1;if(at(g,b,d,step,offset,m)==value)lo=m+1;else hi=m;}return lo-1;
 }
}


