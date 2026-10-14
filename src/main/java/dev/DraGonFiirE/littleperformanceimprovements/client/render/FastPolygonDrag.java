package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.creativecore.common.util.math.geo.Ray3d;
import team.creative.littletiles.client.tool.shaper.*;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
public final class FastPolygonDrag {
 public static LittleBoxes build(LittleShape shape,ShapeSelection selection,LittleShapeConfig config){
  if(shape.getClass()!=LittleShapePolygon.class||selection.size()<2)return FastCylinderDrag.build(shape,selection,config);
  LittleBox bounds=selection.getOverallBox();
  PolygonPixelRows pixels=new PolygonPixelRows(bounds.maxX-bounds.minX,bounds.maxY-bounds.minY,bounds.maxZ-bounds.minZ);
  if(selection.size()==2)sample(pixels,selection,selection.get(0),selection.get(1),selection.get(1));
  else for(int i=2;i<selection.size();i++)sample(pixels,selection,selection.get(i-2),selection.get(i-1),selection.get(i));
  LittleBoxesSimple result=new LittleBoxesSimple(selection.pos,selection.grid);
  for(int[] b:pixels.finish())result.add(new LittleBox(b[0],b[1],b[2],b[3],b[4],b[5]));
  return result;
 }
 private static void sample(PolygonPixelRows pixels,ShapeSelection selection,ShapePosition first,ShapePosition second,ShapePosition third){
  var a=first.getRelative(selection.pos).getVec(first.getGrid());
  var b=second.getRelative(selection.pos).getVec(second.getGrid());
  var c=third.getRelative(selection.pos).getVec(third.getGrid());
  double half=first.getGrid().halfPixelLength;
  a.x+=half;a.y+=half;a.z+=half;b.x+=half;b.y+=half;b.z+=half;c.x+=half;c.y+=half;c.z+=half;
  var ab=new Ray3d(a,b,false).direction;var ac=new Ray3d(a,c,false).direction;
  int nu=(int)Math.ceil(ab.length()/selection.grid.pixelLength*1.4),nv=(int)Math.ceil(ac.length()/selection.grid.pixelLength*1.4);
  if(PolygonLineSamples.sample(pixels,selection.grid,a,ab,ac,nu,nv))return;
  double su=1.0/(nu-1),sv=1.0/(nv-1);
  for(int i=0;i<nu;i++){double s=i*su;
   for(int j=0;j<nv;j++){double t=j*sv;
    if(!(s<=1&&t<=1&&s+t<=1))continue;
    // Same arithmetic order and grid rounding as native generatePixels.
    int x=selection.grid.toGrid((a.x+ab.x*s)+ac.x*t);
    int y=selection.grid.toGrid((a.y+ab.y*s)+ac.y*t);
    int z=selection.grid.toGrid((a.z+ab.z*s)+ac.z*t);
    pixels.add(x,y,z);
   }
  }
 }
}

