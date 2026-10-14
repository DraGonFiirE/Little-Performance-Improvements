package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import dev.DraGonFiirE.littleperformanceimprovements.performance.*;
/** Merge identical filled columns before emitting boxes; never approximate the volume. */
public final class FastFilledSphere {
 private record Run(int y0,int y1,int z0,int z1){}
 public static void build(LittleBoxes target,LittleBox bounds){
  int sx=bounds.maxX-bounds.minX,sy=bounds.maxY-bounds.minY,sz=bounds.maxZ-bounds.minZ;
  var columns=SphereVoxelColumns.ellipsoid(sx,sy,sz);
  Map<Run,LittleBox> previous=new HashMap<>(),current=new HashMap<>();
  for(int x=0;x<sx;x++){
   PreviewBuildCancellation.check();current.clear();
   for(int y=0;y<sy;){int index=columns.index(x,y);if(!columns.present(index)){y++;continue;}
    int lo=columns.minimum(index),hi=columns.maximum(index),end=y+1;
    while(end<sy){int next=columns.index(x,end);if(!columns.present(next)||columns.minimum(next)!=lo||columns.maximum(next)!=hi)break;end++;}
    var key=new Run(y,end,lo,hi+1);var box=previous.remove(key);
    if(box==null)box=new LittleBox(bounds.minX+x,bounds.minY+y,bounds.minZ+lo,bounds.minX+x+1,bounds.minY+end,bounds.minZ+hi+1);else box.maxX=bounds.minX+x+1;
    current.put(key,box);y=end;
   }
   for(var box:previous.values())target.add(box);
   var swap=previous;previous=current;current=swap;
  }
  for(var box:previous.values())target.add(box);
  SphereDirectFaces.mark(target,bounds,columns);
 }
}
