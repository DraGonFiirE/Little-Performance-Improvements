package dev.DraGonFiirE.littleperformanceimprovements.client.render;
/** Reusable rectangle difference scratch storage; no allocations per subtraction. */
public final class FaceClipBuffer {
 public int[] values=new int[64];private int[] scratch=new int[64];public int size;
 public void begin(int l,int r,int d,int u){size=4;values[0]=l;values[1]=r;values[2]=d;values[3]=u;}
 public void subtract(int cl,int cr,int cd,int cu){
  if(scratch.length<size*4)scratch=new int[Math.max(size*4,scratch.length*2)];int out=0;
  for(int i=0;i<size;i+=4){int x0=values[i],x1=values[i+1],y0=values[i+2],y1=values[i+3];int l=Math.max(x0,cl),r=Math.min(x1,cr),d=Math.max(y0,cd),u=Math.min(y1,cu);
   if(l>=r||d>=u){out=put(out,x0,x1,y0,y1);continue;}
   if(x0<l)out=put(out,x0,l,y0,y1);if(r<x1)out=put(out,r,x1,y0,y1);if(y0<d)out=put(out,l,r,y0,d);if(u<y1)out=put(out,l,r,u,y1);
  }
  int[] swap=values;values=scratch;scratch=swap;size=out;
 }
 private int put(int at,int l,int r,int d,int u){scratch[at++]=l;scratch[at++]=r;scratch[at++]=d;scratch[at++]=u;return at;}
}
