package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
/** Visits only block cells that can intersect a visible face, in original XYZ order. */
public final class TextureFaceCells {
 private final int[] regions=new int[36];
 private int count;
 public int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
 public int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
 public TextureFaceCells(RenderBox box){
  int lx=(int)Math.floor(box.minX),ly=(int)Math.floor(box.minY),lz=(int)Math.floor(box.minZ);
  int hx=(int)Math.ceil(box.maxX)-1,hy=(int)Math.ceil(box.maxY)-1,hz=(int)Math.ceil(box.maxZ)-1;
  if(box instanceof LittleRenderBoxTransformable){add(lx,ly,lz,hx,hy,hz);return;}
  for(var facing:Facing.VALUES){
   if(!box.shouldRenderFace(facing))continue;
   int x0=lx,y0=ly,z0=lz,x1=hx,y1=hy,z1=hz;
   var face=box.getFace(facing);
   if(face.hasCachedFans()){
    float ax=Float.POSITIVE_INFINITY,ay=ax,az=ax,bx=Float.NEGATIVE_INFINITY,by=bx,bz=bx;
    for(var fan:face.getCachedFans())for(int i=0;i<fan.count();i++){
     var p=fan.get(i);float x=p.x*box.getPreviewScaleX()+box.getPreviewOffX(),y=p.y*box.getPreviewScaleY()+box.getPreviewOffY(),z=p.z*box.getPreviewScaleZ()+box.getPreviewOffZ();
     ax=Math.min(ax,x);ay=Math.min(ay,y);az=Math.min(az,z);bx=Math.max(bx,x);by=Math.max(by,y);bz=Math.max(bz,z);
    }
    if(ax==Float.POSITIVE_INFINITY)continue;
    // Include boundary-touching cells: clipping is the final authority for tiny fans.
    x0=Math.max(lx,(int)Math.floor(ax));y0=Math.max(ly,(int)Math.floor(ay));z0=Math.max(lz,(int)Math.floor(az));
    x1=Math.min(hx,(int)Math.floor(bx));y1=Math.min(hy,(int)Math.floor(by));z1=Math.min(hz,(int)Math.floor(bz));
   }
   switch(facing.axis){case X -> x0=x1=facing.positive?hx:lx;case Y -> y0=y1=facing.positive?hy:ly;case Z -> z0=z1=facing.positive?hz:lz;}
   add(x0,y0,z0,x1,y1,z1);
  }
 }
 private void add(int x0,int y0,int z0,int x1,int y1,int z1){
  if(x0>x1||y0>y1||z0>z1)return;int i=count;regions[i]=x0;regions[i+1]=y0;regions[i+2]=z0;regions[i+3]=x1;regions[i+4]=y1;regions[i+5]=z1;count+=6;
  minX=Math.min(minX,x0);minY=Math.min(minY,y0);minZ=Math.min(minZ,z0);maxX=Math.max(maxX,x1);maxY=Math.max(maxY,y1);maxZ=Math.max(maxZ,z1);
 }
 public boolean empty(){return count==0;}
 public int nextZ(int x,int y,int z){int next=maxZ+1;for(int i=0;i<count;i+=6)if(x>=regions[i]&&x<=regions[i+3]&&y>=regions[i+1]&&y<=regions[i+4]&&z<=regions[i+5]){next=Math.min(next,Math.max(z,regions[i+2]));if(next==z)return z;}return next;}
}
