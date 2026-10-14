package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.Arrays;
/** Exact native cylinder occupancy, emitted as merged row rectangles. */
public final class CylinderDragRows {
 public static int[] build(int u,int v,boolean hollow,int thickness){
  if(u<=0||v<=0)return new int[0];
  double ou=Math.pow(Math.max(1.0,u/2.0),2),ov=Math.pow(Math.max(1.0,v/2.0),2);
  hollow=hollow&&u>2*thickness&&v>2*thickness;
  double iu=Math.pow(Math.max(1.0,(u-thickness-1)/2.0),2),iv=Math.pow(Math.max(1.0,(v-thickness-1)/2.0),2);
  int[] result=new int[Math.multiplyExact(u,8)];int count=0,previousA=-1,previousB=-1;
  for(int row=0;row<u;row++){
   double d=row-u/2+(u%2==0?0.5:0);
   int outer=first(v,Math.pow(d,2)/ou,ov);
   int inner=hollow?first(v,Math.pow(d,2)/iu,iv):v;
   int a=outer,b=v-outer,c=0,e=0;
   if(inner<v){b=Math.min(b,inner);c=Math.max(outer,v-inner);e=v-outer;}
   int nextA=-1,nextB=-1;
   if(a<b){nextA=matching(result,previousA,previousB,row,a,b);if(nextA<0){nextA=count;result[count++]=row;result[count++]=a;result[count++]=row+1;result[count++]=b;}else result[nextA+2]=row+1;}
   if(c<e){nextB=matching(result,previousA,previousB,row,c,e);if(nextB<0){nextB=count;result[count++]=row;result[count++]=c;result[count++]=row+1;result[count++]=e;}else result[nextB+2]=row+1;}
   previousA=nextA;previousB=nextB;
  }
  return Arrays.copyOf(result,count);
 }
 private static int matching(int[] r,int a,int b,int row,int lo,int hi){
  if(a>=0&&r[a+2]==row&&r[a+1]==lo&&r[a+3]==hi)return a;
  if(b>=0&&r[b+2]==row&&r[b+1]==lo&&r[b+3]==hi)return b;
  return -1;
 }
 private static int first(int n,double other,double denominator){
  int low=0,limit=(n+1)/2,high=limit;
  while(low<high){int mid=(low+high)>>>1;double d=mid-n/2+(n%2==0?0.5:0);
   // Retain native arithmetic and <= boundary, including single-pixel axes.
   if(other+Math.pow(d,2)/denominator<=1)high=mid;else low=mid+1;
  }
  return low==limit?n:low;
 }
}
