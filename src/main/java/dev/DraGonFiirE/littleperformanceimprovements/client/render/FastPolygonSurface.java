package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.creativecore.common.util.math.geo.Ray3d;
import team.creative.littletiles.client.tool.shaper.*;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapePolygon;
import team.creative.littletiles.common.placement.shape.config.LittleShapeConfig;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
import team.creative.littletiles.common.grid.LittleGrid;
public final class FastPolygonSurface {
 public static LittleBoxes build(LittleShape shape,ShapeSelection selection,LittleShapeConfig config){
  if(shape.getClass()!=LittleShapePolygon.class||selection.size()<3)return FastPolygonDrag.build(shape,selection,config);
  LittleBox bounds=selection.getOverallBox();
  PolygonSpanRows rows=new PolygonSpanRows(bounds.maxX-bounds.minX,bounds.maxY-bounds.minY,bounds.maxZ-bounds.minZ);
  for(int i=2;i<selection.size();i++)sample(rows,selection,selection.get(i-2),selection.get(i-1),selection.get(i));
  LittleBoxesSimple result=new LittleBoxesSimple(selection.pos,selection.grid);
  for(int[] b:rows.finish())result.add(new LittleBox(b[0],b[1],b[2],b[3],b[4],b[5]));
  return result;
 }
 private static int at(LittleGrid grid,double base,double direction,double step,int j){return grid.toGrid(base+direction*(j*step));}
 private static void sample(PolygonSpanRows rows,ShapeSelection selection,ShapePosition first,ShapePosition second,ShapePosition third){
  var a=first.getRelative(selection.pos).getVec(first.getGrid());var b=second.getRelative(selection.pos).getVec(second.getGrid());var c=third.getRelative(selection.pos).getVec(third.getGrid());
  double half=first.getGrid().halfPixelLength;
  a.x+=half;a.y+=half;a.z+=half;b.x+=half;b.y+=half;b.z+=half;c.x+=half;c.y+=half;c.z+=half;
  var ab=new Ray3d(a,b,false).direction;var ac=new Ray3d(a,c,false).direction;
  int nu=(int)Math.ceil(ab.length()/selection.grid.pixelLength*1.4),nv=(int)Math.ceil(ac.length()/selection.grid.pixelLength*1.4);
  if(nu<=1||nv<=1)return;
  PolygonSpanSampling.fill(rows,selection.grid,new double[]{a.x,a.y,a.z},new double[]{ab.x,ab.y,ab.z},new double[]{ac.x,ac.y,ac.z},nu,nv);
 }
}

