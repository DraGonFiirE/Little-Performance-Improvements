package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.math.box.LittleBox;
/** Add only the portion not covered by the preceding sample, which is already in the union. */
public final class DragSurfaceDelta {
 public static void add(DragSurfaceRows rows,LittleBox b,LittleBox p){
  int[] a={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};
  if(p==null){slab(rows,a);return;}
  int[] i={Math.max(b.minX,p.minX),Math.max(b.minY,p.minY),Math.max(b.minZ,p.minZ),Math.min(b.maxX,p.maxX),Math.min(b.maxY,p.maxY),Math.min(b.maxZ,p.maxZ)};
  if(i[0]>=i[3]||i[1]>=i[4]||i[2]>=i[5]){slab(rows,a);return;}
  for(int axis=0;axis<3;axis++){
   int high=a[axis+3];a[axis+3]=i[axis];slab(rows,a);a[axis+3]=high;a[axis]=i[axis];
   int low=a[axis];a[axis]=i[axis+3];slab(rows,a);a[axis]=low;a[axis+3]=i[axis+3];
  }
 }
 private static void slab(DragSurfaceRows r,int[] b){
  if(b[0]>=b[3]||b[1]>=b[4]||b[2]>=b[5])return;
  for(int w=b[r.w];w<b[r.w+3];w++)for(int v=b[r.v];v<b[r.v+3];v++)r.addRange(b[r.u],b[r.u+3],v,w);
 }
}
