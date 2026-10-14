package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.BoxFace;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
/** A box with one corner collapsed onto its neighbour has exact planar boundary polygons.
 * Reconstruct those polygons without the cache's floating-point plane clipping.
 * Sloping faces and arbitrary transformed boxes retain their original geometry. */
public final class CornerBoundaryPreview {
 public static LittleBoxes apply(LittleBoxes boxes,String purpose){return "preview".equals(purpose)?repair(boxes):boxes;}
 public static LittleBoxes repair(LittleBoxes boxes){
  if(boxes==null||boxes.all().size()!=1)return boxes;
  for(var b:boxes.all())if(b instanceof LittleTransformableBox t){
   var cache=t.requestCache();var corners=t.getTiltedCorners();
   int moved=0;boolean collapsed=true;
   for(var corner:team.creative.creativecore.common.util.math.box.BoxCorner.values()){var v=corners[corner.ordinal()];int changed=(v.x!=b.get(corner.x)?1:0)+(v.y!=b.get(corner.y)?1:0)+(v.z!=b.get(corner.z)?1:0);if(changed!=0){moved++;if(changed!=1||!(v.x==b.minX||v.x==b.maxX)||!(v.y==b.minY||v.y==b.maxY)||!(v.z==b.minZ||v.z==b.maxZ))collapsed=false;}}
   if(moved!=1||!collapsed)continue;
   for(var face:BoxFace.values()){
    var f=face.facing;var fc=cache.get(f);if(fc==null)continue;
    if(fc.tiltedStrip1!=null&&fc.tiltedStrip2!=null){fc.axisStrips=new ArrayList<>();continue;}
    List<Vec3f> points=new ArrayList<>(4);
    for(var corner:face.corners){var v=corners[corner.ordinal()];if(f.axis.get(v.x,v.y,v.z)!=b.get(f)){continue;}boolean duplicate=false;for(var q:points)if(equal(v,q)){duplicate=true;break;}if(!duplicate)points.add(v);}
    if(points.size()<3)continue;
    var a=points.get(0);var c=points.get(1);var d=points.get(2);
    double ux=c.x-a.x,uy=c.y-a.y,uz=c.z-a.z,vx=d.x-a.x,vy=d.y-a.y,vz=d.z-a.z;
    double normal=f.axis.get(uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx);if(normal==0)continue;
    if((normal>0)!=f.positive)Collections.reverse(points);
    fc.axisStrips=new ArrayList<>(List.of(new VectorFan((Vec3f[])points.toArray(Vec3f[]::new))));
   }
  }
  return boxes;
 }
 private static boolean equal(Vec3f a,Vec3f b){return a.x==b.x&&a.y==b.y&&a.z==b.z;}
}
