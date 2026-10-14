package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.grid.LittleGrid;
public final class PolygonSpanSampling {
 private static int at(LittleGrid g,double b,double d,double step,double offset,int j){return PolygonFastGrid.convert(g,(b+d*(j*step))+offset);}
 public static void fill(PolygonSpanRows rows,LittleGrid grid,double[] a,double[] b,double[] c,int nu,int nv){
  boolean swap=(double)nu*(Math.abs(c[rows.v])+Math.abs(c[rows.w]))>(double)nv*(Math.abs(b[rows.v])+Math.abs(b[rows.w]));
  int outer=swap?nv:nu,inner=swap?nu:nv;
  double so=1.0/(outer-1),si=1.0/(inner-1);
  double[] d=swap?b:c;
  double du=d[rows.u],dv=d[rows.v],dw=d[rows.w];
  boolean contiguous=Math.abs(du)*si*grid.count<1;
  for(int i=0;i<outer;i++){
   double s=i*so;if(!(s<=1))continue;
   int low=0,high=inner;while(low<high){int mid=(low+high)>>>1;double t=mid*si;if(t<=1&&s+t<=1)low=mid+1;else high=mid;}
   int last=low-1;
   double bu=swap?a[rows.u]:a[rows.u]+b[rows.u]*s,bv=swap?a[rows.v]:a[rows.v]+b[rows.v]*s,bw=swap?a[rows.w]:a[rows.w]+b[rows.w]*s;
   double ou=swap?c[rows.u]*s:0,ov=swap?c[rows.v]*s:0,ow=swap?c[rows.w]*s:0;
   int v=0,w=0,vEnd=-1,wEnd=-1;
   for(int j=0;j<=last;){
    if(j>vEnd){v=at(grid,bv,dv,si,ov,j);vEnd=contiguous?PolygonOrderedBoundary.end(grid,bv,dv,si,ov,j,last,v):j;}
    if(j>wEnd){w=at(grid,bw,dw,si,ow,j);wEnd=contiguous?PolygonOrderedBoundary.end(grid,bw,dw,si,ow,j,last,w):j;}
    int end=Math.min(vEnd,wEnd);
    int u0=at(grid,bu,du,si,ou,j),u1=at(grid,bu,du,si,ou,end);
    rows.addRange(Math.min(u0,u1),Math.max(u0,u1)+1,v,w);j=end+1;
   }
  }
 }
}

